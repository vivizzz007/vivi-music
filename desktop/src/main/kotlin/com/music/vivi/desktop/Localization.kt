package com.music.vivi.desktop

/**
 * Desktop string table. English is the primary (source) language; other
 * languages fall back to English until their translations are added under the
 * matching locale tag (e.g. `"it" to mapOf("search" to "Cerca", ...)`).
 */
object Localization {
    private val strings: Map<String, Map<String, String>> = mapOf(
        "en" to mapOf(
            "header" to "VIVI Music (desktop)",
            "search" to "Search",
            "search_placeholder" to "Search YouTube Music",
            "search_button" to "Search",
            "loading" to "Loading…",
            "error" to "Error",
            "device_sync" to "Device sync",
            "relay_server" to "Relay server (wss://)",
            "connect" to "Connect",
            "generate_code" to "Generate code",
            "code_placeholder" to "6-digit code",
            "pair" to "Pair",
            "code_hint" to "Code to enter on the other device",
            "lan_sync" to "LAN sync (same Wi-Fi)",
            "start_lan" to "Start LAN server",
            "stop_lan" to "Stop LAN server",
            "lan_address" to "Phone connects to",
            "lan_hint" to "On your phone, open Settings → Devices, set the relay server to the address above, then enter the code.",
            "status" to "Status",
            "connected" to "Connected",
            "disconnected" to "Disconnected",
            "connection_failed" to "Connection failed — check the relay server URL",
            "paired_with" to "Paired with",
            "code_generated" to "Code generated",
            "snapshot_received" to "Snapshot received from",
            "synced_settings" to "Synced settings",
            "home" to "Home",
            "library" to "Library",
            "settings" to "Settings",
            "albums" to "Albums",
            "artists" to "Artists",
            "playlists" to "Playlists",
            "songs" to "Songs",
            "top_results" to "Top results",
            "play" to "Play",
            "pause" to "Pause",
            "lyrics" to "Lyrics",
            "no_lyrics" to "Lyrics not found",
            "nothing_playing" to "Nothing playing",
            "library_placeholder" to "Your library will appear here once YouTube login is available (coming in a later phase).",
            "playback_soon" to "Audio playback is coming soon — pick a song to see it here.",
            "stream_error" to "Could not resolve the audio stream for this track",
            "resolving" to "Resolving audio…",
            "back" to "Back",
            "about" to "About",
            "language" to "Language",
            "choose_language" to "Choose your language",
            "mobile" to "Mobile",
            "de" to "DE",
            "updates" to "Updates",
            "check_updates" to "Check for updates",
            "checking" to "Checking…",
            "up_to_date" to "You're up to date",
            "update_available" to "Update available",
            "download" to "Download",
            "include_prereleases" to "Include pre-releases",
            "update_failed" to "Update check failed",
            "current_version" to "Current version",
        ),
    )

    fun get(language: String, key: String): String =
        strings[language]?.get(key) ?: strings["en"]?.get(key) ?: key
}
