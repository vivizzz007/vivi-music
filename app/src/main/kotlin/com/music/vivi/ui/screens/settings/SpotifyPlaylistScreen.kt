/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.spotify.SpotifyMapper
import com.music.spotify.models.SpotifyPlaylist
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.ui.component.AnimatedActionButton
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.detachedItemShape
import com.music.vivi.ui.component.endItemShape
import com.music.vivi.ui.component.leadingItemShape
import com.music.vivi.ui.component.middleItemShape
import androidx.compose.ui.graphics.Color
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.viewmodels.SpotifyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyPlaylistScreen(
    navController: NavController,
    viewModel: SpotifyViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val selectedIds = remember { mutableStateListOf<String>() }

    val displayItems = remember(state.likedSongsCount, state.playlists) {
        val list = mutableListOf<Any>()
        if (state.likedSongsCount > 0) {
            list.add("liked_songs")
        }
        list.addAll(state.playlists)
        list
    }

    var searchQuery by remember { mutableStateOf("") }

    val filteredDisplayItems = remember(displayItems, searchQuery) {
        if (searchQuery.isBlank()) {
            displayItems
        } else {
            displayItems.filter { item ->
                if (item is String && item == "liked_songs") {
                    val likedSongsText = context.getString(R.string.spotify_liked_songs)
                    likedSongsText.contains(searchQuery, ignoreCase = true)
                } else if (item is SpotifyPlaylist) {
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.owner?.displayName.orEmpty().contains(searchQuery, ignoreCase = true)
                } else {
                    false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp) // Room for player
        ) {
            item {
                Spacer(
                    Modifier.windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
                    )
                )



                // Search Text Field
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            androidx.compose.material3.IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = null
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .height(52.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )


            }

            // Playlist list
            itemsIndexed(
                items = filteredDisplayItems,
                key = { _, item ->
                    if (item is String) item else (item as SpotifyPlaylist).id
                }
            ) { index, item ->
                // Apply horizontal padding around each item
                Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 1.dp)) {
                    val shape = when {
                        filteredDisplayItems.size == 1 -> detachedItemShape()
                        index == 0 -> leadingItemShape()
                        index == filteredDisplayItems.size - 1 -> endItemShape()
                        else -> middleItemShape()
                    }

                    if (item is String && item == "liked_songs") {
                        SpotifySourceRow(
                            title = stringResource(R.string.spotify_liked_songs),
                            subtitle = stringResource(R.string.spotify_liked_songs_desc),
                            thumbnailUrl = null,
                            trackCount = state.likedSongsCount,
                            selected = "liked_songs" in selectedIds,
                            shape = shape,
                            onClick = {
                                if ("liked_songs" in selectedIds) selectedIds.remove("liked_songs")
                                else selectedIds.add("liked_songs")
                            }
                        )
                    } else if (item is SpotifyPlaylist) {
                        SpotifySourceRow(
                            title = item.name,
                            subtitle = item.owner?.displayName.orEmpty(),
                            thumbnailUrl = SpotifyMapper.getPlaylistThumbnail(item),
                            trackCount = item.tracks?.total ?: 0,
                            selected = item.id in selectedIds,
                            shape = shape,
                            onClick = {
                                if (item.id in selectedIds) selectedIds.remove(item.id)
                                else selectedIds.add(item.id)
                            }
                        )
                    }
                }
            }
        }



        // Transparent TopAppBar purely for back navigation and title
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.playlists),
                    fontWeight = FontWeight.Bold,
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
            actions = {
                if (selectedIds.isNotEmpty() && !state.isLoading) {
                    IconButton(
                        onClick = {
                            viewModel.startImport(selectedIds.toList())
                            navController.navigateUp()
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.download),
                            contentDescription = stringResource(R.string.import_action)
                        )
                    }
                }

                val isAllSelected = filteredDisplayItems.isNotEmpty() && filteredDisplayItems.all { item ->
                    val id = if (item is String) item else (item as SpotifyPlaylist).id
                    id in selectedIds
                }
                Checkbox(
                    checked = isAllSelected,
                    onCheckedChange = { checked ->
                        filteredDisplayItems.forEach { item ->
                            val id = if (item is String) item else (item as SpotifyPlaylist).id
                            if (checked) {
                                if (id !in selectedIds) selectedIds.add(id)
                            } else {
                                selectedIds.remove(id)
                            }
                        }
                    },
                    modifier = Modifier.padding(end = 4.dp)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun SpotifySourceRow(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    trackCount: Int,
    selected: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                if (!thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.favorite),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (trackCount > 0) stringResource(R.string.spotify_track_count, trackCount) else subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Checkbox(
                checked = selected,
                onCheckedChange = { onClick() },
            )
        }
    }
}
