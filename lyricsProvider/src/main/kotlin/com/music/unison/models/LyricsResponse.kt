package com.music.unison.models

import kotlinx.serialization.Serializable

@Serializable
data class LyricsResponse(
    val success: Boolean,
    val data: LyricsData? = null,
    val error: String? = null,
)
