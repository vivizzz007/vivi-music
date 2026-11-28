/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.vivimusic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext

/**
 * Checks if Bluetooth headphones (A2DP or SCO) are currently connected.
 */
fun isBluetoothHeadphoneConnected(context: Context): Boolean {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        audioDevices.any { device ->
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }
    } else {
        // For older Android versions, use deprecated method
        @Suppress("DEPRECATION")
        audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn
    }
}
/**
 * Returns the name of the currently connected Bluetooth audio device, or null if none.
 */
/**
 * Returns the name of the currently connected Bluetooth audio device, or null if none.
 * Only returns the name if Bluetooth is the ACTIVE audio output.
 */
fun getConnectedBluetoothDeviceName(context: Context, preferredDeviceId: Int? = null): String? {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && preferredDeviceId != null) {
        val preferred = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .find { it.id == preferredDeviceId }
        if (preferred != null) {
            return if (preferred.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                preferred.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            ) {
                preferred.productName?.toString()
            } else {
                null
            }
        }
    }

    // Check if Bluetooth is actually the active route
    val isBluetoothActive = audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn
    if (!isBluetoothActive) return null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        
        val activeBluetoothDevice = audioDevices.find { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
            ?: audioDevices.find { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            
        return activeBluetoothDevice?.productName?.toString()
    } else {
        return null
    }
}

@Composable
fun rememberActiveBluetoothDeviceName(preferredDeviceId: Int?): String? {
    val context = LocalContext.current
    val name by produceState<String?>(
        initialValue = getConnectedBluetoothDeviceName(context, preferredDeviceId),
        key1 = preferredDeviceId,
    ) {
        value = getConnectedBluetoothDeviceName(context, preferredDeviceId)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                value = getConnectedBluetoothDeviceName(context, preferredDeviceId)
            }
        }

        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            object : android.media.AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    value = getConnectedBluetoothDeviceName(context, preferredDeviceId)
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    value = getConnectedBluetoothDeviceName(context, preferredDeviceId)
                }
            }
        } else null

        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction("android.bluetooth.adapter.action.STATE_CHANGED")
            addAction("android.bluetooth.device.action.ACL_CONNECTED")
            addAction("android.bluetooth.device.action.ACL_DISCONNECTED")
            addAction("android.media.AUDIO_BECOMING_NOISY")
        }

        context.registerReceiver(receiver, filter)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && callback != null) {
            audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        }

        awaitDispose {
            context.unregisterReceiver(receiver)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && callback != null) {
                audioManager.unregisterAudioDeviceCallback(callback)
            }
        }
    }
    return name
}

/**
 * Returns true if the device name suggests it is a pair of earbuds (buds).
 */
fun isBuds(name: String?): Boolean {
    if (name == null) return false
    val lowerName = name.lowercase()
    return lowerName.contains("buds") || 
           lowerName.contains("airpods") || 
           lowerName.contains("earpods") || 
           lowerName.contains("earphone") ||
           lowerName.contains("freebuds") ||
           lowerName.contains("pods")
}

/**
 * Returns true if the device name suggests it is a speaker.
 */
fun isSpeaker(name: String?): Boolean {
    if (name == null) return false
    val lowerName = name.lowercase()
    return lowerName.contains("speaker") || 
           lowerName.contains("soundbar") || 
           lowerName.contains("homepod") || 
           lowerName.contains("echo") ||
           lowerName.contains("boombox") ||
           lowerName.contains("audio system") ||
           lowerName.contains("sound") ||
           lowerName.contains("audio") ||
           lowerName.contains("stereo") ||
           lowerName.contains("music") ||
           lowerName.contains("box") ||
           lowerName.contains("party") ||
           lowerName.contains("waves")
}
