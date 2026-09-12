package com.music.vivi.github

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.constants.GitHubAccessTokenKey
import com.music.vivi.constants.HasStarredRepoKey
import com.music.vivi.github.network.GitHubService
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GitHubViewModel @Inject constructor(
    private val gitHubService: GitHubService
) : ViewModel() {

    private val _isStarred = MutableStateFlow(false)
    val isStarred: StateFlow<Boolean> = _isStarred.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showThankYouDialog = MutableStateFlow(false)
    val showThankYouDialog: StateFlow<Boolean> = _showThankYouDialog.asStateFlow()

    private val _starCount = MutableStateFlow<Int?>(null)
    val starCount: StateFlow<Int?> = _starCount.asStateFlow()

    fun fetchStarCount() {
        viewModelScope.launch {
            val details = gitHubService.getRepoDetails()
            if (details != null) {
                _starCount.value = details.stargazersCount
            }
        }
    }

    fun dismissThankYouDialog() {
        _showThankYouDialog.value = false
    }

    fun checkStarStatus(context: Context) {
        viewModelScope.launch {
            _isStarred.value = context.dataStore[HasStarredRepoKey] ?: false
        }
    }

    fun toggleStar(
        context: Context,
        onNavigateToRepo: () -> Unit
    ) {
        viewModelScope.launch {
            // Already starred — do nothing
            if (_isStarred.value) return@launch

            _isStarred.value = true
            _showThankYouDialog.value = true
            context.dataStore.edit { preferences ->
                preferences[HasStarredRepoKey] = true
            }
            onNavigateToRepo()
        }
    }
}
