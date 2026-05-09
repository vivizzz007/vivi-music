/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.search.suggestions

data class SuggestionTrack(
    val rank: Int,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val appleMusicUrl: String? = null
)

data class SuggestionArtist(
    val rank: Int,
    val name: String,
    val thumbnailUrl: String?
)

data class SuggestionAlbum(
    val rank: Int,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val appleMusicUrl: String? = null
)
