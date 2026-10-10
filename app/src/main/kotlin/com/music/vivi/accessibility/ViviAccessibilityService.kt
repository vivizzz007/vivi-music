package com.music.vivi.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.view.Display
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.music.vivi.constants.PlusButtonOpenViviKey
import com.music.vivi.constants.PowerButtonCameraKey
import com.music.vivi.constants.PowerButtonIntervalKey
import com.music.vivi.constants.ScreenOffVolumeSkipKey
import com.music.vivi.playback.MusicService
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

class ViviAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())

    private var screenOffVolumeSkipEnabled = false
    private var powerButtonCameraEnabled = false
    private var powerButtonIntervalMs = 200
    private var plusButtonOpenViviEnabled = false

    // Volume long-press detection
    private var volumeLongPressRunnable: Runnable? = null
    private var isVolumeLongPressTriggered = false

    // The volume key whose press we swallowed on the way down. Its release has to be handled by us as well,
    // even if the screen / player state changed in between (e.g. the player is buffering after the skip).
    private var swallowedVolumeKey = KeyEvent.KEYCODE_UNKNOWN

    // Power button double-press detection.
    // The power key itself is usually not delivered to accessibility services, so the screen turning off and on again is
    // watched through two independent signals (display state and screen broadcasts) plus the raw key as a bonus.
    // Each signal keeps its own timestamps so that one press is never counted twice; a shared debounce makes sure the
    // camera is only launched once however many of them noticed the double press.
    private var lastPowerPressTime = 0L
    private var lastBroadcastToggleTime = 0L
    private var lastDisplayToggleTime = 0L
    private var lastKnownDisplayOn: Boolean? = null
    private var lastCameraLaunchTime = 0L
    private var lastScreenTouchTime = 0L

    // Plus button double-press detection
    private var lastPlusPressTime = 0L

    private val displayManager by lazy { getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (!powerButtonCameraEnabled) return
            val isOn = displayManager.displays.any { it.state == Display.STATE_ON }
            // Rotation, refresh rate changes etc. also end up here; only an on <-> off change is a power toggle.
            if (lastKnownDisplayOn == isOn) return
            lastKnownDisplayOn = isOn
            onScreenToggled(SystemClock.uptimeMillis(), lastDisplayToggleTime)?.let { lastDisplayToggleTime = it }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == Intent.ACTION_SCREEN_OFF || action == Intent.ACTION_SCREEN_ON) {
                if (!powerButtonCameraEnabled) return
                onScreenToggled(SystemClock.uptimeMillis(), lastBroadcastToggleTime)?.let { lastBroadcastToggleTime = it }
            }
        }
    }

    /**
     * Called for every screen on/off transition reported by one signal.
     *
     * @param previousToggle when this same signal reported the previous transition
     * @return the timestamp to remember for the next call (0 after a successful double press), or null to keep the old one
     */
    private fun onScreenToggled(now: Long, previousToggle: Long): Long? {
        // A transition right after a touch is a double-tap-to-wake / sleep gesture, not a power button press.
        // It is simply ignored; earlier presses are kept so a real double press is not lost.
        if (now - lastScreenTouchTime < TOUCH_GUARD_MS) {
            Timber.tag(TAG).d("Screen state changed via touchscreen; ignoring")
            return null
        }

        val timeDiff = now - previousToggle
        val maxInterval = (powerButtonIntervalMs + 350L).coerceAtLeast(600L)
        return if (previousToggle != 0L && timeDiff in 50L..maxInterval) {
            Timber.tag(TAG).d("Power button double-press detected via screen toggle ($timeDiff ms)! Launching camera.")
            launchCamera()
            0L
        } else {
            now
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        Timber.tag(TAG).d("ViviAccessibilityService connected")

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.apply {
            // Only touch events are needed (to tell a double-tap-to-wake from a power button press). Receiving every
            // accessibility event of every app used to keep this service's main thread busy, which delayed the key
            // handling below and made the shortcuts flaky: the system gives up on a key event after ~500 ms.
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_LONG_CLICKED or
                    AccessibilityEvent.TYPE_TOUCH_INTERACTION_START or
                    AccessibilityEvent.TYPE_GESTURE_DETECTION_START
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = (flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS) and
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS.inv()
        }
        serviceInfo = info

        // Register screen state receiver for power toggle tracking
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
        lastKnownDisplayOn = displayManager.displays.any { it.state == Display.STATE_ON }
        displayManager.registerDisplayListener(displayListener, handler)

        // Observe DataStore preferences
        serviceScope.launch {
            dataStore.data.map { preferences ->
                preferences[ScreenOffVolumeSkipKey] ?: false
            }.distinctUntilChanged().collect { enabled ->
                screenOffVolumeSkipEnabled = enabled
                Timber.tag(TAG).d("screenOffVolumeSkipEnabled: $enabled")
            }
        }

        serviceScope.launch {
            dataStore.data.map { preferences ->
                preferences[PowerButtonCameraKey] ?: false
            }.distinctUntilChanged().collect { enabled ->
                powerButtonCameraEnabled = enabled
                Timber.tag(TAG).d("powerButtonCameraEnabled: $enabled")
            }
        }

        serviceScope.launch {
            dataStore.data.map { preferences ->
                preferences[PowerButtonIntervalKey] ?: 250
            }.distinctUntilChanged().collect { interval ->
                powerButtonIntervalMs = interval.coerceIn(100, 600)
                Timber.tag(TAG).d("powerButtonIntervalMs: $powerButtonIntervalMs")
            }
        }

        serviceScope.launch {
            dataStore.data.map { preferences ->
                preferences[PlusButtonOpenViviKey] ?: false
            }.distinctUntilChanged().collect { enabled ->
                plusButtonOpenViviEnabled = enabled
                Timber.tag(TAG).d("plusButtonOpenViviEnabled: $enabled")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Track recent touch interactions on screen to avoid triggering when double-tapping the display
        when (event?.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START,
            AccessibilityEvent.TYPE_GESTURE_DETECTION_START -> {
                lastScreenTouchTime = SystemClock.uptimeMillis()
            }
        }
    }

    override fun onInterrupt() {
        Timber.tag(TAG).d("ViviAccessibilityService interrupted")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode

        // Physical Power button direct key capture
        if (keyCode == KeyEvent.KEYCODE_POWER && powerButtonCameraEnabled) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val now = SystemClock.uptimeMillis()
                val diff = now - lastPowerPressTime
                val maxInterval = (powerButtonIntervalMs + 200L).coerceAtLeast(500L)
                if (diff in 50L..maxInterval) {
                    Timber.tag(TAG).d("Power button double-press detected directly ($diff ms)! Launching camera.")
                    lastPowerPressTime = 0L
                    launchCamera()
                    return false
                } else {
                    lastPowerPressTime = now
                }
            }
            return false
        }

        // Physical Plus key double-press to open Vivi
        if (plusButtonOpenViviEnabled && isPlusKey(keyCode)) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val now = SystemClock.uptimeMillis()
                val diff = now - lastPlusPressTime
                if (diff in 50L..500L) {
                    Timber.tag(TAG).d("Plus button double-press detected ($diff ms)! Launching Vivi.")
                    lastPlusPressTime = 0L
                    vibrateCameraTrigger()
                    launchViviApp()
                    return true
                } else {
                    lastPlusPressTime = now
                }
            }
            return false
        }

        // Screen-off volume long-press skip track
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount > 0 && swallowedVolumeKey == keyCode) {
                        // Key repeat while holding. If the timer was late (busy main thread), the hold time on the
                        // event itself is just as good a clock.
                        if (!isVolumeLongPressTriggered &&
                            event.eventTime - event.downTime >= LONG_PRESS_TIMEOUT_MS
                        ) {
                            triggerVolumeLongPress(keyCode)
                        }
                        return true
                    }

                    if (screenOffVolumeSkipEnabled && isScreenOff() && MusicService.isPlaybackActive()) {
                        swallowedVolumeKey = keyCode
                        isVolumeLongPressTriggered = false
                        cancelVolumeLongPress()
                        val runnable = Runnable { triggerVolumeLongPress(keyCode) }
                        volumeLongPressRunnable = runnable
                        handler.postDelayed(runnable, LONG_PRESS_TIMEOUT_MS)
                        return true // Consume to prevent volume change during initial press
                    }
                }

                KeyEvent.ACTION_UP -> {
                    if (swallowedVolumeKey == keyCode) {
                        swallowedVolumeKey = KeyEvent.KEYCODE_UNKNOWN
                        cancelVolumeLongPress()
                        val longPressHandled = isVolumeLongPressTriggered
                        isVolumeLongPressTriggered = false
                        if (!longPressHandled) {
                            // Short click released before the threshold: adjust volume by 1 step normally
                            adjustVolumeOneStep(keyCode)
                        }
                        // Always consume the release of a press we swallowed, so the system never sees a lone "up".
                        return true
                    }
                }
            }
        }

        return super.onKeyEvent(event)
    }

    private fun isScreenOff(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isInteractive == false
    }

    private fun cancelVolumeLongPress() {
        volumeLongPressRunnable?.let { handler.removeCallbacks(it) }
        volumeLongPressRunnable = null
    }

    private fun triggerVolumeLongPress(keyCode: Int) {
        if (isVolumeLongPressTriggered) return
        isVolumeLongPressTriggered = true
        volumeLongPressRunnable = null
        vibrateVolumeSkip()
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            Timber.tag(TAG).d("Volume UP long-pressed with screen off: skipping to next track")
            MusicService.skipNext()
        } else {
            Timber.tag(TAG).d("Volume DOWN long-pressed with screen off: skipping to previous track")
            MusicService.skipPrevious()
        }
    }

    private fun adjustVolumeOneStep(keyCode: Int) {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val direction = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                AudioManager.ADJUST_RAISE
            } else {
                AudioManager.ADJUST_LOWER
            }
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error adjusting volume one step")
        }
    }

    private fun vibrateVolumeSkip() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50L)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Volume skip vibration failed")
        }
    }

    private fun vibrateCameraTrigger() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Double pulse: 0ms delay, 45ms on, 60ms off, 45ms on
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 45, 60, 45), -1))
                } else {
                    vibrator?.vibrate(VibrationEffect.createOneShot(100L, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 45, 60, 45), -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100L)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Camera trigger vibration failed")
        }
    }

    private fun launchCamera() {
        // The same double press can be noticed by several signals; launch only once.
        val now = SystemClock.uptimeMillis()
        if (lastCameraLaunchTime != 0L && now - lastCameraLaunchTime < CAMERA_LAUNCH_DEBOUNCE_MS) return
        lastCameraLaunchTime = now
        try {
            vibrateCameraTrigger()

            // Turn screen on if locked/off
            try {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                @Suppress("DEPRECATION")
                val wakeLock = powerManager?.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "vivi:camera_wake"
                )
                wakeLock?.acquire(2000L)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "WakeLock acquisition failed")
            }

            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            val isLocked = keyguardManager?.isKeyguardLocked == true
            val action = if (isLocked) {
                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE
            } else {
                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
            }

            var launched = false
            try {
                val cameraIntent = Intent(action).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                startActivity(cameraIntent)
                launched = true
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Error launching camera with action $action")
            }

            if (!launched) {
                try {
                    val fallbackIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    }
                    startActivity(fallbackIntent)
                    launched = true
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Error launching standard camera")
                }
            }

            if (!launched) {
                try {
                    val fallbackCaptureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(fallbackCaptureIntent)
                } catch (e2: Exception) {
                    Timber.tag(TAG).e(e2, "Failed to launch all camera fallbacks")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error launching camera")
        }
    }

    private fun isPlusKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_PLUS ||
        keyCode == KeyEvent.KEYCODE_NUMPAD_ADD ||
        keyCode == 81 || // KEYCODE_PLUS standard integer value
        keyCode == 157   // KEYCODE_NUMPAD_ADD standard integer value

    private fun launchViviApp() {
        try {
            try {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                @Suppress("DEPRECATION")
                val wakeLock = powerManager?.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "vivi:plus_wake"
                )
                wakeLock?.acquire(2000L)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "WakeLock acquisition for Plus key failed")
            }

            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                Timber.tag(TAG).w("Could not get launch intent for package: $packageName")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error launching Vivi Music from Plus key")
        }
    }

    override fun onDestroy() {
        isServiceRunning = false
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Ignore
        }
        try {
            displayManager.unregisterDisplayListener(displayListener)
        } catch (e: Exception) {
            // Ignore
        }
        cancelVolumeLongPress()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ViviAccessibility"
        private const val LONG_PRESS_TIMEOUT_MS = 500L
        private const val TOUCH_GUARD_MS = 300L
        private const val CAMERA_LAUNCH_DEBOUNCE_MS = 2000L

        @Volatile
        var isServiceRunning = false
            private set

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val expectedComponentName = ComponentName(context, ViviAccessibilityService::class.java)
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)
            while (colonSplitter.hasNext()) {
                val componentNameString = colonSplitter.next()
                val enabledComponent = ComponentName.unflattenFromString(componentNameString)
                if (enabledComponent != null && enabledComponent == expectedComponentName) {
                    return true
                }
            }
            return false
        }
    }
}
