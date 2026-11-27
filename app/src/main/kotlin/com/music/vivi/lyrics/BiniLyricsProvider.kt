/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.lyrics

import android.content.Context
import com.music.vivi.constants.EnableBiniLyricsKey
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Lyrics provider backed by the Binimum API.
 *
 * Endpoint: `GET https://lyrics-api.binimum.org/getLyrics?track=...&artist=...`
 * Returns a JSON with a TTML URL, which is then parsed into RichSync LRC format.
 */
object BiniLyricsProvider : LyricsProvider {

    override val name = "Bini Lyrics"

    private const val BASE_URL = "https://lyrics-api.binimum.org"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableBiniLyricsKey] ?: true

    suspend fun identify(
        title: String,
        artist: String,
        duration: Int = -1,
        album: String? = null,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = "$BASE_URL/getLyrics".toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("track", title)
                ?.addQueryParameter("artist", artist)
            if (duration > 0) urlBuilder?.addQueryParameter("duration", duration.toString())
            if (!album.isNullOrBlank()) urlBuilder?.addQueryParameter("album", album)
            val url = urlBuilder?.build()?.toString() ?: return@withContext null

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext null
            }

            val body = response.body?.string() ?: run {
                response.close()
                return@withContext null
            }
            response.close()

            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return@withContext null
            if (results.length() == 0) return@withContext null
            val first = results.getJSONObject(0)
            first.optString("isrc").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = "$BASE_URL/getLyrics".toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("track", title)
                ?.addQueryParameter("artist", artist)
            if (duration > 0) urlBuilder?.addQueryParameter("duration", duration.toString())
            if (!album.isNullOrBlank()) urlBuilder?.addQueryParameter("album", album)
            val url = urlBuilder?.build()?.toString() ?: return@withContext Result.failure(Exception("Invalid URL"))

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext Result.failure(Exception("Bini Lyrics API returned ${response.code}"))
            }

            val body = response.body?.string() ?: run {
                response.close()
                return@withContext Result.failure(Exception("Empty API response"))
            }
            response.close()

            val json = JSONObject(body)
            val results = json.optJSONArray("results")
            if (results == null || results.length() == 0) {
                return@withContext Result.failure(Exception("Bini Lyrics: no results found"))
            }

            // Get the first result's TTML url
            val firstResult = results.getJSONObject(0)
            val lyricsUrl = firstResult.optString("lyricsUrl").takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(Exception("Bini Lyrics: no lyricsUrl in result"))

            // Fetch the TTML
            val ttmlReq = Request.Builder().url(lyricsUrl).build()
            val ttmlRes = client.newCall(ttmlReq).execute()
            if (!ttmlRes.isSuccessful) {
                ttmlRes.close()
                return@withContext Result.failure(Exception("Failed to fetch TTML"))
            }

            val ttmlBody = ttmlRes.body?.string() ?: run {
                ttmlRes.close()
                return@withContext Result.failure(Exception("Empty TTML response"))
            }
            ttmlRes.close()

            val lrc = ttmlToLrc(ttmlBody)
            if (lrc.isNotBlank()) {
                Result.success(lrc)
            } else {
                Result.failure(Exception("Failed to parse TTML or empty lyrics"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Parsing Logic
    // ──────────────────────────────────────────────────────────────────────

    private val pRegex = """<p([^>]*)>(.*?)</p>""".toRegex(RegexOption.DOT_MATCHES_ALL)
    private val tokenRegex = """(<[^>]+>)|([^<]+)""".toRegex()
    private val beginRegex = """begin=["']([\w:.]+)["']""".toRegex()
    private val endRegex = """end=["']([\w:.]+)["']""".toRegex()
    private val roleRegex = """(?:ttm:role|role)=["']([^"']+)["']""".toRegex()
    private val agentRegex = """(?:ttm:agent|agent)=["']([^"']+)["']""".toRegex()

    /**
     * Parse Apple Music TTML format and convert to standard LRC or RichSync LRC.
     * Visible for testing.
     */
    internal fun ttmlToLrc(rawTtml: String): String {
        var ttml = rawTtml.replace("\uFEFF", "").trim()
        if (ttml.contains("&lt;tt", ignoreCase = true)) {
            ttml = ttml.replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&apos;", "'").replace("&amp;", "&")
        }
        if (!ttml.contains("<p", ignoreCase = true) && Regex("""\[\d{1,2}:\d{2}\.\d{2,3}]""").containsMatchIn(ttml)) {
            return ttml
        }
        val lrcBuilder = java.lang.StringBuilder()

        for (pMatch in pRegex.findAll(ttml)) {
            val pAttrs = pMatch.groupValues[1]
            val pBeginStr = beginRegex.find(pAttrs)?.groupValues?.get(1) ?: continue
            val lineStartMs = parseTtmlTimeToMs(pBeginStr) ?: continue
            val lineEndMs = parseTtmlTimeToMs(endRegex.find(pAttrs)?.groupValues?.get(1))
            val agent = agentRegex.find(pAttrs)?.groupValues?.get(1)
            val innerHtml = pMatch.groupValues[2]

            val fgPieces = mutableListOf<Pair<Long?, String>>()
            val bgPieces = mutableListOf<Pair<Long?, String>>()
            var currentSpanMs: Long? = null
            var maxFgEndMs = -1L
            var maxBgEndMs = -1L

            val bgStack = mutableListOf<Boolean>()
            val skipStack = mutableListOf<Boolean>()
            val timeStack = mutableListOf<Long?>()

            for (tokenMatch in tokenRegex.findAll(innerHtml)) {
                val tag = tokenMatch.groupValues[1]
                val text = tokenMatch.groupValues[2]

                if (tag.isNotEmpty()) {
                    if (tag.startsWith("</")) {
                        if (bgStack.isNotEmpty()) bgStack.removeLast()
                        if (skipStack.isNotEmpty()) skipStack.removeLast()
                        if (timeStack.isNotEmpty()) {
                            timeStack.removeLast()
                            currentSpanMs = timeStack.lastOrNull()
                        }
                    } else if (tag.startsWith("<span") || tag.startsWith("<s ")) {
                        val role = roleRegex.find(tag)?.groupValues?.get(1)
                        val isSkip = (role == "x-translation" || role == "x-roman") || (skipStack.lastOrNull() ?: false)
                        val isBg = (role == "x-bg") || (bgStack.lastOrNull() ?: false)

                        bgStack.add(isBg)
                        skipStack.add(isSkip)

                        val tEndMs = parseTtmlTimeToMs(endRegex.find(tag)?.groupValues?.get(1))
                        if (!isSkip && tEndMs != null) {
                            if (isBg) maxBgEndMs = maxOf(maxBgEndMs, tEndMs)
                            else maxFgEndMs = maxOf(maxFgEndMs, tEndMs)
                        }

                        val tMs = parseTtmlTimeToMs(beginRegex.find(tag)?.groupValues?.get(1))
                        if (tMs != null) {
                            timeStack.add(tMs)
                            currentSpanMs = tMs
                        } else {
                            timeStack.add(currentSpanMs)
                        }
                    }
                } else if (text.isNotEmpty()) {
                    if (skipStack.lastOrNull() == true) continue
                    val cleanText = text.replace(Regex("""\s+"""), " ")
                    if (cleanText.isNotEmpty()) {
                        val isBackground = bgStack.lastOrNull() == true
                        if (isBackground) bgPieces.add(currentSpanMs to cleanText)
                        else fgPieces.add(currentSpanMs to cleanText)
                    }
                }
            }

            fun appendLine(pieces: List<Pair<Long?, String>>, isBg: Boolean, endMs: Long?) {
                if (pieces.isEmpty()) return
                val lineSb = java.lang.StringBuilder()
                lineSb.append("[${formatLrcTimeMs(lineStartMs)}]")
                if (isBg) lineSb.append("{bg}")
                if (agent != null) lineSb.append("{agent:$agent}")

                var lastMs = -1L
                var hasSpanTags = false
                for ((ms, text) in pieces) {
                    if (text.isBlank()) {
                        lineSb.append(text)
                        continue
                    }
                    if (ms != null && ms != lastMs) {
                        lineSb.append("<${formatLrcTimeMs(ms)}> ")
                        lastMs = ms
                        hasSpanTags = true
                    }
                    lineSb.append(text.trimStart())
                }
                if (hasSpanTags && endMs != null && endMs > lineStartMs && endMs > lastMs) {
                    lineSb.append(" <${formatLrcTimeMs(endMs)}>")
                }
                val formatted = lineSb.toString().trimEnd()
                if (formatted.isNotEmpty()) {
                    lrcBuilder.append(formatted).append("\n")
                }
            }

            appendLine(fgPieces, false, if (maxFgEndMs > 0L) maxFgEndMs else lineEndMs)
            appendLine(bgPieces, true, if (maxBgEndMs > 0L) maxBgEndMs else lineEndMs)
        }

        return lrcBuilder.toString().trimEnd()
    }

    private fun formatLrcTimeMs(ms: Long): String {
        val safeMs = ms.coerceAtLeast(0L)
        val mm = safeMs / 60000
        val ss = (safeMs % 60000) / 1000
        val cs = (safeMs % 1000) / 10
        return String.format(Locale.US, "%02d:%02d.%02d", mm, ss, cs)
    }

    private fun parseTtmlTimeToMs(timeStr: String?): Long? {
        val raw = timeStr?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val isMs = raw.endsWith("ms", ignoreCase = true)
        val cleanStr = raw.replace(Regex("[a-zA-Z]+$"), "")
        val parts = cleanStr.split(":")

        val seconds = when (parts.size) {
            3 -> {
                val h = parts[0].toDoubleOrNull() ?: return null
                val m = parts[1].toDoubleOrNull() ?: return null
                val s = parts[2].toDoubleOrNull() ?: return null
                h * 3600.0 + m * 60.0 + s
            }
            2 -> {
                val m = parts[0].toDoubleOrNull() ?: return null
                val s = parts[1].toDoubleOrNull() ?: return null
                m * 60.0 + s
            }
            else -> {
                cleanStr.toDoubleOrNull() ?: return null
            }
        }
        return if (isMs) seconds.toLong() else (seconds * 1000.0).toLong()
    }
}
