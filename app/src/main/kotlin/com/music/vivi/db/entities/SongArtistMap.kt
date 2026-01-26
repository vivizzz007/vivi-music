package com.music.vivi.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "song_artist_map",
    primaryKeys = ["songId", "artistId"],
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = ["id"],
            childColumns = ["artistId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
/**
 * Map connecting Songs to Artists.
 *
 * @property songId ID of the song.
 * @property artistId ID of the artist.
 * @property position Order of the artist (e.g., Main artist vs Featured artist).
 */
data class SongArtistMap(
    @ColumnInfo(index = true) val songId: String,
    @ColumnInfo(index = true) val artistId: String,
    val position: Int,
)
