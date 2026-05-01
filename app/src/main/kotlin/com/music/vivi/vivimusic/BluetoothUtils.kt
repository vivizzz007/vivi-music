package com.music.vivi.vivimusic

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi

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
fun getConnectedBluetoothDeviceName(context: Context): String? {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

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
