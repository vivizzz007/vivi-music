package com.music.vivi.desktop.player

import com.music.vivi.desktop.DesktopSettings
import com.music.vivi.desktop.GuestSession
import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.YouTubeExtractor
import com.music.innertube.models.YouTubeClient
import com.music.innertube.models.response.PlayerResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    /**
     * Audio quality: picks the preferred AAC-LC itag. `139` (HE-AAC) and `251`
     * (Opus) are deliberately excluded because the JAAD decoder cannot decode
     * them. `AUTO`/`HIGH` prefer 256 kbps (itag 141), `LOW` prefers 128 kbps
     * (itag 140).
     */
    enum class AudioQuality(val preferredItags: List<Int>) {
        AUTO(listOf(141, 140)),
        HIGH(listOf(141, 140)),
        LOW(listOf(140, 141));

        companion object {
            fun from(key: String?): AudioQuality =
                entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: AUTO
        }
    }

    /** A resolved stream URL plus the User-Agent required to download it, and
     *  the authoritative track length (from `videoDetails.lengthSeconds`) when
     *  known — used to give the seek slider a correct range immediately. */
    data class ResolvedStream(val url: String, val userAgent: String, val durationMs: Long? = null)

    /** Fast, PoToken-free main client (same as the mobile app). */
    private val MAIN_CLIENT: YouTubeClient = YouTubeClient.ANDROID_VR_1_43_32

    /**
     * Ordered fallback clients. Only non-PoToken clients are used: `WEB` and
     * `WEB_REMIX` are deliberately excluded because their googlevideo URLs are
     * signed for the YouTube-Music web client and answer 403 without a PoToken,
     * which the desktop cannot generate.
     */
    private val FALLBACK_CLIENTS: List<YouTubeClient> = listOf(
        YouTubeClient.ANDROID_VR_1_61_48,
        YouTubeClient.ANDROID_VR_NO_AUTH,
        YouTubeClient.VISIONOS,
        YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        YouTubeClient.ANDROID_CREATOR,
        YouTubeClient.IPADOS,
        YouTubeClient.IOS,
        YouTubeClient.IOS_MUSIC,
        YouTubeClient.MOBILE,
        YouTubeClient.ANDROID_MUSIC,
        YouTubeClient.ANDROID_NO_SDK,
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Short-lived in-memory cache of resolved stream URLs, so starting the
     * same track again (or retrying it) does not re-run the whole resolution
     * chain (NewPipe signature + player client chain + URL validation) for as
     * long as the URLs are still valid. googlevideo URLs are single-use and
     * expire quickly, hence the 10-minute TTL.
     */
    private class Cached(val streams: List<ResolvedStream>, val expiresAt: Long)

    private val cache = java.util.concurrent.ConcurrentHashMap<String, Cached>()
    private const val CACHE_MAX_ENTRIES = 32

    /** Cache lifetime in ms, read from the user setting (1–60 minutes);
     *  0 (or any non-positive value) means the cache never expires. */
    private fun cacheTtlMs(): Long = when (val minutes = DesktopSettings.load().streamCacheMinutes) {
        in 1..60 -> minutes * 60_000L
        else -> Long.MAX_VALUE
    }

    /**
     * Returns a direct HTTP URL to an AAC audio stream, or null if it cannot be
     * resolved. Callers should invoke this from a background coroutine: the
     * NewPipe path is blocking and the player path performs network I/O.
     */
    /**
     * Resolves an ordered list of candidate stream URLs (each with the
     * User-Agent required to download it). The player tries them in order and
     * falls through on failure, so a bot-blocked URL from one source never
     * prevents playback when another source works.
     */
    suspend fun resolveAacStream(videoId: String, quality: AudioQuality = AudioQuality.AUTO): List<ResolvedStream> {
        // Serve from the in-memory cache first: the URLs are only valid for a
        // few minutes anyway, so re-resolving is pure waste when we still have
        // a working candidate.
        val now = System.currentTimeMillis()
        cache[videoId]?.let { hit ->
            if (hit.expiresAt > now && hit.streams.isNotEmpty()) return hit.streams
            cache.remove(videoId)
        }
        GuestSession.ensure()
        var resolution = resolveOnce(videoId, quality)
        // Bot detection: when no candidate URL passed validation, YouTube likely
        // flagged the guest identity — rotate it and retry once (mirrors the
        // Android BotDetectionMitigator).
        if (!resolution.anyValidated) {
            GuestSession.rotate()
            resolution = resolveOnce(videoId, quality)
        }
        // Last resort: transient failures can leave us with no candidates at all;
        // retry a couple of times with a short backoff before reporting failure.
        var attempts = 0
        while (resolution.streams.isEmpty() && attempts < 2) {
            delay(750L)
            resolution = resolveOnce(videoId, quality)
            attempts++
        }
        if (resolution.streams.isNotEmpty()) {
            val ttl = cacheTtlMs()
            val expiresAt = if (ttl == Long.MAX_VALUE) Long.MAX_VALUE else System.currentTimeMillis() + ttl
            cache[videoId] = Cached(resolution.streams, expiresAt)
            while (cache.size > CACHE_MAX_ENTRIES) {
                val oldest = cache.entries.minByOrNull { it.value.expiresAt }?.key ?: break
                cache.remove(oldest)
            }
        }
        return resolution.streams
    }

    private data class Resolution(val streams: List<ResolvedStream>, val anyValidated: Boolean)

    private suspend fun resolveOnce(videoId: String, quality: AudioQuality): Resolution {
        val candidates = mutableListOf<ResolvedStream>()

        // 1) NewPipe — handles the signature cipher and returns already-playable
        //    stream URLs when its extractor is not bot-blocked. These URLs are
        //    served to NewPipe's Firefox UA, so keep that UA for the download.
        val newPipeUrl = withContext(Dispatchers.IO) {
            runCatching {
                val urls = YouTube.getNewPipeStreamUrls(videoId)
                quality.preferredItags.firstNotNullOfOrNull { tag -> urls.firstOrNull { it.first == tag }?.second }
            }.getOrNull()
        }
        if (!newPipeUrl.isNullOrBlank()) {
            candidates += ResolvedStream(newPipeUrl, YouTubeClient.USER_AGENT_WEB)
        }

        // 2) Client chain: collect every resolved AAC URL, preferring the ones
        //    that pass HEAD validation. googlevideo sometimes rejects HEAD, so
        //    unvalidated URLs are still kept as a last resort.
        val validated = mutableListOf<ResolvedStream>()
        val unvalidated = mutableListOf<ResolvedStream>()
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

            val url = resolveFromResponse(response, quality.preferredItags, ytClient) ?: continue
            val durationMs = response.videoDetails?.lengthSeconds?.toDoubleOrNull()?.times(1000)?.toLong()
            val stream = ResolvedStream(url, ytClient.userAgent, durationMs)
            val seen = (candidates + validated + unvalidated).any { it.url == url }
            if (seen) continue
            if (validateUrl(url, ytClient.userAgent)) validated += stream else unvalidated += stream
        }

        return Resolution(candidates + validated + unvalidated, validated.isNotEmpty())
    }

    /** Picks the best AAC format from a successful player response and resolves its URL. */
    private fun resolveFromResponse(response: PlayerResponse, preferredItags: List<Int>, ytClient: YouTubeClient): String? {
        val adaptive = response.streamingData?.adaptiveFormats ?: return null
        // Only AAC-LC (codec mp4a.40.2, itags 140/141): the JAAD decoder handles
        // AAC-LC, but fails on HE-AAC/SBR (mp4a.40.5, e.g. itag 139) with a
        // "FIL element overread" error. Order by the requested quality's itags.
        val aacLc = adaptive.filter { it.isAudio && it.isOriginal && it.mimeType.contains("mp4a.40.2") }
        val format = preferredItags.firstNotNullOfOrNull { tag -> aacLc.firstOrNull { it.itag == tag } }
            ?: aacLc.firstOrNull()
            ?: return null

        val raw = when {
            !format.url.isNullOrEmpty() -> format.url
            else -> format.signatureCipher ?: format.cipher
        } ?: return null

        val url = if (raw.startsWith("http")) raw else YouTubeExtractor.decryptUrl(raw)
        if (url.isNullOrBlank()) return null

        // Apply the n-parameter transform only for web clients, matching the
        // mobile app. Transforming Android/iOS/VisionOS URLs with the web
        // player's throttle deobfuscator corrupts their `n` param and googlevideo
        // then answers 403.
        return if (ytClient.useWebPoTokens) {
            runCatching { YouTubeExtractor.deobfuscateUrlNParam(url) }.getOrDefault(url)
        } else {
            url
        }
    }

    /** Best-effort HEAD validation of a resolved stream URL. */
    private fun validateUrl(url: String, userAgent: String): Boolean = runCatching {
        val request = Request.Builder()
            .head()
            .url(url)
            .header("User-Agent", userAgent)
            .build()
        client.newCall(request).execute().use { it.isSuccessful }
    }.getOrDefault(false)
}
