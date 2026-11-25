/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.lyrics

import com.music.vivi.constants.PreferredLyricsProvider

/**
 * Central registry for all lyrics providers.
 * Maps provider names (used for persistence) to provider objects,
 * and handles serialization/deserialization of the custom priority order.
 */
object LyricsProviderRegistry {
    private val providerMap = mapOf(
        "LrcRed"          to LrcRedLyricsProvider,
        "LrcCx"           to LrcCxLyricsProvider,
        "LrcLib"          to LrcLibLyricsProvider,
        "BetterLyrics"    to BetterLyricsProvider,
        "Kugou"           to KuGouLyricsProvider,
        "Unison"          to UnisonLyricsProvider,
        "BiniLyrics"      to BiniLyricsProvider,
        "Spotify"         to SpotifyLyricsProvider,
        "Paxsenix"        to PaxSenixLyricsProvider,
        "BetterLyricsPortato" to BetterLyricsPortatoLyricsProvider,
        "YouTubeSubtitle" to YouTubeSubtitleLyricsProvider,
        "YouTubeMusic"    to YouTubeLyricsProvider,
        "Genius"          to GeniusLyricsProvider,
        "YouLyPlus"       to YouLyPlusLyricsProvider,
    )

    val providerNames = providerMap.keys.toList()

    fun getProviderByName(name: String): LyricsProvider? = providerMap[name]

    fun deserializeProviderOrder(orderString: String): List<String> {
        if (orderString.isBlank()) return getDefaultProviderOrder()
        return orderString.split(",").map { it.trim() }.filter { it in providerNames }
    }

    fun serializeProviderOrder(providers: List<String>): String =
        providers.filter { it in providerNames }.joinToString(",")

    fun getDefaultProviderOrder(): List<String> = listOf(
        "LrcRed",
        "LrcCx",
        "LrcLib",
        "BetterLyrics",
        "Kugou",
        "Unison",
        "BiniLyrics",
        "Spotify",
        "Paxsenix",
        "BetterLyricsPortato",
        "YouTubeSubtitle",
        "YouTubeMusic",
        "Genius",
        "YouLyPlus",
    )

    fun getOrderedProviders(orderString: String): List<LyricsProvider> =
        deserializeProviderOrder(orderString).mapNotNull { getProviderByName(it) }

    /** Maps a [PreferredLyricsProvider] enum value to its registry name, used for migration. */
    fun getProviderNameForEnum(enum: PreferredLyricsProvider): String = when (enum) {
        PreferredLyricsProvider.LRCLIB           -> "LrcLib"
        PreferredLyricsProvider.KUGOU            -> "Kugou"
        PreferredLyricsProvider.BETTER_LYRICS    -> "BetterLyrics"
        PreferredLyricsProvider.YOULYPLUS        -> "YouLyPlus"
        PreferredLyricsProvider.PAXSENIX         -> "Paxsenix"
        PreferredLyricsProvider.UNISON           -> "Unison"
        PreferredLyricsProvider.SPOTIFY          -> "Spotify"
        PreferredLyricsProvider.BINI_LYRICS      -> "BiniLyrics"
        PreferredLyricsProvider.LRC_RED          -> "LrcRed"
        PreferredLyricsProvider.YOUTUBE_SUBTITLE -> "YouTubeSubtitle"
        PreferredLyricsProvider.YOUTUBE_MUSIC    -> "YouTubeMusic"
    }

    /** Returns the human-readable display name for a registry provider key. */
    fun getDisplayName(name: String): String = when (name) {
        "YouLyPlus"       -> "YouLyPlus"
        "Paxsenix"        -> "PaxSenix"
        "BetterLyrics"    -> "Better Lyrics"
        "LrcLib"          -> "LrcLib"
        "Kugou"           -> "KuGou"
        "Unison"          -> "Unison"
        "BiniLyrics"      -> "Bini Lyrics"
        "Spotify"         -> "Spotify"
        "LrcRed"          -> "lrc.red"
        "LrcCx"           -> "LrcAPI"
        "YouTubeSubtitle" -> "YouTube Subtitle"
        "YouTubeMusic"    -> "YouTube Music"
        "BetterLyricsPortato" -> "Better Lyrics (Portato)"
        "Genius"          -> "Genius"
        else              -> name
    }
}
