package com.music.vivi.playback

import android.R.attr.delay
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.Timeline
import com.music.vivi.db.MusicDatabase
import com.music.vivi.extensions.currentMetadata
import com.music.vivi.extensions.getCurrentQueueIndex
import com.music.vivi.extensions.getQueueWindows
import com.music.vivi.extensions.metadata
import com.music.vivi.playback.MusicService.MusicBinder
import com.music.vivi.playback.queues.Queue
import com.music.vivi.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerConnection(
    context: Context,
    binder: MusicBinder,
    val database: MusicDatabase,
    scope: CoroutineScope,
) : Player.Listener {
    val service = binder.service
    val player = service.player

    val playbackState = MutableStateFlow(player.playbackState)
    private val playWhenReady = MutableStateFlow(player.playWhenReady)
    val isPlaying =
        combine(playbackState, playWhenReady) { playbackState, playWhenReady ->
            playWhenReady && playbackState != STATE_ENDED
        }.stateIn(
            scope,
            SharingStarted.Lazily,
            player.playWhenReady && player.playbackState != STATE_ENDED
        )
    val mediaMetadata = MutableStateFlow(player.currentMetadata)
    val currentSong =
        mediaMetadata.flatMapLatest {
            database.song(it?.id)
        }
    val currentLyrics = mediaMetadata.flatMapLatest { mediaMetadata ->
        database.lyrics(mediaMetadata?.id)
    }
    val currentFormat =
        mediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }

    val queueTitle = MutableStateFlow<String?>(null)
    val queueWindows = MutableStateFlow<List<Timeline.Window>>(emptyList())
    val currentMediaItemIndex = MutableStateFlow(-1)
    val currentWindowIndex = MutableStateFlow(-1)

    val shuffleModeEnabled = MutableStateFlow(false)
    val repeatMode = MutableStateFlow(REPEAT_MODE_OFF)

    val canSkipPrevious = MutableStateFlow(true)
    val canSkipNext = MutableStateFlow(true)

    val error = MutableStateFlow<PlaybackException?>(null)

    // CROSSFADE ADDITIONS:

    // 1. Expose crossfade state to UI
    val isPerformingCrossfade = MutableStateFlow(false)
    val crossfadeDuration = MutableStateFlow(3000) // Default 3 seconds

    // 2. Track crossfade progress for UI animations
    val crossfadeProgress = MutableStateFlow(0f) // 0f to 1f during crossfade

    // 3. Add volume state for UI feedback during crossfade
    val currentVolume = MutableStateFlow(1f)

    init {
        player.addListener(this)

        playbackState.value = player.playbackState
        playWhenReady.value = player.playWhenReady
        mediaMetadata.value = player.currentMetadata
        queueTitle.value = service.queueTitle
        queueWindows.value = player.getQueueWindows()
        currentWindowIndex.value = player.getCurrentQueueIndex()
        currentMediaItemIndex.value = player.currentMediaItemIndex
        shuffleModeEnabled.value = player.shuffleModeEnabled
        repeatMode.value = player.repeatMode

        // Initialize crossfade states
        updateCrossfadeStates()
    }


    fun playQueue(queue: Queue) {
        service.playQueue(queue)
    }

    fun playNext(item: MediaItem) = playNext(listOf(item))

    fun playNext(items: List<MediaItem>) {
        service.playNext(items)
        // Note: Service handles crossfade restart internally
    }

    fun addToQueue(item: MediaItem) = addToQueue(listOf(item))

    fun addToQueue(items: List<MediaItem>) {
        service.addToQueue(items)
        // Note: Service handles crossfade restart internally
    }

    fun toggleLike() {
        service.toggleLike()
    }

    // 3. Enhanced seek methods that handle crossfade properly
    // 3. Enhanced seek methods that handle crossfade properly
    fun seekToNext() {
        // Cancel any ongoing crossfade before manual seek
        service.crossfadeJob?.cancel()
        service.isPerformingCrossfade = false

        player.seekToNext()

        // Reset crossfade states immediately
        isPerformingCrossfade.value = false
        crossfadeProgress.value = 0f
        currentVolume.value = service.playerVolume.value
    }

    fun seekToPrevious() {
        // Cancel any ongoing crossfade before manual seek
        service.crossfadeJob?.cancel()
        service.isPerformingCrossfade = false

        player.seekToPrevious()

        // Reset crossfade states immediately
        isPerformingCrossfade.value = false
        crossfadeProgress.value = 0f
        currentVolume.value = service.playerVolume.value
    }

    fun seekTo(positionMs: Long) {
        // Cancel crossfade on manual seek within track
        if (service.isPerformingCrossfade) {
            service.crossfadeJob?.cancel()
            service.isPerformingCrossfade = false
            isPerformingCrossfade.value = false
            crossfadeProgress.value = 0f
        }

        player.seekTo(positionMs)
    }

    fun seekTo(mediaItemIndex: Int, positionMs: Long = 0L) {
        // Cancel crossfade on manual seek to different item
        service.crossfadeJob?.cancel()
        service.isPerformingCrossfade = false

        player.seekTo(mediaItemIndex, positionMs)

        // Reset states
        isPerformingCrossfade.value = false
        crossfadeProgress.value = 0f
        currentVolume.value = service.playerVolume.value
    }

    // 4. Crossfade control methods
    fun setCrossfadeDuration(durationMs: Int) {
        service.setCrossfadeDuration(durationMs)
        crossfadeDuration.value = durationMs

        // If crossfade is currently active and duration changed significantly, restart monitoring
        if (service.isPerformingCrossfade &&
            kotlin.math.abs(durationMs - crossfadeDuration.value) > 500) {
            service.crossfadeJob?.cancel()
            if (player.hasNextMediaItem() && player.isPlaying) {
                // Restart crossfade monitoring with new duration
                service.startCrossfadeMonitoring()
            }
        }
    }

    fun getCrossfadeDuration(): Int {
        return crossfadeDuration.value
    }

    override fun onPlaybackStateChanged(state: Int) {
        playbackState.value = state
        error.value = player.playerError

        // Update crossfade states based on playback state
        when (state) {
            STATE_IDLE, STATE_ENDED -> {
                isPerformingCrossfade.value = false
                crossfadeProgress.value = 0f
                currentVolume.value = 1f
            }
            STATE_READY -> {
                // Update volume state when ready
                currentVolume.value = player.volume
            }
        }
    }

    override fun onPlayWhenReadyChanged(
        newPlayWhenReady: Boolean,
        reason: Int,
    ) {
        playWhenReady.value = newPlayWhenReady

        // Handle crossfade state when playback stops/starts
        if (!newPlayWhenReady) {
            // Paused - keep crossfade state but don't progress
            if (service.isPerformingCrossfade) {
                service.crossfadeJob?.cancel()
                service.isPerformingCrossfade = false
                isPerformingCrossfade.value = false
            }
            crossfadeProgress.value = 0f
        } else {
            // Started playing - restart crossfade monitoring if needed
            if (player.hasNextMediaItem() && crossfadeDuration.value > 0) {
                // Small delay to ensure playback is stable
                service.scope.launch {
                    delay(200)
                    if (player.isPlaying && player.hasNextMediaItem()) {
                        service.startCrossfadeMonitoring()
                    }
                }
            }
        }
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        mediaMetadata.value = mediaItem?.metadata
        currentMediaItemIndex.value = player.currentMediaItemIndex
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()

        // Handle crossfade state during transitions
        when (reason) {
            Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> {
                // Natural progression - update crossfade states
                updateCrossfadeStates()
                // Start monitoring for next crossfade if playing
                if (player.isPlaying && player.hasNextMediaItem() && crossfadeDuration.value > 0) {
                    service.scope.launch {
                        delay(100) // Brief delay for transition to stabilize
                        if (player.isPlaying && !service.isPerformingCrossfade) {
                            service.startCrossfadeMonitoring()
                        }
                    }
                }
            }
            Player.MEDIA_ITEM_TRANSITION_REASON_SEEK,
            Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> {
                // Manual operations - reset crossfade state
                isPerformingCrossfade.value = false
                crossfadeProgress.value = 0f
                currentVolume.value = service.playerVolume.value
            }
        }
    }


    override fun onTimelineChanged(
        timeline: Timeline,
        reason: Int,
    ) {
        queueWindows.value = player.getQueueWindows()
        queueTitle.value = service.queueTitle
        currentMediaItemIndex.value = player.currentMediaItemIndex
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onShuffleModeEnabledChanged(enabled: Boolean) {
        shuffleModeEnabled.value = enabled
        queueWindows.value = player.getQueueWindows()
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()

        // Reset crossfade state when shuffle changes queue order
        service.crossfadeJob?.cancel()
        service.isPerformingCrossfade = false
        isPerformingCrossfade.value = false
        crossfadeProgress.value = 0f
    }
    override fun onRepeatModeChanged(mode: Int) {
        repeatMode.value = mode
        updateCanSkipPreviousAndNext()

        // Handle crossfade based on repeat mode
        when (mode) {
            REPEAT_MODE_ONE -> {
                // Disable crossfade for single song repeat
                service.crossfadeJob?.cancel()
                service.isPerformingCrossfade = false
                isPerformingCrossfade.value = false
                crossfadeProgress.value = 0f
            }
            REPEAT_MODE_ALL, REPEAT_MODE_OFF -> {
                // Re-enable crossfade monitoring if playing
                if (player.isPlaying && player.hasNextMediaItem() && crossfadeDuration.value > 0) {
                    service.scope.launch {
                        delay(100)
                        if (!service.isPerformingCrossfade) {
                            service.startCrossfadeMonitoring()
                        }
                    }
                }
            }
        }
    }

    override fun onPlayerErrorChanged(playbackError: PlaybackException?) {
        if (playbackError != null) {
            reportException(playbackError)
            // Reset crossfade state on error
            service.crossfadeJob?.cancel()
            service.isPerformingCrossfade = false
            isPerformingCrossfade.value = false
            crossfadeProgress.value = 0f
            currentVolume.value = 1f
        }
        error.value = playbackError
    }

    // 6. Helper method to update crossfade states
    private fun updateCrossfadeStates() {
        // Sync crossfade state with service
        isPerformingCrossfade.value = service.isPerformingCrossfade

        // Update current volume
        currentVolume.value = player.volume

        // Calculate crossfade progress if active
        if (service.isPerformingCrossfade && player.duration > 0) {
            val timeRemaining = player.duration - player.currentPosition
            val crossfadeStart = crossfadeDuration.value

            if (timeRemaining <= crossfadeStart) {
                val progress = 1f - (timeRemaining.toFloat() / crossfadeStart.toFloat())
                crossfadeProgress.value = progress.coerceIn(0f, 1f)
            }
        } else if (!service.isPerformingCrossfade) {
            crossfadeProgress.value = 0f
        }
    }

    private fun updateCanSkipPreviousAndNext() {
        if (!player.currentTimeline.isEmpty) {
            val window =
                player.currentTimeline.getWindow(player.currentMediaItemIndex, Timeline.Window())
            canSkipPrevious.value = player.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) ||
                    !window.isLive ||
                    player.isCommandAvailable(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            canSkipNext.value = window.isLive &&
                    window.isDynamic ||
                    player.isCommandAvailable(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        } else {
            canSkipPrevious.value = false
            canSkipNext.value = false
        }
    }

    fun dispose() {
        player.removeListener(this)
    }
}
