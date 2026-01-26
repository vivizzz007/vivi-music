package com.music.vivi.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.constants.AddToPlaylistSortDescendingKey
import com.music.vivi.constants.AddToPlaylistSortTypeKey
import com.music.vivi.constants.PlaylistSortType
import com.music.vivi.db.MusicDatabase
import com.music.vivi.extensions.toEnum
import com.music.vivi.utils.SyncUtils
import com.music.vivi.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the "Add to Playlist" sheet or Playlists management screen.
 * Displays all user playlists and handles syncing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
public class PlaylistsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    public val allPlaylists: StateFlow<List<com.music.vivi.db.entities.Playlist>> =
        context.dataStore.data
            .map {
                it[AddToPlaylistSortTypeKey].toEnum(PlaylistSortType.CREATE_DATE) to (
                    it[AddToPlaylistSortDescendingKey]
                        ?: true
                    )
            }.distinctUntilChanged()
            .flatMapLatest { (sortType, descending) ->
                database.playlists(sortType, descending)
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Suspend function that waits for sync to complete
    public suspend fun sync() {
        syncUtils.syncSavedPlaylists()
    }
}
