/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.recognition

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.db.entities.RecognitionHistory
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.ItemThumbnail
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.listItemShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecognitionHistoryScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()

    val historyItems by database.recognitionHistory().collectAsState(initial = emptyList())
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }
    val isPlaying by playerConnection?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

    var showClearDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<RecognitionHistory?>(null) }

    fun playHistoryItem(item: RecognitionHistory) {
        val videoId = item.youtubeVideoId
        if (videoId != null && playerConnection != null) {
            playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = videoId)))
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            val query = "${item.title} ${item.artist}"
            val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
            val songs = searchResult?.items?.filterIsInstance<SongItem>().orEmpty()
            val bestMatch = songs.firstOrNull { s ->
                s.title.contains(item.title, ignoreCase = true) &&
                s.artists.any { a -> a.name.contains(item.artist, ignoreCase = true) }
            } ?: songs.firstOrNull()

            withContext(Dispatchers.Main) {
                if (bestMatch != null && playerConnection != null) {
                    playerConnection.playQueue(
                        YouTubeQueue(
                            WatchEndpoint(videoId = bestMatch.id),
                            bestMatch.toMediaMetadata()
                        )
                    )
                } else {
                    navController.navigate("search/${java.net.URLEncoder.encode(query, "UTF-8")}")
                }
            }
        }
    }

    // Search state
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    // Filtered list - client-side, matches title or artist
    val filteredItems = remember(historyItems, query) {
        if (query.text.isEmpty()) historyItems
        else historyItems.filter { item ->
            item.title.contains(query.text, ignoreCase = true) ||
                item.artist.contains(query.text, ignoreCase = true)
        }
    }

    // Group by date label matching HistoryScreen (Today / Yesterday / This Week / Last Week / yyyy/MM)
    val groupedItems = remember(filteredItems) {
        val today = LocalDate.now()
        filteredItems.groupBy { item ->
            val date = item.recognizedAt.toLocalDate()
            when {
                date == today -> context.getString(R.string.today)
                date == today.minusDays(1) -> context.getString(R.string.yesterday)
                date >= today.minusDays(7) -> context.getString(R.string.this_week)
                date >= today.minusDays(14) -> context.getString(R.string.last_week)
                else -> item.recognizedAt.format(DateTimeFormatter.ofPattern("yyyy/MM"))
            }
        }
    }

    if (showClearDialog) {
        DefaultDialog(
            onDismiss = { showClearDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.clear_recognition_history)) },
            buttons = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            database.query {
                                clearRecognitionHistory()
                            }
                        }
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(R.string.clear))
                }
            }
        ) {
            Text(
                text = stringResource(R.string.clear_recognition_history_confirm),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    itemToDelete?.let { item ->
        DefaultDialog(
            onDismiss = { itemToDelete = null },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.delete)) },
            buttons = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            database.query {
                                deleteRecognitionHistoryById(item.id)
                            }
                        }
                        itemToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            }
        ) {
            Text(
                text = stringResource(R.string.delete_playlist_confirm, item.title),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    Scaffold(
        contentWindowInsets = LocalPlayerAwareWindowInsets.current,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recognition_history)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        onLongClick = { navController.backToMain() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    if (historyItems.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.clear_all),
                                contentDescription = stringResource(R.string.clear_recognition_history)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Static search bar ──────────────────────────────────
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (query.text.isNotEmpty()) {
                        IconButton(onClick = { query = TextFieldValue() }) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor  = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ── List / empty state ─────────────────────────────────
            when {
                historyItems.isEmpty() -> {
                    // No history at all
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.history),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No recognition history",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                filteredItems.isEmpty() -> {
                    // History exists but query matches nothing
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No results for \"${query.text}\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (query.text.isEmpty()) {
                            // Grouped by date - regular scrolling headers matching HistoryScreen
                            groupedItems.forEach { (label, groupItems) ->
                                item(key = "header_$label") {
                                    NavigationTitle(
                                        title = label,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface)
                                            .animateItem()
                                    )
                                }
                                itemsIndexed(
                                    items = groupItems,
                                    key = { _, item -> item.id }
                                ) { index, item ->
                                    val isActive = (item.youtubeVideoId != null && item.youtubeVideoId == mediaMetadata?.id) ||
                                        (mediaMetadata != null &&
                                            mediaMetadata?.title.equals(item.title, ignoreCase = true) &&
                                            mediaMetadata?.artists?.any { it.name.contains(item.artist, ignoreCase = true) || item.artist.contains(it.name, ignoreCase = true) } == true)

                                    RecognitionHistoryItem(
                                        item = item,
                                        shape = listItemShape(index, groupItems.size),
                                        isActive = isActive,
                                        isPlaying = isPlaying,
                                        onClick = {
                                            if (isActive) {
                                                playerConnection?.togglePlayPause()
                                            } else {
                                                playHistoryItem(item)
                                            }
                                        },
                                        onMoreClick = { itemToDelete = item }
                                    )
                                }
                            }
                        } else {
                            // Active search → flat list, no date headers
                            itemsIndexed(
                                items = filteredItems,
                                key = { _, item -> item.id }
                            ) { index, item ->
                                val isActive = (item.youtubeVideoId != null && item.youtubeVideoId == mediaMetadata?.id) ||
                                    (mediaMetadata != null &&
                                        mediaMetadata?.title.equals(item.title, ignoreCase = true) &&
                                        mediaMetadata?.artists?.any { it.name.contains(item.artist, ignoreCase = true) || item.artist.contains(it.name, ignoreCase = true) } == true)

                                RecognitionHistoryItem(
                                    item = item,
                                    shape = listItemShape(index, filteredItems.size),
                                    isActive = isActive,
                                    isPlaying = isPlaying,
                                    onClick = {
                                        if (isActive) {
                                            playerConnection?.togglePlayPause()
                                        } else {
                                            playHistoryItem(item)
                                        }
                                    },
                                    onMoreClick = { itemToDelete = item }
                                )
                            }
                        }
                        item {
                            Spacer(Modifier.height(50.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecognitionHistoryItem(
    item: RecognitionHistory,
    shape: Shape,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm") }

    Surface(
        shape = shape,
        color = if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(shape)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art with smooth rounded corners and active playing indicator overlay
            ItemThumbnail(
                thumbnailUrl = item.coverArtHqUrl ?: item.coverArtUrl,
                isActive = isActive,
                isPlaying = isPlaying,
                shape = RoundedCornerShape(ThumbnailCornerRadius),
                modifier = Modifier.size(52.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Track info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.recognizedAt.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // More / options action on the right side
            IconButton(onClick = onMoreClick) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
