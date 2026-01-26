package com.music.vivi.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded

/**
 * Represents a complete Artist with statistics.
 *
 * @property artist The core Artist entity.
 * @property songCount Number of songs by this artist in the database.
 * @property timeListened Total time listened to this artist.
 */
@Immutable
data class Artist(
    @Embedded
    val artist: ArtistEntity,
    val songCount: Int,
    val timeListened: Int? = 0,
) : LocalItem() {
    override val id: String
        get() = artist.id
    override val title: String
        get() = artist.name
    override val thumbnailUrl: String?
        get() = artist.thumbnailUrl
}
