/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
/**
 * VIVI-LYRICS (C) 2026
 *
 * PROPRIETARY LICENSE:
 * This file is source-available for viewing. Copying, modification,
 * redistribution, or reuse in other applications is strictly prohibited.
 * Licensed exclusively for use in the official vivimusic application.
 */

package com.music.musixmatch

import com.music.musixmatch.models.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

object Musixmatch {
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    private const val BASE_URL = "https://apic.musixmatch.com/ws/1.1/"
    private const val APP_ID = "android-player-v1.0"

    private val secretCache = AtomicReference<String?>(null)
    private val tokenCache = AtomicReference<String?>(null)
    private val processGuid = java.util.UUID.randomUUID().toString()

    private val client by lazy {
        HttpClient(OkHttp) {
            expectSuccess = false
            install(ContentNegotiation) {
                val json = Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    encodeDefaults = true
                }
                json(json)
                json(json, ContentType.Text.Html)
                json(json, ContentType.Text.Plain)
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
        }
    }

    private const val DEFAULT_SECRET = "e9966b7e2aff48cb090d7c1bebd9d7a2"

    private fun getSecret(): String = DEFAULT_SECRET

    private fun sign(url: String, secret: String): String {
        val normalizedUrl = url.replace("%20", "+").replace(" ", "+")
        val formatter = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val dateStr = formatter.format(Date())
        val message = normalizedUrl + dateStr
        
        val keySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(keySpec)
        
        val hmacBytes = mac.doFinal(message.toByteArray(StandardCharsets.UTF_8))
        val base64Sig = Base64.getEncoder().encodeToString(hmacBytes)
        val encodedSig = URLEncoder.encode(base64Sig, StandardCharsets.UTF_8.name())
        
        return "$normalizedUrl&signature=$encodedSig&signature_protocol=sha256"
    }

    private suspend fun getUserToken(secret: String): String {
        val cached = tokenCache.get()
        if (cached != null) return cached

        val tokenUrl = "${BASE_URL}token.get?app_id=$APP_ID&guid=$processGuid&format=json"
        val signedUrl = sign(tokenUrl, secret)
        
        val response = client.get(signedUrl) {
            header("User-Agent", USER_AGENT)
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "en-US,en;q=0.9")
        }.body<TokenResponse>()

        val bodyElement = response.message.body
        val userToken = if (bodyElement is kotlinx.serialization.json.JsonObject) {
            val json = Json { ignoreUnknownKeys = true }
            val bodyObj = json.decodeFromJsonElement<TokenResponseBody>(bodyElement)
            bodyObj.userToken
        } else {
            null
        }

        if (userToken == null) {
            val code = response.message.header.statusCode
            throw IllegalStateException("Failed to retrieve user_token from Musixmatch (status: $code)")
        }

