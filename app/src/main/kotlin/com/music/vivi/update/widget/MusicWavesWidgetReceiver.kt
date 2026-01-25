package com.music.vivi.update.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import com.music.vivi.playback.MusicService

class MusicWavesWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Trigger update through MusicService if running
        if (MusicService.isRunning) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = MusicPlayerWidgetReceiver.ACTION_UPDATE_WIDGET
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Service might be restricted in background
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            MusicPlayerWidgetReceiver.ACTION_PLAY_PAUSE,
            MusicPlayerWidgetReceiver.ACTION_LIKE,
            MusicPlayerWidgetReceiver.ACTION_PLAY_SONG,
            MusicPlayerWidgetReceiver.ACTION_PLAY_QUEUE_ITEM,
            -> {
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
                    // Service might be restricted in background
                }
            }
        }
    }
}
