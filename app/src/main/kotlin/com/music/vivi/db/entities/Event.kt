package com.music.vivi.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Represents a listening event (History).
 * Records when a user listened to a song and for how long.
 *
 * @property id Auto-generated ID.
 * @property songId ID of the song listened to.
 * @property timestamp When the event occurred.
 * @property playTime How long the song was played (Play duration, not song duration).
 */
@Immutable
@Entity(
    tableName = "event",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val songId: String,
    val timestamp: LocalDateTime,
    val playTime: Long,
)
