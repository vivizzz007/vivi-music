#!/usr/bin/env python3
"""
Generate the desktop `Localization.kt` from the Android app's string resources.

The desktop string table uses its own keys (e.g. "relay_server", "autoplay_next")
that don't exist verbatim in the Android `strings.xml`. A `MAPPING` table maps
the desktop keys onto the Android resource names where the meaning matches; the
Android translations (values-XX/strings.xml) are then used for every language.

Desktop keys without a mapping fall back to English at runtime, so only the
mapped keys are emitted per language. English stays the full source table.

Run from the repo root:  python3 scripts/generate_desktop_localization.py
"""

import os
import xml.etree.ElementTree as ET

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "app", "src", "main", "res")
OUT = os.path.join(
    REPO,
    "desktop",
    "src",
    "main",
    "kotlin",
    "com",
    "music",
    "vivi",
    "desktop",
    "Localization.kt",
)

# Android resource directory suffix -> desktop language code (the ones the
# desktop edition actually supports; regional variants are skipped).
DIR_TO_LANG = {
    "": "en",
    "-ar": "ar",
    "-as": "as",
    "-az": "az",
    "-b+sr+Latn": "sr",
    "-be": "be",
    "-bg": "bg",
    "-bn": "bn",
    "-bs": "bs",
    "-ca": "ca",
    "-cs": "cs",
    "-de": "de",
    "-el": "el",
    "-es": "es",
    "-et": "et",
    "-eu": "eu",
    "-fi": "fi",
    "-fil": "fil",
    "-fr": "fr",
    "-hi": "hi",
    "-hr": "hr",
    "-hu": "hu",
    "-in": "id",
    "-it": "it",
    "-ja": "ja",
    "-km": "km",
    "-ko": "ko",
    "-lt": "lt",
    "-ml": "ml",
    "-ms": "ms",
    "-nb-rNO": "nb",
    "-nl": "nl",
    "-pa": "pa",
    "-pl": "pl",
    "-pt": "pt",
    "-ro": "ro",
    "-ru": "ru",
    "-sk": "sk",
    "-sl": "sl",
    "-sv": "sv",
    "-ta": "ta",
    "-te": "te",
    "-th": "th",
    "-tr": "tr",
    "-uk": "uk",
    "-vi": "vi",
    "-zh-rCN": "zh-rCN",
    "-zh-rTW": "zh-rTW",
}

