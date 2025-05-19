package com.music.vivi.models.spotify.tracks

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Track(
    @SerialName("track")
    val trackItem: TrackItem
)