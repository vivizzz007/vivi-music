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


//
//object UpdateNotificationManager {
//    private const val CHANNEL_ID = "app_update_channel"
//    private const val CHANNEL_NAME = "App Updates"
//    private const val NOTIFICATION_ID = 9999
//    private const val GITHUB_API_URL = "https://api.github.com/repos/vivizzz007/vivi-music/releases/latest"
//
//    fun initialize(context: Context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val importance = NotificationManager.IMPORTANCE_HIGH
//            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
//                description = "Notifications for app updates"
//            }
//            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//        }
//    }
//
//    suspend fun checkForUpdates(context: Context, currentVersionName: String, currentVersionCode: Int) {
//        withContext(Dispatchers.IO) {
//            try {
//                val latestRelease = fetchLatestRelease()
//                val latestVersionName = latestRelease.tagName.removePrefix("v").trim()
//                val currentVersion = currentVersionName.removePrefix("v").trim()
//
//                // Log for debugging
//                android.util.Log.d("UpdateChecker", "Current: $currentVersion (code: $currentVersionCode)")
//                android.util.Log.d("UpdateChecker", "Latest: $latestVersionName")
//
//                // Compare version strings semantically
//                if (isNewerVersion(latestVersionName, currentVersion)) {
//                    withContext(Dispatchers.Main) {
//                        showUpdateNotification(
//                            context,
//                            latestRelease.tagName,
//                            latestRelease.name,
//                            latestRelease.downloadUrl
//                        )
//                    }
//                } else {
//                    android.util.Log.d("UpdateChecker", "App is up to date")
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                android.util.Log.e("UpdateChecker", "Error checking updates", e)
//            }
//        }
//    }
//
//    private fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
//        try {
//            val latestParts = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }
//            val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
//
//            // Pad with zeros to make same length
//            val maxLength = maxOf(latestParts.size, currentParts.size)
//            val latest = latestParts + List(maxLength - latestParts.size) { 0 }
//            val current = currentParts + List(maxLength - currentParts.size) { 0 }
//
//            // Compare each part
//            for (i in 0 until maxLength) {
//                when {
//                    latest[i] > current[i] -> return true
//                    latest[i] < current[i] -> return false
//                }
//            }
//            return false // Versions are equal
//        } catch (e: Exception) {
//            e.printStackTrace()
//            return false
//        }
//    }
//
//    private fun fetchLatestRelease(): ReleaseInfo {
//        val url = URL(GITHUB_API_URL)
//        val connection = url.openConnection() as HttpURLConnection
//
//        try {
//            connection.requestMethod = "GET"
//            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
//            connection.connectTimeout = 10000
//            connection.readTimeout = 10000
//
//            val responseCode = connection.responseCode
//            if (responseCode == HttpURLConnection.HTTP_OK) {
//                val response = connection.inputStream.bufferedReader().use { it.readText() }
//                return parseReleaseInfo(response)
//            } else {
//                throw Exception("Failed to fetch release info: HTTP $responseCode")
//            }
//        } finally {
//            connection.disconnect()
//        }
//    }
//
//    private fun parseReleaseInfo(jsonResponse: String): ReleaseInfo {
//        val json = org.json.JSONObject(jsonResponse)
//        val tagName = json.getString("tag_name")
//        val name = json.getString("name")
//        val htmlUrl = json.getString("html_url")
//        val body = json.optString("body", "")
//
//        // Get first APK download URL from assets
//        val assets = json.getJSONArray("assets")
//        var downloadUrl = htmlUrl
//
//        for (i in 0 until assets.length()) {
//            val asset = assets.getJSONObject(i)
//            val assetName = asset.getString("name")
//            if (assetName.endsWith(".apk", ignoreCase = true)) {
//                downloadUrl = asset.getString("browser_download_url")
//                break
//            }
//        }
//
//        return ReleaseInfo(tagName, name, htmlUrl, downloadUrl, body)
//    }
//
//    private fun extractVersionCode(tagName: String): Int {
//        // Extract version code from tag name
//        // Assumes format like "v1.0.0" or "1.0.0"
//        val versionString = tagName.removePrefix("v").trim()
//        val parts = versionString.split(".")
//
//        return try {
//            when (parts.size) {
//                3 -> {
//                    // Convert semantic version to integer: major * 10000 + minor * 100 + patch
//                    val major = parts[0].toIntOrNull() ?: 0
//                    val minor = parts[1].toIntOrNull() ?: 0
//                    val patch = parts[2].toIntOrNull() ?: 0
//                    major * 10000 + minor * 100 + patch
//                }
//                2 -> {
//                    val major = parts[0].toIntOrNull() ?: 0
//                    val minor = parts[1].toIntOrNull() ?: 0
//                    major * 100 + minor
//                }
//                1 -> parts[0].toIntOrNull() ?: 0
//                else -> 0
//            }
//        } catch (e: Exception) {
//            0
//        }
//    }
//
//    private fun showUpdateNotification(
//        context: Context,
//        version: String,
//        releaseName: String,
//        downloadUrl: String
//    ) {
//        // Intent to open download page
//        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
//        val pendingIntent = PendingIntent.getActivity(
//            context,
//            0,
//            intent,
//            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
//        )
//
//        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setContentTitle("Update Available!")
//            .setContentText("Version $version is now available")
//            .setStyle(
//                NotificationCompat.BigTextStyle()
//                    .bigText("$releaseName\n\nTap to download the latest version of Vivi Music.")
//            )
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setAutoCancel(true)
//            .setContentIntent(pendingIntent)
//            .addAction(
//                R.drawable.ic_launcher_foreground,
//                "Download",
//                pendingIntent
//            )
//            .build()
//
//        with(NotificationManagerCompat.from(context)) {
//            notify(NOTIFICATION_ID, notification)
//        }
//    }
//
//    data class ReleaseInfo(
//        val tagName: String,
//        val name: String,
//        val htmlUrl: String,
//        val downloadUrl: String,
//        val body: String
//    )
//}

// ============ INTEGRATION INSTRUCTIONS ============

// 1. Add to AndroidManifest.xml (if not already present):
/*
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
*/

// 2. Add to your app-level build.gradle dependencies:
/*
dependencies {
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
}
*/

// 3. In MainActivity.onCreate(), add BEFORE setContent { ... }:
/*
// Initialize update notification channel
UpdateNotificationManager.initialize(this)

// Check for updates on app start
lifecycleScope.launch {
    val currentVersion = BuildConfig.VERSION_CODE
    UpdateNotificationManager.checkForUpdates(this@MainActivity, currentVersion)
}
*/

// 4. OPTIONAL - To check periodically (add inside onCreate after setContent):
/*
// Check for updates every 24 hours while app is open
lifecycleScope.launch {
    while (true) {
        delay(24 * 60 * 60 * 1000L) // 24 hours
        val currentVersion = BuildConfig.VERSION_CODE
        UpdateNotificationManager.checkForUpdates(this@MainActivity, currentVersion)
    }
}
*/