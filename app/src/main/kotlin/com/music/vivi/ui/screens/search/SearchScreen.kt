/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.utils.YouTubeUrlParser
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalIsPlayerExpanded
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.PauseSearchHistoryKey
import com.music.vivi.constants.SearchSource
import com.music.vivi.constants.SearchSourceKey
import com.music.vivi.db.entities.SearchHistory
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.MoodAndGenresViewModel
import com.music.vivi.viewmodels.ExploreViewModel
import com.music.vivi.ui.screens.search.suggestions.SuggestionsTabContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder
import androidx.compose.runtime.collectAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.YouTubeGridItem
import com.music.vivi.ui.menu.YouTubeAlbumMenu
import com.music.vivi.constants.GridThumbnailHeight
import com.music.vivi.constants.GridItemsSizeKey
import com.music.vivi.constants.GridItemSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    pureBlack: Boolean
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isPlayerExpanded = LocalIsPlayerExpanded.current
    val playerConnection = LocalPlayerConnection.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)
    var isFirstLaunch by rememberSaveable { mutableStateOf(true) }
    
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var showSearchContent by remember { mutableStateOf(false) }

    LaunchedEffect(searchActive) {
        if (searchActive) {
            // Small delay to let the initial expansion animation run smoothly
            // before composing the potentially heavy search results/history
            kotlinx.coroutines.delay(100)
            showSearchContent = true
        } else {
            showSearchContent = false
        }
    }

    val searchBarHorizontalPadding by animateDpAsState(
        targetValue = if (searchActive) 0.dp else 16.dp,
        animationSpec = tween(durationMillis = 245, easing = FastOutSlowInEasing),
        label = "SearchBarHorizontalPadding"
    )
    val searchBarTopPadding by animateDpAsState(
        targetValue = if (searchActive) 0.dp else 8.dp,
        animationSpec = tween(durationMillis = 245, easing = FastOutSlowInEasing),
        label = "SearchBarTopPadding"
    )

    val onSearch: (String) -> Unit = remember {
        { searchQuery ->
            if (searchQuery.isNotEmpty()) {
                focusManager.clearFocus()
                println("[LINK_PARSE_DEBUG] onSearch initiated for: $searchQuery")
                
                when (val parsedUrl = YouTubeUrlParser.parse(searchQuery)) {
                    is YouTubeUrlParser.ParsedUrl.Video -> {
                        println("[LINK_PARSE_DEBUG] Performing direct playback for Video ID: ${parsedUrl.id}")
                        playerConnection?.playQueue(
                            YouTubeQueue(
                                WatchEndpoint(videoId = parsedUrl.id),
                            ),
                        )
                    }

                    is YouTubeUrlParser.ParsedUrl.Artist -> {
                        println("[LINK_PARSE_DEBUG] Navigating to Artist: ${parsedUrl.id}")
                        navController.navigate("artist/${parsedUrl.id}")
                    }

                    null -> {
                        println("[LINK_PARSE_DEBUG] No URL detected in search action")
                        navController.navigate("search/${URLEncoder.encode(searchQuery, "UTF-8")}")
                    }
                }

                if (!pauseSearchHistory) {
                    coroutineScope.launch(Dispatchers.IO) {
                        database.query {
                            insert(SearchHistory(query = searchQuery))
                        }
                    }
                }
            }
        }
    }

    val onSearchFromSuggestion: (String) -> Unit = remember {
        { searchQuery ->
            if (searchQuery.isNotEmpty()) {
                focusManager.clearFocus()
                println("[LINK_PARSE_DEBUG] onSearchFromSuggestion initiated for: $searchQuery")
                
                when (val parsedUrl = YouTubeUrlParser.parse(searchQuery)) {
                    is YouTubeUrlParser.ParsedUrl.Video -> {
                        println("[LINK_PARSE_DEBUG] Performing direct playback from suggestion for Video ID: ${parsedUrl.id}")
                        playerConnection?.playQueue(
                            YouTubeQueue(
                                WatchEndpoint(videoId = parsedUrl.id),
                            ),
                        )
                    }

                    is YouTubeUrlParser.ParsedUrl.Artist -> {
                        println("[LINK_PARSE_DEBUG] Navigating to Artist from suggestion: ${parsedUrl.id}")
                        navController.navigate("artist/${parsedUrl.id}")
                    }

                    null -> {
                        println("[LINK_PARSE_DEBUG] No URL detected in suggestion action")
                        navController.navigate("search/${URLEncoder.encode(searchQuery, "UTF-8")}")
                    }
                }

                if (!pauseSearchHistory) {
                    coroutineScope.launch(Dispatchers.IO) {
                        database.query {
                            insert(SearchHistory(query = searchQuery))
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface)
            ) {
                SearchBar(
                    query = query.text,
                    onQueryChange = { query = TextFieldValue(it) },
                    onSearch = { 
                        onSearch(it)
                        searchActive = false
                    },
                    active = searchActive,
                    onActiveChange = { searchActive = it },
                    placeholder = {
                        Text(
                            text = stringResource(
                                when (searchSource) {
                                    SearchSource.LOCAL -> R.string.search_library
                                    SearchSource.ONLINE -> R.string.search_yt_music
                                }
                            ),
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 16.sp
                            )
                        )
                    },
                    leadingIcon = {
                        IconButton(onClick = {
                            if (searchActive) {
                                searchActive = false
                                query = TextFieldValue("") // Clear text when dismissing search
                            } else {
                                searchActive = true // Focus search instead of navigating back
                            }
                        }) {
                            Icon(
                                painter = painterResource(if (searchActive) R.drawable.arrow_back else R.drawable.search),
                                contentDescription = if (searchActive) stringResource(R.string.dismiss) else null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (query.text.isNotEmpty()) {
                                IconButton(onClick = { query = TextFieldValue("") }) {
                                    Icon(
                                        painter = painterResource(R.drawable.close),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    searchSource = if (searchSource == SearchSource.ONLINE) 
                                        SearchSource.LOCAL else SearchSource.ONLINE
                                }
                            ) {
                                Icon(
                                    painter = painterResource(
                                        when (searchSource) {
                                            SearchSource.LOCAL -> R.drawable.library_music
                                            SearchSource.ONLINE -> R.drawable.globe_search
                                        }
                                    ),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    colors = SearchBarDefaults.colors(
                        containerColor = if (pureBlack) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = searchBarHorizontalPadding)
                        .padding(top = searchBarTopPadding)
                ) {
                    if (showSearchContent) {
                        when (searchSource) {
                            SearchSource.LOCAL -> LocalSearchScreen(
                                query = query.text,
                                navController = navController,
                                onDismiss = { searchActive = false },
                                pureBlack = pureBlack
                            )
                            SearchSource.ONLINE -> OnlineSearchScreen(
                                query = query.text,
                                onQueryChange = { query = it },
                                navController = navController,
                                onSearch = {
                                    onSearchFromSuggestion(it)
                                    searchActive = false
                                },
                                onDismiss = { searchActive = false },
                                pureBlack = pureBlack
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = !searchActive,
                    enter = expandVertically(animationSpec = tween(durationMillis = 245, easing = FastOutSlowInEasing)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(durationMillis = 245, easing = FastOutSlowInEasing)) + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        SecondaryTabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = Color.Transparent,
                            indicator = {
                                Box(
                                    modifier = Modifier
                                        .tabIndicatorOffset(selectedTabIndex)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(32.dp)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        ) {
                            Tab(
                                selected = selectedTabIndex == 0,
                                onClick = { selectedTabIndex = 0 },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                text = { Text(stringResource(R.string.tab_explore)) }
                            )
                            Tab(
                                selected = selectedTabIndex == 1,
                                onClick = { selectedTabIndex = 1 },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                text = { Text(stringResource(R.string.tab_Suggestions)) }
                            )
                            Tab(
                                selected = selectedTabIndex == 2,
                                onClick = { selectedTabIndex = 2 },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                text = { Text(stringResource(R.string.tab_album)) }
                            )
                        }
                    }
                }
            }
        },
        containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
        
        Box(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .fillMaxSize()
        ) {
            if (!searchActive) {
                val tabPadding = PaddingValues(bottom = bottomPadding)
                when (selectedTabIndex) {
                    0 -> ExploreTabContent(navController = navController, contentPadding = tabPadding)
                    1 -> SuggestionsTabContent(navController = navController, contentPadding = tabPadding)
                    2 -> AlbumsTabContent(navController = navController, contentPadding = tabPadding)
                }
            }
        }
    }

    // Handle lifecycle events to manage keyboard visibility
    DisposableEffect(lifecycleOwner, isPlayerExpanded) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // Always hide keyboard when resuming if player is expanded
                    if (isPlayerExpanded) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    } else if (isFirstLaunch) {
                        // Only request focus on first launch when player is not expanded
                        try {
                            focusRequester.requestFocus()
                        } catch (e: Exception) {
                            // Ignore focus request failures
                        }
                        isFirstLaunch = false
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // Clear focus when pausing to prevent keyboard from showing on resume
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        // Initial check - hide keyboard if player is expanded
        if (isPlayerExpanded) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
fun ExploreTabContent(
    navController: NavController,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val moodAndGenresList by viewModel.moodAndGenres.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        moodAndGenresList?.forEach { section ->
            item {
                NavigationTitle(title = section.title)
            }
            // chunk items into rows of 2
            val rows = section.items.chunked(2)
            items(rows) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                ) {
                    row.forEach { item ->
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier
                                .weight(1f)
                                .padding(6.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .clickable {
                                    navController.navigate(
                                        "youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}"
                                    )
                                }
                                .padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    // fill empty slot if row has only 1 item
                    repeat(2 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (moodAndGenresList == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator()
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun AlbumsTabContent(
    navController: NavController,
    viewModel: ExploreViewModel = hiltViewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by (playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) })
    val isPlaying by (playerConnection?.isEffectivelyPlaying?.collectAsState() ?: remember { mutableStateOf(false) })
    val coroutineScope = rememberCoroutineScope()
    
    val explorePage by viewModel.explorePage.collectAsState()
    val newReleaseAlbums = explorePage?.newReleaseAlbums

    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    if (newReleaseAlbums == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularWavyProgressIndicator()
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = 12.dp,
                end = 12.dp,
                bottom = 12.dp + contentPadding.calculateBottomPadding()
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = newReleaseAlbums.distinctBy { it.id },
                key = { it.id }
            ) { album ->
                YouTubeGridItem(
                    item = album,
                    isActive = mediaMetadata?.album?.id == album.id,
                    isPlaying = isPlaying,
                    coroutineScope = coroutineScope,
                    fillMaxWidth = true,
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                navController.navigate("album/${album.id}")
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    YouTubeAlbumMenu(
                                        albumItem = album,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        )
                )
            }
        }
    }
}
