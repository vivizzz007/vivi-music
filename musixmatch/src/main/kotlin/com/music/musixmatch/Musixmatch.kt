/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * If you copy, adapt, or distribute this code, you must attribute the original source:
 * https://github.com/vivizzz007/vivi-music
 * and adhere to the terms of the GPL-3.0 license.
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

    private val secretCache = AtomicReference<String?>(null)
    private val tokenCache = AtomicReference<String?>(null)

    private val client by lazy {
        HttpClient(OkHttp) {
            expectSuccess = true
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

    private suspend fun getSecret(): String {
        val cached = secretCache.get()
        if (cached != null) return cached

        val secret = try {
            val searchPage = client.get("https://www.musixmatch.com/search") {
                header("User-Agent", USER_AGENT)
                header("Cookie", "mxm_bab=AB")
                header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                header("Accept-Language", "en-US,en;q=0.9")
                header("Sec-Fetch-Dest", "document")
                header("Sec-Fetch-Mode", "navigate")
                header("Sec-Fetch-Site", "none")
                header("Sec-Fetch-User", "?1")
                header("Upgrade-Insecure-Requests", "1")
            }.body<String>()

            val regex = """src="([^"]*/_next/static/chunks/pages/_app-[^"]+\.js)"""".toRegex()
            val match = regex.find(searchPage) ?: throw IllegalStateException("Could not find _app JS in Musixmatch page")
            val appJsUrl = match.groupValues[1]

            val jsContent = client.get(appJsUrl) {
                header("User-Agent", USER_AGENT)
                header("Accept", "*/*")
                header("Accept-Language", "en-US,en;q=0.9")
                header("Sec-Fetch-Dest", "script")
                header("Sec-Fetch-Mode", "no-cors")
                header("Sec-Fetch-Site", "cross-site")
            }.body<String>()

            val secretRegex = """from\(\s*"(.*?)"\s*\.split""".toRegex()
            val secretMatch = secretRegex.find(jsContent) ?: throw IllegalStateException("Could not find secret in Musixmatch JS")
            val encodedSecret = secretMatch.groupValues[1]
            
            val reversed = encodedSecret.reversed()
            val decodedBytes = Base64.getDecoder().decode(reversed)
            String(decodedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            logDebug("Failed to fetch secret dynamically, falling back to static secret: ${e.message}")
            "b3dc8788299f5806a70a6a20a0cb0ffc"
        }
        
        secretCache.set(secret)
        return secret
    }

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

        val tokenUrl = "${BASE_URL}token.get?app_id=web-desktop-app-v1.0&format=json"
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
                throw retryException
            }
        }
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

            // 1. Search for the track using separate track/artist params for accurate version matching
            val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.name())
            val encodedArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8.name())
            val searchUrl = "${BASE_URL}track.search?app_id=web-desktop-app-v1.0&format=json&q_track=$encodedTitle&q_artist=$encodedArtist&f_has_lyrics=true&page_size=10&usertoken=$token"
            val signedSearch = sign(searchUrl, secret)

            val searchResponse = client.get(signedSearch) {
                header("User-Agent", USER_AGENT)
                header("Accept", "application/json, text/plain, */*")
                header("Accept-Language", "en-US,en;q=0.9")
            }.body<SearchTrackResponse>()

            val searchHeader = searchResponse.message.header
            checkStatusCode(searchHeader.statusCode)

            val bodyElement = searchResponse.message.body
            val trackList = if (bodyElement is kotlinx.serialization.json.JsonObject) {
                val json = Json { ignoreUnknownKeys = true }
                val bodyObj = json.decodeFromJsonElement<SearchTrackResponseBody>(bodyElement)
                bodyObj.trackList
            } else {
                emptyList()
            }

            if (trackList.isEmpty()) {
                val code = searchResponse.message.header.statusCode
                throw IllegalStateException("Track not found on Musixmatch (status: $code)")
            }

            val normalizedQueryTitle = cleanText(title)
            logDebug("Searching for track: Title='$title', Artist='$artist', Duration=$duration")

            // Score each track using title match as primary criteria, duration as secondary
            val bestTrack = trackList.map { it.track }.minByOrNull { track ->
                val trackTitleCleaned = cleanText(track.trackName)
                val textScore = when {
                    trackTitleCleaned == normalizedQueryTitle -> 0
                    trackTitleCleaned.contains(normalizedQueryTitle) || normalizedQueryTitle.contains(trackTitleCleaned) -> 1
                    else -> 2
                }

                val trackDur = track.trackLength ?: 0
                val durDelta = if (duration > 0 && trackDur > 0) abs(trackDur - duration) else if (trackDur == 0) 999 else Int.MAX_VALUE
                
                logDebug("Track option: '${track.trackName}' by '${track.artistName}' (id=${track.trackId}, dur=${trackDur}s) -> textScore=$textScore, durDelta=$durDelta")
                
                (textScore.toLong() shl 32) + durDelta
            } ?: trackList[0].track

            logDebug("Selected best track: '${bestTrack.trackName}' by '${bestTrack.artistName}' (id=${bestTrack.trackId})")
            val trackId = bestTrack.trackId
            val trackLength = bestTrack.trackLength ?: duration

            // 2. Fetch lyrics using 3-tier priority chain
            // Tier 1: RichSync (Word-level timing)
            val richsyncResult = getRichSyncLyrics(trackId, trackLength, token, secret)
            if (richsyncResult.isSuccess) {
                return@runWithTokenRetry richsyncResult.getOrThrow()
            }

            // Tier 2: Subtitle (Line-level synced timing)
            val subtitleResult = getSubtitleLyrics(trackId, trackLength, token, secret)
            if (subtitleResult.isSuccess) {
                return@runWithTokenRetry subtitleResult.getOrThrow()
            }

            // Tier 3: Plain Lyrics (Unsynced lyrics)
            val plainLyricsResult = getPlainLyrics(trackId, token, secret)
            if (plainLyricsResult.isSuccess) {
                return@runWithTokenRetry plainLyricsResult.getOrThrow()
            }

            throw IllegalStateException("No lyrics found for trackId $trackId")
        }
    }

    private suspend fun getRichSyncLyrics(trackId: Long, duration: Int, token: String, secret: String): Result<String> = runCatching {
        val richsyncUrl = "${BASE_URL}track.richsync.get?app_id=web-desktop-app-v1.0&format=json&track_id=$trackId&usertoken=$token&f_richsync_length=$duration&f_richsync_length_max_deviation=10"
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

    private suspend fun getSubtitleLyrics(trackId: Long, duration: Int, token: String, secret: String): Result<String> = runCatching {
        val subtitleUrl = "${BASE_URL}track.subtitle.get?app_id=web-desktop-app-v1.0&format=json&track_id=$trackId&usertoken=$token&f_subtitle_length=$duration&f_subtitle_length_max_deviation=10"
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
        body
    }

    private suspend fun getPlainLyrics(trackId: Long, token: String, secret: String): Result<String> = runCatching {
        val lyricsUrl = "${BASE_URL}track.lyrics.get?app_id=web-desktop-app-v1.0&format=json&track_id=$trackId&usertoken=$token"
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
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun cleanText(text: String): String {
        return text.replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]"), "")
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
