package com.music.vivi.desktop.player

import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.YouTubeExtractor
import com.music.innertube.models.YouTubeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resolves a playable AAC (`audio/mp4`, itag 140) stream URL for a YouTube
 * video id. AAC is used because the desktop player decodes it with the
 * pure-Java `jaad` decoder; Opus would need a WebM demuxer we do not have.
 */
object StreamResolver {

    /**
     * Returns a direct HTTP URL to an AAC audio stream, or null if it cannot be
     * resolved. Callers should invoke this from a background coroutine: the
     * NewPipe path is blocking and the player path performs network I/O.
     */
    suspend fun resolveAacUrl(videoId: String): String? {
        // 1) NewPipe — handles signature cipher, n-param and bot detection for
        //    us and returns already-playable stream URLs.
        val newPipeUrl = withContext(Dispatchers.IO) {
            runCatching {
                NewPipeExtractor.newPipePlayer(videoId)
                    .firstOrNull { it.first == 140 }
                    ?.second
            }.getOrNull()
        }
        if (!newPipeUrl.isNullOrBlank()) return newPipeUrl

        // 2) Direct player response via ANDROID_VR (no PoToken required).
        return runCatching {
            val response = YouTube.player(
                videoId = videoId,
                playlistId = null,
                client = YouTubeClient.ANDROID_VR_1_43_32,
            ).getOrNull() ?: return null

            val format = response.streamingData?.adaptiveFormats
                ?.filter { it.isAudio && it.isOriginal }
                ?.firstOrNull { it.mimeType.startsWith("audio/mp4") }
                ?: response.streamingData?.adaptiveFormats
                    ?.firstOrNull { it.isAudio && it.isOriginal }

            val raw = when {
                !format?.url.isNullOrEmpty() -> format?.url
                else -> format?.signatureCipher ?: format?.cipher
            } ?: return null

            if (raw.startsWith("http")) raw else YouTubeExtractor.decryptUrl(raw)
        }.getOrNull()
    }
}
