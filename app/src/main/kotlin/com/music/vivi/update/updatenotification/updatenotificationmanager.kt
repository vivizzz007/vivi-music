package com.music.vivi.update.updatenotification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.TimeUnit
import android.content.BroadcastReceiver


class UpdateNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "vivi_updates"
        private const val CHANNEL_NAME = "App Updates"
        private const val NOTIFICATION_ID = 1001

        private const val GITHUB_REPO = "vivizzz007/vivi-music"

        private const val PREF_NAME = "update_checker"

        private const val PREF_LAST_VERSION = "last_checked_version"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new app updates"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showUpdateNotification(version: String, downloadUrl: String, releaseNotes: String) {
        // Check if we have permission to post notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, cannot show notification
                return
            }
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done) // Replace with your app icon
                .setContentTitle("VIVI Music Update Available")
                .setContentText("Version $version is now available")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Version $version is now available!\n\n$releaseNotes")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
                .addAction(
                    android.R.drawable.stat_sys_download,
                    "Download",
                    pendingIntent
                )
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Handle security exception if permission was revoked
            e.printStackTrace()
        }
    }

    fun schedulePeriodicUpdateCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val updateCheckRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            6, TimeUnit.HOURS // Check every 6 hours
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "update_checker",
            ExistingPeriodicWorkPolicy.KEEP,
            updateCheckRequest
        )
    }

    fun cancelPeriodicUpdateCheck() {
        WorkManager.getInstance(context).cancelUniqueWork("update_checker")
    }
}

// UpdateCheckWorker.kt
class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val latestRelease = checkForUpdates()
            if (latestRelease != null) {
                val currentVersion = getCurrentVersion()
                val latestVersion = latestRelease.getString("tag_name")

                // Check if this is a new version
                val prefs = applicationContext.getSharedPreferences(
                    "update_checker",
                    Context.MODE_PRIVATE
                )
                val lastCheckedVersion = prefs.getString("last_checked_version", "")

                if (latestVersion != lastCheckedVersion && isNewerVersion(currentVersion, latestVersion)) {
                    val downloadUrl = getDownloadUrl(latestRelease)
                    val releaseNotes = latestRelease.optString("body", "Check the release page for details")

                    withContext(Dispatchers.Main) {
                        UpdateNotificationManager(applicationContext).showUpdateNotification(
                            latestVersion,
                            downloadUrl,
                            releaseNotes.take(200) // Limit length
                        )
                    }

                    // Save the version we just notified about
                    prefs.edit().putString("last_checked_version", latestVersion).apply()
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun checkForUpdates(): JSONObject? {
        return try {
            val url = URL("https://api.github.com/repos/vivizzz007/vivi-music/releases/latest")
            val connection = url.openConnection()
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            val response = connection.getInputStream().bufferedReader().readText()
            JSONObject(response)
        } catch (e: Exception) {
            null
        }
    }

    private fun getCurrentVersion(): String {
        return try {
            val packageInfo = applicationContext.packageManager.getPackageInfo(
                applicationContext.packageName,
                0
            )
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun getDownloadUrl(release: JSONObject): String {
        return try {
            val assets = release.getJSONArray("assets")
            if (assets.length() > 0) {
                assets.getJSONObject(0).getString("browser_download_url")
            } else {
                release.getString("html_url")
            }
        } catch (e: Exception) {
            release.getString("html_url")
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentClean = current.removePrefix("v").split(".")
        val latestClean = latest.removePrefix("v").split(".")

        for (i in 0 until maxOf(currentClean.size, latestClean.size)) {
            val currentPart = currentClean.getOrNull(i)?.toIntOrNull() ?: 0
            val latestPart = latestClean.getOrNull(i)?.toIntOrNull() ?: 0

            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }
        return false
    }
}

