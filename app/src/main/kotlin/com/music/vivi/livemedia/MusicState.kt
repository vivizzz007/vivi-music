package com.music.vivi.livemedia

import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.PlaybackState

data class MusicState(
    val title: String,
    val artist: String,
    val albumArt: Bitmap?,
    val isPlaying: Boolean,
    val duration: Long,
    val position: Long,
    val packageName: String,
    val mediaSessionActive: Boolean,
    val albumName: String
) {
    constructor(
        metadata: MediaMetadata,
        playbackState: PlaybackState,
        packageName: String
    ) : this(
        title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title",
        artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist",
        albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART),
        isPlaying = playbackState.state == PlaybackState.STATE_PLAYING,
        duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0L),
        position = playbackState.position.coerceAtLeast(0L),
        packageName = packageName,
        mediaSessionActive = true,
        albumName = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "Unknown Album",
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (javaClass != other?.javaClass) return false

        other as MusicState

        if (title != other.title) return false
        if (artist != other.artist) return false
        if (packageName != other.packageName) return false
        if (albumArt != other.albumArt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + packageName.hashCode()
        return result
    }

    companion object {
        const val EMPTY_ALBUM = "Unknown Album"
        const val EMPTY_ARTIST = "Unknown Artist"
    }
}
