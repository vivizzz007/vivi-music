package com.music.vivi.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
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

    // Volume long-press detection
    private var volumeLongPressRunnable: Runnable? = null
    private var isVolumeLongPressTriggered = false
    private var activeVolumeKeyCode = KeyEvent.KEYCODE_UNKNOWN

    // Power button double-press detection
    private var lastPowerPressTime = 0L
    private var lastScreenStateChangeTime = 0L
    private var lastScreenTouchTime = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val now = SystemClock.uptimeMillis()
            val action = intent.action
            if (action == Intent.ACTION_SCREEN_OFF || action == Intent.ACTION_SCREEN_ON) {
                if (!powerButtonCameraEnabled) return

                // Check if screen was recently touched (e.g. double tap to wake/sleep on screen)
                val isFromTouch = (now - lastScreenTouchTime) < 400L
                if (isFromTouch) {
                    Timber.tag(TAG).d("Screen state changed via touchscreen; ignoring power button trigger")
                    lastScreenStateChangeTime = 0L
                    return
                }

                val timeDiff = now - lastScreenStateChangeTime
                if (timeDiff in 40L..powerButtonIntervalMs.toLong()) {
                    Timber.tag(TAG).d("Power button double-press detected via screen toggle ($timeDiff ms)! Launching camera.")
                    launchCamera()
                    lastScreenStateChangeTime = 0L
                } else {
                    lastScreenStateChangeTime = now
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        Timber.tag(TAG).d("ViviAccessibilityService connected")

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        serviceInfo = info

        // Register screen state receiver for power toggle tracking
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)

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
                preferences[PowerButtonIntervalKey] ?: 200
            }.distinctUntilChanged().collect { interval ->
                powerButtonIntervalMs = interval.coerceIn(100, 300)
                Timber.tag(TAG).d("powerButtonIntervalMs: $powerButtonIntervalMs")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Track recent touch interactions on screen to avoid triggering when double-tapping the display
        when (event?.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START,
            AccessibilityEvent.TYPE_GESTURE_DETECTION_START,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
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
                if (diff in 40L..powerButtonIntervalMs.toLong()) {
                    Timber.tag(TAG).d("Power button double-press detected directly ($diff ms)! Launching camera.")
                    launchCamera()
                    lastPowerPressTime = 0L
                    return false
                } else {
                    lastPowerPressTime = now
                }
            }
            return false
        }

        // Screen-off volume long-press skip track
        if (screenOffVolumeSkipEnabled &&
            (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        ) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isScreenOff = powerManager?.isInteractive == false
            val isMusicPlaying = MusicService.isPlaying()

            if (isScreenOff && isMusicPlaying) {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (event.repeatCount == 0) {
                            activeVolumeKeyCode = keyCode
                            isVolumeLongPressTriggered = false
                            volumeLongPressRunnable?.let { handler.removeCallbacks(it) }

                            val runnable = Runnable {
                                isVolumeLongPressTriggered = true
                                vibrateHaptic()
                                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                                    Timber.tag(TAG).d("Volume UP long-pressed with screen off: skipping to next track")
                                    MusicService.skipNext()
                                } else {
                                    Timber.tag(TAG).d("Volume DOWN long-pressed with screen off: skipping to previous track")
                                    MusicService.skipPrevious()
                                }
                            }
                            volumeLongPressRunnable = runnable
                            handler.postDelayed(runnable, LONG_PRESS_TIMEOUT_MS)
                            return true // Consume to prevent volume change during initial press
                        } else {
                            // Repeating key down while holding
                            return true
                        }
                    }

                    KeyEvent.ACTION_UP -> {
                        volumeLongPressRunnable?.let {
                            handler.removeCallbacks(it)
                            volumeLongPressRunnable = null
                        }

                        if (isVolumeLongPressTriggered) {
                            // Long-press was executed, consume release event so volume does NOT change
                            isVolumeLongPressTriggered = false
                            return true
                        } else {
                            // Short click released before threshold: adjust volume by 1 step normally
                            adjustVolumeOneStep(keyCode)
                            return true
                        }
                    }
                }
            }
        }

        return super.onKeyEvent(event)
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

    private fun vibrateHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50L)
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }

    private fun launchCamera() {
        try {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            val isLocked = keyguardManager?.isKeyguardLocked == true
            val action = if (isLocked) {
                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE
            } else {
                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
            }

            val cameraIntent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(cameraIntent)
            vibrateHaptic()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error launching secure camera, attempting fallback")
            try {
                val fallbackIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Timber.tag(TAG).e(e2, "Failed to launch fallback camera")
            }
        }
    }

    override fun onDestroy() {
        isServiceRunning = false
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Ignore
        }
        volumeLongPressRunnable?.let { handler.removeCallbacks(it) }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ViviAccessibility"
        private const val LONG_PRESS_TIMEOUT_MS = 500L

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
