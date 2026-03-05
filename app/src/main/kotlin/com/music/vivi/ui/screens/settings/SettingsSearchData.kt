/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import com.music.vivi.R

/**
 * Represents a searchable settings entry with its title, icon, category, route, and setting key.
 * [settingKey] matches the [settingKey] field in Material3SettingsItem for highlight+scroll.
 */
data class SettingsSearchEntry(
    val titleResId: Int,
    val iconResId: Int,
    val categoryResId: Int,
    val route: String,
    val settingKey: String,
    val extraKeywords: List<String> = emptyList()
)

/**
 * Returns all searchable settings entries across every settings sub-screen.
 */
fun getSettingsSearchEntries(): List<SettingsSearchEntry> = buildList {

    // ── Update ──
    fun update(titleResId: Int, iconResId: Int, key: String, extra: List<String> = emptyList()) =
        add(SettingsSearchEntry(titleResId, iconResId, R.string.system_update, "settings/update", key, extra))

    update(R.string.system_update, R.drawable.network_update, "system_update", listOf("update", "version"))
    update(R.string.auto_update_check, R.drawable.update, "auto_update_check", listOf("auto update"))
    update(R.string.update_notifications, R.drawable.notification, "update_notifications")
    update(R.string.beta_updates, R.drawable.biotech, "beta_updates", listOf("beta"))
    update(R.string.changelog, R.drawable.history, "changelog", listOf("release notes"))
    update(R.string.commits, R.drawable.commit, "commits", listOf("git"))
    update(R.string.namespace, R.drawable.info, "namespace")
    update(R.string.update_settings_title, R.drawable.settings, "update_settings_title")

    // ── Appearance ──
    fun appearance(titleResId: Int, iconResId: Int, key: String, extra: List<String> = emptyList()) =
        add(SettingsSearchEntry(titleResId, iconResId, R.string.appearance, "settings/appearance", key, extra))

    appearance(R.string.theme, R.drawable.palette, "theme", listOf("dark mode", "light mode", "pure black", "color", "material you"))
    appearance(R.string.enable_dynamic_theme, R.drawable.palette, "enable_dynamic_theme", listOf("dynamic", "material you", "wallpaper"))
    appearance(R.string.enable_dynamic_icon, R.drawable.ic_dynamic_icon, "enable_dynamic_icon", listOf("icon", "app icon"))
    appearance(R.string.enable_high_refresh_rate, R.drawable.speed, "enable_high_refresh_rate", listOf("120hz", "90hz"))
    appearance(R.string.default_open_tab, R.drawable.nav_bar, "default_open_tab", listOf("home", "search", "library","startup"))
    appearance(R.string.default_lib_chips, R.drawable.tab, "default_lib_chips", listOf("library", "chip", "songs", "artists"))
    appearance(R.string.grid_cell_size, R.drawable.grid_view, "grid_cell_size", listOf("grid", "big", "small"))
    appearance(R.string.display_density, R.drawable.grid_view, "display_density", listOf("density", "scale", "zoom"))
    appearance(R.string.slim_navbar, R.drawable.nav_bar, "slim_navbar", listOf("navigation bar", "bottom bar"))
    appearance(R.string.listen_together_in_top_bar, R.drawable.group_outlined, "listen_together_in_top_bar", listOf("top bar"))
    appearance(R.string.new_player_design, R.drawable.palette, "new_player_design", listOf("player design"))
    appearance(R.string.new_mini_player_design, R.drawable.nav_bar, "new_mini_player_design", listOf("mini player"))
    appearance(R.string.pure_black_mini_player, R.drawable.contrast, "pure_black_mini_player", listOf("amoled", "oled"))
    appearance(R.string.hide_player_thumbnail, R.drawable.hide_image, "hide_player_thumbnail", listOf("album art", "cover"))
    appearance(R.string.thumbnail_corner_radius, R.drawable.image, "thumbnail_corner_radius", listOf("corner radius", "rounded"))
    appearance(R.string.crop_album_art, R.drawable.crop, "crop_album_art", listOf("square", "crop"))
    appearance(R.string.vivimusic_canvas, R.drawable.palette, "vivimusic_canvas", listOf("animated", "canvas"))
    appearance(R.string.player_background_style, R.drawable.gradient, "player_background_style", listOf("gradient", "blur", "glow"))
    appearance(R.string.player_buttons_style, R.drawable.palette, "player_buttons_style", listOf("button color"))
    appearance(R.string.player_slider_style, R.drawable.sliders, "player_slider_style", listOf("slider", "wavy", "squiggly"))
    appearance(R.string.enable_swipe_thumbnail, R.drawable.swipe, "enable_swipe_thumbnail", listOf("swipe", "gesture"))
    appearance(R.string.swipe_sensitivity, R.drawable.tune, "swipe_sensitivity", listOf("sensitivity"))
    appearance(R.string.swipe_song_to_add, R.drawable.swipe, "swipe_song_to_add", listOf("swipe", "queue"))
    appearance(R.string.swipe_song_to_remove, R.drawable.swipe, "swipe_song_to_remove", listOf("swipe", "remove"))
    appearance(R.string.lyrics_click_change, R.drawable.lyrics, "lyrics_click_change", listOf("lyrics", "click"))
    appearance(R.string.lyrics_auto_scroll, R.drawable.lyrics, "lyrics_auto_scroll", listOf("lyrics", "scroll"))
    appearance(R.string.lyrics_glow_effect, R.drawable.lyrics, "lyrics_glow_effect", listOf("lyrics", "glow"))
    appearance(R.string.apple_music_lyrics_blur, R.drawable.lyrics, "apple_music_lyrics_blur", listOf("lyrics", "blur", "apple"))
    appearance(R.string.lyrics_animation_style, R.drawable.lyrics, "lyrics_animation_style", listOf("lyrics", "animation", "karaoke"))
    appearance(R.string.lyrics_text_position, R.drawable.lyrics, "lyrics_text_position", listOf("lyrics", "position"))
    appearance(R.string.lyrics_text_size, R.drawable.lyrics, "lyrics_text_size", listOf("lyrics", "font size"))
    appearance(R.string.lyrics_line_spacing, R.drawable.lyrics, "lyrics_line_spacing", listOf("lyrics", "spacing"))
    appearance(R.string.lyrics_swipe_to_change_song, R.drawable.swipe, "lyrics_swipe_to_change_song", listOf("lyrics", "swipe"))
    appearance(R.string.lyrics_thumbnail_play_pause, R.drawable.play, "lyrics_thumbnail_play_pause", listOf("lyrics", "thumbnail"))
    appearance(R.string.show_liked_playlist, R.drawable.favorite, "show_liked_playlist", listOf("liked", "playlist"))
    appearance(R.string.show_downloaded_playlist, R.drawable.offline, "show_downloaded_playlist", listOf("downloaded", "playlist"))
    appearance(R.string.show_top_playlist, R.drawable.trending_up, "show_top_playlist", listOf("top", "playlist"))
    appearance(R.string.show_cached_playlist, R.drawable.cached, "show_cached_playlist", listOf("cached", "playlist"))
    appearance(R.string.show_uploaded_playlist, R.drawable.backup, "show_uploaded_playlist", listOf("uploaded", "playlist"))

    // ── Account ──
    fun account(titleResId: Int, iconResId: Int, key: String, extra: List<String> = emptyList()) =
        add(SettingsSearchEntry(titleResId, iconResId, R.string.account, "settings/account", key, extra))

    account(R.string.account, R.drawable.account, "account", listOf("login", "sign in", "google"))
    account(R.string.yt_sync, R.drawable.cached, "yt_sync", listOf("sync", "youtube"))
    account(R.string.more_content, R.drawable.add_circle, "more_content")
    account(R.string.integrations, R.drawable.integration, "integrations", listOf("discord", "last.fm"))

    // ── Listen Together ──
    fun together(titleResId: Int, iconResId: Int, key: String, extra: List<String> = emptyList()) =
        add(SettingsSearchEntry(titleResId, iconResId, R.string.listen_together, "settings/integrations/listen_together", key, extra))

    together(R.string.listen_together_server_url, R.drawable.cloud, "listen_together_server_url", listOf("server"))
    together(R.string.listen_together_choose_server, R.drawable.cloud, "listen_together_choose_server", listOf("server"))
    together(R.string.listen_together_username, R.drawable.person, "listen_together_username", listOf("username"))
    together(R.string.listen_together_create_room, R.drawable.group, "listen_together_create_room", listOf("create", "host"))
    together(R.string.listen_together_join_room, R.drawable.group, "listen_together_join_room", listOf("join"))
    together(R.string.listen_together_auto_approval, R.drawable.done, "listen_together_auto_approval", listOf("auto approve"))
    together(R.string.listen_together_sync_volume, R.drawable.volume_up, "listen_together_sync_volume", listOf("volume"))
    together(R.string.listen_together_view_logs, R.drawable.bug_report, "listen_together_view_logs", listOf("logs", "debug"))
    together(R.string.listen_together_blocked_users, R.drawable.person, "listen_together_blocked_users", listOf("blocked", "ban"))

    // ── Player & Audio ──
    fun player(titleResId: Int, iconResId: Int, key: String, extra: List<String> = emptyList()) =
        add(SettingsSearchEntry(titleResId, iconResId, R.string.player_and_audio, "settings/player", key, extra))

    player(R.string.audio_quality, R.drawable.graphic_eq, "audio_quality", listOf("quality", "bitrate"))
    player(R.string.crossfade, R.drawable.linear_scale, "crossfade", listOf("transition"))
    player(R.string.crossfade_duration, R.drawable.timer, "crossfade_duration", listOf("seconds"))
    player(R.string.crossfade_gapless, R.drawable.album, "crossfade_gapless", listOf("gapless"))
    player(R.string.history_duration, R.drawable.history, "history_duration", listOf("history"))
    player(R.string.skip_silence, R.drawable.fast_forward, "skip_silence", listOf("quiet"))
    player(R.string.skip_silence_instant, R.drawable.skip_next, "skip_silence_instant", listOf("instant"))
    player(R.string.audio_normalization, R.drawable.volume_up, "audio_normalization", listOf("volume", "loudness"))
    player(R.string.audio_offload, R.drawable.graphic_eq, "audio_offload", listOf("power", "battery"))
    player(R.string.google_cast, R.drawable.cast, "google_cast", listOf("chromecast"))
    player(R.string.seek_seconds_addup, R.drawable.arrow_forward, "seek_seconds_addup", listOf("seek", "forward"))
    player(R.string.persistent_queue, R.drawable.queue_music, "persistent_queue", listOf("queue", "save"))
    player(R.string.auto_load_more, R.drawable.playlist_add, "auto_load_more", listOf("queue", "auto load"))
    player(R.string.disable_load_more_when_repeat_all, R.drawable.repeat, "disable_load_more_when_repeat_all", listOf("repeat"))
    player(R.string.auto_download_on_like, R.drawable.download, "auto_download_on_like", listOf("download", "like"))
    player(R.string.enable_similar_content, R.drawable.similar, "enable_similar_content", listOf("recommendations"))
    player(R.string.persistent_shuffle_title, R.drawable.shuffle, "persistent_shuffle_title", listOf("shuffle"))
    player(R.string.remember_shuffle_and_repeat, R.drawable.shuffle, "remember_shuffle_and_repeat", listOf("remember"))
    player(R.string.shuffle_playlist_first, R.drawable.shuffle, "shuffle_playlist_first")
    player(R.string.prevent_duplicate_tracks_in_queue, R.drawable.queue_music, "prevent_duplicate_tracks_in_queue", listOf("duplicate"))
    player(R.string.auto_skip_next_on_error, R.drawable.skip_next, "auto_skip_next_on_error", listOf("skip", "error"))
    player(R.string.stop_music_on_task_clear, R.drawable.clear_all, "stop_music_on_task_clear", listOf("task"))
    player(R.string.pause_music_when_media_is_muted, R.drawable.volume_off_pause, "pause_music_when_media_is_muted", listOf("mute"))
    player(R.string.resume_on_bluetooth_connect, R.drawable.bluetooth, "resume_on_bluetooth_connect", listOf("bluetooth", "headphones"))
    player(R.string.keep_screen_on_when_player_is_expanded, R.drawable.screenshot, "keep_screen_on_when_player_is_expanded", listOf("screen on"))

    // ── Content ──
    fun content(titleResId: Int, iconResId: Int, key: String, extra: List<String> = emptyList()) =
        add(SettingsSearchEntry(titleResId, iconResId, R.string.content, "settings/content", key, extra))

    content(R.string.content_language, R.drawable.language, "content_language", listOf("language", "locale"))
    content(R.string.content_country, R.drawable.location_on, "content_country", listOf("country", "region"))
    content(R.string.app_language, R.drawable.language, "app_language", listOf("language", "translation"))
    content(R.string.set_quick_picks, R.drawable.home_outlined, "set_quick_picks", listOf("quick picks", "home"))
    content(R.string.set_first_lyrics_provider, R.drawable.lyrics, "set_first_lyrics_provider", listOf("lyrics", "provider"))
    content(R.string.enable_better_lyrics, R.drawable.lyrics, "enable_better_lyrics", listOf("better lyrics", "word by word"))
    content(R.string.enable_kugou, R.drawable.lyrics, "enable_kugou", listOf("kugou"))
    content(R.string.enable_lrclib, R.drawable.lyrics, "enable_lrclib", listOf("lrclib"))
    content(R.string.enable_simpmusic, R.drawable.lyrics, "enable_simpmusic", listOf("simpmusic"))
    content(R.string.enable_proxy, R.drawable.wifi_proxy, "enable_proxy", listOf("proxy", "vpn"))
    content(R.string.config_proxy, R.drawable.settings, "config_proxy", listOf("proxy"))
    content(R.string.hide_explicit, R.drawable.explicit, "hide_explicit", listOf("explicit", "nsfw"))
    content(R.string.hide_video_songs, R.drawable.slow_motion_video, "hide_video_songs", listOf("video"))
    content(R.string.hide_youtube_shorts, R.drawable.hide_image, "hide_youtube_shorts", listOf("shorts", "youtube"))
    content(R.string.lyrics_romanization, R.drawable.language_korean_latin, "lyrics_romanization", listOf("romanization"))
    content(R.string.top_length, R.drawable.trending_up, "top_length", listOf("top", "list length"))
    content(R.string.randomize_home_order, R.drawable.shuffle, "randomize_home_order", listOf("home", "randomize"))
    content(R.string.show_wrapped_card, R.drawable.trending_up, "show_wrapped_card", listOf("wrapped", "recap"))
    content(R.string.show_artist_description, R.drawable.info, "show_artist_description", listOf("artist", "bio"))
    content(R.string.show_artist_subscriber_count, R.drawable.person, "show_artist_subscriber_count", listOf("subscriber"))
    content(R.string.show_artist_monthly_listeners, R.drawable.person, "show_artist_monthly_listeners", listOf("monthly listeners"))

    // ── AI Lyrics Translation ──
    fun ai(titleResId: Int, iconResId: Int, key: String, extra: List<String> = emptyList()) =
        add(SettingsSearchEntry(titleResId, iconResId, R.string.ai_lyrics_translation, "settings/ai", key, extra))

    ai(R.string.ai_provider, R.drawable.explore_outlined, "ai_provider", listOf("openai", "claude", "gemini", "deepl"))
    ai(R.string.ai_base_url, R.drawable.link, "ai_base_url", listOf("url", "endpoint"))
    ai(R.string.ai_api_key, R.drawable.key, "ai_api_key", listOf("api key", "token"))
    ai(R.string.ai_model, R.drawable.discover_tune, "ai_model", listOf("model", "gpt"))
    ai(R.string.ai_translation_mode, R.drawable.translate, "ai_translation_mode", listOf("translation"))
    ai(R.string.ai_target_language, R.drawable.language, "ai_target_language", listOf("target language"))
    ai(R.string.ai_provider_help, R.drawable.info, "ai_provider_help", listOf("help"))
    ai(R.string.ai_deepl_formality, R.drawable.tune, "ai_deepl_formality", listOf("formality"))

    // ── Privacy ──
    fun privacy(titleResId: Int, iconResId: Int, key: String, extra: List<String> = emptyList()) =
        add(SettingsSearchEntry(titleResId, iconResId, R.string.privacy, "settings/privacy", key, extra))

    privacy(R.string.pause_listen_history, R.drawable.history, "pause_listen_history", listOf("listen history", "pause"))
    privacy(R.string.clear_listen_history, R.drawable.delete_history, "clear_listen_history", listOf("listen history", "delete"))
    privacy(R.string.pause_search_history, R.drawable.search_off, "pause_search_history", listOf("search history", "pause"))
    privacy(R.string.clear_search_history, R.drawable.clear_all, "clear_search_history", listOf("search history", "delete"))
    privacy(R.string.disable_screenshot, R.drawable.screenshot, "disable_screenshot", listOf("screenshot", "screen capture"))

    // ── Storage ──
    fun storage(titleResId: Int, iconResId: Int, key: String, extra: List<String> = emptyList()) =
        add(SettingsSearchEntry(titleResId, iconResId, R.string.storage, "settings/storage", key, extra))

    storage(R.string.max_song_cache_size, R.drawable.cached, "max_song_cache_size", listOf("cache", "song", "limit"))
    storage(R.string.max_image_cache_size, R.drawable.manage_search, "max_image_cache_size", listOf("cache", "image", "limit"))
    storage(R.string.clear_song_cache, R.drawable.clear_all, "clear_song_cache", listOf("clear", "cache", "songs"))
    storage(R.string.clear_image_cache, R.drawable.clear_all, "clear_image_cache", listOf("clear", "cache", "images"))
    storage(R.string.clear_all_downloads, R.drawable.clear_all, "clear_all_downloads", listOf("clear", "downloads"))
    storage(R.string.downloaded_songs, R.drawable.storage, "downloaded_songs", listOf("offline"))

    // ── Backup & Restore ──
    fun backup(titleResId: Int, iconResId: Int, key: String, extra: List<String> = emptyList()) =
        add(SettingsSearchEntry(titleResId, iconResId, R.string.backup_restore, "settings/backup_restore", key, extra))

    backup(R.string.action_backup, R.drawable.backup, "action_backup", listOf("backup", "export"))
    backup(R.string.action_restore, R.drawable.restore, "action_restore", listOf("restore", "import"))
    backup(R.string.import_online, R.drawable.language, "import_online", listOf("m3u", "import"))
    backup(R.string.import_csv, R.drawable.language, "import_csv", listOf("csv", "import"))

    // ── About ──
    fun about(titleResId: Int, iconResId: Int, key: String, extra: List<String> = emptyList()) =
        add(SettingsSearchEntry(titleResId, iconResId, R.string.about, "settings/about", key, extra))

    about(R.string.developer_name, R.drawable.dev, "developer_name", listOf("developer", "author"))
    about(R.string.collaborator_tboyke, R.drawable.collab, "collaborator_tboyke")
    about(R.string.github_repository, R.drawable.github, "github_repository", listOf("source code"))
    about(R.string.license, R.drawable.license_vivi, "license", listOf("gpl", "open source"))
    about(R.string.website, R.drawable.web_link, "website")
    about(R.string.telegram_channel, R.drawable.telegram, "telegram_channel", listOf("community"))
    about(R.string.version_code, R.drawable.info, "version_code", listOf("version", "build"))
    about(R.string.installed_date_title, R.drawable.deployed_app_update, "installed_date_title", listOf("install date"))

    // ── Discord Integration ──
    fun discord(titleResId: Int, iconResId: Int, key: String, extra: List<String> = emptyList()) =
        add(SettingsSearchEntry(titleResId, iconResId, R.string.discord, "settings/integrations/discord", key, extra))

    discord(R.string.enable_discord_rpc, R.drawable.discord, "enable_discord_rpc", listOf("discord", "rich presence"))
    discord(R.string.discord_activity_type, R.drawable.discord, "discord_activity_type", listOf("playing", "listening"))
    discord(R.string.discord_activity_name, R.drawable.discord, "discord_activity_name", listOf("activity"))
    discord(R.string.discord_status, R.drawable.discord, "discord_status", listOf("online", "idle", "dnd"))
    discord(R.string.discord_use_details, R.drawable.discord, "discord_use_details", listOf("details"))
    discord(R.string.discord_advanced_mode, R.drawable.discord, "discord_advanced_mode", listOf("advanced"))
    discord(R.string.discord_button_1, R.drawable.discord, "discord_button_1", listOf("button"))
    discord(R.string.discord_button_2, R.drawable.discord, "discord_button_2", listOf("button"))

    // ── Last.fm ──
    fun lastfm(titleResId: Int, iconResId: Int, key: String, extra: List<String> = emptyList()) =
        add(SettingsSearchEntry(titleResId, iconResId, R.string.lastfm_integration, "settings/integrations/lastfm", key, extra))

    lastfm(R.string.enable_scrobbling, R.drawable.music_note, "enable_scrobbling", listOf("scrobble", "last.fm"))
    lastfm(R.string.lastfm_now_playing, R.drawable.music_note, "lastfm_now_playing", listOf("now playing"))
    lastfm(R.string.last_fm_send_likes, R.drawable.music_note, "last_fm_send_likes", listOf("like", "love"))
    lastfm(R.string.scrobble_min_track_duration, R.drawable.music_note, "scrobble_min_track_duration", listOf("duration", "minimum"))
    lastfm(R.string.scrobble_delay_percent, R.drawable.music_note, "scrobble_delay_percent", listOf("delay", "percent"))
    lastfm(R.string.scrobble_delay_minutes, R.drawable.music_note, "scrobble_delay_minutes", listOf("delay", "minutes"))

    // ── Romanization ──
    fun roman(titleResId: Int, iconResId: Int, key: String, extra: List<String> = emptyList()) =
        add(SettingsSearchEntry(titleResId, iconResId, R.string.lyrics_romanize_title, "settings/content/romanization", key, extra))

    roman(R.string.lyrics_romanize_as_main, R.drawable.lyrics, "lyrics_romanize_as_main", listOf("romanize", "main"))
    roman(R.string.lyrics_romanize_japanese, R.drawable.language_japanese_latin, "lyrics_romanize_japanese", listOf("japanese", "romaji"))
    roman(R.string.lyrics_romanize_korean, R.drawable.language_korean_latin, "lyrics_romanize_korean", listOf("korean", "hangul"))
    roman(R.string.lyrics_romanize_chinese, R.drawable.language, "lyrics_romanize_chinese", listOf("chinese", "pinyin"))
    roman(R.string.lyrics_romanize_hindi, R.drawable.language, "lyrics_romanize_hindi", listOf("hindi"))
    roman(R.string.lyrics_romanize_punjabi, R.drawable.language, "lyrics_romanize_punjabi", listOf("punjabi"))
    roman(R.string.lyrics_romanize_russian, R.drawable.alphabet_cyrillic, "lyrics_romanize_russian", listOf("russian", "cyrillic"))
    roman(R.string.lyrics_romanize_ukrainian, R.drawable.alphabet_cyrillic, "lyrics_romanize_ukrainian", listOf("ukrainian"))
    roman(R.string.lyrics_romanize_belarusian, R.drawable.alphabet_cyrillic, "lyrics_romanize_belarusian", listOf("belarusian"))
    roman(R.string.lyrics_romanize_kyrgyz, R.drawable.alphabet_cyrillic, "lyrics_romanize_kyrgyz", listOf("kyrgyz"))
    roman(R.string.lyrics_romanize_serbian, R.drawable.alphabet_cyrillic, "lyrics_romanize_serbian", listOf("serbian"))
    roman(R.string.lyrics_romanize_bulgarian, R.drawable.alphabet_cyrillic, "lyrics_romanize_bulgarian", listOf("bulgarian"))
    roman(R.string.lyrics_romanize_macedonian, R.drawable.alphabet_cyrillic, "lyrics_romanize_macedonian", listOf("macedonian"))
    roman(R.string.line_by_line_option_title, R.drawable.warning, "line_by_line_option_title", listOf("detect language"))
}
