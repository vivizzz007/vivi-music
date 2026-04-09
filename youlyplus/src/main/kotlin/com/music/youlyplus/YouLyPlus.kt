package com.music.youlyplus

import com.music.youlyplus.models.BinimumSearchResponse
import com.music.youlyplus.models.LyricsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

/**
 * YouLyPlus / LyricsPlus KPoe API client.
 *
 * This replicates the multi-server fetch strategy from the YouLyPlus browser
 * extension (ibratabian17/YouLyPlus), querying community-hosted instances of
 * the open-source LyricsPlus backend (ibratabian17/lyricsplus).
 *
 * API endpoint: GET {server}/v2/lyrics/get?title=...&artist=...&duration=...
 */
object YouLyPlus {
    private const val BINIMUM_SEARCH_URL = "https://lyrics-api.binimum.org/"

    /** Mirror of YouLyPlus extension's KPOE_SERVERS constant. */
    
    private val SERVERS = listOf(
        "https://lyricsplus.prjktla.my.id",       // youly's server
        "https://lyricsplus.atomix.one",          // meow's mirror
        "https://lyricsplus.binimum.org",         // binimum's server
        "https://lyricsplus.prjktla.workers.dev", // ibra's cf worker
        "https://lyricsplus-seven.vercel.app",    // jigen's mirror
        "https://lyrics-plus-backend.vercel.app", // ibra's vercel
    )

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }
            expectSuccess = true
        }
    }

    private data class ResolvedLyrics(
        val lyrics: String,
        val isWordSynced: Boolean,
    )

    /**
     * Fetch lyrics from the first responding server.
     * Prefers synced lyrics; falls back to plain text if available.
     * Converts structured KPoe line-arrays to LRC string format.
     */
    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        id: String? = null,
        isrc: String? = null,
    ): Result<String> = runCatching {
        val binimumResult = fetchFromBinimumApi(title, artist, duration, album, isrc)
        var binimumLineFallback: String? = null

        when {
            binimumResult?.isWordSynced == true -> return@runCatching binimumResult.lyrics
            binimumResult != null -> binimumLineFallback = binimumResult.lyrics
        }

        for (server in SERVERS) {
            val response = fetchFromServer(server, title, artist, duration, album, id, isrc)
            if (response != null) {
                val lyrics = resolveLyrics(response) ?: continue
                if (binimumLineFallback != null) {
                    if (lyrics.isWordSynced) return@runCatching lyrics.lyrics
                    continue
                }
                return@runCatching lyrics.lyrics
            }
        }
        if (!binimumLineFallback.isNullOrBlank()) return@runCatching binimumLineFallback
        throw IllegalStateException("No lyrics found from any YouLyPlus server")
    }

    /**
     * Collect all lyrics options across servers; invokes [callback] for each
     * distinct non-blank result.
     */
    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        id: String? = null,
        isrc: String? = null,
        callback: (String) -> Unit,
    ) {
        val seen = mutableSetOf<String>()
        fetchFromBinimumApi(title, artist, duration, album, isrc)?.lyrics?.let { lyrics ->
            if (seen.add(lyrics)) callback(lyrics)
        }

        for (server in SERVERS) {
            runCatching {
                val response = fetchFromServer(server, title, artist, duration, album, id, isrc)
                if (response != null) {
                    val lyrics = resolveLyrics(response)?.lyrics
                    if (!lyrics.isNullOrBlank() && seen.add(lyrics)) {
                        callback(lyrics)
                    }
                }
            }
        }
    }

    private fun resolveLyrics(response: LyricsResponse): ResolvedLyrics? {
        response.syncedLyrics?.takeIf { it.isNotBlank() }?.let {
            return ResolvedLyrics(it, it.contains("<") && it.contains(">"))
        }

        response.lyrics?.convertToLrc()?.takeIf { it.isNotBlank() }?.let {
            val hasWordSync = response.lyrics.any { line -> !line.syllabus.isNullOrEmpty() }
            return ResolvedLyrics(it, hasWordSync)
        }

        response.plainLyrics?.takeIf { it.isNotBlank() }?.let {
            return ResolvedLyrics(it, false)
        }
        return null
    }

    /**
     * Converts a list of LyricsItem (with millisecond 'time') to a standard 
     * [mm:ss.xxx]LRC string. Supports word-by-word rich sync if syllables are present.
     */
    private fun List<com.music.youlyplus.models.LyricsItem>.convertToLrc(): String? {
        if (isEmpty()) return null
        return joinToString("\n") { item ->
            val lineTime = item.time ?: 0L
            
            // Check if any syllable or the item itself is marked as background
            val isBg = item.syllabus?.any { it.isBackground == true } == true
            val lineTimestamp = formatTime(lineTime)
            val bgMarker = if (isBg) "{bg}" else ""
            
            val syllabus = item.syllabus
            if (!syllabus.isNullOrEmpty()) {
                val sb = StringBuilder(lineTimestamp)
                sb.append(bgMarker)
                syllabus.forEach { syl ->
                    val sylTime = syl.time ?: 0L
                    sb.append(formatTime(sylTime, isSyllable = true))
                    sb.append(syl.text ?: "")
                    // Add space after word if it's missing and not the last word
                    if (syl.text?.endsWith(" ") == false) {
                        sb.append(" ")
                    }
                }
                sb.toString().trim()
            } else {
                lineTimestamp + bgMarker + (item.text ?: "")
            }
        }
    }


    private fun formatTime(timeMs: Long, isSyllable: Boolean = false): String {
        val minutes = (timeMs / 1000) / 60
        val seconds = (timeMs / 1000) % 60
        val millis = timeMs % 1000
        val prefix = if (isSyllable) "<" else "["
        val suffix = if (isSyllable) ">" else "]"
        return "%s%02d:%02d.%03d%s".format(prefix, minutes, seconds, millis, suffix)
    }


    private suspend fun fetchFromServer(
        baseUrl: String,
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        id: String? = null,
        isrc: String? = null,
    ): LyricsResponse? = runCatching {
        client.get(baseUrl.let { if (it.endsWith("/")) it else "$it/" } + "v2/lyrics/get") {
            parameter("title", title)
            parameter("artist", artist)
            parameter("duration", duration)
            if (album != null) parameter("album", album)
            if (id != null) parameter("id", id)
            if (isrc != null) parameter("isrc", isrc)
        }.body<LyricsResponse>()
    }.getOrNull()

    private suspend fun fetchFromBinimumApi(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        isrc: String? = null,
    ): ResolvedLyrics? = runCatching {
        val response = client.get(BINIMUM_SEARCH_URL) {
            if (!isrc.isNullOrBlank()) {
                parameter("isrc", isrc)
            } else {
                parameter("track", title)
                parameter("artist", artist)
                if (!album.isNullOrBlank()) parameter("album", album)
                if (duration > 0) parameter("duration", duration)
            }
        }.body<BinimumSearchResponse>()

        val bestResult = response.results.firstOrNull() ?: return@runCatching null
        val lyricsUrl = bestResult.lyricsUrl?.takeIf { it.isNotBlank() } ?: return@runCatching null
        val ttml = client.get(lyricsUrl).bodyAsText().takeIf { it.isNotBlank() } ?: return@runCatching null
        val lrc = AppleTtmlConverter.toLrc(ttml).takeIf { it.isNotBlank() } ?: return@runCatching null

        ResolvedLyrics(
            lyrics = lrc,
            isWordSynced = bestResult.timingType.equals("word", ignoreCase = true),
        )
    }.getOrNull()
}

