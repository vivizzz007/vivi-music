package com.music.vivi.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Mapping table relating Songs to Playlists.
 * Handles the ordering of songs within a playlist.
 *
 * @property id Auto-incrementing ID.
 * @property playlistId ID of the playlist.
 * @property songId ID of the song.
 * @property position The order/index of the song in the playlist.
 * @property setVideoId Associated video set ID if applicable.
 */
@Entity(
    tableName = "playlist_song_map",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistSongMap(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(index = true) val playlistId: String,
    @ColumnInfo(index = true) val songId: String,
    val position: Int = 0,
    val setVideoId: String? = null,
)
