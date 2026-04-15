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
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Fetches Apple Music artist motion artwork (HLS canvas) for the artist screen.
 *
 * 1. Searches for the artist by name.
 * 2. Fetches the artist profile with `extend=editorialVideo,editorialArtwork`.
 *
 * Results are cached for 24 hours.
 */
object AppleMusicArtistBackgroundProvider {

    // Public read-only JWT used by the Apple Music web player for unauthenticated catalog reads.
    private const val APPLE_MUSIC_TOKEN =
        "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IldlYlBsYXlLaWQifQ" +
        ".eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzc0NDU2MzgyLCJleHAiOjE3ODE3" +
        "MTM5ODIsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ" +
        ".4n8qYF4qa18sL1E0G9A3qX35cD8wQ-IJcS9Bh8ZT8JV_yLBtVq46B-9-2ZS3EvWHuw3yK9BYFYAhAdTaDm38vQ"

    private const val AMP_BASE_URL = "https://amp-api.music.apple.com"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
                register(ContentType.Text.JavaScript, KotlinxSerializationConverter(json))
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 25_000
                socketTimeoutMillis = 25_000
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
            install(HttpCache)
            expectSuccess = false
        }
    }

    private data class CacheEntry(
        val videoUrl: String?,
        val expiresAtMs: Long,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 hours

    suspend fun getByArtistName(
        artistName: String,
        storefront: String = "us",
    ): String? {
        if (artistName.isBlank()) return null
        val key = cacheKey("artist", artistName, storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.videoUrl }

        val result = searchAndFetchArtistMotion(artistName, storefront)
        cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    private suspend fun searchAndFetchArtistMotion(
        artistName: String,
        storefront: String,
    ): String? {
        return runCatching {
            val url = "$AMP_BASE_URL/v1/catalog/$storefront/search"
            val response = client.get(url) {
                header("Authorization", "Bearer $APPLE_MUSIC_TOKEN")
                header("Origin", "https://music.apple.com")
                header("Referer", "https://music.apple.com/")
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                parameter("term", artistName)
                parameter("types", "artists")
                parameter("limit", "3")
            }
            if (response.status != HttpStatusCode.OK) return@runCatching null

            val root = response.body<JsonObject>()
            val results = root["results"]?.jsonObject?.get("artists")?.jsonObject?.get("data")?.jsonArray ?: return@runCatching null
            
            val scoredResults = results.mapNotNull { item ->
                val obj = item.jsonObject
                val attributes = obj["attributes"]?.jsonObject ?: return@mapNotNull null
                val resultName = attributes["name"]?.jsonPrimitive?.contentOrNull ?: ""
                
                if (!resultName.contains(artistName, ignoreCase = true) && 
                    !artistName.contains(resultName, ignoreCase = true)) return@mapNotNull null
                
                var score = 0
                if (resultName.equals(artistName, ignoreCase = true)) score += 10
                else if (resultName.contains(artistName, ignoreCase = true) || artistName.contains(resultName, ignoreCase = true)) score += 5
                
                score to obj
            }.sortedByDescending { it.first }
            
            for ((score, obj) in scoredResults) {
                if (score < 4) continue 
                val artistId = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                val fetched = fetchArtistMotionByAppleId(artistId, storefront)
                if (fetched != null) return@runCatching fetched
            }
            null
        }.getOrNull()
    }

    private suspend fun fetchArtistMotionByAppleId(
        artistId: String,
        storefront: String,
    ): String? {
        return runCatching {
            val url = "$AMP_BASE_URL/v1/catalog/$storefront/artists/$artistId"
            val response = client.get(url) {
                header("Authorization", "Bearer $APPLE_MUSIC_TOKEN")
                header("Origin", "https://music.apple.com")
                header("Referer", "https://music.apple.com/")
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                parameter("extend", "editorialVideo,editorialArtwork")
            }
            if (response.status != HttpStatusCode.OK) return@runCatching null

            val root = response.body<JsonObject>()
            val data = root["data"]?.jsonArray
            if (data.isNullOrEmpty()) return@runCatching null
            
            val artistObj = data.firstOrNull()?.jsonObject ?: return@runCatching null
            val attributes = artistObj["attributes"]?.jsonObject
            
            // Look for editorialVideo first
            val ev = attributes?.get("editorialVideo")?.jsonObject
            if (ev != null) {
                val videoUrl = extractEditorialVideoUrl(ev)
                if (!videoUrl.isNullOrBlank()) {
                    return@runCatching videoUrl
                }
            }

            // Fallback to editorialArtwork
            val ea = attributes?.get("editorialArtwork")?.jsonObject
            if (ea != null) {
                val videoUrl = extractEditorialVideoUrl(ea)
                if (!videoUrl.isNullOrBlank()) {
                    return@runCatching videoUrl
                }
            }

            null
        }.getOrNull()
    }

    private fun extractEditorialVideoUrl(editorialData: JsonObject): String? {
        val preferredKeys = listOf("motionDetailRaw", "motionDetailTall", "motionDetailSquare", "motionSquareVideo1x1", "motionTallVideo3x4")
        for (key in preferredKeys) {
            val videoUrl = editorialData[key]?.jsonObject?.get("video")?.jsonPrimitive?.contentOrNull
            if (!videoUrl.isNullOrBlank()) return videoUrl
        }
        for ((_, value) in editorialData) {
            val videoUrl = (value as? JsonObject)?.get("video")?.jsonPrimitive?.contentOrNull
            if (!videoUrl.isNullOrBlank()) return videoUrl
        }
        return null
    }

    private fun cacheKey(prefix: String, vararg parts: String): String {
        return "$prefix|" + parts.joinToString("|") { it.trim().lowercase(Locale.ROOT) }
    }
}
