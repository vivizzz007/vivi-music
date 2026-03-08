/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.listentogether

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ListenTogetherServer(
    val name: String,
    val url: String,
    val location: String,
    val operator: String
)

object ListenTogetherServers {
    private const val ServersJson = """
        [
          {
            "name": "ViviMusic Sync Server",
            "url": "wss://vivimusic-listen-together.onrender.com",
            "location": "USA",
            "operator": "Vividh"
          }
        ]
    """

    private val json = Json { ignoreUnknownKeys = true }

    val servers: List<ListenTogetherServer> by lazy {
        json.decodeFromString(ServersJson)
    }

    val defaultServerUrl: String
        get() = servers.first().url

    fun findByUrl(url: String): ListenTogetherServer? = servers.firstOrNull { it.url == url }
}
