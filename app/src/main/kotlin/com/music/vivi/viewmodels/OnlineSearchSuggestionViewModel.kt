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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for showing Search Suggestions (Auto-complete) and History.
 * Combines local search history with remote YouTube search suggestions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
public class OnlineSearchSuggestionViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    database: MusicDatabase,
) : ViewModel() {
    public val query: MutableStateFlow<String> = MutableStateFlow("")
    private val _viewState = MutableStateFlow(SearchSuggestionViewState())
    public val viewState: StateFlow<SearchSuggestionViewState> = _viewState.asStateFlow()

    init {
        viewModelScope.launch {
            query
                .debounce(300)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isEmpty()) {
                        database.searchHistory().map { history ->
                            SearchSuggestionViewState(
                                history = history
                            )
                        }
                    } else {
                        flow {
                            emit(_viewState.value.copy(isLoading = true, error = null))

                            val history = database.searchHistory(query).first().take(3)
                            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

                            YouTube.searchSuggestions(query)
                                .onSuccess { result ->
                                    emit(
                                        SearchSuggestionViewState(
                                            history = history,
                                            suggestions = result.queries.filter { sug ->
                                                history.none { it.query == sug }
                                            },
                                            items = result.recommendedItems
                                                .distinctBy { it.id }
                                                .filterExplicit(hideExplicit)
                                                .filterVideoSongs(hideVideoSongs),
                                            isLoading = false,
                                            error = null
                                        )
                                    )
                                }.onFailure { error ->
                                    emit(
                                        SearchSuggestionViewState(
                                            history = history,
                                            isLoading = false,
                                            error = error
                                        )
                                    )
                                }
                        }
                    }
                }.collect {
                    _viewState.value = it
                }
        }
    }
}

public data class SearchSuggestionViewState(
    val history: List<SearchHistory> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val items: List<YTItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: Throwable? = null,
)
