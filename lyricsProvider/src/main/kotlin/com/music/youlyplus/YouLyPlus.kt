/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.youlyplus

import com.music.youlyplus.models.LyricsItem
import com.music.youlyplus.models.LyricsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
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

    /** Mirror of YouLyPlus v5.1.0 extension's KPOE_SERVERS constant. */
    private val BASE_SERVERS = listOf(
        "https://lyricsplus.prjktla.my.id",       // youly's server
        "https://lyricsplus.binimum.org",         // binimum's server
        "https://lyricsplus.prjktla.workers.dev", // ibra's cf worker
    )

    private const val DEFAULT_SOURCE_ORDER = "apple,qq,musixmatch,musixmatch-word"

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
        raceServers(title, artist, duration, album, id, isrc)?.let { return@runCatching it }

        val cleanedTitle = cleanVideoTitle(title)
        if (cleanedTitle != title || duration > 0) {
            raceServers(cleanedTitle, artist, duration = 0, album, id, isrc)?.let { return@runCatching it }
        }

        throw IllegalStateException("No lyrics found from any YouLyPlus server")
    }

    private suspend fun raceServers(
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        id: String?,
        isrc: String?,
    ): String? {
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

                val lrc = winLyrics?.toLrcOrNull()
                if (!lrc.isNullOrBlank()) {
                    lastWorkingServer.set(winServer)
                    return lrc
                }
            }
            return null
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
        val foundAny = collectFromServers(title, artist, duration, album, id, isrc, callback)
        if (!foundAny) {
            val cleanedTitle = cleanVideoTitle(title)
            if (cleanedTitle != title || duration > 0) {
                collectFromServers(cleanedTitle, artist, duration = 0, album, id, isrc, callback)
            }
        }
    }

    private suspend fun collectFromServers(
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        id: String?,
        isrc: String?,
        callback: (String) -> Unit,
    ): Boolean {
        var foundAny = false
        val scope = CoroutineScope(Dispatchers.IO)
        val jobs = servers.map { server ->
            scope.async {
                runCatching {
                    fetchFromServer(server, title, artist, duration, album, id, isrc)?.toLrcOrNull()
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
                if (!lrc.isNullOrBlank()) {
                    foundAny = true
                    callback(lrc)
                }
            }
        } finally {
            scope.coroutineContext.cancelChildren()
        }
        return foundAny
    }

    private fun LyricsResponse.toLrcOrNull(): String? =
        syncedLyrics?.takeIf { it.isNotBlank() }
            ?: lyrics?.convertToLrc()?.takeIf { it.isNotBlank() }
            ?: plainLyrics?.takeIf { it.isNotBlank() }

    private fun cleanVideoTitle(title: String): String = title
        .replace("(Official Video)", "")
        .replace("(Official Music Video)", "")
        .trim()

    /**
     * Converts a list of LyricsItem (with millisecond 'time') to a standard 
     * [mm:ss.xxx]LRC string. Supports word-by-word rich sync if syllables are present.
     */
    internal fun List<LyricsItem>.convertToLrc(): String? {
        if (isEmpty()) return null
        return joinToString("\n") { item ->
            val lineTime = item.time ?: 0L

            val isBg = item.element?.isBackground == true ||
                item.syllabus?.any { it.isBackground == true } == true
            val agent = item.element?.singer?.takeIf { it.isNotBlank() }
            val lineTimestamp = formatTime(lineTime)
            val bgMarker = if (isBg) "{bg}" else ""
            val agentMarker = if (agent != null) "{agent:$agent}" else ""

            val syllabus = item.syllabus
            if (!syllabus.isNullOrEmpty()) {
                val sb = StringBuilder(lineTimestamp)
                sb.append(bgMarker)
                sb.append(agentMarker)
                syllabus.forEach { syl ->
                    val sylTime = syl.time ?: 0L
                    sb.append(formatTime(sylTime, isSyllable = true))
                    sb.append(syl.text ?: "")
                    // Trust the API's own spacing — do not inject extra spaces,
                    // as that would split intra-word syllables (e.g. "vi"+"dh" → "vi dh")
                }
                sb.toString().trimEnd()
            } else {
                lineTimestamp + bgMarker + agentMarker + (item.text ?: "")
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
    ): LyricsResponse? = try {
        client.get(baseUrl.let { if (it.endsWith("/")) it else "$it/" } + "v2/lyrics/get") {
            parameter("title", title)
            parameter("artist", artist)
            if (duration > 0) parameter("duration", duration)
            if (!album.isNullOrBlank()) parameter("album", album)
            if (!id.isNullOrBlank()) parameter("id", id)
            if (!isrc.isNullOrBlank()) parameter("isrc", isrc)
            parameter("source", DEFAULT_SOURCE_ORDER)
        }.body<LyricsResponse>()
    } catch (e: ClientRequestException) {
        if (e.response.status == HttpStatusCode.NotFound && !isrc.isNullOrBlank()) {
            fetchFromServer(baseUrl, title, artist, duration, album, id, isrc = null)
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}
