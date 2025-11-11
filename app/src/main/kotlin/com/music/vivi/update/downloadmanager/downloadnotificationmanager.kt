package com.music.vivi.update.downloadmanager


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toColorInt
import com.music.vivi.R


object DownloadNotificationManager {
    private lateinit var notificationManager: NotificationManager
    private lateinit var appContext: Context

    const val CHANNEL_ID = "download_progress_channel"
    private const val CHANNEL_NAME = "Download Progress"
    private const val NOTIFICATION_ID = 5678

    fun initialize(context: Context) {
        appContext = context
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH  // CHANGED: From LOW to HIGH
            ).apply {
                description = "Shows download progress for app updates"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC  // ADDED: Show on lock screen
                enableVibration(false)
                enableLights(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show download starting notification
     */
    fun showDownloadStarting(version: String, fileSize: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            showDownloadStartingModern(version, fileSize)
        } else {
            showDownloadStartingLegacy(version, fileSize)
        }
    }

    /**
     * Update download progress notification
     */
    fun updateDownloadProgress(progress: Int, version: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            updateDownloadProgressModern(progress, version)
        } else {
            updateDownloadProgressLegacy(progress, version)
        }
    }

    /**
     * Show download completed notification
     */
    fun showDownloadComplete(version: String, filePath: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            showDownloadCompleteModern(version, filePath)
        } else {
            showDownloadCompleteLegacy(version, filePath)
        }
    }

    /**
     * Show download failed notification
     */
    fun showDownloadFailed(version: String, errorMessage: String) {
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Update Failed")
            .setContentText("Failed to download version $version")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Failed to download version $version\n$errorMessage"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // ADDED: For lock screen
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Cancel/dismiss the notification
     */
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    // ============ Modern Implementation (Android 16+) ============
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun showDownloadStartingModern(version: String, fileSize: String) {
        val progressStyle = Notification.ProgressStyle().apply {
            isStyledByProgress = true
            progress = 0
            progressSegments = listOf(
                Notification.ProgressStyle.Segment(100)
                    .setColor("#4285F4".toColorInt())
            )

            progressStartIcon = Icon.createWithResource(
                appContext,
                R.drawable.web_browser
            ).setTint("#4285F4".toColorInt())

            progressEndIcon = Icon.createWithResource(
                appContext,
                R.drawable.deployed_code_update_server
            ).setTint("#4285F4".toColorInt())

            progressTrackerIcon = Icon.createWithResource(
                appContext,
                R.drawable.delivery_truck
            )
        }

        val notification = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.delivery_truck)
            .setContentTitle("Downloading Update")
            .setContentText("Version $version • $fileSize")
            .setOngoing(true)
            .setStyle(progressStyle)
            .setVisibility(Notification.VISIBILITY_PUBLIC)  // ADDED: Show on lock screen
            .setCategory(Notification.CATEGORY_PROGRESS)    // ADDED: Proper category
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun updateDownloadProgressModern(progress: Int, version: String) {
        val progressStyle = Notification.ProgressStyle().apply {
            isStyledByProgress = true
            this.progress = progress

            progressSegments = listOf(
                Notification.ProgressStyle.Segment(100)
                    .setColor(
                        if (progress < 100) "#4285F4".toColorInt()
                        else "#34A853".toColorInt()
                    )
            )

            progressStartIcon = Icon.createWithResource(
                appContext,
                R.drawable.cloud
            )

            progressEndIcon = Icon.createWithResource(
                appContext,
                R.drawable.app
            )

            progressTrackerIcon = Icon.createWithResource(
                appContext,
                R.drawable.delivery_truck_me
            )
        }

        val notification = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading Update")
            .setContentText("Version $version • $progress%")
            .setOngoing(progress < 100)
            .setStyle(progressStyle)
            .setVisibility(Notification.VISIBILITY_PUBLIC)  // ADDED: Show on lock screen
            .setCategory(Notification.CATEGORY_PROGRESS)    // ADDED: Proper category
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun showDownloadCompleteModern(version: String, filePath: String) {
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                androidx.core.content.FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.FileProvider",
                    java.io.File(filePath)
                ),
                "application/vnd.android.package-archive"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            installIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val progressStyle = Notification.ProgressStyle().apply {
            isStyledByProgress = true
            progress = 100

            progressSegments = listOf(
                Notification.ProgressStyle.Segment(100)
                    .setColor("#34A853".toColorInt())
            )

            progressStartIcon = Icon.createWithResource(
                appContext,
                R.drawable.web_browser
            )

            progressEndIcon = Icon.createWithResource(
                appContext,
                R.drawable.deployed_code_update_server
            )

            progressTrackerIcon = Icon.createWithResource(
                appContext,
                R.drawable.delivery_truck_me
            )
        }

        val notification = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Update Ready")
            .setContentText("Tap to install version $version")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(progressStyle)
            .setVisibility(Notification.VISIBILITY_PUBLIC)  // ADDED: Show on lock screen
            .setCategory(Notification.CATEGORY_STATUS)      // ADDED: Status category for completion
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    // ============ Legacy Implementation (Pre-Android 16) ============
    private fun showDownloadStartingLegacy(version: String, fileSize: String) {
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading Update")
            .setContentText("Version $version • $fileSize")
            .setProgress(100, 0, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // CHANGED: From LOW to HIGH
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // ADDED: For lock screen
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)   // ADDED: Proper category
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateDownloadProgressLegacy(progress: Int, version: String) {
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading Update")
            .setContentText("Version $version • $progress%")
            .setProgress(100, progress, false)
            .setOngoing(progress < 100)
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // CHANGED: From LOW to HIGH
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // ADDED: For lock screen
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)   // ADDED: Proper category
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showDownloadCompleteLegacy(version: String, filePath: String) {
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                androidx.core.content.FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.FileProvider",
                    java.io.File(filePath)
                ),
                "application/vnd.android.package-archive"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            installIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Update Ready")
            .setContentText("Tap to install version $version")
            .setProgress(0, 0, false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // ADDED: For lock screen
            .setCategory(NotificationCompat.CATEGORY_STATUS)      // ADDED: Status category
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}