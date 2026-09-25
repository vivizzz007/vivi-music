package com.music.lastfm.models

import kotlinx.serialization.Serializable

@Serializable
data class PendingScrobble(
    val artist: String,
    val track: String,
    val timestamp: Long,
    val album: String? = null,
    val trackNumber: Int? = null,
    val duration: Int? = null
)
