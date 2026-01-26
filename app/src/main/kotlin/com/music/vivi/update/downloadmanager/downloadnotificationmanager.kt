package com.music.vivi.update.downloadmanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toColorInt
import com.music.vivi.R

object DownloadNotificationManager {
    private lateinit var notificationManager: NotificationManager
    private lateinit var appContext: Context

    const val CHANNEL_ID = "download_progress_channel"
    private const val CHANNEL_NAME = "Download Progress" // Will be replaced with context.getString in initialize()
    private const val NOTIFICATION_ID = 5678

    fun initialize(context: Context) {
        appContext = context
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.download_progress_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.download_progress_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
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
            .setContentTitle(appContext.getString(R.string.update_failed))
            .setContentText(appContext.getString(R.string.failed_to_download_version, version))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(appContext.getString(R.string.failed_to_download_version_error, version, errorMessage))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
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
        val progressStyle = Notification.ProgressStyle()
            .also {
                // Create 4 segments alternating between primary and tertiary colors
                for (i in 0 until 4) {
                    it.addProgressSegment(
                        Notification.ProgressStyle.Segment(25)
                            .setColor(
                                if (i % 2 == 0) {
                                    "#4285F4".toColorInt() // Primary blue
                                } else {
                                    "#8E24AA".toColorInt() // Tertiary maroon/purple
                                }
                            )
                    )
                }
            }
            .setProgress(0)

        val builder = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Your app icon
            .setContentTitle(appContext.getString(R.string.downloading_update))
            .setContentText(appContext.getString(R.string.version_file_size, version, fileSize))
            .setOngoing(true)
            .setStyle(progressStyle)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setWhen(System.currentTimeMillis())

        // Use safe helpers for Android 16 features
        setRequestPromotedOngoingSafely(builder, true)
        setShortCriticalTextSafely(builder, appContext.getString(R.string.starting))

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun updateDownloadProgressModern(progress: Int, version: String) {
        val progressStyle = Notification.ProgressStyle()
            .also {
                // Create 4 segments alternating colors like train carriages
                for (i in 0 until 4) {
                    it.addProgressSegment(
                        Notification.ProgressStyle.Segment(25)
                            .setColor(
                                if (i % 2 == 0) {
                                    "#4285F4".toColorInt() // Primary blue
                                } else {
                                    "#8E24AA".toColorInt() // Tertiary maroon/purple
                                }
                            )
                    )
                }
            }
            .setProgress(progress)

        val builder = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Your app icon
            .setContentTitle(appContext.getString(R.string.downloading_update))
            .setContentText(appContext.getString(R.string.version_progress, version, progress))
            .setOngoing(progress < 100)
            .setStyle(progressStyle)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setWhen(System.currentTimeMillis())

        // Use safe helpers for Android 16 features
        setRequestPromotedOngoingSafely(builder, progress < 100)
        setShortCriticalTextSafely(builder, "$progress%")

        notificationManager.notify(NOTIFICATION_ID, builder.build())
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

        val progressStyle = Notification.ProgressStyle()
            .also {
                // All segments completed - alternating colors
                for (i in 0 until 4) {
                    it.addProgressSegment(
                        Notification.ProgressStyle.Segment(25)
                            .setColor(
                                if (i % 2 == 0) {
                                    "#4285F4".toColorInt() // Primary blue
                                } else {
                                    "#8E24AA".toColorInt() // Tertiary maroon/purple
                                }
                            )
                    )
                }
            }
            .setProgress(100)

        val builder = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.updated) // Checkmark icon when complete
            .setContentTitle(appContext.getString(R.string.update_ready))
            .setContentText(appContext.getString(R.string.tap_to_install_version, version))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(progressStyle)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_STATUS)
            .setWhen(System.currentTimeMillis())

        // Use safe helpers for Android 16 features
        setRequestPromotedOngoingSafely(builder, false)
        setShortCriticalTextSafely(builder, appContext.getString(R.string.done))

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun setShortCriticalTextSafely(builder: Notification.Builder, text: String) {
        try {
            val method = Notification.Builder::class.java.getMethod("setShortCriticalText", CharSequence::class.java)
            method.invoke(builder, text)
        } catch (e: Exception) {
            builder.getExtras().putCharSequence("android.shortCriticalText", text)
        }
    }

    private fun setRequestPromotedOngoingSafely(builder: Notification.Builder, promoted: Boolean) {
        // Fallback: set via extras first
        builder.getExtras().putBoolean("android.requestPromotedOngoing", promoted)

        try {
            // Try different possible method names from various previews
            val methodNames = arrayOf("setRequestPromotedOngoing", "setPromotedOngoing", "setOngoingActivity")
            for (name in methodNames) {
                try {
                    val method = Notification.Builder::class.java.getMethod(name, Boolean::class.javaPrimitiveType)
                    method.invoke(builder, promoted)
                    break
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {}
    }

    // ============ Legacy Implementation (Pre-Android 16) ============
    private fun showDownloadStartingLegacy(version: String, fileSize: String) {
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Your app icon
            .setContentTitle(appContext.getString(R.string.downloading_update))
            .setContentText(appContext.getString(R.string.version_file_size, version, fileSize))
            .setProgress(100, 0, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateDownloadProgressLegacy(progress: Int, version: String) {
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher) // Your app icon
            .setContentTitle(appContext.getString(R.string.downloading_update))
            .setContentText(appContext.getString(R.string.version_progress, version, progress))
            .setProgress(100, progress, false)
            .setOngoing(progress < 100)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
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
            .setSmallIcon(R.drawable.updated) // Checkmark icon when complete
            .setContentTitle(appContext.getString(R.string.update_ready))
            .setContentText(appContext.getString(R.string.tap_to_install_version, version))
            .setProgress(0, 0, false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
