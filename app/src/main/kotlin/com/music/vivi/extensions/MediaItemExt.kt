package com.music.vivi.extensions

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import com.music.innertube.models.SongItem
import com.music.vivi.db.entities.Song
import com.music.vivi.db.entities.SongEntity
import com.music.vivi.models.MediaMetadata
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.ui.utils.resize

/**
 * Helper to extract [MediaMetadata] from a [MediaItem].
 * Expects the tag property to hold the metadata object.
 */
val MediaItem.metadata: MediaMetadata?
    get() = localConfiguration?.tag as? MediaMetadata

/**
 * Converts a [Song] persistence entity to a Media3 [MediaItem].
 * Populates standard metadata fields (Title, Artist, Album, Artwork URI).
 */
fun Song.toMediaItem() = MediaItem
    .Builder()
    .setMediaId(song.id)
    .setUri(song.id)
    .setCustomCacheKey(song.id)
    .setTag(toMediaMetadata())
    .setMediaMetadata(
        androidx.media3.common.MediaMetadata
            .Builder()
            .setTitle(song.title)
            .setSubtitle(artists.joinToString { it.name })
            .setArtist(artists.joinToString { it.name })
            .setArtworkUri(song.thumbnailUrl?.toUri())
            .setAlbumTitle(song.albumName)
            .setMediaType(MEDIA_TYPE_MUSIC)
            .build()
    ).build()

/**
 * Converts a [SongItem] (from InnerTube/Network) to a Media3 [MediaItem].
 * Resizes artwork to 544x544 for better quality.
 */
fun SongItem.toMediaItem() = MediaItem
    .Builder()
    .setMediaId(id)
    .setUri(id)
    .setCustomCacheKey(id)
    .setTag(toMediaMetadata())
    .setMediaMetadata(
        androidx.media3.common.MediaMetadata
            .Builder()
            .setTitle(title)
            .setSubtitle(artists.joinToString { it.name })
            .setArtist(artists.joinToString { it.name })
            .setArtworkUri(thumbnail.resize(544, 544).toUri())
            .setAlbumTitle(album?.name)
            .setMediaType(MEDIA_TYPE_MUSIC)
            .build()
    ).build()

/**
 * Converts a [MediaMetadata] domain object to a Media3 [MediaItem].
 */
fun MediaMetadata.toMediaItem() = MediaItem
    .Builder()
    .setMediaId(id)
    .setUri(id)
    .setCustomCacheKey(id)
    .setTag(this)
    .setMediaMetadata(
        androidx.media3.common.MediaMetadata
            .Builder()
            .setTitle(title)
            .setSubtitle(artists.joinToString { it.name })
            .setArtist(artists.joinToString { it.name })
            .setArtworkUri(thumbnailUrl?.toUri())
            .setAlbumTitle(album?.title)
            .setMediaType(MEDIA_TYPE_MUSIC)
            .build()
    ).build()

fun SongEntity.toMediaItem() = toMediaMetadata().toMediaItem()
