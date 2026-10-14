/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.lyrics

import android.content.Context
import com.music.vivi.constants.EnableLrcRedKey
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

object LrcRedLyricsProvider : LyricsProvider {
    override val name = "LrcRed"

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
            defaultRequest {
                url("https://lrc.red/")
            }
            expectSuccess = true
        }
    }

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableLrcRedKey] ?: true

    @Serializable
    data class Hit(
        val isrc: String? = null,
        val title: String? = null,
        val artist: String? = null,
        val duration: Double? = null
    )

    @Serializable
    data class Response(
        val hits: List<Hit>? = null
    )

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?
    ): Result<String> = runCatching {
        val q = "$title $artist".trim()
        val response = client.get("search.json") {
            parameter("q", q)
        }.body<Response>()

        val hits = response.hits ?: throw IllegalStateException("No results from lrc.red")
        
        val hit = bestHit(hits, title, artist, duration)
            ?: throw IllegalStateException("No matching track found")

        val isrc = hit.isrc?.trim()?.uppercase(Locale.ROOT)
            ?: throw IllegalStateException("ISRC missing in hit")
            
        val ttml = client.get("s/$isrc.ttml").bodyAsText()
        parseTtmlToLrc(ttml)
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit
    ) {
        val q = "$title $artist".trim()
        val response = runCatching { 
            client.get("search.json") { parameter("q", q) }.body<Response>() 
        }.getOrNull()
        
        val hits = response?.hits ?: return

        var count = 0
        hits.forEach { hit ->
            if (count > 4) return@forEach
            currentCoroutineContext().ensureActive()
            
            val hitDurationS = hit.duration ?: 0.0
            val isDurationMatch = duration <= 0 || hitDurationS <= 0.0 || abs(hitDurationS - duration) <= 3.0

            if (isDurationMatch && !hit.isrc.isNullOrBlank()) {
                val isrc = hit.isrc.trim().uppercase(Locale.ROOT)
                runCatching {
                    val ttml = client.get("s/$isrc.ttml").bodyAsText()
                    val lrc = parseTtmlToLrc(ttml)
                    if (lrc.isNotBlank()) {
                        callback(lrc)
                        count++
                    }
                }
            }
        }
    }

    private fun bestHit(hits: List<Hit>, title: String, artist: String, durationSeconds: Int): Hit? {
        val wantedTitle = coreOf(title)
        val wantedVersion = versionOf(title)
        val wantedArtists = artistsOf(artist)
        val seconds = durationSeconds.toDouble()

        fun distance(hit: Hit): Double =
            if (durationSeconds > 0 && hit.duration != null) abs(hit.duration - seconds) else 0.0

        return hits
            .filter { hit ->
                val name = hit.title ?: return@filter false
                !hit.isrc.isNullOrBlank() &&
                    coreOf(name) == wantedTitle &&
                    versionOf(name) == wantedVersion &&
                    (wantedArtists.isEmpty() || artistsOf(hit.artist.orEmpty()).any { it in wantedArtists }) &&
                    (durationSeconds <= 0 || distance(hit) <= 3.0)
            }
            .minByOrNull(::distance)
    }

    private fun coreOf(title: String): String =
        normalized(title.replace(Regex("""[(\[][^)\]]*[)\]]"""), " ").substringBefore(" - "))
            .ifEmpty { normalized(title) }

    private fun versionOf(title: String): Set<String> {
        val extras = Regex("""[(\[][^)\]]*[)\]]""").findAll(title).joinToString(" ") { it.value } +
            " " + title.substringAfter(" - ", "")
        return normalized(extras).split(' ').filter { 
            it in setOf(
                "live", "remix", "remixed", "mix", "acoustic", "unplugged", "instrumental",
                "karaoke", "cappella", "acapella", "demo", "edit", "version", "cover",
                "sped", "slowed", "reverb", "nightcore", "lofi", "orchestral", "extended"
            )
        }.toSet()
    }

    private fun artistsOf(artist: String): Set<String> =
        artist.split(Regex("""\s*(?:,|&|;|/|\s+and\s+|\s+x\s+|\s+with\s+|\s+feat\.?\s+|\s+ft\.?\s+)\s*""", RegexOption.IGNORE_CASE))
            .map(::normalized).filter { it.isNotEmpty() }.toSet()

    private fun normalized(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
            .lowercase(Locale.ROOT)
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .replace(Regex("""\s+"""), " ")
            .trim()

    /**
     * Parses Apple Music TTML into LRC format. 
     * Expects <p begin="MM:SS.mmm"> or <p begin="SS.mmm"> pattern.
     */
    private fun parseTtmlToLrc(rawTtml: String): String {
        // Strip BOM and clean formatting
        val ttml = rawTtml.replace("\uFEFF", "")
        
        val pRegex = Regex("""<p([^>]*)>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)
        val tokenRegex = Regex("""(<[^>]+>)|([^<]+)""")
        val beginRegex = Regex("""begin="([^"]+)"""")
        val roleRegex = Regex("""(?:ttm:role|role)="([^"]+)"""")
        val agentRegex = Regex("""(?:ttm:agent|agent)="([^"]+)"""")
        
        val lines = buildMap<Long, MutableList<String>> {
            for (pMatch in pRegex.findAll(ttml)) {
                val pAttrs = pMatch.groupValues[1]
                val innerHtml = pMatch.groupValues[2]
                
                val pBeginStr = beginRegex.find(pAttrs)?.groupValues?.get(1) ?: continue
                val lineStartMs = timeToMs(pBeginStr) ?: continue
                val agent = agentRegex.find(pAttrs)?.groupValues?.get(1)
                
                val fgPieces = mutableListOf<Pair<Long, String>>()
                val bgPieces = mutableListOf<Pair<Long, String>>()
                var currentSpanMs: Long = lineStartMs
                
                // Track nesting states
                val bgStack = mutableListOf<Boolean>()
                val skipStack = mutableListOf<Boolean>()
                val timeStack = mutableListOf<Long>()
                
                for (tokenMatch in tokenRegex.findAll(innerHtml)) {
                    val tag = tokenMatch.groupValues[1]
                    val text = tokenMatch.groupValues[2]
                    
                    if (tag.isNotEmpty()) {
                        if (tag.startsWith("</")) {
                            if (bgStack.isNotEmpty()) bgStack.removeLast()
                            if (skipStack.isNotEmpty()) skipStack.removeLast()
                            if (timeStack.isNotEmpty()) {
                                timeStack.removeLast()
                                currentSpanMs = timeStack.lastOrNull() ?: lineStartMs
                            }
                        } else if (tag.startsWith("<span") || tag.startsWith("<s ")) {
                            val role = roleRegex.find(tag)?.groupValues?.get(1)
                            val isSkip = role == "x-translation" || role == "x-roman"
                            val isBg = role == "x-bg"
                            
                            bgStack.add(isBg || (bgStack.lastOrNull() ?: false))
                            skipStack.add(isSkip || (skipStack.lastOrNull() ?: false))
                            
                            val tBegin = beginRegex.find(tag)?.groupValues?.get(1)
                            if (tBegin != null) {
                                val tMs = timeToMs(tBegin)
                                if (tMs != null) {
                                    timeStack.add(tMs)
                                    currentSpanMs = tMs
                                    continue
                                }
                            }
                            timeStack.add(currentSpanMs)
                        }
                    } else if (text.isNotEmpty()) {
                        if (skipStack.lastOrNull() == true) continue
                        val isBackground = bgStack.lastOrNull() == true
                        val cleanText = text.replace(Regex("""\s+"""), " ")
                        if (cleanText.isNotEmpty()) {
                            if (isBackground) bgPieces.add(currentSpanMs to cleanText)
                            else fgPieces.add(currentSpanMs to cleanText)
                        }
                    }
                }
                
                fun buildLine(pieces: List<Pair<Long, String>>, isBg: Boolean) {
                    if (pieces.isEmpty()) return
                    val sb = java.lang.StringBuilder()
                    
                    if (isBg) sb.append("{bg}")
                    if (agent != null) sb.append("{agent:$agent}")
                    
                    var lastMs = -1L
                    for ((ms, text) in pieces) {
                        if (text.isBlank()) {
                            sb.append(text)
                            continue
                        }
                        if (ms != lastMs) {
                            sb.append("<${formatTime(ms)}>")
                            lastMs = ms
                        }
                        sb.append(text)
                    }
                    val richText = sb.toString().trim()
                    if (richText.isNotEmpty()) {
                        getOrPut(lineStartMs) { mutableListOf() }.add(richText)
                    }
                }
                
                buildLine(fgPieces, false)
                buildLine(bgPieces, true)
            }
        }.entries.sortedBy { it.key }
        
        return lines.joinToString("\n") { (timeMs, list) ->
            list.joinToString("\n") { text ->
                "[${formatTime(timeMs)}]$text"
            }
        }
    }

    private fun formatTime(ms: Long): String {
        val minutes = (ms / 60000).toString().padStart(2, '0')
        val seconds = ((ms % 60000) / 1000).toString().padStart(2, '0')
        val millis = ((ms % 1000) / 10).toString().padStart(2, '0')
        return "$minutes:$seconds.$millis"
    }

    private fun timeToMs(value: String?): Long? {
        val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (raw.endsWith("ms")) return raw.dropLast(2).toDoubleOrNull()?.toLong()
        val stripped = raw.removeSuffix("s")
        val parts = stripped.split(':')
        val seconds = when (parts.size) {
            1 -> parts[0].toDoubleOrNull()
            2 -> parts[0].toDoubleOrNull()?.let { m -> parts[1].toDoubleOrNull()?.let { m * 60 + it } }
            3 -> parts[0].toDoubleOrNull()?.let { h ->
                parts[1].toDoubleOrNull()?.let { m ->
                    parts[2].toDoubleOrNull()?.let { h * 3600 + m * 60 + it }
                }
            }
            else -> null
        } ?: return null
        return (seconds * 1000).toLong()
    }
}
