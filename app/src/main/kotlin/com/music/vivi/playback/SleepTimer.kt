package com.music.vivi.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages the sleep timer functionality.
 * Can stop playback after a specific duration or at the end of the current song.
 */
class SleepTimer(private val scope: CoroutineScope, val player: Player) : Player.Listener {
    private var sleepTimerJob: Job? = null
    var triggerTime by mutableLongStateOf(-1L)
        private set
    var pauseWhenSongEnd by mutableStateOf(false)
        private set
    val isActive: Boolean
        get() = triggerTime != -1L || pauseWhenSongEnd

    /**
     * Starts the sleep timer.
     *
     * @param durationMillis The duration in milliseconds to wait before pausing.
     *                       Pass -1L to pause when the current song ends.
     */
    fun start(durationMillis: Long) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        if (durationMillis == -1L) {
            pauseWhenSongEnd = true
        } else {
            triggerTime = System.currentTimeMillis() + durationMillis
            sleepTimerJob =
                scope.launch {
                    delay(durationMillis)
                    player.pause()
                    triggerTime = -1L
                }
        }
    }

    fun clear() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        pauseWhenSongEnd = false
        triggerTime = -1L
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (pauseWhenSongEnd) {
            pauseWhenSongEnd = false
            player.pause()
        }
    }

    override fun onPlaybackStateChanged(@Player.State playbackState: Int) {
        if (playbackState == Player.STATE_ENDED && pauseWhenSongEnd) {
            pauseWhenSongEnd = false
            player.pause()
        }
    }
}
