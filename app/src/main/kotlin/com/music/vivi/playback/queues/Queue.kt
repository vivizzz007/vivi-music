package com.music.vivi.playback.queues

import androidx.media3.common.MediaItem
import com.music.innertube.models.filterVideoSongs
import com.music.vivi.extensions.metadata
import com.music.vivi.models.MediaMetadata

/**
 * Represents a playable sequence of media items.
 * Can be a static list, or a dynamic queue (like infinite radio).
 */
interface Queue {
    /**
     * An item to preload/display immediately while fetching the full queue status.
     */
    val preloadItem: MediaMetadata?

    /**
     * Fetches the initial state of the queue (e.g., first batch of songs).
     */
    suspend fun getInitialStatus(): Status

    /**
     * Returns true if there are more items to fetch.
     */
    fun hasNextPage(): Boolean

    /**
     * Fetches the next batch of items.
     */
    suspend fun nextPage(): List<MediaItem>

    /**
     * Snapshot of the queue status.
     *
     * @param title The title of the queue (e.g. Playlist Name).
     * @param items The list of media items currently available.
     * @param mediaItemIndex The index to start playing from.
     * @param position The start position in ms.
     */
    data class Status(
        val title: String?,
        val items: List<MediaItem>,
        val mediaItemIndex: Int,
        val position: Long = 0L,
    ) {
        fun filterExplicit(enabled: Boolean = true) = if (enabled) {
            copy(
                items = items.filterExplicit()
            )
        } else {
            this
        }
        fun filterVideoSongs(disableVideos: Boolean = false) = if (disableVideos) {
            copy(
                items = items.filterVideoSongs(true)
            )
        } else {
            this
        }
    }
}

fun List<MediaItem>.filterExplicit(enabled: Boolean = true) = if (enabled) {
    filterNot {
        it.metadata?.explicit == true
    }
} else {
    this
}
fun List<MediaItem>.filterVideoSongs(disableVideos: Boolean = false) = if (disableVideos) {
    filterNot { it.metadata?.isVideoSong == true }
} else {
    this
}
