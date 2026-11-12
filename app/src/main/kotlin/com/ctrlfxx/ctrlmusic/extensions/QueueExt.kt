/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.ctrlfxx.ctrlmusic.extensions

import com.ctrlfxx.ctrlmusic.models.MediaMetadata
import com.ctrlfxx.ctrlmusic.models.PersistQueue
import com.ctrlfxx.ctrlmusic.models.QueueData
import com.ctrlfxx.ctrlmusic.models.QueueType
import com.ctrlfxx.ctrlmusic.playback.queues.ListQueue
import com.ctrlfxx.ctrlmusic.playback.queues.LocalAlbumRadio
import com.ctrlfxx.ctrlmusic.playback.queues.Queue
import com.ctrlfxx.ctrlmusic.playback.queues.YouTubeAlbumRadio
import com.ctrlfxx.ctrlmusic.playback.queues.YouTubeQueue

fun Queue.toPersistQueue(
    title: String?,
    items: List<MediaMetadata>,
    mediaItemIndex: Int,
    position: Long
): PersistQueue {
    return when (this) {
        is ListQueue -> {
            if (this.isRadio) {
                PersistQueue(
                    title = title,
                    items = items,
                    mediaItemIndex = mediaItemIndex,
                    position = position,
                    queueType = QueueType.YOUTUBE,
                    queueData = QueueData.YouTubeData(endpoint = "youtube_queue")
                )
            } else {
                PersistQueue(
                    title = title,
                    items = items,
                    mediaItemIndex = mediaItemIndex,
                    position = position,
                    queueType = QueueType.LIST
                )
            }
        }
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
}

fun PersistQueue.toQueue(): Queue {
    return when (queueType) {
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
                position = position,
                isRadio = true
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
}
