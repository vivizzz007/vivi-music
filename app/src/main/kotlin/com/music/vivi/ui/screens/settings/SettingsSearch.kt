/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.music.vivi.R

data class SettingSearchEntry(
    val title: String,
    val description: String,
    val category: String,
    val route: String,
    val keywords: List<String> = emptyList(),
    val iconRes: Int = R.drawable.settings
)

@Composable
fun rememberSettingsSearchIndex(): List<SettingSearchEntry> {
    val context = LocalContext.current
    return remember(context) {
        buildSettingSearchIndex(context)
    }
}

fun searchSettings(query: String, allEntries: List<SettingSearchEntry>): List<SettingSearchEntry> {
    val trimmed = query.trim().lowercase()
    if (trimmed.isEmpty()) return emptyList()

    val tokens = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }

    return allEntries.mapNotNull { entry ->
        val titleLower = entry.title.lowercase()
        val descLower = entry.description.lowercase()
        val catLower = entry.category.lowercase()
        val keywordsLower = entry.keywords.map { it.lowercase() }

        var score = 0
        var allTokensMatch = true

        for (token in tokens) {
            val inTitle = titleLower.contains(token)
            val inKeywords = keywordsLower.any { it.contains(token) }
            val inDesc = descLower.contains(token)
            val inCat = catLower.contains(token)

            if (!inTitle && !inKeywords && !inDesc && !inCat) {
                allTokensMatch = false
                break
            }

            if (titleLower.startsWith(token)) score += 50
            else if (inTitle) score += 30
            if (inKeywords) score += 20
            if (inDesc) score += 10
            if (inCat) score += 5
        }

        if (allTokensMatch) entry to score else null
    }
    .sortedByDescending { it.second }
    .map { it.first }
}

