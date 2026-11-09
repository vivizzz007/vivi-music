/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import com.music.vivi.db.MusicDatabase
import com.music.vivi.extensions.currentMetadata
import com.music.vivi.extensions.getCurrentQueueIndex
import com.music.vivi.extensions.getQueueWindows
import com.music.vivi.extensions.metadata
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.MediaMetadata
import com.music.vivi.playback.MusicService.MusicBinder
import com.music.vivi.playback.queues.Queue
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.innertube.pages.RadioChip
import com.music.vivi.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerConnection(
    context: Context,
    binder: MusicBinder,
    val database: MusicDatabase,
    scope: CoroutineScope,
) : Player.Listener {
    private companion object {
        private const val TAG = "PlayerConnection"
        private const val PLAYER_INIT_TIMEOUT_MS = 5000L // 5 second timeout for player initialization
    }

    val service = binder.service
    private val playerReadinessFlow = service.isPlayerReady
    
    /**
     * Safe player accessor checks readiness & handles errors.
     * Should be used by all player access within this class.
     */
    private fun getPlayerSafe(): ExoPlayer {
        return try {
            if (!playerReadinessFlow.value) {
                Timber.tag(TAG).w("Player accessed before service initialization complete; returning best-effort reference")
            }
            service.player
        } catch (e: UninitializedPropertyAccessException) {
            Timber.tag(TAG).e(e, "Fatal: player property accessed but not initialized")
            throw IllegalStateException("MusicService.player not initialized; possible race condition in service startup", e)
        }
    }

    /**
     * Public accessor for player. Throws if player not ready.
     * Callers should check [isPlayerInitialized] before calling, or handle exceptions.
     */
    val player: ExoPlayer
        get() = getPlayerSafe()

    /** Tracks whether player initialization completed successfully */
    private val isPlayerInitialized = MutableStateFlow(service.isPlayerReady.value)

    val playbackState: MutableStateFlow<Int>
    private val playWhenReady: MutableStateFlow<Boolean>
    val isPlaying: kotlinx.coroutines.flow.StateFlow<Boolean>
    
    init {
        Timber.tag(TAG).d("PlayerConnection init: playerReady=${playerReadinessFlow.value}")
        
        // Initialize with player state or safe defaults if player not ready
        val initialState = try {
            val initialPlayer = getPlayerSafe()
            Triple(initialPlayer.playbackState, initialPlayer.playWhenReady, 
                   initialPlayer.playWhenReady && initialPlayer.playbackState != STATE_ENDED)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error during PlayerConnection initialization, using defaults")
            Triple(Player.STATE_IDLE, false, false)
        }
        
        playbackState = MutableStateFlow(initialState.first)
        playWhenReady = MutableStateFlow(initialState.second)
        isPlaying = combine(playbackState, playWhenReady, service.isCrossfadingFlow) { state, ready, isCrossfading ->
            if (isCrossfading) {
                ready
            } else {
                ready && state != STATE_ENDED
            }
        }.stateIn(
            scope,
            SharingStarted.Lazily,
            initialState.third
        )
        
        // Track service readiness changes in background.
        scope.launch {
            playerReadinessFlow.collect { ready ->
                isPlayerInitialized.value = ready
                if (ready) {
                    Timber.tag(TAG).d("Service player initialization detected by PlayerConnection")
                }
            }
        }
        
        Timber.tag(TAG).d("PlayerConnection state flows initialized successfully")
    }
    
    // Effective playing state, considers Cast when active
    val isEffectivelyPlaying = combine(
        isPlaying,
        service.castConnectionHandler?.isCasting ?: MutableStateFlow(false),
        service.castConnectionHandler?.castIsPlaying ?: MutableStateFlow(false)
    ) { localPlaying, isCasting, castPlaying ->
        if (isCasting) castPlaying else localPlaying
    }.stateIn(
        scope,
        SharingStarted.Lazily,
        isPlaying.value
    )
    
    val mediaMetadata = MutableStateFlow<MediaMetadata?>(try { player.currentMetadata } catch (_: Exception) { null })
    fun getNextMediaMetadata(playerInstance: Player = player): MediaMetadata? {
        return try {
            val nextIndex = playerInstance.nextMediaItemIndex
            if (nextIndex != androidx.media3.common.C.INDEX_UNSET && nextIndex < playerInstance.mediaItemCount) {
                playerInstance.getMediaItemAt(nextIndex).metadata
            } else null
        } catch (_: Exception) {
            null
        }
    }
    val nextMediaMetadata = MutableStateFlow<MediaMetadata?>(getNextMediaMetadata())
    val isCrossfading = service.isCrossfadingFlow
    val crossfadeOutgoingItem = service.crossfadeOutgoingItem
    val crossfadeIncomingItem = service.crossfadeIncomingItem
    val crossfadeProgress = service.crossfadeProgressFlow
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

    val radioChips = service.radioChips
    val queueTitle = MutableStateFlow<String?>(null)
    val queueWindows = MutableStateFlow<List<Timeline.Window>>(emptyList())
    val currentMediaItemIndex = MutableStateFlow(-1)
    val currentWindowIndex = MutableStateFlow(-1)

    val shuffleModeEnabled = MutableStateFlow(false)
    val repeatMode = MutableStateFlow(REPEAT_MODE_OFF)

    val canSkipPrevious = MutableStateFlow(true)
    val canSkipNext = MutableStateFlow(true)

    val error = MutableStateFlow<PlaybackException?>(null)
    val isMuted = service.isMuted

    val waitingForNetworkConnection = service.waitingForNetworkConnection
    
    // Callback to check if playback changes should be blocked (e.g., Listen Together guest)
    var shouldBlockPlaybackChanges: (() -> Boolean)? = null
    
    // Flag to allow internal sync operations to bypass blocking (set by ListenTogetherManager)
    @Volatile
    var allowInternalSync: Boolean = false

    var onSkipPrevious: (() -> Unit)? = null
    var onSkipNext: (() -> Unit)? = null

    private var attachedPlayer: Player? = null

    init {
        try {
            // Observe player changes (e.g. crossfade swap)
            scope.launch {
                service.playerFlow.collect { newPlayer ->
                    if (newPlayer != null) {
                        updateAttachedPlayer(newPlayer)
                    }
                }
            }

            // Keep metadata and queue in sync with authoritative service state
            scope.launch {
                service.currentMediaMetadata.collect { meta ->
                    if (meta != null && meta.id != mediaMetadata.value?.id) {
                        mediaMetadata.value = meta
                        val p = attachedPlayer ?: player
                        currentMediaItemIndex.value = p.currentMediaItemIndex
                        currentWindowIndex.value = p.getCurrentQueueIndex()
                        queueWindows.value = p.getQueueWindows()
                        nextMediaMetadata.value = getNextMediaMetadata(p)
                        updateCanSkipPreviousAndNext()
                        Timber.tag(TAG).d("currentMediaMetadata sync: updated to ${meta.title} (id=${meta.id})")
                    }
                }
            }
            
            // Initial setup if flow hasn't emitted yet but service is ready
            if (attachedPlayer == null && service.isPlayerReady.value) {
                try {
                    updateAttachedPlayer(player)
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Service marked ready but player attach deferred to flow observer")
                }
            }

            Timber.tag(TAG).d("PlayerConnection flow observer registered")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to initialize PlayerConnection listener or state")
            // Propagate the error so MainActivity can retry
            throw e
        }
    }

    private fun updateAttachedPlayer(newPlayer: Player) {
        attachedPlayer?.removeListener(this)
        attachedPlayer = newPlayer
        newPlayer.addListener(this)
        
        // Refresh all state from new player
        playbackState.value = newPlayer.playbackState
        playWhenReady.value = newPlayer.playWhenReady
        val meta = newPlayer.currentMetadata
        mediaMetadata.value = meta
        queueTitle.value = service.queueTitle
        queueWindows.value = newPlayer.getQueueWindows()
        currentWindowIndex.value = newPlayer.getCurrentQueueIndex()
        currentMediaItemIndex.value = newPlayer.currentMediaItemIndex
        shuffleModeEnabled.value = newPlayer.shuffleModeEnabled
        repeatMode.value = newPlayer.repeatMode
        nextMediaMetadata.value = getNextMediaMetadata(newPlayer)
        updateCanSkipPreviousAndNext()
        
        Timber.tag(TAG).d("updateAttachedPlayer: mediaMetadata updated to ${meta?.title} (id=${meta?.id})")
        Timber.tag(TAG).d("updateAttachedPlayer: queueIndex=${newPlayer.currentMediaItemIndex}, queueSize=${newPlayer.mediaItemCount}, canSkipNext=${canSkipNext.value}, canSkipPrevious=${canSkipPrevious.value}")
    }

    fun playQueue(queue: Queue) {
        // Block if Listen Together guest (unless internal sync)
        if (!allowInternalSync && shouldBlockPlaybackChanges?.invoke() == true) {
            Timber.tag(TAG).d("playQueue blocked - Listen Together guest")
            return
        }
        if (!playerReadinessFlow.value) {
            Timber.tag(TAG).w("playQueue called before player ready; delegating to service")
        }
        try {
            service.playQueue(queue)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in playQueue")
            throw e
        }
    }

    fun selectRadioChip(chip: RadioChip) {
        service.applyRadioChip(chip)
    }

    fun startRadioSeamlessly() {
        // Block if Listen Together guest
        if (shouldBlockPlaybackChanges?.invoke() == true) {
            Timber.tag("PlayerConnection").d("startRadioSeamlessly blocked - Listen Together guest")
            return
        }
        if (!playerReadinessFlow.value) {
            Timber.tag(TAG).w("startRadioSeamlessly called before player ready; delegating to service")
        }
        try {
            service.startRadioSeamlessly()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in startRadioSeamlessly")
            throw e
        }
    }

    fun playNext(item: MediaItem) = playNext(listOf(item))

    fun playNext(items: List<MediaItem>) {
        // Block if Listen Together guest (unless internal sync)
        if (!allowInternalSync && shouldBlockPlaybackChanges?.invoke() == true) {
            Timber.tag("PlayerConnection").d("playNext blocked - Listen Together guest")
            return
        }
        try {
            service.playNext(items)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in playNext")
            throw e
        }
    }

    fun addToQueue(item: MediaItem) = addToQueue(listOf(item))

    fun addToQueue(items: List<MediaItem>) {
        // Block if Listen Together guest (unless internal sync)
        if (!allowInternalSync && shouldBlockPlaybackChanges?.invoke() == true) {
            Timber.tag("PlayerConnection").d("addToQueue blocked - Listen Together guest")
            return
        }
        try {
            service.addToQueue(items)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in addToQueue")
            throw e
        }
    }

    fun toggleLike() {
        try {
            service.toggleLike()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in toggleLike")
        }
    }

    fun toggleMute() {
        service.toggleMute()
    }

    fun setMuted(muted: Boolean) {
        service.setMuted(muted)
    }

    fun toggleLibrary() {
        try {
            service.toggleLibrary()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in toggleLibrary")
        }
    }

    /**
     * Toggle play/pause - handles Cast when active
     */
    fun togglePlayPause() {
        // Block if Listen Together guest (unless internal sync)
        if (!allowInternalSync && shouldBlockPlaybackChanges?.invoke() == true) {
            Timber.tag(TAG).d("togglePlayPause blocked - Listen Together guest")
            return
        }
        try {
            val castHandler = service.castConnectionHandler
            if (castHandler?.isCasting?.value == true) {
                if (castHandler.castIsPlaying.value) {
                    castHandler.pause()
                } else {
                    castHandler.play()
                }
            } else {
                player.togglePlayPause()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in togglePlayPause")
        }
    }
    
    /**
     * Start playback - handles Cast when active
     */
    fun play() {
        // Block if Listen Together guest (unless internal sync)
        if (!allowInternalSync && shouldBlockPlaybackChanges?.invoke() == true) {
            Timber.tag(TAG).d("play blocked - Listen Together guest")
            return
        }
        try {
            val castHandler = service.castConnectionHandler
            if (castHandler?.isCasting?.value == true) {
                castHandler.play()
            } else {
                if (player.playbackState == Player.STATE_IDLE) {
                    player.prepare()
                }
                player.playWhenReady = true
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in play")
        }
    }
    
    /**
     * Pause playback - handles Cast when active
     */
    fun pause() {
        // Block if Listen Together guest (unless internal sync)
        if (!allowInternalSync && shouldBlockPlaybackChanges?.invoke() == true) {
            Timber.tag(TAG).d("pause blocked - Listen Together guest")
            return
        }
        try {
            val castHandler = service.castConnectionHandler
            if (castHandler?.isCasting?.value == true) {
                castHandler.pause()
            } else {
                player.playWhenReady = false
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in pause")
        }
    }

    /**
     * Seek to position - handles Cast when active
     */
    fun seekTo(position: Long) {
        // Block if Listen Together guest (unless internal sync)
        if (!allowInternalSync && shouldBlockPlaybackChanges?.invoke() == true) {
            Timber.tag(TAG).d("seekTo blocked - Listen Together guest")
            return
        }
        try {
            val castHandler = service.castConnectionHandler
            if (castHandler?.isCasting?.value == true) {
                castHandler.seekTo(position)
            } else {
                player.seekTo(position)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in seekTo")
        }
    }

    fun seekToNext() {
        // Block if Listen Together guest (unless internal sync)
        if (!allowInternalSync && shouldBlockPlaybackChanges?.invoke() == true) {
            Timber.tag(TAG).d("seekToNext blocked - Listen Together guest")
            return
        }
        try {
            // When casting, use Cast skip instead of local player
            val castHandler = service.castConnectionHandler
            if (castHandler?.isCasting?.value == true) {
                castHandler.skipToNext()
                return
            }
            // Try a crossfaded manual skip first (only engages if the user
            // enabled "Crossfade on manual skip"); fall back to an instant
            // seek otherwise.
            if (!service.manualSkipToNextWithCrossfade()) {
                service.cancelCrossfadeIfActive()
                val p = attachedPlayer ?: player
                p.seekToNext()
                if (p.playbackState == Player.STATE_IDLE || p.playbackState == Player.STATE_ENDED) {
                    p.prepare()
                }
                p.playWhenReady = true
                updateCanSkipPreviousAndNext()
            }
            onSkipNext?.invoke()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in seekToNext")
        }
    }

    var onRestartSong: (() -> Unit)? = null

    fun seekToPrevious() {
        // Block if Listen Together guest (unless internal sync)
        if (!allowInternalSync && shouldBlockPlaybackChanges?.invoke() == true) {
            Timber.tag(TAG).d("seekToPrevious blocked - Listen Together guest")
            return
        }
        try {
            // When casting, use Cast skip instead of local player
            val castHandler = service.castConnectionHandler
            if (castHandler?.isCasting?.value == true) {
                castHandler.skipToPrevious()
                return
            }

            val p = attachedPlayer ?: player
            // Logic to mimic standard seekToPrevious behavior but with explicit callbacks
            // If we are more than 3 seconds in, just restart the song
            if (p.currentPosition > 3000 || !p.hasPreviousMediaItem()) {
                p.seekTo(0)
                if (p.playbackState == Player.STATE_IDLE || p.playbackState == Player.STATE_ENDED) {
                    p.prepare()
                }
                p.playWhenReady = true
                updateCanSkipPreviousAndNext()
                onRestartSong?.invoke()
            } else {
                // Otherwise go to previous media item — try a crossfaded
                // manual skip first, falling back to an instant seek.
                if (!service.manualSkipToPreviousWithCrossfade()) {
                    service.cancelCrossfadeIfActive()
                    p.seekToPreviousMediaItem()
                    if (p.playbackState == Player.STATE_IDLE || p.playbackState == Player.STATE_ENDED) {
                        p.prepare()
                    }
                    p.playWhenReady = true
                    updateCanSkipPreviousAndNext()
                }
                onSkipPrevious?.invoke()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in seekToPrevious")
        }
    }

    fun seekToQueueItem(index: Int) {
        if (!service.manualSeekToIndexWithCrossfade(index)) {
            service.cancelCrossfadeIfActive()
            player.seekToDefaultPosition(index)
            player.playWhenReady = true
        }
    }

    override fun onPlaybackStateChanged(state: Int) {
        if (isCrossfading.value && state == Player.STATE_ENDED) {
            return
        }
        playbackState.value = state
        error.value = player.playerError
    }

    override fun onPlayWhenReadyChanged(
        newPlayWhenReady: Boolean,
        reason: Int,
    ) {
        if (isCrossfading.value && reason != Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
            return
        }
        playWhenReady.value = newPlayWhenReady
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        mediaMetadata.value = mediaItem?.metadata
        val p = attachedPlayer ?: player
        currentMediaItemIndex.value = p.currentMediaItemIndex
        currentWindowIndex.value = p.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
        // Auto-refresh radio filter chips for every newly active song
        if (mediaItem != null) {
            service.refreshChipsForCurrentSong()
        }
    }

    override fun onTimelineChanged(
        timeline: Timeline,
        reason: Int,
    ) {
        val p = attachedPlayer ?: player
        mediaMetadata.value = p.currentMetadata
        queueWindows.value = p.getQueueWindows()
        queueTitle.value = service.queueTitle
        currentMediaItemIndex.value = p.currentMediaItemIndex
        currentWindowIndex.value = p.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onShuffleModeEnabledChanged(enabled: Boolean) {
        val p = attachedPlayer ?: player
        shuffleModeEnabled.value = enabled
        queueWindows.value = p.getQueueWindows()
        currentWindowIndex.value = p.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onRepeatModeChanged(mode: Int) {
        repeatMode.value = mode
        updateCanSkipPreviousAndNext()
    }

    override fun onPlayerErrorChanged(playbackError: PlaybackException?) {
        if (playbackError != null) {
            reportException(playbackError)
        }
        error.value = playbackError
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_AVAILABLE_COMMANDS_CHANGED,
                Player.EVENT_TIMELINE_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_REPEAT_MODE_CHANGED,
                Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED
            )
        ) {
            updateCanSkipPreviousAndNext()
        }
    }

    private fun updateCanSkipPreviousAndNext() {
        val p = attachedPlayer ?: player
        nextMediaMetadata.value = getNextMediaMetadata(p)
        try {
            if (!p.currentTimeline.isEmpty && p.currentMediaItemIndex in 0 until p.currentTimeline.windowCount) {
                val window =
                    p.currentTimeline.getWindow(p.currentMediaItemIndex, Timeline.Window())
                canSkipPrevious.value = p.hasPreviousMediaItem() ||
                        p.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) ||
                        !window.isLive ||
                        p.isCommandAvailable(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                canSkipNext.value = p.hasNextMediaItem() ||
                        (window.isLive && window.isDynamic) ||
                        p.isCommandAvailable(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            } else {
                canSkipPrevious.value = p.hasPreviousMediaItem()
                canSkipNext.value = p.hasNextMediaItem()
            }
        } catch (_: Exception) {
            canSkipPrevious.value = p.hasPreviousMediaItem()
            canSkipNext.value = p.hasNextMediaItem()
        }
    }

    fun dispose() {
        try {
            attachedPlayer?.removeListener(this)
            attachedPlayer = null
            Timber.tag(TAG).d("PlayerConnection disposed successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error during PlayerConnection disposal")
        }
    }
}