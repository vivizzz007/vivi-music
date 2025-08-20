package com.music.vivi.update

import android.app.Service
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Response
import retrofit2.http.GET
// 5. Notification Helper
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.music.vivi.R
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
// 7. Update Scheduler using WorkManager

import androidx.work.*
import java.util.concurrent.TimeUnit
// 8. Update Check Worker

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters



// 2. Data Classes
data class GitHubRelease(
    val tag_name: String,
    val name: String,
    val body: String,
    val assets: List<GitHubAsset>,
    val published_at: String
)

data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
    val size: Long
)

interface GitHubApiService {
    @GET("repos/vivizzz007/vivi-music/releases/latest")
    suspend fun getLatestRelease(): Response<GitHubRelease>
}



class UpdateCheckService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var apiService: GitHubApiService

    override fun onCreate() {
        super.onCreate()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(GitHubApiService::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        checkForUpdates()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkForUpdates() {
        serviceScope.launch {
            try {
                val response = apiService.getLatestRelease()
                if (response.isSuccessful) {
                    val release = response.body()
                    release?.let {
                        val currentVersion = getCurrentAppVersion()
                        val latestVersion = it.tag_name.replace("v", "")

                        if (isNewerVersion(currentVersion, latestVersion)) {
                            withContext(Dispatchers.Main) {
                                showUpdateNotification(it)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UpdateCheck", "Error checking for updates", e)
            } finally {
                stopSelf()
            }
        }
    }

    private fun getCurrentAppVersion(): String {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        try {
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }

            val maxLength = maxOf(currentParts.size, latestParts.size)

            for (i in 0 until maxLength) {
                val currentPart = currentParts.getOrElse(i) { 0 }
                val latestPart = latestParts.getOrElse(i) { 0 }

                when {
                    latestPart > currentPart -> return true
                    latestPart < currentPart -> return false
                }
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    private fun showUpdateNotification(release: GitHubRelease) {
        val notificationHelper = NotificationHelper(this)
        val apkAsset = release.assets.find { it.name.endsWith(".apk") }

        notificationHelper.showUpdateNotification(
            title = "Update Available",
            message = "Vivi Music ${release.tag_name} is now available!",
            downloadUrl = apkAsset?.browser_download_url ?: "",
            releaseNotes = release.body
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}



class NotificationHelper(private val context: Context) {
    companion object {
        private const val CHANNEL_ID = "update_notifications"
        private const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for app updates"
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showUpdateNotification(
        title: String,
        message: String,
        downloadUrl: String,
        releaseNotes: String
    ) {
        // Intent to open download URL
        val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
        val downloadPendingIntent = PendingIntent.getActivity(
            context,
            0,
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to open your app
        val openAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download) // Using system icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$message\n\nWhat's New:\n$releaseNotes"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(downloadPendingIntent)
            .addAction(android.R.drawable.stat_sys_download_done, "Download", downloadPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Open App", openAppPendingIntent)
            .build()

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                // Start update check service
                val serviceIntent = Intent(context, UpdateCheckService::class.java)
                context.startService(serviceIntent)

                // Schedule periodic checks
                UpdateScheduler.schedulePeriodicCheck(context)
            }
        }
    }
}



class UpdateScheduler {
    companion object {
        private const val WORK_NAME = "update_check_work"

        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val updateCheckRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                6, TimeUnit.HOURS // Check every 6 hours
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES) // Initial delay
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    updateCheckRequest
                )
        }

        fun cancelScheduledCheck(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}



class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val intent = Intent(applicationContext, UpdateCheckService::class.java)
            applicationContext.startService(intent)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

// 9. Usage in MainActivity
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Remove setContentView if you don't have activity_main layout
        // setContentView(R.layout.activity_main)

        // Request notification permission (Android 13+)
        requestNotificationPermission()

        // Start initial update check
        checkForUpdatesOnAppStart()

        // Schedule periodic checks
        UpdateScheduler.schedulePeriodicCheck(this)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private fun checkForUpdatesOnAppStart() {
        val intent = Intent(this, UpdateCheckService::class.java)
        startService(intent)
    }
}

// 10. Add to build.gradle (Module: app)
/*
dependencies {
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'androidx.work:work-runtime-ktx:2.8.1'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4'
}
*/