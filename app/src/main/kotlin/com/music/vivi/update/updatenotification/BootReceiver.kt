package com.music.vivi.update.updatenotification


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Check if automatic update check is enabled
            val prefs = context.getSharedPreferences("update_checker", Context.MODE_PRIVATE)
            val autoCheckEnabled = prefs.getBoolean("notification_enabled", true)

            if (autoCheckEnabled) {
                // Restart the periodic update check after device reboot
                val updateManager = UpdateNotificationManager(context)
                updateManager.schedulePeriodicUpdateCheck()
            }
        }
    }
}