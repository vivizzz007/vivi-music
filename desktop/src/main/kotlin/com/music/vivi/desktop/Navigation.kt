package com.music.vivi.desktop

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
    data object History : Screen
    data object Player : Screen
    data object Lyrics : Screen
    data object Queue : Screen
    data object Changelog : Screen
}

/**
 * Lightweight "now playing" state shown by the mini-player and the Player /
 * Lyrics screens. Actual audio playback lands in Phase 4; until then this only
 * drives the UI.
 */
data class NowPlaying(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnail: String?,
)
