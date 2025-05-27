package com.music.vivi.ui.screens.settings.import_from_spotify.model

import com.music.vivi.models.spotify.playlists.SpotifyPlaylistItem

data class ImportFromSpotifyScreenState(
    val isRequesting: Boolean,
    val accessToken: String,
    val error: Boolean,
    val exception: Exception?,
    val userName:String,
    val isObtainingAccessTokenSuccessful:Boolean,
    val playlists:List<SpotifyPlaylistItem>,
    val totalPlaylistsCount:Int,
    val reachedEndForPlaylistPagination:Boolean
)