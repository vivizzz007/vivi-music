/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.db.MusicDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class DetailedListeningHistoryViewModel @Inject constructor(
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val startTimestamp: Long = savedStateHandle.get<String>("startTimestamp")?.toLongOrNull() ?: 0L
    private val endTimestamp: Long = Instant.ofEpochMilli(startTimestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .plusDays(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val totalPlayTime = database.getTotalPlayTimeInRange(startTimestamp, endTimestamp)
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val uniqueSongsCount = database.getUniqueSongCountInRange(startTimestamp, endTimestamp)
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val uniqueArtistsCount = database.getUniqueArtistCountInRange(startTimestamp, endTimestamp)
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val uniqueAlbumsCount = database.getUniqueAlbumCountInRange(startTimestamp, endTimestamp)
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val mostPlayedSongs = database.mostPlayedSongsStats(fromTimeStamp = startTimestamp, toTimeStamp = endTimestamp, limit = -1)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedArtists = database.mostPlayedArtists(fromTimeStamp = startTimestamp, toTimeStamp = endTimestamp, limit = -1)
        .map { artists -> artists.filter { it.artist.isYouTubeArtist } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedAlbums = database.mostPlayedAlbums(fromTimeStamp = startTimestamp, toTimeStamp = endTimestamp, limit = -1)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
