package com.music.vivi.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import androidx.core.app.NotificationCompat
import com.music.vivi.MainActivity
import com.music.vivi.R
/**
 * Dummy service that does nothing but post a notification
 */
class KeepAlive : Service() {
    private lateinit var notification: Notification
    private val binder = DummyBinder()
    inner class DummyBinder : Binder() {
        val service: KeepAlive
            get() = this@KeepAlive
    }
    override fun onCreate() {
        super.onCreate()
        notification = getNotification()
        createNotificationChannel()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, notification)
        return START_STICKY
    }
    override fun onBind(intent: Intent?) = binder
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                KEEP_ALIVE_CHANNEL_ID,
                "ot_keep_alive",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
    private fun getNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, KEEP_ALIVE_CHANNEL_ID)
            .setContentTitle("Keep alive")
            .setSmallIcon(R.drawable.music_library_ic_icon)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setAutoCancel(false)
            .setShowWhen(false)
            .setLocalOnly(true)
            .setOngoing(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }
    companion object {
        const val KEEP_ALIVE_CHANNEL_ID = "vivi_keep_alive"
    }
}