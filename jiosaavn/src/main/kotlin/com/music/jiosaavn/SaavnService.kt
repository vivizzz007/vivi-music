/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * JioSaavn audio streaming service.
 * Uses the Melo API (meloapi.vercel.app) which is an open wrapper around JioSaavn.
 *
 * API endpoints used:
 *   - GET /api/search/songs?query={q}        → search songs by name+artist
 *   - GET /api/songs/{id}                    → get song details + downloadUrl list
 */

package com.music.jiosaavn

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ─── Data models ────────────────────────────────────────────────────────────

@Serializable
data class SaavnDownloadUrl(
    @SerialName("quality") val quality: String = "",
    @SerialName("url")     val url: String     = ""
)

@Serializable
data class SaavnImage(
    @SerialName("quality") val quality: String = "",
    @SerialName("url")     val url: String     = ""
)

@Serializable
data class SaavnArtistItem(
    @SerialName("id")   val id: String   = "",
    @SerialName("name") val name: String = ""
)

@Serializable
data class SaavnArtists(
    @SerialName("primary")  val primary:  List<SaavnArtistItem> = emptyList(),
    @SerialName("featured") val featured: List<SaavnArtistItem> = emptyList(),
    @SerialName("all")      val all:      List<SaavnArtistItem> = emptyList()
)

@Serializable
data class SaavnSong(
    @SerialName("id")              val id:              String                 = "",
    @SerialName("name")            val name:            String                 = "",
    @SerialName("duration")        val duration:        Int?                   = null,
    @SerialName("explicitContent") val explicitContent: Boolean                = false,
    @SerialName("artists")         val artists:         SaavnArtists           = SaavnArtists(),
    @SerialName("image")           val image:           List<SaavnImage>       = emptyList(),
    @SerialName("downloadUrl")     val downloadUrl:     List<SaavnDownloadUrl> = emptyList()
)

// ─── Search response ─────────────────────────────────────────────────────────

@Serializable
data class SaavnSearchSongsResult(
    @SerialName("total")   val total: Int             = 0,
    @SerialName("results") val results: List<SaavnSong> = emptyList()
)

@Serializable
data class SaavnSearchResponse(
    @SerialName("success") val success: Boolean                    = false,
    @SerialName("data")    val data:    SaavnSearchSongsResult?    = null
)

// ─── Song-by-ID response ─────────────────────────────────────────────────────

@Serializable
data class SaavnSongResponse(
    @SerialName("success") val success: Boolean          = false,
    @SerialName("data")    val data:    List<SaavnSong>  = emptyList()
)

// ─── Service ─────────────────────────────────────────────────────────────────

object SaavnService {

    private const val TAG = "SaavnService"
    private const val BASE_URL = "https://meloapi.vercel.app/api/"

