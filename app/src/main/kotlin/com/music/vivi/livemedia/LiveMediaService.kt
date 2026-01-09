package com.music.vivi.livemedia

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.music.vivi.R
import com.music.vivi.constants.*
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LiveMediaService : NotificationListenerService() {
    private val TAG = "LiveMediaService"
    private lateinit var mediaStateManager: MediaStateManager
    private lateinit var lockScreenManager: LockScreenManager
    private lateinit var notificationUpdateScheduler: NotificationUpdateScheduler
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isQsOpen = false
    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")

        createNotificationChannel()

        lockScreenManager = LockScreenManager(
            this,
            deviceLocked = {
                Log.i(TAG, "Device Locked. Clear notification")
                clearNotification()
            },
            deviceUnlocked = {
                Log.i(TAG, "Device unlocked. Show notification")
                mediaStateManager.getUpdatedMusicState()?.let {
                    updateNotification(it)
                }
            })

        mediaStateManager = MediaStateManager(
            this,
            onStateUpdated = { state ->
                Log.i(TAG, "StateUpdated")
                updateNotification(state)
                notificationUpdateScheduler.scheduleUpdate(state)
            },
            noActiveMedia = {
                Log.i(TAG, "No audio. Disable notification")
                clearNotification()
            })

        notificationUpdateScheduler =
            NotificationUpdateScheduler {
                Log.i(TAG, "Time to check new state")
                mediaStateManager.getUpdatedMusicState()?.let {
                    updateNotification(it)
                }
            }

        serviceScope.launch {
            QSStateProvider.isQsOpen.collectLatest { isOpen ->
                val wasOpen = isQsOpen
                isQsOpen = isOpen
                val hideOnQs = dataStore.get(LiveMediaHideOnQsOpenKey, true)
                if (isOpen) {
                    if (hideOnQs) {
                        clearNotification()
                    }
                } else if (wasOpen) {
                    mediaStateManager.getUpdatedMusicState()?.let {
                        updateNotification(it)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn?.notification?.category == Notification.CATEGORY_TRANSPORT) {
            Log.i(TAG, "Media notification detected from: ${sbn.packageName}")
            mediaStateManager.maybeUpdateMediaController()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mediaStateManager.handleTransportControl(intent?.action)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateNotification(musicState: MusicState) {
        val enabled = dataStore.get(LiveMediaEnabledKey, false)
        if (!enabled) {
            clearNotification()
            return
        }

        val hideOnQs = dataStore.get(LiveMediaHideOnQsOpenKey, true)
        if (!lockScreenManager.isScreenUnlocked() || (isQsOpen && hideOnQs)) {
            clearNotification()
            return
        }

        val notification = buildNotification(musicState)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(musicState: MusicState): Notification {
        var contentIntent: PendingIntent? = null
        val launchIntent = packageManager.getLaunchIntentForPackage(musicState.packageName)

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            contentIntent = PendingIntent.getActivity(
                this, 0,
                launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val showTimestamp = dataStore.get(LiveMediaShowTimestampKey, false)
        val showAlbumArt = dataStore.get(LiveMediaShowAlbumArtKey, true)
        val showProgress = dataStore.get(LiveMediaShowProgressKey, true)
        val showArtistName = dataStore.get(LiveMediaShowArtistNameKey, true)
        val showAlbumName = dataStore.get(LiveMediaShowAlbumNameKey, true)
        val showActionButtons = dataStore.get(LiveMediaShowActionButtonsKey, true)
        val pillContent = dataStore.get(LiveMediaPillContentKey, "TITLE")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(getAppIcon())
            .setContentTitle(musicState.title)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShortCriticalText(providePillText(
                musicState.title,
                musicState.position.toInt(),
                musicState.duration.toInt(),
                musicState.isPlaying,
                pillContent
            ))
            .setRequestPromotedOngoing(true)
            .setShowWhen(false)
            .setStyle(buildBaseBigTextStyle())
            .setSubText(
                combineProviderAndTimestamp(
                    showTimestamp,
                    musicState.position.toInt(),
                    musicState.duration.toInt()
                )
            )

        if (showAlbumArt) notification.setLargeIcon(musicState.albumArt)

        if (showProgress) {
            notification.setProgress(
                musicState.duration.toInt(),
                musicState.position.toInt(),
                false
            )
        }

        if (showArtistName || showAlbumName) {
            notification.setContentText(
                buildArtisAlbumTitle(
                    showArtistName,
                    showAlbumName,
                    musicState
                )
            )
        }

        if (showActionButtons) {
            notification.addAction(prevMusicAction)
            notification.addAction(if (musicState.isPlaying) pauseMusicAction else playMusicAction)
            notification.addAction(nextMusicAction)
        }

        if (contentIntent != null) {
            notification.setContentIntent(contentIntent)
        }

        return notification.build()
    }

    private fun clearNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Media Live Updates", NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private val playMusicAction by lazy {
        createAction(
            android.R.drawable.ic_media_play,
            getString(R.string.play_action),
            MediaStateManager.ACTION_PLAY_PAUSE,
            MediaStateManager.REQUEST_CODE_PLAY_PAUSE,
            this,
            this::class.java
        )
    }

    private val pauseMusicAction by lazy {
        createAction(
            android.R.drawable.ic_media_pause,
            getString(R.string.pause_action),
            MediaStateManager.ACTION_PLAY_PAUSE,
            MediaStateManager.REQUEST_CODE_PLAY_PAUSE,
            this,
            this::class.java
        )
    }

    private val prevMusicAction by lazy {
        createAction(
            android.R.drawable.ic_media_previous,
            getString(R.string.previous_action),
            MediaStateManager.ACTION_SKIP_TO_PREVIOUS,
            MediaStateManager.REQUEST_CODE_PREVIOUS,
            this,
            this::class.java
        )
    }
    
    private val nextMusicAction by lazy {
        createAction(
            android.R.drawable.ic_media_next,
            getString(R.string.next_action),
            MediaStateManager.ACTION_SKIP_TO_NEXT,
            MediaStateManager.REQUEST_CODE_NEXT,
            this,
            this::class.java
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 1337
        private const val CHANNEL_ID = "MediaLiveUpdateChannel"
    }
}
