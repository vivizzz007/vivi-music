/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.kugou

import com.music.kugou.models.DownloadLyricsResponse
import com.music.kugou.models.Keyword
import com.music.kugou.models.SearchLyricsResponse
import com.music.kugou.models.SearchSongResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.encodeURLParameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.lang.Integer.min
import java.util.Locale
import java.util.zip.Inflater
import kotlin.math.abs

@OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
private val client = HttpClient {
    expectSuccess = true

    install(ContentNegotiation) {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }
        json(json)
        json(json, ContentType.Text.Html)
        json(json, ContentType.Text.Plain)
    }

    install(ContentEncoding) {
        gzip()
        deflate()
    }
}

private const val PAGE_SIZE = 8
private const val HEAD_CUT_LIMIT = 30

/**
 * KuGou Lyrics Library
 * Modified from [ViMusic](https://github.com/vfsfitvnm/ViMusic)
 */
object KuGou {
    var useTraditionalChinese: Boolean = false

    suspend fun getLyrics(title: String, artist: String, duration: Int, album: String? = null): Result<String> =
        runCatching {
            val keyword = generateKeyword(title, artist, album)
            val candidate = getLyricsCandidate(keyword, duration)
                ?: run {
                    val primary = extractPrimaryArtist(keyword.artist)
                    if (primary != keyword.artist) {
                        getLyricsCandidate(keyword.copy(artist = primary), duration)
                    } else null
                }
                ?: throw IllegalStateException("No lyrics candidate")

            fetchLyricsFromCandidate(candidate)
                ?: throw IllegalStateException("Failed to download or parse lyrics")
        }

    suspend fun getAllPossibleLyricsOptions(
        title: String, artist: String, duration: Int, album: String? = null, callback: (String) -> Unit
    ) {
        val keyword = generateKeyword(title, artist, album)
        val infoList = searchSongs(keyword).data.info.ifEmpty {
            val primary = extractPrimaryArtist(keyword.artist)
            if (primary != keyword.artist) {
                searchSongs(keyword.copy(artist = primary)).data.info
            } else emptyList()
        }

        infoList.forEach {
            if (duration == -1 || abs(it.duration - duration) <= DURATION_TOLERANCE) {
                val candidate = searchLyricsByHash(it.hash).candidates.let { list ->
                    list.firstOrNull { (it.krctype ?: 0) > 0 } ?: list.firstOrNull()
                }
                candidate?.let { c ->
                    fetchLyricsFromCandidate(c)?.let(callback)
                }
            }
        }

        val keywordCandidates = searchLyricsByKeyword(keyword, duration).candidates.ifEmpty {
            val primary = extractPrimaryArtist(keyword.artist)
            if (primary != keyword.artist) {
                searchLyricsByKeyword(keyword.copy(artist = primary), duration).candidates
            } else emptyList()
        }

        val sortedCandidates = keywordCandidates.sortedByDescending { it.krctype ?: 0 }
        sortedCandidates.forEach { candidate ->
            fetchLyricsFromCandidate(candidate)?.let(callback)
        }
    }

    suspend fun getLyricsCandidate(
        keyword: Keyword, duration: Int
    ): SearchLyricsResponse.Candidate? {
        searchSongs(keyword).data.info.forEach { song ->
            if (duration == -1 || abs(song.duration - duration) <= DURATION_TOLERANCE) {
                val candidate = searchLyricsByHash(song.hash).candidates.let { list ->
                    list.firstOrNull { (it.krctype ?: 0) > 0 } ?: list.firstOrNull()
                }
                if (candidate != null) return candidate
            }
        }
        return searchLyricsByKeyword(keyword, duration).candidates.let { list ->
            list.firstOrNull { (it.krctype ?: 0) > 0 } ?: list.firstOrNull()
        }
    }

