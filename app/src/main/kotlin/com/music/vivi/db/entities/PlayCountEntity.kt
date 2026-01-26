package com.music.vivi.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity

/**
 * Tracks the number of plays for a song, aggregated by month/year.
 * Used for "Most Played" and time-based statistics.
 *
 * @property song The Song ID.
 * @property year The year of the play count (e.g., 2026).
 * @property month The month of the play count (1-12).
 * @property count The number of plays in that specific month/year.
 */
@Immutable
@Entity(
    tableName = "playCount",
    primaryKeys = ["song", "year", "month"]
)
class PlayCountEntity(
    val song: String, // song id
    val year: Int = -1,
    val month: Int = -1,
    val count: Int = -1,
)