private fun buildSettingSearchIndex(context: Context): List<SettingSearchEntry> {
    fun str(resId: Int): String = try { context.getString(resId) } catch (_: Exception) { "" }

    return listOf(
        // System & Updates
        SettingSearchEntry(
            title = str(R.string.system_update).ifEmpty { "System Update" },
            description = str(R.string.update_available).ifEmpty { "Check for app updates and release notes" },
            category = "Updates",
            route = "settings/update",
            keywords = listOf("update", "version", "upgrade", "check", "apk", "latest", "new"),
            iconRes = R.drawable.network_update
        ),
        SettingSearchEntry(
            title = str(R.string.changelog).ifEmpty { "Changelog" },
            description = "View latest release changes and improvements",
            category = "Updates",
            route = "settings/changelog",
            keywords = listOf("changelog", "release notes", "what's new", "changes", "history"),
            iconRes = R.drawable.history
        ),
        SettingSearchEntry(
            title = "Commit History",
            description = "Browse latest commit logs and developer updates",
            category = "Updates",
            route = "settings/commits",
            keywords = listOf("commits", "git", "history", "logs", "development"),
            iconRes = R.drawable.commit
        ),

        // Account
        SettingSearchEntry(
            title = str(R.string.account).ifEmpty { "Account" },
            description = str(R.string.setting_account_desc).ifEmpty { "YouTube Music login and account management" },
            category = "Account",
            route = "settings/account",
            keywords = listOf("google", "youtube", "login", "sign in", "auth", "visitor data", "channel"),
            iconRes = R.drawable.google
        ),
        SettingSearchEntry(
            title = str(R.string.integrations).ifEmpty { "Integrations" },
            description = "Connect external services like Discord and Last.fm",
            category = "Account",
            route = "settings/integrations",
            keywords = listOf("discord", "lastfm", "scrobble", "rpc", "rich presence", "third party"),
            iconRes = R.drawable.integration
        ),
        SettingSearchEntry(
            title = "Discord RPC",
            description = "Display currently playing song as Discord Rich Presence",
            category = "Integrations",
            route = "settings/integrations/discord",
            keywords = listOf("discord", "rpc", "rich presence", "status", "listening", "activity"),
            iconRes = R.drawable.discord
        ),
        SettingSearchEntry(
            title = "Last.fm Scrobbler",
            description = "Track listening stats and scrobble played songs",
            category = "Integrations",
            route = "settings/integrations/lastfm",
            keywords = listOf("lastfm", "last.fm", "scrobble", "stats", "tracking", "music"),
            iconRes = R.drawable.network_node
        ),
        SettingSearchEntry(
            title = str(R.string.listen_together).ifEmpty { "Listen Together" },
            description = str(R.string.setting_listen_together_desc).ifEmpty { "Synchronized playback with friends" },
            category = "Social",
            route = "settings/integrations/listen_together",
            keywords = listOf("listen together", "room", "party", "sync", "friends", "host", "join"),
            iconRes = R.drawable.group
        ),
        SettingSearchEntry(
            title = "Spotify Sync",
            description = "Import and synchronize Spotify playlists",
            category = "Integrations",
            route = "settings/spotify",
            keywords = listOf("spotify", "import", "playlists", "sync", "transfer"),
            iconRes = R.drawable.spotify
        ),

        // Appearance
        SettingSearchEntry(
            title = str(R.string.appearance).ifEmpty { "Appearance" },
            description = str(R.string.setting_appearance_desc).ifEmpty { "Theme, colors, player styling, and fonts" },
            category = "Appearance",
            route = "settings/appearance",
            keywords = listOf("theme", "color", "dark mode", "amoled", "black", "style", "ui"),
            iconRes = R.drawable.palette
        ),
        SettingSearchEntry(
            title = str(R.string.theme).ifEmpty { "Theme & Palettes" },
            description = "Customize dark theme, dynamic colors, and accents",
            category = "Appearance",
            route = "settings/appearance/theme",
            keywords = listOf("dynamic color", "pure black", "amoled", "palette", "material you", "dark", "light"),
            iconRes = R.drawable.palette
        ),
        SettingSearchEntry(
            title = "Canvas & Visuals",
            description = "Animated Canvas and Spotify/Apple canvas backgrounds",
            category = "Appearance",
            route = "settings/appearance/canvas",
            keywords = listOf("canvas", "video", "loop", "animation", "background", "artwork"),
            iconRes = R.drawable.canvas_art
        ),
        SettingSearchEntry(
            title = str(R.string.app_font).ifEmpty { "Custom Font" },
            description = "Choose app typography and font styles",
            category = "Appearance",
            route = "settings/appearance/font",
            keywords = listOf("font", "typography", "text", "style", "typeface"),
            iconRes = R.drawable.edit
        ),

        // Player & Audio
        SettingSearchEntry(
            title = str(R.string.player_and_audio).ifEmpty { "Player & Audio" },
            description = str(R.string.setting_player_desc).ifEmpty { "Equalizer, loudness, skip silence, and playback" },
            category = "Playback",
            route = "settings/player",
            keywords = listOf("audio", "sound", "playback", "volume", "equalizer", "eq", "quality"),
            iconRes = R.drawable.earbud_case
        ),
        SettingSearchEntry(
            title = str(R.string.vivi_equalizer).ifEmpty { "Equalizer" },
            description = "System or built-in audio equalizer and sound effects",
            category = "Playback",
            route = "settings/equalizer",
            keywords = listOf("equalizer", "eq", "bass", "treble", "sound", "effects", "dsp"),
            iconRes = R.drawable.equalizer
        ),
        SettingSearchEntry(
            title = str(R.string.sponsorblock).ifEmpty { "SponsorBlock" },
            description = str(R.string.sponsorblock_desc).ifEmpty { "Automatically skip sponsored segments, intros, and outros" },
            category = "Playback",
            route = "settings/player/sponsorblock",
            keywords = listOf("sponsor", "sponsorblock", "skip", "intro", "outro", "segment", "ads"),
            iconRes = R.drawable.fast_forward
        ),
        SettingSearchEntry(
            title = "JioSaavn Streaming",
            description = "High quality alternative audio source streaming",
            category = "Playback",
            route = "settings/player/jio",
            keywords = listOf("jiosaavn", "jio", "saavn", "flac", "320kbps", "quality", "source"),
            iconRes = R.drawable.earbud_case
        ),
        SettingSearchEntry(
            title = str(R.string.audio_normalization).ifEmpty { "Volume Normalization" },
            description = "Keep audio levels consistent across all tracks",
            category = "Playback",
            route = "settings/player",
            keywords = listOf("normalization", "loudness", "replaygain", "volume", "level", "gain"),
            iconRes = R.drawable.volume_up
        ),
        SettingSearchEntry(
            title = str(R.string.skip_silence).ifEmpty { "Skip Silence" },
            description = "Automatically skip silent gaps between songs",
            category = "Playback",
            route = "settings/player",
            keywords = listOf("skip silence", "gapless", "silence", "gap", "continuous"),
            iconRes = R.drawable.slow_motion_video
        ),
        SettingSearchEntry(
            title = str(R.string.sleep_timer).ifEmpty { "Sleep Timer" },
            description = "Stop playback automatically after a set duration",
            category = "Playback",
            route = "settings/player",
            keywords = listOf("sleep timer", "timer", "stop", "bedtime", "auto stop"),
            iconRes = R.drawable.sleep_timer
        ),
        SettingSearchEntry(
            title = str(R.string.persistent_queue).ifEmpty { "Persistent Queue" },
            description = "Remember and restore current queue across app restarts",
            category = "Playback",
            route = "settings/player",
            keywords = listOf("persistent queue", "save queue", "restore", "remember", "state"),
            iconRes = R.drawable.queue_music
        ),
        SettingSearchEntry(
            title = str(R.string.prevent_duplicate_tracks_in_queue).ifEmpty { "Prevent Duplicate Tracks" },
            description = "Avoid adding identical songs multiple times to the queue",
            category = "Playback",
            route = "settings/player",
            keywords = listOf("duplicate", "deduplicate", "unique", "prevent duplicate"),
            iconRes = R.drawable.queue_music
        ),
        SettingSearchEntry(
            title = str(R.string.auto_load_more).ifEmpty { "Auto Load More Songs" },
            description = "Automatically append more recommended tracks as queue finishes",
            category = "Playback",
            route = "settings/player",
            keywords = listOf("auto load", "infinite", "radio", "continuous", "recommendations"),
            iconRes = R.drawable.queue_music
        ),

        // Content & Language
        SettingSearchEntry(
            title = str(R.string.content).ifEmpty { "Content & Language" },
            description = str(R.string.setting_content_desc).ifEmpty { "App language, country content, explicit filter" },
            category = "Content",
            route = "settings/content",
            keywords = listOf("language", "country", "region", "explicit", "lyrics", "filter"),
            iconRes = R.drawable.language
        ),
        SettingSearchEntry(
            title = str(R.string.ai_lyrics_translation).ifEmpty { "AI Lyrics Translation" },
            description = str(R.string.setting_ai_lyrics_translation_desc).ifEmpty { "Translate synced lyrics in real-time using Gemini AI" },
            category = "Content",
            route = "settings/ai",
            keywords = listOf("ai", "gemini", "translate", "translation", "lyrics", "api key"),
            iconRes = R.drawable.translate
        ),
        SettingSearchEntry(
            title = "Romanization",
            description = "Romanize Asian scripts (Japanese, Korean, Chinese, Hindi)",
            category = "Content",
            route = "settings/content/romanization",
            keywords = listOf("romanize", "pinyin", "romaji", "hangul", "lyrics", "translation"),
            iconRes = R.drawable.translate
        ),
        SettingSearchEntry(
            title = str(R.string.hide_explicit).ifEmpty { "Hide Explicit Content" },
            description = "Filter out songs marked with explicit parental warning labels",
            category = "Content",
            route = "settings/content",
            keywords = listOf("explicit", "clean", "filter", "parental", "hide"),
            iconRes = R.drawable.explicit
        ),

        // Privacy
        SettingSearchEntry(
            title = str(R.string.privacy).ifEmpty { "Privacy" },
            description = str(R.string.setting_privacy_desc).ifEmpty { "Search and playback history settings" },
            category = "Privacy",
            route = "settings/privacy",
            keywords = listOf("privacy", "history", "search history", "playback history", "incognito", "clear"),
            iconRes = R.drawable.security
        ),
        SettingSearchEntry(
            title = str(R.string.pause_search_history).ifEmpty { "Pause Search History" },
            description = "Do not record new search terms",
            category = "Privacy",
            route = "settings/privacy",
            keywords = listOf("pause search", "search history", "incognito"),
            iconRes = R.drawable.security
        ),
        SettingSearchEntry(
            title = str(R.string.pause_listen_history).ifEmpty { "Pause Playback History" },
            description = "Do not record newly played songs to history",
            category = "Privacy",
            route = "settings/privacy",
            keywords = listOf("pause playback", "history", "recent"),
            iconRes = R.drawable.security
        ),

        // Storage & Cache
        SettingSearchEntry(
            title = str(R.string.storage).ifEmpty { "Storage & Cache" },
            description = str(R.string.setting_storage_desc).ifEmpty { "Image cache, song cache, and downloaded audio" },
            category = "Storage",
            route = "settings/storage",
            keywords = listOf("storage", "cache", "disk", "clear cache", "thumbnail", "download size", "memory"),
            iconRes = R.drawable.storage
        ),
        SettingSearchEntry(
            title = str(R.string.image_cache).ifEmpty { "Thumbnail Image Cache" },
            description = "Set max disk space for thumbnails and clear image cache",
            category = "Storage",
            route = "settings/storage",
            keywords = listOf("image cache", "thumbnail", "disk cache", "clear image", "size limit"),
            iconRes = R.drawable.storage
        ),
        SettingSearchEntry(
            title = str(R.string.song_cache).ifEmpty { "Song Audio Cache" },
            description = "Set max disk space for streamed song cache and clear audio cache",
            category = "Storage",
            route = "settings/storage",
            keywords = listOf("song cache", "audio cache", "download", "cache limit"),
            iconRes = R.drawable.storage
        ),

        // Data Saver
        SettingSearchEntry(
            title = str(R.string.data_saver).ifEmpty { "Data Saver" },
            description = str(R.string.setting_data_saver_desc).ifEmpty { "Reduce data consumption over mobile networks" },
            category = "Network",
            route = "settings/datasaver",
            keywords = listOf("data saver", "bandwidth", "cellular", "mobile data", "low quality", "wifi only"),
            iconRes = R.drawable.energy_savings_leaf
        ),

        // Backup & Restore
        SettingSearchEntry(
            title = str(R.string.backup_restore).ifEmpty { "Backup & Restore" },
            description = str(R.string.setting_backup_restore_desc).ifEmpty { "Export and import your library, playlists, and preferences" },
            category = "Data",
            route = "settings/backup_restore",
            keywords = listOf("backup", "restore", "export", "import", "database", "settings file"),
            iconRes = R.drawable.restore
        ),
        SettingSearchEntry(
            title = "Auto Backup",
            description = "Schedule recurring local backups of your library",
            category = "Data",
            route = "settings/backup_restore/autobackup",
            keywords = listOf("auto backup", "schedule", "automatic", "periodical"),
            iconRes = R.drawable.restore
        ),

        // About
        SettingSearchEntry(
            title = str(R.string.about).ifEmpty { "About Vivi Music" },
            description = str(R.string.setting_about_desc).ifEmpty { "Version information, open source license, and GitHub links" },
            category = "About",
            route = "settings/about",
            keywords = listOf("about", "version", "author", "developer", "license", "github", "source"),
            iconRes = R.drawable.info
        )
    )
}
