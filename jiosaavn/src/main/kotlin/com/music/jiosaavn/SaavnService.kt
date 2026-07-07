/**
 * VIVI-SAAVN (C) 2026
 *
 * PROPRIETARY LICENSE:
 * This file is source-available for viewing. Copying, modification,
 * redistribution, or reuse in other applications is strictly prohibited.
 * Licensed exclusively for use in the official vivimusic application.
 *
 * JioSaavn audio streaming service.
 * Fetches search results and stream details directly from JioSaavn's public API endpoints,
 * and decrypts CDN media links locally on the device using DES-ECB.
 */

package com.music.jiosaavn

import android.util.Base64
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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

// ─── Data models for public client ──────────────────────────────────────────

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
data class SaavnAlbum(
    @SerialName("id")   val id:   String? = null,
    @SerialName("name") val name: String? = null
)

@Serializable
data class SaavnSong(
    @SerialName("id")              val id:              String                 = "",
    @SerialName("name")            val name:            String                 = "",
    @SerialName("duration")        val duration:        Int?                   = null,
    @SerialName("explicitContent") val explicitContent: Boolean                = false,
    @SerialName("artists")         val artists:         SaavnArtists           = SaavnArtists(),
    @SerialName("image")           val image:           List<SaavnImage>       = emptyList(),
    @SerialName("downloadUrl")     val downloadUrl:     List<SaavnDownloadUrl> = emptyList(),
    @SerialName("album")           val album:           SaavnAlbum?            = null
)

// ─── JioSaavn Raw API Response Models ───────────────────────────────────────

@Serializable
data class RawArtistMapItem(
    val id: String = "",
    val name: String = "",
    val role: String = "",
    val type: String = ""
)

@Serializable
data class RawArtistMap(
    @SerialName("primary_artists") val primaryArtists: List<RawArtistMapItem> = emptyList(),
    @SerialName("featured_artists") val featuredArtists: List<RawArtistMapItem> = emptyList(),
    val artists: List<RawArtistMapItem> = emptyList()
)

@Serializable
data class RawMoreInfo(
    val album_id: String = "",
    val album: String = "",
    @SerialName("encrypted_media_url") val encryptedMediaUrl: String = "",
    val duration: String = "",
    val artistMap: RawArtistMap = RawArtistMap()
)

@Serializable
data class RawSongItem(
    val id: String = "",
    val title: String = "",
    val type: String = "",
    val year: String = "",
    val image: String = "",
    val language: String = "",
    @SerialName("play_count") val playCount: String = "",
    @SerialName("explicit_content") val explicitContent: String = "",
    @SerialName("more_info") val moreInfo: RawMoreInfo = RawMoreInfo()
)

@Serializable
data class RawSearchResponse(
    val total: Int = 0,
    val start: Int = 0,
    val results: List<RawSongItem> = emptyList()
)

@Serializable
data class RawSongsResponse(
    val songs: List<RawSongItem> = emptyList()
)

// ─── Service ─────────────────────────────────────────────────────────────────

object SaavnService {

    private const val TAG = "SaavnService"