# desktop key -> Android string resource name (only where the meaning matches).
# Values come from either strings.xml (base ViMusic strings) or vivi_strings.xml
# (VIVI-specific strings).
MAPPING = {
    # strings.xml
    "search": "search",
    "search_button": "search",
    "search_placeholder": "search_yt_music",
    "error": "error_unknown",
    "device_sync": "device_sync",
    "relay_server": "device_sync_server",
    "generate_code": "device_sync_generate_code",
    "pair": "device_sync_pair",
    "unpair": "device_sync_unpair",
    "home": "home",
    "library": "filter_library",
    "settings": "settings",
    "albums": "albums",
    "artists": "artists",
    "playlists": "playlists",
    "songs": "songs",
    "play": "play",
    "pause": "pause",
    "no_lyrics": "lyrics_not_found",
    "stream_error": "error_no_stream",
    "about": "about",
    "download": "action_download",
    "appearance": "appearance",
    "theme_mode": "theme_mode",
    "player_audio": "player_and_audio",
    "queue": "queue",
    "history": "history",
    "storage": "storage",
    "downloading": "downloading",
    "delete_installers": "clear_downloaded_updates",
    "account": "account",
    "login": "login",
    "logout": "action_logout",
    "not_logged_in": "not_logged_in",
    "shuffle": "shuffle",
    "volume": "volume",
    "mood_and_genres": "mood_and_genres",
    "content": "content",
    "content_language": "content_language",
    "content_country": "content_country",
    "system_default": "system_default",
    "privacy": "privacy",
    "filter_all": "filter_all",
    "filter_songs": "filter_songs",
    "filter_videos": "filter_videos",
    "filter_albums": "filter_albums",
    "filter_artists": "filter_artists",
    "filter_playlists": "filter_featured_playlists",
    "no_results_found": "no_results_found",
    "suggestions": "suggestions",
    "pure_black": "pure_black",
    "audio_quality": "audio_quality",
    "audio_quality_auto": "audio_quality_auto",
    "audio_quality_high": "audio_quality_high",
    "audio_quality_low": "audio_quality_low",
    "remember_shuffle_repeat": "remember_shuffle_and_repeat",
    "persistent_queue": "persistent_queue",
    "lyrics_text_size": "lyrics_text_size",
    # About screen
    "developer_section": "developer_section",
    "app_developer": "app_developer",
    "website": "website",
    "community_section": "community_section",
    "github_repository": "github_repository",
    "telegram_channel": "telegram_channel",
    "app_info_section": "app_info_section",
    "installed_date_title": "installed_date_title",
    "version_code": "version_code",
    "license": "license",
    "unknown": "unknown",
    # vivi_strings.xml
    "lyrics": "lyrics",
    "now_playing": "now_playing",
    "next": "next",
    "previous": "previous",
    "play_all": "play_all",
    "repeat": "repeat",
    "changelog": "changelog_title",
    "connect": "connect",
    "disconnect": "disconnect",
    "logging_in": "logging_in",
    "update_available": "update_available_title",
    "language": "app_language",
    "theme_light": "cd_light_mode",
    "theme_dark": "cd_dark_mode",
    "theme_system": "cd_system_mode",
}

