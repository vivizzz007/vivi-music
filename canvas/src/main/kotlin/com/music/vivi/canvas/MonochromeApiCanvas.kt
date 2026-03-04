package com.music.vivi.canvas

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
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
 * A canvas provider that fetches Tidal video covers via Monochrome API instances.
 * This serves as a fallback when ArchiveTuneCanvas fails to find an artwork.
 */
object MonochromeApiCanvas {
    private val INSTANCES = listOf(
        "https://eu-central.monochrome.tf/",
        "https://us-west.monochrome.tf/",
        "https://arran.monochrome.tf/",
        "https://api.monochrome.tf/"
    )

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

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null
    ): CanvasArtwork? {
        val key = cacheKey("search", song, artist, album ?: "")
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        val result = searchForVideoCover(song, artist, album)
        cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    private suspend fun searchForVideoCover(
        song: String,
        artist: String,
        album: String?
    ): CanvasArtwork? {
        // Try to be specific to avoid false positives
        val query = if (!album.isNullOrBlank()) "$artist - $song - $album" else "$artist - $song"
        
        for (baseUrl in INSTANCES) {
            try {
                val response = client.get("${baseUrl}search/") {
                    parameter("s", query)
                }
                if (response.status != HttpStatusCode.OK) continue

                val root = response.body<JsonObject>()
                val tracksSection = findSearchSection(root, "tracks") ?: continue
                val items = tracksSection.jsonObject["items"]?.jsonArray ?: continue

                for (item in items) {
                    val track = item.jsonObject
                    
                    // Basic validation to ensure we found the right track
                    val trackTitle = track["title"]?.jsonPrimitive?.contentOrNull
                    if (trackTitle != null && !trackTitle.contains(song, ignoreCase = true)) continue

                    val albumObj = track["album"]?.jsonObject ?: continue
                    val videoCover = albumObj["videoCover"]?.jsonPrimitive?.contentOrNull
                    
                    if (!videoCover.isNullOrBlank()) {
                        val videoUrl = formatVideoUrl(videoCover)
                        if (videoUrl != null) {
                            return CanvasArtwork(
                                name = trackTitle ?: song,
                                artist = artist,
                                videoUrl = videoUrl
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Try next instance if one fails
                continue
            }
        }
        return null
    }

    /**
     * Recursively find a section in the search response that matches the key and has items.
     */
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
        // 1280x1280 is the standard high-res video cover size used by Tidal
        return "https://resources.tidal.com/videos/${parts[0]}/${parts[1]}/${parts[2]}/${parts[3]}/${parts[4]}/1280x1280.mp4"
    }

    private fun cacheKey(prefix: String, vararg parts: String): String {
        return "$prefix|" + parts.joinToString("|") { it.trim().lowercase(Locale.ROOT) }
    }
}
