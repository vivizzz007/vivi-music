package com.music.vivi.utils

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject

object Updater {
    private val client = HttpClient()
    var lastCheckTime = -1L
        private set

    suspend fun getLatestVersionName(): Result<String> =
        runCatching {
            val response =
                client.get("https://api.github.com/repos/vivizzz007/vivi-music/releases/latest")
                    .bodyAsText()
            val json = JSONObject(response)
            val versionTag = json.getString("tag_name")  // correct for "v3.0.1" etc.
            lastCheckTime = System.currentTimeMillis()
            versionTag
        }
}
