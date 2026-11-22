/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.pages.MoodAndGenres
import com.music.vivi.utils.MoodThumbnailHelper
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoodAndGenresViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val moodAndGenres = MutableStateFlow<List<MoodAndGenres>?>(null)

    private val _thumbnails = MutableStateFlow<Map<String, String>>(emptyMap())
    val thumbnails = _thumbnails.asStateFlow()

    init {
        // Bulk load all previously cached thumbnails into memory on frame 1
        _thumbnails.value = MoodThumbnailHelper.loadAllCached(context)

        viewModelScope.launch {
            YouTube
                .moodAndGenres()
                .onSuccess { list ->
                    moodAndGenres.value = list
                    // Parallel preloading of ALL categories
                    MoodThumbnailHelper.preloadAll(context, list) { key, url ->
                        _thumbnails.update { it + (key to url) }
                    }
                }.onFailure {
                    reportException(it)
                }
        }
    }
}
