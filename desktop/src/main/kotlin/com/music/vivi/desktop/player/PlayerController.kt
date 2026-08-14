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

data class PlayerState(
    val queue: List<NowPlaying> = emptyList(),
    val index: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    /** Localization key shown when stream resolution fails. */
    val errorKey: String? = null,
) {
    val current: NowPlaying? get() = queue.getOrNull(index)
}

/**
 * Owns the [AudioPlayer] and exposes UI-facing playback state, including a
 * full queue (add/remove/next/previous/skip/auto-advance). The current track
 * is resolved to an AAC stream and played on a background coroutine.
 */
class PlayerController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val player = AudioPlayer()

    /** Whether to automatically play the next queued track when one ends. */
    @Volatile var autoPlayNext: Boolean = true

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /**
     * Monotonic token identifying the active play session. Incremented whenever
     * playback is (re)started or explicitly stopped, so a stale onComplete from
     * a superseded session never triggers auto-advance.
     */
    private var playToken = 0

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
        if (s.index < s.queue.lastIndex) playAt(s.queue, s.index + 1)
    }

    fun previous() {
        val s = _state.value
        if (s.index > 0) playAt(s.queue, s.index - 1)
    }

    fun skipTo(index: Int) {
        val s = _state.value
        if (index in s.queue.indices) playAt(s.queue, index)
    }

    fun removeAt(index: Int) {
        val s = _state.value
        if (index !in s.queue.indices) return
        val newQueue = s.queue.toMutableList().apply { removeAt(index) }
        when {
            newQueue.isEmpty() -> {
                playToken++
                player.stop()
                _state.value = PlayerState()
            }
            index < s.index -> _state.update { it.copy(queue = newQueue, index = it.index - 1) }
            index == s.index -> playAt(newQueue, s.index.coerceAtMost(newQueue.lastIndex))
            else -> _state.update { it.copy(queue = newQueue) }
        }
    }

    fun clearQueue() {
        playToken++
        player.stop()
        _state.value = PlayerState()
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

    private fun playAt(tracks: List<NowPlaying>, index: Int) {
        val track = tracks[index]
        val token = ++playToken
        scope.launch {
            player.stop()
            _state.value = PlayerState(queue = tracks, index = index, isPlaying = true, positionMs = 0L)

            val url = StreamResolver.resolveAacUrl(track.videoId)
            if (url == null) {
                _state.update { it.copy(isPlaying = false, errorKey = "stream_error") }
                return@launch
            }
            _state.update { it.copy(errorKey = null) }

            player.play(
                url = url,
                onPosition = { pos ->
                    _state.update { s ->
                        if (s.index == index && s.queue.getOrNull(index)?.videoId == track.videoId) {
                            s.copy(positionMs = pos)
                        } else s
                    }
                },
                onComplete = {
                    if (token != playToken) return@play
                    val s = _state.value
                    if (s.index == index && s.queue.getOrNull(index)?.videoId == track.videoId) {
                        // Natural end of the current track → auto-advance (if enabled).
                        if (autoPlayNext && index < s.queue.lastIndex) {
                            playAt(s.queue, index + 1)
                        } else {
                            _state.update { it.copy(isPlaying = false) }
                        }
                    }
                },
            )
        }
    }
}
