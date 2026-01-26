package com.music.vivi.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * Mapping table relating Albums to Artists.
 * Stores the relationship and ordering of artists on an album.
 *
 * @property albumId ID of the album.
 * @property artistId ID of the artist.
 * @property order The order of the artist (e.g. Primary vs Featured).
 */
@Entity(
    tableName = "album_artist_map",
    primaryKeys = ["albumId", "artistId"],
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
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
data class AlbumArtistMap(
    @ColumnInfo(index = true) val albumId: String,
    @ColumnInfo(index = true) val artistId: String,
    val order: Int,
)
