package com.music.vivi.ui.screens.settings.import_from_spotify.model

sealed class ImportProgressEvent {
    data class LikedSongsProgress(val completed: Boolean, val currentCount: Int,val totalTracksCount: Int) :
        ImportProgressEvent()

    data class PlaylistsProgress(
        val completed: Boolean,
        val playlistName: String,
        val progressedTrackCount: Int,
        val totalTracksCount: Int,
        val currentPlaylistIndex: Int
    ) : ImportProgressEvent()
}