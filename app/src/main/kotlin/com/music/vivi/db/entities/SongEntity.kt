package com.music.vivi.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.music.innertube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Represents a Song stored in the local database.
 * The central entity for all music media.
 *
 * @property id Unique identifier (YouTube Video ID).
 * @property title Song title.
 * @property duration Duration in seconds.
 * @property thumbnailUrl Thumbnail URL.
 * @property albumId Associated Album ID.
 * @property albumName Denormalized Album Name (for quick access).
 * @property explicit Explicit content flag.
 * @property year Release year.
 * @property date ID3 tag date.
 * @property dateModified File modification date.
 * @property liked User's like status.
 * @property likedDate Timestamp of like.
 * @property totalPlayTime Total milliseconds played.
 * @property inLibrary Timestamp if added to library.
 * @property dateDownload Timestamp when downloaded.
 * @property isLocal Whether the file is local.
 * @property libraryAddToken YouTube library add token.
 * @property libraryRemoveToken YouTube library remove token.
 * @property lyricsOffset Offset for synced lyrics.
 * @property romanizeLyrics Preference for lyrics romanization.
 * @property isDownloaded Whether the song is downloaded (cached).
 * @property isUploaded Whether it's an uploaded song.
 * @property isVideo Whether this is a music video.
 */
@Immutable
@Entity(
    tableName = "song",
    indices = [
        Index(
            value = ["albumId"]
        )
    ]
)
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val duration: Int = -1, // in seconds
    val thumbnailUrl: String? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    @ColumnInfo(defaultValue = "0")
    val explicit: Boolean = false,
    val year: Int? = null,
    val date: LocalDateTime? = null, // ID3 tag property
    val dateModified: LocalDateTime? = null, // file property
    val liked: Boolean = false,
    val likedDate: LocalDateTime? = null,
    val totalPlayTime: Long = 0, // in milliseconds
    val inLibrary: LocalDateTime? = null,
    val dateDownload: LocalDateTime? = null,
    @ColumnInfo(name = "isLocal")
    val isLocal: Boolean = false,
    val libraryAddToken: String? = null,
    val libraryRemoveToken: String? = null,
    @ColumnInfo(defaultValue = "0")
    val lyricsOffset: Int = 0,
    @ColumnInfo(defaultValue = true.toString())
    val romanizeLyrics: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val isDownloaded: Boolean = false,
    @ColumnInfo(name = "isUploaded", defaultValue = false.toString())
    val isUploaded: Boolean = false,
    @ColumnInfo(name = "isVideo", defaultValue = false.toString())
    val isVideo: Boolean = false,
) {
    fun localToggleLike() = copy(
        liked = !liked,
        likedDate = if (!liked) LocalDateTime.now() else null
    )

    fun toggleLike() = copy(
        liked = !liked,
        likedDate = if (!liked) LocalDateTime.now() else null,
        inLibrary = if (!liked) inLibrary ?: LocalDateTime.now() else inLibrary
    ).also {
        CoroutineScope(Dispatchers.IO).launch {
            YouTube.likeVideo(id, !liked)
        }
    }

    fun toggleLibrary() = copy(
        liked = if (inLibrary == null) liked else false,
        inLibrary = if (inLibrary == null) LocalDateTime.now() else null,
        likedDate = if (inLibrary == null) likedDate else null
    )

    fun toggleUploaded() = copy(
        isUploaded = !isUploaded
    )
}
