package com.music.spotify

import com.music.spotify.models.SpotifyInternalToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.floor

@Serializable
data class SpotifyTokenResponse(
    val access_token: String,
    val token_type: String = "Bearer",
    val scope: String? = null,
    val expires_in: Long = 3600,
    val refresh_token: String? = null,
)

object SpotifyAuth {
    const val DEFAULT_CLIENT_ID = "27915201a590446c80075c3e79060e67"
    const val DEFAULT_REDIRECT_URI = "https://open.spotify.com/"
    const val SCOPES = "playlist-read-private playlist-read-collaborative playlist-modify-public playlist-modify-private user-library-read"

    private const val TOKEN_URL = "https://open.spotify.com/api/token"
    private const val SERVER_TIME_URL = "https://open.spotify.com/api/server-time"
    private const val NUANCE_GIST_URL =
        "https://api.github.com/gists/22ed9c6ba463899e933427f7de1f0eef"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    const val LOGIN_URL = "https://accounts.spotify.com/login?continue=https%3A%2F%2Fopen.spotify.com%2F"

    fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(64)
        secureRandom.nextBytes(code)
        return Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(bytes, 0, bytes.size)
        val digest = messageDigest.digest()
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun buildAuthorizeUrl(
        clientId: String = DEFAULT_CLIENT_ID,
        redirectUri: String = DEFAULT_REDIRECT_URI,
        codeChallenge: String,
    ): String {
        val encodedRedirect = URLEncoder.encode(redirectUri, "UTF-8")
        val encodedScopes = URLEncoder.encode(SCOPES, "UTF-8")
        return "https://accounts.spotify.com/authorize?" +
                "client_id=$clientId" +
                "&response_type=code" +
                "&redirect_uri=$encodedRedirect" +
                "&code_challenge_method=S256" +
                "&code_challenge=$codeChallenge" +
                "&scope=$encodedScopes"
    }

    suspend fun exchangeCode(
        clientId: String = DEFAULT_CLIENT_ID,
        redirectUri: String = DEFAULT_REDIRECT_URI,
        code: String,
        codeVerifier: String,
    ): Result<SpotifyTokenResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("https://accounts.spotify.com/api/token")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("User-Agent", USER_AGENT)

            val params = "grant_type=authorization_code" +
                    "&client_id=" + URLEncoder.encode(clientId, "UTF-8") +
                    "&code=" + URLEncoder.encode(code, "UTF-8") +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8") +
                    "&code_verifier=" + URLEncoder.encode(codeVerifier, "UTF-8")

            connection.outputStream.use { it.write(params.toByteArray(Charsets.UTF_8)) }

            val responseCode = connection.responseCode
            val responseBody = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw Spotify.SpotifyException(responseCode, "Token exchange failed ($responseCode): $err")
            }

            json.decodeFromString<SpotifyTokenResponse>(responseBody)
        }
    }

    suspend fun refreshAccessToken(
        clientId: String = DEFAULT_CLIENT_ID,
        refreshToken: String,
    ): Result<SpotifyTokenResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("https://accounts.spotify.com/api/token")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("User-Agent", USER_AGENT)

            val params = "grant_type=refresh_token" +
                    "&client_id=" + URLEncoder.encode(clientId, "UTF-8") +
                    "&refresh_token=" + URLEncoder.encode(refreshToken, "UTF-8")

            connection.outputStream.use { it.write(params.toByteArray(Charsets.UTF_8)) }

            val responseCode = connection.responseCode
            val responseBody = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw Spotify.SpotifyException(responseCode, "Token refresh failed ($responseCode): $err")
            }

            val parsed = json.decodeFromString<SpotifyTokenResponse>(responseBody)
            if (parsed.refresh_token == null) {
                parsed.copy(refresh_token = refreshToken)
            } else {
                parsed
            }
        }
    }

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    @Serializable
    private data class Nuance(val s: String, val v: Int)

    @Serializable
    private data class GistFile(val content: String)

    @Serializable
    private data class GistFiles(val files: Map<String, GistFile>)

    @Serializable
    private data class ServerTimeResponse(val serverTime: Long)

    suspend fun fetchAccessToken(
        spDc: String,
        spKey: String = "",
    ): Result<SpotifyInternalToken> = runCatching {
        val nuance = fetchNuance()
        val serverTimeSec = fetchServerTime()
        val totp = generateTotp(nuance.s, serverTimeSec)

        val tokenUrl = buildString {
            append(TOKEN_URL)
            append("?reason=transport")
            append("&productType=web-player")
            append("&totp=$totp")
            append("&totpServer=$totp")
            append("&totpVer=${nuance.v}")
        }

        val cookieHeader = buildString {
            append("sp_dc=$spDc")
            if (spKey.isNotEmpty()) {
                append("; sp_key=$spKey")
            }
        }

        val body = withContext(Dispatchers.IO) {
            httpGet(tokenUrl, mapOf("Cookie" to cookieHeader))
        }

        val token = json.decodeFromString<SpotifyInternalToken>(body)

        if (token.isAnonymous || token.accessToken.isBlank()) {
            throw Spotify.SpotifyException(
                401,
                "Received anonymous token — sp_dc cookie is invalid or expired",
            )
        }

        token
    }

    private suspend fun fetchNuance(): Nuance = withContext(Dispatchers.IO) {
        val body = try {
            httpGet(NUANCE_GIST_URL, emptyMap())
        } catch (e: Exception) {
            throw Spotify.SpotifyException(
                503,
                "Failed to fetch TOTP secret from gist: ${e.message}",
            )
        }
        val gist = json.decodeFromString<GistFiles>(body)
        val nuancesJson = gist.files.values.firstOrNull()?.content
            ?: throw Spotify.SpotifyException(500, "Gist has no files")
        val nuances = json.decodeFromString<List<Nuance>>(nuancesJson)
        nuances.maxByOrNull { it.v }
            ?: throw Spotify.SpotifyException(500, "No nuance data found in gist")
    }

    private suspend fun fetchServerTime(): Long = withContext(Dispatchers.IO) {
        val body = try {
            httpGet(SERVER_TIME_URL, emptyMap())
        } catch (e: Exception) {
            throw Spotify.SpotifyException(
                503,
                "Failed to fetch Spotify server time: ${e.message}",
            )
        }
        val response = json.decodeFromString<ServerTimeResponse>(body)
        response.serverTime
    }

    private fun generateTotp(secret: String, serverTimeSec: Long): String {
        val key = base32Decode(secret)
        val interval = 30L
        val timeStep = floor(serverTimeSec.toDouble() / interval).toLong()

        val timeBytes = ByteArray(8)
        var value = timeStep
        for (i in 7 downTo 0) {
            timeBytes[i] = (value and 0xFF).toByte()
            value = value shr 8
        }

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(timeBytes)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        val code = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)

        val otp = code % 1_000_000
        return otp.toString().padStart(6, '0')
    }

    private fun base32Decode(input: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val cleaned = input.uppercase().replace("=", "")

        val output = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0

        for (c in cleaned) {
            val value = alphabet.indexOf(c)
            if (value < 0) continue
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }

        return output.toByteArray()
    }

    private fun httpGet(urlString: String, extraHeaders: Map<String, String>): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json, text/plain, */*")
            connection.setRequestProperty("Accept-Language", "en")
            for ((key, value) in extraHeaders) {
                connection.setRequestProperty(key, value)
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw Spotify.SpotifyException(
                    responseCode,
                    "HTTP $responseCode: $errorBody",
                )
            }

            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