    private suspend fun fetchLyricsFromCandidate(candidate: SearchLyricsResponse.Candidate): String? {
        if ((candidate.krctype ?: 0) > 0) {
            val krcResult = runCatching {
                val resp = downloadLyrics(candidate.id, candidate.accesskey, fmt = "krc")
                val decoded = decodeKrc(resp.content)
                if (!decoded.isNullOrBlank()) {
                    krcToWordSyncLrc(decoded)
                } else null
            }.getOrNull()

            if (!krcResult.isNullOrBlank()) {
                return krcResult
            }
        }

        return runCatching {
            val resp = downloadLyrics(candidate.id, candidate.accesskey, fmt = "lrc")
            Base64.Default.decode(resp.content).decodeToString().normalize()
        }.getOrNull()
    }

    suspend fun searchSongs(keyword: Keyword) =
        client.get("https://mobileservice.kugou.com/api/v3/search/song") {
            parameter("version", 9108)
            parameter("plat", 0)
            parameter("pagesize", PAGE_SIZE)
            parameter("showtype", 0)
            val searchQuery = buildString {
                append(keyword.title)
                append(" - ")
                append(keyword.artist)
                if (!keyword.album.isNullOrBlank()) {
                    append(" ")
                    append(keyword.album)
                }
            }
            url.encodedParameters.append(
                "keyword",
                searchQuery.encodeURLParameter(spaceToPlus = false)
            )
        }.body<SearchSongResponse>()

    private suspend fun searchLyricsByKeyword(keyword: Keyword, duration: Int) =
        client.get("https://lyrics.kugou.com/search") {
            parameter("ver", 1)
            parameter("man", "yes")
            parameter("client", "pc")
            parameter(
                "duration", duration.takeIf { it != -1 }?.times(1000)
            ) // if duration == -1, we don't care duration
            val searchQuery = buildString {
                append(keyword.title)
                append(" - ")
                append(keyword.artist)
                if (!keyword.album.isNullOrBlank()) {
                    append(" ")
                    append(keyword.album)
                }
            }
            url.encodedParameters.append(
                "keyword",
                searchQuery.encodeURLParameter(spaceToPlus = false)
            )
        }.body<SearchLyricsResponse>()

    private suspend fun searchLyricsByHash(hash: String) =
        client.get("https://lyrics.kugou.com/search") {
            parameter("ver", 1)
            parameter("man", "yes")
            parameter("client", "pc")
            parameter("hash", hash)
        }.body<SearchLyricsResponse>()

    private suspend fun downloadLyrics(id: Long, accessKey: String, fmt: String = "lrc") =
        client.get("https://lyrics.kugou.com/download") {
            parameter("fmt", fmt)
            parameter("charset", "utf8")
            parameter("client", "pc")
            parameter("ver", 1)
            parameter("id", id)
            parameter("accesskey", accessKey)
        }.body<DownloadLyricsResponse>()

    private val KRC_KEY = intArrayOf(
        0x40, 0x47, 0x61, 0x77, 0x5E, 0x32, 0x74, 0x47,
        0x51, 0x36, 0x31, 0x2D, 0xCE, 0xD2, 0x6E, 0x69
    )

    private val KRC_LINE_REGEX = """^\[(\d+),(\d+)\](.*)$""".toRegex()
    private val KRC_WORD_REGEX = """<(\d+),(\d+),\d+>([^<]*)""".toRegex()

