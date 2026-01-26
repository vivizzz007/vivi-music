package com.music.vivi.utils

import com.music.lastfm.LastFM
import com.music.vivi.models.MediaMetadata
import kotlinx.coroutines.*
import kotlin.math.min

/**
 * Manages Last.fm scrobbling logic.
 *
 * This class handles the rules for valid scrobbles:
 * - Song must be longer than [minSongDuration] (default 30s).
 * - User must listen for at least [scrobbleDelayPercent] (50%) or [scrobbleDelaySeconds] (4 minutes).
 *
 * It manages a timer that pauses/resumes with playback and triggers the actual scrobble network call
 * when the threshold is reached.
 *
 * @param scope Coroutine scope for launching timer and network jobs.
 * @param minSongDuration Minimum duration in seconds for a song to be scrobble-able.
 * @param scrobbleDelayPercent Percentage of song that must be played (0.0 - 1.0).
 * @param scrobbleDelaySeconds Maximum seconds to wait before scrobbling (cap for long songs).
 */
class ScrobbleManager(
    private val scope: CoroutineScope,
    var minSongDuration: Int = 30,
    var scrobbleDelayPercent: Float = 0.5f,
    var scrobbleDelaySeconds: Int = 50,
) {
    private var scrobbleJob: Job? = null
    private var scrobbleRemainingMillis: Long = 0L
    private var scrobbleTimerStartedAt: Long = 0L
    private var songStartedAt: Long = 0L
    private var songStarted = false
    var useNowPlaying = true

    fun destroy() {
        scrobbleJob?.cancel()
        scrobbleRemainingMillis = 0L
        scrobbleTimerStartedAt = 0L
        songStartedAt = 0L
        songStarted = false
    }

    /**
     * Called when a new song starts playing.
     * Starts the scrobble timer and optionally updates "Now Playing" status.
     *
     * @param metadata The metadata of the started song.
     * @param duration Optional override for duration (in milliseconds).
     */
    fun onSongStart(metadata: MediaMetadata?, duration: Long? = null) {
        if (metadata == null) return
        songStartedAt = System.currentTimeMillis() / 1000
        songStarted = true
        startScrobbleTimer(metadata, duration)
        if (useNowPlaying) {
            updateNowPlaying(metadata)
        }
    }

    fun onSongResume(metadata: MediaMetadata) {
        resumeScrobbleTimer(metadata)
    }

    fun onSongPause() {
        pauseScrobbleTimer()
    }

    fun onSongStop() {
        stopScrobbleTimer()
        songStarted = false
    }

    private fun startScrobbleTimer(metadata: MediaMetadata, duration: Long? = null) {
        scrobbleJob?.cancel()
        val duration = duration?.toInt()?.div(1000) ?: metadata.duration

        if (duration <= minSongDuration) return

        val threshold = duration * 1000L * scrobbleDelayPercent
        scrobbleRemainingMillis = min(threshold.toLong(), scrobbleDelaySeconds * 1000L)

        if (scrobbleRemainingMillis <= 0) {
            scrobbleSong(metadata)
            return
        }
        scrobbleTimerStartedAt = System.currentTimeMillis()
        scrobbleJob = scope.launch {
            delay(scrobbleRemainingMillis)
            scrobbleSong(metadata)
            scrobbleJob = null
        }
    }

    private fun pauseScrobbleTimer() {
        scrobbleJob?.cancel()
        if (scrobbleTimerStartedAt != 0L) {
            val elapsed = System.currentTimeMillis() - scrobbleTimerStartedAt
            scrobbleRemainingMillis -= elapsed
            if (scrobbleRemainingMillis < 0) scrobbleRemainingMillis = 0
            scrobbleTimerStartedAt = 0L
        } else {
        }
    }

    private fun resumeScrobbleTimer(metadata: MediaMetadata) {
        if (scrobbleRemainingMillis <= 0) return
        scrobbleJob?.cancel()
        scrobbleTimerStartedAt = System.currentTimeMillis()
        scrobbleJob = scope.launch {
            delay(scrobbleRemainingMillis)
            scrobbleSong(metadata)
            scrobbleJob = null
        }
    }

    private fun stopScrobbleTimer() {
        scrobbleJob?.cancel()
        scrobbleJob = null
        scrobbleRemainingMillis = 0
    }

    private fun scrobbleSong(metadata: MediaMetadata) {
        scope.launch {
            LastFM.scrobble(
                artist = metadata.artists.joinToString { it.name },
                track = metadata.title,
                duration = metadata.duration,
                timestamp = songStartedAt,
                album = metadata.album?.title
            )
        }
    }

    private fun updateNowPlaying(metadata: MediaMetadata) {
        scope.launch {
            LastFM.updateNowPlaying(
                artist = metadata.artists.joinToString { it.name },
                track = metadata.title,
                album = metadata.album?.title,
                duration = metadata.duration
            )
        }
    }

    fun onPlayerStateChanged(isPlaying: Boolean, metadata: MediaMetadata?, duration: Long? = null) {
        if (metadata == null) return
        if (isPlaying) {
            if (!songStarted) {
                onSongStart(metadata, duration)
            } else {
                onSongResume(metadata)
            }
        } else {
            onSongPause()
        }
    }
}
