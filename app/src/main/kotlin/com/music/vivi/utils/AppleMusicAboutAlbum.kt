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
                header("Authorization", "Bearer $APPLE_MUSIC_TOKEN")
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