    internal fun decodeKrc(base64Payload: String): String? = runCatching {
        if (base64Payload.isBlank()) return null
        val raw = Base64.Default.decode(base64Payload)
        if (raw.size < 4) return null
        if (raw[0] != 0x6B.toByte() || raw[1] != 0x72.toByte() || raw[2] != 0x63.toByte() || raw[3] != 0x31.toByte()) {
            return null
        }

        val encrypted = raw.copyOfRange(4, raw.size)
        val decrypted = ByteArray(encrypted.size)
        for (i in encrypted.indices) {
            val b = (encrypted[i].toInt() and 0xFF) xor KRC_KEY[i % KRC_KEY.size]
            decrypted[i] = b.toByte()
        }

        val inflater = Inflater()
        inflater.setInput(decrypted)
        val outputStream = ByteArrayOutputStream(decrypted.size * 2)
        val buffer = ByteArray(1024)
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count == 0 && inflater.needsInput()) break
            outputStream.write(buffer, 0, count)
        }
        inflater.end()
        outputStream.toString("UTF-8")
    }.getOrNull()

    internal fun krcToWordSyncLrc(krcText: String): String? = runCatching {
        if (krcText.isBlank()) return null
        val lines = krcText.lines()
        val result = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val match = KRC_LINE_REGEX.matchEntire(trimmed) ?: continue
            val lineStartMs = match.groupValues[1].toLongOrNull() ?: continue
            val content = match.groupValues[3]

            val wordMatches = KRC_WORD_REGEX.findAll(content).toList()
            if (wordMatches.isEmpty()) continue

            val lineText = buildString {
                for (w in wordMatches) {
                    append(w.groupValues[3])
                }
            }.trim()

            if (lineText.isBlank() || lineText.matches(BANNED_REGEX)) continue

            val minutes = lineStartMs / 60000
            val seconds = (lineStartMs % 60000) / 1000
            val centiseconds = (lineStartMs % 1000) / 10
            val lineTag = String.format(Locale.US, "[%02d:%02d.%02d]%s", minutes, seconds, centiseconds, lineText)

            val wordsData = wordMatches.map { wMatch ->
                val offsetMs = wMatch.groupValues[1].toLongOrNull() ?: 0L
                val durMs = wMatch.groupValues[2].toLongOrNull() ?: 0L
                val rawWord = wMatch.groupValues[3]
                val safeWord = rawWord.replace("|", " ").replace(":", " ")
                val startSec = (lineStartMs + offsetMs) / 1000.0
                val endSec = (lineStartMs + offsetMs + maxOf(50L, durMs)) / 1000.0
                String.format(Locale.US, "%s:%.3f:%.3f", safeWord, startSec, endSec)
            }.joinToString("|")

            result.add(lineTag)
            result.add("<$wordsData>")
        }

        if (result.isEmpty()) null else result.joinToString("\n")
    }.getOrNull()

    private val artistSeparators = listOf("、", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ")

    private fun extractPrimaryArtist(artist: String): String {
        var cleaned = artist.trim()
        for (sep in artistSeparators) {
            if (cleaned.contains(sep, ignoreCase = true)) {
                cleaned = cleaned.split(sep, ignoreCase = true, limit = 2)[0]
                break
            }
        }
        return cleaned.trim()
    }

    private fun normalizeTitle(title: String) =
        title.replace("\\(.*\\)".toRegex(), "").replace("（.*）".toRegex(), "")
            .replace("「.*」".toRegex(), "").replace("『.*』".toRegex(), "")
            .replace("<.*>".toRegex(), "").replace("《.*》".toRegex(), "")
            .replace("〈.*〉".toRegex(), "").replace("＜.*＞".toRegex(), "")

    private fun normalizeArtist(artist: String) =
        artist.replace(", ", "、").replace(" & ", "、").replace(".", "").replace("和", "、")
            .replace("\\(.*\\)".toRegex(), "").replace("（.*）".toRegex(), "")

    fun generateKeyword(title: String, artist: String, album: String? = null) =
        Keyword(normalizeTitle(title), normalizeArtist(artist), album)

    private fun String.normalize(): String =
        lines().filter { line -> line.matches(ACCEPTED_REGEX) }
            .let { lines ->
                // Remove useless information such as singer, writer, composer, guitar, etc.
                var headCutLine = 0
                for (i in min(HEAD_CUT_LIMIT, lines.lastIndex) downTo 0) {
                    if (lines[i].matches(BANNED_REGEX)) {
                        headCutLine = i + 1
                        break
                    }
                }
                val filteredLines = lines.drop(headCutLine)

                var tailCutLine = 0
                for (i in min(lines.size - HEAD_CUT_LIMIT, lines.lastIndex) downTo 0) {
                    if (lines[lines.lastIndex - i].matches(BANNED_REGEX)) {
                        tailCutLine = i + 1
                        break
                    }
                }
                val finalLines = filteredLines.dropLast(tailCutLine)

                return@let finalLines.joinToString("\n")
            }

    @Suppress("RegExpRedundantEscape")
    private val ACCEPTED_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\].*".toRegex()
    private val BANNED_REGEX = ".+].+[:：].+".toRegex()

    private const val DURATION_TOLERANCE = 8
}
