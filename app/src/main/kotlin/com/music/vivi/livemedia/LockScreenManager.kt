package com.music.vivi.livemedia

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

class LockScreenManager(
    private val context: Context,
    private val deviceLocked: () -> Unit,
    private val deviceUnlocked: () -> Unit
) {
    private val TAG = "LockScreenManager"
    private val lockStateReceiver = LockStateReceiver()

    init {
        Log.i(TAG, "start and registerReceiver lockStateReceiver")
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        context.registerReceiver(lockStateReceiver, filter)
    }

    fun isScreenUnlocked(): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return !(keyguardManager?.isDeviceLocked ?: false)
    }

    private inner class LockStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.i(TAG, "Screen OFF broadcast received.")
                    deviceLocked()
                }

                Intent.ACTION_USER_PRESENT -> {
                    Log.i(TAG, "User PRESENT broadcast received.")
                    deviceUnlocked()
                }
            }
        }
    }
}
