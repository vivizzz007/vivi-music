package com.music.vivi.models.spotify

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Tracks(
    @SerialName("total")
    val totalTracksCount: Int
)