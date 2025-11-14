package com.music.vivi.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.innertube.models.*
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.constants.SuggestionItemHeight
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.SearchBarIconOffsetX
import com.music.vivi.ui.component.YouTubeListItem
import com.music.vivi.ui.menu.*
import com.music.vivi.viewmodels.OnlineSearchSuggestionViewModel
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.graphics.Color
import com.music.vivi.R
import com.music.vivi.constants.ListItemHeight
import androidx.compose.foundation.shape.RoundedCornerShape
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchScreen(
    query: String,
    onQueryChange: (TextFieldValue) -> Unit,
    navController: NavController,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    pureBlack: Boolean,
    viewModel: OnlineSearchSuggestionViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val scope = rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val viewState by viewModel.viewState.collectAsState()

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect {
                keyboardController?.hide()
            }
    }

    LaunchedEffect(query) {
        viewModel.query.value = query
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        items(viewState.history, key = { "history_${it.query}" }) { history ->
            SuggestionItem(
                query = history.query,
                online = false,
                onClick = {
                    onSearch(history.query)
                    onDismiss()
                },
                onDelete = {
                    database.query {
                        delete(history)
                    }
                },
                onFillTextField = {
                    onQueryChange(TextFieldValue(history.query, TextRange(history.query.length)))
                },
                modifier = Modifier.animateItem(),
                pureBlack = pureBlack
            )
        }

        items(viewState.suggestions, key = { "suggestion_$it" }) { query ->
            SuggestionItem(
                query = query,
                online = true,
                onClick = {
                    onSearch(query)
                    onDismiss()
                },
                onFillTextField = {
                    onQueryChange(TextFieldValue(query, TextRange(query.length)))
                },
                modifier = Modifier.animateItem(),
                pureBlack = pureBlack
            )
        }

        if (viewState.items.isNotEmpty() && viewState.history.size + viewState.suggestions.size > 0) {
            item(key = "search_divider") {
                HorizontalDivider(
                    modifier = Modifier.animateItem()
                )
            }
        }

        // Replace the items section in OnlineSearchScreen with this:

// Group items in a container with curves
        if (viewState.items.isNotEmpty()) {
            item(key = "items_container") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    viewState.items.distinctBy { it.id }.forEachIndexed { index, item ->
                        val isFirst = index == 0
                        val isLast = index == viewState.items.distinctBy { it.id }.size - 1
                        val isSingleItem = viewState.items.distinctBy { it.id }.size == 1

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = if (isFirst) 20.dp else 0.dp,
                                        topEnd = if (isFirst) 20.dp else 0.dp,
                                        bottomStart = if (isLast && !isSingleItem) 20.dp else 0.dp,
                                        bottomEnd = if (isLast && !isSingleItem) 20.dp else 0.dp
                                    )
                                )
                                .background(
                                    when {
                                        item is SongItem && mediaMetadata?.id == item.id ->
                                            MaterialTheme.colorScheme.secondaryContainer
                                        item is AlbumItem && mediaMetadata?.album?.id == item.id ->
                                            MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceContainer
                                    }
                                )
                        ) {
                            YouTubeListItem(
                                item = item,
                                isActive = when (item) {
                                    is SongItem -> mediaMetadata?.id == item.id
                                    is AlbumItem -> mediaMetadata?.album?.id == item.id
                                    else -> false
                                },
                                isPlaying = isPlaying,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                when (item) {
                                                    is SongItem -> YouTubeSongMenu(
                                                        song = item,
                                                        navController = navController,
                                                        onDismiss = {
                                                            menuState.dismiss()
                                                            onDismiss()
                                                        }
                                                    )
                                                    is AlbumItem -> YouTubeAlbumMenu(
                                                        albumItem = item,
                                                        navController = navController,
                                                        onDismiss = {
                                                            menuState.dismiss()
                                                            onDismiss()
                                                        }
                                                    )
                                                    is ArtistItem -> YouTubeArtistMenu(
                                                        artist = item,
                                                        onDismiss = {
                                                            menuState.dismiss()
                                                            onDismiss()
                                                        }
                                                    )
                                                    is PlaylistItem -> YouTubePlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = scope,
                                                        onDismiss = {
                                                            menuState.dismiss()
                                                            onDismiss()
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .combinedClickable(
                                        onClick = {
                                            when (item) {
                                                is SongItem -> {
                                                    if (item.id == mediaMetadata?.id) {
                                                        playerConnection.player.togglePlayPause()
                                                    } else {
                                                        playerConnection.playQueue(
                                                            YouTubeQueue.radio(item.toMediaMetadata())
                                                        )
                                                        onDismiss()
                                                    }
                                                }
                                                is AlbumItem -> {
                                                    navController.navigate("album/${item.id}")
                                                    onDismiss()
                                                }
                                                is ArtistItem -> {
                                                    navController.navigate("artist/${item.id}")
                                                    onDismiss()
                                                }
                                                is PlaylistItem -> {
                                                    navController.navigate("online_playlist/${item.id}")
                                                    onDismiss()
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                when (item) {
                                                    is SongItem -> YouTubeSongMenu(
                                                        song = item,
                                                        navController = navController,
                                                        onDismiss = {
                                                            menuState.dismiss()
                                                            onDismiss()
                                                        }
                                                    )
                                                    is AlbumItem -> YouTubeAlbumMenu(
                                                        albumItem = item,
                                                        navController = navController,
                                                        onDismiss = {
                                                            menuState.dismiss()
                                                            onDismiss()
                                                        }
                                                    )
                                                    is ArtistItem -> YouTubeArtistMenu(
                                                        artist = item,
                                                        onDismiss = {
                                                            menuState.dismiss()
                                                            onDismiss()
                                                        }
                                                    )
                                                    is PlaylistItem -> YouTubePlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = {
                                                            menuState.dismiss()
                                                            onDismiss()
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    )
                            )
                        }

                        // Add 3dp spacer between items (except after last)
                        if (!isLast) {
                            Spacer(modifier = Modifier.height(3.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionItem(
    modifier: Modifier = Modifier,
    query: String,
    online: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onFillTextField: () -> Unit,
    pureBlack: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(SuggestionItemHeight)
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(end = SearchBarIconOffsetX)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        Icon(
            painterResource(if (online) R.drawable.search else R.drawable.history),
            contentDescription = null,
            modifier = Modifier.padding(horizontal = 16.dp).alpha(0.5f)
        )

        Text(
            text = query,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (!online) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.alpha(0.5f),
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                )
            }
        }

        IconButton(
            onClick = onFillTextField,
            modifier = Modifier.alpha(0.5f),
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_top_left),
                contentDescription = null,
            )
        }
    }
}
