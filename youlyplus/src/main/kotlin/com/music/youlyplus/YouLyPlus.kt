package com.music.youlyplus

import com.music.youlyplus.models.LyricsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.selects.select
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicReference

/**
 * YouLyPlus / LyricsPlus KPoe API client.
 *
 * This replicates the multi-server fetch strategy from the YouLyPlus browser
 * extension (ibratabian17/YouLyPlus), querying community-hosted instances of
 * the open-source LyricsPlus backend (ibratabian17/lyricsplus).
 *
 * API endpoint: GET {server}/v2/lyrics/get?title=...&artist=...&duration=...
 */
object YouLyPlus {

    /** Mirror of YouLyPlus extension's KPOE_SERVERS constant. */
    private val BASE_SERVERS = listOf(
        "https://lyricsplus.prjktla.my.id",       // youly's server
        "https://lyricsplus.atomix.one",          // meow's mirror
        "https://lyricsplus.binimum.org",         // binimum's server
        "https://lyricsplus.prjktla.workers.dev", // ibra's cf worker
        "https://lyricsplus-seven.vercel.app",    // jigen's mirror
        "https://lyrics-plus-backend.vercel.app", // ibra's vercel
    )

    /**
     * Remembers the last server that returned a valid result so it is tried
     * first on the next call, giving a fast path on repeated fetches.
     */
    private val lastWorkingServer = AtomicReference<String?>(null)

    /** Returns the server list with the last-working server promoted to front. */
    private val servers: List<String>
        get() {
            val lws = lastWorkingServer.get() ?: return BASE_SERVERS
            return listOf(lws) + BASE_SERVERS.filter { it != lws }
        }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = 3_000
                requestTimeoutMillis = 8_000
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }
            expectSuccess = true
        }
    }

    /**
     * Fetch lyrics by racing all servers in parallel.
     * Returns the first non-blank result; records the winning server so future
     * calls skip the slow ones.
     */
    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        id: String? = null,
        isrc: String? = null,
    ): Result<String> = runCatching {
        val scope = CoroutineScope(Dispatchers.IO)
        val jobs = servers.map { server ->
            server to scope.async {
                fetchFromServer(server, title, artist, duration, album, id, isrc)
            }
        }

        try {
            // Poll until one server returns a usable result
            val remaining = jobs.toMutableList()
            while (remaining.isNotEmpty()) {
                val (winServer, winLyrics) = select {
                    remaining.forEach { (srv, deferred) ->
                        deferred.onAwait { response -> srv to response }
                    }
                }
                remaining.removeAll { it.first == winServer }

                val lrc = winLyrics?.let { resp ->
                    resp.syncedLyrics?.takeIf { it.isNotBlank() }
                        ?: resp.lyrics?.convertToLrc()?.takeIf { it.isNotBlank() }
                        ?: resp.plainLyrics?.takeIf { it.isNotBlank() }
                }
                if (!lrc.isNullOrBlank()) {
                    lastWorkingServer.set(winServer)
                    return@runCatching lrc
                }
            }
            throw IllegalStateException("No lyrics found from any YouLyPlus server")
        } finally {
            scope.coroutineContext.cancelChildren()
        }
    }

    /**
     * Collect all lyrics options across servers; invokes [callback] for each
     * distinct non-blank result. Each server is queried in parallel; callbacks
     * are delivered as results arrive.
     */
    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        id: String? = null,
        isrc: String? = null,
        callback: (String) -> Unit,
    ) {
        val scope = CoroutineScope(Dispatchers.IO)
        val jobs = servers.map { server ->
            scope.async {
                runCatching {
                    val response = fetchFromServer(server, title, artist, duration, album, id, isrc)
                    if (response != null) {
                        response.syncedLyrics?.takeIf { it.isNotBlank() }
                            ?: response.lyrics?.convertToLrc()?.takeIf { it.isNotBlank() }
                            ?: response.plainLyrics?.takeIf { it.isNotBlank() }
                    } else null
                }.getOrNull()
            }
        }
        try {
            val remaining = jobs.toMutableList()
            while (remaining.isNotEmpty()) {
                val lrc = select {
                    remaining.forEach { deferred -> deferred.onAwait { it } }
                }
                remaining.removeAll { it.isCompleted }
                if (!lrc.isNullOrBlank()) callback(lrc)
            }
        } finally {
            scope.coroutineContext.cancelChildren()
        }
    }

    /**
     * Converts a list of LyricsItem (with millisecond 'time') to a standard 
     * [mm:ss.xxx]LRC string. Supports word-by-word rich sync if syllables are present.
     */
    private fun List<com.music.youlyplus.models.LyricsItem>.convertToLrc(): String? {
        if (isEmpty()) return null
        return joinToString("\n") { item ->
            val lineTime = item.time ?: 0L
            
            // Check if any syllable or the item itself is marked as background
            val isBg = item.syllabus?.any { it.isBackground == true } == true
            val lineTimestamp = formatTime(lineTime)
            val bgMarker = if (isBg) "{bg}" else ""
            
            val syllabus = item.syllabus
            if (!syllabus.isNullOrEmpty()) {
                val sb = StringBuilder(lineTimestamp)
                sb.append(bgMarker)
                syllabus.forEach { syl ->
                    val sylTime = syl.time ?: 0L
                    sb.append(formatTime(sylTime, isSyllable = true))
                    sb.append(syl.text ?: "")
                    // Add space after word if it's missing and not the last word
                    if (syl.text?.endsWith(" ") == false) {
                        sb.append(" ")
                    }
                }
                sb.toString().trim()
            } else {
                lineTimestamp + bgMarker + (item.text ?: "")
            }
        }
    }


    private fun formatTime(timeMs: Long, isSyllable: Boolean = false): String {
        val minutes = (timeMs / 1000) / 60
        val seconds = (timeMs / 1000) % 60
        val millis = timeMs % 1000
        val prefix = if (isSyllable) "<" else "["
        val suffix = if (isSyllable) ">" else "]"
        return "%s%02d:%02d.%03d%s".format(prefix, minutes, seconds, millis, suffix)
    }


    private suspend fun fetchFromServer(
        baseUrl: String,
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        id: String? = null,
        isrc: String? = null,
    ): LyricsResponse? = runCatching {
        client.get(baseUrl.let { if (it.endsWith("/")) it else "$it/" } + "v2/lyrics/get") {
            parameter("title", title)
            parameter("artist", artist)
            parameter("duration", duration)
            if (album != null) parameter("album", album)
            if (id != null) parameter("id", id)
            if (isrc != null) parameter("isrc", isrc)
        }.body<LyricsResponse>()
    }.getOrNull()
}
