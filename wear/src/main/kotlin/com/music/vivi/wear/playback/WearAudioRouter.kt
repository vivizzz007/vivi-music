/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.playback

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Abstraction representing an audio output peripheral device.
 */
interface AudioDeviceOutput {
    val type: Int
    val productName: CharSequence?
    val address: String?
}

/**
 * Wraps Android framework [AudioDeviceInfo].
 */
class SystemAudioDeviceInfo(val device: AudioDeviceInfo) : AudioDeviceOutput {
    override val type: Int get() = device.type
    override val productName: CharSequence? get() = device.productName
    override val address: String? get() = device.address
}

/**
 * Lightweight data class for testing audio output routing.
 */
data class SimpleAudioDevice(
    override val type: Int,
    override val productName: CharSequence? = null,
    override val address: String? = null,
) : AudioDeviceOutput

/**
 * Manages audio routing for standalone Wear OS playback.
 *
 * Supports phone-free playback through paired Bluetooth audio peripherals
 * (Bluetooth A2DP, BLE Headsets, Hearing Aids) and Galaxy Watch built-in speakers.
 */
class WearAudioRouter(
    private val context: Context,
    val audioManager: AudioManager? = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager,
    private val deviceProvider: (() -> List<AudioDeviceOutput>)? = null,
) {
    private val _bluetoothState = MutableStateFlow<BluetoothOutputState>(BluetoothOutputState.Disconnected)
    val bluetoothState: StateFlow<BluetoothOutputState> = _bluetoothState.asStateFlow()

    /**
     * Optional callback invoked when Bluetooth audio is disconnected during playback.
     */
    var onBluetoothDisconnected: (() -> Unit)? = null

    /**
     * Optional callback invoked when a Bluetooth audio device connects.
     */
    var onBluetoothConnected: ((String) -> Unit)? = null

    private var isCallbackRegistered = false

    internal val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            handleDevicesAdded(addedDevices?.map { SystemAudioDeviceInfo(it) }.orEmpty())
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            handleDevicesRemoved(removedDevices?.map { SystemAudioDeviceInfo(it) }.orEmpty())
        }
    }

    internal fun handleDevicesAdded(addedDevices: List<AudioDeviceOutput>) {
        val connectedBt = addedDevices.firstOrNull { it.type in BLUETOOTH_OUTPUT_TYPES }
        if (connectedBt != null) {
            val name = connectedBt.productName?.toString()?.ifBlank { "Bluetooth Audio" } ?: "Bluetooth Audio"
            Timber.d("Bluetooth audio device connected: %s (type=%d)", name, connectedBt.type)
            _bluetoothState.value = BluetoothOutputState.Connected(
                deviceName = name,
                deviceType = connectedBt.type,
                address = connectedBt.address,
            )
            onBluetoothConnected?.invoke(name)
        }
    }

    internal fun handleDevicesRemoved(removedDevices: List<AudioDeviceOutput>) {
        if (!isBluetoothAudioConnected()) {
            Timber.w("Bluetooth audio device disconnected! No remaining Bluetooth audio output.")
            _bluetoothState.value = BluetoothOutputState.Disconnected
            onBluetoothDisconnected?.invoke()
        } else {
            refreshState()
        }
    }

    /**
     * Returns all currently connected audio output devices.
     */
    fun getOutputDevices(): List<AudioDeviceOutput> {
        return deviceProvider?.invoke()
            ?: audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.map { SystemAudioDeviceInfo(it) }
            ?: emptyList()
    }

    /**
     * Checks whether any Bluetooth audio output peripheral is currently connected.
     * Supported device types:
     * - [AudioDeviceInfo.TYPE_BLUETOOTH_A2DP]
     * - [AudioDeviceInfo.TYPE_BLE_HEADSET]
     * - [AudioDeviceInfo.TYPE_HEARING_AID]
     * - [AudioDeviceInfo.TYPE_BLE_SPEAKER]
     */
    fun isBluetoothAudioConnected(): Boolean {
        return getOutputDevices().any { it.type in BLUETOOTH_OUTPUT_TYPES }
    }

    /**
     * Returns the first connected Bluetooth audio output device info, or null if none connected.
     */
    fun getConnectedBluetoothDevice(): AudioDeviceOutput? {
        return getOutputDevices().firstOrNull { it.type in BLUETOOTH_OUTPUT_TYPES }
    }

    /**
     * Returns the name of the currently connected Bluetooth audio device, or null.
     */
    fun getConnectedDeviceName(): String? {
        val device = getConnectedBluetoothDevice() ?: return null
        return device.productName?.toString()?.ifBlank { "Bluetooth Audio" } ?: "Bluetooth Audio"
    }

    /**
     * Returns a user-facing label for the current audio output route.
     * Returns the connected Bluetooth device name if available, or "Watch Speaker".
     */
    fun getCurrentAudioRouteLabel(): String {
        return if (isBluetoothAudioConnected()) {
            getConnectedDeviceName() ?: "Bluetooth Audio"
        } else {
            "Watch Speaker"
        }
    }

    /**
     * Checks whether audio is currently routed through the watch built-in speaker.
     */
    fun isUsingSpeaker(): Boolean = !isBluetoothAudioConnected()

    /**
     * Registers system [AudioDeviceCallback] to monitor connection/disconnection of audio peripherals.
     */
    fun registerAudioDeviceCallback() {
        if (isCallbackRegistered) return
        val manager = audioManager ?: return

        // Initialize current state
        refreshState()

        val handler = Handler(Looper.getMainLooper())
        manager.registerAudioDeviceCallback(audioDeviceCallback, handler)
        isCallbackRegistered = true
        Timber.d("WearAudioRouter registered AudioDeviceCallback")
    }

    /**
     * Unregisters system [AudioDeviceCallback].
     */
    fun unregisterAudioDeviceCallback() {
        if (!isCallbackRegistered) return
        audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
        isCallbackRegistered = false
        Timber.d("WearAudioRouter unregistered AudioDeviceCallback")
    }

    /**
     * Forces a refresh of the current Bluetooth audio output connection state.
     */
    fun refreshState() {
        val btDevice = getConnectedBluetoothDevice()
        if (btDevice != null) {
            val name = btDevice.productName?.toString()?.ifBlank { "Bluetooth Audio" } ?: "Bluetooth Audio"
            _bluetoothState.value = BluetoothOutputState.Connected(
                deviceName = name,
                deviceType = btDevice.type,
                address = btDevice.address,
            )
        } else {
            _bluetoothState.value = BluetoothOutputState.Disconnected
        }
    }

    /**
     * Generates an Intent to open the system Bluetooth settings screen on Wear OS.
     */
    fun openBluetoothSettingsIntent(): Intent {
        return Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    /**
     * Safely attempts to open the system Bluetooth settings screen.
     */
    fun openBluetoothSettings(callerContext: Context = context): Boolean {
        return try {
            callerContext.startActivity(openBluetoothSettingsIntent())
            true
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "Bluetooth settings activity not found on device")
            false
        }
    }

    companion object {
        /**
         * Set of supported Bluetooth audio peripheral device types for playback routing.
         */
        val BLUETOOTH_OUTPUT_TYPES: Set<Int> = setOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_HEARING_AID,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
        )
    }
}
