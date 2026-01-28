package com.music.vivi.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import com.music.innertube.NewPipeUtils
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_CREATOR
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
import okhttp3.OkHttpClient
import timber.log.Timber

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .build()

    /**
     * The main client is used for metadata and initial streams.
     * Do not use other clients for this because it can result in inconsistent metadata.
     * For example other clients can have different normalization targets (loudnessDb).
     *
     * [com.music.innertube.models.YouTubeClient.WEB_REMIX] should be preferred here because currently it is the only client which provides:
     * - the correct metadata (like loudnessDb)
     * - premium formats
     */
    private val MAIN_CLIENT: YouTubeClient = ANDROID_VR_1_43_32

    /**
     * Clients used for fallback streams in case the streams of the main client do not work.
     */
    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        ANDROID_VR_1_61_48,
        WEB_REMIX,
        ANDROID_CREATOR,
        IPADOS,
        ANDROID_VR_NO_AUTH,
        MOBILE,
        TVHTML5,
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        IOS,
        WEB,
        WEB_CREATOR
    )
    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )

    /**
     * Custom player response intended to use for playback.
     * Metadata like audioConfig and videoDetails are from [MAIN_CLIENT].
     * Format & stream can be from [MAIN_CLIENT] or [STREAM_FALLBACK_CLIENTS].
     */
    /**
     * Retrieves the [PlaybackData] required to play a song/video.
     *
     * This method implements a robust fallback mechanism to find a working stream:
     * 1.  **Main Client**: Attempts to fetch data using the [MAIN_CLIENT] (usually Android VR) to get accurate metadata and volume normalization.
     * 2.  **Fallback Clients**: If the main client fails (or stream is invalid), iterates through [STREAM_FALLBACK_CLIENTS] (Web, iOS, TV, etc.).
     * 3.  **Validation**: Validates the stream URL via a HEAD request to ensure it's accessible.
     *
     * @param videoId The YouTube video ID.
     * @param playlistId Optional context playlist ID.
     * @param audioQuality The desired [AudioQuality].
     * @param connectivityManager System connectivity manager for network checks.
     * @param httpClient The OkHttpClient used for validation requests.
     * @return A [Result] containing the [PlaybackData] on success, or an exception on failure.
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        httpClient: OkHttpClient,
    ): Result<PlaybackData> = runCatching {
        Timber.tag(logTag).d("Fetching player response for videoId: $videoId, playlistId: $playlistId")
        /**
         * This is required for some clients to get working streams however
         * it should not be forced for the [MAIN_CLIENT] because the response of the [MAIN_CLIENT]
         * is required even if the streams won't work from this client.
         * This is why it is allowed to be null.
         */
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        Timber.tag(logTag).d("Signature timestamp: $signatureTimestamp")

        val isLoggedIn = YouTube.cookie != null
        val sessionId =
            if (isLoggedIn) {
                // signed in sessions use dataSyncId as identifier
                YouTube.dataSyncId
            } else {
                // signed out sessions use visitorData as identifier
                YouTube.visitorData
            }
        Timber.tag(logTag).d("Session authentication status: ${if (isLoggedIn) "Logged in" else "Not logged in"}")

        Timber.tag(logTag).d("Attempting to get player response using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        val mainPlayerResponse =
            YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp).getOrThrow()
        val audioConfig = mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = mainPlayerResponse.videoDetails
        val playbackTracking = mainPlayerResponse.playbackTracking
        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null

        for (clientIndex in (-1 until STREAM_FALLBACK_CLIENTS.size)) {
            // reset for each client
            format = null
            streamUrl = null
            streamExpiresInSeconds = null

            // decide which client to use for streams and load its player response
            val client: YouTubeClient
            if (clientIndex == -1) {
                // try with streams from main client first
                client = MAIN_CLIENT
                streamPlayerResponse = mainPlayerResponse
                Timber.tag(logTag).d("Trying stream from MAIN_CLIENT: ${client.clientName}")
            } else {
                // after main client use fallback clients
                client = STREAM_FALLBACK_CLIENTS[clientIndex]
                Timber.tag(
                    logTag
                ).d("Trying fallback client ${clientIndex + 1}/${STREAM_FALLBACK_CLIENTS.size}: ${client.clientName}")

                if (client.loginRequired && !isLoggedIn && YouTube.cookie == null) {
                    // skip client if it requires login but user is not logged in
                    Timber.tag(
                        logTag
                    ).d("Skipping client ${client.clientName} - requires login but user is not logged in")
                    continue
                }

                Timber.tag(logTag).d("Fetching player response for fallback client: ${client.clientName}")
                streamPlayerResponse =
                    YouTube.player(videoId, playlistId, client, signatureTimestamp).getOrNull()
            }

            // process current client response
            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                Timber.tag(
                    logTag
                ).d(
                    "Player response status OK for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}"
                )

                format =
                    findFormat(
                        streamPlayerResponse,
                        audioQuality,
                        connectivityManager
                    )

                if (format == null) {
                    Timber.tag(
                        logTag
                    ).d(
                        "No suitable format found for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}"
                    )
                    continue
                }

                Timber.tag(logTag).d("Format found: ${format.mimeType}, bitrate: ${format.bitrate}")

                streamUrl = findUrlOrNull(format, videoId)
                if (streamUrl == null) {
                    Timber.tag(logTag).d("Stream URL not found for format")
                    continue
                }

                streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds
                if (streamExpiresInSeconds == null) {
                    Timber.tag(logTag).d("Stream expiration time not found")
                    continue
                }

                Timber.tag(logTag).d("Stream expires in: $streamExpiresInSeconds seconds")

                if (clientIndex == STREAM_FALLBACK_CLIENTS.size - 1) {
                    /** skip [validateStatus] for last client */
                    Timber.tag(
                        logTag
                    ).d(
                        "Using last fallback client without validation: ${STREAM_FALLBACK_CLIENTS[clientIndex].clientName}"
                    )
                    break
                }

                if (validateStatus(streamUrl, httpClient)) {
                    // working stream found
                    Timber.tag(
                        logTag
                    ).d(
                        "Stream validated successfully with client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}"
                    )
                    break
                } else {
                    Timber.tag(
                        logTag
                    ).d(
                        "Stream validation failed for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}"
                    )
                }
            } else {
                Timber.tag(
                    logTag
                ).d(
                    "Player response status not OK: ${streamPlayerResponse?.playabilityStatus?.status}, reason: ${streamPlayerResponse?.playabilityStatus?.reason}"
                )
            }
        }

        if (streamPlayerResponse == null) {
            Timber.tag(logTag).e("Bad stream player response - all clients failed")
            throw Exception("Bad stream player response")
        }

        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            val errorReason = streamPlayerResponse.playabilityStatus.reason
            Timber.tag(logTag).e("Playability status not OK: $errorReason")
            throw PlaybackException(
                errorReason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        if (streamExpiresInSeconds == null) {
            Timber.tag(logTag).e("Missing stream expire time")
            throw Exception("Missing stream expire time")
        }

        if (format == null) {
            Timber.tag(logTag).e("Could not find format")
            throw Exception("Could not find format")
        }

        if (streamUrl == null) {
            Timber.tag(logTag).e("Could not find stream url")
            throw Exception("Could not find stream url")
        }

        Timber.tag(
            logTag
        ).d("Successfully obtained playback data with format: ${format.mimeType}, bitrate: ${format.bitrate}")
        PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds
        )
    }

    /**
     * Simple player response intended to use for metadata only.
     * Stream URLs of this response might not work so don't use them.
     */
    /**
     * Fetches metadata-only player response (lighter weight than full playback response).
     *
     * Use this when you only need metadata (title, artist, duration, related songs) and not the actual audio stream.
     * Uses `WEB_REMIX` client for best metadata accuracy.
     *
     * @param videoId The YouTube video ID.
     * @param playlistId Optional context playlist ID.
     * @return A [Result] containing the [PlayerResponse].
     */
    suspend fun playerResponseForMetadata(videoId: String, playlistId: String? = null): Result<PlayerResponse> {
        Timber.tag(
            logTag
        ).d("Fetching metadata-only player response for videoId: $videoId using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        return YouTube.player(videoId, playlistId, client = WEB_REMIX) // ANDROID_VR does not work with history
            .onSuccess { Timber.tag(logTag).d("Successfully fetched metadata") }
            .onFailure { Timber.tag(logTag).e(it, "Failed to fetch metadata") }
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? {
        Timber.tag(
            logTag
        ).d(
            "Finding format with audioQuality: $audioQuality, network metered: ${connectivityManager.isActiveNetworkMetered}"
        )

        val format = playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio && it.isOriginal }
            ?.maxByOrNull {
                it.bitrate * when (audioQuality) {
                    // Update Auto to always use best available (prioritize high bitrate/opus)
                    AudioQuality.AUTO -> {
                        if (it.itag == 774) 100 else 2
                    }
                    AudioQuality.HIGH -> 1
                    AudioQuality.VERY_HIGH -> {
                        // Prioritize ITAG 774 (256kbps Opus) slightly more than others, but generally prefer higher bitrate
                        if (it.itag == 774) 100 else 2
                    }
                    AudioQuality.LOW -> -1
                } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) // prefer opus stream
            }

        if (format != null) {
            Timber.tag(logTag).d("Selected format: ${format.mimeType}, bitrate: ${format.bitrate}")
        } else {
            Timber.tag(logTag).d("No suitable audio format found")
        }

        return format
    }

    /**
     * Checks if the stream url returns a successful status.
     * If this returns true the url is likely to work.
     * If this returns false the url might cause an error during playback.
     */
    private fun validateStatus(url: String, httpClient: OkHttpClient): Boolean {
        Timber.tag(logTag).d("Validating stream URL status")
        try {
            val requestBuilder = okhttp3.Request.Builder()
                .head()
                .url(url)
            val response = httpClient.newCall(requestBuilder.build()).execute()
            val isSuccessful = response.isSuccessful
            Timber.tag(
                logTag
            ).d("Stream URL validation result: ${if (isSuccessful) "Success" else "Failed"} (${response.code})")
            return isSuccessful
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "Stream URL validation failed with exception")
            reportException(e)
        }
        return false
    }

    /**
     * Wrapper around the [NewPipeUtils.getSignatureTimestamp] function which reports exceptions
     */
    private fun getSignatureTimestampOrNull(videoId: String): Int? {
        Timber.tag(logTag).d("Getting signature timestamp for videoId: $videoId")
        return NewPipeUtils.getSignatureTimestamp(videoId)
            .onSuccess { Timber.tag(logTag).d("Signature timestamp obtained: $it") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get signature timestamp")
                reportException(it)
            }
            .getOrNull()
    }

    /**
     * Wrapper around the [NewPipeUtils.getStreamUrl] function which reports exceptions
     */
    private fun findUrlOrNull(format: PlayerResponse.StreamingData.Format, videoId: String): String? {
        Timber.tag(logTag).d("Finding stream URL for format: ${format.mimeType}, videoId: $videoId")
        return NewPipeUtils.getStreamUrl(format, videoId)
            .onSuccess { Timber.tag(logTag).d("Stream URL obtained successfully") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get stream URL")
                reportException(it)
            }
            .getOrNull()
    }

    /**
     * Fetches video stream URL for playing music videos.
     * Returns the best quality video format available with audio.
     *
     * @param videoId The YouTube video ID.
     * @param videoQuality The desired video quality.
     * @param wifiFastMode Whether to enable ultra-fast loading on WiFi.
     * @return A [Result] containing the video stream URL, or null if not available.
     */
    suspend fun getVideoStreamUrl(videoId: String, videoQuality: com.music.vivi.constants.VideoQuality = com.music.vivi.constants.VideoQuality.AUTO, wifiFastMode: Boolean = false): Result<String?> = runCatching {
        Timber.tag(logTag).d("Fetching video stream for videoId: $videoId")
        
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        
        // Try main client first
        var playerResponse = YouTube.player(videoId, null, MAIN_CLIENT, signatureTimestamp).getOrNull()
        
        // Fallback to WEB_REMIX if main client fails
        if (playerResponse == null || playerResponse.playabilityStatus?.status != "OK") {
            Timber.tag(logTag).d("Main client failed, trying WEB_REMIX")
            playerResponse = YouTube.player(videoId, null, WEB_REMIX, signatureTimestamp).getOrNull()
        }
        
        if (playerResponse?.playabilityStatus?.status != "OK") {
            Timber.tag(logTag).d("Video not available: ${playerResponse?.playabilityStatus?.reason}")
            return@runCatching null
        }
        
        // Filter video formats based on selected quality
        fun filterFormatsByQuality(formats: List<com.music.innertube.models.response.PlayerResponse.StreamingData.Format>?): List<com.music.innertube.models.response.PlayerResponse.StreamingData.Format>? {
            if (videoQuality == com.music.vivi.constants.VideoQuality.AUTO) return formats
            
            return formats?.filter { format ->
                when (videoQuality) {
                    com.music.vivi.constants.VideoQuality.P144 -> format.qualityLabel?.contains("144p", ignoreCase = true) == true
                    com.music.vivi.constants.VideoQuality.P240 -> format.qualityLabel?.contains("240p", ignoreCase = true) == true
                    com.music.vivi.constants.VideoQuality.P360 -> format.qualityLabel?.contains("360p", ignoreCase = true) == true
                    com.music.vivi.constants.VideoQuality.P480 -> format.qualityLabel?.contains("480p", ignoreCase = true) == true
                    com.music.vivi.constants.VideoQuality.P720 -> format.qualityLabel?.contains("720p", ignoreCase = true) == true
                    com.music.vivi.constants.VideoQuality.P1080 -> format.qualityLabel?.contains("1080p", ignoreCase = true) == true
                    com.music.vivi.constants.VideoQuality.P1440 -> format.qualityLabel?.contains("1440p", ignoreCase = true) == true
                    com.music.vivi.constants.VideoQuality.P2160 -> format.qualityLabel?.contains("2160p", ignoreCase = true) == true
                    else -> true
                }
            }
        }
        
        // Get video+audio formats (muxed streams) or best video format
        val videoFormats = filterFormatsByQuality(playerResponse.streamingData?.formats?.filter { format ->
            format.mimeType?.startsWith("video/") == true
        })
        
        val adaptiveVideoFormats = playerResponse.streamingData?.adaptiveFormats?.filter { format ->
            format.mimeType?.startsWith("video/") == true
        }
        
        // Prefer muxed streams (video+audio in one) for simplicity
        val bestFormat = if (wifiFastMode) {
            // When WiFi fast mode is enabled, prioritize higher quality formats even if they have higher bitrate
            val allFormats = videoFormats?.filter { it.mimeType?.contains("mp4") == true } ?: emptyList()
            val adaptiveFormats = adaptiveVideoFormats?.filter { it.mimeType?.contains("mp4") == true } ?: emptyList()
            
            // Combine all formats and select the highest quality available
            (allFormats + adaptiveFormats)
                .maxByOrNull { format ->
                    // Prioritize quality label first, then bitrate
                    val qualityPriority = when {
                        format.qualityLabel?.contains("1080p", ignoreCase = true) == true -> 10
                        format.qualityLabel?.contains("720p", ignoreCase = true) == true -> 9
                        format.qualityLabel?.contains("480p", ignoreCase = true) == true -> 8
                        format.qualityLabel?.contains("360p", ignoreCase = true) == true -> 7
                        format.qualityLabel?.contains("240p", ignoreCase = true) == true -> 6
                        format.qualityLabel?.contains("144p", ignoreCase = true) == true -> 5
                        else -> 0
                    }
                    qualityPriority * 1000000 + (format.bitrate ?: 0)
                }
        } else {
            // Normal mode - prefer muxed streams first
            videoFormats
                ?.filter { it.mimeType?.contains("mp4") == true }
                ?.maxByOrNull { it.bitrate ?: 0 }
                ?: adaptiveVideoFormats
                    ?.filter { it.mimeType?.contains("mp4") == true }
                    ?.maxByOrNull { it.bitrate ?: 0 }
        }
        
        if (bestFormat == null) {
            Timber.tag(logTag).d("No suitable video format found")
            return@runCatching null
        }
        
        Timber.tag(logTag).d("Selected video format: ${bestFormat.mimeType}, quality: ${bestFormat.qualityLabel}, bitrate: ${bestFormat.bitrate}")
        
        val videoUrl = findUrlOrNull(bestFormat, videoId)
        if (videoUrl != null) {
            Timber.tag(logTag).d("Successfully obtained video stream URL")
        } else {
            Timber.tag(logTag).d("Failed to extract video URL from format")
        }
        
        videoUrl
    }
}
