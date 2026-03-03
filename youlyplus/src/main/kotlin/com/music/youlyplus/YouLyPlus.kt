package com.music.youlyplus

import com.music.youlyplus.models.LyricsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

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
    private val SERVERS = listOf(
        "https://lyricsplus.atomix.one",          // meow's mirror
        "https://lyricsplus-seven.vercel.app",    // jigen's mirror
        "https://lyricsplus.prjktla.workers.dev", // ibra's cf worker
        "http://ly.mxtiy.xyz",                    // painfueg0's server (http)
        "https://lyrics-plus-backend.vercel.app", // ibra's vercel
        "https://youlyplus.binimum.org",          // binimum's server
    )

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
            expectSuccess = true
        }
    }

    /**
     * Fetch lyrics from the first responding server.
     * Prefers synced lyrics; falls back to plain text if available.
     * Converts structured KPoe line-arrays to LRC string format.
     */
    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        id: String? = null,
        isrc: String? = null,
    ): Result<String> = runCatching {
        for (server in SERVERS) {
            val response = fetchFromServer(server, title, artist, duration, album, id, isrc)
            if (response != null) {
                // 1. Check for pre-formatted synced lyrics (LRC)
                if (!response.syncedLyrics.isNullOrBlank()) return@runCatching response.syncedLyrics
                
                // 2. Check for structured lyrics array and convert to LRC
                val converted = response.lyrics?.convertToLrc()
                if (!converted.isNullOrBlank()) return@runCatching converted
                
                // 3. Fallback to plain lyrics
                if (!response.plainLyrics.isNullOrBlank()) return@runCatching response.plainLyrics
            }
        }
        throw IllegalStateException("No lyrics found from any YouLyPlus server")
    }

    /**
     * Collect all lyrics options across servers; invokes [callback] for each
     * distinct non-blank result.
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
        for (server in SERVERS) {
            runCatching {
                val response = fetchFromServer(server, title, artist, duration, album, id, isrc)
                if (response != null) {
                    val lrc = response.syncedLyrics?.takeIf { it.isNotBlank() }
                        ?: response.lyrics?.convertToLrc()?.takeIf { it.isNotBlank() }
                        ?: response.plainLyrics?.takeIf { it.isNotBlank() }
                    
                    lrc?.let(callback)
                }
            }
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