        tokenCache.set(userToken)
        return userToken
    }

    private class TokenExpiredException(message: String) : Exception(message)

    private fun checkStatusCode(statusCode: Int) {
        if (statusCode == 401 || statusCode == 402) {
            tokenCache.set(null)
            secretCache.set(null)
            throw TokenExpiredException("Token expired or unauthorized (status: $statusCode)")
        }
    }

    private suspend fun <T> runWithTokenRetry(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: TokenExpiredException) {
            tokenCache.set(null)
            secretCache.set(null)
            try {
                block()
            } catch (retryException: Exception) {
                tokenCache.set(null)
                secretCache.set(null)
                throw retryException
            }
        } catch (e: Exception) {
            tokenCache.set(null)
            secretCache.set(null)
            throw e
        }
    }

    private val titleCleanupPatterns = listOf(
        Regex("""\s*\(.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\]""", RegexOption.IGNORE_CASE),
        Regex("""\s*【.*?】"""),
        Regex("""\s*\|.*$"""),
        Regex("""\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
    )

    internal fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        for (pattern in titleCleanupPatterns) {
            cleaned = cleaned.replace(pattern, "")
        }
        return cleaned.trim()
    }

    private val artistSeparators = listOf(
        " feat. ", " feat ", " ft. ", " ft ",
        " & ", " / ", " x ", " X ",
        ", ", " with ", " With ",
    )

    internal fun cleanArtist(artist: String): String {
        var cleaned = artist.trim()
        for (separator in artistSeparators) {
            if (cleaned.contains(separator, ignoreCase = true)) {
                cleaned = cleaned.split(separator, ignoreCase = true, limit = 2)[0]
            }
        }
        return cleaned.trim()
    }

    private suspend fun searchTracks(
        trackParam: String?,
        artistParam: String?,
        queryParam: String?,
        token: String,
        secret: String
    ): List<TrackContainer> {
        val params = mutableListOf("app_id=$APP_ID", "format=json", "f_has_lyrics=true", "page_size=10", "usertoken=$token")
        if (!trackParam.isNullOrBlank()) {
            params.add("q_track=${URLEncoder.encode(trackParam, StandardCharsets.UTF_8.name())}")
        }
        if (!artistParam.isNullOrBlank()) {
            params.add("q_artist=${URLEncoder.encode(artistParam, StandardCharsets.UTF_8.name())}")
        }
        if (!queryParam.isNullOrBlank()) {
            params.add("q=${URLEncoder.encode(queryParam, StandardCharsets.UTF_8.name())}")
        }

        val searchUrl = "${BASE_URL}track.search?" + params.joinToString("&")
        val signedSearch = sign(searchUrl, secret)

        val searchResponse = client.get(signedSearch) {
            header("User-Agent", USER_AGENT)
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "en-US,en;q=0.9")
        }.body<SearchTrackResponse>()

        val searchHeader = searchResponse.message.header
        checkStatusCode(searchHeader.statusCode)

        val bodyElement = searchResponse.message.body
        return if (bodyElement is kotlinx.serialization.json.JsonObject) {
            val json = Json { ignoreUnknownKeys = true }
            val bodyObj = json.decodeFromJsonElement<SearchTrackResponseBody>(bodyElement)
            bodyObj.trackList
        } else {
            emptyList()
        }
    }

    private suspend fun findTracks(
        title: String,
        artist: String,
        token: String,
        secret: String
    ): List<TrackContainer> {
        val cleanedTitle = cleanTitle(title).ifBlank { title.trim() }
        val cleanedArtist = cleanArtist(artist).ifBlank { artist.trim() }

        // Tier 1: Cleaned title + full artist string
        var tracks = searchTracks(trackParam = cleanedTitle, artistParam = artist.trim(), queryParam = null, token, secret)
        if (tracks.isNotEmpty()) return tracks

        // Tier 2: Cleaned title + primary collaborator/artist (if different)
        if (!cleanedArtist.equals(artist.trim(), ignoreCase = true)) {
            tracks = searchTracks(trackParam = cleanedTitle, artistParam = cleanedArtist, queryParam = null, token, secret)
            if (tracks.isNotEmpty()) return tracks
        }

        // Tier 3: Unified query search q="Artist Title"
        val queryStr = "$cleanedArtist $cleanedTitle".trim()
        if (queryStr.isNotBlank()) {
            tracks = searchTracks(trackParam = null, artistParam = null, queryParam = queryStr, token, secret)
            if (tracks.isNotEmpty()) return tracks
        }

        // Tier 4: Fallback to original raw title if cleaned was different
        if (cleanedTitle != title.trim()) {
            tracks = searchTracks(trackParam = title.trim(), artistParam = artist.trim(), queryParam = null, token, secret)
            if (tracks.isNotEmpty()) return tracks
        }

        return emptyList()
    }

    internal fun selectBestTrack(
        tracks: List<TrackContainer>,
        queryTitle: String,
        duration: Int
    ): Track? {
        if (tracks.isEmpty()) return null
        val normalizedQueryTitle = cleanText(queryTitle)

        return tracks.map { it.track }.minByOrNull { track ->
            val trackTitleCleaned = cleanText(track.trackName)
            val textScore = when {
                trackTitleCleaned == normalizedQueryTitle -> 0
                trackTitleCleaned.contains(normalizedQueryTitle) || normalizedQueryTitle.contains(trackTitleCleaned) -> 1
                else -> 2
            }

            // Sync score: prioritize word-sync (RichSync) -> line-sync (Subtitles) -> plain lyrics
            val syncScore = when {
                track.hasRichSync == 1 -> 0
                track.hasSubtitles == 1 -> 1
                track.hasLyrics == 1 -> 2
                else -> 3
            }

            val trackDur = track.trackLength ?: 0
            val durDelta = if (duration > 0 && trackDur > 0) abs(trackDur - duration) else if (trackDur == 0) 999 else Int.MAX_VALUE

            logDebug("Track option: '${track.trackName}' by '${track.artistName}' (id=${track.trackId}, dur=${trackDur}s, richSync=${track.hasRichSync}, sub=${track.hasSubtitles}) -> textScore=$textScore, syncScore=$syncScore, durDelta=$durDelta")

            (textScore.toLong() shl 36) + (syncScore.toLong() shl 32) + durDelta.toLong().coerceAtMost(0xFFFFFFFFL)
        } ?: tracks[0].track
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null
    ): Result<String> = runCatching {
        runWithTokenRetry {
            val secret = getSecret()
            val token = getUserToken(secret)

            logDebug("Searching for track: Title='$title', Artist='$artist', Duration=$duration")
            val trackList = findTracks(title, artist, token, secret)
            if (trackList.isEmpty()) {
                throw IllegalStateException("Track not found on Musixmatch")
            }

            val bestTrack = selectBestTrack(trackList, title, duration)
                ?: throw IllegalStateException("No matching track found on Musixmatch")

            logDebug("Selected best track: '${bestTrack.trackName}' by '${bestTrack.artistName}' (id=${bestTrack.trackId}, richSync=${bestTrack.hasRichSync}, sub=${bestTrack.hasSubtitles})")
            val trackId = bestTrack.trackId

            // Fetch tiers concurrently but yield in strict priority order (Synced first)
            return@runWithTokenRetry kotlinx.coroutines.coroutineScope {
                val richSyncJob = if (bestTrack.hasRichSync == 1 || bestTrack.hasRichSync == null) {
                    async { getRichSyncLyrics(trackId, token, secret) }
                } else null

                val subtitleJob = if (bestTrack.hasSubtitles == 1 || bestTrack.hasSubtitles == null) {
                    async { getSubtitleLyrics(trackId, token, secret) }
                } else null

                val lyricsJob = if (bestTrack.hasLyrics == 1 || bestTrack.hasLyrics == null) {
                    async { getPlainLyrics(trackId, token, secret) }
                } else null

                richSyncJob?.await()?.getOrNull()?.let { return@coroutineScope it }
                subtitleJob?.await()?.getOrNull()?.let { return@coroutineScope it }
                lyricsJob?.await()?.getOrNull()?.let { return@coroutineScope it }

                throw IllegalStateException("No lyrics found for trackId $trackId")
            }
        }
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        callback: (String) -> Unit
    ) {
        runCatching {
            runWithTokenRetry {
                val secret = getSecret()
                val token = getUserToken(secret)

                val trackList = findTracks(title, artist, token, secret)
                if (trackList.isEmpty()) return@runWithTokenRetry

                val bestTrack = selectBestTrack(trackList, title, duration) ?: return@runWithTokenRetry
                val trackId = bestTrack.trackId

                // Fetch tiers concurrently but yield in strict priority order (Synced first)
                kotlinx.coroutines.coroutineScope {
                    val richSyncJob = if (bestTrack.hasRichSync == 1 || bestTrack.hasRichSync == null) {
                        async { getRichSyncLyrics(trackId, token, secret) }
                    } else null

                    val subtitleJob = if (bestTrack.hasSubtitles == 1 || bestTrack.hasSubtitles == null) {
                        async { getSubtitleLyrics(trackId, token, secret) }
                    } else null

                    val plainJob = if (bestTrack.hasLyrics == 1 || bestTrack.hasLyrics == null) {
                        async { getPlainLyrics(trackId, token, secret) }
                    } else null

                    richSyncJob?.await()?.onSuccess { callback(it) }
                    subtitleJob?.await()?.onSuccess { callback(it) }
                    plainJob?.await()?.onSuccess { callback(it) }
                }
            }
        }.onFailure {
            logDebug("getAllLyrics failed: ${it.message}")
        }
    }

    private suspend fun getRichSyncLyrics(trackId: Long, token: String, secret: String): Result<String> = runCatching {
        val richsyncUrl = "${BASE_URL}track.richsync.get?app_id=$APP_ID&format=json&track_id=$trackId&usertoken=$token"
        val signedUrl = sign(richsyncUrl, secret)
        
        val response = client.get(signedUrl) {
            header("User-Agent", USER_AGENT)
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "en-US,en;q=0.9")
        }.body<RichSyncResponse>()

        val statusCode = response.message.header.statusCode
        checkStatusCode(statusCode)

        val bodyElement = response.message.body
        val richsyncObj = if (bodyElement is kotlinx.serialization.json.JsonObject) {
            val json = Json { ignoreUnknownKeys = true }
            val bodyObj = json.decodeFromJsonElement<RichSyncResponseBody>(bodyElement)
            bodyObj.richsync
        } else {
            null
        }

        val body = richsyncObj?.richsyncBody ?: throw IllegalStateException("Richsync unavailable")
        val json = Json { ignoreUnknownKeys = true }
        val entries = json.decodeFromString<List<RichSyncEntry>>(body)
        
        convertRichSyncToLrc(entries)
    }

    private suspend fun getSubtitleLyrics(trackId: Long, token: String, secret: String): Result<String> = runCatching {
        val subtitleUrl = "${BASE_URL}track.subtitle.get?app_id=$APP_ID&format=json&track_id=$trackId&subtitle_format=lrc&usertoken=$token"
        val signedUrl = sign(subtitleUrl, secret)

        val response = client.get(signedUrl) {
            header("User-Agent", USER_AGENT)
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "en-US,en;q=0.9")
        }.body<TrackSubtitleResponse>()

        val statusCode = response.message.header.statusCode
        checkStatusCode(statusCode)

        val bodyElement = response.message.body
        val subtitleObj = if (bodyElement is kotlinx.serialization.json.JsonObject) {
            val json = Json { ignoreUnknownKeys = true }
            val bodyObj = json.decodeFromJsonElement<TrackSubtitleResponseBody>(bodyElement)
            bodyObj.subtitle
        } else {
            null
        }

        val body = subtitleObj?.subtitleBody ?: throw IllegalStateException("Subtitles unavailable")
        if (body.isBlank()) {
            throw IllegalStateException("Subtitle body is empty")
        }
        convertSubtitleJsonToLrc(body)
    }

    private suspend fun getPlainLyrics(trackId: Long, token: String, secret: String): Result<String> = runCatching {
        val lyricsUrl = "${BASE_URL}track.lyrics.get?app_id=$APP_ID&format=json&track_id=$trackId&usertoken=$token"
        val signedUrl = sign(lyricsUrl, secret)

        val response = client.get(signedUrl) {
            header("User-Agent", USER_AGENT)
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "en-US,en;q=0.9")
        }.body<TrackLyricsResponse>()

        val statusCode = response.message.header.statusCode
        checkStatusCode(statusCode)

        val bodyElement = response.message.body
        val lyricsObj = if (bodyElement is kotlinx.serialization.json.JsonObject) {
            val json = Json { ignoreUnknownKeys = true }
            val bodyObj = json.decodeFromJsonElement<TrackLyricsResponseBody>(bodyElement)
            bodyObj.lyrics
        } else {
            null
        }

        val body = lyricsObj?.lyricsBody ?: throw IllegalStateException("Lyrics body unavailable")
        if (body.isBlank()) {
            throw IllegalStateException("Lyrics body is empty")
        }
        body
    }

    private fun formatTime(timeMs: Long, isSyllable: Boolean): String {
        val minutes = (timeMs / 1000) / 60
        val seconds = (timeMs / 1000) % 60
        val millis = timeMs % 1000
        val prefix = if (isSyllable) "<" else "["
        val suffix = if (isSyllable) ">" else "]"
        return String.format(Locale.US, "%s%02d:%02d.%03d%s", prefix, minutes, seconds, millis, suffix)
    }

    internal fun convertRichSyncToLrc(entries: List<RichSyncEntry>): String {
        val sb = StringBuilder()
        for (entry in entries) {
            val lineText = entry.x.trim()
            if (lineText.isEmpty()) continue

            // Line header timestamp
            val lineTimeMs = (entry.ts * 1000).toLong()
            sb.append(formatTime(lineTimeMs, isSyllable = false))

            // Build inline syllable timings
            for (word in entry.l) {
                if (word.c.isBlank()) {
                    sb.append(word.c)
                } else {
                    val wordTimeMs = ((entry.ts + word.o) * 1000).toLong()
                    sb.append(formatTime(wordTimeMs, isSyllable = true))
                    sb.append(word.c)
                }
            }

            // Append line end timestamp tag so word sync animation and instrumental gaps know the exact line end
            val lineEndMs = (entry.te * 1000).toLong()
            if (lineEndMs > lineTimeMs) {
                sb.append(formatTime(lineEndMs, isSyllable = true))
            }
            sb.append("\n")
        }
        return sb.toString().trimEnd()
    }

    internal fun convertSubtitleJsonToLrc(body: String): String {
        return try {
            val entries = Json { ignoreUnknownKeys = true }.decodeFromString<List<SubtitleEntry>>(body)
            val sb = StringBuilder()
            entries.forEach { entry ->
                sb.append(formatTime((entry.time.total * 1000).toLong(), isSyllable = false))
                sb.append(entry.text)
                sb.append("\n")
            }
            sb.toString()
        } catch (e: Exception) {
            body
        }
    }

    internal fun cleanText(text: String): String {
        val noSpecialChars = text.replace(",", " ").replace("&", " ")
        return noSpecialChars.replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun logDebug(message: String) {
        try {
            val logClass = Class.forName("android.util.Log")
            val dMethod = logClass.getMethod("d", String::class.java, String::class.java)
            dMethod.invoke(null, "Musixmatch", message)
        } catch (e: Exception) {
            println("Musixmatch: $message")
        }
    }
}
