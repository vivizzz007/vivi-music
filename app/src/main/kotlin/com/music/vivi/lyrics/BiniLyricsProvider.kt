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

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/getLyrics".toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("track", title)
                ?.addQueryParameter("artist", artist)
                ?.build()
                ?.toString() ?: return@withContext Result.failure(Exception("Invalid URL"))

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

    private val pRegex = """<p\s+begin="([\d.]+)"[^>]*>(.*?)</p>""".toRegex(RegexOption.DOT_MATCHES_ALL)
    private val spanRegex = """<span\s+begin="([\d.]+)"[^>]*>(.*?)</span>""".toRegex(RegexOption.DOT_MATCHES_ALL)

    /**
     * Parse Apple Music TTML format and convert to standard LRC or RichSync LRC
     */
    private fun ttmlToLrc(ttml: String): String {
        val lrcBuilder = java.lang.StringBuilder()

        for (pMatch in pRegex.findAll(ttml)) {
            val pBeginSec = pMatch.groupValues[1].toFloatOrNull() ?: continue
            val innerHtml = pMatch.groupValues[2]

            val mainTime = formatLrcTime(pBeginSec)
            lrcBuilder.append("[$mainTime]")

            val spanMatches = spanRegex.findAll(innerHtml).toList()
            if (spanMatches.isNotEmpty()) {
                for (spanMatch in spanMatches) {
                    val spanBeginSec = spanMatch.groupValues[1].toFloatOrNull() ?: continue
                    // Clean HTML entities or nested tags from the word just in case
                    val word = spanMatch.groupValues[2].replace(Regex("<[^>]*>"), "").trim()
                    if (word.isNotEmpty()) {
                        val spanTime = formatLrcTime(spanBeginSec)
                        lrcBuilder.append("<$spanTime> $word ")
                    }
                }
            } else {
                // Unsynced spans or just plain text inside <p>
                val plainText = innerHtml.replace(Regex("<[^>]*>"), "").trim()
                lrcBuilder.append(plainText)
            }
            lrcBuilder.append("\n")
        }

        return lrcBuilder.toString().trimEnd()
    }

    private fun formatLrcTime(sec: Float): String {
        val mm = (sec / 60).toInt()
        val ss = sec % 60
        // Result format: MM:SS.xxx where x is fractional padded
        val ssStr = String.format(Locale.US, "%05.2f", ss) // "09.52", "45.10" etc
        // ViviMusic regex for Line Rich Sync expects \d{2,3} for ms block: [MM:SS.xx] or [MM:SS.xxx]
        return String.format(Locale.US, "%02d:%s", mm, ssStr)
    }
}
