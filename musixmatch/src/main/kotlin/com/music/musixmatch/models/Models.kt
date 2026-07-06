/**
 * VIVI-LYRICS (C) 2026
 *
 * PROPRIETARY LICENSE:
 * This file is source-available for viewing. Copying, modification,
 * redistribution, or reuse in other applications is strictly prohibited.
 * Licensed exclusively for use in the official vivimusic application.
 */

package com.music.musixmatch.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MessageHeader(
    @SerialName("status_code") val statusCode: Int
)

@Serializable
data class TokenResponseBody(
    @SerialName("user_token") val userToken: String? = null
)

@Serializable
data class TokenResponseContainer(
    val header: MessageHeader,
    val body: JsonElement? = null
)

@Serializable
data class TokenResponse(
    val message: TokenResponseContainer
)

@Serializable
data class Track(
    @SerialName("track_id") val trackId: Long,
    @SerialName("track_name") val trackName: String,
    @SerialName("artist_name") val artistName: String,
    @SerialName("track_length") val trackLength: Int? = null,
    @SerialName("track_isrc") val trackIsrc: String? = null
)

@Serializable
data class TrackContainer(
    val track: Track
)

@Serializable
data class SearchTrackResponseBody(
    @SerialName("track_list") val trackList: List<TrackContainer> = emptyList()
)

@Serializable
data class SearchTrackResponseContainer(
    val header: MessageHeader,
    val body: JsonElement? = null
)

@Serializable
data class SearchTrackResponse(
    val message: SearchTrackResponseContainer
)

@Serializable
data class Lyrics(
    @SerialName("lyrics_body") val lyricsBody: String? = null,
    @SerialName("has_richsync") val hasRichSync: Int? = null
)

@Serializable
data class TrackLyricsResponseBody(
    val lyrics: Lyrics? = null
)

@Serializable
data class TrackLyricsResponseContainer(
    val header: MessageHeader,
    val body: JsonElement? = null
)

@Serializable
data class TrackLyricsResponse(
    val message: TrackLyricsResponseContainer
)

@Serializable
data class RichSync(
    @SerialName("richsync_body") val richsyncBody: String? = null
)

@Serializable
data class RichSyncResponseBody(
    val richsync: RichSync? = null
)

@Serializable
data class RichSyncResponseContainer(
    val header: MessageHeader,
    val body: JsonElement? = null
)

@Serializable
data class RichSyncResponse(
    val message: RichSyncResponseContainer
)

@Serializable
data class RichSyncEntry(
    val ts: Double, // start time in seconds
    val te: Double, // end time in seconds
    val l: List<WordEntry> = emptyList(), // word offsets
    val x: String // line text
)

@Serializable
data class WordEntry(
    val c: String, // characters/word
    val o: Double // offset in seconds from line start
)

@Serializable
data class Subtitle(
    @SerialName("subtitle_body") val subtitleBody: String? = null
)

@Serializable
data class TrackSubtitleResponseBody(
    val subtitle: Subtitle? = null
)

@Serializable
data class TrackSubtitleResponseContainer(
    val header: MessageHeader,
    val body: JsonElement? = null
)

@Serializable
data class TrackSubtitleResponse(
    val message: TrackSubtitleResponseContainer
)
