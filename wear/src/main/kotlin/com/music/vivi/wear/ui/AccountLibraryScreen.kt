/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.innertube.YouTube
import com.music.innertube.models.PlaylistItem
import com.music.vivi.models.MediaMetadata
import com.music.vivi.wear.WearApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Account Library screen showing the user's YouTube Music playlists and Liked Songs.
 *
 * Fetches playlists from YouTube via :innertube and presents them in a scrollable list.
 * Each playlist can be tapped to browse its tracks, or downloaded in one tap.
 */
@Composable
fun AccountLibraryScreen(
    onNavigateToPlaylist: (playlistId: String, playlistTitle: String) -> Unit,
) {
    val scope = rememberCoroutineScope()

    var playlists by remember { mutableStateOf<List<PlaylistItem>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadPlaylists() {
        isLoading = true
        errorMessage = null
        playlists = null
        scope.launch(Dispatchers.IO) {
            val result = runCatching {
                val page = YouTube.library("FEmusic_liked_playlists").getOrThrow()
                page.items
                    .filterIsInstance<PlaylistItem>()
                    .filter { it.id != "SE" && it.id.isNotBlank() }
            }
            withContext(Dispatchers.Main) {
                isLoading = false
                if (result.isSuccess) {
                    playlists = result.getOrNull() ?: emptyList()
                } else {
                    errorMessage = "Failed to load library.\nCheck connection and try again."
                }
            }
        }
    }

    LaunchedEffect(Unit) { loadPlaylists() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    colors = androidx.wear.compose.material3.ProgressIndicatorDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
            errorMessage != null -> {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { loadPlaylists() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text("Retry", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            else -> {
                val listState = rememberScalingLazyListState()
                ScalingLazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        ListHeader {
                            Text(
                                "My Library",
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    // Liked Songs shortcut
                    item {
                        PlaylistCard(
                            id = "LM",
                            title = "Liked Songs",
                            authorName = null,
                            thumbnailUrl = null,
                            isLikedSongs = true,
                            onTap = { onNavigateToPlaylist("LM", "Liked Songs") },
                            onDownloadAll = {
                                scope.launch(Dispatchers.IO) {
                                    runCatching {
                                        val page = YouTube.playlist("LM").getOrThrow()
                                        val songs = page.songs.map { song ->
                                            MediaMetadata(
                                                id = song.id,
                                                title = song.title,
                                                artists = song.artists.map { a ->
                                                    MediaMetadata.Artist(
                                                        id = a.id,
                                                        name = a.name,
                                                    )
                                                },
                                                duration = song.duration ?: 0,
                                                thumbnailUrl = song.thumbnail,
                                            )
                                        }
                                        WearApp.instance.downloadManager.downloadPlaylist("LM", "Liked Songs", songs)
                                    }
                                }
                            },
                        )
                    }

                    items(
                        items = playlists ?: emptyList(),
                        key = { it.id },
                    ) { playlist ->
                        PlaylistCard(
                            id = playlist.id,
                            title = playlist.title,
                            authorName = playlist.author?.name,
                            thumbnailUrl = playlist.thumbnail,
                            isLikedSongs = false,
                            onTap = { onNavigateToPlaylist(playlist.id, playlist.title) },
                            onDownloadAll = {
                                scope.launch(Dispatchers.IO) {
                                    runCatching {
                                        val page = YouTube.playlist(playlist.id).getOrThrow()
                                        val songs = page.songs.map { song ->
                                            MediaMetadata(
                                                id = song.id,
                                                title = song.title,
                                                artists = song.artists.map { a ->
                                                    MediaMetadata.Artist(
                                                        id = a.id,
                                                        name = a.name,
                                                    )
                                                },
                                                duration = song.duration ?: 0,
                                                thumbnailUrl = song.thumbnail,
                                            )
                                        }
                                        WearApp.instance.downloadManager.downloadPlaylist(
                                            playlist.id,
                                            playlist.title,
                                            songs,
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    id: String,
    title: String,
    authorName: String?,
    thumbnailUrl: String?,
    isLikedSongs: Boolean,
    onTap: () -> Unit,
    onDownloadAll: () -> Unit,
) {
    Card(
        onClick = onTap,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLikedSongs) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            } else if (thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (authorName != null) {
                    Text(
                        text = authorName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(
                onClick = onDownloadAll,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudDownload,
                    contentDescription = "Download playlist",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
