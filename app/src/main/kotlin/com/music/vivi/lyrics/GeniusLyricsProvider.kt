/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.lyrics

import android.content.Context
import com.music.vivi.constants.EnableGeniusLyricsKey
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Fallback plain-text scraper for Genius.com.
 * Used when none of the time-synced providers have lyrics for a track.
 */
object GeniusLyricsProvider : LyricsProvider {
    override val name = "Genius"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(8, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableGeniusLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanTitle = cleanSearchTerm(title)
            val cleanArtist = cleanSearchTerm(artist)

            val songUrl = searchSongUrl("$cleanArtist $cleanTitle".trim(), cleanTitle, cleanArtist)
                ?: searchSongUrl(cleanTitle, cleanTitle, cleanArtist)
                ?: throw IllegalStateException("Genius: song not found")

            val html = fetchHtml(songUrl) ?: throw IllegalStateException("Genius: could not fetch page")
            val lyrics = parseLyricsFromHtml(html)
            if (lyrics.isBlank()) throw IllegalStateException("Genius: lyrics container empty")
            lyrics
        }
    }

    private fun searchSongUrl(query: String, targetTitle: String, targetArtist: String): String? {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://genius.com/api/search/multi?q=$encoded"
        val body = httpGet(url) ?: return null

        return runCatching {
            val root = JSONObject(body)
            val response = root.optJSONObject("response") ?: return null
            val sections = response.optJSONArray("sections") ?: return null

            for (i in 0 until sections.length()) {
                val sec = sections.getJSONObject(i)
                if (sec.optString("type") == "song") {
                    val hits = sec.optJSONArray("hits") ?: continue
                    val candidates = (0 until hits.length()).mapNotNull { hits.getJSONObject(it).optJSONObject("result") }
                    val best = bestMatch(candidates, targetTitle, targetArtist)
                    if (best != null) {
                        return best.optString("url").takeIf { it.isNotBlank() }
                    }
                }
            }
            null
        }.getOrNull()
    }

    private fun bestMatch(
        candidates: List<JSONObject>,
        targetTitle: String,
        targetArtist: String,
    ): JSONObject? {
        if (candidates.isEmpty()) return null
        val normTitle = targetTitle.lowercase(Locale.ROOT)
        val normArtist = targetArtist.lowercase(Locale.ROOT)

        var bestObj: JSONObject? = null
        var highestScore = -1

        for (item in candidates) {
            val title = item.optString("title").lowercase(Locale.ROOT)
            val artist = item.optString("artist_names").lowercase(Locale.ROOT)
            var score = 0

            val titleMatches = normTitle.isNotBlank() && (title == normTitle || title.contains(normTitle) || normTitle.contains(title))
            val artistMatches = normArtist.isNotBlank() && (artist == normArtist || artist.contains(normArtist) || normArtist.contains(artist))

            if (!titleMatches && !artistMatches) continue

            if (title == normTitle) score += 50
            else if (titleMatches) score += 25

            if (artistMatches) {
                if (artist == normArtist) score += 40
                else score += 20
            }

            val path = item.optString("path", "")
            if (path.contains("translation", ignoreCase = true) && !normTitle.contains("translation")) score -= 30
            if (path.contains("tracklist", ignoreCase = true) || path.contains("album-art", ignoreCase = true)) score -= 50

            if (score > highestScore) {
                highestScore = score
                bestObj = item
            }
        }
        return if (highestScore > 0) bestObj else null
    }

    private fun parseLyricsFromHtml(html: String): String {
        val containerRegex = Regex("""<div[^>]*data-lyrics-container="true"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
        var matches = containerRegex.findAll(html).toList()
        if (matches.isEmpty()) {
            val legacyRegex = Regex("""<div[^>]*class="[^"]*lyrics[^"]*"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
            matches = legacyRegex.findAll(html).toList()
        }
        if (matches.isEmpty()) return ""

        val sb = StringBuilder()
        for (match in matches) {
            var content = match.groupValues[1]
            // Remove scripts and styles
            content = content.replace(Regex("""<(script|style)[^>]*>.*?</\1>""", RegexOption.DOT_MATCHES_ALL), "")
            // Convert <br> and <p> to newlines
            content = content.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            content = content.replace(Regex("""</p>""", RegexOption.IGNORE_CASE), "\n\n")
            // Strip remaining tags
            content = content.replace(Regex("""<[^>]+>"""), "")
            // Unescape HTML entities
            content = decodeEntities(content)
            sb.append(content).append("\n")
        }

        return sb.toString()
            .replace('\u00A0', ' ')
            .replace('\u200B', ' ')
            .replace('\uFEFF', ' ')
            .replace(Regex("""\d*Embed$"""), "")
            .replace(Regex("""You might also like.*""", RegexOption.DOT_MATCHES_ALL), "")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    private fun decodeEntities(text: String): String {
        return text.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace(Regex("""&#(\d+);""")) { match ->
                match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
            }
            .replace(Regex("""&#x([0-9a-fA-F]+);""")) { match ->
                match.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: match.value
            }
    }

    private fun cleanSearchTerm(term: String): String =
        term.replace(Regex("""\([^)]*\)"""), " ")
            .replace(Regex("""\[[^]]*]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun httpGet(url: String): String? = runCatching {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "ViviMusic")
            .build()
        httpClient.newCall(req).execute().use { res ->
            if (res.isSuccessful) res.body?.string() else null
        }
    }.getOrNull()

    private fun fetchHtml(url: String): String? = httpGet(url)
}

