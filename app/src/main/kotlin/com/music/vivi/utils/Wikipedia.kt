package com.music.vivi.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Utility for fetching summaries from Wikipedia via the REST API.
 * Defines a custom Ktor client with a specific User-Agent/Content-Negotiation configuration.
 */
object Wikipedia {
    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }
            defaultRequest { url("https://en.wikipedia.org/api/rest_v1/") }
            expectSuccess = true
        }
    }

    @Serializable
    private data class WikiSummary(val extract: String? = null, val type: String? = null)

    private suspend fun fetchPageSummary(title: String): String? = runCatching {
        client.get("page/summary/${title.replace(" ", "_").encodeURLParameter()}")
            .body<WikiSummary>()
            .extract
    }.onFailure {
        Timber.e(it, "Failed to fetch Wikipedia summary for: $title")
    }.getOrNull()

    /**
     * Attempts to find the Wikipedia summary for a specific album.
     *
     * It uses a heuristic approach to find the correct page:
     * 1.  **Precise Query**: Tries "Album (Artist album)" or "Album (Artist)".
     * 2.  **Generic Query**: Tries "Album (album)" or just "Album".
     *
     * To avoid incorrect matches (e.g., getting a "Greatest Hits" page for a different artist),
     * generic results are validated to ensure they mention the artist's name.
     *
     * @param albumTitle The title of the album.
     * @param artistName The name of the artist (optional but recommended for accuracy).
     * @return The extract/summary text if found, or null.
     */
    suspend fun fetchAlbumInfo(albumTitle: String, artistName: String?): String? {
        // Precise queries: explicitly include artist name in the search term
        if (artistName != null) {
            val preciseQueries = listOf(
                "$albumTitle ($artistName album)",
                "$albumTitle ($artistName)"
            )
            for (query in preciseQueries) {
                val summary = fetchPageSummary(query)
                if (summary != null && !summary.contains("may refer to", ignoreCase = true)) {
                    return summary
                }
            }
        }

        // Generic queries: rely on validation of the returned content
        val genericQueries = listOf(
            "$albumTitle (album)",
            albumTitle
        )

        for (query in genericQueries) {
            val summary = fetchPageSummary(query)
            if (summary != null && !summary.contains("may refer to", ignoreCase = true)) {
                // If we know the artist, ensure the summary actually mentions them.
                // This prevents "Greatest Hits" returning the wrong album.
                if (artistName != null) {
                    if (summary.contains(artistName, ignoreCase = true)) {
                        return summary
                    }
                } else {
                    return summary
                }
            }
        }

        return null
    }

    suspend fun fetchPlaylistInfo(playlistTitle: String): String? = fetchPageSummary(playlistTitle)
}
