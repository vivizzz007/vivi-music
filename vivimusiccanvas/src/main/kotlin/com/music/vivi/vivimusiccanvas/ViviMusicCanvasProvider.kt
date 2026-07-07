package com.music.vivi.vivimusiccanvas

import com.music.vivi.canvas.CanvasArtwork
import com.music.vivi.canvas.normalizeForComparison
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ViviMusicCanvasManifest(
    val items: List<ViviMusicCanvasItem> = emptyList()
)

@Serializable
data class ViviMusicCanvasItem(
    val song: String,
    val artist: String,
    val url: String,
    val album: String = "",
)

object ViviMusicCanvasProvider {
    private const val BASE_URL = "https://vivimusicanvas.mkmdevilmi.workers.dev/canvas.json" //new link

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 12_000
                requestTimeoutMillis = 18_000
                socketTimeoutMillis = 18_000
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
        val value: ViviMusicCanvasManifest?,
        val expiresAtMs: Long,
    )

    private var manifestCache: CacheEntry? = null
    // Cache TTL 1 minute (re-fetches json index every minute max for instant updates)
    private val ttlMs = 60_000L

    private suspend fun fetchManifest(): ViviMusicCanvasManifest? {
        val currentCache = manifestCache
        if (currentCache != null && currentCache.expiresAtMs > System.currentTimeMillis()) {
            return currentCache.value
        }

        return try {
            val manifest: ViviMusicCanvasManifest = client.get(BASE_URL).body()
            
            manifestCache = CacheEntry(
                value = manifest,
                expiresAtMs = System.currentTimeMillis() + ttlMs
            )
            manifest
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String,
    ): CanvasArtwork? {
        if (song.isBlank() || artist.isBlank()) return null
        
        val manifest = fetchManifest() ?: return null

        // Strict 3-way match: song + artist + album — all three must match
        val target = manifest.items.firstOrNull { item ->
            val matchSong = song.normalizeForComparison().contains(item.song.normalizeForComparison()) ||
                    item.song.normalizeForComparison().contains(song.normalizeForComparison())
            val matchArtist = artist.normalizeForComparison().contains(item.artist.normalizeForComparison()) ||
                    item.artist.normalizeForComparison().contains(artist.normalizeForComparison())
            val matchAlbum = album.normalizeForComparison() == item.album.normalizeForComparison()
            matchSong && matchArtist && matchAlbum
        }

        if (target != null) {
            return CanvasArtwork(
                name = target.song,
                artist = target.artist,
                albumName = target.album.takeIf { it.isNotBlank() },
                videoUrl = target.url,
                animated = target.url
            )
        } else {
            return null
        }
    }
}
