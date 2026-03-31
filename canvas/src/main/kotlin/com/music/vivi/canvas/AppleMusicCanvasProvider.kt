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
 * Fetches Apple Music album motion artwork (HLS canvas) for the album screen.
 *
 * Two extraction strategies are tried in order:
 *
 * 1. **editorialVideo** — present on albums that have Apple Motion artwork.
 *    Accessed via `?extend=editorialVideo` on the AMP albums endpoint.
 *
 * 2. **music-video tracks** — some albums embed a full-length music video as a track.
 *    Accessed via `?include=tracks`.
 *
 * Results are cached for 24 hours.
 */
object AppleMusicCanvasProvider {

    // Public read-only JWT used by the Apple Music web player for unauthenticated catalog reads.
    private const val APPLE_MUSIC_TOKEN =
        "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IldlYlBsYXlLaWQifQ" +
        ".eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzc0NDU2MzgyLCJleHAiOjE3ODE3" +
        "MTM5ODIsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ" +
        ".4n8qYF4qa18sL1E0G9A3qX35cD8wQ-IJcS9Bh8ZT8JV_yLBtVq46B-9-2ZS3EvWHuw3yK9BYFYAhAdTaDm38vQ"

    private const val ITUNES_SEARCH_URL = "https://itunes.apple.com/search"
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
                // iTunes returns text/javascript for JSON responses
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
        val value: CanvasArtwork?,
        val expiresAtMs: Long,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 hours

