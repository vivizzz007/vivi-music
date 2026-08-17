package com.music.vivi.desktop.player

import com.music.vivi.desktop.DesktopSettings
import com.music.vivi.desktop.GuestSession
import com.music.vivi.desktop.NowPlaying
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

enum class RepeatMode { OFF, ALL, ONE }

/** Loading state shown in the player while a track is being resolved/downloaded. */
enum class LoadPhase { NONE, RESOLVING, DOWNLOADING }

data class PlayerState(
    val queue: List<NowPlaying> = emptyList(),
    val index: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1f,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    /** Localization key shown when stream resolution fails. */
    val errorKey: String? = null,
    /** Human-readable technical detail for playback failures. */
    val errorDetail: String? = null,
    /** Current load phase (resolving / downloading / none). */
    val loadPhase: LoadPhase = LoadPhase.NONE,
) {
    val current: NowPlaying? get() = queue.getOrNull(index)
    val isLoading: Boolean get() = loadPhase != LoadPhase.NONE
}

/**
 * Owns the [AudioPlayer] and exposes UI-facing playback state, including a
 * full queue (add/remove/next/previous/skip/auto-advance), shuffle, repeat,
 * volume and seeking. The current track is resolved to an AAC stream and
 * played on a background coroutine.
 */
class PlayerController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val player = AudioPlayer()

    /** Whether to automatically play the next queued track when one ends. */
    @Volatile var autoPlayNext: Boolean = true

    private companion object {
        /** Total resolution/playback attempts before an error is surfaced. */
        const val MAX_PLAY_ATTEMPTS = 3
    }

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** User-initiated seeks (emitted so the sync layer can push them instantly). */
    private val _seekEvents = MutableSharedFlow<Long>(extraBufferCapacity = 16)
    val seekEvents: SharedFlow<Long> = _seekEvents.asSharedFlow()

    init {
        // Restore the saved shuffle/repeat state when "remember" is enabled.
        val s = DesktopSettings.load()
        if (s.rememberShuffleRepeat) {
            _state.value = PlayerState(
                isShuffle = s.isShuffle,
                repeatMode = repeatModeFromKey(s.repeatModeKey),
            )
        }
    }

    /** Monotonic token identifying the active play session. */
    private var playToken = 0

    /**
     * videoId currently loaded (or being resolved/loaded) in the [AudioPlayer].
     * A track restored from the persistent queue has no stream loaded, so
     * pressing play must trigger a real load instead of a no-op `resume()`.
     */
    @Volatile
    private var loadedVideoId: String? = null

    /** Back-navigation history used by "previous" in shuffle mode. */
    private val previousStack = ArrayDeque<Int>()

    fun play(track: NowPlaying) = playAt(listOf(track), 0)

    fun playAll(tracks: List<NowPlaying>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        playAt(tracks, startIndex.coerceIn(0, tracks.lastIndex))
    }

    /** Appends a track to the queue; if nothing is playing, starts it. */
    fun addToQueue(track: NowPlaying) {
        val s = _state.value
        if (s.current == null) {
            play(track)
        } else {
            _state.update { it.copy(queue = it.queue + track) }
        }
    }

    /** Appends a list of tracks to the queue; if nothing is playing, starts them. */
    fun addAllToQueue(tracks: List<NowPlaying>) {
        val s = _state.value
        if (s.current == null) {
            playAll(tracks)
        } else {
            _state.update { it.copy(queue = it.queue + tracks) }
        }
    }

    fun next() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        val nextIndex = when {
            s.queue.size == 1 -> 0
            s.isShuffle -> randomIndexExcluding(s.queue.size, s.index)
            s.index < s.queue.lastIndex -> s.index + 1
            s.repeatMode == RepeatMode.ALL -> 0
            else -> return
        }
        previousStack.addLast(s.index)
        playAt(s.queue, nextIndex)
    }

    fun previous() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        val prevIndex = when {
            s.isShuffle -> previousStack.removeLastOrNull() ?: randomIndexExcluding(s.queue.size, s.index)
            s.index > 0 -> s.index - 1
            s.repeatMode == RepeatMode.ALL -> s.queue.lastIndex
            else -> return
        }
        playAt(s.queue, prevIndex)
    }

    fun skipTo(index: Int) {
        val s = _state.value
        if (index in s.queue.indices) {
            previousStack.addLast(s.index)
            playAt(s.queue, index)
        }
    }

    fun removeAt(index: Int) {
        val s = _state.value
        if (index !in s.queue.indices) return
        val newQueue = s.queue.toMutableList().apply { removeAt(index) }
        when {
            newQueue.isEmpty() -> {
                playToken++
                player.stop()
                loadedVideoId = null
                _state.value = PlayerState(volume = s.volume, isShuffle = s.isShuffle, repeatMode = s.repeatMode)
            }
            index < s.index -> _state.update { it.copy(queue = newQueue, index = it.index - 1) }
            index == s.index -> playAt(newQueue, s.index.coerceAtMost(newQueue.lastIndex))
            else -> _state.update { it.copy(queue = newQueue) }
        }
    }

    fun clearQueue() {
        val s = _state.value
        playToken++
        player.stop()
        loadedVideoId = null
        _state.value = PlayerState(volume = s.volume, isShuffle = s.isShuffle, repeatMode = s.repeatMode)
    }

    /**
     * Applies a new ordering of the same queue items (drag-to-reorder),
     * keeping the currently playing track selected.
     */
    fun reorder(newQueue: List<NowPlaying>) {
        val s = _state.value
        if (newQueue.size != s.queue.size) return
        val currentId = s.current?.videoId
        val newIndex = newQueue.indexOfFirst { it.videoId == currentId }.takeIf { it != -1 } ?: s.index
        _state.update { it.copy(queue = newQueue, index = newIndex) }
    }

    fun toggle() {
        val s = _state.value
        if (s.current == null) return
        if (s.isPlaying) {
            player.pause()
            _state.update { it.copy(isPlaying = false) }
        } else {
            startCurrent(s)
        }
    }

    /**
     * Starts the current track. If its stream isn't loaded yet (e.g. it was
     * restored from the persistent queue, or a previous load failed), trigger a
     * real resolution + load instead of a no-op `resume()`.
     */
    private fun startCurrent(s: PlayerState) {
        if (loadedVideoId != s.current?.videoId) {
            playAt(s.queue, s.index, startAtMs = s.positionMs, startPaused = false)
        } else {
            player.resume()
            _state.update { it.copy(isPlaying = true) }
        }
    }

    fun stop() {
        playToken++
        player.stop()
        loadedVideoId = null
        _state.update { it.copy(isPlaying = false, positionMs = 0L) }
    }

    fun seekTo(ms: Long) {
        seekInternal(ms)?.let { _seekEvents.tryEmit(it) }
    }

    /**
     * Applies a remote seek in place (same track) without emitting a seek event
     * and without restarting the stream, then matches the peer's play/pause.
     *
     * When [toleranceMs] > 0 and the requested position is already within that
     * tolerance while playing, the seek is skipped (only play/pause is matched)
     * so periodic re-sync ticks don't cause audible seek glitches.
     */
    fun seekRemote(positionMs: Long, isPlaying: Boolean, toleranceMs: Long = 0L) {
        if (_state.value.current == null) return
        if (toleranceMs > 0 && isPlaying &&
            abs(positionMs - _state.value.positionMs) <= toleranceMs
        ) {
            setPlaying(isPlaying)
            return
        }
        seekInternal(positionMs)
        setPlaying(isPlaying)
    }

    private fun seekInternal(ms: Long): Long? {
        val s = _state.value
        if (s.current == null) return null
        val target = ms.coerceIn(0L, if (s.durationMs > 0) s.durationMs else ms)
        player.seekTo(target)
        _state.update { it.copy(positionMs = target) }
        return target
    }

    private fun setPlaying(playing: Boolean) {
        val s = _state.value
        if (s.isPlaying == playing) return
        if (playing) {
            startCurrent(s)
        } else {
            player.pause()
            _state.update { it.copy(isPlaying = false) }
        }
    }

    fun setVolume(v: Float) {
        player.setVolume(v)
        _state.update { it.copy(volume = v.coerceIn(0f, 1f)) }
    }

    fun toggleShuffle() {
        val s = _state.value
        val newShuffle = !s.isShuffle
        if (!newShuffle) previousStack.clear()
        _state.update { it.copy(isShuffle = newShuffle) }
        persistShuffleRepeat()
    }

    fun cycleRepeatMode() {
        val s = _state.value
        val next = when (s.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _state.update { it.copy(repeatMode = next) }
        persistShuffleRepeat()
    }

    /** Sets the shuffle state (used when applying a remote device-sync snapshot). */
    fun setShuffle(enabled: Boolean) {
        val s = _state.value
        if (s.isShuffle == enabled) return
        if (!enabled) previousStack.clear()
        _state.update { it.copy(isShuffle = enabled) }
        persistShuffleRepeat()
    }

    /** Sets the repeat mode (used when applying a remote device-sync snapshot). */
    fun setRepeatMode(mode: RepeatMode) {
        val s = _state.value
        if (s.repeatMode == mode) return
        _state.update { it.copy(repeatMode = mode) }
        persistShuffleRepeat()
    }

    /** Restores a saved queue without starting playback (persistent queue). */
    fun restoreQueue(tracks: List<NowPlaying>, index: Int) {
        if (tracks.isEmpty()) return
        playToken++
        player.stop()
        loadedVideoId = null
        val idx = index.coerceIn(0, tracks.lastIndex)
        _state.update {
            it.copy(
                queue = tracks,
                index = idx,
                isPlaying = false,
                positionMs = 0L,
                // Report the saved duration so the seek slider is usable
                // immediately, even before the stream is resolved.
                durationMs = tracks[idx].durationMs,
            )
        }
    }

    /**
     * Applies a remote playback snapshot (from device sync): replaces the
     * queue, jumps to the given index and position, and starts/pauses.
     */
    fun applyRemotePlayback(tracks: List<NowPlaying>, index: Int, positionMs: Long, isPlaying: Boolean) {
        if (tracks.isEmpty()) return
        val idx = index.coerceIn(0, tracks.lastIndex)
        playAt(tracks, idx, startAtMs = positionMs.coerceAtLeast(0L), startPaused = !isPlaying)
    }

    private fun playAt(
        tracks: List<NowPlaying>,
        index: Int,
        startAtMs: Long = 0L,
        startPaused: Boolean = false,
    ) = playAtAttempt(tracks, index, startAtMs, startPaused, attempt = 0)

    private fun playAtAttempt(
        tracks: List<NowPlaying>,
        index: Int,
        startAtMs: Long,
        startPaused: Boolean,
        attempt: Int,
    ) {
        val track = tracks[index]
        val token = ++playToken
        loadedVideoId = track.videoId
        scope.launch {
            player.stop()
            _state.value = PlayerState(
                queue = tracks,
                index = index,
                isPlaying = !startPaused,
                positionMs = startAtMs,
                // Report the known duration immediately so the seek slider has
                // a correct range before the stream resolves (otherwise it shows
                // as disabled / stuck at the end while positionMs > 0).
                durationMs = track.durationMs,
                volume = _state.value.volume,
                isShuffle = _state.value.isShuffle,
                repeatMode = _state.value.repeatMode,
                loadPhase = LoadPhase.RESOLVING,
            )

            val streams = StreamResolver.resolveAacStream(
                track.videoId,
                StreamResolver.AudioQuality.from(DesktopSettings.load().audioQuality),
            )
            if (streams.isEmpty()) {
                if (attempt + 1 < MAX_PLAY_ATTEMPTS) {
                    // Bot detection / transient resolution failure: rotate the
                    // guest identity and try a fresh resolution.
                    GuestSession.rotate()
                    playAtAttempt(tracks, index, startAtMs, startPaused, attempt + 1)
                } else {
                    loadedVideoId = null
                    _state.update { it.copy(isPlaying = false, errorKey = "stream_error", errorDetail = null, loadPhase = LoadPhase.NONE) }
                }
                return@launch
            }
            _state.update { it.copy(errorKey = null, errorDetail = null, loadPhase = LoadPhase.DOWNLOADING) }

            player.play(
                streams = streams,
                cacheKey = track.videoId,
                startAtMs = startAtMs,
                startPaused = startPaused,
                onError = { msg ->
                    if (attempt + 1 < MAX_PLAY_ATTEMPTS) {
                        // Download/decode failure (e.g. stale googlevideo 403):
                        // rotate the guest identity and re-resolve, then retry.
                        scope.launch {
                            GuestSession.rotate()
                            playAtAttempt(tracks, index, startAtMs, startPaused, attempt + 1)
                        }
                    } else {
                        loadedVideoId = null
                        _state.update { s ->
                            if (s.index == index && s.queue.getOrNull(index)?.videoId == track.videoId) {
                                s.copy(isPlaying = false, errorDetail = msg, loadPhase = LoadPhase.NONE)
                            } else s
                        }
                    }
                },
                onPosition = { pos ->
                    _state.update { s ->
                        if (s.index == index && s.queue.getOrNull(index)?.videoId == track.videoId) {
                            s.copy(positionMs = pos)
                        } else s
                    }
                },
                onDuration = { dur ->
                    _state.update { s ->
                        if (s.index == index && s.queue.getOrNull(index)?.videoId == track.videoId) {
                            s.copy(durationMs = dur, loadPhase = LoadPhase.NONE)
                        } else s
                    }
                },
                onComplete = {
                    if (token != playToken) return@play
                    val s = _state.value
                    if (s.index == index && s.queue.getOrNull(index)?.videoId == track.videoId) {
                        handleTrackEnd(s, index, token)
                    }
                },
            )
        }
    }

    private fun handleTrackEnd(s: PlayerState, index: Int, token: Int) {
        when {
            s.repeatMode == RepeatMode.ONE -> {
                if (token == playToken) playAt(s.queue, index)
            }
            autoPlayNext -> {
                val nextIndex = when {
                    s.queue.size == 1 && s.repeatMode != RepeatMode.ALL -> -1
                    s.isShuffle -> randomIndexExcluding(s.queue.size, s.index)
                    s.index < s.queue.lastIndex -> s.index + 1
                    s.repeatMode == RepeatMode.ALL -> 0
                    else -> -1
                }
                if (nextIndex >= 0) {
                    previousStack.addLast(s.index)
                    playAt(s.queue, nextIndex)
                } else {
                    loadedVideoId = null
                    _state.update { it.copy(isPlaying = false) }
                }
            }
            else -> {
                loadedVideoId = null
                _state.update { it.copy(isPlaying = false) }
            }
        }
    }

    private fun randomIndexExcluding(size: Int, exclude: Int): Int {
        if (size <= 1) return 0
        var idx = Random.nextInt(size)
        while (idx == exclude) idx = Random.nextInt(size)
        return idx
    }

    private fun persistShuffleRepeat() {
        val s = DesktopSettings.load()
        if (s.rememberShuffleRepeat) {
            DesktopSettings.update {
                it.copy(
                    isShuffle = _state.value.isShuffle,
                    repeatModeKey = _state.value.repeatMode.name,
                )
            }
        }
    }

    private fun repeatModeFromKey(key: String): RepeatMode =
        runCatching { RepeatMode.valueOf(key) }.getOrDefault(RepeatMode.OFF)
}
