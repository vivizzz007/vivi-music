package com.music.vivi.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Represents a complete Playlist with thumbnail previews.
 *
 * @property playlist The core Playlist entity.
 * @property songCount Total number of songs in the playlist.
 * @property songThumbnails List of thumbnails from the first few songs (used for playlist cover grid).
 */
@Immutable
data class Playlist(
    @Embedded
    val playlist: PlaylistEntity,
    val songCount: Int,
    @Relation(
        entity = SongEntity::class,
        entityColumn = "id",
        parentColumn = "id",
        projection = ["thumbnailUrl"],
        associateBy =
        Junction(
            value = PlaylistSongMapPreview::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val songThumbnails: List<String?>,
) : LocalItem() {
    override val id: String
        get() = playlist.id
    override val title: String
        get() = playlist.name
    override val thumbnailUrl: String?
        get() = null

    val thumbnails: List<String>
        get() {
            return if (playlist.thumbnailUrl != null) {
                listOf(playlist.thumbnailUrl)
            } else {
                songThumbnails.filterNotNull()
            }
        }
}
