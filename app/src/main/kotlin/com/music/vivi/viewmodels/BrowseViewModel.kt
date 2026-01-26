package com.music.vivi.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.YTItem
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Generic ViewModel for browsing YouTube Music endpoints.
 * Used for dynamic content pages that don't fit into other specific categories.
 */
@HiltViewModel
public class BrowseViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val browseId: String? = savedStateHandle.get<String>("browseId")

    public val items: MutableStateFlow<List<YTItem>?> = MutableStateFlow(emptyList())
    public val title: MutableStateFlow<String?> = MutableStateFlow("")

    init {
        viewModelScope.launch {
            browseId?.let {
                YouTube.browse(browseId, null).onSuccess { result ->
                    // Store the title
                    title.value = result.title

                    // Flatten the nested structure to get all YTItems
                    val allItems = result.items.flatMap { it.items }
                    items.value = allItems
                }.onFailure {
                    reportException(it)
                }
            }
        }
    }
}
