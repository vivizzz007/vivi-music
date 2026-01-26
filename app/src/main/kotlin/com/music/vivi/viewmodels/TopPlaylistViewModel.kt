package com.music.vivi.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.constants.MyTopFilter
import com.music.vivi.db.MusicDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for "My Top 100" playlists (Most Played).
 * Supports filtering by time period (Week, Month, Year, All Time).
 */
@HiltViewModel
public class TopPlaylistViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _top = MutableStateFlow(savedStateHandle.get<String>("top"))
    public val top: StateFlow<String?> = _top.asStateFlow()

    public val topPeriod: MutableStateFlow<MyTopFilter> = MutableStateFlow(MyTopFilter.ALL_TIME)

    @OptIn(ExperimentalCoroutinesApi::class)
    public val topSongs: StateFlow<List<com.music.vivi.db.entities.Song>> =
        combine(topPeriod, _top.filterNotNull()) { period, topVal ->
            Pair(period, topVal)
        }.flatMapLatest { (period, topVal) ->
            database.mostPlayedSongs(period.toTimeMillis(), topVal.toInt())
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    public fun setTop(topParam: String) {
        if (_top.value != topParam) {
            _top.value = topParam
        }
    }
}
