package com.music.vivi.db.entities

import androidx.compose.runtime.Immutable

@Immutable
/**
 * A projection of Song data with additional statistics.
 *
 * @property id Song ID.
 * @property title Song Title.
 * @property thumbnailUrl Thumbnail URL.
 * @property songCountListened Number of times listened.
 * @property timeListened Total duration listened.
 */
data class SongWithStats(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val songCountListened: Int,
    val timeListened: Long?,
)
