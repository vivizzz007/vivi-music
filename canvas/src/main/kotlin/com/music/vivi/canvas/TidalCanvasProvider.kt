package com.music.vivi.canvas

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * A canvas provider that fetches Tidal video covers directly from api.tidal.com
 * using the public embed player token.
 */
object TidalCanvasProvider {
    private const val BASE_URL = "https://api.tidal.com/v1/"
    private const val TIDAL_TOKEN = "vNVdglQOjFJJGG2U"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
            install(HttpCache)
            expectSuccess = false
        }
    }

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    private data class CacheEntry(
        val value: CanvasArtwork?,
        val expiresAtMs: Long
    )

    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 hours

    private val countryCode by lazy {
        val country = Locale.getDefault().country
        if (country.length == 2) country.uppercase(Locale.ROOT) else "US"
    }

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null
    ): CanvasArtwork? {
        val key = cacheKey("search_song", song, artist, album ?: "")
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        val query = if (!album.isNullOrBlank()) "$album $artist $song" else "$artist $song"

        val result = searchOnTidal(
            query = query,
            types = "TRACKS",
            songValidation = song,
            artistValidation = artist
        )
        if (result != null) {
            cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        return result
    }

    suspend fun getByAlbumArtist(
        album: String,
        artist: String
    ): CanvasArtwork? {
        val key = cacheKey("search_album", album, artist)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        val result = searchOnTidal(
            query = "$album $artist",
            types = "ALBUMS",
            songValidation = null,
            artistValidation = artist,
            albumValidation = album
        )
        if (result != null) {
            cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        return result
    }

    private suspend fun searchOnTidal(
        query: String,
        types: String,
        songValidation: String? = null,
        artistValidation: String? = null,
        albumValidation: String? = null
    ): CanvasArtwork? {
        try {
            val response = client.get("${BASE_URL}search") {
                header("X-Tidal-Token", TIDAL_TOKEN)
                parameter("query", query)
                parameter("limit", "10")
                parameter("types", types)
                parameter("countryCode", countryCode)
            }
            if (response.status != HttpStatusCode.OK) return null

            val root = response.body<JsonObject>()
            val key = types.lowercase(Locale.ROOT) // "tracks" or "albums"
            val section = findSearchSection(root, key) ?: return null
            val items = section.jsonObject["items"]?.jsonArray ?: return null

            for (item in items) {
                val obj = item.jsonObject

                // Validate track title if searching tracks
                val resultTitle = obj["title"]?.jsonPrimitive?.contentOrNull
                if (songValidation != null && resultTitle != null && !resultTitle.contains(songValidation, ignoreCase = true)) {
                    continue
                }

                // Validate album title if searching albums
                if (albumValidation != null && resultTitle != null && !resultTitle.contains(albumValidation, ignoreCase = true) && !albumValidation.contains(resultTitle, ignoreCase = true)) {
                    continue
                }

                // Validate artist (either in array of artists or artist object)
                val artists = obj["artists"]?.jsonArray
                val primaryArtist = obj["artist"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
                    ?: artists?.firstOrNull()?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
                
                if (artistValidation != null && primaryArtist != null) {
                    val splitDelimiters = Regex("(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)", RegexOption.IGNORE_CASE)
                    val requestedList = artistValidation.split(splitDelimiters).map { it.replace(Regex("\\s+"), " ").trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }
                    val returnedList = primaryArtist.split(splitDelimiters).map { it.replace(Regex("\\s+"), " ").trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }
                    val artistMatches = requestedList.any { req -> returnedList.any { res -> res.contains(req) || req.contains(res) } }
                    if (!artistMatches) continue
                }

                // Retrieve videoCover
                val albumObj = if (types == "TRACKS") obj["album"]?.jsonObject else obj
                val videoCover = albumObj?.get("videoCover")?.jsonPrimitive?.contentOrNull

                if (!videoCover.isNullOrBlank()) {
                    val videoUrl = formatVideoUrl(videoCover)
                    if (videoUrl != null) {
                        return CanvasArtwork(
                            name = resultTitle ?: songValidation ?: albumValidation ?: "",
                            artist = primaryArtist ?: artistValidation ?: "",
                            videoUrl = videoUrl,
                            albumName = if (types == "TRACKS") albumObj?.get("title")?.jsonPrimitive?.contentOrNull else resultTitle
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore & return null
        }
        return null
    }

    private fun findSearchSection(source: JsonElement, key: String): JsonElement? {
        if (source is JsonObject) {
            if (source.containsKey("items") && source["items"] is JsonArray) return source
            if (source.containsKey(key)) {
                val found = findSearchSection(source[key]!!, key)
                if (found != null) return found
            }
            for (value in source.values) {
                val found = findSearchSection(value, key)
                if (found != null) return found
            }
        } else if (source is JsonArray) {
            for (element in source) {
                val found = findSearchSection(element, key)
                if (found != null) return found
            }
        }
        return null
    }

    internal fun formatVideoUrl(id: String): String? {
        val parts = id.split("-")
        if (parts.size != 5) return null
        return "https://resources.tidal.com/videos/${parts[0]}/${parts[1]}/${parts[2]}/${parts[3]}/${parts[4]}/1280x1280.mp4"
    }

    private fun cacheKey(prefix: String, vararg parts: String): String {
        return "$prefix|" + parts.joinToString("|") { it.trim().lowercase(Locale.ROOT) }
    }
}
