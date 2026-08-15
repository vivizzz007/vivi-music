package com.music.vivi.desktop.player

import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.YouTubeExtractor
import com.music.innertube.models.YouTubeClient
import com.music.innertube.models.response.PlayerResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Resolves a playable AAC (`audio/mp4`, itag 140) stream URL for a YouTube
 * video id. AAC is used because the desktop player decodes it with the
 * pure-Java `jaad` decoder; Opus would need a WebM demuxer we do not have.
 *
 * This mirrors the mobile app's `YTPlayerUtils.resolvePlaybackData` client
 * chain (main client + fallback clients + n-param deobfuscation + URL
 * validation), minus the Android-only PoToken / connectivity logic. The single
 * ANDROID_VR attempt previously used here was not enough: YouTube often
 * answers it with `LOGIN_REQUIRED` as a bot-detection signal, and without a
 * fallback the resolver returned null ("could not resolve the audio stream").
 */
object StreamResolver {

    /** Fast, PoToken-free main client (same as the mobile app). */
    private val MAIN_CLIENT: YouTubeClient = YouTubeClient.ANDROID_VR_1_43_32

    /**
     * Ordered fallback clients. Non-PoToken, non-login clients come first;
     * PoToken clients (WEB_REMIX) are the last resort since the desktop does
     * not generate PoTokens.
     */
    private val FALLBACK_CLIENTS: List<YouTubeClient> = listOf(
        YouTubeClient.ANDROID_VR_1_61_48,
        YouTubeClient.ANDROID_VR_NO_AUTH,
        YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        YouTubeClient.ANDROID_CREATOR,
        YouTubeClient.IPADOS,
        YouTubeClient.IOS,
        YouTubeClient.MOBILE,
        YouTubeClient.ANDROID_MUSIC,
        YouTubeClient.ANDROID_NO_SDK,
        YouTubeClient.WEB,
        YouTubeClient.WEB_REMIX,
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Returns a direct HTTP URL to an AAC audio stream, or null if it cannot be
     * resolved. Callers should invoke this from a background coroutine: the
     * NewPipe path is blocking and the player path performs network I/O.
     */
    suspend fun resolveAacUrl(videoId: String): String? {
        // 1) NewPipe — handles signature cipher and returns already-playable
        //    stream URLs when its extractor is not bot-blocked.
        val newPipeUrl = withContext(Dispatchers.IO) {
            runCatching {
                YouTube.getNewPipeStreamUrls(videoId)
                    .firstOrNull { it.first == 140 }
                    ?.second
            }.getOrNull()
        }
        if (!newPipeUrl.isNullOrBlank()) return newPipeUrl

        // 2) Client chain: try the main client then the fallbacks until we get
        //    a validated AAC URL. Keep the first resolved URL as a last-resort
        //    fallback in case validation is flaky (googlevideo sometimes
        //    rejects HEAD requests).
        var firstResolved: String? = null
        var signatureTimestamp: Int? = null
        var signatureFetched = false

        for (ytClient in listOf(MAIN_CLIENT) + FALLBACK_CLIENTS) {
            if (ytClient.useSignatureTimestamp && !signatureFetched) {
                signatureTimestamp = withContext(Dispatchers.IO) {
                    runCatching { NewPipeExtractor.getSignatureTimestamp(videoId).getOrNull() }.getOrNull()
                }
                signatureFetched = true
            }
            val ts = if (ytClient.useSignatureTimestamp) signatureTimestamp else null

            val response = YouTube.player(videoId, null, ytClient, ts, null).getOrNull()
                ?: continue
            if (response.playabilityStatus.status != "OK") continue

            val url = resolveFromResponse(response) ?: continue
            if (firstResolved == null) firstResolved = url
            if (validateUrl(url)) return url
        }

        return firstResolved
    }

    /** Picks the best AAC format from a successful player response and resolves its URL. */
    private fun resolveFromResponse(response: PlayerResponse): String? {
        val adaptive = response.streamingData?.adaptiveFormats ?: return null
        val audioFormats = adaptive.filter { it.isAudio && it.isOriginal && it.mimeType.startsWith("audio/mp4") }
        // Prefer AAC-LC (codec mp4a.40.2, itags 140/141): the JAAD decoder handles
        // AAC-LC, but fails on HE-AAC/SBR (mp4a.40.5, e.g. itag 139) with a
        // "FIL element overread" error. Only fall back to other audio/mp4 codecs
        // (HE-AAC) if no AAC-LC stream exists.
        val format = audioFormats.firstOrNull { it.mimeType.contains("mp4a.40.2") }
            ?: audioFormats.firstOrNull()
            ?: adaptive.filter { it.isAudio && it.isOriginal }.firstOrNull()
            ?: return null

        val raw = when {
            !format.url.isNullOrEmpty() -> format.url
            else -> format.signatureCipher ?: format.cipher
        } ?: return null

        val url = if (raw.startsWith("http")) raw else YouTubeExtractor.decryptUrl(raw)
        if (url.isNullOrBlank()) return null

        // Apply the n-parameter transform (no-op when there is no `n=` param).
        return runCatching { YouTubeExtractor.deobfuscateUrlNParam(url) }.getOrDefault(url)
    }

    /** Best-effort HEAD validation of a resolved stream URL. */
    private fun validateUrl(url: String): Boolean = runCatching {
        val request = Request.Builder()
            .head()
            .url(url)
            .header("User-Agent", YouTubeClient.USER_AGENT_WEB)
            .build()
        client.newCall(request).execute().use { it.isSuccessful }
    }.getOrDefault(false)
}
