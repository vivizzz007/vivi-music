/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.playback.queues

import androidx.media3.common.MediaItem
import com.music.innertube.YouTube
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.extensions.toMediaItem
import com.music.innertube.pages.RadioChip
import com.music.vivi.models.MediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

val Queue.YouTubeQueue: YouTubeQueue?
    get() = this as? YouTubeQueue

class YouTubeQueue(
    var endpoint: WatchEndpoint,
    override val preloadItem: MediaMetadata? = null,
) : Queue {
    private val _radioChips = MutableStateFlow<List<RadioChip>>(emptyList())
    override val radioChips: StateFlow<List<RadioChip>> = _radioChips.asStateFlow()

    private var continuation: String? = null
    private var retryCount = 0
    private val maxRetries = 3

    override suspend fun getInitialStatus(): Queue.Status {
        return withContext(IO) {
            var lastException: Throwable? = null
            
            // Always use RDAMVM for single songs to ensure Radio Chips are provided
            if (endpoint.videoId != null && endpoint.playlistId == null) {
                endpoint = WatchEndpoint(
                    videoId = endpoint.videoId,
                    playlistId = "RDAMVM${endpoint.videoId}"
                )
            }
            
            for (attempt in 0..maxRetries) {
                try {
                    var nextResult = YouTube.next(endpoint, continuation).getOrThrow()
                    
                    if (nextResult.radioChips.isEmpty() && endpoint.videoId != null) {
                        try {
                            val fallbackResult = YouTube.next(WatchEndpoint(videoId = endpoint.videoId)).getOrNull()
                            if (fallbackResult != null && fallbackResult.radioChips.isNotEmpty()) {
                                nextResult = nextResult.copy(radioChips = fallbackResult.radioChips)
                            }
                        } catch (e: Exception) {
                            // Ignored
                        }
                    }

                    endpoint = nextResult.endpoint
                    continuation = nextResult.continuation
                    _radioChips.value = nextResult.radioChips
                    timber.log.Timber.tag("Chippy").e("YouTubeQueue fetched NextResult, pushing ${nextResult.radioChips.size} chips to StateFlow! Endpoint: ${endpoint.playlistId}")
                    retryCount = 0
                    return@withContext Queue.Status(
                        title = nextResult.title,
                        items = nextResult.items.map { it.toMediaItem() },
                        mediaItemIndex = nextResult.currentIndex ?: 0,
                    )
                } catch (e: Exception) {
                    lastException = e
                }
            }
            throw lastException ?: Exception("Failed to get initial status")
        }
    }

    override fun hasNextPage(): Boolean = continuation != null

    override suspend fun nextPage(): List<MediaItem> {
        return withContext(IO) {
            var lastException: Throwable? = null
            
            for (attempt in 0..maxRetries) {
                try {
                    val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
                    endpoint = nextResult.endpoint
                    continuation = nextResult.continuation
                    retryCount = 0
                    return@withContext nextResult.items.map { it.toMediaItem() }
                } catch (e: Exception) {
                    lastException = e
                    retryCount++
                    if (retryCount >= maxRetries) {
                        continuation = null // Stop trying to load more
                    }
                }
            }
            throw lastException ?: Exception("Failed to get next page")
        }
    }

    companion object {
        /**
         * Creates a radio queue based on a song.
         * Uses only videoId to let YouTube personalize recommendations based on user's listening history.
         */
        fun radio(song: MediaMetadata): YouTubeQueue {
            return YouTubeQueue(
                WatchEndpoint(
                    videoId = song.id,
                    playlistId = "RDAMVM${song.id}"
                ),
                song
            )
        }
    }
}
