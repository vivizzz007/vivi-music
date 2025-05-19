package com.music.vivi.updater

import com.google.gson.Gson
import com.music.vivi.models.GithubRelease
import java.net.HttpURLConnection
import java.net.URL

object GithubUpdater {
    private const val LATEST_RELEASE_URL = "https://github.com/vivizzz007/vivi-music/releases/latest"

    fun fetchLatestRelease(): GithubRelease? {
        return try {
            val url = URL(LATEST_RELEASE_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.doInput = true

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val response = inputStream.bufferedReader().use { it.readText() }
                Gson().fromJson(response, GithubRelease::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
