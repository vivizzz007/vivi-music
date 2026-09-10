/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.playback

/**
 * Represents the current connection state of Bluetooth audio output peripherals
 * (headphones, earbuds, hearing aids) on Wear OS.
 */
sealed interface BluetoothOutputState {
    /**
     * A Bluetooth audio output device is connected and available for media routing.
     *
     * @property deviceName User-friendly product name of the connected peripheral.
     * @property deviceType One of [android.media.AudioDeviceInfo] types (e.g. TYPE_BLUETOOTH_A2DP, TYPE_BLE_HEADSET).
     * @property address Hardware MAC/address if available from the system.
     */
    data class Connected(
        val deviceName: String,
        val deviceType: Int,
        val address: String? = null,
    ) : BluetoothOutputState

    /**
     * No Bluetooth audio output peripheral is actively connected.
     */
    data object Disconnected : BluetoothOutputState
}