    private val BASE_URL = String(Base64.decode("aHR0cHM6Ly93d3cuamlvc2Fhdm4uY29tL2FwaS5waHA=", Base64.DEFAULT), Charsets.UTF_8)

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
                // falls back to YouTube quickly.
                requestTimeoutMillis = 6_000
                connectTimeoutMillis = 4_000
                socketTimeoutMillis  = 6_000
            }
            defaultRequest {
                url(BASE_URL)
                headers.append(HttpHeaders.Accept, "application/json")
                // Use a standard valid browser User-Agent to avoid client blocks
                headers.append(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
                headers.append("X-Forwarded-For", "49.36.0.1")
                headers.append("X-Real-IP", "49.36.0.1")
                headers.append("Accept-Language", "en-IN,en;q=0.9")
                headers.append(HttpHeaders.Cookie, "explicit_content=1")
            }
            expectSuccess = false
        }
    }

    /**
     * Decrypt the encrypted media URL string returned by JioSaavn using DES-ECB.
     */
    private fun decryptUrl(encryptedUrl: String): String {
        if (encryptedUrl.isBlank()) return ""
        Log.d(TAG, "decryptUrl: encryptedUrl length = ${encryptedUrl.length}")
        return try {
            val key = "38346591" // DES 8-byte key
            val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decodedBytes = Base64.decode(encryptedUrl, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            val decryptedUrl = String(decryptedBytes, Charsets.UTF_8).trim()
            Log.d(TAG, "decryptUrl: successfully decrypted: $decryptedUrl")
            decryptedUrl
        } catch (e: Exception) {
            Log.e(TAG, "JioSaavn URL decryption failed", e)
            ""
        }
    }

    /**
     * Decode and build direct CDN download URLs for different audio bitrates.
     */
    private fun createDownloadLinks(encryptedUrl: String): List<SaavnDownloadUrl> {
        Log.d(TAG, "createDownloadLinks: starting decryption for URL...")
        val decryptedUrl = decryptUrl(encryptedUrl)
        if (decryptedUrl.isBlank()) {
            Log.w(TAG, "createDownloadLinks: decryptedUrl is blank!")
            return emptyList()
        }

        val qualities = listOf(
            Pair("_96", "96kbps"),
            Pair("_160", "160kbps"),
            Pair("_320", "320kbps")
        )

        val suffixRegex = Regex("_(48|96|160|320)\\.(mp4|aac|mp3)$")
        val generatedUrls = qualities.map { (suffix, bitrate) ->
            val url = if (decryptedUrl.contains(suffixRegex)) {
                decryptedUrl.replace(suffixRegex) { match ->
                    "${suffix}.${match.groupValues[2]}"
                }
            } else {
                decryptedUrl.replace("_96", suffix)
            }
            SaavnDownloadUrl(quality = bitrate, url = url)
        }
        Log.d(TAG, "createDownloadLinks: generated URLs: ${generatedUrls.map { "${it.quality} -> ${it.url}" }}")
        return generatedUrls
    }

    /**
     * Clean and generate higher quality image URLs matching typical grid sizes.
     */
    private fun createImageLinks(link: String): List<SaavnImage> {
        if (link.isBlank()) return emptyList()
        val qualities = listOf("50x50", "150x150", "500x500")
        val qualityRegex = Regex("150x150|50x50")
        val protocolRegex = Regex("^http://")

        return qualities.map { quality ->
            val url = link.replace(qualityRegex, quality).replace(protocolRegex, "https://")
            SaavnImage(quality = quality, url = url)
        }
    }

    /**
     * Map the raw API structure received from JioSaavn into SaavnSong models used in playback.
     */
    private fun mapRawToSaavnSong(raw: RawSongItem): SaavnSong {
        val primaryArtists = raw.moreInfo.artistMap.primaryArtists.map {
            SaavnArtistItem(id = it.id, name = it.name)
        }
        val featuredArtists = raw.moreInfo.artistMap.featuredArtists.map {
            SaavnArtistItem(id = it.id, name = it.name)
        }
        val allArtists = raw.moreInfo.artistMap.artists.map {
            SaavnArtistItem(id = it.id, name = it.name)
        }

        return SaavnSong(
            id = raw.id,
            name = raw.title,
            duration = raw.moreInfo.duration.toIntOrNull(),
            explicitContent = raw.explicitContent == "1",
            artists = SaavnArtists(
                primary = primaryArtists,
                featured = featuredArtists,
                all = allArtists
            ),
            image = createImageLinks(raw.image),
            downloadUrl = createDownloadLinks(raw.moreInfo.encryptedMediaUrl),
            album = SaavnAlbum(
                id = raw.moreInfo.album_id,
                name = raw.moreInfo.album
            )
        )
    }

    /**
     * Search for songs on JioSaavn directly by a free-form query.
     */
    suspend fun searchSongs(query: String): Result<List<SaavnSong>> = runCatching {
        Log.d(TAG, "searchSongs: query=\"$query\"")
        val response = client.get("") {
            parameter("__call", "search.getResults")
            parameter("_format", "json")
            parameter("_marker", "0")
            parameter("api_version", "4")
            parameter("ctx", "wap6dot0")
            parameter("q", query)
            parameter("p", "1")
            parameter("n", "5")
        }

        Log.d(TAG, "searchSongs: HTTP response status: ${response.status}")
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Saavn search failed: HTTP ${response.status.value}")
        }

        val responseText = response.bodyAsText()
        val body = json.decodeFromString<RawSearchResponse>(responseText)
        val results = body.results.map { mapRawToSaavnSong(it) }
        Log.d(TAG, "searchSongs: results.size=${results.size}")

        if (results.isEmpty()) {
            throw NoSuchElementException("No songs found on JioSaavn for: \"$query\"")
        }

        results
    }.onFailure {
        Log.e(TAG, "searchSongs: failed for query=\"$query\"", it)
    }

    /**
     * Choose the best stream URL matching [quality] from a list of download URLs.
     * If the exact quality is not found, it falls back to 320kbps or the highest quality.
     */
    fun selectBestUrl(urls: List<SaavnDownloadUrl>, quality: String): String? {
        Log.d(TAG, "selectBestUrl: choosing quality=$quality out of: ${urls.map { "${it.quality} -> ${it.url}" }}")
        val filteredUrls = urls.filter { it.url.isNotBlank() }
        if (filteredUrls.isEmpty()) {
            Log.w(TAG, "selectBestUrl: list of URLs is empty!")
            return null
        }

        // 1. Try the exact requested quality
        val exactUrl = filteredUrls.firstOrNull { it.quality.equals(quality, ignoreCase = true) }?.url
        if (exactUrl != null) {
            Log.d(TAG, "selectBestUrl: exact match found for $quality: $exactUrl")
            return exactUrl
        }

        // 2. Fall back to 320kbps if available
        val fallback320 = filteredUrls.firstOrNull { it.quality.equals("320kbps", ignoreCase = true) }?.url
        if (fallback320 != null) {
            Log.d(TAG, "selectBestUrl: fallback to 320kbps: $fallback320")
            return fallback320
        }

        // 3. Fall back to highest bitrate (last entry tends to be highest)
        val highestUrl = filteredUrls.lastOrNull()?.url
        Log.d(TAG, "selectBestUrl: final fallback (highest available): $highestUrl")
        return highestUrl
    }

    /**
     * Fetch the [SaavnSong] detail for a known Saavn song ID and extract the
     * best stream URL matching [quality].
     */
    suspend fun getBestStreamUrl(saavnSongId: String, quality: String): String? {
        val result = runCatching {
            Log.d(TAG, "getBestStreamUrl: saavnSongId=$saavnSongId, quality=$quality")
            val response = client.get("") {
                parameter("__call", "song.getDetails")
                parameter("_format", "json")
                parameter("_marker", "0")
                parameter("api_version", "4")
                parameter("ctx", "wap6dot0")
                parameter("pids", saavnSongId)
            }

            Log.d(TAG, "getBestStreamUrl: HTTP response status: ${response.status}")
            if (response.status != HttpStatusCode.OK) {
                throw IllegalStateException("Saavn getBestStreamUrl failed: HTTP ${response.status.value}")
            }

            val responseText = response.bodyAsText()
            val body = json.decodeFromString<RawSongsResponse>(responseText)
            Log.d(TAG, "getBestStreamUrl: songs size=${body.songs.size}")

            val rawSong = body.songs.firstOrNull() ?: throw NoSuchElementException("Song not found")
            val saavnSong = mapRawToSaavnSong(rawSong)
            Log.d(TAG, "getBestStreamUrl: raw song details name=${rawSong.title}, encryptedUrl=${rawSong.moreInfo.encryptedMediaUrl}")

            selectBestUrl(saavnSong.downloadUrl, quality)
        }
        if (result.isSuccess) {
            return result.getOrNull()
        } else {
            Log.e(TAG, "getBestStreamUrl failed for saavnSongId=$saavnSongId", result.exceptionOrNull())
            return null
        }
    }

    /**
     * Retrieve the byte size of a JioSaavn stream URL by issuing a lightweight HTTP GET request
     * with a Range: bytes=0-0 header and reading the Content-Range response header.
     */
    suspend fun getContentLength(url: String): Long? = runCatching {
        Log.d(TAG, "getContentLength: url=$url")
        val getClient = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 4_000
                connectTimeoutMillis = 2_000
                socketTimeoutMillis  = 4_000
            }
            expectSuccess = false
            followRedirects = true
        }
        getClient.use { c ->
            val response = c.get(url) {
                headers.append(HttpHeaders.Range, "bytes=0-0")
                headers.append(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
            }
            Log.d(TAG, "getContentLength: HTTP status=${response.status}")
            if (response.status == HttpStatusCode.PartialContent || response.status == HttpStatusCode.OK) {
                val contentRange = response.headers[HttpHeaders.ContentRange]
                val len = contentRange?.substringAfter('/')?.toLongOrNull()
                    ?: response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                Log.d(TAG, "getContentLength: resolved length=$len from contentRange=$contentRange")
                len
            } else {
                null
            }
        }
    }.onFailure {
        Log.e(TAG, "getContentLength failed for url=$url", it)
    }.getOrNull()
}
