package com.music.lastfm.models

import kotlinx.serialization.Serializable

@Serializable
data class PendingFavorite(
    val artist: String,
    val track: String,
    val isFavorite: Boolean
)
