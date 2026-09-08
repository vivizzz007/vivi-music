/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.spotify.SpotifyMapper
import com.music.spotify.models.SpotifyTrack
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.shimmer.ListItemPlaceHolder
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.makeTimeString
import com.music.vivi.viewmodels.SpotifyImportViewModel
import com.music.vivi.viewmodels.SpotifyPlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyPlaylistDetailScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: SpotifyPlaylistViewModel = hiltViewModel(),
    importViewModel: SpotifyImportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val importProgress by importViewModel.importProgress.collectAsStateWithLifecycle()
    val playerConnection = LocalPlayerConnection.current
    val listState = rememberLazyListState()

    // Trigger load more when near the end of list
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 10
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.isLoadingMore && state.tracks.size < state.totalTracks) {
            viewModel.loadMoreTracks()
        }
    }

    val showTopBarTitle by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = showTopBarTitle,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Text(
                            text = state.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            importViewModel.startImport(listOf(state.playlistId))
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.bookmark_star_library),
                            contentDescription = stringResource(R.string.import_playlist),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            contentPadding = paddingValues,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
        ) {
            // Header: Cover Art, Title, Subtitle, Play & Import Action Buttons
            item(key = "header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (viewModel.isLikedSongs) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (viewModel.isLikedSongs) {
                            Icon(
                                painter = painterResource(R.drawable.favorite),
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        } else if (!state.coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = state.coverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.music_note),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = state.title.ifBlank { "Spotify Playlist" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val subtitleText = buildString {
                        if (!state.ownerName.isNullOrBlank()) {
                            append(state.ownerName)
                            append(" • ")
                        }
                        if (state.totalTracks > 0) {
                            append(state.totalTracks)
                            append(" songs")
                        }
                    }

                    if (subtitleText.isNotBlank()) {
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                playerConnection?.let { pc ->
                                    viewModel.playAll(pc, shuffle = false)
                                }
                            },
                            enabled = state.tracks.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.play),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.play))
                        }

                        OutlinedButton(
                            onClick = {
                                playerConnection?.let { pc ->
                                    viewModel.playAll(pc, shuffle = true)
                                }
                            },
                            enabled = state.tracks.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.shuffle))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FilledTonalButton(
                        onClick = {
                            importViewModel.startImport(listOf(state.playlistId))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.bookmark_star_library),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.import_playlist))
                    }
                }
            }

            // Error display
            state.error?.let { err ->
                item(key = "error") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadPlaylist() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }

            // Songs List
            if (state.isLoading && state.tracks.isEmpty()) {
                items(8) {
                    ShimmerHost {
                        ListItemPlaceHolder()
                    }
                }
            } else {
                itemsIndexed(
                    items = state.tracks,
                    key = { index, track -> track.id.ifBlank { "track_$index" } }
                ) { index, track ->
                    SpotifyTrackRow(
                        index = index + 1,
                        track = track,
                        isResolving = state.resolvingTrackId == track.id,
                        onClick = {
                            playerConnection?.let { pc ->
                                viewModel.playTrack(track, pc)
                            }
                        }
                    )
                }

                if (state.isLoadingMore) {
                    item(key = "loading_more") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }

    // Import Progress Dialog
    importProgress?.let { progress ->
        val isFinished = progress.isFinished || progress.percent >= 1f
        DefaultDialog(
            onDismiss = {
                if (isFinished) {
                    importViewModel.dismissImportProgress()
                } else {
                    importViewModel.cancelImport()
                }
            },
            title = {
                Text(
                    text = if (isFinished) {
                        stringResource(R.string.spotify_import_complete)
                    } else {
                        stringResource(R.string.spotify_import_in_progress)
                    }
                )
            },
            buttons = {
                if (isFinished) {
                    Button(
                        onClick = { importViewModel.dismissImportProgress() },
                        shape = CircleShape
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                } else {
                    TextButton(onClick = { importViewModel.cancelImport() }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = progress.playlistName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isFinished) {
                        stringResource(R.string.spotify_imported_songs_count, progress.currentSongIndex, progress.totalSongs)
                    } else {
                        stringResource(R.string.spotify_importing_songs_count, progress.currentSongIndex, progress.totalSongs)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { progress.percent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SpotifyTrackRow(
    index: Int,
    track: SpotifyTrack,
    isResolving: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val thumbnailUrl = SpotifyMapper.getTrackThumbnail(track)
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            val artistName = track.artists.joinToString(", ") { it.name }
            Text(
                text = artistName.ifBlank { "Unknown Artist" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isResolving) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else if (track.durationMs > 0) {
            Text(
                text = makeTimeString(track.durationMs.toLong()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
