package com.music.vivi.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.*
import java.util.Locale
import timber.log.Timber

/**
 * Utility for fetching album descriptions from Apple Music via the AMP API.
 */
object AppleMusicAboutAlbum {

    // Public read-only JWT used by the Apple Music web player for unauthenticated catalog reads.
    private const val APPLE_MUSIC_TOKEN =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IldlYlBsYXlLaWQifQ" +
        ".eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzgxMDMyODU1LCJleHAiOjE3ODQw" +
        "NTY4NTUsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ" +
        ".fiMFcJWkfSlxKP9NVA0UW9CbItD1Rge0SISuepz203XcpU762OqdCpU9M-YkmtKkjRmaIWtjsfGgqZPrlMonpA"

    private var cachedToken: String? = null
    private var tokenExpiryMs: Long = 0L

    private suspend fun getOrFetchToken(): String {
        val now = System.currentTimeMillis()
        if (cachedToken != null && now < tokenExpiryMs - 60_000) {
            return cachedToken!!
        }

        return try {
            val html = client.get("https://music.apple.com/us/browse") {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }.body<String>()

            val scriptRegex = Regex("""/assets/index(?:-legacy)?[~-][a-zA-Z0-9_-]+\.js""")
            val scripts = scriptRegex.findAll(html).map { it.value }.distinct().toList()

            var fetchedToken: String? = null
            for (scriptPath in scripts) {
                val scriptUrl = "https://music.apple.com$scriptPath"
                val scriptText = client.get(scriptUrl) {
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                }.body<String>()

                val tokenRegex = Regex("""ey[a-zA-Z0-9_-]+\.ey[a-zA-Z0-9_-]+\.[a-zA-Z0-9_-]+""")
                val tokens = tokenRegex.findAll(scriptText).map { it.value }
                for (token in tokens) {
                    try {
                        val body = token.split(".")[1]
                        val decodedBytes = java.util.Base64.getUrlDecoder().decode(body)
                        val decoded = String(decodedBytes, Charsets.UTF_8)
                        if (decoded.contains("iss") && decoded.contains("exp")) {
                            val expIndex = decoded.indexOf("\"exp\":")
                            if (expIndex != -1) {
                                val expValStr = decoded.substring(expIndex + 6).takeWhile { it.isDigit() }
                                val expSeconds = expValStr.toLongOrNull() ?: 0L
                                if (expSeconds * 1000 > now) {
                                    fetchedToken = token
                                    tokenExpiryMs = expSeconds * 1000
                                    break
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore decoding issues
                    }
                }
                if (fetchedToken != null) break
            }

            if (fetchedToken != null) {
                cachedToken = fetchedToken
                fetchedToken
            } else {
                APPLE_MUSIC_TOKEN
            }
        } catch (e: Exception) {
            APPLE_MUSIC_TOKEN
        }
    }

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
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 25_000
                socketTimeoutMillis = 25_000
            }
            expectSuccess = false
        }
    }

    /**
     * Attempts to find the Apple Music description for a specific album.
     *
     * @param albumTitle The title of the album.
     * @param artistName The name of the artist.
     * @param storefront The Apple Music storefront (default "us").
     * @return The editorial note/summary text if found, or null.
     */
    suspend fun fetchAlbumDescription(
        albumTitle: String,
        artistName: String?,
        storefront: String = "us"
    ): String? {
        return runCatching {
            // 1. Search for the album to get the ID
            val query = if (artistName != null && !albumTitle.contains(artistName, ignoreCase = true)) {
                "$artistName $albumTitle"
            } else {
                albumTitle
            }

            val searchUrl = "$AMP_BASE_URL/v1/catalog/$storefront/search"
            val searchResponse = client.get(searchUrl) {
                header("Authorization", "Bearer ${getOrFetchToken()}")
                header("Origin", "https://music.apple.com")
                header("Referer", "https://music.apple.com/")
                parameter("term", query)
                parameter("types", "albums")
                parameter("limit", "5")
                parameter("extend", "editorialNotes")
            }

            if (searchResponse.status != HttpStatusCode.OK) return@runCatching null

            val searchRoot = searchResponse.body<JsonObject>()
            val albumsData = searchRoot["results"]?.jsonObject?.get("albums")?.jsonObject?.get("data")?.jsonArray 
                ?: return@runCatching null

            // Score and find best match with editorial notes
            val bestMatch = albumsData.mapNotNull { item ->
                val obj = item.jsonObject
                val attributes = obj["attributes"]?.jsonObject ?: return@mapNotNull null
                val resultArtistName = attributes["artistName"]?.jsonPrimitive?.contentOrNull ?: ""
                val resultName = attributes["name"]?.jsonPrimitive?.contentOrNull ?: ""
                
                var score = 0
                if (artistName != null) {
                    if (resultArtistName.equals(artistName, ignoreCase = true)) score += 10
                    else if (resultArtistName.contains(artistName, ignoreCase = true) || artistName.contains(resultArtistName, ignoreCase = true)) score += 5
                }
                
                if (resultName.equals(albumTitle, ignoreCase = true)) score += 10
                else if (resultName.contains(albumTitle, ignoreCase = true) || albumTitle.contains(resultName, ignoreCase = true)) score += 5
                
                score to attributes
            }.sortedByDescending { it.first }
                .firstOrNull { it.first >= 10 }?.second ?: return@runCatching null

            val editorialNotes = bestMatch["editorialNotes"]?.jsonObject
            val description = editorialNotes?.get("standard")?.jsonPrimitive?.contentOrNull
                ?: editorialNotes?.get("short")?.jsonPrimitive?.contentOrNull

            // Remove HTML tags if present
            description?.replace(Regex("<[^>]*>"), "")?.trim()
        }.onFailure {
            Timber.w("Failed to fetch Apple Music description for $albumTitle: ${it.message}")
        }.getOrNull()
    }
}
