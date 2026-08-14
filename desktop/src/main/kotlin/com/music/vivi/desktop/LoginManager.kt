package com.music.vivi.desktop

import com.music.innertube.YouTube
import com.music.innertube.models.AccountInfo
import com.music.innertube.models.YouTubeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Desktop YouTube authentication. The desktop has no WebView, so the user logs
 * into music.youtube.com in their browser and pastes the `Cookie` header here.
 * We then extract the account's `DATASYNC_ID` and `VISITOR_DATA` from the
 * music.youtube.com shell and validate the session via `YouTube.accountInfo()`.
 *
 * The cookie is stored locally in `~/.vivimusic/device-sync.json` (same as the
 * Android app, which keeps it in SharedPreferences).
 */
object LoginManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun isLoggedIn(): Boolean {
        val cookie = YouTube.cookie.orEmpty()
        return cookie.isNotBlank() && "SAPISID" in cookie
    }

    /** Restores persisted credentials into the [YouTube] singleton at startup. */
    fun restore() {
        val s = DesktopSettings.load()
        if (s.cookie.isBlank()) return
        YouTube.cookie = s.cookie
        YouTube.dataSyncId = s.dataSyncId.ifBlank { null }
        YouTube.visitorData = s.visitorData.ifBlank { null }
        YouTube.useLoginForBrowse = true
    }

    /**
     * Logs in with a pasted `Cookie` header. Throws with a readable message on
     * failure. Returns the validated account info.
     */
    suspend fun login(cookie: String): AccountInfo = withContext(Dispatchers.IO) {
        val trimmed = cookie.trim()
        if (trimmed.isBlank()) throw IllegalArgumentException("Cookie is empty")
        if ("SAPISID" !in trimmed) {
            throw IllegalArgumentException("Cookie is missing SAPISID — paste the full Cookie header")
        }

        YouTube.cookie = trimmed

        val (dataSyncId, visitorData) = extractAccountIds(trimmed)
        YouTube.dataSyncId = dataSyncId
        YouTube.visitorData = visitorData
        YouTube.useLoginForBrowse = true

        val account = YouTube.accountInfo().getOrElse { e ->
            YouTube.cookie = null
            throw IllegalStateException("Login validation failed: ${e.message ?: "unknown error"}")
        }

        DesktopSettings.save(
            DesktopSettings.load().copy(
                cookie = trimmed,
                dataSyncId = dataSyncId.orEmpty(),
                visitorData = visitorData.orEmpty(),
                accountName = account.name,
                accountEmail = account.email.orEmpty(),
                accountChannelHandle = account.channelHandle.orEmpty(),
            )
        )
        account
    }

    fun logout() {
        YouTube.cookie = null
        YouTube.dataSyncId = null
        YouTube.visitorData = null
        YouTube.useLoginForBrowse = false
        DesktopSettings.save(
            DesktopSettings.load().copy(
                cookie = "",
                dataSyncId = "",
                visitorData = "",
                accountName = "",
                accountEmail = "",
                accountChannelHandle = "",
            )
        )
    }

    /**
     * Fetches the music.youtube.com shell with the session cookie and extracts
     * `DATASYNC_ID` (delegated account id) and `VISITOR_DATA` from the page.
     */
    private suspend fun extractAccountIds(cookie: String): Pair<String?, String?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url("https://music.youtube.com/")
                    .header("Cookie", cookie)
                    .header("User-Agent", YouTubeClient.USER_AGENT_WEB)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null to null
                    val body = response.body.string()
                    val dataSyncId = Regex("\"DATASYNC_ID\"\\s*:\\s*\"([^\"]+)\"")
                        .find(body)?.groupValues?.get(1)
                    val visitorData = Regex("\"VISITOR_DATA\"\\s*:\\s*\"([^\"]+)\"")
                        .find(body)?.groupValues?.get(1)
                    dataSyncId to visitorData
                }
            }.getOrElse { null to null }
        }
}
