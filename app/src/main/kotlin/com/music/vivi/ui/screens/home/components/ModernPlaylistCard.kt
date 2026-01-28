package com.music.vivi.ui.component.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.vivi.R
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.ui.component.PlayingIndicatorBox
import com.music.vivi.utils.makeTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ModernPlaylistCard(
    playlist: PlaylistItem,
    tracks: List<SongItem>,
    isPlaying: Boolean,
    activeMediaId: String?,
    isPaused: Boolean,
    onPlayClick: () -> Unit,
    onRadioClick: () -> Unit,
    onTrackClick: (SongItem) -> Unit,
    onTrackMenuClick: (SongItem) -> Unit,
    onHeaderClick: () -> Unit,
    onTracksFetched: (List<SongItem>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var fetchedTracks by remember(playlist.id) { mutableStateOf<List<SongItem>>(emptyList()) }
    var isLoading by remember(playlist.id) { mutableStateOf(false) }

    val displayTracks = remember(tracks, fetchedTracks) {
        if (tracks.isNotEmpty()) tracks else fetchedTracks
    }

    LaunchedEffect(playlist.id, tracks) {
        if (tracks.isEmpty() && fetchedTracks.isEmpty()) {
            isLoading = true
            withContext(Dispatchers.IO) {
                YouTube.playlist(playlist.id).onSuccess { page ->
                    val newTracks = page.songs.take(3)
                    fetchedTracks = newTracks
                    onTracksFetched(newTracks)
                }.onFailure {
                    // Fallback to empty list or handle error
                }
            }
            isLoading = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
        ) {
            // Header Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHeaderClick() }
            ) {
                // Single Playlist Artwork
                AsyncImage(
                    model = playlist.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                        .background(MaterialTheme.colorScheme.surface)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = playlist.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = playlist.author?.name ?: playlist.songCountText ?: "Created for you",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description (Localized string)
            Box(modifier = Modifier.height(60.dp)) {
                Text(
                    text = stringResource(R.string.modern_playlist_description),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            // Tracks Preview
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(170.dp) // Slightly reduced to fit 415dp better
            ) {
                displayTracks.take(3).forEach { track ->
                    val isActive = track.id == activeMediaId
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTrackClick(track) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(48.dp)
                        ) {
                            AsyncImage(
                                model = track.thumbnail,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )

                            PlayingIndicatorBox(
                                isActive = isActive,
                                playWhenReady = !isPaused,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f))
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${track.artists.firstOrNull()?.name ?: "Unknown"} • ${makeTimeString(track.duration?.times(1000L))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(onClick = { onTrackMenuClick(track) }) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledIconButton(
                    onClick = onPlayClick,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play_arrow),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onRadioClick,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        painter = painterResource(R.drawable.radio),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }

            }
        }
    }
}
