package com.music.vivi.update.updatenotification


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            // Initialize update notifications after device boot or app update
            val updateManager = UpdateNotificationManager(context.applicationContext)
            updateManager.schedulePeriodicUpdateCheck()
        }
    }
}
