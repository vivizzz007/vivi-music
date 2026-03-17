package com.music.vivi.canvas

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Fetches album canvas artwork from artwork.boidu.dev (used by Monochrome).
 */
object MonochromeAlbumCanvas {
    private const val BASE_URL = "https://artwork.boidu.dev/"

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
            defaultRequest { url(BASE_URL) }
            expectSuccess = false
        }
    }

    private data class CacheEntry(
        val value: CanvasArtwork?,
        val expiresAtMs: Long
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 hours

    suspend fun getByAlbumArtist(
        album: String,
        artist: String
    ): CanvasArtwork? {
        val key = cacheKey(album, artist)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        val result = fetchFromBoidu(album, artist)
        cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    private suspend fun fetchFromBoidu(
        album: String,
        artist: String
    ): CanvasArtwork? {
        return runCatching {
            val response = client.get("") {
                parameter("s", album)
                parameter("a", artist)
            }
            
            if (response.status == HttpStatusCode.OK) {
                val body = response.body<kotlinx.serialization.json.JsonObject>()
                val videoUrl = body["videoUrl"]?.jsonPrimitive?.contentOrNull
                val animated = body["animated"]?.jsonPrimitive?.contentOrNull
                
                if (!videoUrl.isNullOrBlank() || !animated.isNullOrBlank()) {
                    CanvasArtwork(
                        name = album,
                        artist = artist,
                        videoUrl = videoUrl,
                        animated = animated
                    )
                } else null
            } else null
        }.getOrNull()
    }

    private fun cacheKey(vararg parts: String): String {
        return parts.joinToString("|") { it.trim().lowercase(Locale.ROOT) }
    }
}
