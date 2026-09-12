package com.music.vivi.vivimusic.release

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.music.vivi.MainActivity
import com.music.vivi.R
import com.music.vivi.constants.EnableNotificationsKey
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import timber.log.Timber
import java.net.URL

object NewReleaseNotificationHelper {
    private const val CHANNEL_ID = "new_releases"
    private const val TAG = "NewReleaseNotif"

    fun showReleaseNotification(
        context: Context,
        artistName: String,
        releaseTitle: String,
        releaseType: String,
        deepLinkUrl: String,
        thumbnailUrl: String? = null
    ) {
        val notificationsEnabled = context.dataStore.get(EnableNotificationsKey, true)
        if (!notificationsEnabled) return

        val nm = context.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.new_release_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.new_release_channel_desc)
            }
            nm.createNotificationChannel(channel)
        }

        // Parse deep link URI and create Intent for MainActivity
        val intent = Intent(Intent.ACTION_VIEW, deepLinkUrl.toUri(), context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val notificationId = deepLinkUrl.hashCode()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(context, notificationId, intent, flags)

        val title = "$artistName released a new $releaseType!"

        // Try to load the album/song artwork for the large icon
        val largeIcon: Bitmap? = thumbnailUrl?.let { url ->
            try {
                val connection = URL(url).openConnection().apply {
                    connectTimeout = 5_000
                    readTimeout = 5_000
                }
                BitmapFactory.decodeStream(connection.getInputStream())
            } catch (e: Exception) {
                Timber.tag(TAG).w("Failed to load thumbnail for notification: %s", e.message)
                null
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.vivimusicnotification)
            .setContentTitle(title)
            .setContentText(releaseTitle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .apply {
                if (largeIcon != null) {
                    setLargeIcon(largeIcon)
                    // BigPictureStyle shows the album art expanded in the notification shade
                    setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(largeIcon)
                            .bigLargeIcon(null as Bitmap?) // hides large icon when expanded
                    )
                }
            }
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}
