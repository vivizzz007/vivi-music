/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.playback

import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import com.music.innertube.models.response.PlayerResponse
import timber.log.Timber

/**
 * Resolves playable audio stream URLs for online tracks using :innertube.
 *
 * Utilizes low-latency YouTube clients (ANDROID_VR, IOS) that provide direct audio
 * streams without requiring JavaScript cipher deobfuscation or poToken, preserving
 * watch battery and CPU resources.
 */
object WearStreamResolver {

    /**
     * Attempts to resolve the direct HTTP audio stream URL for a given [videoId].
     */
    suspend fun resolveStreamUrl(videoId: String): String? {
        Timber.d("Resolving audio stream URL for videoId=%s", videoId)

        // Step 1: ANDROID_VR client (fastest response, no poToken requirement)
        val vrResult = runCatching {
            val response = YouTube.player(
                videoId = videoId,
                client = YouTubeClient.ANDROID_VR_1_43_32,
            ).getOrNull()
            extractBestAudioUrl(response)
        }.getOrNull()

        if (!vrResult.isNullOrBlank()) {
            Timber.d("Successfully resolved stream URL via ANDROID_VR for %s", videoId)
            return vrResult
        }

        // Step 2: IOS client fallback
        val iosResult = runCatching {
            val response = YouTube.player(
                videoId = videoId,
                client = YouTubeClient.IOS,
            ).getOrNull()
            extractBestAudioUrl(response)
        }.getOrNull()

        if (!iosResult.isNullOrBlank()) {
            Timber.d("Successfully resolved stream URL via IOS for %s", videoId)
            return iosResult
        }

        // Step 3: Comprehensive player waterfall fallback
        val waterfallResult = runCatching {
            val response = YouTube.playerWithFallback(videoId).getOrNull()
            extractBestAudioUrl(response)
        }.getOrNull()

        if (!waterfallResult.isNullOrBlank()) {
            Timber.d("Successfully resolved stream URL via waterfall for %s", videoId)
            return waterfallResult
        }

        Timber.w("Unable to resolve audio stream URL for videoId=%s", videoId)
        return null
    }

    private fun extractBestAudioUrl(response: PlayerResponse?): String? {
        val adaptiveFormats = response?.streamingData?.adaptiveFormats.orEmpty()
        return adaptiveFormats
            .filter { it.mimeType.startsWith("audio/") && !it.url.isNullOrBlank() }
            .maxByOrNull { it.bitrate }
            ?.url
    }
}
