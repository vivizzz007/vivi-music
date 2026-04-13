package com.music.innertube.utils

import com.music.innertube.models.WatchEndpoint

/**
 * Utility class for parsing YouTube and YouTube Music URLs.
 * Extracts video IDs, playlist IDs, and creates WatchEndpoints from URLs.
 */
object YouTubeUrlParser {
    /**
     * Represents the type of YouTube link parsed.
     */
    sealed class ParsedUrl {
        abstract val id: String

        data class Video(
            override val id: String,
        ) : ParsedUrl()

        data class Artist(
            override val id: String,
        ) : ParsedUrl()
    }

    /**
     * Pattern for matching YouTube video URLs.
     */
    private val VIDEO_URL_PATTERNS =
        listOf(
            Regex("""(?:https?://)?(?:www\.)?(?:music\.)?youtube\.com/watch\?.*v=([a-zA-Z0-9_-]{11})"""),
            Regex("""(?:https?://)?(?:www\.)?(?:music\.)?youtube\.com/watch\?v=([a-zA-Z0-9_-]{11})"""),
            Regex("""(?:https?://)?youtu\.be/([a-zA-Z0-9_-]{11})"""),
            Regex("""(?:https?://)?(?:www\.)?youtube\.com/shorts/([a-zA-Z0-9_-]{11})"""),
        )

    /**
     * Pattern for matching YouTube Music artist URLs.
     */
    private val ARTIST_URL_PATTERNS =
        listOf(
            Regex("""(?:https?://)?(?:www\.)?music\.youtube\.com/channel/([a-zA-Z0-9_-]+)"""),
            Regex("""(?:https?://)?(?:www\.)?music\.youtube\.com/browse/(MPRE[a-zA-Z0-9_-]+)"""),
        )

    /**
     * Checks if the given text is a YouTube URL.
     */
    fun isYouTubeUrl(text: String): Boolean = parse(text) != null

    /**
     * Parses a YouTube URL and returns the parsed result.
     *
     * @param url The URL to parse
     * @return ParsedUrl if valid, null otherwise
     */
    fun parse(url: String): ParsedUrl? {
        val trimmedUrl = url.trim()
        println("[LINK_PARSE_DEBUG] Parsing URL: $trimmedUrl")

        // Check for video URLs
        for (pattern in VIDEO_URL_PATTERNS) {
            pattern.find(trimmedUrl)?.let { matchResult ->
                matchResult.groupValues.getOrNull(1)?.let { videoId ->
                    println("[LINK_PARSE_DEBUG] Detected Video ID: $videoId")
                    return ParsedUrl.Video(videoId)
                }
            }
        }

        // Check for artist URLs
        if (trimmedUrl.contains("music.youtube.com")) {
            for (pattern in ARTIST_URL_PATTERNS) {
                pattern.find(trimmedUrl)?.let { matchResult ->
                    matchResult.groupValues.getOrNull(1)?.let { artistId ->
                        println("[LINK_PARSE_DEBUG] Detected Artist ID: $artistId")
                        return ParsedUrl.Artist(artistId)
                    }
                }
            }
        }

        println("[LINK_PARSE_DEBUG] No match found or type restricted")
        return null
    }

    /**
     * Extracts video ID from a YouTube URL.
     */
    fun extractVideoId(url: String): String? = (parse(url) as? ParsedUrl.Video)?.id

    /**
     * Creates a WatchEndpoint from a YouTube video URL.
     */
    fun createWatchEndpoint(url: String): WatchEndpoint? =
        extractVideoId(url)?.let { videoId ->
            WatchEndpoint(videoId = videoId)
        }
}
