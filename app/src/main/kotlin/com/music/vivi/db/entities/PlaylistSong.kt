package com.music.vivi.db.entities

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Represents a Song entry within a Playlist.
 * Combines the mapping info (position) with the actual Song entity.
 */
data class PlaylistSong(
    @Embedded val map: PlaylistSongMap,
    @Relation(
        parentColumn = "songId",
        entityColumn = "id",
        entity = SongEntity::class
    )
    val song: Song,
)
