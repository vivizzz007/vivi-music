package com.music.vivi.desktop

import kotlinx.serialization.Serializable

/**
 * Desktop navigation destinations. The top-level entries (Home, Search,
 * Library, Settings) live in the sidebar; the others are pushed onto a simple
 * back stack when the user opens an item.
 */
sealed interface Screen {
    data object Home : Screen
    data object Search : Screen
    data object Library : Screen
    data object Settings : Screen
    data class Album(val browseId: String) : Screen
    data class Artist(val browseId: String) : Screen
    data class Playlist(val playlistId: String) : Screen
    data object LocalPlaylists : Screen
    data class LocalPlaylist(val playlistId: String) : Screen
    data class Browse(val browseId: String, val params: String?) : Screen
    data object History : Screen
    data object Player : Screen
    data object Lyrics : Screen
    data object Queue : Screen
    data object Changelog : Screen
    data object Login : Screen

    // Settings sub-screens (ported from the mobile settings structure).
    data object SettingsLanguage : Screen
    data object SettingsAppearance : Screen
    data object SettingsPlayer : Screen
    data object SettingsAccount : Screen
    data object SettingsDevices : Screen
    data object SettingsContent : Screen
    data object SettingsLyrics : Screen
    data object SettingsPrivacy : Screen
    data object SettingsStorage : Screen
    data object SettingsUpdates : Screen
    data object SettingsAbout : Screen
    data object SettingsDeveloper : Screen
    data object SettingsBackup : Screen
}

/**
 * Lightweight "now playing" state shown by the mini-player and the Player /
 * Lyrics screens. Actual audio playback lands in Phase 4; until then this only
 * drives the UI.
 */
@Serializable
data class NowPlaying(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnail: String? = null,
)