    private val json = Json {
        isLenient         = true
        ignoreUnknownKeys = true
        explicitNulls     = false
    }

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                // Keep timeouts short so that a slow/unavailable Saavn response
                // falls back to YouTube quickly without the user noticing a stall.
                requestTimeoutMillis = 4_000
                connectTimeoutMillis = 3_000
                socketTimeoutMillis  = 4_000
            }
            defaultRequest {
                url(BASE_URL)
                headers.append(HttpHeaders.Accept, "application/json")
                headers.append(HttpHeaders.UserAgent, "ViviMusic/1.0")
            }
            expectSuccess = false
        }
    }

    /**
     * Search for songs on JioSaavn by a free-form query (title + artist recommended).
     *
     * @return Result wrapping a list of matched [SaavnSong]s, or failure if the
     *         request fails or returns no results.
     */
    suspend fun searchSongs(query: String): Result<List<SaavnSong>> = runCatching {
        Log.d(TAG, "searchSongs: query=\"$query\"")
        val response = client.get("search/songs") {
            parameter("query", query)
            parameter("limit", 5)   // fetch top-5 candidates; we only use #1
        }

        Log.d(TAG, "searchSongs: HTTP response status: ${response.status}")
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Saavn search failed: HTTP ${response.status.value}")
        }

        val body = response.body<SaavnSearchResponse>()
        val results = body.data?.results.orEmpty()
        Log.d(TAG, "searchSongs: success=${body.success}, total=${body.data?.total}, results.size=${results.size}")

        if (!body.success || results.isEmpty()) {
            throw NoSuchElementException("No songs found on JioSaavn for: \"$query\"")
        }

        results.forEachIndexed { index, song ->
            Log.d(TAG, "  - Result #$index: id=${song.id}, name=\"${song.name}\", duration=${song.duration}, artists=${song.artists.primary.map { it.name }}")
        }

        results
    }.onFailure {
        Log.e(TAG, "searchSongs: failed for query=\"$query\"", it)
    }

    /**
     * Fetch the [SaavnSong] detail for a known Saavn song ID and extract the
     * best stream URL matching [quality].
     *
     * The JioSaavn quality string is expected to be "320kbps", "160kbps", or "96kbps".
     * If the exact quality is unavailable, the highest available quality is returned
     * as a fallback. Returns null only if no downloadUrl entries exist at all.
     */
    suspend fun getBestStreamUrl(saavnSongId: String, quality: String): String? =
        runCatching {
            Log.d(TAG, "getBestStreamUrl: saavnSongId=$saavnSongId, quality=$quality")
            val response = client.get("songs/$saavnSongId")

            Log.d(TAG, "getBestStreamUrl: HTTP response status: ${response.status}")
            if (response.status != HttpStatusCode.OK) return@runCatching null

            val body = response.body<SaavnSongResponse>()
            Log.d(TAG, "getBestStreamUrl: success=${body.success}, data size=${body.data.size}")
            if (!body.success) return@runCatching null

            val urls = body.data.firstOrNull()?.downloadUrl.orEmpty()
                .filter { it.url.isNotBlank() }

            Log.d(TAG, "getBestStreamUrl: found ${urls.size} download URLs")
            urls.forEach { 
                Log.d(TAG, "  - URL quality=${it.quality}, url=${it.url}")
            }

            if (urls.isEmpty()) return@runCatching null

            // 1. Try the exact requested quality
            val exactUrl = urls.firstOrNull { it.quality.equals(quality, ignoreCase = true) }?.url
            if (exactUrl != null) {
                Log.d(TAG, "getBestStreamUrl: selected exact quality $quality URL: $exactUrl")
                return@runCatching exactUrl
            }
            // 2. Fall back to 320kbps if available
            val fallback320 = urls.firstOrNull { it.quality.equals("320kbps", ignoreCase = true) }?.url
            if (fallback320 != null) {
                Log.d(TAG, "getBestStreamUrl: exact quality $quality not found, falling back to 320kbps URL: $fallback320")
                return@runCatching fallback320
            }
            // 3. Fall back to highest bitrate (last entry tends to be highest)
            val fallbackLast = urls.lastOrNull()?.url
            Log.d(TAG, "getBestStreamUrl: falling back to last available URL: $fallbackLast")
            fallbackLast
        }.onFailure {
            Log.e(TAG, "getBestStreamUrl failed for saavnSongId=$saavnSongId", it)
        }.getOrNull()

    /**
     * Retrieve the byte size of a JioSaavn stream URL by issuing an HTTP HEAD request
     * and reading the [Content-Length] response header.
     *
     * This is a lightweight call — no audio data is transferred. The result is used
     * to populate [contentLength] in the playback [Format] so that the downloader and
     * player can report accurate file sizes.
     *
     * Returns null if the server does not advertise a content length or if the request
     * fails for any reason (network error, timeout, etc.). Null is always safe because
     * ExoPlayer will re-determine the size from the actual stream headers when needed.
     */
    suspend fun getContentLength(url: String): Long? = runCatching {
        Log.d(TAG, "getContentLength: url=$url")
        // Use a dedicated lightweight client for HEAD requests — we don't need JSON
        // negotiation here and we want a short timeout so it never blocks playback.
        val headClient = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 3_000
                connectTimeoutMillis = 2_000
                socketTimeoutMillis  = 3_000
            }
            expectSuccess = false
            followRedirects = true
        }
        headClient.use { c ->
            val response = c.head(url)
            Log.d(TAG, "getContentLength: HTTP status=${response.status}")
            if (response.status == HttpStatusCode.OK ||
                response.status == HttpStatusCode.PartialContent) {
                val len = response.headers[io.ktor.http.HttpHeaders.ContentLength]?.toLongOrNull()
                Log.d(TAG, "getContentLength: resolved length=$len")
                len
            } else {
                null
            }
        }
    }.onFailure {
        Log.e(TAG, "getContentLength failed for url=$url", it)
    }.getOrNull()
}
