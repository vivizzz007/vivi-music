package com.music.vivi.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.constants.PlaylistSongSortDescendingKey
import com.music.vivi.constants.PlaylistSongSortType
import com.music.vivi.constants.PlaylistSongSortTypeKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.PlaylistSong
import com.music.vivi.extensions.reversed
import com.music.vivi.extensions.toEnum
import com.music.vivi.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for a User-created Local Playlist.
 * Handles fetching playlist details and sorting its songs based on user preference.
 */
@HiltViewModel
public class LocalPlaylistViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _playlistId = MutableStateFlow(savedStateHandle.get<String>("playlistId"))
    public val playlistId: StateFlow<String?> = _playlistId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    public val playlist: StateFlow<com.music.vivi.db.entities.Playlist?> = _playlistId.filterNotNull().flatMapLatest { id ->
        database.playlist(id)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    public val playlistSongs: StateFlow<List<PlaylistSong>> =
        combine(
            _playlistId.filterNotNull(),
            context.dataStore.data
                .map {
                    it[PlaylistSongSortTypeKey].toEnum(PlaylistSongSortType.CUSTOM) to
                        (
                            it[PlaylistSongSortDescendingKey]
                                ?: true
                            )
                }.distinctUntilChanged()
        ) { id, sortOptions ->
            Triple(id, sortOptions.first, sortOptions.second)
        }.flatMapLatest { (id, sortType, sortDescending) ->
            database.playlistSongs(id).map { songs ->
                sortSongs(songs, sortType, sortDescending)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private fun sortSongs(
        songs: List<PlaylistSong>,
        sortType: PlaylistSongSortType,
        sortDescending: Boolean,
    ): List<PlaylistSong> = when (sortType) {
        PlaylistSongSortType.CUSTOM -> songs
        PlaylistSongSortType.CREATE_DATE -> songs.sortedBy { it.map.id }
        PlaylistSongSortType.NAME -> {
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            songs.sortedWith(compareBy(collator) { it.song.song.title })
        }
        PlaylistSongSortType.ARTIST -> {
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            songs
                .sortedWith(compareBy(collator) { song -> song.song.artists.joinToString("") { it.name } })
                .groupBy { it.song.album?.title }
                .flatMap { (_, songsByAlbum) ->
                    songsByAlbum.sortedBy {
                        it.song.artists.joinToString(
                            ""
                        ) { it.name }
                    }
                }
        }
        PlaylistSongSortType.PLAY_TIME -> songs.sortedBy { it.song.song.totalPlayTime }
    }.reversed(sortDescending && sortType != PlaylistSongSortType.CUSTOM)

    public fun setPlaylistId(id: String) {
        if (_playlistId.value != id) {
            _playlistId.value = id
        }
    }
}
