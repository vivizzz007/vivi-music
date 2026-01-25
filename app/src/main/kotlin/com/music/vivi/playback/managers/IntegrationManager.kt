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
import com.music.vivi.constants.PowerSaverDiscordKey
import com.music.vivi.constants.PowerSaverKey
import com.music.vivi.constants.PowerSaverLastFMKey
import com.music.vivi.constants.ScrobbleDelayPercentKey
import com.music.vivi.constants.ScrobbleDelaySecondsKey
import com.music.vivi.constants.ScrobbleMinSongDurationKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.extensions.currentMetadata
import com.music.vivi.extensions.metadata
import com.music.vivi.models.MediaMetadata
import com.music.vivi.utils.DiscordRPC
import com.music.vivi.utils.ScrobbleManager
import com.music.vivi.utils.get
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
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

/**
 * Manages external integrations and background observation tasks.
 *
 * Responsibilities include:
 * 1. **Discord RPC**: Updates Discord status with current song info (using [DiscordRPC]).
 * 2. **LastFM Scrobbling**: Reports "Now Playing" and "Scrobble" events to LastFM (using [ScrobbleManager]).
 * 3. **DataStore Observation**: Listens to user preferences (like RPC enabled, Scrobble settings) and rebuilds
 *    integration states dynamically.
 */
class IntegrationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val database: MusicDatabase,
) : Player.Listener {
    // ... (rest of the file remains unchanged)
    private var scope: CoroutineScope? = null
    private var player: ExoPlayer? = null
    private var discordRpc: DiscordRPC? = null
    private var scrobbleManager: ScrobbleManager? = null
    private var discordUpdateJob: Job? = null
    private var lastPlaybackSpeed = 1.0f

    private val currentMediaMetadata = MutableStateFlow<MediaMetadata?>(null)

    private var currentSongFlow: kotlinx.coroutines.flow.StateFlow<com.music.vivi.db.entities.Song?>? = null

    fun start(scope: CoroutineScope, player: ExoPlayer) {
        this.scope = scope
        this.player = player

        // Initialize currentSong flow
        currentSongFlow = currentMediaMetadata
            .flatMapLatest { mediaMetadata ->
                database.song(mediaMetadata?.id)
            }.stateIn(scope, SharingStarted.Lazily, null)

        setupDiscord(player)
        setupScrobble(player)
        setupSongUpdates(player)

        // Initial metadata
        currentMediaMetadata.value = player.currentMetadata

        player.addListener(this)
    }

    /**
     * Watches DataStore for Discord settings changes and initializes/cleans up [DiscordRPC].
     *
     * It observes not just the "Enabled" flag, but also the "Power Saver" mode, which can
     * override this feature to save battery.
     */
    private fun setupDiscord(player: ExoPlayer) {
        scope?.launch {
            dataStore.data
                .map {
                    Triple(
                        it[DiscordTokenKey],
                        it[EnableDiscordRPCKey] ?: true,
                        (it[PowerSaverKey] ?: false) && (it[PowerSaverDiscordKey] ?: true)
                    )
                }
                .debounce(300)
                .distinctUntilChanged()
                .collect { (key, enabled, powerSaver) ->
                    if (discordRpc?.isRpcRunning() == true) {
                        discordRpc?.closeRPC()
                    }
                    discordRpc = null
                    // If enabled is true AND powerSaver is false (i.e., not saving power OR specific Discord saver is off)
                    // Wait, logic: enabled=true. PowerSaver=true.
                    // effectivelyEnabled = enabled && !powerSaver
                    if (key != null && enabled && !powerSaver) {
                        discordRpc = DiscordRPC(context, key)
                        if (player.playbackState == Player.STATE_READY && player.playWhenReady) {
                            currentSongFlow?.value?.let {
                                discordRpc?.updateSong(
                                    it,
                                    player.currentPosition,
                                    player.playbackParameters.speed,
                                    dataStore.get(DiscordUseDetailsKey, false)
                                )
                            }
                        }
                    }
                }
        }
        // ...
    }

    private fun setupSongUpdates(player: ExoPlayer) {
        scope?.launch {
            currentSongFlow?.collectLatest { song ->
                if (song != null && player.isPlaying) {
                    discordRpc?.updateSong(
                        song,
                        player.currentPosition,
                        player.playbackParameters.speed,
                        dataStore.get(DiscordUseDetailsKey, false)
                    )
                }
            }
        }
    }

    private fun setupScrobble(player: ExoPlayer) {
        scope?.launch {
            dataStore.data
                .map {
                    (it[EnableLastFMScrobblingKey] ?: false) to
                        ((it[PowerSaverKey] ?: false) && (it[PowerSaverLastFMKey] ?: true))
                }
                .debounce(300)
                .distinctUntilChanged()
                .collect { (enabled, powerSaver) ->
                    val shouldEnable = enabled && !powerSaver
                    if (shouldEnable && scrobbleManager == null) {
                        // FIX: Explicitly cast default values to match the Key types (Float and Int)
                        val delayPercent = dataStore.get(
                            ScrobbleDelayPercentKey,
                            LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT.toFloat()
                        )
                        val minSongDuration = dataStore.get(
                            ScrobbleMinSongDurationKey,
                            LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION.toInt()
                        )
                        val delaySeconds = dataStore.get(ScrobbleDelaySecondsKey, LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS)
                        scrobbleManager = ScrobbleManager(
                            scope!!,
                            minSongDuration = minSongDuration,
                            scrobbleDelayPercent = delayPercent,
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
                    // FIX: Explicitly cast the constants to match preference key types
                    Triple(
                        prefs[ScrobbleDelayPercentKey] ?: LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT.toFloat(),
                        prefs[ScrobbleMinSongDurationKey] ?: LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION.toInt(),
                        prefs[ScrobbleDelaySecondsKey] ?: LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS
                    )
                }
                .distinctUntilChanged()
                .collect { (delayPercent, minSongDuration, delaySeconds) ->
                    scrobbleManager?.let {
                        it.scrobbleDelayPercent = delayPercent
                        it.minSongDuration = minSongDuration
                        it.scrobbleDelaySeconds = delaySeconds
                    }
                }
        }
    }

    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
        lastPlaybackSpeed = -1.0f // force update song
        discordUpdateJob?.cancel()
        currentMediaMetadata.value = mediaItem?.metadata
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
            scrobbleManager?.onSongStop()
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_TIMELINE_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY,
                Player.EVENT_MEDIA_ITEM_TRANSITION
            )
        ) {
            currentMediaMetadata.value = player.currentMetadata
        }

        // Discord RPC updates
        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
            if (player.isPlaying) {
                currentSongFlow?.value?.let { song ->
                    scope?.launch {
                        discordRpc?.updateSong(
                            song,
                            player.currentPosition,
                            player.playbackParameters.speed,
                            dataStore.get(DiscordUseDetailsKey, false)
                        )
                    }
                }
            } else if (!events.containsAny(Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_MEDIA_ITEM_TRANSITION)) {
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
                        discordRpc?.updateSong(
                            song,
                            player?.currentPosition ?: 0L,
                            playbackParameters.speed,
                            dataStore.get(DiscordUseDetailsKey, false)
                        )
                    }
                }
            }
        }
    }

    fun stop(player: ExoPlayer) {
        player.removeListener(this)
        discordRpc?.closeRPC()
        scrobbleManager?.destroy()
        discordUpdateJob?.cancel()
        scope = null
        this.player = null
    }
}
