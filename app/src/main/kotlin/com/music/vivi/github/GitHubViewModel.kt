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

    fun checkStarStatus(context: Context) {
        viewModelScope.launch {
            val token = context.dataStore[GitHubAccessTokenKey]
            if (!token.isNullOrBlank()) {
                val starred = gitHubService.isRepoStarred(token)
                _isStarred.value = starred
                context.dataStore.edit { preferences ->
                    preferences[HasStarredRepoKey] = starred
                }
            } else {
                _isStarred.value = context.dataStore[HasStarredRepoKey] ?: false
            }
        }
    }

    fun toggleStar(
        context: Context,
        onAuthRequired: () -> Unit
    ) {
        viewModelScope.launch {
            val token = context.dataStore[GitHubAccessTokenKey]
            if (token.isNullOrBlank()) {
                onAuthRequired()
                return@launch
            }

            _isLoading.value = true
            val currentlyStarred = _isStarred.value
            val success = if (currentlyStarred) {
                gitHubService.unstarRepo(token)
            } else {
                gitHubService.starRepo(token)
            }

            if (success) {
                val newState = !currentlyStarred
                _isStarred.value = newState
                context.dataStore.edit { preferences ->
                    preferences[HasStarredRepoKey] = newState
                }
            }
            _isLoading.value = false
        }
    }

    fun saveAccessToken(context: Context, token: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[GitHubAccessTokenKey] = token
            }
            checkStarStatus(context)
        }
    }
}
