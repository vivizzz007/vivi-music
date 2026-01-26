package com.music.vivi.extensions

import com.music.vivi.models.MediaMetadata
import com.music.vivi.models.PersistQueue
import com.music.vivi.models.QueueData
import com.music.vivi.models.QueueType
import com.music.vivi.playback.queues.*

/**
 * Converts a runtime [Queue] into a persistable [PersistQueue] data object.
 * This is used to save the playback state to disk when the service is destroyed.
 *
 * @param title Title of the queue (e.g. Playlist name).
 * @param items List of metadata items in the queue.
 * @param mediaItemIndex Current playing index.
 * @param position Current playback position in ms.
 * @return A serializable [PersistQueue].
 */
fun Queue.toPersistQueue(
    title: String?,
    items: List<MediaMetadata>,
    mediaItemIndex: Int,
    position: Long,
): PersistQueue = when (this) {
    is ListQueue -> PersistQueue(
        title = title,
        items = items,
        mediaItemIndex = mediaItemIndex,
        position = position,
        queueType = QueueType.LIST
    )
    is YouTubeQueue -> {
        // Since endpoint is private, we'll store a simplified version
        val endpoint = "youtube_queue"
        PersistQueue(
            title = title,
            items = items,
            mediaItemIndex = mediaItemIndex,
            position = position,
            queueType = QueueType.YOUTUBE,
            queueData = QueueData.YouTubeData(endpoint = endpoint)
        )
    }
    is YouTubeAlbumRadio -> {
        // Since playlistId is private, we'll store a simplified version
        PersistQueue(
            title = title,
            items = items,
            mediaItemIndex = mediaItemIndex,
            position = position,
            queueType = QueueType.YOUTUBE_ALBUM_RADIO,
            queueData = QueueData.YouTubeAlbumRadioData(
                playlistId = "youtube_album_radio"
            )
        )
    }
    is LocalAlbumRadio -> {
        // Since albumWithSongs and startIndex are private, we'll store a simplified version
        PersistQueue(
            title = title,
            items = items,
            mediaItemIndex = mediaItemIndex,
            position = position,
            queueType = QueueType.LOCAL_ALBUM_RADIO,
            queueData = QueueData.LocalAlbumRadioData(
                albumId = "local_album_radio",
                startIndex = 0
            )
        )
    }
    else -> PersistQueue(
        title = title,
        items = items,
        mediaItemIndex = mediaItemIndex,
        position = position,
        queueType = QueueType.LIST
    )
}

/**
 * Restores a [PersistQueue] back into a runtime [Queue] implementation.
 *
 * NOTE: Currently falls back to [ListQueue] for complex types (YouTubeQueue, AlbumRadio)
 * if full reconstruction is not possible without network.
 */
fun PersistQueue.toQueue(): Queue = when (queueType) {
    is QueueType.LIST -> ListQueue(
        title = title,
        items = items.map { it.toMediaItem() },
        startIndex = mediaItemIndex,
        position = position
    )
    is QueueType.YOUTUBE -> {
        // For now, fallback to ListQueue since we can't reconstruct YouTubeQueue properly
        ListQueue(
            title = title,
            items = items.map { it.toMediaItem() },
            startIndex = mediaItemIndex,
            position = position
        )
    }
    is QueueType.YOUTUBE_ALBUM_RADIO -> {
        // For now, fallback to ListQueue since we can't reconstruct YouTubeAlbumRadio properly
        ListQueue(
            title = title,
            items = items.map { it.toMediaItem() },
            startIndex = mediaItemIndex,
            position = position
        )
    }
    is QueueType.LOCAL_ALBUM_RADIO -> {
        // For now, fallback to ListQueue since we can't reconstruct LocalAlbumRadio properly
        ListQueue(
            title = title,
            items = items.map { it.toMediaItem() },
            startIndex = mediaItemIndex,
            position = position
        )
    }
}
