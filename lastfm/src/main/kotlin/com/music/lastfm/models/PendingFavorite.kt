/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.lastfm.models

import kotlinx.serialization.Serializable

@Serializable
data class PendingFavorite(
    val artist: String,
    val track: String,
    val isFavorite: Boolean
)
