package com.music.vivi.desktop

import com.music.vivi.sync.LibrarySnapshot
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
    val selectedFont: String = "system",
    /** UI density scale (1f = 100%, then 0.85/0.75/0.65/0.55). */
    val densityScale: Float = 1f,
    /** Adaptive grid cell width in dp for album/artist/playlist grids. */
    val gridItemSize: Int = 160,
    /** Screen transition style between navigations: off / fade / slide. */
    val screenTransition: String = "fade",
    /** Player slider style: slim / squiggly / wavy. */
    val sliderStyle: String = "slim",
    /** Full-player layout variant: classic / new / v2 / expressive. */
    val playerDesign: String = "classic",
    /** Full-player background style: canvas / gradient / blur / glow / apple_music / live_mesh. */
    val playerBackground: String = "canvas",
    /** Slowly rotate the player artwork while playing. */
    val rotatingThumbnail: Boolean = false,
    val miniPlayerStyle: String = "standard",
    val homeUseLastListen: Boolean = false,
    val randomizeHomeOrder: Boolean = false,
    val pauseSearchHistory: Boolean = false,
    val pauseListenHistory: Boolean = false,
    val searchHistory: List<String> = emptyList(),
    val lyricsLineSpacing: Float = 1.35f,
    val discordRpcEnabled: Boolean = false,
    val discordClientId: String = "",
    val lastfmEnabled: Boolean = false,
    val lastfmSession: String = "",
    val lastfmNowPlaying: Boolean = true,
    /** Apple-style mini player variant. */
    val canvasEnabled: Boolean = true,
    val canvasSource: String = "AUTO",
    val autoPlayNext: Boolean = true,
    val sidebarCollapsed: Boolean = false,
    val cookie: String = "",
    val dataSyncId: String = "",
    val visitorData: String = "",
    val accountName: String = "",
    val accountEmail: String = "",
    val accountChannelHandle: String = "",
    val contentLanguage: String = "",
    val contentCountry: String = "",
    val syncedLyrics: Boolean = true,
    val pureBlack: Boolean = false,
    val audioQuality: String = "auto",
    val rememberShuffleRepeat: Boolean = false,
    val isShuffle: Boolean = false,
    val repeatModeKey: String = "OFF",
    val persistentQueue: Boolean = true,
    val queueJson: String = "",
    val queueIndex: Int = 0,
    val lyricsTextSize: Float = 18f,
    val library: LibrarySnapshot? = null,
    val firstLaunchDate: Long = 0L,
    val developerOptions: Boolean = false,
    val devToolsMode: String = "OVERLAY",
    val devOverlayMovable: Boolean = true,
    val devShowInTitleBar: Boolean = false,
    val devProfile: String = "FULL",
    val updateCheckIntervalHours: Int = 24,
    /** Update source: "fork" (PiBOH/vivi-music, default) or "original" (vivizzz007/vivi-music). */
    val updateSource: String = "fork",
    /** Where update notifications are shown: "in_app" (default) or "native". */
    val notificationMode: String = "in_app",
    /** Record every notification (in-app and native) for the history screen. */
    val saveNotificationHistory: Boolean = true,
    /** Seconds before an in-app (main window) notification auto-dismisses; 0 = never. */
    val inAppNotificationDurationSeconds: Int = 5,
    /** Recent notifications (newest first), capped at a small number. */
    val notificationHistory: List<NotificationRecord> = emptyList(),
    /** Master toggle for automatic backups. */
    val autoBackupEnabled: Boolean = false,
    /** Run an automatic backup once a week. */
    val autoBackupWeekly: Boolean = false,
    /** Run an automatic backup before installing an update. */
    val autoBackupBeforeUpdate: Boolean = true,
    /** Sync the in-app (VIVI) player volume slider between devices. */
    val syncViviVolume: Boolean = true,
)

object DesktopSettings {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    /** Serializes load/save so concurrent writers can't clobber each other. */
    private val lock = Any()

    private val file: File by lazy {
        File(System.getProperty("user.home"), ".vivimusic/device-sync.json").apply {
            parentFile?.mkdirs()
        }
    }

    fun load(): DesktopSyncState = synchronized(lock) {
        try {
            if (file.exists()) json.decodeFromString(DesktopSyncState.serializer(), file.readText())
            else DesktopSyncState()
        } catch (_: Exception) {
            DesktopSyncState()
        }
    }

    fun save(state: DesktopSyncState) {
        synchronized(lock) {
            try {
                file.writeText(json.encodeToString(DesktopSyncState.serializer(), state))
            } catch (_: Exception) {
                // best-effort
            }
        }
    }

    /**
     * Atomic read-modify-write: applies [transform] to the freshly-loaded state
     * and saves the result under the same lock. Use this instead of
     * `save(load().copy(...))`, which races when the UI thread and the
     * device-sync IO coroutines save at the same time and can silently drop a
     * setting the user just changed (e.g. the notification mode).
     */
    fun update(transform: (DesktopSyncState) -> DesktopSyncState) {
        synchronized(lock) {
            save(transform(load()))
        }
    }

    fun newDeviceId(): String {
        val existing = load().deviceId
        if (existing.isNotEmpty()) return existing
        val id = UUID.randomUUID().toString()
        update { it.copy(deviceId = id) }
        return id
    }

    /**
     * Returns the first-launch timestamp (epoch millis), recording "now" on the
     * very first call so the About screen shows the first-launch date rather than
     * the last-update install date.
     */
    fun ensureFirstLaunchDate(): Long {
        val state = load()
        if (state.firstLaunchDate > 0) return state.firstLaunchDate
        val now = System.currentTimeMillis()
        update { it.copy(firstLaunchDate = now) }
        return now
    }
}
