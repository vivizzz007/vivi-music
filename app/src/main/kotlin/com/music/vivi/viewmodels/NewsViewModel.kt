package com.music.vivi.viewmodels

import androidx.lifecycle.ViewModel
import com.music.vivi.repositories.NewsRepository
import com.music.vivi.repositories.NewsItem // Import from repository
import com.music.vivi.repositories.ContentBlock // Import from repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository
) : ViewModel() {

    val newsItems: StateFlow<List<NewsItem>> = repository.newsItems
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val isRefreshing: StateFlow<Boolean> = repository.isRefreshing
    val error: StateFlow<String?> = repository.error
    val readNewsIds: StateFlow<Set<String>> = repository.readNewsIds
    val hasUnreadNews: StateFlow<Boolean> = repository.hasUnreadNews

    fun fetchNews(isRefresh: Boolean = false, silent: Boolean = false) {
        repository.fetchNews(isRefresh, silent)
    }

    fun markAsRead(item: NewsItem) {
        repository.markAsRead(item)
    }

    fun getItemId(item: NewsItem): String {
        return repository.getItemId(item)
    }
}

