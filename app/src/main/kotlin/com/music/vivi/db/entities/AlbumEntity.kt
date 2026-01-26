package com.music.vivi.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.music.innertube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Represents an Album stored in the local database.
 *
 * @property id Unique identifier (YouTube Browse ID or generated local ID).
 * @property playlistId Optional ID if the album acts as a playlist.
 * @property title Album title.
 * @property year Release year.
 * @property thumbnailUrl Cover image URL.
 * @property themeColor Extracted theme color.
 * @property songCount Number of songs.
 * @property duration Total duration in seconds.
 * @property explicit Whether it contains explicit content.
 * @property lastUpdateTime Timestamp of last update.
 * @property bookmarkedAt Timestamp when the user liked/bookmarked the album.
 * @property likedDate Timestamp of like (redundant with bookmarkedAt in some contexts, specific to synced likes).
 * @property inLibrary Timestamp if added to library.
 * @property isLocal Whether this album is from local device storage.
 * @property isUploaded Whether this album is an upload.
 * @property description Album description.
 */
@Immutable
@Entity(tableName = "album")
data class AlbumEntity(
    @PrimaryKey val id: String,
    val playlistId: String? = null,
    val title: String,
    val year: Int? = null,
    val thumbnailUrl: String? = null,
    val themeColor: Int? = null,
    val songCount: Int,
    val duration: Int,
    @ColumnInfo(defaultValue = "0")
    val explicit: Boolean = false,
    val lastUpdateTime: LocalDateTime = LocalDateTime.now(),
    val bookmarkedAt: LocalDateTime? = null,
    val likedDate: LocalDateTime? = null,
    val inLibrary: LocalDateTime? = null,
    @ColumnInfo(name = "isLocal", defaultValue = false.toString())
    val isLocal: Boolean = false,
    @ColumnInfo(name = "isUploaded", defaultValue = false.toString())
    val isUploaded: Boolean = false,
    val description: String? = null,
) {
    fun localToggleLike() = copy(
        bookmarkedAt = if (bookmarkedAt != null) null else LocalDateTime.now()
    )

    fun toggleUploaded() = copy(
        isUploaded = !isUploaded
    )

    fun toggleLike() = localToggleLike().also {
        CoroutineScope(Dispatchers.IO).launch {
            if (playlistId != null) {
                YouTube.likePlaylist(playlistId, bookmarkedAt == null)
            }
        }
    }
}
