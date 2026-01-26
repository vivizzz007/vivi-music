package com.music.vivi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import com.music.vivi.db.entities.EventWithSong
import com.music.vivi.extensions.metadata
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.ui.component.HideOnScrollFAB
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.RoundedCheckbox
import com.music.vivi.ui.component.history.HistoryFilterChips
import com.music.vivi.ui.component.history.localHistoryList
import com.music.vivi.ui.component.history.remoteHistoryList
import com.music.vivi.ui.menu.SelectionMediaMetadataMenu
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.DateAgo
import com.music.vivi.viewmodels.HistoryViewModel
import java.time.format.DateTimeFormatter

/**
 * Screen displaying playback history.
 *
 * Features:
 * - Toggle between Local (App DB) and Remote (YouTube Account) history.
 * - Search/Filter within history.
 * - Multi-select to create playlists or delete entries.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
public fun HistoryScreen(navController: NavController, viewModel: HistoryViewModel = hiltViewModel()) {
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

    val historySource: com.music.vivi.constants.HistorySource by viewModel.historySource.collectAsState()

    val historyPage: com.music.innertube.pages.HistoryPage? by viewModel.historyPage.collectAsState()

    val events: Map<com.music.vivi.viewmodels.DateAgo, List<com.music.vivi.db.entities.EventWithSong>> by viewModel.events.collectAsState()

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    fun dateAgoToString(dateAgo: DateAgo): String = when (dateAgo) {
        DateAgo.Today -> context.getString(R.string.today)
        DateAgo.Yesterday -> context.getString(R.string.yesterday)
        DateAgo.ThisWeek -> context.getString(R.string.this_week)
        DateAgo.LastWeek -> context.getString(R.string.last_week)
        is DateAgo.Other -> dateAgo.date.format(DateTimeFormatter.ofPattern("yyyy/MM"))
    }

    val filteredEvents: Map<DateAgo, List<EventWithSong>> = remember(events, query) {
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
            contentPadding = LocalPlayerAwareWindowInsets.current.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
            )
                .asPaddingValues(),
            modifier = Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        ) {
            item(key = "chips_row") {
                HistoryFilterChips(
                    historySource = historySource,
                    isLoggedIn = isLoggedIn,
                    onSourceSelected = { source ->
                        viewModel.historySource.value = source
                        if (source == HistorySource.REMOTE) {
                            viewModel.fetchRemoteHistory()
                        }
                    }
                )
            }

            if (historySource == HistorySource.REMOTE && isLoggedIn) {
                remoteHistoryList(
                    filteredRemoteContent = filteredRemoteContent,
                    mediaMetadata = mediaMetadata,
                    isPlaying = isPlaying,
                    playerConnection = playerConnection,
                    navController = navController,
                    menuState = menuState,
                    onHistoryRemoved = { viewModel.fetchRemoteHistory() }
                )
            } else {
                localHistoryList(
                    filteredEvents = filteredEvents,
                    mediaMetadata = mediaMetadata,
                    isPlaying = isPlaying,
                    selection = selection,
                    selectedEventIds = selectedEventIds,
                    onSelectionChange = { selection = it },
                    playerConnection = playerConnection,
                    navController = navController,
                    menuState = menuState,
                    haptic = haptic,
                    dateAgoToString = ::dateAgoToString
                )
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
                        disabledIndicatorColor = Color.Transparent
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
                RoundedCheckbox(
                    checked = count == totalCount,
                    onCheckedChange = { checked ->
                        if (checked) {
                            selectedEventIds.clear()
                            selectedEventIds.addAll(filteredEvents.values.flatten().map { it.event.id })
                        } else {
                            selectedEventIds.clear()
                        }
                    },
                    modifier = Modifier.padding(end = 12.dp)
                )
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