# Full desktop English table (source language).
ENGLISH = {
    "header": "VIVI Music (desktop)",
    "search": "Search",
    "search_placeholder": "Search YouTube Music",
    "search_button": "Search",
    "loading": "Loading…",
    "error": "Error",
    "device_sync": "Device sync",
    "relay_server": "Relay server (wss://)",
    "connect": "Connect",
    "generate_code": "Generate code",
    "generate_new_code": "Generate new code",
    "code_expires_in": "Expires in",
    "code_expired": "Code expired",
    "code_placeholder": "6-digit code",
    "pair": "Pair",
    "unpair": "Unpair device",
    "code_hint": "Enter this code on your phone",
    "lan_sync": "LAN sync (same Wi-Fi)",
    "start_lan": "Start LAN server",
    "stop_lan": "Stop LAN server",
    "lan_address": "Phone connects to",
    "scan_qr": "Scan to connect",
    "lan_hint": "On your phone, open Settings → Devices, set the relay server to the address above, then enter the code.",
    "status": "Status",
    "connected": "Connected",
    "disconnected": "Disconnected",
    "connection_failed": "Connection failed — check the relay server URL",
    "paired_with": "Paired with",
    "code_generated": "Code generated",
    "snapshot_received": "Snapshot received from",
    "synced_settings": "Synced settings",
    "home": "Home",
    "library": "Library",
    "settings": "Settings",
    "albums": "Albums",
    "artists": "Artists",
    "playlists": "Playlists",
    "songs": "Songs",
    "top_results": "Top results",
    "play": "Play",
    "pause": "Pause",
    "lyrics": "Lyrics",
    "no_lyrics": "Lyrics not found",
    "nothing_playing": "Nothing playing",
    "library_placeholder": "Your library will appear here once YouTube login is available (coming in a later phase).",
    "playback_soon": "Audio playback is coming soon — pick a song to see it here.",
    "stream_error": "Could not resolve the audio stream for this track",
    "resolving": "Resolving audio…",
    "back": "Back",
    "about": "About",
    "language": "Language",
    "choose_language": "Choose your language",
    "mobile": "Mobile",
    "de": "DE",
    "updates": "Updates",
    "check_updates": "Check for updates",
    "checking": "Checking…",
    "up_to_date": "You're up to date",
    "update_available": "Update available",
    "download": "Download",
    "include_prereleases": "Include pre-releases",
    "update_failed": "Update check failed",
    "current_version": "Current version",
    "appearance": "Appearance",
    "theme_mode": "Theme mode",
    "theme_system": "System",
    "theme_light": "Light",
    "theme_dark": "Dark",
    "accent_color": "Accent color",
    "play_all": "Play all",
    "queue": "Queue",
    "queue_empty": "Queue is empty",
    "clear_queue": "Clear queue",
    "history": "History",
    "history_empty": "No history yet",
    "player_audio": "Player & audio",
    "autoplay_next": "Autoplay next track",
    "storage": "Storage",
    "cache_size": "Cache size",
    "clear_cache": "Clear cache",
    "changelog": "Changelog",
    "latest_release": "Latest release notes",
    "changelog_unavailable": "Changelog not available",
    "downloading": "Downloading",
    "downloaded": "Downloaded",
    "open_installer": "Open installer",
    "installers_downloaded": "Downloaded installers",
    "delete_installers": "Delete installers",
    "account": "Account",
    "login": "Log in",
    "logout": "Log out",
    "not_logged_in": "Not logged in",
    "logged_in_as": "Logged in as",
    "logging_in": "Logging in…",
    "cookie_label": "Cookie header (from music.youtube.com)",
    "login_instructions": "Log in to music.youtube.com in your browser, then open DevTools → Network, reload, click any music.youtube.com request and copy the full value of its 'Cookie' request header. Paste it below. Your cookie is stored only on this device.",
    "library_login_prompt": "Log in to see your library",
    "library_empty": "Nothing here yet",
    "drag_to_reorder": "Drag the ⠿ handle to reorder",
    "now_playing": "Now playing",
    "shuffle": "Shuffle",
    "repeat": "Repeat",
    "volume": "Volume",
    "previous": "Previous",
    "next": "Next",
    "mood_and_genres": "Mood & genres",
    "data_sync_id_label": "DATASYNC_ID (optional)",
    "visitor_data_label": "VISITOR_DATA (optional)",
    "advanced_login_hint": "Optional: if auto-detection fails, paste DATASYNC_ID and VISITOR_DATA from the music.youtube.com page source.",
    "open_failed": "Could not open the installer. Find it in ~/.vivimusic/updates and open it manually.",
    "content": "Content",
    "content_language": "Content language",
    "content_country": "Content region",
    "system_default": "System default",
    "privacy": "Privacy",
    "privacy_desc": "Session cookies, cached audio and downloaded installers are stored only on this device. You can remove them here.",
    "synced_lyrics": "Synced lyrics",
    "synced_lyrics_desc": "Highlight the current line as the song plays",
    "clear_session": "Clear session data",
    "cache_cleared": "Cache cleared",
    "installers_deleted": "Installers deleted",
    "filter_all": "All",
    "filter_songs": "Songs",
    "filter_videos": "Videos",
    "filter_albums": "Albums",
    "filter_artists": "Artists",
    "filter_playlists": "Playlists",
    "no_results_found": "No results found",
    "suggestions": "Suggestions",
    "shuffle_all": "Shuffle all",
    "pure_black": "Pure black",
    "audio_quality": "Audio quality",
    "audio_quality_auto": "Auto",
    "audio_quality_high": "High",
    "audio_quality_low": "Low",
    "remember_shuffle_repeat": "Remember shuffle and repeat",
    "persistent_queue": "Persistent queue",
    "lyrics_text_size": "Lyrics text size",
    "install_now": "Install now",
    "dismiss": "Dismiss",
    "developer_options": "Developer options",
    "developer_options_enabled": "Developer options enabled",
    "developer_options_desc": "Live CPU, RAM, GPU and network usage of VIVI Music DE.",
    "dev_tools_mode": "Display mode",
    "dev_tools_overlay": "Overlay in main window",
    "dev_tools_window": "Dedicated window",
    "tap_version_code_hint": "Tap the version code 7 times to enable developer options",
    "cpu": "CPU",
    "memory": "Memory",
    "gpu": "GPU",
    "network": "Network",
    "total_traffic": "Total traffic",
    "paired_device": "Paired device",
    "no_paired_device": "No paired device",
    "threads": "Threads",
    "uptime": "Uptime",
    "system_info": "System",
    "process": "Process",
    "system": "System",
    "heap": "Heap",
}


