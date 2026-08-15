package com.music.vivi.desktop

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Minimal JSON-backed settings store for the desktop edition.
 * Persists the device id, pairing id, relay url and the last synced
 * settings snapshot under `~/.vivimusic/device-sync.json`.
 */
@Serializable
data class DesktopSyncState(
    val deviceId: String = "",
    val deviceName: String = "Desktop",
    val pairId: String = "",
    val serverUrl: String = "",
    val settings: Map<String, String> = emptyMap(),
    val language: String = "",
    val includePreReleases: Boolean = false,
    val darkMode: String = "system",
    val accentColor: Int = 0xFFED5564.toInt(),
    val autoPlayNext: Boolean = true,
    val sidebarCollapsed: Boolean = false,
    val cookie: String = "",
    val dataSyncId: String = "",
    val visitorData: String = "",
    val accountName: String = "",
    val accountEmail: String = "",
    val accountChannelHandle: String = "",
)

object DesktopSettings {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    private val file: File by lazy {
        File(System.getProperty("user.home"), ".vivimusic/device-sync.json").apply {
            parentFile?.mkdirs()
        }
    }

    fun load(): DesktopSyncState = try {
        if (file.exists()) json.decodeFromString(DesktopSyncState.serializer(), file.readText())
        else DesktopSyncState()
    } catch (_: Exception) {
        DesktopSyncState()
    }

    fun save(state: DesktopSyncState) {
        try {
            file.writeText(json.encodeToString(DesktopSyncState.serializer(), state))
        } catch (_: Exception) {
            // best-effort
        }
    }

    fun newDeviceId(): String {
        val existing = load().deviceId
        if (existing.isNotEmpty()) return existing
        val id = UUID.randomUUID().toString()
        save(load().copy(deviceId = id))
        return id
    }
}
