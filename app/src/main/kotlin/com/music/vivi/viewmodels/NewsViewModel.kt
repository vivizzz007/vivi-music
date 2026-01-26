package com.music.vivi.viewmodels

import androidx.lifecycle.ViewModel
import com.music.vivi.repositories.NewsItem // Import from repository
import com.music.vivi.repositories.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ViewModel for the App News/Updates system.
 * Delegates to [NewsRepository] to fetch and manage read state of news items.
 */
@HiltViewModel
public class NewsViewModel @Inject constructor(private val repository: NewsRepository) : ViewModel() {

    public val newsItems: StateFlow<List<NewsItem>> = repository.newsItems
    public val isLoading: StateFlow<Boolean> = repository.isLoading
    public val isRefreshing: StateFlow<Boolean> = repository.isRefreshing
    public val error: StateFlow<String?> = repository.error
    public val readNewsIds: StateFlow<Set<String>> = repository.readNewsIds
    public val hasUnreadNews: StateFlow<Boolean> = repository.hasUnreadNews

    public fun fetchNews(isRefresh: Boolean = false, silent: Boolean = false) {
        repository.fetchNews(isRefresh, silent)
    }

    public fun markAsRead(item: NewsItem) {
        repository.markAsRead(item)
    }

    public fun getItemId(item: NewsItem): String = repository.getItemId(item)
}
