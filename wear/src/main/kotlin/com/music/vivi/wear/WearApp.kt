/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import com.music.innertube.YouTube
import com.music.vivi.db.MusicDatabase
import com.music.vivi.wear.auth.WearAuthManager
import com.music.vivi.wear.auth.WearAuthPreferences
import com.music.vivi.wear.auth.WearAuthUtils
import com.music.vivi.wear.playback.WearAudioRouter
import com.music.vivi.wear.playback.WearDownloadCache
import com.music.vivi.wear.playback.WearDownloadManager
import com.music.vivi.wear.playback.WearDownloadService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Main application class for the standalone Wear OS 5 Vivi Music client.
 *
 * Initializes persistent database, caching singletons, Bluetooth audio routing,
 * background download coordinators, and YouTube Music auth observers.
 */
class WearApp : Application() {

    lateinit var database: MusicDatabase
        private set

    lateinit var databaseProvider: DatabaseProvider
        private set

    lateinit var audioRouter: WearAudioRouter
        private set

    lateinit var downloadManager: WearDownloadManager
        private set

    val authPreferences: WearAuthPreferences by lazy { WearAuthPreferences(this) }
    val authManager: WearAuthManager by lazy { WearAuthManager(this, authPreferences) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.i("WearApp initializing on Wear OS 5 / API %d", android.os.Build.VERSION.SDK_INT)

        // 1. Initialize persistent Room database from :core-database
        database = MusicDatabase.newInstance(this)

        // 2. Initialize Media3 standalone database provider
        databaseProvider = StandaloneDatabaseProvider(this)

        // 3. Initialize offline & playback SimpleCache instances
        WearDownloadCache.initialize(this, databaseProvider)

        // 4. Initialize Bluetooth audio routing enforcement
        audioRouter = WearAudioRouter(this)
        audioRouter.registerAudioDeviceCallback()

        // 5. Initialize background offline download manager
        downloadManager = WearDownloadManager(
            context = this,
            database = database,
            databaseProvider = databaseProvider,
            downloadCache = WearDownloadCache.downloadCache,
            upstreamDataSourceFactory = WearDownloadCache.getDownloadUpstreamDataSourceFactory(this),
        )

        // 6. Observe & restore YouTube credentials from DataStore
        val authScope = CoroutineScope(Dispatchers.IO)
        authScope.launch {
            authPreferences.innerTubeCookie.distinctUntilChanged().collect { cookie ->
                YouTube.cookie = cookie
                Timber.d("YouTube.cookie updated from DataStore")
            }
        }
        authScope.launch {
            authPreferences.dataSyncId.distinctUntilChanged().collect { dataSyncId ->
                YouTube.dataSyncId = WearAuthUtils.normalizeDataSyncId(dataSyncId)
                Timber.d("YouTube.dataSyncId updated from DataStore")
            }
        }
        authScope.launch {
            authPreferences.visitorData.distinctUntilChanged().collect { visitorData ->
                YouTube.visitorData = visitorData
                Timber.d("YouTube.visitorData updated from DataStore")
            }
        }
        authScope.launch {
            authManager.initFromDataStore()
        }

        // 7. Ensure system notification channels
        createNotificationChannels()
    }

    override fun onTerminate() {
        super.onTerminate()
        audioRouter.unregisterAudioDeviceCallback()
        downloadManager.release()
        WearDownloadCache.release()
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // Download progress channel
        val downloadChannel = NotificationChannel(
            WearDownloadService.CHANNEL_ID,
            getString(R.string.download_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.download_channel_description)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(downloadChannel)

        // Playback channel
        val playbackChannel = NotificationChannel(
            PLAYBACK_CHANNEL_ID,
            getString(R.string.playback_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.playback_channel_description)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(playbackChannel)
    }

    companion object {
        const val PLAYBACK_CHANNEL_ID = "wear_playback"

        lateinit var instance: WearApp
            private set
    }
}
