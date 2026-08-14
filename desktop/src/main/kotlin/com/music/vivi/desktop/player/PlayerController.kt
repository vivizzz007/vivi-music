package com.music.vivi.desktop.player

import com.music.vivi.desktop.NowPlaying
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class RepeatMode { OFF, ALL, ONE }

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
) {
    val current: NowPlaying? get() = queue.getOrNull(index)
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

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** Monotonic token identifying the active play session. */
    private var playToken = 0

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
            player.resume()
            _state.update { it.copy(isPlaying = true) }
        }
    }

    fun stop() {
        playToken++
        player.stop()
        _state.update { it.copy(isPlaying = false, positionMs = 0L) }
    }

    fun seekTo(ms: Long) {
        val s = _state.value
        if (s.current == null) return
        val target = ms.coerceIn(0L, if (s.durationMs > 0) s.durationMs else ms)
        player.seekTo(target)
        _state.update { it.copy(positionMs = target) }
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
    }

    fun cycleRepeatMode() {
        val s = _state.value
        val next = when (s.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _state.update { it.copy(repeatMode = next) }
    }

    private fun playAt(tracks: List<NowPlaying>, index: Int) {
        val track = tracks[index]
        val token = ++playToken
        scope.launch {
            player.stop()
            _state.value = PlayerState(
                queue = tracks,
                index = index,
                isPlaying = true,
                positionMs = 0L,
                volume = _state.value.volume,
                isShuffle = _state.value.isShuffle,
                repeatMode = _state.value.repeatMode,
            )

            val url = StreamResolver.resolveAacUrl(track.videoId)
            if (url == null) {
                _state.update { it.copy(isPlaying = false, errorKey = "stream_error") }
                return@launch
            }
            _state.update { it.copy(errorKey = null) }

            player.play(
                url = url,
                cacheKey = track.videoId,
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
                            s.copy(durationMs = dur)
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
                    _state.update { it.copy(isPlaying = false) }
                }
            }
            else -> _state.update { it.copy(isPlaying = false) }
        }
    }

    private fun randomIndexExcluding(size: Int, exclude: Int): Int {
        if (size <= 1) return 0
        var idx = Random.nextInt(size)
        while (idx == exclude) idx = Random.nextInt(size)
        return idx
    }
}
