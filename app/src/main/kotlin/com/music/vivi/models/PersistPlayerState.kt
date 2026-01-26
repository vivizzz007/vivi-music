package com.music.vivi.models

import java.io.Serializable

/**
 * Data class representing the saved state of the player.
 * Used to restore playback state (position, queue index, etc.) across app restarts.
 *
 * @property playWhenReady Whether playback should start automatically.
 * @property repeatMode The current repeat mode (OFF, ONE, ALL).
 * @property shuffleModeEnabled Whether shuffle is enabled.
 * @property volume The current volume level (0.0 to 1.0).
 * @property currentPosition The playback position in milliseconds.
 * @property currentMediaItemIndex The index of the currently playing item in the queue.
 * @property playbackState The ExoPlayer playback state constant.
 * @property timestamp The time when this state was saved.
 */
data class PersistPlayerState(
    val playWhenReady: Boolean,
    val repeatMode: Int,
    val shuffleModeEnabled: Boolean,
    val volume: Float,
    val currentPosition: Long,
    val currentMediaItemIndex: Int,
    val playbackState: Int,
    val timestamp: Long = System.currentTimeMillis(),
) : Serializable
