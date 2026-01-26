package com.music.vivi.db.entities

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

/**
 * Database View for accessing album songs sorted by their track index.
 */
@DatabaseView(
    viewName = "sorted_song_album_map",
    value = "SELECT * FROM song_album_map ORDER BY `index`"
)
data class SortedSongAlbumMap(
    @ColumnInfo(index = true) val songId: String,
    @ColumnInfo(index = true) val albumId: String,
    val index: Int,
)