    suspend fun getByAlbumArtist(
        album: String,
        artist: String,
        storefront: String = "us",
    ): CanvasArtwork? {
        val key = cacheKey("sa", album, artist, storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        val result = searchAndFetchMotion(album, artist, storefront, "albums")
        cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        storefront: String = "us",
    ): CanvasArtwork? {
        val key = cacheKey("song", song, artist, storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        // Use searchAndFetchMotion which can handle song searches by resolving to albums
        val result = searchAndFetchMotion(song, artist, storefront, "songs")
        cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    suspend fun getByAlbumId(
        albumId: String,
        storefront: String = "us",
    ): CanvasArtwork? {
        val key = cacheKey("id", albumId, storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        val result = fetchMotionArtwork(albumId, storefront, null)
        cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    /**
     * Searches via AMP API and tries to fetch motion artwork.
     * This is faster than iTunes search + AMP lookup.
     */
    private suspend fun searchAndFetchMotion(
        term: String,
        artist: String,
        storefront: String,
        type: String, // "albums" or "songs"
    ): CanvasArtwork? {
        return runCatching {
            val query = if (term.contains(artist, ignoreCase = true)) term else "$artist $term"
            val url = "$AMP_BASE_URL/v1/catalog/$storefront/search"
            val response = client.get(url) {
                header("Authorization", "Bearer $APPLE_MUSIC_TOKEN")
                header("Origin", "https://music.apple.com")
                header("Referer", "https://music.apple.com/")
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                parameter("term", query)
                parameter("types", type)
                parameter("limit", "5")
                parameter("extend", "editorialVideo")
            }
            if (response.status != HttpStatusCode.OK) return@runCatching null

            val root = response.body<JsonObject>()
            val results = root["results"]?.jsonObject?.get(type)?.jsonObject?.get("data")?.jsonArray ?: return@runCatching null
            
            // Score results for quality and edition matching
            val scoredResults = results.mapNotNull { item ->
                val obj = item.jsonObject
                val attributes = obj["attributes"]?.jsonObject ?: return@mapNotNull null
                val resultArtistName = attributes["artistName"]?.jsonPrimitive?.contentOrNull ?: ""
                val resultName = attributes["name"]?.jsonPrimitive?.contentOrNull ?: ""
                
                // Fuzzy artist match to ensure quality
                if (!resultArtistName.contains(artist, ignoreCase = true) && 
                    !artist.contains(resultArtistName, ignoreCase = true)) return@mapNotNull null
                
                var score = 0
                if (resultArtistName.equals(artist, ignoreCase = true)) score += 5
                
                // Content matching
                if (resultName.equals(term, ignoreCase = true)) score += 10
                else if (resultName.contains(term, ignoreCase = true) || term.contains(resultName, ignoreCase = true)) score += 5

                // Special editions handling (Deluxe, Expanded, etc)
                val editionWords = listOf("deluxe", "expanded", "remastered", "remix", "version", "edition", "bonus")
                for (word in editionWords) {
                    val inTerm = term.contains(word, ignoreCase = true)
                    val inResult = resultName.contains(word, ignoreCase = true)
                    if (inTerm && inResult) score += 3
                    else if (inTerm != inResult) score -= 2 
                }
                
                score to item
            }.sortedByDescending { it.first }
            
            // Try results until we find motion or exhaustion
            for ((score, item) in scoredResults) {
                if (score < 4) continue // Skip poor matches
                val obj = item.jsonObject
                val attributes = obj["attributes"]?.jsonObject ?: continue
                val resultArtistName = attributes["artistName"]?.jsonPrimitive?.contentOrNull ?: ""

                // If it's a song, we need the album ID (collectionId or equivalent)
                val targetAlbumId = if (type == "songs") {
                    // Try to find album relationship or collectionId
                    obj["relationships"]?.jsonObject?.get("albums")?.jsonObject?.get("data")?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                } else {
                    obj["id"]?.jsonPrimitive?.contentOrNull
                } ?: continue

                // Check if motion is already in search attributes (sometimes it's there!)
                val ev = attributes["editorialVideo"]?.jsonObject
                if (ev != null) {
                    val hlsUrl = extractEditorialVideoUrl(ev)
                    if (!hlsUrl.isNullOrBlank()) {
                        val name = attributes["name"]?.jsonPrimitive?.contentOrNull
                        return@runCatching CanvasArtwork(name, resultArtistName, targetAlbumId, animated = hlsUrl)
                    }
                }

                // If not in search, do the full lookup for this ID
                val fetched = fetchMotionArtwork(targetAlbumId, storefront, resultArtistName)
                if (fetched != null) return@runCatching fetched
            }
            null
        }.getOrNull()
    }

    private suspend fun fetchMotionArtwork(
        albumId: String,
        storefront: String,
        fallbackArtist: String?,
    ): CanvasArtwork? {
        return runCatching {
            val url = "$AMP_BASE_URL/v1/catalog/$storefront/albums/$albumId"
            val response = client.get(url) {
                header("Authorization", "Bearer $APPLE_MUSIC_TOKEN")
                header("Origin", "https://music.apple.com")
                header("Referer", "https://music.apple.com/")
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                parameter("extend", "editorialVideo")
                parameter("include", "tracks")
            }
            if (response.status != HttpStatusCode.OK) return@runCatching null

            val root = response.body<JsonObject>()
            val data = root["data"]?.jsonArray
            if (data.isNullOrEmpty()) return@runCatching null
            
            val albumObj = data.firstOrNull()?.jsonObject ?: return@runCatching null
            val attributes = albumObj["attributes"]?.jsonObject
            val albumName = attributes?.get("name")?.jsonPrimitive?.contentOrNull
            val artistName = attributes?.get("artistName")?.jsonPrimitive?.contentOrNull ?: fallbackArtist

            // Strategy 1: editorialVideo
            val ev = attributes?.get("editorialVideo")?.jsonObject
            if (ev != null) {
                val url = extractEditorialVideoUrl(ev)
                if (!url.isNullOrBlank()) {
                    return@runCatching CanvasArtwork(albumName, artistName, albumId, animated = url)
                }
            }

            // Strategy 2: music-video tracks
            val tracks = albumObj["relationships"]?.jsonObject?.get("tracks")?.jsonObject?.get("data")?.jsonArray
            val musicVideoHls = tracks?.firstNotNullOfOrNull { track ->
                val trackObj = track.jsonObject
                if (trackObj["type"]?.jsonPrimitive?.contentOrNull != "music-videos") return@firstNotNullOfOrNull null
                trackObj["attributes"]?.jsonObject?.get("previews")?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("hlsUrl")?.jsonPrimitive?.contentOrNull
                    ?.takeIf { it.isNotBlank() }
            }

            if (!musicVideoHls.isNullOrBlank()) {
                return@runCatching CanvasArtwork(albumName, artistName, albumId, animated = musicVideoHls)
            }

            null
        }.getOrNull()
    }

    private fun extractEditorialVideoUrl(editorialVideo: JsonObject): String? {
        val preferredKeys = listOf("motionDetailSquare", "motionDetailTall", "motionSquareVideo1x1", "motionTallVideo3x4")
        for (key in preferredKeys) {
            val url = editorialVideo[key]?.jsonObject?.get("video")?.jsonPrimitive?.contentOrNull
            if (!url.isNullOrBlank()) return url
        }
        for ((_, value) in editorialVideo) {
            val url = (value as? JsonObject)?.get("video")?.jsonPrimitive?.contentOrNull
            if (!url.isNullOrBlank()) return url
        }
        return null
    }

    private fun cacheKey(prefix: String, vararg parts: String): String {
        return "$prefix|" + parts.joinToString("|") { it.trim().lowercase(Locale.ROOT) }
    }
}
