/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.ctrlfxx.ctrlmusic.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ctrlfxx.ctrlmusic.LocalPlayerAwareWindowInsets
import com.ctrlfxx.ctrlmusic.LocalPlayerConnection
import com.ctrlfxx.ctrlmusic.R
import com.ctrlfxx.ctrlmusic.constants.CONTENT_TYPE_HEADER
import com.ctrlfxx.ctrlmusic.constants.CONTENT_TYPE_SONG
import com.ctrlfxx.ctrlmusic.constants.HideExplicitKey
import com.ctrlfxx.ctrlmusic.constants.SongFilter
import com.ctrlfxx.ctrlmusic.constants.SongFilterKey
import com.ctrlfxx.ctrlmusic.constants.SongSortDescendingKey
import com.ctrlfxx.ctrlmusic.constants.SongSortType
import com.ctrlfxx.ctrlmusic.constants.SongSortTypeKey
import com.ctrlfxx.ctrlmusic.constants.YtmSyncKey
import com.ctrlfxx.ctrlmusic.extensions.toMediaItem
import com.ctrlfxx.ctrlmusic.playback.queues.ListQueue
import com.ctrlfxx.ctrlmusic.ui.component.ChipsRow
import com.ctrlfxx.ctrlmusic.ui.component.HideOnScrollFAB
import com.ctrlfxx.ctrlmusic.ui.component.LocalMenuState
import com.ctrlfxx.ctrlmusic.ui.component.SongListItem
import com.ctrlfxx.ctrlmusic.ui.component.SortHeader
import com.ctrlfxx.ctrlmusic.ui.menu.SongMenu
import com.ctrlfxx.ctrlmusic.utils.listItemShape
import com.ctrlfxx.ctrlmusic.utils.rememberEnumPreference
import com.ctrlfxx.ctrlmusic.utils.rememberPreference
import com.ctrlfxx.ctrlmusic.viewmodels.LibrarySongsViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibrarySongsScreen(
    navController: NavController,
    onDeselect: () -> Unit,
    viewModel: LibrarySongsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        SongSortTypeKey,
        SongSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val songs by viewModel.allSongs.collectAsState()

    var filter by rememberEnumPreference(SongFilterKey, SongFilter.LIKED)

    LaunchedEffect(Unit) {
        if (ytmSync) {
            when (filter) {
                SongFilter.LIKED -> viewModel.syncLikedSongs()
                SongFilter.LIBRARY -> viewModel.syncLibrarySongs()
                else -> return@LaunchedEffect
            }
        }
    }

    val lazyListState = rememberLazyListState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val filteredSongs = if (hideExplicit) {
        songs.filter { !it.song.explicit }
    } else {
        songs
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            item(
                key = "filter",
                contentType = CONTENT_TYPE_HEADER,
            ) {
                Row {
                    Spacer(Modifier.width(12.dp))
                    FilterChip(
                        label = { Text(stringResource(R.string.songs)) },
                        selected = true,
                        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
                        onClick = onDeselect,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = ""
                            )
                        },
                        modifier = Modifier.height(35.dp)
                    )
                    ChipsRow(
                        chips =
                        listOf(
                            SongFilter.LIKED to stringResource(R.string.filter_liked),
                            SongFilter.LIBRARY to stringResource(R.string.filter_library),
                            SongFilter.DOWNLOADED to stringResource(R.string.filter_downloaded),
                        ),
                        currentValue = filter,
                        onValueUpdate = {
                            filter = it
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item(
                key = "header",
                contentType = CONTENT_TYPE_HEADER,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.n_song,
                            filteredSongs.size,
                            filteredSongs.size
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )

                    Spacer(Modifier.weight(1f))
                    
                    com.ctrlfxx.ctrlmusic.ui.component.SortDropdownMenu(
                        sortType = sortType,
                        sortDescending = sortDescending,
                        onSortTypeChange = onSortTypeChange,
                        onSortDescendingChange = onSortDescendingChange,
                        sortTypeText = { sortType ->
                            when (sortType) {
                                SongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                SongSortType.NAME -> R.string.sort_by_name
                                SongSortType.ARTIST -> R.string.sort_by_artist
                                SongSortType.PLAY_TIME -> R.string.sort_by_play_time
                            }
                        },
                    )
                }
            }

            itemsIndexed(
                items = filteredSongs,
                key = { _, item -> item.song.id },
                contentType = { _, _ -> CONTENT_TYPE_SONG },
            ) { index, song ->
                SongListItem(
                    song = song,
                    showInLibraryIcon = true,
                    isActive = song.id == mediaMetadata?.id,
                    isPlaying = isPlaying,
                    showLikedIcon = true,
                    showDownloadIcon = filter != SongFilter.DOWNLOADED,
                    shape = listItemShape(index, filteredSongs.size),
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    SongMenu(
                                        originalSong = song,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (song.id == mediaMetadata?.id) {
                                playerConnection.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = context.getString(R.string.queue_all_songs),
                                        items = filteredSongs.map { it.toMediaItem() },
                                        startIndex = index,
                                    ),
                                )
                            }
                        }
                        .animateItem(),
                )
            }
        }

        HideOnScrollFAB(
            visible = filteredSongs.isNotEmpty(),
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            onClick = {
                playerConnection.playQueue(
                    ListQueue(
                        title = context.getString(R.string.queue_all_songs),
                        items = filteredSongs.shuffled().map { it.toMediaItem() },
                    ),
                )
            },
        )
    }
}
