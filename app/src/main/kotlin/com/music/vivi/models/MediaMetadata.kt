package com.music.vivi.models

import androidx.compose.runtime.Immutable
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import com.music.vivi.db.entities.Song
import com.music.vivi.db.entities.SongEntity
import com.music.vivi.ui.utils.resize
import java.io.Serializable
import java.time.LocalDateTime

/**
 * A Serializable representation of media metadata, used for persistence and passing between components.
 * Similar to Media3's [androidx.media3.common.MediaMetadata] but tailored for our app's needs (persistence, custom fields).
 *
 * @property id Unique identifier of the media (Song ID).
 * @property title Title of the song.
 * @property artists List of artists associated with the song.
 * @property duration Duration of the song in seconds.
 * @property thumbnailUrl URL of the album art or thumbnail.
 * @property album Album information, if available.
 * @property setVideoId The ID of the associated music video, if any.
 * @property musicVideoType type of the music video (e.g., "ATV" implies Audio-only or specific type).
 * @property explicit Whether the song is marked as explicit.
 * @property liked Whether the song is liked by the user.
 * @property likedDate Timestamp when the song was liked.
 * @property inLibrary Timestamp when the song was added to the library.
 * @property libraryAddToken Token used for adding to library (YouTube specific).
 * @property libraryRemoveToken Token used for removing from library (YouTube specific).
 */
@Immutable
data class MediaMetadata(
    val id: String,
    val title: String,
    val artists: List<Artist>,
    val duration: Int,
    val thumbnailUrl: String? = null,
    val album: Album? = null,
    val setVideoId: String? = null,
    val musicVideoType: String? = null,
    val explicit: Boolean = false,
    val liked: Boolean = false,
    val likedDate: LocalDateTime? = null,
    val inLibrary: LocalDateTime? = null,
    val libraryAddToken: String? = null,
    val libraryRemoveToken: String? = null,
) : Serializable {
    /**
     * Checks if this metadata represents a video song (i.e., has a video type that is NOT just an Audio track).
     */
    val isVideoSong: Boolean
        get() = musicVideoType != null && musicVideoType != MUSIC_VIDEO_TYPE_ATV

    /**
     * Represents a simplified Artist model for Metadata.
     */
    data class Artist(val id: String?, val name: String) : Serializable

    /**
     * Represents a simplified Album model for Metadata.
     */
    data class Album(val id: String, val title: String) : Serializable

    fun toSongEntity() = SongEntity(
        id = id,
        title = title,
        duration = duration,
        thumbnailUrl = thumbnailUrl,
        albumId = album?.id,
        albumName = album?.title,
        explicit = explicit,
        liked = liked,
        likedDate = likedDate,
        inLibrary = inLibrary,
        libraryAddToken = libraryAddToken,
        libraryRemoveToken = libraryRemoveToken
    )
}

fun Song.toMediaMetadata() = MediaMetadata(
    id = song.id,
    title = song.title,
    artists =
    artists.map {
        MediaMetadata.Artist(
            id = it.id,
            name = it.name
        )
    },
    duration = song.duration,
    thumbnailUrl = song.thumbnailUrl,
    album =
    album?.let {
        MediaMetadata.Album(
            id = it.id,
            title = it.title
        )
    } ?: song.albumId?.let { albumId ->
        MediaMetadata.Album(
            id = albumId,
            title = song.albumName.orEmpty()

        )
    },
    musicVideoType = null
)

fun SongItem.toMediaMetadata() = MediaMetadata(
    id = id,
    title = title,
    artists =
    artists.map {
        MediaMetadata.Artist(
            id = it.id,
            name = it.name
        )
    },
    duration = duration ?: -1,
    thumbnailUrl = thumbnail.resize(544, 544),
    album =
    album?.let {
        MediaMetadata.Album(
            id = it.id,
            title = it.name
        )
    },
    explicit = explicit,
    setVideoId = setVideoId,
    musicVideoType = musicVideoType,
    libraryAddToken = libraryAddToken,
    libraryRemoveToken = libraryRemoveToken
)

fun SongEntity.toMediaMetadata() = MediaMetadata(
    id = id,
    title = title,
    artists = emptyList(),
    duration = duration,
    thumbnailUrl = thumbnailUrl,
    album = if (albumId != null) MediaMetadata.Album(albumId, albumName.orEmpty()) else null,
    explicit = explicit,
    liked = liked,
    likedDate = likedDate,
    inLibrary = inLibrary,
    libraryAddToken = libraryAddToken,
    libraryRemoveToken = libraryRemoveToken
)
