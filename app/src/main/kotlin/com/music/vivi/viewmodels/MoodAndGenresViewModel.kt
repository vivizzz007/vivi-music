package com.music.vivi.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.pages.MoodAndGenres
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for fetching the list of Moods and Genres categories.
 */
@HiltViewModel
public class MoodAndGenresViewModel
@Inject
constructor() : ViewModel() {
    public val moodAndGenres: MutableStateFlow<List<MoodAndGenres>?> = MutableStateFlow(null)

    init {
        viewModelScope.launch {
            YouTube
                .moodAndGenres()
                .onSuccess {
                    moodAndGenres.value = it
                }.onFailure {
                    reportException(it)
                }
        }
    }
}
