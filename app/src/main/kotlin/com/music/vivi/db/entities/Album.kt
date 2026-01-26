package com.music.vivi.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.music.vivi.db.entities.SongEntity

/**
 * Represents a complete Album with its relationships.
 * Fetches the AlbumEntity along with associated Artists and Songs.
 *
 * @property album The core Album entity data.
 * @property artists List of artists associated with this album (via [AlbumArtistMap]).
 * @property songs List of songs in this album (via [SortedSongAlbumMap]).
 * @property songCountListened Playback statistics (number of times songs from this album were listened to).
 * @property timeListened Playback statistics (total time listened).
 */
@Immutable
data class Album(
    @Embedded
    val album: AlbumEntity,
    @Relation(
        entity = ArtistEntity::class,
        entityColumn = "id",
        parentColumn = "id",
        associateBy =
        Junction(
            value = AlbumArtistMap::class,
            parentColumn = "albumId",
            entityColumn = "artistId"
        )
    )
    val artists: List<ArtistEntity> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "albumId"
    )
    val songs: List<SongEntity> = emptyList(),
    val songCountListened: Int? = 0,
    val timeListened: Long? = 0,
) : LocalItem() {
    override val id: String
        get() = album.id
    override val title: String
        get() = album.title
    override val thumbnailUrl: String?
        get() = album.thumbnailUrl
}
