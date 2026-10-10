/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import com.music.vivi.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * A playable music-video stream. Either [audioUrl] is null (progressive stream that already contains sound) or the
 * two urls have to be merged by the player (adaptive video-only + audio-only).
 */
data class MusicVideoStream(
    val videoUrl: String,
    val audioUrl: String?,
    val userAgent: String,
    val label: String,
)

/**
 * Finds out whether a song has a music video and resolves a stream for it.
 *
 * Lookups are cached for the lifetime of the process so that swiping through a queue only costs one request per song.
 */
object MusicVideoRepository {
    private const val TAG = "MusicVideoRepository"
    private const val NO_VIDEO = ""
    private const val MAX_CACHE_ENTRIES = 400

    // songId -> videoId of the music video, or NO_VIDEO when the song has none.
    private val videoIdCache = ConcurrentHashMap<String, String>()

    /** Cached answer, or null when the song has not been looked up yet. */
    fun cachedVideoId(metadata: MediaMetadata): String? {
        if (metadata.isVideoSong) return metadata.id
        return videoIdCache[metadata.id]?.takeIf { it != NO_VIDEO }
    }

    fun isLookedUp(metadata: MediaMetadata): Boolean = metadata.isVideoSong || videoIdCache.containsKey(metadata.id)

    /** The id of the music video for [metadata], or null when it has none (or the lookup failed). */
    suspend fun findVideoId(metadata: MediaMetadata): String? {
        if (metadata.isVideoSong) return metadata.id
        videoIdCache[metadata.id]?.let { return it.takeIf { id -> id != NO_VIDEO } }

        val result = withContext(Dispatchers.IO) { YouTube.musicVideoFor(metadata.id) }
        val info = result.getOrElse {
            // Network problem: don't cache, so the next song change / reopen can try again.
            Timber.tag(TAG).w(it, "Music video lookup failed for ${metadata.id}")
            return null
        }
        if (videoIdCache.size > MAX_CACHE_ENTRIES) videoIdCache.clear()
        videoIdCache[metadata.id] = info?.videoId ?: NO_VIDEO
        return info?.videoId
    }

    /** Resolves a stream for [videoId] using the NewPipe extractor, preferring ~720p. */
    suspend fun resolveStream(videoId: String): MusicVideoStream? = withContext(Dispatchers.IO) {
        val streams = runCatching { NewPipeExtractor.newPipePlayer(videoId) }.getOrDefault(emptyList())
        if (streams.isEmpty()) return@withContext null
        val byItag = HashMap<Int, String>()
        streams.forEach { (itag, url) -> byItag.putIfAbsent(itag, url) }

        fun progressive(itag: Int, label: String): MusicVideoStream? = byItag[itag]?.let {
            MusicVideoStream(videoUrl = it, audioUrl = null, userAgent = userAgentFor(it), label = label)
        }

        fun adaptive(videoItag: Int, audioItag: Int, label: String): MusicVideoStream? {
            val video = byItag[videoItag] ?: return null
            val audio = byItag[audioItag] ?: return null
            return MusicVideoStream(video, audio, userAgentFor(video), label)
        }

        // Muxed streams are the most reliable, adaptive pairs give a sharper picture when no muxed 720p exists.
        progressive(22, "720p")
            ?: adaptive(136, 140, "720p")
            ?: adaptive(247, 251, "720p")
            ?: progressive(18, "360p")
            ?: adaptive(135, 140, "480p")
            ?: adaptive(244, 251, "480p")
            ?: adaptive(134, 140, "360p")
            ?: adaptive(243, 251, "360p")
    }

    private fun userAgentFor(url: String): String = when {
        url.contains("c=ANDROID_VR") -> YouTubeClient.ANDROID_VR_NO_AUTH.userAgent
        url.contains("c=TVHTML5") -> YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER.userAgent
        url.contains("c=IOS") -> YouTubeClient.IOS.userAgent
        url.contains("c=ANDROID") -> YouTubeClient.MOBILE.userAgent
        else -> YouTubeClient.USER_AGENT_WEB
    }
}
