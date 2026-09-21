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
import kotlin.math.abs

data class SettingSearchEntry(
    val title: String,
    val description: String,
    val category: String,
    val route: String,
    val settingKey: String? = null,
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

/**
 * Calculates Levenshtein edit distance between s1 and s2 with early termination
 * when distance exceeds maxLimit.
 */
fun levenshteinDistance(s1: String, s2: String, maxLimit: Int = 2): Int {
    val m = s1.length
    val n = s2.length
    if (abs(m - n) > maxLimit) return maxLimit + 1
    if (m == 0) return n
    if (n == 0) return m

    val dp = IntArray(n + 1) { it }
    for (i in 1..m) {
        var prev = dp[0]
        dp[0] = i
        var minRow = dp[0]
        for (j in 1..n) {
            val temp = dp[j]
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            dp[j] = minOf(
                dp[j] + 1,
                dp[j - 1] + 1,
                prev + cost
            )
            prev = temp
            if (dp[j] < minRow) minRow = dp[j]
        }
        if (minRow > maxLimit) return maxLimit + 1
    }
    return dp[n]
}

/**
 * Performs typo-tolerant fuzzy and substring search across settings titles, descriptions,
 * keywords, and categories.
 */
fun searchSettings(query: String, allEntries: List<SettingSearchEntry>): List<SettingSearchEntry> {
    val trimmed = query.trim().lowercase()
    if (trimmed.isEmpty()) return emptyList()

    val tokens = trimmed.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return emptyList()

    return allEntries.mapNotNull { entry ->
        val titleLower = entry.title.lowercase()
        val descLower = entry.description.lowercase()
        val catLower = entry.category.lowercase()
        val keywordsLower = entry.keywords.map { it.lowercase() }

        val titleWords = titleLower.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
        val descWords = descLower.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
        val keywordWords = keywordsLower.flatMap { it.split(Regex("[^a-zA-Z0-9]+")) }.filter { it.isNotBlank() }

        var score = 0
        var allTokensMatch = true

        for (token in tokens) {
            var tokenMatched = false

            // 1. Direct exact or prefix match in Title
            if (titleLower == token) {
                score += 120
                tokenMatched = true
            } else if (titleLower.startsWith(token)) {
                score += 80
                tokenMatched = true
            } else if (titleWords.any { it.startsWith(token) }) {
                score += 60
                tokenMatched = true
            } else if (titleWords.any { it.contains(token) } || titleLower.contains(token)) {
                score += 45
                tokenMatched = true
            }

            // 2. Direct match in Keywords
            if (keywordsLower.any { it == token || it.startsWith(token) }) {
                score += 40
                tokenMatched = true
            } else if (keywordWords.any { it.contains(token) } || keywordsLower.any { it.contains(token) }) {
                score += 30
                tokenMatched = true
            }

            // 3. Direct match in Description (matching inside description or inside words)
            if (descWords.any { it.startsWith(token) }) {
                score += 35
                tokenMatched = true
            } else if (descWords.any { it.contains(token) } || descLower.contains(token)) {
                score += 25
                tokenMatched = true
            }

            // 4. Match in Category
            if (catLower.contains(token)) {
                score += 15
                tokenMatched = true
            }

            // 5. Fuzzy / typo-tolerant match (Levenshtein distance)
            if (!tokenMatched && token.length >= 3) {
                val maxDist = if (token.length <= 4) 1 else 2

                // Check title words
                var bestTitleDist = maxDist + 1
                for (w in titleWords) {
                    if (abs(w.length - token.length) <= maxDist) {
                        val d = levenshteinDistance(token, w, maxDist)
                        if (d < bestTitleDist) bestTitleDist = d
                    }
                }
                if (bestTitleDist <= maxDist) {
                    score += 50 - bestTitleDist * 15
                    tokenMatched = true
                }

                // Check keyword words
                if (!tokenMatched) {
                    var bestKwDist = maxDist + 1
                    for (w in keywordWords) {
                        if (abs(w.length - token.length) <= maxDist) {
                            val d = levenshteinDistance(token, w, maxDist)
                            if (d < bestKwDist) bestKwDist = d
                        }
                    }
                    if (bestKwDist <= maxDist) {
                        score += 35 - bestKwDist * 10
                        tokenMatched = true
                    }
                }

                // Check description words
                if (!tokenMatched) {
                    var bestDescDist = maxDist + 1
                    for (w in descWords) {
                        if (abs(w.length - token.length) <= maxDist) {
                            val d = levenshteinDistance(token, w, maxDist)
                            if (d < bestDescDist) bestDescDist = d
                        }
                    }
                    if (bestDescDist <= maxDist) {
                        score += 25 - bestDescDist * 8
                        tokenMatched = true
                    }
                }
            }

            if (!tokenMatched) {
                allTokensMatch = false
                break
            }
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
            title = str(R.string.system_update).ifEmpty { "System update" },
            description = str(R.string.update_available).ifEmpty { "Check for app updates and release notes" },
            category = "Updates",
            route = "settings/update",
            settingKey = "system_update",
            keywords = listOf("update", "version", "upgrade", "check", "apk", "latest", "new"),
            iconRes = R.drawable.network_update
        ),
        SettingSearchEntry(
            title = str(R.string.changelog).ifEmpty { "Changelog" },
            description = "View latest release changes and improvements",
            category = "Updates",
            route = "settings/changelog",
            settingKey = "changelog",
            keywords = listOf("changelog", "release notes", "what's new", "changes", "history"),
            iconRes = R.drawable.history
        ),
        SettingSearchEntry(
            title = "Commit history",
            description = "Browse latest commit logs and developer updates",
            category = "Updates",
            route = "settings/commits",
            settingKey = "commits",
            keywords = listOf("commits", "git", "history", "logs", "development"),
            iconRes = R.drawable.commit
        ),
        SettingSearchEntry(
            title = "Beta & nightly updates",
            description = "Receive cutting-edge beta and nightly workflow updates",
            category = "Updates",
            route = "settings/update",
            settingKey = "beta_nightly",
            keywords = listOf("beta", "nightly", "bleeding edge", "testing", "experimental", "channel"),
            iconRes = R.drawable.network_update
        ),
        SettingSearchEntry(
            title = "Automatic update check",
            description = "Periodically check for new versions on startup in the background",
            category = "Updates",
            route = "settings/update",
            settingKey = "auto_update_check",
            keywords = listOf("auto update", "automatic check", "background check", "notification"),
            iconRes = R.drawable.network_update
        ),
        SettingSearchEntry(
            title = "Downloaded APK manager",
            description = "View and clear previously downloaded update APK files",
            category = "Updates",
            route = "settings/update",
            settingKey = "clear_apk",
            keywords = listOf("apk", "downloaded apk", "clear apk", "installer", "package", "cleanup"),
            iconRes = R.drawable.storage
        ),

        // Account & Integrations
        SettingSearchEntry(
            title = str(R.string.account).ifEmpty { "Account" },
            description = str(R.string.setting_account_desc).ifEmpty { "YouTube Music login and account management" },
            category = "Account",
            route = "settings/account",
            settingKey = "account_login",
            keywords = listOf("google", "youtube", "login", "sign in", "auth", "visitor data", "channel", "account"),
            iconRes = R.drawable.google
        ),
        SettingSearchEntry(
            title = str(R.string.integrations).ifEmpty { "Integrations" },
            description = "Connect external services like Discord and Last.fm",
            category = "Account",
            route = "settings/integrations",
            settingKey = "integrations",
            keywords = listOf("discord", "lastfm", "scrobble", "rpc", "rich presence", "third party"),
            iconRes = R.drawable.integration
        ),
        SettingSearchEntry(
            title = "Discord RPC",
            description = "Display currently playing song as Discord Rich Presence",
            category = "Integrations",
            route = "settings/integrations/discord",
            settingKey = "discord_rpc",
            keywords = listOf("discord", "rpc", "rich presence", "status", "listening", "activity"),
            iconRes = R.drawable.discord
        ),
        SettingSearchEntry(
            title = "Last.fm scrobbler",
            description = "Track listening stats and scrobble played songs",
            category = "Integrations",
            route = "settings/integrations/lastfm",
            settingKey = "lastfm_scrobble",
            keywords = listOf("lastfm", "last.fm", "scrobble", "stats", "tracking", "music", "delay"),
            iconRes = R.drawable.network_node
        ),
        SettingSearchEntry(
            title = str(R.string.listen_together).ifEmpty { "Listen together" },
            description = str(R.string.setting_listen_together_desc).ifEmpty { "Synchronized playback with friends" },
            category = "Social",
            route = "settings/integrations/listen_together",
            settingKey = "listen_together",
            keywords = listOf("listen together", "room", "party", "sync", "friends", "host", "join"),
            iconRes = R.drawable.group
        ),
        SettingSearchEntry(
            title = "Spotify playlist sync",
            description = "Import and export playlists between Vivi Music and Spotify",
            category = "Integrations",
            route = "settings/spotify",
            settingKey = "spotify_sync",
            keywords = listOf("spotify", "import", "playlists", "sync", "transfer", "export", "playlist sync", "spotify sync", "library"),
            iconRes = R.drawable.spotify
        ),

        // Appearance
        SettingSearchEntry(
            title = str(R.string.appearance).ifEmpty { "Appearance" },
            description = str(R.string.setting_appearance_desc).ifEmpty { "Theme, colors, player styling, and fonts" },
            category = "Appearance",
            route = "settings/appearance",
            settingKey = "appearance_general",
            keywords = listOf("theme", "color", "dark mode", "amoled", "black", "style", "ui", "look"),
            iconRes = R.drawable.palette
        ),
        SettingSearchEntry(
            title = str(R.string.theme).ifEmpty { "Theme & palettes" },
            description = "Customize dark theme, dynamic colors, and accents",
            category = "Appearance",
            route = "settings/appearance/theme",
            settingKey = "theme_palettes",
            keywords = listOf("dynamic color", "palette", "material you", "dark", "light", "color wheel", "accent"),
            iconRes = R.drawable.palette
        ),
        SettingSearchEntry(
            title = "Pure black (AMOLED)",
            description = "True pitch black background for OLED and AMOLED displays",
            category = "Appearance",
            route = "settings/appearance/theme",
            settingKey = "pure_black",
            keywords = listOf("pure black", "amoled", "oled", "true black", "dark mode", "pitch black", "battery"),
            iconRes = R.drawable.contrast
        ),
        SettingSearchEntry(
            title = "120Hz high refresh rate",
            description = "Force 120Hz / 90Hz high refresh rate for ultra-smooth animations",
            category = "Appearance",
            route = "settings/appearance",
            settingKey = "high_refresh_rate",
            keywords = listOf("120hz", "90hz", "60hz", "high refresh rate", "refresh rate", "fps", "smooth", "display", "motion"),
            iconRes = R.drawable.speed
        ),
        SettingSearchEntry(
            title = "Floating navigation bar",
            description = "Detached pill-style floating bottom bar",
            category = "Appearance",
            route = "settings/appearance",
            settingKey = "floating_navbar",
            keywords = listOf("floating", "navigation bar", "nav bar", "floating bar", "bottom bar"),
            iconRes = R.drawable.palette
        ),
        SettingSearchEntry(
            title = "Slim navigation bar",
            description = "Compact low-profile bottom navigation bar",
            category = "Appearance",
            route = "settings/appearance",
            settingKey = "slim_navbar",
            keywords = listOf("slim", "compact nav", "navigation bar", "nav bar", "small bottom bar"),
            iconRes = R.drawable.palette
        ),
        SettingSearchEntry(
            title = "Default start tab",
            description = "Set whether the app opens to Home, Explore, or Library",
            category = "Appearance",
            route = "settings/appearance",
            settingKey = "default_tab",
            keywords = listOf("default tab", "start screen", "open tab", "startup tab", "home", "explore", "library"),
            iconRes = R.drawable.palette
        ),
        SettingSearchEntry(
            title = "Pure black mini-player",
            description = "AMOLED black background for the mini-player bar",
            category = "Appearance",
            route = "settings/appearance",
            settingKey = "pure_black_mini_player",
            keywords = listOf("mini player", "pure black mini player", "amoled mini player", "mini-player", "outline"),
            iconRes = R.drawable.contrast
        ),
        SettingSearchEntry(
            title = "Mini-player outline",
            description = "Subtle border outline around the mini-player",
            category = "Appearance",
            route = "settings/appearance",
            settingKey = "mini_player_outline",
            keywords = listOf("mini player outline", "mini player border", "border", "stroke"),
            iconRes = R.drawable.palette
        ),
        SettingSearchEntry(
            title = "Canvas & visuals",
            description = "Animated Canvas and Spotify/Apple canvas backgrounds",
            category = "Appearance",
            route = "settings/appearance/canvas",
            settingKey = "canvas_visuals",
            keywords = listOf("canvas", "video", "loop", "animation", "background", "artwork", "apple canvas", "spotify canvas"),
            iconRes = R.drawable.canvas_art
        ),
        SettingSearchEntry(
            title = str(R.string.app_font).ifEmpty { "Custom font" },
            description = "Choose app typography and font styles (Google Sans, Outfit, etc.)",
            category = "Appearance",
            route = "settings/appearance/font",
            settingKey = "app_font",
            keywords = listOf("font", "typography", "text", "style", "typeface", "google sans", "outfit", "plus jakarta"),
            iconRes = R.drawable.edit
        ),
        SettingSearchEntry(
            title = "Player background style",
            description = "Select background styling: Blurred Album Art, Gradient, or Expressive",
            category = "Appearance",
            route = "settings/appearance",
            settingKey = "player_background",
            keywords = listOf("player background", "blurred artwork", "gradient background", "expressive", "blur"),
            iconRes = R.drawable.palette
        ),
        SettingSearchEntry(
            title = "Player slider style",
            description = "Customize the seekbar progress slider style (Squiggly or Classic)",
            category = "Appearance",
            route = "settings/appearance",
            settingKey = "player_slider_style",
            keywords = listOf("slider", "squiggly slider", "wavy slider", "seekbar", "progress bar", "wave"),
            iconRes = R.drawable.slow_motion_video
        ),
        SettingSearchEntry(
            title = "Rotating vinyl album art",
            description = "Spinning vinyl record animation for player thumbnail",
            category = "Appearance",
            route = "settings/appearance",
            settingKey = "rotating_vinyl",
            keywords = listOf("rotating", "vinyl", "record", "spin", "album cover animation", "disc"),
            iconRes = R.drawable.slow_motion_video
        ),
        SettingSearchEntry(
            title = "Player thumbnail shadow & elevation",
            description = "Adjust depth shadow and glow elevation behind album cover",
            category = "Appearance",
            route = "settings/appearance",
            settingKey = "thumbnail_shadow",
            keywords = listOf("shadow", "elevation", "album art shadow", "thumbnail elevation", "drop shadow"),
            iconRes = R.drawable.palette
        ),
        SettingSearchEntry(
            title = "UI density scale",
            description = "Compact scaling options for high information density",
            category = "Appearance",
            route = "settings/appearance",
            settingKey = "density_scale",
            keywords = listOf("density", "scale", "compact", "zoom", "ui size", "compact view"),
            iconRes = R.drawable.palette
        ),
        SettingSearchEntry(
            title = "Grid items size",
            description = "Adjust thumbnail size for grid cards in library and browse",
            category = "Appearance",
            route = "settings/appearance",
            settingKey = "grid_cell_size",
            keywords = listOf("grid size", "thumbnail size", "card size", "grid items"),
            iconRes = R.drawable.palette
        ),
        SettingSearchEntry(
            title = "Swipe thumbnail to skip track",
            description = "Swipe left or right on album artwork to change songs",
            category = "Appearance",
            route = "settings/appearance",
            settingKey = "swipe_thumbnail",
            keywords = listOf("swipe thumbnail", "gesture", "next song", "previous song", "swipe art"),
            iconRes = R.drawable.fast_forward
        ),
        SettingSearchEntry(
            title = "Dynamic app icon",
            description = "Automatically update home launcher icon with theme",
            category = "Appearance",
            route = "settings/appearance",
            settingKey = "dynamic_icon",
            keywords = listOf("dynamic icon", "launcher icon", "app icon", "themed icon"),
            iconRes = R.drawable.palette
        ),

        // Player & Audio
        SettingSearchEntry(
            title = str(R.string.player_and_audio).ifEmpty { "Player & audio" },
            description = str(R.string.setting_player_desc).ifEmpty { "Equalizer, loudness, skip silence, and playback" },
            category = "Playback",
            route = "settings/player",
            settingKey = "player_general",
            keywords = listOf("audio", "sound", "playback", "volume", "equalizer", "eq", "quality"),
            iconRes = R.drawable.earbud_case
        ),
        SettingSearchEntry(
            title = "Audio quality",
            description = "Streaming bitrate quality (High 256kbps, Medium, Low 128kbps)",
            category = "Playback",
            route = "settings/player",
            settingKey = "audio_quality",
            keywords = listOf("audio quality", "bitrate", "256kbps", "128kbps", "high quality", "low quality", "streaming quality"),
            iconRes = R.drawable.earbud_case
        ),
        SettingSearchEntry(
            title = "Audio offload (hardware acceleration)",
            description = "Use dedicated DSP hardware audio offload to save battery",
            category = "Playback",
            route = "settings/player",
            settingKey = "audio_offload",
            keywords = listOf("audio offload", "hardware acceleration", "dsp", "battery", "power saving", "codec"),
            iconRes = R.drawable.earbud_case
        ),
        SettingSearchEntry(
            title = str(R.string.vivi_equalizer).ifEmpty { "Equalizer" },
            description = "System or built-in audio equalizer, bass boost, and sound presets",
            category = "Playback",
            route = "settings/equalizer",
            settingKey = "equalizer",
            keywords = listOf("equalizer", "eq", "bass", "treble", "sound", "effects", "dsp", "axion", "preset"),
            iconRes = R.drawable.equalizer
        ),
        SettingSearchEntry(
            title = str(R.string.sponsorblock).ifEmpty { "SponsorBlock" },
            description = str(R.string.sponsorblock_desc).ifEmpty { "Automatically skip sponsored segments, intros, and outros" },
            category = "Playback",
            route = "settings/player/sponsorblock",
            settingKey = "sponsorblock",
            keywords = listOf("sponsor", "sponsorblock", "skip", "intro", "outro", "segment", "ads"),
            iconRes = R.drawable.fast_forward
        ),
        SettingSearchEntry(
            title = "JioSaavn streaming",
            description = "High quality alternative audio source streaming (320kbps / FLAC)",
            category = "Playback",
            route = "settings/player/jio",
            settingKey = "jiosaavn_settings",
            keywords = listOf("jiosaavn", "jio", "saavn", "flac", "320kbps", "quality", "source", "alternative stream"),
            iconRes = R.drawable.earbud_case
        ),
        SettingSearchEntry(
            title = str(R.string.audio_normalization).ifEmpty { "Volume normalization" },
            description = "Keep audio levels consistent across all tracks with ReplayGain",
            category = "Playback",
            route = "settings/player",
            settingKey = "audio_normalization",
            keywords = listOf("normalization", "loudness", "replaygain", "volume", "level", "gain", "consistent volume"),
            iconRes = R.drawable.volume_up
        ),
        SettingSearchEntry(
            title = str(R.string.skip_silence).ifEmpty { "Skip silence" },
            description = "Automatically skip silent gaps between songs",
            category = "Playback",
            route = "settings/player",
            settingKey = "skip_silence",
            keywords = listOf("skip silence", "gapless", "silence", "gap", "continuous", "instant skip"),
            iconRes = R.drawable.slow_motion_video
        ),
        SettingSearchEntry(
            title = str(R.string.sleep_timer).ifEmpty { "Sleep timer" },
            description = "Stop playback automatically after a set duration or end of song",
            category = "Playback",
            route = "settings/player",
            settingKey = "sleep_timer",
            keywords = listOf("sleep timer", "timer", "stop", "bedtime", "auto stop", "off timer"),
            iconRes = R.drawable.sleep_timer
        ),
        SettingSearchEntry(
            title = str(R.string.persistent_control_center).ifEmpty { "Persistent control center player" },
            description = str(R.string.persistent_control_center_desc).ifEmpty { "Keep playback controls permanently in OxygenOS / Android Control Center even when swiped from recents" },
            category = "Playback",
            route = "settings/player",
            settingKey = "persistent_control_center",
            keywords = listOf("control center", "oxygenos", "coloros", "notification", "lockscreen", "persistent", "background play", "task manager", "swipe", "resume", "media player", "oneplus"),
            iconRes = R.drawable.notification
        ),
        SettingSearchEntry(
            title = "Loudness enhancer",
            description = "Boost playback volume and loudness beyond default system limit",
            category = "Playback",
            route = "settings/player",
            settingKey = "loudness_enhancer",
            keywords = listOf("loudness", "boost", "volume boost", "gain", "amplifier", "louder", "sound"),
            iconRes = R.drawable.volume_up
        ),
        SettingSearchEntry(
            title = "Pitch & speed control",
            description = "Adjust audio pitch, playback tempo, and track playback speed",
            category = "Playback",
            route = "settings/player",
            settingKey = "pitch_and_speed",
            keywords = listOf("speed", "pitch", "tempo", "playback speed", "rate", "fast", "slow", "pitch shift"),
            iconRes = R.drawable.slow_motion_video
        ),
        SettingSearchEntry(
            title = "Audio crossfade",
            description = "Smoothly crossfade volume between consecutive songs",
            category = "Playback",
            route = "settings/player",
            settingKey = "crossfade",
            keywords = listOf("crossfade", "fade", "transition", "smooth", "gapless"),
            iconRes = R.drawable.queue_music
        ),
        SettingSearchEntry(
            title = str(R.string.persistent_queue).ifEmpty { "Persistent queue" },
            description = "Remember and restore current queue across app restarts",
            category = "Playback",
            route = "settings/player",
            settingKey = "persistent_queue",
            keywords = listOf("persistent queue", "save queue", "restore", "remember", "state", "resume queue"),
            iconRes = R.drawable.queue_music
        ),
        SettingSearchEntry(
            title = str(R.string.prevent_duplicate_tracks_in_queue).ifEmpty { "Prevent duplicate tracks" },
            description = "Avoid adding identical songs multiple times to the queue",
            category = "Playback",
            route = "settings/player",
            settingKey = "prevent_duplicate_tracks",
            keywords = listOf("duplicate", "deduplicate", "unique", "prevent duplicate"),
            iconRes = R.drawable.queue_music
        ),
        SettingSearchEntry(
            title = str(R.string.auto_load_more).ifEmpty { "Auto load more songs" },
            description = "Automatically append more recommended tracks as queue finishes",
            category = "Playback",
            route = "settings/player",
            settingKey = "auto_load_more",
            keywords = listOf("auto load", "infinite", "radio", "continuous", "recommendations", "auto play"),
            iconRes = R.drawable.queue_music
        ),
        SettingSearchEntry(
            title = "Stop playback on task clear",
            description = "Stop music service when the app is swiped away from recent apps",
            category = "Playback",
            route = "settings/player",
            settingKey = "stop_music_on_task_clear",
            keywords = listOf("stop playback", "task clear", "swipe away", "kill app", "close player", "exit"),
            iconRes = R.drawable.slow_motion_video
        ),
        SettingSearchEntry(
            title = "Resume on Bluetooth connect",
            description = "Automatically start playback when Bluetooth headphones or car connects",
            category = "Playback",
            route = "settings/player",
            settingKey = "resume_on_bluetooth",
            keywords = listOf("bluetooth", "auto resume", "headphones", "car", "connect", "audio device"),
            iconRes = R.drawable.earbud_case
        ),
        SettingSearchEntry(
            title = "Pause on mute",
            description = "Pause playback when device media volume is set to zero",
            category = "Playback",
            route = "settings/player",
            settingKey = "pause_on_mute",
            keywords = listOf("pause on mute", "mute", "volume zero", "silence", "auto pause"),
            iconRes = R.drawable.volume_up
        ),
        SettingSearchEntry(
            title = "Keep screen on",
            description = "Prevent phone display from sleeping while on the player screen",
            category = "Playback",
            route = "settings/player",
            settingKey = "keep_screen_on",
            keywords = listOf("keep screen on", "screen awake", "display stay on", "no timeout"),
            iconRes = R.drawable.palette
        ),
        SettingSearchEntry(
            title = "Custom player action buttons",
            description = "Select which quick actions appear on the player screen",
            category = "Playback",
            route = "settings/player",
            settingKey = "custom_player_buttons",
            keywords = listOf("custom player buttons", "player controls", "action buttons", "favorite button", "lyrics button"),
            iconRes = R.drawable.queue_music
        ),
        SettingSearchEntry(
            title = "Seek jump duration",
            description = "Adjust rewind and fast forward extra seconds skip duration",
            category = "Playback",
            route = "settings/player",
            settingKey = "seek_extra_seconds",
            keywords = listOf("seek extra seconds", "skip seconds", "rewind", "fast forward", "jump 10s"),
            iconRes = R.drawable.fast_forward
        ),
        SettingSearchEntry(
            title = "Google Cast",
            description = "Cast and stream playback to Chromecast and Google Home devices",
            category = "Playback",
            route = "settings/player",
            settingKey = "google_cast",
            keywords = listOf("google cast", "chromecast", "cast", "stream to tv", "smart speaker"),
            iconRes = R.drawable.integration
        ),
        SettingSearchEntry(
            title = str(R.string.screen_off_volume_skip).ifEmpty { "Screen-off volume skip track" },
            description = str(R.string.screen_off_volume_skip_desc).ifEmpty { "Long press volume up/down while screen is off to skip to the next or previous track" },
            category = "Playback",
            route = "settings/player",
            settingKey = "screen_off_volume_skip",
            keywords = listOf("volume", "screen off", "skip", "next track", "previous track", "volume skip", "long press", "hardware buttons", "physical keys"),
            iconRes = R.drawable.volume_up
        ),
        SettingSearchEntry(
            title = str(R.string.power_button_camera).ifEmpty { "Double press power button to open camera" },
            description = str(R.string.power_button_camera_desc).ifEmpty { "Quickly press the power button twice to launch camera" },
            category = "Playback",
            route = "settings/player",
            settingKey = "power_button_camera",
            keywords = listOf("power button", "camera", "double press", "power", "double tap", "power camera", "shortcut", "hardware key"),
            iconRes = R.drawable.camera
        ),
        SettingSearchEntry(
            title = "Double press power interval",
            description = "Adjust maximum delay between power button presses to trigger camera",
            category = "Playback",
            route = "settings/player",
            settingKey = "power_button_interval",
            keywords = listOf("interval", "power button interval", "double press speed", "delay", "timing", "timeout", "power button"),
            iconRes = R.drawable.timer
        ),

        // Content & Language
        SettingSearchEntry(
            title = str(R.string.content).ifEmpty { "Content & language" },
            description = str(R.string.setting_content_desc).ifEmpty { "App language, country content, explicit filter" },
            category = "Content",
            route = "settings/content",
            settingKey = "content_general",
            keywords = listOf("content", "language", "country", "region", "filter"),
            iconRes = R.drawable.language
        ),
        SettingSearchEntry(
            title = "App interface language",
            description = "Select user interface translation language",
            category = "Content",
            route = "settings/content",
            settingKey = "app_language",
            keywords = listOf("app language", "ui language", "locale", "translation"),
            iconRes = R.drawable.language
        ),
        SettingSearchEntry(
            title = "Content language & country",
            description = "Select region and language for music recommendations and charts",
            category = "Content",
            route = "settings/content",
            settingKey = "content_language",
            keywords = listOf("content language", "content country", "charts region", "music country", "locale"),
            iconRes = R.drawable.language
        ),
        SettingSearchEntry(
            title = "Search autocomplete region",
            description = "Set geographic region for search bar suggestions",
            category = "Content",
            route = "settings/content",
            settingKey = "search_suggestions_region",
            keywords = listOf("suggestion region", "autocomplete", "search suggestions", "region"),
            iconRes = R.drawable.language
        ),
        SettingSearchEntry(
            title = str(R.string.ai_lyrics_translation).ifEmpty { "AI lyrics translation" },
            description = str(R.string.setting_ai_lyrics_translation_desc).ifEmpty { "Translate synced lyrics in real-time using Gemini AI" },
            category = "Content",
            route = "settings/ai",
            settingKey = "ai_lyrics_translation",
            keywords = listOf("ai", "gemini", "translate", "translation", "lyrics", "api key", "real time"),
            iconRes = R.drawable.translate
        ),
        SettingSearchEntry(
            title = "Romanization",
            description = "Romanize Asian scripts (Japanese, Korean, Chinese, Hindi)",
            category = "Content",
            route = "settings/content/romanization",
            settingKey = "lyrics_romanization",
            keywords = listOf("romanize", "pinyin", "romaji", "hangul", "lyrics", "translation", "japanese", "korean", "chinese"),
            iconRes = R.drawable.translate
        ),
        SettingSearchEntry(
            title = "Draggable lyrics providers",
            description = "Enable, disable, and prioritize lyrics sources (Musixmatch, Kugou, LrcLib, YouLyPlus, Paxsenix, Unison, BiniLyrics)",
            category = "Content",
            route = "settings/content",
            settingKey = "lyrics_providers",
            keywords = listOf("lyrics providers", "musixmatch", "kugou", "lrclib", "youlyplus", "paxsenix", "unison", "binilyrics", "reorder lyrics", "provider"),
            iconRes = R.drawable.lyrics
        ),
        SettingSearchEntry(
            title = "Synced lyrics & styling",
            description = "Apple Music style blur, text glow, line spacing, and auto-scroll",
            category = "Content",
            route = "settings/content",
            settingKey = "synced_lyrics",
            keywords = listOf("lyrics", "synced lyrics", "blur", "glow", "apple lyrics", "lrc", "karaoke", "line spacing", "animation"),
            iconRes = R.drawable.lyrics
        ),
        SettingSearchEntry(
            title = str(R.string.hide_explicit).ifEmpty { "Hide explicit content" },
            description = "Filter out songs marked with explicit parental warning labels",
            category = "Content",
            route = "settings/content",
            settingKey = "hide_explicit",
            keywords = listOf("explicit", "clean", "filter", "parental", "hide", "18+"),
            iconRes = R.drawable.explicit
        ),
        SettingSearchEntry(
            title = "Hide video songs & shorts",
            description = "Hide official music videos and YouTube Shorts from feeds and results",
            category = "Content",
            route = "settings/content",
            settingKey = "hide_video_songs",
            keywords = listOf("hide video songs", "hide shorts", "youtube shorts", "audio only", "video filter"),
            iconRes = R.drawable.slow_motion_video
        ),
        SettingSearchEntry(
            title = "Home screen sections & quick picks",
            description = "Customize, disable, or randomize sections shown on the home page",
            category = "Content",
            route = "settings/content",
            settingKey = "home_sections",
            keywords = listOf("quick picks", "home sections", "randomize home", "disable sections", "home feed"),
            iconRes = R.drawable.palette
        ),
        SettingSearchEntry(
            title = "Network proxy (HTTP / SOCKS5)",
            description = "Configure custom proxy host, port, and credentials for InnerTube requests",
            category = "Content",
            route = "settings/content",
            settingKey = "network_proxy",
            keywords = listOf("proxy", "http proxy", "socks5", "vpn", "bypass", "host", "port", "credentials"),
            iconRes = R.drawable.network_node
        ),
        SettingSearchEntry(
            title = "IP protocol version (IPv4 / IPv6)",
            description = "Select default internet protocol version for network requests",
            category = "Content",
            route = "settings/content",
            settingKey = "ip_protocol",
            keywords = listOf("ipv4", "ipv6", "ip version", "network protocol"),
            iconRes = R.drawable.network_node
        ),

        // Privacy
        SettingSearchEntry(
            title = str(R.string.privacy).ifEmpty { "Privacy" },
            description = str(R.string.setting_privacy_desc).ifEmpty { "Search and playback history settings" },
            category = "Privacy",
            route = "settings/privacy",
            settingKey = "privacy_general",
            keywords = listOf("privacy", "history", "search history", "playback history", "incognito", "clear"),
            iconRes = R.drawable.security
        ),
        SettingSearchEntry(
            title = str(R.string.pause_search_history).ifEmpty { "Pause search history" },
            description = "Do not record new search terms",
            category = "Privacy",
            route = "settings/privacy",
            settingKey = "pause_search_history",
            keywords = listOf("pause search", "search history", "incognito"),
            iconRes = R.drawable.security
        ),
        SettingSearchEntry(
            title = str(R.string.pause_listen_history).ifEmpty { "Pause playback history" },
            description = "Do not record newly played songs to history",
            category = "Privacy",
            route = "settings/privacy",
            settingKey = "pause_listen_history",
            keywords = listOf("pause playback", "history", "recent", "incognito listening"),
            iconRes = R.drawable.security
        ),
        SettingSearchEntry(
            title = "Clear search history",
            description = "Delete all previously saved search queries",
            category = "Privacy",
            route = "settings/privacy",
            settingKey = "clear_search_history",
            keywords = listOf("clear search history", "delete search", "wipe search queries"),
            iconRes = R.drawable.security
        ),
        SettingSearchEntry(
            title = "Clear playback history",
            description = "Delete all played song history and jump back in records",
            category = "Privacy",
            route = "settings/privacy",
            settingKey = "clear_playback_history",
            keywords = listOf("clear playback history", "delete listen history", "wipe history", "jump back in"),
            iconRes = R.drawable.security
        ),

        // Storage & Cache
        SettingSearchEntry(
            title = str(R.string.storage).ifEmpty { "Storage & cache" },
            description = str(R.string.setting_storage_desc).ifEmpty { "Image cache, song cache, and downloaded audio" },
            category = "Storage",
            route = "settings/storage",
            settingKey = "storage_general",
            keywords = listOf("storage", "cache", "disk", "clear cache", "thumbnail", "download size", "memory"),
            iconRes = R.drawable.storage
        ),
        SettingSearchEntry(
            title = "Save downloads to public folder",
            description = "Export downloaded songs directly to phone storage (Music/ViviMusic)",
            category = "Storage",
            route = "settings/storage",
            settingKey = "save_downloads_to_public",
            keywords = listOf("public folder", "export", "phone storage", "music/vivimusic", "save downloads", "external storage", "sd card", "export mp3"),
            iconRes = R.drawable.storage
        ),
        SettingSearchEntry(
            title = str(R.string.image_cache).ifEmpty { "Thumbnail image cache" },
            description = "Set max disk space for thumbnails and clear image cache",
            category = "Storage",
            route = "settings/storage",
            settingKey = "image_cache",
            keywords = listOf("image cache", "thumbnail", "disk cache", "clear image", "size limit"),
            iconRes = R.drawable.storage
        ),
        SettingSearchEntry(
            title = str(R.string.song_cache).ifEmpty { "Song audio cache" },
            description = "Set max disk space for streamed song cache and clear audio cache",
            category = "Storage",
            route = "settings/storage",
            settingKey = "song_cache",
            keywords = listOf("song cache", "audio cache", "download", "cache limit", "stream cache"),
            iconRes = R.drawable.storage
        ),
        SettingSearchEntry(
            title = "Clear audio cache",
            description = "Free up device storage by deleting cached stream audio files",
            category = "Storage",
            route = "settings/storage",
            settingKey = "clear_audio_cache",
            keywords = listOf("clear audio cache", "delete song cache", "wipe cache", "free space"),
            iconRes = R.drawable.storage
        ),
        SettingSearchEntry(
            title = "Clear image cache",
            description = "Free up device storage by deleting cached thumbnail images",
            category = "Storage",
            route = "settings/storage",
            settingKey = "clear_image_cache",
            keywords = listOf("clear image cache", "delete thumbnail cache", "wipe image cache"),
            iconRes = R.drawable.storage
        ),
        SettingSearchEntry(
            title = "Clear all downloads",
            description = "Delete all offline downloaded tracks from device",
            category = "Storage",
            route = "settings/storage",
            settingKey = "clear_all_downloads",
            keywords = listOf("clear downloads", "delete all downloads", "remove offline music"),
            iconRes = R.drawable.storage
        ),

        // Data Saver
        SettingSearchEntry(
            title = str(R.string.data_saver).ifEmpty { "Data saver" },
            description = str(R.string.setting_data_saver_desc).ifEmpty { "Reduce data consumption over mobile networks" },
            category = "Network",
            route = "settings/datasaver",
            settingKey = "data_saver",
            keywords = listOf("data saver", "bandwidth", "cellular", "mobile data", "low quality", "wifi only"),
            iconRes = R.drawable.energy_savings_leaf
        ),
        SettingSearchEntry(
            title = "Restrict canvas on mobile data",
            description = "Automatically disable canvas video loops when on cellular network",
            category = "Network",
            route = "settings/datasaver",
            settingKey = "restrict_canvas",
            keywords = listOf("canvas mobile data", "disable video", "save mobile data", "restrict cellular"),
            iconRes = R.drawable.energy_savings_leaf
        ),

        // Backup & Restore
        SettingSearchEntry(
            title = str(R.string.backup_restore).ifEmpty { "Backup & restore" },
            description = str(R.string.setting_backup_restore_desc).ifEmpty { "Export and import your library, playlists, and preferences" },
            category = "Data",
            route = "settings/backup_restore",
            settingKey = "backup_restore",
            keywords = listOf("backup", "restore", "export", "import", "database", "settings file", "transfer library"),
            iconRes = R.drawable.restore
        ),
        SettingSearchEntry(
            title = "Auto backup",
            description = "Schedule recurring local backups of your library",
            category = "Data",
            route = "settings/backup_restore/autobackup",
            settingKey = "auto_backup",
            keywords = listOf("auto backup", "schedule", "automatic", "periodical", "daily backup"),
            iconRes = R.drawable.restore
        ),

        // About
        SettingSearchEntry(
            title = str(R.string.about).ifEmpty { "About Vivi Music" },
            description = str(R.string.setting_about_desc).ifEmpty { "Version information, open source license, and GitHub links" },
            category = "About",
            route = "settings/about",
            settingKey = "about_app",
            keywords = listOf("about", "version", "author", "developer", "license", "github", "source", "pwpp08"),
            iconRes = R.drawable.info
        )
    )
}
