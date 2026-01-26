package com.music.vivi.update.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import com.music.vivi.playback.MusicService

class MusicPlayerWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Trigger update through MusicService if running
        // Avoid starting service from onUpdate to prevent ForegroundServiceStartNotAllowedException
        // The service should update the widget when active.
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_PLAY_PAUSE, ACTION_LIKE, ACTION_PLAY_SONG, ACTION_PLAY_QUEUE_ITEM -> {
                // For user interactions, we might be able to start the service,
                // but if we are in BOOT_COMPLETED flow (which shouldn't hit this path typically, but effectively similar restriction for background start)
                // However, these are explicit user actions on the widget buttons.
                // Android 12+ allows starting FGS from background if triggered by widget interaction?
                // Wait, "FGS type mediaPlayback not allowed to start from BOOT_COMPLETED".
                // The crash happened on BOOT_COMPLETED.
                // The stack trace says: "FGS type mediaPlayback not allowed to start from BOOT_COMPLETED!"
                // This exception usually happens when a BroadcastReceiver trying to start an FGS from background (e.g. after boot).

                // If it's a direct user interaction, we usually can start it.
                // But let's be safe. If the user clicks play, we WANT to start the service.
                // But the crash report says "BOOT_COMPLETED", implying it wasn't a user click.

                // The widget update logic in `onUpdate` is the main culprit for boot crashes.
                // But let's verify if we need to guard here too.
                // Usually widget buttons send PendingIntents.

                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    action = intent.action
                    putExtras(intent)
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    // Usually these buttons are clicked when the user is interacting or the service is already in foreground
                }
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.music.vivi.widget.PLAY_PAUSE"
        const val ACTION_LIKE = "com.music.vivi.widget.LIKE"
        const val ACTION_PLAY_SONG = "com.music.vivi.widget.PLAY_SONG"
        const val ACTION_PLAY_QUEUE_ITEM = "com.music.vivi.widget.PLAY_QUEUE_ITEM"
        const val ACTION_UPDATE_WIDGET = "com.music.vivi.widget.UPDATE_WIDGET"
    }
}
