/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.spotify.models.SpotifyPlaylist
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.GridItemSize
import com.music.vivi.constants.GridItemsSizeKey
import com.music.vivi.constants.GridThumbnailHeight
import com.music.vivi.ui.component.ChipsRow
import com.music.vivi.ui.component.EmptyPlaceholder
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.shimmer.GridItemPlaceHolder
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.screens.settings.SpotifyPlaylistBottomSheet
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.viewmodels.SpotifyImportViewModel

private enum class SpotifyAccountContentType {
    PLAYLISTS, LIKED_SONGS
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SpotifyAccountScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: SpotifyImportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var selectedContentType by remember { mutableStateOf(SpotifyAccountContentType.PLAYLISTS) }
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    var showPlaylistSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (state.playlists.isEmpty()) {
            viewModel.loadSources()
        }
    }

    if (showPlaylistSheet) {
        SpotifyPlaylistBottomSheet(
            onDismiss = { showPlaylistSheet = false },
            viewModel = viewModel,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.accountName.isNotBlank()) state.accountName else stringResource(R.string.spotify_account),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                        onClick = { navController.navigate("settings/spotify") }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.settings),
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxSize()
        ) {
            // Profile Card
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!state.accountAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = state.accountAvatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.spotify),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.accountName.isNotBlank()) state.accountName else stringResource(R.string.spotify_account),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${state.playlists.size} playlists • ${state.likedSongsCount} liked songs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                ChipsRow(
                    chips = listOf(
                        SpotifyAccountContentType.PLAYLISTS to stringResource(R.string.spotify_playlists),
                        SpotifyAccountContentType.LIKED_SONGS to stringResource(R.string.spotify_liked_songs),
                    ),
                    currentValue = selectedContentType,
                    onValueUpdate = { selectedContentType = it },
                )
            }

            when (selectedContentType) {
                SpotifyAccountContentType.PLAYLISTS -> {
                    if (state.isLoading && state.playlists.isEmpty()) {
                        items(8) {
                            ShimmerHost {
                                GridItemPlaceHolder(fillMaxWidth = true)
                            }
                        }
                    } else if (state.playlists.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyPlaceholder(
                                icon = R.drawable.bookmark_star_library,
                                text = stringResource(R.string.spotify_no_playlists),
                            )
                        }
                    } else {
                        items(
                            items = state.playlists,
                            key = { it.id }
                        ) { playlist ->
                            SpotifyGridCard(
                                title = playlist.name,
                                subtitle = "${playlist.tracks?.total ?: 0} tracks",
                                imageUrl = playlist.images.firstOrNull()?.url,
                                onClick = { showPlaylistSheet = true }
                            )
                        }
                    }
                }

                SpotifyAccountContentType.LIKED_SONGS -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpotifyGridCard(
                            title = stringResource(R.string.spotify_liked_songs),
                            subtitle = "${state.likedSongsCount} songs",
                            imageUrl = null,
                            isLikedSongs = true,
                            onClick = { showPlaylistSheet = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpotifyGridCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    isLikedSongs: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isLikedSongs) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLikedSongs) {
                Icon(
                    painter = painterResource(R.drawable.favorite),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
