/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.music.vivi.MainActivity
import com.music.vivi.R
import com.music.vivi.viewmodels.SpotifyImportViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber

class SpotifySyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var observerJob: Job? = null
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                Timber.i("SpotifySyncService cancelled by user")
                SpotifyImportViewModel.cancelActiveJobs()
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_STOP -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            else -> {
                startForegroundWithInitialNotification()
                observeProgress()
                return START_STICKY
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.spotify_sync),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.spotify_sync_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithInitialNotification() {
        val notification = buildNotification(
            title = getString(R.string.spotify_sync),
            content = getString(R.string.push_in_progress),
            progress = 0,
            indeterminate = true,
            isFinished = false
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun observeProgress() {
        observerJob?.cancel()
        observerJob = serviceScope.launch {
            combine(
                SpotifyImportViewModel.importProgressFlow,
                SpotifyImportViewModel.pushProgressFlow
            ) { importProg, pushProg ->
                Pair(importProg, pushProg)
            }.collect { (importProg, pushProg) ->
                val activeImport = importProg != null
                val activePush = pushProg != null

                if (!activeImport && !activePush) {
                    // Both idle or dismissed
                    stopForegroundService()
                    return@collect
                }

                if (importProg != null) {
                    val isFinished = importProg.isFinished
                    val percentInt = (importProg.percent * 100).toInt().coerceIn(0, 100)
                    val content = if (isFinished) {
                        getString(R.string.spotify_import_finished)
                    } else if (importProg.playlistName.isNotBlank()) {
                        "${importProg.playlistName} (${importProg.currentSongIndex}/${importProg.totalSongs})"
                    } else {
                        getString(R.string.spotify_import_in_progress)
                    }

                    val notification = buildNotification(
                        title = if (isFinished) getString(R.string.spotify_import_complete) else getString(R.string.spotify_import_in_progress),
                        content = content,
                        progress = percentInt,
                        indeterminate = !isFinished && percentInt == 0 && importProg.totalSongs == 0,
                        isFinished = isFinished
                    )
                    notificationManager.notify(NOTIFICATION_ID, notification)

                    if (isFinished) {
                        delay(2500)
                        if (SpotifyImportViewModel.importProgressFlow.value?.isFinished == true &&
                            SpotifyImportViewModel.pushProgressFlow.value == null) {
                            stopForegroundService()
                        }
                    }
                } else if (pushProg != null) {
                    val isFinished = pushProg.isFinished
                    val percentInt = (pushProg.percent * 100).toInt().coerceIn(0, 100)
                    val content = if (pushProg.status.isNotBlank()) {
                        pushProg.status
                    } else if (pushProg.playlistName.isNotBlank()) {
                        pushProg.playlistName
                    } else {
                        getString(R.string.push_to_spotify)
                    }

                    val notification = buildNotification(
                        title = getString(R.string.push_to_spotify),
                        content = content,
                        progress = percentInt,
                        indeterminate = !isFinished && percentInt == 0,
                        isFinished = isFinished
                    )
                    notificationManager.notify(NOTIFICATION_ID, notification)

                    if (isFinished) {
                        delay(2500)
                        if (SpotifyImportViewModel.pushProgressFlow.value?.isFinished == true &&
                            SpotifyImportViewModel.importProgressFlow.value == null) {
                            stopForegroundService()
                        }
                    }
                }
            }
        }
    }

    private fun buildNotification(
        title: String,
        content: String,
        progress: Int,
        indeterminate: Boolean,
        isFinished: Boolean
    ) = NotificationCompat.Builder(this, CHANNEL_ID).apply {
        setSmallIcon(R.drawable.cached)
        setContentTitle(title)
        setContentText(content)
        setOngoing(!isFinished)
        setOnlyAlertOnce(true)

        val openIntent = Intent(this@SpotifySyncService, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this@SpotifySyncService,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        setContentIntent(contentPendingIntent)

        if (!isFinished) {
            setProgress(100, progress, indeterminate)
            val cancelIntent = Intent(this@SpotifySyncService, SpotifySyncService::class.java).apply {
                action = ACTION_CANCEL
            }
            val cancelPendingIntent = PendingIntent.getService(
                this@SpotifySyncService,
                1,
                cancelIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            addAction(R.drawable.close, getString(android.R.string.cancel), cancelPendingIntent)
        } else {
            setAutoCancel(true)
        }
    }.build()

    private fun stopForegroundService() {
        observerJob?.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "com.music.vivi.SPOTIFY_SYNC_CHANNEL"
        const val NOTIFICATION_ID = 2026

        const val ACTION_START = "com.music.vivi.action.SPOTIFY_SYNC_START"
        const val ACTION_STOP = "com.music.vivi.action.SPOTIFY_SYNC_STOP"
        const val ACTION_CANCEL = "com.music.vivi.action.SPOTIFY_SYNC_CANCEL"

        fun start(context: Context) {
            try {
                val intent = Intent(context, SpotifySyncService::class.java).apply {
                    action = ACTION_START
                }
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Timber.e(e, "Failed to start SpotifySyncService")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, SpotifySyncService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Timber.e(e, "Failed to stop SpotifySyncService")
            }
        }
    }
}
