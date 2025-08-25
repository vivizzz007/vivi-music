package com.music.vivi.update.notificationupdate

import com.music.vivi.BuildConfig
import com.music.vivi.MainActivity
import com.music.vivi.R


import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.os.Build

import androidx.core.app.NotificationCompat

import java.net.URL
import android.app.NotificationChannel

import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import org.json.JSONObject
import java.net.HttpURLConnection
import android.net.Uri
import android.widget.Toast

import java.util.concurrent.TimeUnit


class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_OPEN_APP -> handleOpenApp(context)
            ACTION_REMIND_LATER -> handleRemindLater(context)
            ACTION_DISMISS -> handleDismiss(context)
            ACTION_UPDATE_NOW -> handleUpdateNow(context, intent.getStringExtra(EXTRA_VERSION) ?: "")
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                schedulePeriodicUpdateCheck(context)
                checkForUpdatesImmediately(context)
            }
            ACTION_CHECK_UPDATES -> checkForUpdatesImmediately(context)
        }
    }

    private fun handleOpenApp(context: Context) {
        Log.d(TAG, "Open App clicked")
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(mainIntent)
        cancelNotification(context)
    }

    private fun handleRemindLater(context: Context) {
        Log.d(TAG, "Remind Later clicked")
        cancelNotification(context)
        scheduleDelayedCheck(context, getRemindLaterDelay(context))
    }

    private fun handleDismiss(context: Context) {
        Log.d(TAG, "Dismiss clicked")
        cancelNotification(context)
    }

    private fun handleUpdateNow(context: Context, version: String) {
        Log.d(TAG, "Update Now clicked for version: $version")
        cancelNotification(context)
        openDownloadPage(context, version)
    }

    private fun openDownloadPage(context: Context, version: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://github.com/vivizzz007/vivi-music/releases/tag/v$version")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open download page", e)
            Toast.makeText(context, "Failed to open download page", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun schedulePeriodicUpdateCheck(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val intervalHours = prefs.getInt(KEY_UPDATE_CHECK_INTERVAL, DEFAULT_UPDATE_CHECK_INTERVAL).toLong()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false)
            .build()

        val updateCheckWork = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            intervalHours, TimeUnit.HOURS
        ).setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WORKER_BACKOFF_DELAY,
                TimeUnit.MINUTES
            ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "periodic_update_check",
            ExistingPeriodicWorkPolicy.REPLACE,
            updateCheckWork
        )
    }

    private fun scheduleDelayedCheck(context: Context, delayMillis: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val checkWork = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(checkWork)
    }

    private fun checkForUpdatesImmediately(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val checkWork = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(checkWork)
    }

    private fun getRemindLaterDelay(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val intervalHours = prefs.getInt(KEY_UPDATE_CHECK_INTERVAL, DEFAULT_UPDATE_CHECK_INTERVAL)
        return intervalHours * 60 * 60 * 1000L // Convert hours to milliseconds
    }
//
    companion object {
        private const val TAG = "UpdateNotification"
        private const val PREFS_NAME = "app_settings"
        private const val KEY_UPDATE_CHECK_INTERVAL = "update_check_interval"
        private const val DEFAULT_UPDATE_CHECK_INTERVAL = 4 // hours
        private const val WORKER_BACKOFF_DELAY = 5L // 5 minutes

        const val ACTION_OPEN_APP = "open_app"
        const val ACTION_REMIND_LATER = "remind_later"
        const val ACTION_DISMISS = "dismiss"
        const val ACTION_UPDATE_NOW = "update_now"
        const val ACTION_CHECK_UPDATES = "check_updates"
        const val NOTIFICATION_CHANNEL_ID = "vivi_music_updates"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_VERSION = "extra_version"

        fun checkForUpdatesOnStartup(context: Context) {
            val checkIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_CHECK_UPDATES
            }
            context.sendBroadcast(checkIntent)
        }

        fun scheduleInitialCheck(context: Context) {
            NotificationActionReceiver().schedulePeriodicUpdateCheck(context)
            checkForUpdatesOnStartup(context)
        }
    }
}


class UpdateCheckWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            val latestRelease = checkGitHubForUpdates()
            latestRelease?.let { release ->
                showUpdateNotification(
                    context = applicationContext,
                    version = release.version,
                    releaseNotes = release.releaseNotes,
                    downloadUrl = release.downloadUrl
                )
                Result.success()
            } ?: Result.success().also {
                Log.d(TAG, "No new version available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            Result.retry()
        }
    }

    private suspend fun checkGitHubForUpdates(): GitHubRelease? {
        return try {
            val url = URL("https://api.github.com/repos/vivizzz007/vivi-music/releases/latest")
            val connection = withContext(Dispatchers.IO) {
                url.openConnection() as HttpURLConnection
            }

            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val release = JSONObject(json)
                val tag = release.getString("tag_name").removePrefix("v")
                val releaseNotes = release.optString("body", "")
                val downloadUrl = release.getJSONArray("assets")
                    .optJSONObject(0)
                    ?.optString("browser_download_url", "")

                if (isNewerVersion(tag, BuildConfig.VERSION_NAME)) {
                    GitHubRelease(tag, releaseNotes, downloadUrl)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "GitHub API call failed", e)
            null
        }
    }

    private fun showUpdateNotification(context: Context, version: String, releaseNotes: String?, downloadUrl: String?) {
        createNotificationChannel(context)

        // Create app icon bitmap for large notification icon
        val largeIcon = createAppIconBitmap(context)

        // Create a more informative notification with expandable content
        val notificationStyle = NotificationCompat.BigTextStyle()
            .bigText("Version $version is now available! ${releaseNotes?.take(200) ?: "Bug fixes and performance improvements."}...")
            .setBigContentTitle("🎵 New Vivi Music Update")
            .setSummaryText("Tap to update")

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("🎵 Vivi Music Update Available")
            .setContentText("Version $version - Tap to see what's new")
            .setSmallIcon(R.drawable.ic_vivi_notification)
            .setLargeIcon(largeIcon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setStyle(notificationStyle)
            .setColor(ContextCompat.getColor(context, R.color.purple_500))
            .setContentIntent(createContentIntent(context))
            .addAction(
                R.drawable.ic_download,
                "Update Now",
                createUpdateIntent(context, version)
            )
            .addAction(
                R.drawable.ic_snooze,
                "Remind Later",
                createActionIntent(context, NotificationActionReceiver.ACTION_REMIND_LATER, 1)
            )
            .addAction(
                R.drawable.ic_close,
                "Dismiss",
                createDismissIntent(context)
            )
            .setGroup("vivi_updates_group")
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun createAppIconBitmap(context: Context): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable?.setBounds(0, 0, canvas.width, canvas.height)
            drawable?.draw(canvas)
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create app icon bitmap", e)
            null
        }
    }

    private fun createContentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("show_update_dialog", true)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createActionIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createUpdateIntent(context: Context, version: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_UPDATE_NOW
            putExtra(NotificationActionReceiver.EXTRA_VERSION, version)
        }
        return PendingIntent.getBroadcast(
            context,
            2,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createDismissIntent(context: Context): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
        }
        return PendingIntent.getBroadcast(
            context,
            3,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
        val latestParts = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val latest = latestParts.getOrElse(i) { 0 }
            val current = currentParts.getOrElse(i) { 0 }
            when {
                latest > current -> return true
                latest < current -> return false
            }
        }
        return false
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Vivi Music Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about new Vivi Music versions"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    data class GitHubRelease(
        val version: String,
        val releaseNotes: String,
        val downloadUrl: String?
    )

    companion object {
        private const val TAG = "UpdateCheckWorker"
        const val NOTIFICATION_CHANNEL_ID = "vivi_music_updates"
        const val NOTIFICATION_ID = 1001
    }
}

