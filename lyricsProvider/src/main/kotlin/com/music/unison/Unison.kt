package com.music.unison

import com.music.unison.models.LyricsData
import com.music.unison.models.LyricsResponse
import com.music.unison.models.SearchResponse
import com.music.vivi.betterlyrics.TTMLParser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import kotlin.math.abs

/**
 * Unison Lyrics Library
 * Crowdsourced lyrics API: https://unison.boidu.dev
 * Data licensed under ODbL-1.0. Attribution required:
 * "Lyrics from Unison (https://unison.boidu.dev)"
 */
object Unison {

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }
            defaultRequest {
                url("https://unison.boidu.dev")
            }
            expectSuccess = false
        }
    }

    // ─── Title / artist cleanup ───────────────────────────────────────────────

    private val titleCleanupPatterns = listOf(
        Regex("""^\s*[\[(【《〈＜].*?[\])】》〉＞]\s*"""),
        Regex("""\s*[\[(【《〈＜].*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?[\])】》〉＞]""", RegexOption.IGNORE_CASE),
        Regex("""\s*\|.*$"""),
        Regex("""\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
    )

    private val artistSeparators =
        listOf(" & ", " and ", ", ", " x ", " X ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ")

    private fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        for (pattern in titleCleanupPatterns) cleaned = cleaned.replace(pattern, "")
        return cleaned.trim()
    }

    private fun cleanArtist(artist: String): String {
        var cleaned = artist.trim()
        for (sep in artistSeparators) {
            if (cleaned.contains(sep, ignoreCase = true)) {
                cleaned = cleaned.split(sep, ignoreCase = true, limit = 2)[0]
                break
            }
        }
        return cleaned.trim()
    }

    // ─── API calls ───────────────────────────────────────────────────────────

    /** Fetch the single best matching lyric directly from /lyrics. */
    private suspend fun fetchBest(
        videoId: String? = null,
        song: String? = null,
        artist: String? = null,
        album: String? = null,
        duration: Int? = null,
    ): LyricsData? = runCatching {
        client.get("/lyrics") {
            if (videoId != null) parameter("v", videoId)
            if (song != null) parameter("song", song)
            if (artist != null) parameter("artist", artist)
            if (album != null) parameter("album", album)
            if (duration != null && duration > 0) parameter("duration", duration)
        }.body<LyricsResponse>().takeIf { it.success }?.data
    }.getOrNull()

    /** Fetch all matching results from /lyrics/search (sorted by score desc). */
    private suspend fun search(
        song: String? = null,
        artist: String? = null,
        album: String? = null,
        duration: Int? = null,
    ): List<LyricsData> = runCatching {
        client.get("/lyrics/search") {
            if (song != null) parameter("song", song)
            if (artist != null) parameter("artist", artist)
            if (album != null) parameter("album", album)
            if (duration != null && duration > 0) parameter("duration", duration)
        }.body<SearchResponse>().takeIf { it.success }?.data ?: emptyList()
    }.getOrDefault(emptyList())

    // ─── Format helpers ──────────────────────────────────────────────────────

    /**
     * Converts raw lyrics text to the format the player expects.
     * - TTML → parsed & converted to LRC+word-timing via TTMLParser
     * - LRC / plain → returned as-is
     */
    private fun convertLyrics(raw: String, format: String?): String {
        val isTtml = format?.lowercase() == "ttml" || raw.trimStart().startsWith("<tt", ignoreCase = true)
        return if (isTtml) {
            val parsed = TTMLParser.parseTTML(raw)
            if (parsed.isNotEmpty()) TTMLParser.toLRC(parsed) else raw
        } else {
            raw
        }
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Returns the best matching lyrics text, trying several fallback strategies.
     * Format preference: synced (LRC) > plain text.
     */
    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        videoId: String? = null,
    ): Result<String> = runCatching {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        val effectiveDuration = duration.takeIf { it > 0 }

        // Strategy 1: direct best-match via /lyrics (uses videoId if available)
        fetchBest(
            videoId = videoId,
            song = cleanedTitle,
            artist = cleanedArtist,
            album = album,
            duration = effectiveDuration,
        )?.let { item ->
            val raw = item.lyrics
            if (!raw.isNullOrBlank()) return@runCatching convertLyrics(raw, item.format)
        }

        // Strategy 2: search with cleaned metadata
        val results = search(
            song = cleanedTitle,
            artist = cleanedArtist,
            album = album,
            duration = effectiveDuration,
        ).filter { it.lyrics != null }

        val best = pickBest(results, cleanedTitle, cleanedArtist, duration)
        val bestLyrics = best?.lyrics ?: throw IllegalStateException("Lyrics unavailable")
        convertLyrics(bestLyrics, best.format)
    }

    /**
     * Iterates all viable results and delivers each lyrics string to [callback].
     * Useful for the "pick best" editor mode in the app.
     */
    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        videoId: String? = null,
        callback: (String) -> Unit,
    ) {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        val effectiveDuration = duration.takeIf { it > 0 }

        // Collect from /lyrics/search
        val results = search(
            song = cleanedTitle,
            artist = cleanedArtist,
            album = album,
            duration = effectiveDuration,
        ).filter { it.lyrics != null }

        val sorted = sortResults(results, cleanedTitle, cleanedArtist, duration)
        var count = 0
        for (item in sorted) {
            currentCoroutineContext().ensureActive()
            if (count >= 5) break
            val raw = item.lyrics ?: continue
            if (raw.isNotBlank()) {
                callback(convertLyrics(raw, item.format))
                count++
            }
        }
    }

    // ─── Ranking helpers ─────────────────────────────────────────────────────

    private fun pickBest(
        results: List<LyricsData>,
        title: String,
        artist: String,
        duration: Int,
    ): LyricsData? {
        if (results.isEmpty()) return null
        return sortResults(results, title, artist, duration).firstOrNull()
    }

    private fun sortResults(
        results: List<LyricsData>,
        title: String,
        artist: String,
        duration: Int,
    ): List<LyricsData> {
        return results.sortedByDescending { item ->
            var score = (item.effectiveScore ?: item.score ?: 0.0) * 0.4

            val titleSim = stringSimilarity(title, item.song ?: "")
            val artistSim = stringSimilarity(artist, item.artist ?: "")
            score += (titleSim + artistSim) / 2.0 * 0.4

            // Prefer synced lyrics
            if (item.syncType == "linesync" || item.syncType == "richsync") score += 0.1

            // Duration proximity bonus (±2 s full, ±10 s partial)
            if (duration > 0 && item.duration != null) {
                val diff = abs(item.duration - duration)
                score += when {
                    diff <= 2  -> 0.1
                    diff <= 10 -> 0.05
                    else       -> 0.0
                }
            }

            score
        }
    }

    private fun stringSimilarity(a: String, b: String): Double {
        val s1 = a.trim().lowercase()
        val s2 = b.trim().lowercase()
        return when {
            s1 == s2                    -> 1.0
            s1.isEmpty() || s2.isEmpty() -> 0.0
            s1.contains(s2) || s2.contains(s1) -> 0.8
            else -> {
                val maxLen = maxOf(s1.length, s2.length)
                1.0 - (levenshtein(s1, s2).toDouble() / maxLen)
            }
        }
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[a.length][b.length]
    }
}
