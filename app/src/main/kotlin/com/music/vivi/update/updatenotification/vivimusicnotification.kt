package com.music.vivi.update.updatenotification


import android.Manifest
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
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat


class UpdateCheckerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val CHANNEL_ID = "app_updates"
        private const val NOTIFICATION_ID = 1001
        private const val GITHUB_API_URL = "https://api.github.com/repos/vivizzz007/vivi-music/releases/latest"
        private const val PREF_NAME = "update_prefs"
        private const val KEY_LAST_VERSION = "last_notified_version"

        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWork = PeriodicWorkRequestBuilder<UpdateCheckerWorker>(
                4, TimeUnit.HOURS // Check every 6 hours
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "update_checker",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )
        }

        fun checkNow(context: Context) {
            android.util.Log.d("UpdateChecker", "Manual check triggered")
            val workRequest = OneTimeWorkRequestBuilder<UpdateCheckerWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }

        // Force check and show notification even if already notified (for testing)
        fun forceCheckNow(context: Context) {
            android.util.Log.d("UpdateChecker", "Force check triggered - clearing last version")
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_LAST_VERSION)
                .apply()
            checkNow(context)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("UpdateChecker", "Starting update check...")
            val releaseInfo = fetchLatestRelease()
            android.util.Log.d("UpdateChecker", "Release info: $releaseInfo")

            if (releaseInfo != null && isNewVersion(releaseInfo.version)) {
                android.util.Log.d("UpdateChecker", "New version found: ${releaseInfo.version}")
                createNotificationChannel()
                showUpdateNotification(releaseInfo)
                saveLastVersion(releaseInfo.version)
            } else {
                android.util.Log.d("UpdateChecker", "No new version or already notified")
            }
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("UpdateChecker", "Error checking for updates", e)
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun fetchLatestRelease(): ReleaseInfo? {
        val url = URL(GITHUB_API_URL)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                return parseReleaseInfo(response)
            }
        } finally {
            connection.disconnect()
        }
        return null
    }

    private fun parseReleaseInfo(json: String): ReleaseInfo? {
        try {
            val jsonObject = org.json.JSONObject(json)
            val tagName = jsonObject.optString("tag_name", "")
            val name = jsonObject.optString("name", "")
            val body = jsonObject.optString("body", "")
            val htmlUrl = jsonObject.optString("html_url", "")
            val publishedAt = jsonObject.optString("published_at", "")

            // Get download URL for APK
            val assets = jsonObject.optJSONArray("assets")
            var downloadUrl = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            return ReleaseInfo(
                version = tagName,
                name = name,
                description = body,
                url = htmlUrl,
                downloadUrl = downloadUrl,
                publishedAt = publishedAt
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun isNewVersion(newVersion: String): Boolean {
        if (newVersion.isEmpty()) return false

        val prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val lastVersion = prefs.getString(KEY_LAST_VERSION, "") ?: ""

        // Also check against current app version
        val currentVersion = try {
            applicationContext.packageManager
                .getPackageInfo(applicationContext.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }

        android.util.Log.d("UpdateChecker", "Version check - New: $newVersion, Last: $lastVersion, Current: $currentVersion")

        return newVersion != lastVersion &&
                newVersion != currentVersion &&
                compareVersions(newVersion, currentVersion) > 0
    }

    private fun compareVersions(v1: String, v2: String): Int {
        if (v2.isEmpty()) return 1

        val parts1 = v1.removePrefix("v").split(".", "-").mapNotNull { it.toIntOrNull() }
        val parts2 = v2.removePrefix("v").split(".", "-").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val p1 = parts1.getOrNull(i) ?: 0
            val p2 = parts2.getOrNull(i) ?: 0
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }

    private fun saveLastVersion(version: String) {
        applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_VERSION, version)
            .apply()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Updates"
            val descriptionText = "Notifications for new app updates"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showUpdateNotification(releaseInfo: ReleaseInfo) {
        android.util.Log.d("UpdateChecker", "Attempting to show notification...")

        // Check if user wants to see notifications
        val sharedPrefs = applicationContext.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        val showNotifications = sharedPrefs.getBoolean("show_update_notification", true)

        if (!showNotifications) {
            android.util.Log.d("UpdateChecker", "User disabled update notifications")
            return
        }

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            android.util.Log.d("UpdateChecker", "Notification permission: $hasPermission")

            if (!hasPermission) {
                // Permission not granted, skip notification
                return
            }
        }

        // Intent to open release page
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseInfo.url))
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Get app icon resource ID
        val iconResId = applicationContext.applicationInfo.icon

        // Build notification
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(iconResId)
            .setContentTitle("New Update Available!")
            .setContentText("Tap to view update")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Add download action if APK is available
        if (releaseInfo.downloadUrl.isNotEmpty()) {
            val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseInfo.downloadUrl))
            val downloadPendingIntent = PendingIntent.getActivity(
                applicationContext,
                1,
                downloadIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(
                android.R.drawable.stat_sys_download,
                "Download",
                downloadPendingIntent
            )
        }

        // Show notification
        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, builder.build())
            android.util.Log.d("UpdateChecker", "Notification shown successfully!")
        } catch (e: SecurityException) {
            android.util.Log.e("UpdateChecker", "SecurityException showing notification", e)
            e.printStackTrace()
        }
    }

    data class ReleaseInfo(
        val version: String,
        val name: String,
        val description: String,
        val url: String,
        val downloadUrl: String,
        val publishedAt: String
    )
}