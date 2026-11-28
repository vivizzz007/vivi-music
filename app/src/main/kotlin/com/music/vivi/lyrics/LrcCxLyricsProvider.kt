/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.lyrics

import android.content.Context
import com.music.vivi.constants.EnableLrcCxKey
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs

object LrcCxLyricsProvider : LyricsProvider {
    override val name = "LrcCx"

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
                url("https://api.lrc.cx/")
            }
            expectSuccess = true
        }
    }

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableLrcCxKey] ?: true

    @Serializable
    data class Hit(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val duration: Double? = null,
        val lrc: String? = null,
        val lrc_ttml: String? = null,
        val score: Double? = null,
    )

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = runCatching {
        val hits = searchHits(title, artist, album)
            .filter { !it.lrc_ttml.isNullOrBlank() || !it.lrc.isNullOrBlank() }

        val match = bestHit(hits, duration)
            ?: throw IllegalStateException("No lyrics found on api.lrc.cx")

        extractLyrics(match)
            ?: throw IllegalStateException("Empty lyrics returned from api.lrc.cx")
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        val hits = runCatching { searchHits(title, artist, album) }.getOrNull() ?: return
        var count = 0
        for (hit in hits) {
            if (count >= 5) break
            currentCoroutineContext().ensureActive()
            if (isDurationCompatible(hit.duration, duration)) {
                val text = extractLyrics(hit)
                if (!text.isNullOrEmpty()) {
                    callback(text)
                    count++
                }
            }
        }
    }

    private fun extractLyrics(hit: Hit): String? {
        val ttml = hit.lrc_ttml?.trim()
        if (!ttml.isNullOrEmpty()) {
            val parsed = parseTtmlToLrc(ttml).trim()
            if (parsed.isNotEmpty()) return parsed
        }
        return hit.lrc?.trim()?.takeIf { it.isNotEmpty() }
    }

    private suspend fun searchHits(title: String, artist: String, album: String?): List<Hit> =
        client.get("jsonapi") {
            parameter("title", title)
            if (artist.isNotBlank()) parameter("artist", artist)
            if (!album.isNullOrBlank()) parameter("album", album)
        }.body()

    private fun bestHit(hits: List<Hit>, durationSeconds: Int): Hit? {
        if (hits.isEmpty()) return null
        val compatible = hits.filter { isDurationCompatible(it.duration, durationSeconds) }
        val pool = compatible.ifEmpty { hits }
        // Prefer word-by-word TTML hits when available
        val wordSynced = pool.firstOrNull { !it.lrc_ttml.isNullOrBlank() }
        if (wordSynced != null) return wordSynced
        if (durationSeconds <= 0) return pool.first()
        val target = durationSeconds.toDouble()
        return compatible.firstOrNull()
            ?: hits.minByOrNull { it.duration?.let { d -> abs(d - target) } ?: Double.MAX_VALUE }
    }

    private fun isDurationCompatible(hitDuration: Double?, durationSeconds: Int): Boolean {
        if (durationSeconds <= 0 || hitDuration == null || hitDuration <= 0.0) return true
        // api.lrc.cx durations may be in seconds or milliseconds depending on upstream source
        val hitSeconds = if (hitDuration > 1000.0) hitDuration / 1000.0 else hitDuration
        return abs(hitSeconds - durationSeconds) <= 5.0
    }

    private fun parseTtmlToLrc(rawTtml: String): String {
        val ttml = rawTtml.replace("\uFEFF", "")
        val pRegex = Regex("""<p([^>]*)>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)
        val tokenRegex = Regex("""(<[^>]+>)|([^<]+)""")
        val beginRegex = Regex("""begin="([^"]+)"""")
        val endRegex = Regex("""end="([^"]+)"""")
        val roleRegex = Regex("""(?:ttm:role|role)="([^"]+)"""")
        val agentRegex = Regex("""(?:ttm:agent|agent)="([^"]+)"""")

        val lines = buildMap<Long, MutableList<String>> {
            for (pMatch in pRegex.findAll(ttml)) {
                val pAttrs = pMatch.groupValues[1]
                val innerHtml = pMatch.groupValues[2]

                val pBeginStr = beginRegex.find(pAttrs)?.groupValues?.get(1) ?: continue
                val lineStartMs = timeToMs(pBeginStr) ?: continue
                val lineEndMs = timeToMs(endRegex.find(pAttrs)?.groupValues?.get(1))
                val agent = agentRegex.find(pAttrs)?.groupValues?.get(1)

                val fgPieces = mutableListOf<Pair<Long, String>>()
                val bgPieces = mutableListOf<Pair<Long, String>>()
                var currentSpanMs = lineStartMs
                var maxFgEndMs = -1L
                var maxBgEndMs = -1L

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
                            val isSkip = (role == "x-translation" || role == "x-roman") || (skipStack.lastOrNull() ?: false)
                            val isBg = (role == "x-bg") || (bgStack.lastOrNull() ?: false)

                            bgStack.add(isBg)
                            skipStack.add(isSkip)

                            val tEndMs = timeToMs(endRegex.find(tag)?.groupValues?.get(1))
                            if (!isSkip && tEndMs != null) {
                                if (isBg) maxBgEndMs = maxOf(maxBgEndMs, tEndMs)
                                else maxFgEndMs = maxOf(maxFgEndMs, tEndMs)
                            }

                            val tBegin = beginRegex.find(tag)?.groupValues?.get(1)
                            val tMs = tBegin?.let(::timeToMs)
                            if (tMs != null) {
                                timeStack.add(tMs)
                                currentSpanMs = tMs
                            } else {
                                timeStack.add(currentSpanMs)
                            }
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

                fun buildLine(pieces: List<Pair<Long, String>>, isBg: Boolean, endMs: Long?) {
                    if (pieces.isEmpty()) return
                    val sb = StringBuilder()
                    if (isBg) sb.append("{bg}")
                    if (agent != null) sb.append("{agent:$agent}")

                    // Only emit word-level <MM:SS.mm> tags when at least one span has a
                    // timestamp different from lineStartMs (real word-sync TTML).
                    val hasRealSpanTiming = pieces.any { (ms, _) -> ms != lineStartMs }

                    var lastMs = -1L
                    var hasSpanTags = false
                    for ((ms, text) in pieces) {
                        if (text.isBlank()) {
                            sb.append(text)
                            continue
                        }
                        if (hasRealSpanTiming && ms != lastMs) {
                            sb.append("<${formatTime(ms)}>")
                            lastMs = ms
                            hasSpanTags = true
                        }
                        sb.append(text)
                    }
                    if (hasSpanTags && endMs != null && endMs > lastMs) {
                        sb.append("<${formatTime(endMs)}>")
                    }
                    val richText = sb.toString().trim()
                    if (richText.isNotEmpty()) {
                        getOrPut(lineStartMs) { mutableListOf() }.add(richText)
                    }
                }

                buildLine(fgPieces, false, if (maxFgEndMs > 0L) maxFgEndMs else lineEndMs)
                buildLine(bgPieces, true, if (maxBgEndMs > 0L) maxBgEndMs else lineEndMs)
            }
        }.entries.sortedBy { it.key }

        return lines.joinToString("\n") { (timeMs, list) ->
            list.joinToString("\n") { text -> "[${formatTime(timeMs)}]$text" }
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

