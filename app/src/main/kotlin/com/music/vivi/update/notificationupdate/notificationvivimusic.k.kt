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
import android.app.AlarmManager
import android.os.Build

import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import android.app.NotificationChannel

import android.graphics.BitmapFactory
import android.graphics.Color

import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import com.music.vivi.update.notificationupdate.NotificationActionReceiver.Companion.ACTION_OPEN_APP
import com.music.vivi.update.notificationupdate.NotificationActionReceiver.Companion.ACTION_REMIND_LATER
import com.music.vivi.update.notificationupdate.NotificationActionReceiver.Companion.NOTIFICATION_CHANNEL_ID
import com.music.vivi.update.notificationupdate.NotificationActionReceiver.Companion.NOTIFICATION_ID

import java.util.concurrent.TimeUnit

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_OPEN_APP -> handleOpenApp(context)
            ACTION_REMIND_LATER -> handleRemindLater(context)
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

    companion object {
        private const val TAG = "UpdateNotification"
        private const val PREFS_NAME = "app_settings"
        private const val KEY_UPDATE_CHECK_INTERVAL = "update_check_interval"
        private const val DEFAULT_UPDATE_CHECK_INTERVAL = 4 // hours
        private const val WORKER_BACKOFF_DELAY = 5L // 5 minutes

        const val ACTION_OPEN_APP = "open_app"
        const val ACTION_REMIND_LATER = "remind_later"
        const val ACTION_CHECK_UPDATES = "check_updates"
        const val NOTIFICATION_CHANNEL_ID = "vivi_music_updates"
        const val NOTIFICATION_ID = 1001

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
            val latestVersion = checkGitHubForUpdates()
            latestVersion?.let { version ->
                showUpdateNotification(
                    context = applicationContext,
                    version = version
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

    private suspend fun checkGitHubForUpdates(): String? {
        val url = URL("https://api.github.com/repos/vivizzz007/vivi-music/releases")
        val json = withContext(Dispatchers.IO) {
            url.openStream().bufferedReader().use { it.readText() }
        }
        val releases = JSONArray(json)
        val currentVersion = BuildConfig.VERSION_NAME

        for (i in 0 until releases.length()) {
            val release = releases.getJSONObject(i)
            val tag = release.getString("tag_name").removePrefix("v")
            if (isNewerVersion(tag, currentVersion)) {
                return tag
            }
        }
        return null
    }

    private fun showUpdateNotification(context: Context, version: String) {
        createNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("New Vivi Music Update")
            .setContentText("Version $version is available!")
            .setSmallIcon(R.drawable.vivi) // Your app's notification icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createContentIntent(context))
            .addAction(
                R.drawable.vivi, // Your custom icon
                "Click here",
                createActionIntent(context, ACTION_OPEN_APP, 0)
            )
            .addAction(
                R.drawable.restore, // Your custom icon
                "Remind Later",
                createActionIntent(context, ACTION_REMIND_LATER, 1)
            )
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun createContentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
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
            PendingIntent.FLAG_IMMUTABLE
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

    companion object {
        private const val TAG = "UpdateCheckWorker"
    }
}