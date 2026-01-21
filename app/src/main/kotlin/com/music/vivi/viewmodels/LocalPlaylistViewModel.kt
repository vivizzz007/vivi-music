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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LocalPlaylistViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _playlistId = MutableStateFlow(savedStateHandle.get<String>("playlistId"))
    val playlistId = _playlistId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val playlist = _playlistId.filterNotNull().flatMapLatest { id ->
        database.playlist(id)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val playlistSongs: StateFlow<List<PlaylistSong>> =
        combine(
            _playlistId.filterNotNull(),
            context.dataStore.data
                .map {
                    it[PlaylistSongSortTypeKey].toEnum(PlaylistSongSortType.CUSTOM) to (it[PlaylistSongSortDescendingKey]
                        ?: true)
                }.distinctUntilChanged(),
        ) { id, (sortType, sortDescending) ->
            Triple(id, sortType, sortDescending)
        }.flatMapLatest { (id, sortType, sortDescending) ->
            val songsFlow = database.playlistSongs(id)
            songsFlow.map { songs ->
                 sortSongs(songs, sortType, sortDescending)
            }
            when (sortType) {
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
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private fun sortSongs(songs: List<PlaylistSong>, sortType: PlaylistSongSortType, sortDescending: Boolean): List<PlaylistSong> {
        return when (sortType) {
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
    }

    fun setPlaylistId(id: String) {
        if (_playlistId.value != id) {
             _playlistId.value = id
        }
    }
}
