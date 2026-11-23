/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
/**
 * VIVI-SAAVN (C) 2026
 *
 * PROPRIETARY LICENSE:
 * This file is source-available for viewing. Copying, modification,
 * redistribution, or reuse in other applications is strictly prohibited.
 * Licensed exclusively for use in the official vivimusic application.
 *
 * JioSaavn audio streaming service.
 * Coordinates song search, metadata extraction, and stream URL resolution.
 */

package com.music.jiosaavn

import android.util.Log
import com.music.jiosaavn.cache.SaavnCache
import com.music.jiosaavn.crypto.SaavnCrypto
import com.music.jiosaavn.models.RawSearchResponse
import com.music.jiosaavn.models.RawSongItem
import com.music.jiosaavn.models.SaavnAlbum
import com.music.jiosaavn.models.SaavnArtistItem
import com.music.jiosaavn.models.SaavnArtists
import com.music.jiosaavn.models.SaavnDownloadUrl
import com.music.jiosaavn.models.SaavnImage
import com.music.jiosaavn.models.SaavnSong
import com.music.jiosaavn.network.SaavnClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

// Backward-compatibility typealiases so existing consumers can reference these from com.music.jiosaavn
typealias SaavnSong = com.music.jiosaavn.models.SaavnSong
typealias SaavnDownloadUrl = com.music.jiosaavn.models.SaavnDownloadUrl
typealias SaavnImage = com.music.jiosaavn.models.SaavnImage
typealias SaavnArtistItem = com.music.jiosaavn.models.SaavnArtistItem
typealias SaavnArtists = com.music.jiosaavn.models.SaavnArtists
typealias SaavnAlbum = com.music.jiosaavn.models.SaavnAlbum

object SaavnService {

    private const val TAG = "SaavnService"

    /**
     * Map the raw API structure received from JioSaavn into SaavnSong models used in playback.
     */
    fun mapRawToSaavnSong(raw: RawSongItem): SaavnSong {
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
            image = SaavnCrypto.createImageLinks(raw.image),
            downloadUrl = SaavnCrypto.createDownloadLinks(raw.moreInfo.encryptedMediaUrl),
            album = SaavnAlbum(
                id = raw.moreInfo.album_id,
                name = raw.moreInfo.album
            ),
            isProOnly = raw.moreInfo.rights.isProOnly
        )
    }

    /**
     * Search for songs on JioSaavn directly by a free-form query.
     */
    suspend fun searchSongs(query: String): Result<List<SaavnSong>> {
        SaavnCache.getSearchResults(query)?.let { cached ->
            Log.d(TAG, "searchSongs: cache hit for query=\"$query\" (size=${cached.size})")
            return Result.success(cached)
        }

        return runCatching {
            Log.d(TAG, "searchSongs: query=\"$query\"")
            val response = SaavnClient.client.get("") {
                parameter("__call", "search.getResults")
                parameter("_format", "json")
                parameter("_marker", "0")
                parameter("api_version", "4")
                parameter("ctx", "android")  // android ctx: better 320kbps access than wap6dot0
                parameter("q", query)
                parameter("p", "1")
                parameter("n", "10")  // larger pool → better candidate matching
            }

            Log.d(TAG, "searchSongs: HTTP response status: ${response.status}")
            if (response.status != HttpStatusCode.OK) {
                throw IllegalStateException("Saavn search failed: HTTP ${response.status.value}")
            }

            val responseText = response.bodyAsText()
            val body = SaavnClient.json.decodeFromString<RawSearchResponse>(responseText)
            val results = body.results.map { mapRawToSaavnSong(it) }
            Log.d(TAG, "searchSongs: results.size=${results.size}")

            if (results.isEmpty()) {
                throw NoSuchElementException("No songs found on JioSaavn for: \"$query\"")
            }

            // Populate cache
            SaavnCache.putSearchResults(query, results)
            results.forEach { song ->
                song.downloadUrl.forEach { dl ->
                    if (dl.url.isNotBlank()) {
                        SaavnCache.putStreamUrl(song.id, dl.quality, dl.url)
                    }
                }
            }

            results
        }.onFailure {
            Log.e(TAG, "searchSongs: failed for query=\"$query\"", it)
        }
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
        SaavnCache.getStreamUrl(saavnSongId, quality)?.let { cachedUrl ->
            Log.d(TAG, "getBestStreamUrl: cache hit for saavnSongId=$saavnSongId, quality=$quality")
            return cachedUrl
        }

        val result = runCatching {
            Log.d(TAG, "getBestStreamUrl: saavnSongId=$saavnSongId, quality=$quality")
            val response = SaavnClient.client.get("") {
                parameter("__call", "song.getDetails")
                parameter("_format", "json")
                parameter("_marker", "0")
                parameter("api_version", "4")
                parameter("ctx", "android")  // android ctx: better 320kbps access than wap6dot0
                parameter("pids", saavnSongId)
            }

            Log.d(TAG, "getBestStreamUrl: HTTP response status: ${response.status}")
            if (response.status != HttpStatusCode.OK) {
                throw IllegalStateException("Saavn getBestStreamUrl failed: HTTP ${response.status.value}")
            }

            val responseText = response.bodyAsText()
            val decodedMap = SaavnClient.json.decodeFromString<Map<String, RawSongItem>>(responseText)
            Log.d(TAG, "getBestStreamUrl: songs size=${decodedMap.size}")

            val rawSong = decodedMap.values.firstOrNull() ?: throw NoSuchElementException("Song not found")
            val saavnSong = mapRawToSaavnSong(rawSong)
            Log.d(TAG, "getBestStreamUrl: raw song details name=${rawSong.title}, encryptedUrl=${rawSong.moreInfo.encryptedMediaUrl}")

            val resolvedUrl = selectBestUrl(saavnSong.downloadUrl, quality)
            resolvedUrl?.let { SaavnCache.putStreamUrl(saavnSongId, quality, it) }
            resolvedUrl
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
        val getClient = SaavnClient.createRangeClient()
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
