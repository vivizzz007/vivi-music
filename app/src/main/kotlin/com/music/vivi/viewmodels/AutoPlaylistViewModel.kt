package com.music.vivi.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.SongSortDescendingKey
import com.music.vivi.constants.SongSortType
import com.music.vivi.constants.SongSortTypeKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.extensions.filterExplicit
import com.music.vivi.extensions.toEnum
import com.music.vivi.utils.SyncUtils
import com.music.vivi.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Auto-generated playlists (Liked, Downloaded, Uploaded).
 * Handles sorting and filtering preferences for these special collections.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
public class AutoPlaylistViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    private val _playlist = MutableStateFlow(savedStateHandle.get<String>("playlist"))
    public val playlist: StateFlow<String?> = _playlist.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    public val likedSongs: StateFlow<List<com.music.vivi.db.entities.Song>> =
        context.dataStore.data
            .map {
                Pair(
                    it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE) to (
                        it[SongSortDescendingKey]
                            ?: true
                        ),
                    it[HideExplicitKey] ?: false
                )
            }
            .distinctUntilChanged()
            .flatMapLatest { (sortDesc, hideExplicit) ->
                val (sortType, descending) = sortDesc

                _playlist.filterNotNull().flatMapLatest { playlistName ->
                    when (playlistName) {
                        "liked" -> database.likedSongs(sortType, descending)
                            .map { it.filterExplicit(hideExplicit) }

                        "downloaded" -> database.downloadedSongs(sortType, descending)
                            .map { it.filterExplicit(hideExplicit) }

                        "uploaded" -> database.uploadedSongs(sortType, descending)
                            .map { it.filterExplicit(hideExplicit) }

                        else -> flowOf(emptyList())
                    }
                }
            }
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, emptyList())

    public fun setPlaylist(playlistParam: String) {
        if (_playlist.value != playlistParam) {
            _playlist.value = playlistParam
        }
    }

    public fun syncLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedSongs() }
    }

    public fun syncUploadedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncUploadedSongs() }
    }
}
