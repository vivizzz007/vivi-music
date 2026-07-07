package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import coil3.compose.AsyncImage
import com.music.spotify.SpotifyMapper
import com.music.spotify.models.SpotifyPlaylist
import com.music.vivi.R
import com.music.vivi.ui.component.AnimatedActionButton
import com.music.vivi.ui.component.detachedItemShape
import com.music.vivi.ui.component.endItemShape
import com.music.vivi.ui.component.leadingItemShape
import com.music.vivi.ui.component.middleItemShape
import com.music.vivi.viewmodels.SpotifyImportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyPlaylistBottomSheet(
    onDismiss: () -> Unit,
    viewModel: SpotifyImportViewModel,
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // Header Row (Title and Select All checkbox)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.playlists),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val isAllSelected = filteredDisplayItems.isNotEmpty() && filteredDisplayItems.all { item ->
                    val id = if (item is String) item else (item as SpotifyPlaylist).id
                    id in selectedIds
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.spotify_select_all),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        }
                    )
                }
            }

            // Search Text Field
            OutlinedTextField(
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
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Top Divider
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )

            // Playlist list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(filteredDisplayItems, key = { _, item ->
                    if (item is String) item else (item as SpotifyPlaylist).id
                }) { index, item ->
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

            // Bottom Divider
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )

            // Bottom OK Button Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                AnimatedActionButton(
                    text = stringResource(R.string.import_action),
                    onClick = {
                        viewModel.startImport(selectedIds.toList())
                        onDismiss()
                    },
                    modifier = Modifier.widthIn(min = 120.dp),
                    enabled = selectedIds.isNotEmpty() && !state.isLoading
                )
            }
        }
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
                    .clip(MaterialTheme.shapes.medium)
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
