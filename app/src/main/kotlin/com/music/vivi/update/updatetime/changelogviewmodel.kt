package com.music.vivi.update.updatetime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ChangelogViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChangelogState())
    val uiState: StateFlow<ChangelogState> = _uiState.asStateFlow()

    // Modified to accept a specific versionTag
    fun loadChangelog(repoOwner: String, repoName: String, versionTag: String) { // <--- ADDED versionTag PARAMETER
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Pass the versionTag to the fetching function
                val markdownContent = fetchReleaseMarkdown(repoOwner, repoName, versionTag) // <--- PASSING versionTag
                _uiState.update {
                    it.copy(
                        changes = markdownContent,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error loading changelog for $versionTag: ${e.message}. Tag not found or network issue. May be you are using beta version " // Improved error message
                    )
                }
            }
        }
    }

    // Modified to fetch a specific release by tag
    private suspend fun fetchReleaseMarkdown(owner: String, repo: String, versionTag: String): String { // <--- ADDED versionTag PARAMETER
        return withContext(Dispatchers.IO) {
            val url = URL("https://api.github.com/repos/$owner/$repo/releases/tags/$versionTag") // <--- FETCHING BY SPECIFIC TAG
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                // Throwing an IOException when the tag isn't found will be caught by the ViewModel
                throw IOException(
                    "HTTP error: ${connection.responseCode}. Release tag '$versionTag' might not exist on GitHub."
                )
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(response)
            jsonObject.optString("body", "")
        }
    }
}

// Changelog State
data class ChangelogState(val changes: String = "", val isLoading: Boolean = true, val error: String? = null)
