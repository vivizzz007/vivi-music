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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TopPlaylistViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _top = MutableStateFlow(savedStateHandle.get<String>("top"))
    val top = _top.asStateFlow()

    val topPeriod = MutableStateFlow(MyTopFilter.ALL_TIME)

    @OptIn(ExperimentalCoroutinesApi::class)
    val topSongs =
    val topSongs =
        combine(topPeriod, _top.filterNotNull()) { period, topVal ->
            Pair(period, topVal)
        }.flatMapLatest { (period, topVal) ->
            database.mostPlayedSongs(period.toTimeMillis(), topVal.toInt())
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setTop(topParam: String) {
        if (_top.value != topParam) {
            _top.value = topParam
        }
    }
}
