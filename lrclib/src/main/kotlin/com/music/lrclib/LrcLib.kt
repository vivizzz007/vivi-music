package com.music.lrclib

import com.music.lrclib.models.Track
import com.music.lrclib.models.bestMatchingFor
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import kotlin.math.abs

object LrcLib {
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
                url("https://lrclib.net")
            }

            expectSuccess = true
        }
    }

    private val geniusClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }

            // Add user agent to appear more like a browser
            defaultRequest {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }

            expectSuccess = false // Don't throw on 4xx/5xx, we'll handle it
        }
    }

    // Existing LrcLib methods remain the same
    private suspend fun queryLyrics(
        artist: String,
        title: String,
        album: String? = null,
    ) = client
        .get("/api/search") {
            parameter("track_name", title)
            parameter("artist_name", artist)
            if (album != null) parameter("album_name", album)
        }.body<List<Track>>()
        .filter { it.syncedLyrics != null }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ) = runCatching {
        val tracks = queryLyrics(artist, title, album)

        val res = when {
            duration == -1 -> {
                tracks.bestMatchingFor(duration, title, artist)?.syncedLyrics?.let(LrcLib::Lyrics)
            }
            else -> {
                tracks.bestMatchingFor(duration)?.syncedLyrics?.let(LrcLib::Lyrics)
            }
        }

        if (res != null) {
            return@runCatching res.text
        } else {
            throw IllegalStateException("Lyrics unavailable")
        }
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        callback: (String) -> Unit,
    ) {
        val tracks = queryLyrics(artist, title, album)
        var count = 0
        var plain = 0

        val sortedTracks = when {
            duration == -1 -> {
                tracks.sortedByDescending { track ->
                    var score = 0.0

                    if (track.syncedLyrics != null) score += 1.0

                    val titleSimilarity = calculateStringSimilarity(title, track.trackName)
                    val artistSimilarity = calculateStringSimilarity(artist, track.artistName)
                    score += (titleSimilarity + artistSimilarity) / 2.0

                    score
                }
            }
            else -> {
                tracks.sortedBy { abs(it.duration.toInt() - duration) }
            }
        }

        sortedTracks.forEach { track ->
            currentCoroutineContext().ensureActive()
            if (count <= 4) {
                if (track.syncedLyrics != null && duration == -1) {
                    count++
                    track.syncedLyrics.let(callback)
                } else {
                    if (track.syncedLyrics != null && abs(track.duration.toInt() - duration) <= 2) {
                        count++
                        track.syncedLyrics.let(callback)
                    }
                    if (track.plainLyrics != null && abs(track.duration.toInt() - duration) <= 2 && plain == 0) {
                        count++
                        plain++
                        track.plainLyrics.let(callback)
                    }
                }
            }
        }
    }

    // NEW: Genius integration methods
    suspend fun searchGeniusSongs(
        artist: String,
        title: String
    ) = runCatching {
        val searchQuery = "$artist $title".replace(" ", "%20")
        val searchUrl = "https://genius.com/search?q=$searchQuery"

        val response = geniusClient.get(searchUrl)

        if (response.status.value in 200..299) {
            val html = response.bodyAsText()
            parseGeniusSearchResults(html)
        } else {
            emptyList<GeniusSong>()
        }
    }.getOrElse { emptyList() }

    suspend fun scrapeGeniusLyrics(url: String) = runCatching {
        val response = geniusClient.get(url)

        if (response.status.value in 200..299) {
            val html = response.bodyAsText()
            extractLyricsFromGeniusPage(html)
        } else {
            null
        }
    }.getOrNull()

    // NEW: Enhanced lyrics fetching with Genius fallback
    suspend fun getLyricsWithFallback(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ): LyricsResult {
        // Try LrcLib first (for synced lyrics)
        val lrcResult = getLyrics(title, artist, duration, album)
        if (lrcResult.isSuccess) {
            return LyricsResult.Success(
                lyrics = lrcResult.getOrThrow(),
                source = LyricsSource.LRCLIB,
                synced = true
            )
        }

        // LrcLib failed, try Genius fallback
        return try {
            val geniusSongs = searchGeniusSongs(artist, title)
            println("Found ${geniusSongs.size} Genius songs for '$title' by '$artist'") // Debug log

            val bestMatch = geniusSongs.firstOrNull { song ->
                val titleSim = calculateStringSimilarity(title, song.title)
                val artistSim = calculateStringSimilarity(artist, song.artist)
                println("Checking: '${song.title}' by '${song.artist}' - Title: $titleSim, Artist: $artistSim") // Debug log
                titleSim > 0.6 && artistSim > 0.6 // Lowered threshold for better matches
            }

            if (bestMatch != null) {
                println("Best match found: ${bestMatch.title} by ${bestMatch.artist}") // Debug log
                val lyrics = scrapeGeniusLyrics(bestMatch.url)
                if (lyrics != null && lyrics.isNotBlank()) {
                    LyricsResult.Success(
                        lyrics = lyrics,
                        source = LyricsSource.GENIUS,
                        synced = false
                    )
                } else {
                    println("Failed to scrape lyrics from: ${bestMatch.url}") // Debug log
                    LyricsResult.NotFound
                }
            } else {
                println("No suitable match found in Genius results") // Debug log
                LyricsResult.NotFound
            }
        } catch (e: Exception) {
            println("Genius fallback error: ${e.message}") // Debug log
            LyricsResult.Error("Genius fallback failed: ${e.message}")
        }
    }

    private fun parseGeniusSearchResults(html: String): List<GeniusSong> {
        val songs = mutableListOf<GeniusSong>()

        // Multiple patterns to catch different search result formats
        val patterns = listOf(
            // Pattern 1: Standard search results
            """<a[^>]*href="([^"]*lyrics[^"]*)"[^>]*>.*?<span[^>]*>(.*?)</span>.*?by\s*<a[^>]*>(.*?)</a>""".toRegex(RegexOption.DOT_MATCHES_ALL),
            // Pattern 2: Mini card results
            """mini_card.*?href="([^"]+)".*?<b[^>]*>(.*?)</b>.*?by\s*(.*?)</a>""".toRegex(RegexOption.DOT_MATCHES_ALL),
            // Pattern 3: Search item results
            """search_result.*?href="([^"]+)".*?title[^>]*>(.*?)<.*?artist[^>]*>(.*?)<""".toRegex(RegexOption.DOT_MATCHES_ALL)
        )

        patterns.forEach { pattern ->
            pattern.findAll(html).forEach { match ->
                val url = if (match.groupValues[1].startsWith("http")) {
                    match.groupValues[1]
                } else {
                    "https://genius.com${match.groupValues[1]}"
                }
                val title = match.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
                val artist = match.groupValues[3].replace(Regex("<[^>]+>"), "").trim()

                if (title.isNotBlank() && artist.isNotBlank() && url.contains("lyrics")) {
                    songs.add(GeniusSong(title = title, artist = artist, url = url))
                }
            }
        }

        // Fallback: Look for any links containing "lyrics"
        if (songs.isEmpty()) {
            val fallbackPattern = """href="([^"]*lyrics[^"]*)"[^>]*>([^<]+)</a>""".toRegex()
            fallbackPattern.findAll(html).take(5).forEach { match ->
                val url = if (match.groupValues[1].startsWith("http")) {
                    match.groupValues[1]
                } else {
                    "https://genius.com${match.groupValues[1]}"
                }
                val titleAndArtist = match.groupValues[2].trim()

                // Try to split title and artist
                val parts = titleAndArtist.split(" by ", " - ", " – ")
                if (parts.size >= 2) {
                    songs.add(GeniusSong(
                        title = parts[0].trim(),
                        artist = parts.drop(1).joinToString(" ").trim(),
                        url = url
                    ))
                }
            }
        }

        println("Parsed ${songs.size} songs from Genius search") // Debug log
        return songs.distinctBy { it.url } // Remove duplicates
    }

    private fun extractLyricsFromGeniusPage(html: String): String? {
        val lyricsBuilder = StringBuilder()

        // Multiple patterns to extract lyrics from different Genius page formats
        val patterns = listOf(
            // New format: data-lyrics-container
            """<div[^>]*data-lyrics-container="true"[^>]*>(.*?)</div>""".toRegex(RegexOption.DOT_MATCHES_ALL),
            // Old format: Lyrics__Container
            """<div[^>]*class="[^"]*Lyrics__Container[^"]*"[^>]*>(.*?)</div>""".toRegex(RegexOption.DOT_MATCHES_ALL),
            // Alternative format: lyrics container
            """<div[^>]*class="[^"]*lyrics[^"]*"[^>]*>(.*?)</div>""".toRegex(RegexOption.DOT_MATCHES_ALL)
        )

        var foundLyrics = false

        patterns.forEach { pattern ->
            if (!foundLyrics) {
                val matches = pattern.findAll(html)
                matches.forEach { match ->
                    val content = match.groupValues[1]
                    val cleanContent = cleanHtmlContent(content)

                    if (cleanContent.isNotBlank() && cleanContent.length > 50) { // Only substantial content
                        lyricsBuilder.append(cleanContent).append("\n")
                        foundLyrics = true
                    }
                }
            }
        }

        // Fallback: Look for JSON-LD structured data
        if (!foundLyrics) {
            val jsonPattern = """<script type="application/ld\+json">(.*?)</script>""".toRegex(RegexOption.DOT_MATCHES_ALL)
            jsonPattern.findAll(html).forEach { match ->
                val jsonContent = match.groupValues[1]
                if (jsonContent.contains("\"@type\":\"MusicRecording\"") && jsonContent.contains("\"lyrics\"")) {
                    // Try to extract lyrics from JSON-LD (basic extraction)
                    val lyricsPattern = """"lyrics":\s*"([^"]+)"""".toRegex()
                    lyricsPattern.find(jsonContent)?.let { lyricsMatch ->
                        val lyrics = lyricsMatch.groupValues[1]
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")

                        if (lyrics.length > 50) {
                            lyricsBuilder.append(lyrics)
                            foundLyrics = true
                        }
                    }
                }
            }
        }

        val result = lyricsBuilder.toString().trim()
        println("Extracted lyrics length: ${result.length}") // Debug log

        return if (result.isNotBlank() && result.length > 20) {
            result
        } else null
    }

    private fun cleanHtmlContent(content: String): String {
        return content
            .replace(Regex("<br[^>]*>"), "\n")
            .replace(Regex("<p[^>]*>"), "\n")
            .replace(Regex("</p>"), "\n")
            .replace(Regex("<div[^>]*>"), "\n")
            .replace(Regex("</div>"), "\n")
            .replace(Regex("<[^>]+>"), "") // Remove all other HTML tags
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .replace(Regex("\n\\s*\n"), "\n") // Remove extra line breaks
            .trim()
    }

    // Existing helper methods remain the same
    private fun calculateStringSimilarity(str1: String, str2: String): Double {
        val s1 = str1.trim().lowercase()
        val s2 = str2.trim().lowercase()

        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        return when {
            s1.contains(s2) || s2.contains(s1) -> 0.8
            else -> {
                val maxLength = maxOf(s1.length, s2.length)
                val distance = levenshteinDistance(s1, s2)
                1.0 - (distance.toDouble() / maxLength)
            }
        }
    }

    private fun levenshteinDistance(str1: String, str2: String): Int {
        val len1 = str1.length
        val len2 = str2.length
        val matrix = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) matrix[i][0] = i
        for (j in 0..len2) matrix[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                matrix[i][j] = minOf(
                    matrix[i - 1][j] + 1,      // deletion
                    matrix[i][j - 1] + 1,      // insertion
                    matrix[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return matrix[len1][len2]
    }

    suspend fun lyrics(
        artist: String,
        title: String,
    ) = runCatching {
        queryLyrics(artist = artist, title = title, album = null)
    }

    // Debug method to test Genius functionality
    suspend fun testGeniusFallback(artist: String, title: String) {
        println("=== Testing Genius Fallback for '$title' by '$artist' ===")

        // Test search
        println("1. Searching Genius...")
        val songs = searchGeniusSongs(artist, title)
        println("Found ${songs.size} results:")
        songs.take(3).forEach { song ->
            println("  - '${song.title}' by '${song.artist}' -> ${song.url}")
        }

        // Test best match
        if (songs.isNotEmpty()) {
            val bestMatch = songs.firstOrNull { song ->
                val titleSim = calculateStringSimilarity(title, song.title)
                val artistSim = calculateStringSimilarity(artist, song.artist)
                titleSim > 0.6 && artistSim > 0.6
            }

            if (bestMatch != null) {
                println("2. Best match: '${bestMatch.title}' by '${bestMatch.artist}'")
                println("3. Scraping lyrics from: ${bestMatch.url}")

                val lyrics = scrapeGeniusLyrics(bestMatch.url)
                if (lyrics != null) {
                    println("4. Success! Lyrics preview (first 200 chars):")
                    println(lyrics.take(200) + "...")
                } else {
                    println("4. Failed to extract lyrics")
                }
            } else {
                println("2. No suitable match found")
            }
        }

        println("=== End Test ===")
    }

    @JvmInline
    value class Lyrics(
        val text: String,
    ) {
        val sentences
            get() =
                runCatching {
                    buildMap {
                        put(0L, "")
                        text.trim().lines().filter { it.length >= 10 }.forEach {
                            put(
                                it[8].digitToInt() * 10L +
                                        it[7].digitToInt() * 100 +
                                        it[5].digitToInt() * 1000 +
                                        it[4].digitToInt() * 10000 +
                                        it[2].digitToInt() * 60 * 1000 +
                                        it[1].digitToInt() * 600 * 1000,
                                it.substring(10),
                            )
                        }
                    }
                }.getOrNull()
    }

    // Data classes for Genius integration
    data class GeniusSong(
        val title: String,
        val artist: String,
        val url: String
    )

    sealed class LyricsResult {
        data class Success(
            val lyrics: String,
            val source: LyricsSource,
            val synced: Boolean
        ) : LyricsResult()

        object NotFound : LyricsResult()

        data class Error(val message: String) : LyricsResult()
    }

    enum class LyricsSource {
        LRCLIB, GENIUS
    }
}