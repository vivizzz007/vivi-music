package com.music.vivi.bluetooth

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.M)
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
