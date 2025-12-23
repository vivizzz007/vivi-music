package com.music.vivi.update.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.music.vivi.playback.MusicService

class MusicPlayerWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Trigger update through MusicService if running
        val intent = Intent(context, MusicService::class.java).apply {
            action = ACTION_UPDATE_WIDGET
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            // Service might be restricted in background
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_PLAY_PAUSE, ACTION_LIKE, ACTION_PLAY_SONG, ACTION_PLAY_QUEUE_ITEM -> {
                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    action = intent.action
                    putExtras(intent)
                }
                try {
                    context.startService(serviceIntent)
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
