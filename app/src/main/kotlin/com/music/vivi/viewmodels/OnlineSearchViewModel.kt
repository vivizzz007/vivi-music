package com.music.vivi.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import com.music.innertube.pages.SearchSummaryPage
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.HideVideoSongsKey
import com.music.vivi.models.ItemsPage
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = savedStateHandle.get<String>("query")!!
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<Throwable?>(null)

    init {
        viewModelScope.launch {
            filter.collect { filter ->
                fetchSearchResults(filter)
            }
        }
    }

    fun retry() {
        fetchSearchResults(filter.value)
    }

    private fun fetchSearchResults(filter: YouTube.SearchFilter?) {
        viewModelScope.launch {
            if (filter == null) {
                if (summaryPage == null) {
                    isLoading = true
                    error = null
                    YouTube
                        .searchSummary(query)
                        .onSuccess {
                            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                            summaryPage =
                                it.filterExplicit(
                                    hideExplicit,
                                ).filterVideoSongs(hideVideoSongs)
                            isLoading = false
                        }.onFailure {
                            error = it
                            isLoading = false
                            reportException(it)
                        }
                }
            } else {
                if (viewStateMap[filter.value] == null) {
                    isLoading = true
                    error = null
                    YouTube
                        .search(query, filter)
                        .onSuccess { result ->
                            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                            viewStateMap[filter.value] =
                                ItemsPage(
                                    result.items
                                        .distinctBy { it.id }
                                        .filterExplicit(
                                            hideExplicit,
                                        )
                                        .filterVideoSongs(hideVideoSongs),
                                    result.continuation,
                                )
                            isLoading = false
                        }.onFailure {
                            error = it
                            isLoading = false
                            reportException(it)
                        }
                }
            }
        }
    }

    fun loadMore() {
        val filter = filter.value?.value
        viewModelScope.launch {
            if (filter == null) return@launch
            val viewState = viewStateMap[filter] ?: return@launch
            val continuation = viewState.continuation
            if (continuation != null) {
                val searchResult =
                    YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                val newItems = searchResult.items
                    .filterExplicit(hideExplicit)
                    .filterVideoSongs(hideVideoSongs)
                viewStateMap[filter] = ItemsPage(
                    (viewState.items + newItems).distinctBy { it.id },
                    searchResult.continuation
                )

            }
        }
    }
}
