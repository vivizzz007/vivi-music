package com.music.vivi.update.updatenotification


import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.withTimeout
import com.music.vivi.R

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
        private const val WORK_TIMEOUT_MS = 9000L

        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWork = PeriodicWorkRequestBuilder<UpdateCheckerWorker>(
                4, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "update_checker",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )
        }

        fun checkNow(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<UpdateCheckerWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }

        fun forceCheckNow(context: Context) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_LAST_VERSION)
                .apply()
            checkNow(context)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            withTimeout(WORK_TIMEOUT_MS) {
                val releaseInfo = fetchLatestRelease()

                if (releaseInfo != null && isNewVersion(releaseInfo.version)) {
                    createNotificationChannel()
                    showUpdateNotification(releaseInfo)
                    saveLastVersion(releaseInfo.version)
                }
            }
            Result.success()
        } catch (e: TimeoutCancellationException) {
            Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun fetchLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_API_URL)
            connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseReleaseInfo(response)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseReleaseInfo(json: String): ReleaseInfo? {
        try {
            val jsonObject = org.json.JSONObject(json)
            val tagName = jsonObject.optString("tag_name", "")
            val name = jsonObject.optString("name", "")
            val body = jsonObject.optString("body", "")
            val htmlUrl = jsonObject.optString("html_url", "")
            val publishedAt = jsonObject.optString("published_at", "")

            val assets = jsonObject.optJSONArray("assets")
            var downloadUrl = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.equals("vivi.apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }

                if (downloadUrl.isEmpty()) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val assetName = asset.optString("name", "")
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
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
            return null
        }
    }

    private fun isNewVersion(newVersion: String): Boolean {
        if (newVersion.isEmpty()) return false

        val prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val lastVersion = prefs.getString(KEY_LAST_VERSION, "") ?: ""

        val currentVersion = try {
            applicationContext.packageManager
                .getPackageInfo(applicationContext.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }

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
        val sharedPrefs = applicationContext.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        val showNotifications = sharedPrefs.getBoolean("show_update_notification", true)

        if (!showNotifications) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) return
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseInfo.url))
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.library_music)
            .setContentTitle("New Update Available!")
            .setContentText("Version ${releaseInfo.version} is available")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Version ${releaseInfo.version}: ${releaseInfo.name}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

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
                "Download APK",
                downloadPendingIntent
            )
        }

        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            // Silently fail
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