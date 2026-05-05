package com.music.paxsenix

import android.content.Context
import com.music.paxsenix.models.LyricsResponse
import com.music.paxsenix.models.SearchResponse
import com.music.paxsenix.models.SearchResult
import com.music.vivi.betterlyrics.TTMLParser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.Locale
import kotlin.math.abs

object Paxsenix {
    @Volatile
    private var client: HttpClient? = null
    private var appVersion: String = "Unknown"

    fun init(context: Context) {
        if (client != null) return

        synchronized(this) {
            if (client != null) return

            appVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    ?: "Unknown"
            } catch (e: Exception) {
                Timber.e(e, "Failed to get app version")
                "Unknown"
            }

            Timber.d("Initializing Paxsenix with version: $appVersion")

            client = HttpClient(CIO) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 15000
                    connectTimeoutMillis = 10000
                }
                install(ContentNegotiation) {
                    json(
                        Json {
                            isLenient = true
                            ignoreUnknownKeys = true
                        },
                    )
                }

                defaultRequest {
                    url("https://lyrics.paxsenix.org")
                    header("User-Agent", "ViviMusic/$appVersion")
                }

                expectSuccess = true
            }

            Timber.d("Paxsenix HTTP client initialized")
        }
    }

    private val httpClient: HttpClient
        get() = client ?: throw IllegalStateException("Paxsenix.init() must be called before using Paxsenix")

    private val titleCleanupPatterns = listOf(
        Regex("""\s*\(.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\]""", RegexOption.IGNORE_CASE),
        Regex("""\s*【.*?】"""),
        Regex("""\s*\|.*$"""),
        Regex("""\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\([^)]*\d{4}[^)]*\)""", RegexOption.IGNORE_CASE),
    )

    private val artistSeparators = listOf(" & ", " and ", ", ", " x ", " X ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ")

    private fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        for (pattern in titleCleanupPatterns) {
            cleaned = cleaned.replace(pattern, "")
        }
        return cleaned.trim()
    }

    private fun cleanArtist(artist: String): String {
        var cleaned = artist.trim()
        for (separator in artistSeparators) {
            if (cleaned.contains(separator, ignoreCase = true)) {
                cleaned = cleaned.split(separator, ignoreCase = true, limit = 2)[0]
                break
            }
        }
        return cleaned.trim()
    }

    private suspend fun search(query: String): List<SearchResult> = runCatching {
        Timber.d("Searching for: $query")
        val response = httpClient.get("/apple-music/search") {
            parameter("q", query)
        }.body<SearchResponse>()

        Timber.d("Search results count: ${response.size}")
        response
    }.getOrElse { e ->
        Timber.e(e, "Search error: ${e.message}")
        emptyList()
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ): Result<String> = runCatching {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)

        Timber.d("getLyrics: title='$title', artist='$artist', duration=$duration")

        val searchQueries = buildList {
            add("$cleanedTitle $cleanedArtist")
            add(cleanedTitle)
            if (!album.isNullOrBlank()) {
                add("$cleanedTitle $cleanedArtist $album")
            }
        }

        var allResults: List<Pair<SearchResult, Double>> = emptyList()

        for (query in searchQueries) {
            if (allResults.isEmpty()) {
                val searchResults = search(query)
                if (searchResults.isNotEmpty()) {
                    allResults = scoreAndFilterResults(searchResults, title, artist, duration)
                }
            }
        }

        if (allResults.isEmpty()) {
            Timber.w("No tracks found for any query")
            throw IllegalStateException("No tracks found on Paxsenix")
        }

        var plainOrLineSyncFallback: String? = null
        for ((result, score) in allResults.take(10)) {
            Timber.d("Trying: ${result.displayName} (ID: ${result.id}, score: $score)")
            val (lrc, hasWordTimings) = fetchLyricsForTrackWithType(result.id)
            if (lrc.isEmpty()) continue
            if (hasWordTimings) {
                return Result.success(lrc)
            }
            if (plainOrLineSyncFallback == null) {
                plainOrLineSyncFallback = lrc
            }
        }
        plainOrLineSyncFallback?.let {
            Timber.d("Using Paxsenix lyrics without word-level sync")
            return Result.success(it)
        }
        Timber.w("No lyrics content from Paxsenix for matched tracks")
        return Result.failure(IllegalStateException("No lyrics available from Paxsenix"))
    }

    private fun scoreAndFilterResults(
        results: List<SearchResult>,
        title: String,
        artist: String,
        duration: Int
    ): List<Pair<SearchResult, Double>> {
        val durationMs = duration * 1000
        val cleanupRegex = Regex("""\s*\(.*?\)|\s*\[.*?\]""")

        val cleanedTitle = title.replace(cleanupRegex, "").lowercase().trim()
        val cleanedArtist = cleanArtist(artist).lowercase()

        val targetIsMixed = title.contains("mixed", ignoreCase = true)
        val targetIsRemix = title.contains("remix", ignoreCase = true)

        return results.map { result ->
            var score = 0.0

            result.duration?.let { d ->
                val diff = abs(d - durationMs)
                when {
                    diff <= 2000 -> score += 100
                    diff <= 5000 -> score += 50
                    diff <= 10000 -> score += 10
                    else -> score -= 50
                }
            }

            val resultTitleCleaned = result.displayName.replace(cleanupRegex, "").lowercase().trim()
            when {
                resultTitleCleaned == cleanedTitle -> score += 80
                resultTitleCleaned.contains(cleanedTitle) || cleanedTitle.contains(resultTitleCleaned) -> score += 40
            }

            val resultIsMixed = result.displayName.contains("mixed", ignoreCase = true)
            val resultIsRemix = result.displayName.contains("remix", ignoreCase = true)
            if (resultIsMixed && !targetIsMixed) score -= 60
            if (resultIsRemix && !targetIsRemix) score -= 40

            val resultArtistLower = result.displayArtist.lowercase()
            when {
                resultArtistLower.contains(cleanedArtist) -> score += 50
                else -> {
                    val artistWords = cleanedArtist.split(Regex("\\s+")).filter { it.length > 2 }
                    if (artistWords.any { resultArtistLower.contains(it) }) score += 25
                }
            }

            result to score
        }.sortedByDescending { it.second }.filter { it.second > 0 }.take(10)
    }

    private suspend fun fetchLyricsForTrackWithType(id: String): Pair<String, Boolean> {
        val result = fetchLyricsForTrack(id)
        if (result.isSuccess) {
            val lrc = result.getOrNull()!!
            val hasWordTimings = lrc.contains("<") && lrc.contains(">")
            return lrc to hasWordTimings
        }
        return "" to false
    }

    private suspend fun fetchLyricsForTrack(id: String): Result<String> = runCatching {
        Timber.d("Fetching lyrics for track ID: $id")

        val response = httpClient.get("/apple-music/lyrics") {
            parameter("id", id)
        }.body<LyricsResponse>()

        val lyricsType = response.type
        Timber.d("Lyrics response: type=$lyricsType")

        if (!response.ttmlContent.isNullOrBlank()) {
            val lrc = convertTTMLToAppFormat(response.ttmlContent)
            if (lrc.isNotEmpty()) {
                Timber.d("Generated LRC from ttmlContent using TTMLParser")
                return@runCatching lrc
            }
        }

        if (!response.elrcMultiPerson.isNullOrBlank()) {
            Timber.d("Using elrcMultiPerson as fallback")
            return@runCatching response.elrcMultiPerson
        }
        if (!response.elrc.isNullOrBlank()) {
            Timber.d("Using elrc as fallback")
            return@runCatching response.elrc
        }
        if (!response.plain.isNullOrBlank()) {
            Timber.d("Using plain lyrics field")
            return@runCatching response.plain
        }

        if (response.content.isEmpty()) {
            throw IllegalStateException("No lyrics found")
        }

        val hasWordLevel = lyricsType == "Syllable"
        Timber.d("Using content array as source, hasWordLevel=$hasWordLevel")

        if (!hasWordLevel) {
            val plain = response.content
                .map { line -> line.text.joinToString(" ") { it.text } }
                .filter { it.isNotBlank() }
                .joinToString("\n")
            return@runCatching plain
        }

        val lrc = buildString {
            response.content.forEach { line ->
                val timeMs = line.timestamp
                val minutes = timeMs / 1000 / 60
                val seconds = (timeMs / 1000) % 60
                val centiseconds = (timeMs % 1000) / 10

                val agent = when {
                    line.background -> "{bg}"
                    line.oppositeTurn -> "{agent:v2}"
                    else -> "{agent:v1}"
                }

                val lineText = line.text.joinToString(" ") { it.text }

                if (lineText.isNotBlank()) {
                    appendLine(String.format(Locale.US, "[%02d:%02d.%02d]%s%s", minutes, seconds, centiseconds, agent, lineText))
                    if (line.text.isNotEmpty()) {
                        val wordsData = line.text.joinToString("|") { word ->
                            "${word.text}:${word.timestamp.toDouble() / 1000}:${word.endtime.toDouble() / 1000}"
                        }
                        if (wordsData.isNotEmpty()) appendLine("<$wordsData>")
                    }
                }
            }
        }

        return@runCatching lrc
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        callback: (String) -> Unit,
    ) {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)

        val searchQueries = listOf("$cleanedTitle $cleanedArtist", cleanedTitle)
        var plainFallback: String? = null
        var scoredResults: List<Pair<SearchResult, Double>> = emptyList()

        for (query in searchQueries) {
            val results = search(query)
            if (results.isEmpty()) continue
            val filtered = scoreAndFilterResults(results, title, artist, duration)
            if (filtered.isNotEmpty()) {
                scoredResults = filtered
                break
            }
        }

        for ((result, _) in scoredResults.take(3)) {
            Timber.d("Trying lyrics for: ${result.displayName}")
            val (lrc, hasWordTimings) = fetchLyricsForTrackWithType(result.id)
            if (lrc.isNotEmpty()) {
                if (hasWordTimings) {
                    callback(lrc)
                    return
                } else if (plainFallback == null) {
                    plainFallback = lrc
                }
            }
        }

        plainFallback?.let {
            Timber.d("Offering plain/non-synced lyrics as fallback")
            callback(it)
        }
    }

    private fun convertTTMLToAppFormat(ttml: String): String {
        return try {
            val parsedLines = TTMLParser.parseTTML(ttml)
            TTMLParser.toLRC(parsedLines)
        } catch (e: Exception) {
            Timber.e(e, "TTML conversion failed: ${e.message}")
            ""
        }
    }
}
