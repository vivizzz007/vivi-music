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

object Wikipedia {
    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { 
                    isLenient = true
                    ignoreUnknownKeys = true 
                })
            }
            defaultRequest { url("https://en.wikipedia.org/api/rest_v1/") }
            expectSuccess = true
        }
    }

    @Serializable
    private data class WikiSummary(
        val extract: String? = null,
        val type: String? = null
    )

    private suspend fun fetchPageSummary(title: String): String? = runCatching {
        client.get("page/summary/${title.replace(" ", "_").encodeURLParameter()}")
            .body<WikiSummary>()
            .extract
    }.onFailure { 
        Timber.e(it, "Failed to fetch Wikipedia summary for: $title")
    }.getOrNull()

    suspend fun fetchAlbumInfo(albumTitle: String, artistName: String?): String? {
        // Try precise matches first
        val queries = mutableListOf<String>()
        if (artistName != null) {
            queries.add("$albumTitle ($artistName album)")
            queries.add("$albumTitle ($artistName)")
        }
        queries.add("$albumTitle (album)")
        queries.add(albumTitle)

        for (query in queries) {
            val summary = fetchPageSummary(query)
            if (summary != null && !summary.contains("may refer to", ignoreCase = true)) {
                return summary
            }
        }

        return null
    }

    suspend fun fetchPlaylistInfo(playlistTitle: String): String? {
        return fetchPageSummary(playlistTitle)
    }
}
