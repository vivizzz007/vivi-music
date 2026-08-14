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
            "status" to "Status",
            "connected" to "Connected",
            "disconnected" to "Disconnected",
            "connection_failed" to "Connection failed — check the relay server URL",
            "paired_with" to "Paired with",
            "code_generated" to "Code generated",
            "snapshot_received" to "Snapshot received from",
            "synced_settings" to "Synced settings",
            "about" to "About",
            "language" to "Language",
            "choose_language" to "Choose your language",
            "mobile" to "Mobile",
            "de" to "DE",
        ),
    )

    fun get(language: String, key: String): String =
        strings[language]?.get(key) ?: strings["en"]?.get(key) ?: key
}
