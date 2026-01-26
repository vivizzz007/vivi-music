package com.music.vivi.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents Cached Audio Format information.
 * Stores technical details about the audio stream associated with a song.
 *
 * @property id Song ID.
 * @property itag YouTube ITAG format identifier.
 * @property mimeType Audio mime type.
 * @property codecs Audio codecs.
 * @property bitrate Bitrate in bps.
 * @property sampleRate Sample rate in Hz.
 * @property contentLength Content length in bytes.
 * @property loudnessDb Loudness normalization value.
 * @property perceptualLoudnessDb Perceptual loudness value.
 * @property playbackUrl Deprecated URL field.
 */
@Entity(tableName = "format")
data class FormatEntity(
    @PrimaryKey val id: String,
    val itag: Int,
    val mimeType: String,
    val codecs: String,
    val bitrate: Int,
    val sampleRate: Int?,
    val contentLength: Long,
    val loudnessDb: Double?,
    val perceptualLoudnessDb: Double? = null,
    @Deprecated("playbackTrackingUrl should be retrieved from a fresh player request")
    val playbackUrl: String?,
)
