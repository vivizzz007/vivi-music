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

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
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
        val response = client.get("search/songs") {
            parameter("query", query)
            parameter("limit", 5)   // fetch top-5 candidates; we only use #1
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Saavn search failed: HTTP ${response.status.value}")
        }

        val body = response.body<SaavnSearchResponse>()
        val results = body.data?.results.orEmpty()

        if (!body.success || results.isEmpty()) {
            throw NoSuchElementException("No songs found on JioSaavn for: \"$query\"")
        }

        results
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
            val response = client.get("songs/$saavnSongId")

            if (response.status != HttpStatusCode.OK) return@runCatching null

            val body = response.body<SaavnSongResponse>()
            if (!body.success) return@runCatching null

            val urls = body.data.firstOrNull()?.downloadUrl.orEmpty()
                .filter { it.url.isNotBlank() }

            if (urls.isEmpty()) return@runCatching null

            // 1. Try the exact requested quality
            urls.firstOrNull { it.quality.equals(quality, ignoreCase = true) }?.url
                // 2. Fall back to 320kbps if available
                ?: urls.firstOrNull { it.quality.equals("320kbps", ignoreCase = true) }?.url
                // 3. Fall back to highest bitrate (last entry tends to be highest)
                ?: urls.lastOrNull()?.url
        }.getOrNull()
}
