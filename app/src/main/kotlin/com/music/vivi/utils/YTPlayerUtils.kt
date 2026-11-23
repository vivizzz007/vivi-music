/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

import android.net.ConnectivityManager
import android.util.Log
import androidx.media3.common.PlaybackException
import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_CREATOR
import com.music.vivi.utils.BotDetectionMitigator
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_43_32
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_61_48
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import com.music.innertube.models.YouTubeClient.Companion.IOS
import com.music.innertube.models.YouTubeClient.Companion.IPADOS
import com.music.innertube.models.YouTubeClient.Companion.MOBILE
import com.music.innertube.models.YouTubeClient.Companion.TVHTML5
import com.music.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.music.innertube.models.YouTubeClient.Companion.WEB
import com.music.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import com.music.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.music.innertube.models.response.PlayerResponse
import com.music.vivi.constants.AudioQuality
import com.music.vivi.constants.EnableSaavnStreamingKey
import com.music.vivi.constants.SaavnAudioQuality
import com.music.vivi.constants.SaavnAudioQualityKey
import com.music.vivi.utils.cipher.CipherDeobfuscator
import com.music.vivi.utils.sabr.EjsNTransformSolver
import com.music.vivi.utils.PlaybackLogLevel
import com.music.vivi.utils.PlaybackLogManager
import com.music.innertube.models.IpVersion
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YouTubeClient.Companion.VISIONOS
import com.music.innertube.strategy.ContentAwareFallbackStrategy
import com.music.innertube.strategy.ContentHints
import com.music.jiosaavn.SaavnService
import com.music.jiosaavn.cache.SaavnCache
import com.music.vivi.di.DatabaseEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.Dns
import okhttp3.OkHttpClient
import timber.log.Timber
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"
    private const val TAG = "YTPlayerUtils"

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val addresses = Dns.SYSTEM.lookup(hostname)
                return when (YouTube.ipVersion) {
                    IpVersion.IPV4 -> addresses.filter { it is Inet4Address }.ifEmpty { addresses }
                    IpVersion.IPV6 -> addresses.filter { it is Inet6Address }.ifEmpty { addresses }
                    IpVersion.AUTO -> addresses
                }
            }
        })
        .proxySelector(object : ProxySelector() {
            override fun select(uri: URI?): List<Proxy> = listOfNotNull(YouTube.proxy ?: Proxy.NO_PROXY)
            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                Timber.tag(TAG).e(ioe, "Proxy connection failed for URI: $uri")
            }
        })
        .proxyAuthenticator { _, response ->
            YouTube.proxyAuth?.let { auth ->
                response.request.newBuilder()
                    .header("Proxy-Authorization", auth)
                    .build()
            } ?: response.request
        }
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()



    /**
     * Client used for fast, low-latency stream resolution.
     * ANDROID_VR clients don't require PoToken and start instantly.
     * Note: ANDROID_VR has loginSupported=false, so metadata like audioConfig and
     * playbackTracking must be supplemented from an authenticated client (WEB_REMIX)
     * when the user is logged in.
     */
    private val MAIN_CLIENT: YouTubeClient = ANDROID_VR_1_43_32

    /**
     * Client used to fetch metadata (audioConfig, playbackTracking) when the user is
     * logged in. This ensures remote YouTube history is correctly updated.
     */
    private val METADATA_CLIENT: YouTubeClient = WEB_REMIX
    private val fallbackStrategy = ContentAwareFallbackStrategy()

    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        IOS,
        VISIONOS,
        ANDROID_VR_1_43_32,
        WEB_REMIX,
        TVHTML5,
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        ANDROID_CREATOR,
        WEB_CREATOR
    )

    /**
     * Custom player response intended to use for playback.
     * Stream URLs come from [MAIN_CLIENT] or [STREAM_FALLBACK_CLIENTS] for fast loading.
     * Metadata (audioConfig, playbackTracking) come from [METADATA_CLIENT] (WEB_REMIX)
     * when the user is logged in, to ensure remote history recording works correctly.
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        context: android.content.Context? = null,
        contentHints: ContentHints = ContentHints(),
    ): Result<InnerTubeXPlayer.PlaybackData> {
        // ── JioSaavn intercept ───────────────────────────────────────────────
        // If the user has enabled JioSaavn streaming, try to resolve the stream
        // URL from JioSaavn first. We fall through to YouTube on ANY failure so
        // the user always hears audio.
        if (context != null) {
            val saavnEnabled = context.dataStore.get(EnableSaavnStreamingKey, false)
            if (saavnEnabled) {
                Timber.tag(TAG).d("JioSaavn streaming enabled — trying Saavn for videoId=$videoId")
                val saavnResult = runCatching {
                    val qualityKey = context.dataStore.get(SaavnAudioQualityKey, SaavnAudioQuality.QUALITY_320.name)
                    val quality = runCatching { SaavnAudioQuality.valueOf(qualityKey) }
                        .getOrDefault(SaavnAudioQuality.QUALITY_320)

                    coroutineScope {
                        // Launch YouTube player metadata fetch concurrently for history/scrobbling
                        val metaDeferred = async {
                            playerResponseForMetadata(videoId, playlistId).getOrNull()
                        }

                        // 1. Fast Cache Check: If this videoId was already matched in-memory, reuse it (< 5ms)
                        var bestSong: com.music.jiosaavn.models.SaavnSong? = SaavnCache.getMatchedSong(videoId)

                        // 2. Fast local database lookup: Try reading track info locally in < 2ms
                        val database = runCatching {
                            val appContext = context.applicationContext ?: context
                            EntryPointAccessors.fromApplication(appContext, DatabaseEntryPoint::class.java).database()
                        }.getOrNull()

                        val localSong = runCatching { database?.song(videoId)?.firstOrNull() }.getOrNull()

                        if (bestSong == null) {
                            val title = localSong?.song?.title
                                ?.takeIf { it.isNotBlank() }
                                ?: metaDeferred.await()?.videoDetails?.title.orEmpty()

                            val artistNames: List<String> = if (!localSong?.artists.isNullOrEmpty()) {
                                localSong!!.artists.map { it.name }
                            } else {
                                listOf(metaDeferred.await()?.videoDetails?.author.orEmpty()).filter { it.isNotBlank() }
                            }

                            if (title.isBlank()) return@coroutineScope null

                            val expectedDuration = localSong?.song?.duration
                                ?: metaDeferred.await()?.videoDetails?.lengthSeconds?.toIntOrNull()
                            val albumName = localSong?.album?.title.orEmpty()
                            val wantedTitleLower = title.lowercase(java.util.Locale.US).trim()
                            val wantedArtistsLower = artistNames.map { it.lowercase(java.util.Locale.US).trim() }
                            val wantedExplicit = localSong?.song?.explicit ?: false
                            val wantedAlbumLower = albumName.lowercase(java.util.Locale.US).trim()

                            val primaryArtist = artistNames.firstOrNull().orEmpty()
                            val cleanQuery = "$title $primaryArtist".trim().replace(Regex("\\s+"), " ")
                            val fallbackQuery = "$title ${artistNames.joinToString(" ")}".trim().replace(Regex("\\s+"), " ")

                            suspend fun findMatch(searchQuery: String): com.music.jiosaavn.models.SaavnSong? {
                                if (searchQuery.isBlank()) return null
                                Timber.tag(TAG).d("Saavn: searching with query: \"$searchQuery\"")
                                val rawSongs = SaavnService.searchSongs(searchQuery).getOrNull() ?: return null
                                val songs = rawSongs.sortedWith(
                                    compareByDescending<com.music.jiosaavn.models.SaavnSong> { candidate ->
                                        val candidateAlbumName = candidate.album?.name
                                        if (wantedAlbumLower.isNotBlank() && candidateAlbumName != null) {
                                            candidateAlbumName.lowercase(java.util.Locale.US) == wantedAlbumLower
                                        } else {
                                            false
                                        }
                                    }.thenByDescending { candidate ->
                                        candidate.explicitContent == wantedExplicit
                                    }
                                )
                                return songs.firstOrNull { candidate ->
                                    if (candidate.isProOnly) {
                                        Timber.tag(TAG).d("Saavn: Candidate \"${candidate.name}\" skipped because it is Pro-Only")
                                        return@firstOrNull false
                                    }

                                    val candidateTitleLower = candidate.name.lowercase(java.util.Locale.US).trim()
                                    val candidateArtists = candidate.artists.all.map { it.name.lowercase(java.util.Locale.US).trim() }

                                    val titleMatches = candidateTitleLower == wantedTitleLower
                                    val artistMatches = candidateArtists.intersect(wantedArtistsLower.toSet()).isNotEmpty()

                                    val candidateAlbumName = candidate.album?.name
                                    val albumMatches = if (wantedAlbumLower.isNotBlank()) {
                                        candidateAlbumName?.lowercase(java.util.Locale.US) == wantedAlbumLower
                                    } else {
                                        true
                                    }

                                    val candDuration = candidate.duration
                                    val durationMatches = if (expectedDuration != null && candDuration != null) {
                                        java.lang.Math.abs(expectedDuration - candDuration) <= 12
                                    } else {
                                        true
                                    }

                                    val explicitMatches = candidate.explicitContent == wantedExplicit

                                    titleMatches && artistMatches && albumMatches && durationMatches && explicitMatches
                                }
                            }

                            bestSong = findMatch(cleanQuery)
                            if (bestSong == null && cleanQuery != fallbackQuery) {
                                Timber.tag(TAG).d("Saavn: clean query had no match, trying fallback: \"$fallbackQuery\"")
                                bestSong = findMatch(fallbackQuery)
                            }

                            if (bestSong != null) {
                                SaavnCache.putMatchedSong(videoId, bestSong)
                            }
                        }

                        if (bestSong == null) {
                            Timber.tag(TAG).d("Saavn: no matching candidate found — falling back to YT")
                            return@coroutineScope null
                        }

                        Timber.tag(TAG).i("Saavn: matched \"${bestSong.name}\" (id=${bestSong.id})")

                        // Step 3: Resolve stream URL (check cache first)
                        var streamUrl = SaavnCache.getStreamUrl(bestSong.id, quality.toApiValue())
                        if (streamUrl.isNullOrBlank()) {
                            streamUrl = SaavnService.selectBestUrl(bestSong.downloadUrl, quality.toApiValue())
                            if (streamUrl.isNullOrBlank()) {
                                streamUrl = SaavnService.getBestStreamUrl(bestSong.id, quality.toApiValue())
                            }
                            if (!streamUrl.isNullOrBlank()) {
                                SaavnCache.putStreamUrl(bestSong.id, quality.toApiValue(), streamUrl)
                            }
                        }

                        if (streamUrl.isNullOrBlank()) {
                            Timber.tag(TAG).d("Saavn: no stream URL for songId=${bestSong.id} — falling back to YT")
                            return@coroutineScope null
                        }

                        // Step 4: Calculate instantaneous content length without blocking HTTP ping
                        val bitrateBps = when (quality) {
                            SaavnAudioQuality.QUALITY_320 -> 320_000
                            SaavnAudioQuality.QUALITY_160 -> 160_000
                            SaavnAudioQuality.QUALITY_96  -> 96_000
                        }
                        val durSec = localSong?.song?.duration ?: bestSong.duration
                        val contentLength = durSec?.let { dur ->
                            (dur.toLong() * (bitrateBps / 8L))
                        }

                        // Await concurrent YouTube metadata for playbackTracking and audioConfig
                        val meta = metaDeferred.await()

                        Timber.tag(TAG).i("Saavn: streaming from JioSaavn (quality=${quality.toApiValue()}) resolved url in ultra-fast path for videoId=$videoId")

                        InnerTubeXPlayer.PlaybackData(
                            audioConfig      = meta?.playerConfig?.audioConfig,
                            videoDetails     = meta?.videoDetails,
                            playbackTracking = meta?.playbackTracking,
                            format           = PlayerResponse.StreamingData.Format(
                                itag             = when (quality) {
                                    SaavnAudioQuality.QUALITY_320 -> 141
                                    SaavnAudioQuality.QUALITY_160 -> 140
                                    SaavnAudioQuality.QUALITY_96  -> 139
                                },
                                url              = streamUrl,
                                mimeType         = "audio/mp4; codecs=\"mp4a.40.2\"",
                                bitrate          = bitrateBps,
                                width            = null,
                                height           = null,
                                contentLength    = contentLength,
                                quality          = quality.toApiValue(),
                                fps              = null,
                                qualityLabel     = null,
                                averageBitrate   = null,
                                audioQuality     = quality.toApiValue(),
                                approxDurationMs = null,
                                audioSampleRate  = null,
                                audioChannels    = null,
                                loudnessDb       = null,
                                lastModified     = null,
                                signatureCipher  = null,
                                cipher           = null,
                                audioTrack       = null,
                            ),
                            streamUrl              = streamUrl,
                            streamExpiresInSeconds = 3600,
                            streamClient           = "JioSaavn",
                            streamHeaders          = emptyMap(),
                            requireBoundedRange    = false,
                            rangeChunkSizeBytes    = 0L,
                            useRangeChunks         = false,
                            isSaavnStream          = true,
                        )
                    }
                }.getOrNull()

                if (saavnResult != null) {
                    return Result.success(saavnResult)
                }
                // Any exception or null → fall through to YouTube below
                Timber.tag(TAG).d("Saavn intercept failed or returned null — falling back to YouTube")
            }
        }
        // ── End JioSaavn intercept ───────────────────────────────────────────

        val firstAttempt = resolvePlaybackData(videoId, playlistId, audioQuality, connectivityManager, contentHints)
        
        if (firstAttempt.isFailure && YouTube.cookie == null) {
            Timber.tag(TAG).w("Playback failed for guest. Rotating session and retrying...")
            PlaybackLogManager.log(PlaybackLogLevel.BOT, "Playback failed for guest", "Triggering bot detection mitigation (rotating guest session)")
            BotDetectionMitigator.rotateGuestSession()
            val retryResult = resolvePlaybackData(videoId, playlistId, audioQuality, connectivityManager, contentHints)
            retryResult.onSuccess { BotDetectionMitigator.notifyPlaybackSuccess() }
            if (retryResult.isFailure) {
                val err = retryResult.exceptionOrNull()
                Timber.tag(TAG).e(err, "Stream resolution exhausted for $videoId after session rotation")
                PlaybackLogManager.log(
                    PlaybackLogLevel.ERROR,
                    "No playable stream",
                    "All clients failed for $videoId: ${err?.message ?: "unknown"}"
                )
            }
            return retryResult
        }
        
        firstAttempt.onSuccess { BotDetectionMitigator.notifyPlaybackSuccess() }
        return firstAttempt
    }

    private suspend fun resolvePlaybackData(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        contentHints: ContentHints = ContentHints(),
    ): Result<InnerTubeXPlayer.PlaybackData> {
        Timber.tag(logTag).d("Fetching player response for videoId: ${videoId} via InnerTubeX")
        PlaybackLogManager.log(PlaybackLogLevel.INFO, "Resolving playback data", "Video: ${videoId}")
        println("[PLAYBACK_DEBUG] resolvePlaybackData (InnerTubeX) called: videoId=${videoId}")
        
        val mappedHints = com.metrolist.innertubex.extraction.ContentHints(
            isKidsContent = contentHints.isKidsContent,
            isExplicit = contentHints.isExplicit,
            isLive = contentHints.isLive,
            isUploaded = contentHints.isUploaded
        )
        return InnerTubeXPlayer.playerResponseForPlayback(
            videoId = videoId,
            playlistId = playlistId,
            audioQuality = audioQuality,
            connectivityManager = connectivityManager,
            contentHints = mappedHints,
            allowBoundedRange = false
        )
    }

    /**
     * Simple player response intended to use for metadata only.
     * Stream URLs of this response might not work so don't use them.
     */
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        Timber.tag(logTag).d("Fetching metadata-only player response for videoId: $videoId")
        return YouTube.player(videoId, playlistId, client = WEB_REMIX) // ANDROID_VR does not work with history
            .onSuccess { Timber.tag(logTag).d("Successfully fetched metadata") }
            .onFailure { Timber.tag(logTag).e(it, "Failed to fetch metadata") }
    }
}
