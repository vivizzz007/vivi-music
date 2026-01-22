package com.music.vivi.playback.managers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.music.vivi.constants.DiscordTokenKey
import com.music.vivi.constants.DiscordUseDetailsKey
import com.music.vivi.constants.EnableDiscordRPCKey
import com.music.vivi.constants.EnableLastFMScrobblingKey
import com.music.vivi.constants.LastFMUseNowPlaying
import com.music.vivi.constants.PowerSaverKey
import com.music.vivi.constants.ScrobbleDelayPercentKey
import com.music.vivi.constants.ScrobbleDelaySecondsKey
import com.music.vivi.constants.ScrobbleMinSongDurationKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.models.MediaMetadata
import com.music.vivi.utils.DiscordRPC
import com.music.vivi.utils.ScrobbleManager
import com.music.vivi.utils.get
import com.music.vivi.extensions.metadata
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.extensions.currentMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Fallback object for missing constants if needed, usually imported
object LastFM {
    const val DEFAULT_SCROBBLE_DELAY_PERCENT = 50
    const val DEFAULT_SCROBBLE_MIN_SONG_DURATION = 30L
    const val DEFAULT_SCROBBLE_DELAY_SECONDS = 240
}

class IntegrationManager @Inject constructor(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val database: MusicDatabase,
) : Player.Listener {

    private var scope: CoroutineScope? = null
    private var player: ExoPlayer? = null
    private var discordRpc: DiscordRPC? = null
    private var scrobbleManager: ScrobbleManager? = null
    private var discordUpdateJob: Job? = null
    private var lastPlaybackSpeed = 1.0f

    private val currentMediaMetadata = MutableStateFlow<MediaMetadata?>(null)
    
    private var currentSongFlow: kotlinx.coroutines.flow.StateFlow<com.music.vivi.db.entities.Song?>? = null

    fun start(serviceScope: CoroutineScope, exoPlayer: ExoPlayer) {
        scope = serviceScope
        player = exoPlayer
        
        // Initialize currentSong flow
        currentSongFlow = currentMediaMetadata
            .flatMapLatest { mediaMetadata ->
                database.song(mediaMetadata?.id)
            }.stateIn(scope!!, SharingStarted.Lazily, null)

        setupDiscord(exoPlayer)
        setupScrobble(exoPlayer)
        
        // Initial metadata
        currentMediaMetadata.value = exoPlayer.currentMetadata
        
        exoPlayer.addListener(this)
    }
    
    fun stop(exoPlayer: ExoPlayer) {
        exoPlayer.removeListener(this)
        discordRpc?.closeRPC()
        scrobbleManager?.destroy()
        discordUpdateJob?.cancel()
        player = null
        scope = null
    }
    
    private fun setupDiscord(player: ExoPlayer) {
        scope?.launch {
            dataStore.data
                .map { Triple(it[DiscordTokenKey], it[EnableDiscordRPCKey] ?: true, it[PowerSaverKey] ?: false) }
                .debounce(300)
                .distinctUntilChanged()
                .collect { (key, enabled, powerSaver) ->
                    if (discordRpc?.isRpcRunning() == true) {
                        discordRpc?.closeRPC()
                    }
                    discordRpc = null
                    if (key != null && enabled && !powerSaver) {
                        discordRpc = DiscordRPC(context, key)
                        if (player.playbackState == Player.STATE_READY && player.playWhenReady) {
                            currentSongFlow?.value?.let {
                                discordRpc?.updateSong(it, player.currentPosition, player.playbackParameters.speed, dataStore.get(DiscordUseDetailsKey, false))
                            }
                        }
                    }
                }
        }

        scope?.launch {
            dataStore.data
                .map { it[DiscordUseDetailsKey] ?: false }
                .debounce(1000)
                .distinctUntilChanged()
                .collect { useDetails ->
                    if (player.playbackState == Player.STATE_READY && player.playWhenReady) {
                        currentSongFlow?.value?.let { song ->
                            discordUpdateJob?.cancel()
                            discordUpdateJob = scope?.launch {
                                delay(1000)
                                discordRpc?.updateSong(song, player.currentPosition, player.playbackParameters.speed, useDetails)
                            }
                        }
                    }
                }
        }
        
        // Observer for song changes to update Discord
        scope?.launch {
            currentSongFlow?.debounce(1000)?.collect { song ->
                if (song != null && player.playWhenReady && player.playbackState == Player.STATE_READY) {
                    discordRpc?.updateSong(song, player.currentPosition, player.playbackParameters.speed, dataStore.get(DiscordUseDetailsKey, false))
                } else {
                    discordRpc?.closeRPC()
                }
            }
        }
    }

    private fun setupScrobble(player: ExoPlayer) {
         scope?.launch {
             dataStore.data
                .map { (it[EnableLastFMScrobblingKey] ?: false) to (it[PowerSaverKey] ?: false) }
                .debounce(300)
                .distinctUntilChanged()
                .collect { (enabled, powerSaver) ->
                    val shouldEnable = enabled && !powerSaver
                    if (shouldEnable && scrobbleManager == null) {
                        val delayPercent = dataStore.get(ScrobbleDelayPercentKey, LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT)
                        val minSongDuration = dataStore.get(ScrobbleMinSongDurationKey, LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION)
                        val delaySeconds = dataStore.get(ScrobbleDelaySecondsKey, LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS)
                        scrobbleManager = ScrobbleManager(
                            scope!!,
                            minSongDuration = minSongDuration.toInt(), // FIX: Cast to Int
                            scrobbleDelayPercent = delayPercent.toFloat(), // FIX: Cast to Float
                            scrobbleDelaySeconds = delaySeconds
                        )
                        scrobbleManager?.useNowPlaying = dataStore.get(LastFMUseNowPlaying, false)
                    } else if (!shouldEnable && scrobbleManager != null) {
                        scrobbleManager?.destroy()
                        scrobbleManager = null
                    }
                }
         }
         
         scope?.launch {
            dataStore.data
                .map { it[LastFMUseNowPlaying] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    scrobbleManager?.useNowPlaying = it
                }
         }
         
         scope?.launch {
            dataStore.data
                .map { prefs ->
                    Triple(
                        prefs[ScrobbleDelayPercentKey] ?: LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT,
                        prefs[ScrobbleMinSongDurationKey] ?: LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION,
                        prefs[ScrobbleDelaySecondsKey] ?: LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS
                    )
                }
                .distinctUntilChanged()
                .collect { (delayPercent, minSongDuration, delaySeconds) ->
                    scrobbleManager?.let {
                        it.scrobbleDelayPercent = delayPercent.toFloat() // FIX: Cast to Float
                        it.minSongDuration = minSongDuration.toInt() // FIX: Cast to Int
                        it.scrobbleDelaySeconds = delaySeconds
                    }
                }
         }
    }

    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
        lastPlaybackSpeed = -1.0f // force update song
        discordUpdateJob?.cancel()
        
        // FIX: Removed unnecessary .toMediaItem() call. 
        // Using extension property .metadata directly on the ExoPlayer MediaItem.
        currentMediaMetadata.value = mediaItem?.metadata
    }
    
    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
            scrobbleManager?.onSongStop()
        }
    }
    
    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_MEDIA_ITEM_TRANSITION)) {
             currentMediaMetadata.value = player.currentMetadata
        }

        // Discord RPC updates
        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
            if (player.isPlaying) {
                currentSongFlow?.value?.let { song ->
                    scope?.launch {
                        discordRpc?.updateSong(song, player.currentPosition, player.playbackParameters.speed, dataStore.get(DiscordUseDetailsKey, false))
                    }
                }
            } else if (!events.containsAny(Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_MEDIA_ITEM_TRANSITION)){
                scope?.launch {
                    discordRpc?.stopActivity()
                }
            }
        }

        // Scrobbling
        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
             scrobbleManager?.onPlayerStateChanged(player.isPlaying, player.currentMetadata, duration = player.duration)
        }
    }
    
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        if (playbackParameters.speed != lastPlaybackSpeed) {
            lastPlaybackSpeed = playbackParameters.speed
            discordUpdateJob?.cancel()

            // update scheduling thingy
            discordUpdateJob = scope?.launch {
                delay(1000)
                if (player?.playWhenReady == true && player?.playbackState == Player.STATE_READY) {
                    currentSongFlow?.value?.let { song ->
                         discordRpc?.updateSong(song, player?.currentPosition ?: 0L, playbackParameters.speed, dataStore.get(DiscordUseDetailsKey, false))
                    }
                }
            }
        }
    }
}
