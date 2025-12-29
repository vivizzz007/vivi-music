package com.music.vivi.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.YTItem
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.HideVideoSongsKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.SearchHistory
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnlineSearchSuggestionViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    database: MusicDatabase,
) : ViewModel() {
    val query = MutableStateFlow("")
    private val _viewState = MutableStateFlow(SearchSuggestionViewState())
    val viewState = _viewState.asStateFlow()

    init {
        viewModelScope.launch {
            query
                .flatMapLatest { query ->
                    if (query.isEmpty()) {
                        database.searchHistory().map { history ->
                            SearchSuggestionViewState(
                                history = history,
                            )
                        }
                    } else {
                        _viewState.value = _viewState.value.copy(isLoading = true, error = null)
                        val result = YouTube.searchSuggestions(query).onSuccess { result ->
                            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

                            viewModelScope.launch {
                                database
                                    .searchHistory(query)
                                    .map { it.take(3) }
                                    .map { history ->
                                        SearchSuggestionViewState(
                                            history = history,
                                            suggestions =
                                            result
                                                .queries
                                                .filter { suggestionQuery ->
                                                    history.none { it.query == suggestionQuery }
                                                },
                                            items =
                                            result
                                                .recommendedItems
                                                .distinctBy { it.id }
                                                .filterExplicit(hideExplicit)
                                                .filterVideoSongs(hideVideoSongs),
                                            isLoading = false,
                                            error = null
                                        )
                                    }.collect {
                                        _viewState.value = it
                                    }
                            }
                        }.onFailure {
                            _viewState.value = _viewState.value.copy(isLoading = false, error = it)
                        }
                        
                        // Emit history even if suggestions fail
                        database.searchHistory(query).map { history ->
                            _viewState.value.copy(
                                history = history.take(3),
                                isLoading = false
                            )
                        }
                    }
                }.collect {
                    _viewState.value = it
                }
        }
    }
}

data class SearchSuggestionViewState(
    val history: List<SearchHistory> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val items: List<YTItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: Throwable? = null
)