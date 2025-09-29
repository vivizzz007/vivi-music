package com.music.vivi.utils

import com.music.vivi.BuildConfig
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
            val versionName = json.getString("name")
            lastCheckTime = System.currentTimeMillis()
            versionName
        }

    fun getLatestDownloadUrl(): String {
        val baseUrl = "https://github.com/vivizzz007/vivi-music/releases/latest/download/"
        val architecture = BuildConfig.ARCHITECTURE
        return if (architecture == "universal") {
            baseUrl + "vivi.apk"
        } else {
            baseUrl + "app-${architecture}-release.apk"
        }
    }
}