def read_strings(path):
    """Return {name: value} from an Android strings.xml file."""
    out = {}
    try:
        tree = ET.parse(path)
    except (ET.ParseError, OSError):
        return out
    for node in tree.getroot().iter("string"):
        name = node.get("name")
        if name is None:
            continue
        out[name] = node.text or ""
    return out


def kt_escape(s):
    return (
        s.replace("\\", "\\\\")
        .replace('"', '\\"')
        .replace("$", "\\$")
        .replace("\n", "\\n")
        .replace("\t", "\\t")
    )


def read_lang_files(dirpath):
    """Merge strings.xml + vivi_strings.xml + updater_strings.xml (when present)."""
    merged = {}
    for name in ("strings.xml", "vivi_strings.xml", "updater_strings.xml"):
        merged.update(read_strings(os.path.join(dirpath, name)))
    return merged


def main():
    default = read_lang_files(os.path.join(RES, "values"))

    languages = {}
    for entry in os.listdir(RES):
        if not entry.startswith("values"):
            continue
        suffix = entry[len("values"):]
        lang = DIR_TO_LANG.get(suffix)
        if lang is None or lang == "en":
            continue
        strings = read_lang_files(os.path.join(RES, entry))
        if not strings:
            continue
        # Only keep the mapped keys that are actually translated here.
        mapped = {}
        for key, android_name in MAPPING.items():
            val = strings.get(android_name)
            if val:
                mapped[key] = val
        if mapped:
            languages[lang] = mapped

    # Ensure the default also contributes any translated fallback values, so
    # the "en" table uses the Android English wording for the mapped keys.
    for key, android_name in MAPPING.items():
        val = default.get(android_name)
        if val and key not in ENGLISH:
            ENGLISH[key] = val

    def emit_map(entries, indent):
        pad = " " * indent
        lines = [pad + 'mapOf(']
        for key in sorted(entries):
            lines.append(
                pad + '    "%s" to "%s",' % (key, kt_escape(entries[key]))
            )
        lines.append(pad + ')')
        return "\n".join(lines)

    parts = []
    parts.append("package com.music.vivi.desktop\n")
    parts.append("/**")
    parts.append(" * Desktop string table. English is the primary (source) language; other")
    parts.append(" * languages fall back to English until their translations are added under")
    parts.append(" * the matching locale tag (e.g. `\"it\" to mapOf(\"search\" to \"Cerca\", ...)`).")
    parts.append(" *")
    parts.append(" * This file is GENERATED by `scripts/generate_desktop_localization.py` from")
    parts.append(" * the Android app's `strings.xml` resources — do not edit by hand.")
    parts.append(" */")
    parts.append("object Localization {\n")
    parts.append("    private val strings: Map<String, Map<String, String>> = mapOf(")
    parts.append('        "en" to ' + emit_map(ENGLISH, 8).lstrip() + ",")
    for lang in sorted(languages):
        parts.append(
            '        "%s" to %s,' % (lang, emit_map(languages[lang], 8).lstrip())
        )
    parts.append("    )\n")
    parts.append("    fun get(language: String, key: String): String =")
    parts.append('        strings[language]?.get(key) ?: strings["en"]?.get(key) ?: key')
    parts.append("}\n")

    with open(OUT, "w", encoding="utf-8") as f:
        f.write("\n".join(parts))

    print("Wrote %s (%d languages + English)" % (OUT, len(languages)))


if __name__ == "__main__":
    main()
