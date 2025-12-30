package com.music.vivi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.innertube.utils.parseCookieString
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.HistorySource
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.db.entities.EventWithSong
import com.music.vivi.extensions.metadata
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.ChipsRow

import com.music.vivi.ui.component.HideOnScrollFAB
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.SongListItem
import com.music.vivi.ui.component.YouTubeListItem
import com.music.vivi.ui.menu.SelectionMediaMetadataMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.menu.YouTubeSongMenu
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.DateAgo
import com.music.vivi.viewmodels.HistoryViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val selectedEventIds = remember { mutableStateListOf<Long>() }

    var selection by remember {
        mutableStateOf(false)
    }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }
    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (selection) {
        BackHandler {
            selection = false
            selectedEventIds.clear()
        }
    }

    val historySource by viewModel.historySource.collectAsState()

    val historyPage by viewModel.historyPage.collectAsState()
    
    val events by viewModel.events.collectAsState()

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    fun dateAgoToString(dateAgo: DateAgo): String {
        return when (dateAgo) {
            DateAgo.Today -> context.getString(R.string.today)
            DateAgo.Yesterday -> context.getString(R.string.yesterday)
            DateAgo.ThisWeek -> context.getString(R.string.this_week)
            DateAgo.LastWeek -> context.getString(R.string.last_week)
            is DateAgo.Other -> dateAgo.date.format(DateTimeFormatter.ofPattern("yyyy/MM"))
        }
    }


    val filteredEvents = remember(events, query) {
        if (query.text.isEmpty()) {
            events
        } else {
            events.mapValues { (_, songs) ->
                songs.filter { event ->
                    event.song.song.title.contains(query.text, ignoreCase = true) ||
                            event.song.artists.any {
                                it.name.contains(
                                    query.text,
                                    ignoreCase = true
                                )
                            }
                }
            }.filterValues { it.isNotEmpty() }
        }
    }

    val filteredRemoteContent = remember(historyPage, query) {
        if (query.text.isEmpty()) {
            historyPage?.sections
        } else {
            historyPage?.sections?.map { section ->
                section.copy(
                    songs = section.songs.filter { song ->
                        song.title.contains(query.text, ignoreCase = true) ||
                                song.artists.any { it.name.contains(query.text, ignoreCase = true) }
                    }
                )
            }?.filter { it.songs.isNotEmpty() }
        }
    }



    val lazyListState = rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .asPaddingValues(),
            modifier = Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        ) {
//            item(key = "chips_row") {
//                ChipsRow(
//                    chips = if (isLoggedIn) listOf(
//                        HistorySource.LOCAL to stringResource(R.string.local_history),
//                        HistorySource.REMOTE to stringResource(R.string.remote_history),
//                    ) else {
//                        listOf(HistorySource.LOCAL to stringResource(R.string.local_history))
//                    },
//                    currentValue = historySource,
//                    onValueUpdate = {
//                        viewModel.historySource.value = it
//                        if (it == HistorySource.REMOTE){
//                            viewModel.fetchRemoteHistory()
//                        }
//                    }
//                )
//            }

            item(key = "chips_row") {
                val selectedChip = historySource

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chips = if (isLoggedIn) listOf(
                        HistorySource.LOCAL to stringResource(R.string.local_history),
                        HistorySource.REMOTE to stringResource(R.string.remote_history),
                    ) else {
                        listOf(HistorySource.LOCAL to stringResource(R.string.local_history))
                    }

                    chips.forEach { (source, label) ->
                        val isSelected = selectedChip == source

                        // Animate the corner radius based on selection
                        val cornerRadius by animateDpAsState(
                            targetValue = if (isSelected) 20.dp else 8.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "corner_radius"
                        )

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.historySource.value = source
                                if (source == HistorySource.REMOTE) {
                                    viewModel.fetchRemoteHistory()
                                }
                            },
                            label = { Text(label) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Done,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    )
                                }
                            } else {
                                null
                            },
                            shape = RoundedCornerShape(cornerRadius),
                            modifier = Modifier.animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        )
                    }
                }
            }

            if (historySource == HistorySource.REMOTE && isLoggedIn) {
                filteredRemoteContent?.forEach { section ->
                    stickyHeader {
                        NavigationTitle(
                            title = section.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        )
                    }

                    items(
                        items = section.songs,
                        key = { "${section.title}_${it.id}_${section.songs.indexOf(it)}" }
                    ) { song ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            val index = section.songs.indexOf(song)
                            val isFirst = index == 0
                            val isLast = index == section.songs.size - 1
                            val isSingleSong = section.songs.size == 1

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(ListItemHeight)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = if (isFirst) 20.dp else 0.dp,
                                            topEnd = if (isFirst) 20.dp else 0.dp,
                                            bottomStart = if (isLast && !isSingleSong) 20.dp else 0.dp,
                                            bottomEnd = if (isLast && !isSingleSong) 20.dp else 0.dp
                                        )
                                    )
                                    .background(
                                        if (song.id == mediaMetadata?.id) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainer
                                    )
                            ) {
                                YouTubeListItem(
                                    item = song,
                                    isActive = song.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                        onHistoryRemoved = {
                                                            viewModel.fetchRemoteHistory()
                                                        }
                                                    )
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
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue.radio(song.toMediaMetadata())
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                        onHistoryRemoved = {
                                                            viewModel.fetchRemoteHistory()
                                                        }
                                                    )
                                                }
                                            }
                                        )
                                )
                            }
                            if (!isLast) {
                                Spacer(modifier = Modifier.height(3.dp))
                            } else {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            } else {
                filteredEvents.forEach { (dateAgo, events) ->
                    stickyHeader {
                        NavigationTitle(
                            title = dateAgoToString(dateAgo),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        )
                    }

                    itemsIndexed(
                        items = events,
                        key = { index, event -> "${dateAgo}_${event.event.id}_$index" }
                    ) { index, event ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            val isFirst = index == 0
                            val isLast = index == events.size - 1
                            val isActive = event.song.id == mediaMetadata?.id
                            val isSingleSong = events.size == 1
                            val isSelected = selectedEventIds.contains(event.event.id)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(ListItemHeight)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = if (isFirst) 20.dp else 0.dp,
                                            topEnd = if (isFirst) 20.dp else 0.dp,
                                            bottomStart = if (isLast && !isSingleSong) 20.dp else 0.dp,
                                            bottomEnd = if (isLast && !isSingleSong) 20.dp else 0.dp
                                        )
                                    )
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainer
                                    )
                            ) {
                                SongListItem(
                                    song = event.song,
                                    isActive = isActive,
                                    isPlaying = isPlaying,
                                    showInLibraryIcon = true,
                                    isSelected = isSelected && selection,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                if (!selection) {
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = event.song,
                                                            event = event.event,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss
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
                                                if (!selection) {
                                                    if (event.song.id == mediaMetadata?.id) {
                                                        playerConnection.player.togglePlayPause()
                                                    } else {
                                                        playerConnection.playQueue(
                                                            ListQueue(
                                                                title = dateAgoToString(dateAgo),
                                                                items = events.map { it.song.toMediaItem() },
                                                                startIndex = index
                                                            )
                                                        )
                                                    }
                                                } else {
                                                    if (isSelected) {
                                                        selectedEventIds.remove(event.event.id)
                                                    } else {
                                                        selectedEventIds.add(event.event.id)
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                if (!selection) {
                                                    selection = true
                                                    selectedEventIds.clear()
                                                    selectedEventIds.add(event.event.id)
                                                }
                                            }
                                        )
                                )
                            }

                            // Add 3dp spacer between items (except after last)
                            if (!isLast) {
                                Spacer(modifier = Modifier.height(3.dp))
                            } else {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }

        HideOnScrollFAB(
            visible = if (historySource == HistorySource.REMOTE) {
                filteredRemoteContent?.any { it.songs.isNotEmpty() } == true
            } else {
                filteredEvents.isNotEmpty()
            },
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            onClick = {
                if (historySource == HistorySource.REMOTE && historyPage != null) {
                    val songs = filteredRemoteContent?.flatMap { it.songs } ?: emptyList()
                    if (songs.isNotEmpty()) {
                        playerConnection.playQueue(
                            ListQueue(
                                title = context.getString(R.string.history),
                                items = songs.map { it.toMediaItem() }.shuffled()
                            )
                        )
                    }
                } else {
                    playerConnection.playQueue(
                        ListQueue(
                            title = context.getString(R.string.history),
                            items = filteredEvents.values.flatten().map { it.song.toMediaItem() }.shuffled()
                        )
                    )
                }
            }
        )
        

    }

    TopAppBar(
        title = {
            if (selection) {
                val count = selectedEventIds.size
                Text(
                    text = pluralStringResource(R.plurals.n_song, count, count),
                    style = MaterialTheme.typography.titleLarge
                )
            } else if (isSearching) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            } else {
                Text(stringResource(R.string.history))
            }
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    when {
                        isSearching -> {
                            isSearching = false
                            query = TextFieldValue()
                        }

                        selection -> {
                            selection = false
                            selectedEventIds.clear()
                        }

                        else -> {
                            navController.navigateUp()
                        }
                    }
                },
                onLongClick = {
                    if (!isSearching && !selection) {
                        navController.backToMain()
                    }
                }
            ) {
                Icon(
                    painter = painterResource(
                        if (selection) R.drawable.close else R.drawable.arrow_back
                    ),
                    contentDescription = null
                )
            }
        },
        actions = {
            if (selection) {
                val count = selectedEventIds.size
                val totalCount = filteredEvents.values.sumOf { it.size }
                IconButton(
                    onClick = {
                        if (count == totalCount) {
                            selectedEventIds.clear()
                        } else {
                            selectedEventIds.clear()
                            selectedEventIds.addAll(filteredEvents.values.flatten().map { it.event.id })
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (count == totalCount) R.drawable.deselect else R.drawable.select_all
                        ),
                        contentDescription = null
                    )
                }
                IconButton(
                    onClick = {
                        menuState.show {
                            SelectionMediaMetadataMenu(
                                songSelection = filteredEvents.values.flatten()
                                    .filter { selectedEventIds.contains(it.event.id) }
                                    .map { it.song.toMediaItem().metadata!! },
                                onDismiss = menuState::dismiss,
                                clearAction = {
                                    selection = false
                                    selectedEventIds.clear()
                                },
                                currentItems = emptyList()
                            )
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null
                    )
                }
            } else if (!isSearching) {
                IconButton(
                    onClick = { isSearching = true }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null
                    )
                }
            }
        }
    )
}
