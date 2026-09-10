/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.music.vivi.wear.R
import com.music.vivi.wear.WearApp
import timber.log.Timber

/**
 * Foreground service managing offline downloads on Wear OS.
 *
 * Declared with foregroundServiceType="dataSync" in AndroidManifest.xml.
 * Provides battery-efficient background syncing to internal watch flash storage.
 */
class WearDownloadService : DownloadService(
    NOTIFICATION_ID,
    FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.download_channel_name,
    R.string.download_channel_description,
) {

    private lateinit var notificationHelper: DownloadNotificationHelper

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        notificationHelper = DownloadNotificationHelper(this, CHANNEL_ID)
    }

    override fun getDownloadManager(): DownloadManager {
        return WearApp.instance.downloadManager.media3DownloadManager
    }

    override fun getScheduler(): Scheduler {
        return PlatformScheduler(this, JOB_ID)
    }

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int,
    ): Notification {
        val title = getString(R.string.downloading)
        val message = if (downloads.size == 1) {
            val data = downloads[0].request.data
            if (data.isNotEmpty()) Util.fromUtf8Bytes(data) else downloads[0].request.id
        } else {
            getString(R.string.n_songs, downloads.size)
        }

        val notification = notificationHelper.buildProgressNotification(
            this,
            R.mipmap.ic_launcher,
            null,
            message,
            downloads,
            notMetRequirements,
        )

        // Attach cancel action to notification
        val cancelIntent = Intent(this, WearDownloadService::class.java).apply {
            action = ACTION_CANCEL_ALL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return Notification.Builder.recoverBuilder(this, notification)
            .setContentTitle(title)
            .addAction(
                Notification.Action.Builder(
                    null,
                    getString(R.string.cancel),
                    cancelPendingIntent,
                ).build(),
            )
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL_ALL) {
            Timber.i("WearDownloadService received ACTION_CANCEL_ALL")
            WearApp.instance.downloadManager.cancelAllDownloads()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun ensureNotificationChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existingChannel == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.download_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.download_channel_description)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "wear_downloads"
        const val NOTIFICATION_ID = 1001
        const val JOB_ID = 1002
        const val FOREGROUND_NOTIFICATION_UPDATE_INTERVAL = 1000L
        const val ACTION_CANCEL_ALL = "com.music.vivi.wear.action.CANCEL_ALL_DOWNLOADS"
    }
}
