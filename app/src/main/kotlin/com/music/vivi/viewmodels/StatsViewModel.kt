package com.music.vivi.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.vivi.constants.statToPeriod
import com.music.vivi.db.MusicDatabase
import com.music.vivi.ui.screens.OptionStats
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * ViewModel for the User Statistics Screen.
 *
 * Capabilities:
 * - Computes "Most Played" Songs, Artists, Albums.
 * - Supports filtering by Time Period (Week, Month, Year, etc.).
 * - Fetches thumbnails for top artists if missing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
public class StatsViewModel
@Inject
constructor(public val database: MusicDatabase) : ViewModel() {
    public val selectedOption: MutableStateFlow<OptionStats> = MutableStateFlow(OptionStats.CONTINUOUS)
    public val indexChips: MutableStateFlow<Int> = MutableStateFlow(0)

    public val mostPlayedSongsStats: StateFlow<List<com.music.vivi.db.entities.SongWithStats>> =
        combine(
            selectedOption,
            indexChips
        ) { first, second -> Pair(first, second) }
            .flatMapLatest { (selection, t) ->
                database
                    .mostPlayedSongsStats(
                        fromTimeStamp = statToPeriod(selection, t),
                        limit = -1,
                        toTimeStamp =
                        if (selection == OptionStats.CONTINUOUS || t == 0) {
                            LocalDateTime
                                .now()
                                .toInstant(
                                    ZoneOffset.UTC
                                )
                                .toEpochMilli()
                        } else {
                            statToPeriod(selection, t - 1)
                        }
                    )
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    public val mostPlayedSongs: StateFlow<List<com.music.vivi.db.entities.Song>> =
        combine(
            selectedOption,
            indexChips
        ) { first, second -> Pair(first, second) }
            .flatMapLatest { (selection, t) ->
                database
                    .mostPlayedSongs(
                        fromTimeStamp = statToPeriod(selection, t),
                        limit = -1,
                        toTimeStamp =
                        if (selection == OptionStats.CONTINUOUS || t == 0) {
                            LocalDateTime
                                .now()
                                .toInstant(
                                    ZoneOffset.UTC
                                )
                                .toEpochMilli()
                        } else {
                            statToPeriod(selection, t - 1)
                        }
                    )
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    public val mostPlayedArtists: StateFlow<List<com.music.vivi.db.entities.Artist>> =
        combine(
            selectedOption,
            indexChips
        ) { first, second -> Pair(first, second) }
            .flatMapLatest { (selection, t) ->
                database.mostPlayedArtists(
                    statToPeriod(selection, t),
                    limit = -1,
                    toTimeStamp =
                    if (selection == OptionStats.CONTINUOUS || t == 0) {
                        LocalDateTime
                            .now()
                            .toInstant(
                                ZoneOffset.UTC
                            )
                            .toEpochMilli()
                    } else {
                        statToPeriod(selection, t - 1)
                    }
                ).map { artists ->
                    artists.filter { it.artist.isYouTubeArtist }
                }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    public val mostPlayedAlbums: StateFlow<List<com.music.vivi.db.entities.Album>> =
        combine(
            selectedOption,
            indexChips
        ) { first, second -> Pair(first, second) }
            .flatMapLatest { (selection, t) ->
                database.mostPlayedAlbums(
                    statToPeriod(selection, t),
                    limit = -1,
                    toTimeStamp =
                    if (selection == OptionStats.CONTINUOUS || t == 0) {
                        LocalDateTime
                            .now()
                            .toInstant(
                                ZoneOffset.UTC
                            )
                            .toEpochMilli()
                    } else {
                        statToPeriod(selection, t - 1)
                    }
                )
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    public val firstEvent: StateFlow<com.music.vivi.db.entities.Event?> =
        database.firstEvent().map { it?.event }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        viewModelScope.launch {
            mostPlayedArtists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter {
                        it.thumbnailUrl == null ||
                            Duration.between(
                                it.lastUpdateTime,
                                LocalDateTime.now()
                            ) > Duration.ofDays(10)
                    }.forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
        viewModelScope.launch {
            mostPlayedAlbums.collect { albums ->
                albums
                    .filter {
                        it.album.songCount == 0
                    }.forEach { album ->
                        YouTube
                            .album(album.id)
                            .onSuccess { albumPage ->
                                database.query {
                                    update(album.album, albumPage, album.artists)
                                }
                            }.onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query {
                                        delete(album.album)
                                    }
                                }
                            }
                    }
            }
        }
    }
}
