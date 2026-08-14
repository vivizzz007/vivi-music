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
    val current: NowPlaying? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    /** Localization key shown when stream resolution fails. */
    val errorKey: String? = null,
)

/**
 * Owns the [AudioPlayer] and exposes the UI-facing playback state. The current
 * track is resolved to an AAC stream and played on a background coroutine.
 */
class PlayerController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val player = AudioPlayer()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    fun play(track: NowPlaying) {
        scope.launch {
            player.stop()
            _state.value = PlayerState(current = track, isPlaying = true, positionMs = 0L)
            val url = StreamResolver.resolveAacUrl(track.videoId)
            if (url == null) {
                _state.update { it.copy(isPlaying = false, errorKey = "stream_error") }
                return@launch
            }
            player.play(
                url = url,
                onPosition = { pos ->
                    _state.update { s -> if (s.current?.videoId == track.videoId) s.copy(positionMs = pos) else s }
                },
                onComplete = {
                    _state.update { s -> if (s.current?.videoId == track.videoId) s.copy(isPlaying = false) else s }
                },
            )
        }
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
        player.stop()
        _state.value = PlayerState(current = _state.value.current, isPlaying = false, positionMs = 0L)
    }
}
