/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.lrclib

import com.music.lrclib.models.Track
import com.music.lrclib.models.bestMatchingFor
import com.music.lrclib.models.bestMatchingForRelaxed
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
import java.util.Locale
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

    // Patterns to clean from title
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
    )

    // Patterns to extract primary artist
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
        // Get primary artist (first one before any separator)
        for (separator in artistSeparators) {
            if (cleaned.contains(separator, ignoreCase = true)) {
                cleaned = cleaned.split(separator, ignoreCase = true, limit = 2)[0]
                break
            }
        }
        return cleaned.trim()
    }

    private suspend fun queryLyricsWithParams(
        trackName: String? = null,
        artistName: String? = null,
        albumName: String? = null,
        query: String? = null,
    ): List<Track> = runCatching {
        client.get("/api/search") {
            if (query != null) parameter("q", query)
            if (trackName != null) parameter("track_name", trackName)
            if (artistName != null) parameter("artist_name", artistName)
            if (albumName != null) parameter("album_name", albumName)
        }.body<List<Track>>()
    }.getOrDefault(emptyList())

    private suspend fun queryLyrics(
        artist: String,
        title: String,
        album: String? = null,
    ): List<Track> {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        
        // Strategy 1: Search with cleaned title and artist
        var results = queryLyricsWithParams(
            trackName = cleanedTitle,
            artistName = cleanedArtist,
            albumName = album
        ).filter { it.syncedLyrics != null || it.plainLyrics != null }
        
        if (results.isNotEmpty()) return results
        
        // Strategy 2: Search with cleaned title only (artist might be different)
        results = queryLyricsWithParams(
            trackName = cleanedTitle
        ).filter { it.syncedLyrics != null || it.plainLyrics != null }
        
        if (results.isNotEmpty()) return results
        
        // Strategy 3: Use q parameter with combined search
        results = queryLyricsWithParams(
            query = "$cleanedArtist $cleanedTitle"
        ).filter { it.syncedLyrics != null || it.plainLyrics != null }
        
        if (results.isNotEmpty()) return results
        
        // Strategy 4: Use q parameter with just title
        results = queryLyricsWithParams(
            query = cleanedTitle
        ).filter { it.syncedLyrics != null || it.plainLyrics != null }
        
        if (results.isNotEmpty()) return results
        
        // Strategy 5: Try original title if different from cleaned
        if (cleanedTitle != title.trim()) {
            results = queryLyricsWithParams(
                trackName = title.trim(),
                artistName = artist.trim()
            ).filter { it.syncedLyrics != null || it.plainLyrics != null }
        }
        
        return results
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ) = runCatching {
        val tracks = queryLyrics(artist, title, album)
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)

        val res = when {
            duration == -1 -> {
                tracks.bestMatchingFor(duration, cleanedTitle, cleanedArtist)?.let { track ->
                    resolveLyricsText(track)
                }?.let(LrcLib::Lyrics)
            }
            else -> {
                // Try with relaxed duration matching (±5 seconds instead of ±2)
                tracks.bestMatchingForRelaxed(duration)?.let { track ->
                    resolveLyricsText(track)
                }?.let(LrcLib::Lyrics)
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
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        var count = 0
        var plain = 0

        val sortedTracks = when {
            duration == -1 -> {
                tracks.sortedByDescending { track ->
                    var score = 0.0

                    if (track.hasWordSync == true && !track.lyricsfile.isNullOrBlank()) score += 2.0
                    else if (track.syncedLyrics != null) score += 1.0

                    val titleSimilarity = calculateStringSimilarity(cleanedTitle, track.trackName)
                    val artistSimilarity = calculateStringSimilarity(cleanedArtist, track.artistName)
                    score += (titleSimilarity + artistSimilarity) / 2.0
                    
                    score
                }
            }
            else -> {
                tracks.sortedWith(
                    compareBy<Track> { abs(it.duration.toInt() - duration) }
                        .thenByDescending { if (it.hasWordSync == true && !it.lyricsfile.isNullOrBlank()) 1 else 0 }
                )
            }
        }

        sortedTracks.forEach { track ->
            currentCoroutineContext().ensureActive()
            if (count <= 4) {
                val resolvedText = resolveLyricsText(track)
                val isSynced = (track.hasWordSync == true && !track.lyricsfile.isNullOrBlank()) || track.syncedLyrics != null
                if (resolvedText != null) {
                    if (duration == -1) {
                        count++
                        callback(resolvedText)
                    } else if (abs(track.duration.toInt() - duration) <= 5) {
                        if (isSynced) {
                            count++
                            callback(resolvedText)
                        } else if (plain == 0) {
                            count++
                            plain++
                            callback(resolvedText)
                        }
                    }
                }
            }
        }
    }

    private fun resolveLyricsText(track: Track): String? {
        if (track.hasWordSync == true && !track.lyricsfile.isNullOrBlank()) {
            val wordSyncLrc = parseLyricsfileToWordSyncLrc(track.lyricsfile)
            if (!wordSyncLrc.isNullOrBlank()) {
                return wordSyncLrc
            }
        }
        return track.syncedLyrics ?: track.plainLyrics
    }

    private class ParsedWord(
        val text: String,
        val startTimeSec: Double,
        var endTimeSec: Double,
    )

    private class ParsedLine(
        var text: String,
        var startMs: Long,
        var endMs: Long?,
        val words: MutableList<ParsedWord> = mutableListOf(),
    )

    private fun extractYamlValue(line: String, key: String): String? {
        val trimmed = line.trimStart().removePrefix("-").trimStart()
        if (!trimmed.startsWith("$key:")) return null
        return trimmed.substringAfter("$key:").trim()
    }

    private fun unquoteYaml(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.length >= 2) {
            if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
                return trimmed.substring(1, trimmed.length - 1).replace("''", "'")
            }
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                return trimmed.substring(1, trimmed.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
            }
        }
        return trimmed
    }

    internal fun parseLyricsfileToWordSyncLrc(lyricsfile: String): String? {
        if (lyricsfile.isBlank()) return null
        val lines = lyricsfile.lines()
        val parsedLines = mutableListOf<ParsedLine>()

        var inLinesBlock = false
        var currentLine: ParsedLine? = null
        var inWordsBlock = false
        var lineIndent = -1
        var currentWordText: String? = null
        var currentWordStartMs: Long? = null
        var currentWordEndMs: Long? = null

        fun commitCurrentWord() {
            val wordText = currentWordText
            val wordStart = currentWordStartMs
            if (wordText != null && wordStart != null) {
                val startSec = wordStart / 1000.0
                val endSec = (currentWordEndMs ?: wordStart) / 1000.0
                val safeText = wordText.replace("|", " ").replace(":", " ")
                currentLine?.words?.add(ParsedWord(safeText, startSec, maxOf(startSec, endSec)))
            }
            currentWordText = null
            currentWordStartMs = null
            currentWordEndMs = null
        }

        fun commitCurrentLine() {
            commitCurrentWord()
            currentLine?.let { line ->
                if (line.text.isNotBlank()) {
                    parsedLines.add(line)
                }
            }
            currentLine = null
            inWordsBlock = false
            lineIndent = -1
        }

        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            val leadingSpaces = rawLine.indexOfFirst { !it.isWhitespace() }

            if (leadingSpaces == 0 && !trimmed.startsWith("-")) {
                if (trimmed == "lines:" || trimmed.startsWith("lines:")) {
                    inLinesBlock = true
                } else if (inLinesBlock) {
                    commitCurrentLine()
                    inLinesBlock = false
                }
                continue
            }

            if (!inLinesBlock) continue

            val isListItem = trimmed.startsWith("- ") || trimmed == "-"

            if (isListItem) {
                val contentAfterDash = if (trimmed.startsWith("- ")) trimmed.substring(2).trim() else ""

                if (inWordsBlock && (lineIndent != -1 && leadingSpaces > lineIndent)) {
                    commitCurrentWord()
                    if (contentAfterDash.isNotEmpty()) {
                        extractYamlValue(contentAfterDash, "text")?.let { currentWordText = unquoteYaml(it) }
                        extractYamlValue(contentAfterDash, "start_ms")?.toLongOrNull()?.let { currentWordStartMs = it }
                        extractYamlValue(contentAfterDash, "end_ms")?.toLongOrNull()?.let { currentWordEndMs = it }
                    }
                } else {
                    commitCurrentLine()
                    lineIndent = leadingSpaces
                    currentLine = ParsedLine("", 0L, null)
                    inWordsBlock = false
                    if (contentAfterDash.isNotEmpty()) {
                        extractYamlValue(contentAfterDash, "text")?.let { currentLine?.text = unquoteYaml(it) }
                        extractYamlValue(contentAfterDash, "start_ms")?.toLongOrNull()?.let { currentLine?.startMs = it }
                        extractYamlValue(contentAfterDash, "end_ms")?.toLongOrNull()?.let { currentLine?.endMs = it }
                    }
                }
                continue
            }

            if (trimmed.startsWith("words:")) {
                inWordsBlock = true
                commitCurrentWord()
                continue
            }

            if (inWordsBlock) {
                extractYamlValue(trimmed, "text")?.let { currentWordText = unquoteYaml(it) }
                extractYamlValue(trimmed, "start_ms")?.toLongOrNull()?.let { currentWordStartMs = it }
                extractYamlValue(trimmed, "end_ms")?.toLongOrNull()?.let { currentWordEndMs = it }
            } else if (currentLine != null) {
                extractYamlValue(trimmed, "text")?.let { currentLine?.text = unquoteYaml(it) }
                extractYamlValue(trimmed, "start_ms")?.toLongOrNull()?.let { currentLine?.startMs = it }
                extractYamlValue(trimmed, "end_ms")?.toLongOrNull()?.let { currentLine?.endMs = it }
            }
        }
        commitCurrentLine()

        if (parsedLines.isEmpty()) return null
        val hasAnyWords = parsedLines.any { it.words.isNotEmpty() }
        if (!hasAnyWords) return null

        for (line in parsedLines) {
            for (w in line.words.indices) {
                val word = line.words[w]
                if (word.endTimeSec <= word.startTimeSec) {
                    val nextStart = if (w + 1 < line.words.size) {
                        line.words[w + 1].startTimeSec
                    } else {
                        line.endMs?.let { it / 1000.0 } ?: (word.startTimeSec + 0.5)
                    }
                    word.endTimeSec = maxOf(word.startTimeSec + 0.1, nextStart)
                }
            }
        }

        return buildString {
            parsedLines.forEach { line ->
                val timeMs = line.startMs
                val minutes = timeMs / 60000
                val seconds = (timeMs % 60000) / 1000
                val centiseconds = (timeMs % 1000) / 10

                appendLine(String.format(Locale.US, "[%02d:%02d.%02d]%s", minutes, seconds, centiseconds, line.text))

                if (line.words.isNotEmpty()) {
                    val wordsData = line.words.joinToString("|") { word ->
                        "${word.text}:${word.startTimeSec}:${word.endTimeSec}"
                    }
                    appendLine("<$wordsData>")
                }
            }
        }
    }

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
}


