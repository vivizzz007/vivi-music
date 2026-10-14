/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.unison.models

import kotlinx.serialization.Serializable

@Serializable
data class LyricsResponse(
    val success: Boolean,
    val data: LyricsData? = null,
    val error: String? = null,
)
