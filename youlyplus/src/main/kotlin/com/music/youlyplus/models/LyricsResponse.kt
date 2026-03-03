package com.music.youlyplus.models

import kotlinx.serialization.Serializable

/**
 * Response model for the LyricsPlus / KPoe API (/v2/lyrics/get).
 * Fields mirror what YouLyPlus extension parses from the backend.
 */
@Serializable
data class LyricsResponse(
    // LRCLib style
    val id: Int? = null,
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null,

    // KPoe style (array of lines)
    val lyrics: List<LyricsItem>? = null,
    val type: String? = null,

    // Common metadata
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
)

@Serializable
data class LyricsItem(
    val text: String? = null,
    val time: Long? = null,         // milliseconds
    val duration: Long? = null,     // milliseconds
    val syllabus: List<Syllable>? = null,
)

@Serializable
data class Syllable(
    val text: String? = null,
    val time: Long? = null,         // milliseconds
    val duration: Long? = null,     // milliseconds
    val isBackground: Boolean? = null,
)


