package com.music.vivi.update.downloadmanager

import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.music.vivi.R
import java.util.Locale

/**
 * A custom MediaNotification.Provider that delegates to DefaultMediaNotificationProvider
 * and adds Android 16 Status Chips (Live Updates) support.
 */
@OptIn(UnstableApi::class)
class ViviNotificationProvider(
    private val context: Context,
    notificationIdProvider: DefaultMediaNotificationProvider.NotificationIdProvider,
    channelId: String,
    channelNameResourceId: Int,
) : MediaNotification.Provider {

    private val defaultProvider = DefaultMediaNotificationProvider(
        context,
        notificationIdProvider,
        channelId,
        channelNameResourceId
    )

    /**
     * Set the small icon for the notification. This is used by MusicService.
     */
    fun setSmallIcon(iconResId: Int): ViviNotificationProvider {
        defaultProvider.setSmallIcon(iconResId)
        return this
    }

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback,
    ): MediaNotification {
        // Get the default notification built by Media3
        val mediaNotification = defaultProvider.createNotification(
            mediaSession,
            customLayout,
            actionFactory,
            onNotificationChangedCallback
        )

        // Android 16 Status Chips implementation (API 36 or "Baklava")
        val isAndroid16 = Build.VERSION.SDK_INT >= 36 || Build.VERSION.CODENAME == "Baklava"

        if (isAndroid16) {
            val player = mediaSession.player
            val isPlaying = player.playWhenReady && player.playbackState == Player.STATE_READY

            val durationMs = player.duration
            val currentPosMs = player.currentPosition

            // Format duration for the chip (e.g., "5:20")
            val formattedTime = if (durationMs != C.TIME_UNSET && durationMs > 0) {
                val totalSeconds = durationMs / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                String.Companion.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
            } else {
                null
            }

            // Use platform Builder to recover and modify the notification
            val notification = mediaNotification.notification
            val builder = Notification.Builder.recoverBuilder(context, notification)

            // Essential for Android 16 Status Chips (Live Updates)
            builder.setOngoing(true)
            builder.setCategory(Notification.CATEGORY_TRANSPORT)

            // Research suggests colorized should be false for promoted notifications in some cases
            // but for music it might be okay. Let's try false first for better promotion.
            builder.setColorized(false)

            // Ensure we have a small icon (required for chip)
            builder.setSmallIcon(R.drawable.vivi)

            // Promote to Live Update
            setRequestPromotedOngoingSafely(builder, true)

            // Fallback: also set via extras just in case reflection fails
            builder.getExtras().putBoolean("android.requestPromotedOngoing", true)

            if (isPlaying) {
                // Set the chip text (e.g., the track duration)
                setShortCriticalTextSafely(builder, formattedTime ?: context.getString(R.string.playing_status))

                if (durationMs != C.TIME_UNSET && durationMs > 0) {
                    // Set 'when' to the completion time of the track for a live countdown
                    val remainingMs = durationMs - currentPosMs
                    val endTime = System.currentTimeMillis() + remainingMs
                    builder.setWhen(endTime)
                    builder.setUsesChronometer(true)
                    setChronometerCountDownSafely(builder, true)
                } else {
                    builder.setWhen(System.currentTimeMillis())
                    builder.setShowWhen(true)
                }
            } else {
                // When paused, show "Paused" or static duration in the chip
                setShortCriticalTextSafely(builder, formattedTime ?: context.getString(R.string.paused_status))
                builder.setShowWhen(false)
                builder.setUsesChronometer(false)
            }

            // Re-build the notification
            val updatedNotification = builder.build()

            // Re-attach the media session token if it was lost during build()
            if (Build.VERSION.SDK_INT >= 33) {
                mediaNotification.notification.extras.getParcelable(
                    Notification.EXTRA_MEDIA_SESSION,
                    android.media.session.MediaSession.Token::class.java
                )?.let {
                    updatedNotification.extras.putParcelable(Notification.EXTRA_MEDIA_SESSION, it)
                }
            } else {
                @Suppress("DEPRECATION")
                mediaNotification.notification.extras.getParcelable<android.media.session.MediaSession.Token>(
                    Notification.EXTRA_MEDIA_SESSION
                )?.let {
                    updatedNotification.extras.putParcelable(Notification.EXTRA_MEDIA_SESSION, it)
                }
            }

            return MediaNotification(mediaNotification.notificationId, updatedNotification)
        }

        return mediaNotification
    }

    override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean =
        defaultProvider.handleCustomCommand(session, action, extras)

    private fun setShortCriticalTextSafely(builder: Notification.Builder, text: String) {
        try {
            val method = Notification.Builder::class.java.getMethod("setShortCriticalText", CharSequence::class.java)
            method.invoke(builder, text)
        } catch (e: Exception) {
            builder.getExtras().putCharSequence("android.shortCriticalText", text)
        }
    }

    private fun setChronometerCountDownSafely(builder: Notification.Builder, countDown: Boolean) {
        try {
            val method = Notification.Builder::class.java.getMethod(
                "setChronometerCountDown",
                Boolean::class.javaPrimitiveType
            )
            method.invoke(builder, countDown)
        } catch (e: Exception) {
            builder.getExtras().putBoolean("android.chronometerCountDown", countDown)
        }
    }

    private fun setRequestPromotedOngoingSafely(builder: Notification.Builder, promoted: Boolean) {
        try {
            // Try different possible method names from various previews
            val methodNames = arrayOf("setRequestPromotedOngoing", "setPromotedOngoing", "setOngoingActivity")
            var success = false
            for (name in methodNames) {
                try {
                    val method = Notification.Builder::class.java.getMethod(name, Boolean::class.javaPrimitiveType)
                    method.invoke(builder, promoted)
                    success = true
                    break
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {}
    }
}
