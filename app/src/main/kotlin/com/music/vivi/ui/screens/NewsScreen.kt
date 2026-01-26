package com.music.vivi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.vivi.ui.component.news.NewsDetailScreen
import com.music.vivi.ui.component.news.NewsListScreen
import com.music.vivi.viewmodels.NewsViewModel

/**
 * Screen displaying App News and Announcements.
 * Can show a List view or a Detail view (using AnimatedContent).
 * Auto-refreshes every 30 seconds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun NewsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: NewsViewModel = hiltViewModel(),
) {
    val newsItems by viewModel.newsItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val error by viewModel.error.collectAsState()
    val readNewsIds by viewModel.readNewsIds.collectAsState()

    // Auto-refresh every 30 seconds
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            viewModel.fetchNews(silent = true)
        }
    }

    // Track selected item by Title to ensure we always show the latest version from the ViewModel
    var selectedNewsTitle by remember { mutableStateOf<String?>(null) }

    // Derived state: Find the item in the list that matches the selected title
    val selectedNewsItem = newsItems.find { it.title == selectedNewsTitle }

    // Handle back press when detail view is open
    BackHandler(enabled = selectedNewsTitle != null) {
        selectedNewsTitle = null
    }

    AnimatedContent(
        targetState = selectedNewsTitle,
        transitionSpec = {
            if (targetState != null) {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it / 2 } + fadeOut()
            } else {
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            }
        },
        label = "NewsTransition"
    ) { title ->
        if (title != null && selectedNewsItem != null) {
            NewsDetailScreen(
                newsItem = selectedNewsItem,
                navController = navController,
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.fetchNews(isRefresh = true) }
            )
        } else {
            NewsListScreen(
                navController = navController,
                scrollBehavior = scrollBehavior,
                newsItems = newsItems,
                isLoading = isLoading,
                isRefreshing = isRefreshing,
                error = error,
                readNewsIds = readNewsIds,
                getItemId = { viewModel.getItemId(it) },
                onItemClick = {
                    viewModel.markAsRead(it)
                    selectedNewsTitle = it.title
                },
                onRefresh = { viewModel.fetchNews(isRefresh = true) }
            )
        }
    }
}

// NewsListScreen and NewsDetailScreen extracted to ui/component/news/
