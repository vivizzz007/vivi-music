/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.PlaylistItem
import com.music.vivi.db.entities.Song
import com.music.vivi.utils.resize
import com.music.vivi.wear.R
import com.music.vivi.wear.WearApp
import com.music.vivi.wear.playback.WearPlaybackService
import com.music.vivi.wear.viewmodels.LibraryTab
import com.music.vivi.wear.viewmodels.LibraryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Account Library & Offline Downloads screen for Wear OS.
 *
 * Provides a segmented tab interface:
 * - [LibraryTab.PLAYLISTS]: User playlists and prominent Liked Songs card.
 * - [LibraryTab.ALBUMS]: Saved library albums with cover art and artist info.
 * - [LibraryTab.DOWNLOADS]: Offline downloaded songs stored locally on the watch.
 *
 * Features AMOLED true-black background and rotary crown scrolling support.
 */
@Composable
fun LibraryScreen(
    mediaState: MediaControllerState,
    onNavigateToPlaylist: (playlistId: String, title: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LibraryViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val database = remember { WearApp.instance.database }

    // Observe downloaded songs from Room database
    val downloadedSongs by remember {
        database.downloadedSongsByCreateDateAsc()
    }.collectAsState(initial = emptyList())

    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onRotaryScrollEvent { event ->
                    scope.launch {
                        listState.scrollBy(event.verticalScrollPixels)
                    }
                    true
                },
        ) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.library),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Top item: Tab Selector Chips
            item {
                TabSelector(
                    selectedTab = state.selectedTab,
                    onTabSelected = { viewModel.selectTab(it) },
                )
            }

            when (state.selectedTab) {
                LibraryTab.PLAYLISTS -> {
                    if (!state.isLoggedIn) {
                        item {
                            LoggedOutCard(onNavigateToLogin = onNavigateToLogin)
                        }
                    } else {
                        // Liked Songs Prominent Card
                        item {
                            LikedSongsCard(
                                onClick = { onNavigateToPlaylist("LM", "Liked Songs") },
                            )
                        }

                        if (state.isLoading && state.playlists.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                }
                            }
                        } else if (state.error != null && state.playlists.isEmpty()) {
                            item {
                                ErrorCard(
                                    message = state.error ?: "Failed to load playlists",
                                    onRetry = { viewModel.refresh() },
                                )
                            }
                        } else if (state.playlists.isEmpty()) {
                            item {
                                EmptyContentHint(
                                    title = "No Playlists",
                                    subtitle = "Your playlists will appear here",
                                )
                            }
                        } else {
                            items(
                                items = state.playlists,
                                key = { it.id },
                            ) { playlist ->
                                PlaylistItemCard(
                                    playlist = playlist,
                                    onClick = { onNavigateToPlaylist(playlist.id, playlist.title) },
                                )
                            }
                        }
                    }
                }

                LibraryTab.ALBUMS -> {
                    if (!state.isLoggedIn) {
                        item {
                            LoggedOutCard(onNavigateToLogin = onNavigateToLogin)
                        }
                    } else {
                        if (state.isLoading && state.albums.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                }
                            }
                        } else if (state.error != null && state.albums.isEmpty()) {
                            item {
                                ErrorCard(
                                    message = state.error ?: "Failed to load albums",
                                    onRetry = { viewModel.refresh() },
                                )
                            }
                        } else if (state.albums.isEmpty()) {
                            item {
                                EmptyContentHint(
                                    title = "No Saved Albums",
                                    subtitle = "Saved albums will appear here",
                                )
                            }
                        } else {
                            items(
                                items = state.albums,
                                key = { it.browseId },
                            ) { album ->
                                AlbumItemCard(
                                    album = album,
                                    onClick = { onNavigateToPlaylist(album.playlistId, album.title) },
                                )
                            }
                        }
                    }
                }

                LibraryTab.DOWNLOADS -> {
                    if (downloadedSongs.isEmpty()) {
                        item {
                            EmptyDownloadsHint()
                        }
                    } else {
                        itemsIndexed(
                            items = downloadedSongs,
                            key = { _, item -> item.song.id },
                        ) { index, song ->
                            DownloadedSongItem(
                                song = song,
                                onClick = {
                                    scope.launch(Dispatchers.Main) {
                                        val controller = mediaState.controller ?: return@launch
                                        val mediaItems = downloadedSongs.map { s ->
                                            WearPlaybackService.buildMediaItem(
                                                songId = s.song.id,
                                                title = s.song.title,
                                                artist = s.artists.firstOrNull()?.name,
                                                artworkUri = s.song.thumbnailUrl?.toUri(),
                                            )
                                        }
                                        controller.setMediaItems(mediaItems, index, 0L)
                                        controller.prepare()
                                        controller.play()
                                        onNavigateBack()
                                    }
                                },
                                onRemoveClick = {
                                    WearApp.instance.downloadManager.removeDownload(song.song.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Segmented chips allowing quick switching between Playlists, Albums, and Downloads tabs.
 */
@Composable
private fun TabSelector(
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LibraryTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val label = when (tab) {
                LibraryTab.PLAYLISTS -> "Playlists"
                LibraryTab.ALBUMS -> "Albums"
                LibraryTab.DOWNLOADS -> "Downloads"
            }

            Button(
                onClick = { onTabSelected(tab) },
                colors = if (isSelected) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .height(30.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Informative card shown when logged out, prompting phone sign-in.
 */
@Composable
private fun LoggedOutCard(onNavigateToLogin: () -> Unit) {
    Card(
        onClick = onNavigateToLogin,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sign In via Phone",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Scan QR code on your phone to access your YouTube Music library",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onNavigateToLogin,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
            ) {
                Text("Sign In", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * Prominent Liked Songs card with Heart icon.
 */
@Composable
private fun LikedSongsCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Liked Songs",
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Liked Songs",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Your liked tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * User playlist card displaying thumbnail, title, and track count.
 */
@Composable
private fun PlaylistItemCard(
    playlist: PlaylistItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!playlist.thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(playlist.thumbnail?.resize(120, 120))
                        .crossfade(true)
                        .build(),
                    contentDescription = playlist.title,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = playlist.songCountText ?: "Playlist",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Saved album card displaying album art, title, artist name, and release year.
 */
@Composable
private fun AlbumItemCard(
    album: AlbumItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (album.thumbnail.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(album.thumbnail.resize(120, 120))
                        .crossfade(true)
                        .build(),
                    contentDescription = album.title,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val artistAndYear = listOfNotNull(
                    album.artists?.firstOrNull()?.name,
                    album.year?.toString(),
                ).joinToString(" • ")
                if (artistAndYear.isNotBlank()) {
                    Text(
                        text = artistAndYear,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Downloaded track card displaying title, artist, and remove download button.
 */
@Composable
private fun DownloadedSongItem(
    song: Song,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = song.song.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val artistName = song.artists.firstOrNull()?.name
                if (artistName != null) {
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove download",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyContentHint(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun EmptyDownloadsHint() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = stringResource(R.string.no_downloads),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.no_downloads_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.height(28.dp),
        ) {
            Text("Retry", style = MaterialTheme.typography.labelSmall)
        }
    }
}
