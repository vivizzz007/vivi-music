package com.music.vivi.utils.cipher

import android.content.Context
import android.util.Base64
import com.music.innertube.YouTube
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object PlayerDatesStore {
    private const val TAG = "vivimusic_CipherDates"
    private val remoteUrl = String(
        Base64.decode(
            "aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL1plbWVyVGVhbS96ZW1lci1jaXBoZXIvbWFzdGVyL3BsYXllcl9kYXRlcy5qc29u",
            Base64.DEFAULT,
        ),
        StandardCharsets.UTF_8,
    )

    @Volatile private var dates: Map<String, String> = emptyMap()

    fun initialize(context: Context) {
        val cache = File(File(context.filesDir, "cipher_dates").apply { mkdirs() }, "player_dates.json")
        dates = runCatching { if (cache.exists()) parse(cache.readText()) else emptyMap() }
            .getOrDefault(emptyMap())
        Thread {
            runCatching {
                val body = fetchRemote()
                parse(body).takeIf { it.isNotEmpty() }?.also {
                    dates = it
                    cache.writeText(body)
                }
            }.onFailure { Timber.tag(TAG).d("Dates refresh skipped: ${it.message}") }
        }.apply { isDaemon = true; name = "PlayerDatesRefresh" }.start()
    }

    fun get(hash: String?): String? = hash?.let(dates::get)

    private fun parse(text: String): Map<String, String> = runCatching {
        val root = Json.parseToJsonElement(text) as? JsonObject ?: return emptyMap()
        buildMap {
            root.forEach { (hash, value) ->
                (value as? JsonPrimitive)?.takeIf { it.isString }?.content?.let { put(hash, it) }
            }
        }
    }.getOrDefault(emptyMap())

    private fun fetchRemote(): String {
        val url = URL(remoteUrl)
        val connection = (YouTube.proxy?.let(url::openConnection) ?: url.openConnection()) as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
