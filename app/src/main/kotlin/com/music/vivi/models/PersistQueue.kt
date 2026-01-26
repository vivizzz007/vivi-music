package com.music.vivi.models

import java.io.Serializable

/**
 * Represents a saved playback queue structure for persistence.
 * Captures the list of songs, current position, and queue type (e.g. infinite radio state).
 *
 * @property title Optional title of the queue (e.g. Playlist Name).
 * @property items The list of songs in the current queue window.
 * @property mediaItemIndex The index of the currently active song.
 * @property position The playback position within the active song.
 * @property queueType The type of queue (List, YouTube Radio, Album Radio, etc.).
 * @property queueData Additional data required to restore dynamic queues (endpoints, continuation tokens).
 */
data class PersistQueue(
    val title: String?,
    val items: List<MediaMetadata>,
    val mediaItemIndex: Int,
    val position: Long,
    val queueType: QueueType = QueueType.LIST,
    val queueData: QueueData? = null,
) : Serializable

/**
 * Enumerates the different types of queues supported by the player.
 */
sealed class QueueType : Serializable {
    object LIST : QueueType()
    object YOUTUBE : QueueType()
    object YOUTUBE_ALBUM_RADIO : QueueType()
    object LOCAL_ALBUM_RADIO : QueueType()
}

/**
 * Holds specific state data needed to restore dynamic queues.
 */
sealed class QueueData : Serializable {
    /**
     * Data for a YouTube Mix/Radio.
     */
    data class YouTubeData(val endpoint: String, val continuation: String? = null) : QueueData()

    /**
     * Data for a YouTube Album Radio (Album + Recommendations).
     */
    data class YouTubeAlbumRadioData(
        val playlistId: String,
        val albumSongCount: Int = 0,
        val continuation: String? = null,
        val firstTimeLoaded: Boolean = false,
    ) : QueueData()

    /**
     * Data for a Local Album Radio (Local Album + Recommendations).
     */
    data class LocalAlbumRadioData(
        val albumId: String,
        val startIndex: Int = 0,
        val playlistId: String? = null,
        val continuation: String? = null,
        val firstTimeLoaded: Boolean = false,
    ) : QueueData()
}
