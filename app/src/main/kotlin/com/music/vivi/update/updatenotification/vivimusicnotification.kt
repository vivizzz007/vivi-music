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
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit


class UpdateChecker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos/vivizzz007/vivi-music/releases/latest"
        private const val CHANNEL_ID = "update_channel"
        private const val NOTIFICATION_ID = 1001
        private const val WORK_NAME = "update_checker_work"

        /**
         * Schedule periodic update checks based on user preference
         * @param context Application context
         * @param intervalHours How often to check for updates (in hours)
         */
        fun scheduleUpdateCheck(context: Context, intervalHours: Long = DEFAULT_UPDATE_INTERVAL.toLong()) {
            // Check if automatic updates are enabled
            if (!isAutoUpdateEnabled(context)) {
                // Cancel any existing work
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val updateCheckRequest = PeriodicWorkRequestBuilder<UpdateChecker>(
                intervalHours, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInitialDelay(15, TimeUnit.MINUTES) // Initial delay before first check
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE, // Update the existing work with new interval
                updateCheckRequest
            )
        }

        /**
         * Cancel all scheduled update checks
         */
        fun cancelUpdateChecks(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Check for updates immediately (one-time check)
         */
        fun checkForUpdateNow(context: Context) {
            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<UpdateChecker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Double-check if updates are still enabled
            if (!isAutoUpdateEnabled(context)) {
                return@withContext Result.success()
            }

            val latestVersion = fetchLatestVersion()
            val currentVersion = getCurrentVersion()

            if (isNewerVersion(currentVersion, latestVersion.version)) {
                // Only show notification if user has enabled notifications
                if (isUpdateNotificationsEnabled(context)) {
                    showUpdateNotification(latestVersion)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Retry on network errors, but don't retry indefinitely
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun fetchLatestVersion(): ReleaseInfo {
        val url = URL(GITHUB_API_URL)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "Vivi-Music-Updater")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)

                val tagName = jsonObject.getString("tag_name")
                val releaseName = jsonObject.optString("name", "")
                val releaseBody = jsonObject.optString("body", "")
                val assets = jsonObject.getJSONArray("assets")

                var downloadUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    if (name.equals("vivi.apk", ignoreCase = true)) {
                        downloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }

                return ReleaseInfo(
                    version = tagName.removePrefix("v"),
                    downloadUrl = downloadUrl.ifEmpty {
                        "https://github.com/vivizzz007/vivi-music/releases/latest"
                    },
                    releaseName = releaseName,
                    releaseNotes = releaseBody
                )
            } else {
                throw Exception("Failed to fetch release info: $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "0.0.0"
        } catch (e: Exception) {
            "0.0.0"
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.replace("[^0-9.]".toRegex(), "").split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.replace("[^0-9.]".toRegex(), "").split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(currentParts.size, latestParts.size)

        for (i in 0 until maxLength) {
            val currentPart = currentParts.getOrElse(i) { 0 }
            val latestPart = latestParts.getOrElse(i) { 0 }

            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }

        return false
    }

    private fun showUpdateNotification(releaseInfo: ReleaseInfo) {
        createNotificationChannel()

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseInfo.downloadUrl))
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTitle = "Update Available 🎵"
        val notificationText = "Vivi Music ${releaseInfo.version} is ready to download"
        val bigText = buildString {
            append("A new version (${releaseInfo.version}) of Vivi Music is available.\n\n")
            if (releaseInfo.releaseName.isNotEmpty()) {
                append("${releaseInfo.releaseName}\n\n")
            }
            append("Tap to download and install the latest version.")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bigText)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Updates"
            val descriptionText = "Notifications for new app updates"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    data class ReleaseInfo(
        val version: String,
        val downloadUrl: String,
        val releaseName: String = "",
        val releaseNotes: String = ""
    )
}

