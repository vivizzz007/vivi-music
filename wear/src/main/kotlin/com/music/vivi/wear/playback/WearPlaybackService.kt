/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.playback

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionResult
import com.music.vivi.wear.R
import com.music.vivi.wear.WearApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * Standalone Media3 Playback Service for Wear OS.
 *
 * Configured with:
 * - Unrestricted audio playback routing (Galaxy Watch built-in speaker and paired Bluetooth audio).
 * - Clean pause on audio becoming noisy or Bluetooth disconnect without permanently blocking playback.
 * - Layered CacheDataSource (offline downloads + player LRU cache + network streaming).
 * - Phone-free operation on Samsung Galaxy Watch 7 (Wear OS 5).
 */
class WearPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer
        private set

    internal val sessionCallback = object : MediaSession.Callback {
        @Deprecated("Deprecated in Java", ReplaceWith("super.onPlayerCommandRequest(session, controller, playerCommand)"))
        @Suppress("DEPRECATION")
        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int,
        ): Int {
            // Playback permitted through watch speaker or Bluetooth; delegate directly without interception
            return super.onPlayerCommandRequest(session, controller, playerCommand)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.i("WearPlaybackService onCreate")

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(createMediaSourceFactory())
            .setAudioAttributes(audioAttributes, true) // Handles audio focus internally
            .setHandleAudioBecomingNoisy(true) // Auto-pauses when Bluetooth/headset unplugs
            .setWakeMode(C.WAKE_MODE_NETWORK) // Keeps CPU alive while playing with watch screen dimmed
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Timber.e(error, "WearPlaybackService: Player playback exception: %s", error.message)
            }
        })

        // Listen for Bluetooth audio disconnection via WearAudioRouter
        WearApp.instance.audioRouter.onBluetoothDisconnected = {
            if (player.isPlaying) {
                Timber.i("WearPlaybackService: Pausing playback because Bluetooth audio disconnected")
                player.pause()
            }
        }

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    private fun createMediaSourceFactory(): MediaSource.Factory {
        val cacheDataSourceFactory = WearDownloadCache.getCacheDataSourceFactory(this)
        val resolvingFactory = ResolvingDataSource.Factory(cacheDataSourceFactory) { dataSpec ->
            val uri = dataSpec.uri
            val songId = dataSpec.key ?: uri.lastPathSegment ?: uri.toString()

            // 1. If song is cached in offline downloads or player cache, read straight from watch storage
            if (WearDownloadCache.isDownloaded(songId) || WearDownloadCache.isPlayerCached(songId)) {
                return@Factory dataSpec.buildUpon().setKey(songId).build()
            }

            // 2. If URI already points to direct HTTP/HTTPS stream, play directly
            if (uri.scheme == "http" || uri.scheme == "https") {
                return@Factory dataSpec
            }

            // 3. Resolve stream URL on-the-fly via :innertube
            val streamUrl = runBlocking(Dispatchers.IO) {
                WearStreamResolver.resolveStreamUrl(songId)
            } ?: throw IllegalStateException("Failed to resolve stream URL for $songId")

            dataSpec.withUri(streamUrl.toUri())
        }

        return DefaultMediaSourceFactory(this)
            .setDataSourceFactory(resolvingFactory)
    }

    private fun notifyBluetoothRequired() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                applicationContext,
                R.string.bluetooth_disconnected_warning,
                Toast.LENGTH_SHORT,
            ).show()
        }
        val broadcastIntent = Intent(ACTION_BLUETOOTH_AUDIO_REQUIRED).apply {
            setPackage(packageName)
        }
        sendBroadcast(broadcastIntent)
    }

    override fun onDestroy() {
        Timber.i("WearPlaybackService onDestroy")
        WearApp.instance.audioRouter.onBluetoothDisconnected = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_BLUETOOTH_AUDIO_REQUIRED = "com.music.vivi.wear.action.BLUETOOTH_AUDIO_REQUIRED"

        /**
         * Builds a [MediaItem] with metadata for playback.
         */
        fun buildMediaItem(
            songId: String,
            title: String,
            artist: String? = null,
            artworkUri: Uri? = null,
        ): MediaItem {
            return MediaItem.Builder()
                .setMediaId(songId)
                .setUri("vivi://song/$songId".toUri())
                .setCustomCacheKey(songId)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(artist)
                        .setArtworkUri(artworkUri)
                        .build(),
                )
                .build()
        }
    }
}