private object AppleTtmlConverter {
    data class ParsedLine(
        val text: String,
        val startTime: Double,
        val words: List<ParsedWord>,
    )

    data class ParsedWord(
        val text: String,
        val startTime: Double,
        val endTime: Double,
    )

    fun toLrc(ttml: String): String {
        val lines = parse(ttml)
        if (lines.isEmpty()) return ""

        return buildString {
            lines.forEach { line ->
                val timeMs = (line.startTime * 1000).toLong()
                val minutes = timeMs / 60000
                val seconds = (timeMs % 60000) / 1000
                val centiseconds = (timeMs % 1000) / 10
                appendLine(String.format("[%02d:%02d.%02d]%s", minutes, seconds, centiseconds, line.text))

                if (line.words.isNotEmpty()) {
                    val wordsData = line.words.joinToString("|") { word ->
                        "${word.text}:${word.startTime}:${word.endTime}"
                    }
                    appendLine("<$wordsData>")
                }
            }
        }
    }

    private fun parse(ttml: String): List<ParsedLine> = runCatching {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(ttml.byteInputStream())
        val pElements = doc.getElementsByTagName("p")
        val lines = mutableListOf<ParsedLine>()

        for (i in 0 until pElements.length) {
            val pElement = pElements.item(i) as? Element ?: continue
            val begin = pElement.getAttribute("begin")
            if (begin.isBlank()) continue

            val startTime = parseTime(begin)
            val words = mutableListOf<ParsedWord>()

            val childNodes = pElement.childNodes
            for (j in 0 until childNodes.length) {
                val node = childNodes.item(j)
                if (node.nodeType != Node.ELEMENT_NODE) continue
                val span = node as? Element ?: continue
                if (!span.tagName.equals("span", ignoreCase = true)) continue

                val wordText = span.textContent?.trim()?.takeIf { it.isNotEmpty() } ?: continue
                val wordBegin = span.getAttribute("begin")
                val wordEnd = span.getAttribute("end")
                if (wordBegin.isBlank() || wordEnd.isBlank()) continue

                words += ParsedWord(wordText, parseTime(wordBegin), parseTime(wordEnd))
            }

            val lineText = if (words.isNotEmpty()) {
                words.joinToString(" ") { it.text }
            } else {
                pElement.textContent?.trim().orEmpty()
            }

            if (lineText.isNotBlank()) {
                lines += ParsedLine(lineText, startTime, words)
            }
        }

        lines
    }.getOrDefault(emptyList())

    private fun parseTime(timeStr: String): Double {
        val normalized = timeStr.trim().removeSuffix("s")
        return try {
            when {
                normalized.contains(":") -> {
                    val parts = normalized.split(":")
                    when (parts.size) {
                        2 -> parts[0].toDouble() * 60 + parts[1].toDouble()
                        3 -> parts[0].toDouble() * 3600 + parts[1].toDouble() * 60 + parts[2].toDouble()
                        else -> normalized.toDoubleOrNull() ?: 0.0
                    }
                }
                else -> normalized.toDoubleOrNull() ?: 0.0
            }
        } catch (_: Exception) {
            0.0
        }
    }
}
