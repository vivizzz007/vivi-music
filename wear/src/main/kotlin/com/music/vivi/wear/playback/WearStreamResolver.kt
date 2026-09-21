/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.playback

import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import com.music.innertube.models.response.PlayerResponse
import com.music.innertube.NewPipeExtractor
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves playable audio stream URLs for online tracks using :innertube and NewPipeExtractor.
 *
 * Handles direct stream formats, cipher deobfuscation (signatureCipher), client waterfall fallbacks,
 * and pure NewPipe extraction for maximum reliability on Samsung Galaxy Watch 7.
 */
object WearStreamResolver {

    private val streamUrlCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private const val CACHE_TTL_MS = 3600_000L // 1 hour

    /**
     * Attempts to resolve the direct HTTP audio stream URL for a given [videoId].
     */
    suspend fun resolveStreamUrl(videoId: String): String? {
        Timber.d("Resolving audio stream URL for videoId=%s", videoId)

        // 0. Check in-memory cache
        val cached = streamUrlCache[videoId]
        if (cached != null && (System.currentTimeMillis() - cached.second) < CACHE_TTL_MS) {
            Timber.d("Using cached stream URL for %s", videoId)
            return cached.first
        }

        // 1. Try YouTube.playerWithFallback with current login status
        val isLoggedIn = YouTube.cookie != null
        val response = runCatching {
            YouTube.playerWithFallback(videoId, isLoggedIn = isLoggedIn).getOrNull()
        }.getOrNull()

        val fallbackUrl = extractBestAudioUrl(response, videoId)
        if (!fallbackUrl.isNullOrBlank()) {
            Timber.d("Successfully resolved stream URL via playerWithFallback for %s", videoId)
            streamUrlCache[videoId] = fallbackUrl to System.currentTimeMillis()
            return fallbackUrl
        }

        // 2. Try specific YouTube clients known for accessible streams
        val candidateClients = listOf(
            YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
            YouTubeClient.MWEB,
            YouTubeClient.ANDROID_VR_1_65_10,
            YouTubeClient.ANDROID_VR_1_43_32,
            YouTubeClient.WEB_REMIX,
            YouTubeClient.IOS,
        )

        for (client in candidateClients) {
            val clientResp = runCatching {
                YouTube.player(videoId = videoId, client = client).getOrNull()
            }.getOrNull()

            val clientUrl = extractBestAudioUrl(clientResp, videoId)
            if (!clientUrl.isNullOrBlank()) {
                Timber.d("Successfully resolved stream URL via client %s for %s", client.clientName, videoId)
                streamUrlCache[videoId] = clientUrl to System.currentTimeMillis()
                return clientUrl
            }
        }

        // 3. Fallback to direct NewPipeExtractor stream extraction
        val directStreams = runCatching {
            NewPipeExtractor.newPipePlayer(videoId)
        }.getOrNull()

        if (!directStreams.isNullOrEmpty()) {
            // Find preferred audio itag (251 = opus 160k, 140 = m4a 128k, etc.)
            val preferredStream = directStreams.find { it.first == 140 }
                ?: directStreams.find { it.first == 251 }
                ?: directStreams.firstOrNull()

            val directUrl = preferredStream?.second
            if (!directUrl.isNullOrBlank()) {
                Timber.d("Successfully resolved stream URL via NewPipe direct extraction for %s", videoId)
                streamUrlCache[videoId] = directUrl to System.currentTimeMillis()
                return directUrl
            }
        }

        Timber.w("Unable to resolve audio stream URL for videoId=%s", videoId)
        return null
    }

    private fun extractBestAudioUrl(response: PlayerResponse?, videoId: String): String? {
        val streamingData = response?.streamingData ?: return null
        val audioFormats = (streamingData.adaptiveFormats.orEmpty().filter { it.mimeType.startsWith("audio/") }
            .ifEmpty { streamingData.adaptiveFormats.orEmpty() }
            .ifEmpty { streamingData.formats.orEmpty() })

        if (audioFormats.isEmpty()) return null

        // Sort descending by bitrate
        val sortedFormats = audioFormats.sortedByDescending { it.bitrate }

        for (format in sortedFormats) {
            val directUrl = format.url
            if (!directUrl.isNullOrBlank()) {
                return directUrl
            }

            // If signatureCipher or cipher is present, deobfuscate using NewPipeExtractor
            val cipherFormat = if (format.signatureCipher == null && format.cipher != null) {
                format.copy(signatureCipher = format.cipher)
            } else {
                format
            }

            val deobfuscated = runCatching {
                NewPipeExtractor.getStreamUrl(cipherFormat, videoId)
            }.getOrNull()

            if (!deobfuscated.isNullOrBlank()) {
                return deobfuscated
            }
        }

        return null
    }

    /**
     * Clears the in-memory stream URL cache.
     */
    fun clearCache() {
        streamUrlCache.clear()
    }
}
