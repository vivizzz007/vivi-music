/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * Fetches public Spotify playlist metadata and the track list exposed in Spotify’s embed page
 * (same source BloomeeTunes-style importers use via their content-importer pipeline).
 *
 * Spotify only embeds a limited window of tracks in this HTML; longer playlists are truncated
 * on their side, not by this parser.
 */
object SpotifyPlaylistFetcher {

    data class Track(val title: String, val artistLine: String)

    data class Playlist(
        val title: String,
        val owner: String?,
        val thumbnailUrl: String?,
        val tracks: List<Track>,
    )

    private val client by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 45_000
                connectTimeoutMillis = 20_000
                socketTimeoutMillis = 30_000
            }
        }
    }

    private val playlistIdRegex = listOf(
        Regex("""spotify\.com/(?:intl-[a-z]{2}/)?playlist/([^?/\s]+)""", RegexOption.IGNORE_CASE),
        Regex("""spotify:playlist:([^?\s]+)""", RegexOption.IGNORE_CASE),
    )

    fun extractPlaylistId(urlOrUri: String): String? {
        val trimmed = urlOrUri.trim()
        for (pattern in playlistIdRegex) {
            pattern.find(trimmed)?.groupValues?.getOrNull(1)?.let { return it }
        }
        return null
    }

    suspend fun fetch(playlistId: String): Result<Playlist> = withContext(Dispatchers.IO) {
        runCatching {
            val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
            val html = client.get(embedUrl) {
                header(HttpHeaders.UserAgent, SPOTIFY_EMBED_USER_AGENT)
            }.body<String>()
            parseNextData(html)
        }.onFailure { Timber.tag("SpotifyImport").w(it, "fetch failed") }
    }

    private fun parseNextData(html: String): Playlist {
        val marker = """<script id="__NEXT_DATA__" type="application/json">"""
        val start = html.indexOf(marker)
        if (start < 0) error("Could not read playlist data from Spotify (missing embed payload).")
        val jsonStart = start + marker.length
        val end = html.indexOf("</script>", jsonStart)
        if (end < 0) error("Malformed embed page from Spotify.")
        val root = JSONObject(html.substring(jsonStart, end))
        val entity = root
            .getJSONObject("props")
            .getJSONObject("pageProps")
            .getJSONObject("state")
            .getJSONObject("data")
            .getJSONObject("entity")

        val title = entity.optString("title").ifBlank { entity.optString("name") }
            .ifBlank { error("Playlist has no title.") }

        val owner = entity.optString("subtitle").takeIf { it.isNotBlank() }

        val thumbnailUrl = entity.optJSONObject("coverArt")
            ?.optJSONArray("sources")
            ?.optJSONObject(0)
            ?.optString("url")
            ?.takeIf { it.isNotBlank() }

        val trackList: JSONArray = entity.optJSONArray("trackList")
            ?: error("This playlist has no embedded tracks (it may be private or unavailable).")

        if (trackList.length() == 0) {
            error("This playlist has no embedded tracks (it may be private or unavailable).")
        }

        val tracks = buildList {
            for (i in 0 until trackList.length()) {
                val row = trackList.optJSONObject(i) ?: continue
                val trackTitle = row.optString("title").trim()
                if (trackTitle.isEmpty()) continue
                val subtitle = row.optString("subtitle")
                    .replace('\u00a0', ' ')
                    .trim()
                add(Track(title = trackTitle, artistLine = subtitle))
            }
        }

        if (tracks.isEmpty()) {
            error("Could not parse any tracks from the playlist embed.")
        }

        return Playlist(
            title = title,
            owner = owner,
            thumbnailUrl = thumbnailUrl,
            tracks = tracks,
        )
    }

    private const val SPOTIFY_EMBED_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
}
