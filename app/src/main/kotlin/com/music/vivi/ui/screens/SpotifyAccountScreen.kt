/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.spotify.SpotifyMapper
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.ui.component.EmptyPlaceholder
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.shimmer.ListItemPlaceHolder
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.viewmodels.SpotifyImportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyAccountScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: SpotifyImportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val rotationAngle by if (state.isLoading) {
        val transition = rememberInfiniteTransition(label = "rotation")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotationAngle"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    LaunchedEffect(Unit) {
        if (state.playlists.isEmpty()) {
            viewModel.loadSources()
        }
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
                        onClick = { viewModel.loadSources() },
                        enabled = !state.isLoading
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.sync),
                            contentDescription = stringResource(R.string.spotify_refresh),
                            modifier = Modifier.graphicsLayer {
                                rotationZ = rotationAngle
                            }
                        )
                    }
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
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
        ) {
            // Profile Header Card
            item(key = "profile_card") {
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
                        val summaryText = buildString {
                            append("${state.playlists.size} playlists")
                            if (state.likedSongsCount > 0) {
                                append(" • ${state.likedSongsCount} liked songs")
                            }
                        }
                        Text(
                            text = summaryText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Liked Songs Row (unified in single column at top of items)
            if (state.likedSongsCount > 0 || state.isAuthenticated) {
                item(key = "liked_songs") {
                    SpotifyListItemRow(
                        title = stringResource(R.string.spotify_liked_songs),
                        subtitle = "${state.likedSongsCount} songs",
                        imageUrl = null,
                        isLikedSongs = true,
                        onClick = {
                            navController.navigate("spotify_playlist/liked_songs")
                        }
                    )
                }
            }

            // Loading state
            if (state.isLoading && state.playlists.isEmpty()) {
                items(8) {
                    ShimmerHost {
                        ListItemPlaceHolder()
                    }
                }
            } else if (state.playlists.isEmpty() && state.likedSongsCount == 0) {
                item(key = "empty") {
                    EmptyPlaceholder(
                        icon = R.drawable.bookmark_star_library,
                        text = stringResource(R.string.spotify_no_playlists),
                    )
                }
            } else {
                // Playlists Rows (in same single column without separator labels)
                items(
                    items = state.playlists,
                    key = { it.id }
                ) { playlist ->
                    val trackCount = playlist.tracks?.total ?: 0
                    val subtitle = if (trackCount > 0) "$trackCount songs" else "Playlist"
                    SpotifyListItemRow(
                        title = playlist.name,
                        subtitle = subtitle,
                        imageUrl = SpotifyMapper.getPlaylistThumbnail(playlist),
                        onClick = {
                            navController.navigate("spotify_playlist/${playlist.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotifyListItemRow(
    title: String,
    subtitle: String,
    imageUrl: String?,
    isLikedSongs: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
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
                    modifier = Modifier.size(28.dp),
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
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            painter = painterResource(R.drawable.chevron_right_px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}
