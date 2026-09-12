package com.music.unison.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LyricsData(
    val id: Long,
    @SerialName("videoId")
    val videoId: String? = null,
    val song: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val lyrics: String? = null,
    val format: String? = null,
    val language: String? = null,
    @SerialName("syncType")
    val syncType: String? = null,
    val score: Double? = null,
    @SerialName("effectiveScore")
    val effectiveScore: Double? = null,
    @SerialName("voteCount")
    val voteCount: Int? = null,
    val confidence: String? = null,
    val duration: Double? = null,
)
