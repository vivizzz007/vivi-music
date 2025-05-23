package com.music.vivi.models.spotify.playlists

import com.music.vivi.models.spotify.Tracks
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyPlaylistItem(
    @SerialName("description")
    var playlistDescription: String,
    @SerialName("id")
    var playlistId: String,
    var images: List<Images> = emptyList(),
    @SerialName("name")
    var playlistName: String,
    var tracks: Tracks? = Tracks(0),
    var type: String,
    var uri: String
)