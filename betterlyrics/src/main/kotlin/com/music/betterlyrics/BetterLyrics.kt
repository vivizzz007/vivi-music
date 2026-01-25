package com.music.betterlyrics

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import java.util.Locale

object BetterLyrics {
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
        }
    }

    suspend fun getLyrics(title: String, artist: String, duration: Int?): Result<String> = runCatching {
        fetchLyrics(title, artist, duration)
    }

    suspend fun getAllLyrics(title: String, artist: String, duration: Int, callback: (String) -> Unit) {
        // Try exact match first
        runCatching {
            callback(fetchLyrics(title, artist, duration))
        }.onFailure {
            // If exact match fails, try without duration (broader search)
            runCatching {
                callback(fetchLyrics(title, artist, null))
            }
        }
    }

    private suspend fun fetchLyrics(title: String, artist: String, duration: Int?): String {
        val url = "https://lyrics-api.boidu.dev/getLyrics"
        val response = client.get(url) {
            parameter("s", title)
            parameter("a", artist)
            if (duration != null && duration != -1) {
                parameter("d", duration)
            }
        }

        val json = response.body<JsonObject>()
        val ttml = json["ttml"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalStateException("No TTML found in response")

        return parseTtmlToLrc(ttml)
    }

    private fun parseTtmlToLrc(ttml: String): String {
        val doc = Jsoup.parse(ttml)
        val paragraphs = doc.select("p")
        val sb = StringBuilder()

        for (p in paragraphs) {
            val begin = p.attr("begin")
            val text = p.text()

            if (begin.isNotEmpty() && text.isNotEmpty()) {
                val lineStartMillis = ttmlTimeToMillis(begin)
                val lrcTime = formatLrcTime(lineStartMillis)

                val spans = p.select("span")
                if (spans.isNotEmpty()) {
                    val sbLine = StringBuilder()
                    for (span in spans) {
                        val spanBegin = span.attr("begin")
                        val spanEnd = span.attr("end")
                        val spanText = span.text()

                        if (spanBegin.isNotEmpty()) {
                            val spanStartMillis = ttmlTimeToMillis(spanBegin)
                            val spanStartTime = formatLrcTime(spanStartMillis)

                            var durationStr = ""
                            if (spanEnd.isNotEmpty()) {
                                val spanEndMillis = ttmlTimeToMillis(spanEnd)
                                val duration = spanEndMillis - spanStartMillis
                                if (duration > 0) {
                                    durationStr = ",$duration"
                                }
                            }

                            sbLine.append(" <$spanStartTime$durationStr> $spanText")
                        } else {
                            sbLine.append(" $spanText")
                        }
                    }
                    sb.append("[$lrcTime]${sbLine.toString().trim()}\n")
                } else {
                    sb.append("[$lrcTime]$text\n")
                }
            }
        }

        return sb.toString().trim()
    }

    private fun ttmlTimeToMillis(ttmlTime: String): Long {
        val parts = ttmlTime.split(":")
        var seconds = 0.0

        if (parts.size == 3) {
            seconds += parts[0].toDouble() * 3600
            seconds += parts[1].toDouble() * 60
            seconds += parts[2].toDouble()
        } else if (parts.size == 2) {
            seconds += parts[0].toDouble() * 60
            seconds += parts[1].toDouble()
        } else {
            seconds = ttmlTime.toDoubleOrNull() ?: 0.0
        }

        return (seconds * 1000).toLong()
    }

    private fun formatLrcTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val remainingSeconds = totalSeconds % 60
        val hundredths = (millis % 1000) / 10
        return String.format(Locale.US, "%02d:%02d.%02d", minutes, remainingSeconds, hundredths)
    }
}
