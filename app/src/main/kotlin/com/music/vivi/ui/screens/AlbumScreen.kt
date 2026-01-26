package com.music.vivi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.album.AlbumTopBar
import com.music.vivi.ui.component.album.albumRelatedContent
import com.music.vivi.ui.component.album.albumTrackList
import com.music.vivi.ui.component.media.albums.AlbumHeader
import com.music.vivi.ui.utils.ItemWrapper
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.AlbumViewModel

/**
 * Screen for displaying a single Album's details and tracks.
 *
 * Features:
 * - Parallax Header with Album Art.
 * - Track list with download status integration.
 * - "Other Versions" and "Releases for You" sections.
 * - Multi-select support for tracks.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
public fun AlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    albumId: String? = null,
    viewModel: AlbumViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {},
    onBack: () -> Unit = { navController.navigateUp() },
) {
    if (albumId != null) {
        LaunchedEffect(albumId) {
            viewModel.setAlbumId(albumId)
        }
    }
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    var shapeIndex by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlistId by viewModel.playlistId.collectAsState()
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val otherVersions by viewModel.otherVersions.collectAsState()
    val releasesForYou by viewModel.releasesForYou.collectAsState()
    val albumDescription by viewModel.albumDescription.collectAsState()
    val isDescriptionLoading by viewModel.isDescriptionLoading.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val wrappedSongs = remember(albumWithSongs, hideExplicit) {
        val filteredSongs = if (hideExplicit) {
            albumWithSongs?.songs?.filter { !it.song.explicit } ?: emptyList()
        } else {
            albumWithSongs?.songs ?: emptyList()
        }
        filteredSongs.map { item -> ItemWrapper(item) }.toMutableStateList()
    }

    var selection by remember {
        mutableStateOf(false)
    }

    if (selection) {
        BackHandler {
            selection = false
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(albumWithSongs) {
        val songs = albumWithSongs?.songs?.map { it.id }
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it]?.state == Download.STATE_QUEUED ||
                            downloads[it]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    val lazyListState = rememberLazyListState()

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 200
        }
    }

    // Check if any song is explicit
    val hasExplicitContent = remember(albumWithSongs) {
        albumWithSongs?.songs?.any { it.song.explicit } == true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            modifier = Modifier.fillMaxSize()
        ) {
            val albumWithSongs = albumWithSongs
            if (albumWithSongs != null && albumWithSongs.songs.isNotEmpty()) {
                item(key = "album_header") {
                    AlbumHeader(
                        albumWithSongs = albumWithSongs,
                        downloadState = downloadState,
                        playlistId = playlistId,
                        albumDescription = albumDescription,
                        isDescriptionLoading = isDescriptionLoading,
                        navController = navController
                    )
                }

                // Replace the songs list section in your AlbumScreen with this modified version:

// Songs List with Quick Pick style
                // Songs List with Quick Pick style
                if (true) {
                    albumTrackList(
                        wrappedSongs = wrappedSongs,
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        selection = selection,
                        onSelectionStart = { selection = true },
                        playlistId = playlistId,
                        albumWithSongs = albumWithSongs,
                        playerConnection = playerConnection,
                        navController = navController,
                        menuState = menuState,
                        haptic = haptic
                    )
                }

                // Other Versions Section
                albumRelatedContent(
                    otherVersions = otherVersions,
                    releasesForYou = releasesForYou,
                    mediaMetadata = mediaMetadata,
                    isPlaying = isPlaying,
                    scope = scope,
                    navController = navController,
                    menuState = menuState,
                    haptic = haptic
                )
            } else {
                // Loading indicator
                item(key = "loading_indicator") {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(bottom = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator()
                    }
                }
            }
        }

        // Top App Bar
        AlbumTopBar(
            selection = selection,
            wrappedSongs = wrappedSongs,
            onSelectionChange = { selection = it },
            onSelectAll = { wrappedSongs.forEach { it.isSelected = true } },
            onDeselectAll = { wrappedSongs.forEach { it.isSelected = false } },
            onBack = onBack,
            navController = navController,
            menuState = menuState,
            transparentAppBar = transparentAppBar,
            scrollBehavior = scrollBehavior
        )
    }
}
