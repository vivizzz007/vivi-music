package com.music.spotify

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpStatusCode

object SpotifyCanvasProvider {
    private val client = HttpClient(OkHttp) {
        expectSuccess = false
    }

    suspend fun getCanvasUrl(trackId: String, accessToken: String): Result<String> = runCatching {
        val trackUri = "spotify:track:$trackId"
        
        val uriBytes = trackUri.toByteArray(Charsets.UTF_8)
        val innerMessage = ByteArray(2 + uriBytes.size)
        innerMessage[0] = 0x0A
        innerMessage[1] = uriBytes.size.toByte()
        uriBytes.copyInto(innerMessage, 2)
        
        val payload = ByteArray(2 + innerMessage.size)
        payload[0] = 0x0A
        payload[1] = innerMessage.size.toByte()
        innerMessage.copyInto(payload, 2)

        val response = client.post("https://spclient.wg.spotify.com/canvaz-cache/v0/canvases") {
            header("Authorization", "Bearer $accessToken")
            header("Content-Type", "application/x-protobuf")
            header("Accept", "application/x-protobuf")
            setBody(payload)
        }

        println("SpotifyCanvas: Canvas API responded with status ${response.status}")

        if (response.status != HttpStatusCode.OK) {
            error("Canvas API error: ${response.status}")
        }

        val resBytes = response.readBytes()
        val text = String(resBytes, Charsets.UTF_8)
        
        // Find valid URL in the pseudo-string (mostly binary but strings are intact)
        val regex = Regex("https://[a-zA-Z0-9./_\\-?=&#]+")
        val urls = regex.findAll(text).map { it.value }.toList()
        
        val canvasUrl = urls.find { it.contains("canvaz") || it.contains("video") } ?: error("Canvas URL not found in response")
        canvasUrl
    }
}
