package com.music.vivi.ui.menu

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.ArtistSongSortType
import com.music.vivi.db.entities.Artist
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.LocalBottomSheetPageState
import com.music.vivi.ui.utils.ShowMediaInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

/**
 * The bottom sheet menu for an artist.
 * Provides options to Play, Shuffle, Subscribe (Favorite), Share, etc.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArtistMenu(originalArtist: Artist, coroutineScope: CoroutineScope, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val artistState = database.artist(originalArtist.id).collectAsState(initial = originalArtist)
    val artist = artistState.value ?: originalArtist
    val bottomSheetPageState = LocalBottomSheetPageState.current

    // Design variables
    val cornerRadius = remember { 24.dp }
    val artistArtShape = remember(cornerRadius) {
        AbsoluteSmoothCornerShape(
            cornerRadiusTR = cornerRadius,
            smoothnessAsPercentBR = 60,
            cornerRadiusBR = cornerRadius,
            smoothnessAsPercentTL = 60,
            cornerRadiusTL = cornerRadius,
            smoothnessAsPercentBL = 60,
            cornerRadiusBL = cornerRadius,
            smoothnessAsPercentTR = 60
        )
    }
    val playButtonShape = remember(cornerRadius) {
        AbsoluteSmoothCornerShape(
            cornerRadiusTR = cornerRadius,
            smoothnessAsPercentBR = 60,
            cornerRadiusBR = cornerRadius,
            smoothnessAsPercentTL = 60,
            cornerRadiusTL = cornerRadius,
            smoothnessAsPercentBL = 60,
            cornerRadiusBL = cornerRadius,
            smoothnessAsPercentTR = 60
        )
    }

    // Android 16 grouped shapes
    val topShape = remember(cornerRadius) {
        AbsoluteSmoothCornerShape(
            cornerRadiusTR = cornerRadius,
            smoothnessAsPercentBR = 0,
            cornerRadiusBR = 0.dp,
            smoothnessAsPercentTL = 60,
            cornerRadiusTL = cornerRadius,
            smoothnessAsPercentBL = 0,
            cornerRadiusBL = 0.dp,
            smoothnessAsPercentTR = 60
        )
    }
    val middleShape = remember { RectangleShape }
    val bottomShape = remember(cornerRadius) {
        AbsoluteSmoothCornerShape(
            cornerRadiusTR = 0.dp,
            smoothnessAsPercentBR = 60,
            cornerRadiusBR = cornerRadius,
            smoothnessAsPercentTL = 0,
            cornerRadiusTL = 0.dp,
            smoothnessAsPercentBL = 60,
            cornerRadiusBL = cornerRadius,
            smoothnessAsPercentTR = 0
        )
    }
    val singleShape = remember(cornerRadius) {
        AbsoluteSmoothCornerShape(
            cornerRadiusTR = cornerRadius,
            smoothnessAsPercentBR = 60,
            cornerRadiusBR = cornerRadius,
            smoothnessAsPercentTL = 60,
            cornerRadiusTL = cornerRadius,
            smoothnessAsPercentBL = 60,
            cornerRadiusBL = cornerRadius,
            smoothnessAsPercentTR = 60
        )
    }

    // Favorite state tracking
    val isFavorite = artist.artist.bookmarkedAt != null

    val favoriteButtonCornerRadius by animateDpAsState(
        targetValue = if (isFavorite) cornerRadius else 60.dp,
        animationSpec = tween(durationMillis = 300),
        label = "FavoriteCornerAnimation"
    )
    val favoriteButtonContainerColor by animateColorAsState(
        targetValue = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 300),
        label = "FavoriteContainerColorAnimation"
    )
    val favoriteButtonContentColor by animateColorAsState(
        targetValue = if (isFavorite) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 300),
        label = "FavoriteContentColorAnimation"
    )

    val favoriteButtonShape = remember(favoriteButtonCornerRadius) {
        AbsoluteSmoothCornerShape(
            cornerRadiusTR = favoriteButtonCornerRadius,
            smoothnessAsPercentBR = 60,
            cornerRadiusBR = favoriteButtonCornerRadius,
            smoothnessAsPercentTL = 60,
            cornerRadiusTL = favoriteButtonCornerRadius,
            smoothnessAsPercentBL = 60,
            cornerRadiusBL = favoriteButtonCornerRadius,
            smoothnessAsPercentTR = 60
        )
    }

    // Main Content
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Spacer(modifier = Modifier.height(1.dp))

        // Header Row - Artist Art and Name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = artist.artist.thumbnailUrl,
                contentDescription = stringResource(R.string.artist_art_content_desc),
                modifier = Modifier
                    .size(80.dp)
                    .clip(artistArtShape),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        text = artist.artist.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Light
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (artist.songCount > 0) {
                            "${artist.songCount} ${if (artist.songCount == 1) "song" else "songs"}"
                        } else {
                            stringResource(R.string.artist_label)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Header favorite button
            FilledTonalIconButton(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 6.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = {
                    database.transaction {
                        update(artist.artist.toggleLike())
                    }
                }
            ) {
                Icon(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    painter = painterResource(
                        if (isFavorite) R.drawable.subscribed else R.drawable.subscribe
                    ),
                    contentDescription = if (isFavorite) {
                        stringResource(
                            R.string.unsubscribe
                        )
                    } else {
                        stringResource(R.string.subscribe)
                    },
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row
        if (artist.songCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play Button
                MediumExtendedFloatingActionButton(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    onClick = {
                        coroutineScope.launch {
                            val songs = withContext(Dispatchers.IO) {
                                database
                                    .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                    .first()
                                    .map { it.toMediaItem() }
                            }
                            playerConnection.playQueue(
                                ListQueue(
                                    title = artist.artist.name,
                                    items = songs
                                )
                            )
                        }
                        onDismiss()
                    },
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    shape = playButtonShape,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = stringResource(R.string.play_content_desc)
                        )
                    },
                    text = {
                        Text(
                            modifier = Modifier.padding(end = 10.dp),
                            text = stringResource(R.string.play_text),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                )

                // Favorite Button
                FilledIconButton(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight(),
                    onClick = {
                        database.transaction {
                            update(artist.artist.toggleLike())
                        }
                    },
                    shape = favoriteButtonShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = favoriteButtonContainerColor,
                        contentColor = favoriteButtonContentColor
                    )
                ) {
                    Icon(
                        modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                        painter = painterResource(
                            if (isFavorite) R.drawable.subscribed else R.drawable.subscribe
                        ),
                        contentDescription = if (isFavorite) {
                            stringResource(
                                R.string.unsubscribe
                            )
                        } else {
                            stringResource(R.string.subscribe)
                        },
                        tint = if (isFavorite) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Shuffle Button
                FilledTonalIconButton(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight(),
                    onClick = {
                        coroutineScope.launch {
                            val songs = withContext(Dispatchers.IO) {
                                database
                                    .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                    .first()
                                    .map { it.toMediaItem() }
                                    .shuffled()
                            }
                            playerConnection.playQueue(
                                ListQueue(
                                    title = artist.artist.name,
                                    items = songs
                                )
                            )
                        }
                        onDismiss()
                    },
                    shape = singleShape
                ) {
                    Icon(
                        modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = stringResource(R.string.shuffle)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        } else {
            // If no songs, show subscribe and share buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Favorite Button
                MediumExtendedFloatingActionButton(
                    modifier = Modifier
                        .weight(if (artist.artist.isYouTubeArtist) 0.75f else 1f)
                        .fillMaxHeight(),
                    onClick = {
                        database.transaction {
                            update(artist.artist.toggleLike())
                        }
                    },
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    shape = favoriteButtonShape,
                    containerColor = favoriteButtonContainerColor,
                    contentColor = favoriteButtonContentColor,
                    icon = {
                        Icon(
                            painter = painterResource(
                                if (isFavorite) R.drawable.subscribed else R.drawable.subscribe
                            ),
                            contentDescription = if (isFavorite) "Unsubscribe" else "Subscribe"
                        )
                    },
                    text = {
                        Text(
                            modifier = Modifier.padding(end = 10.dp),
                            text = if (isFavorite) {
                                stringResource(
                                    R.string.subscribed
                                )
                            } else {
                                stringResource(R.string.subscribe)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                )

                // Share Button (only if YouTube artist)
                if (artist.artist.isYouTubeArtist) {
                    FilledTonalIconButton(
                        modifier = Modifier
                            .weight(0.25f)
                            .fillMaxHeight(),
                        onClick = {
                            onDismiss()
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "https://music.youtube.com/channel/${artist.id}"
                                )
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        },
                        shape = singleShape
                    ) {
                        Icon(
                            modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                            painter = painterResource(R.drawable.share),
                            contentDescription = stringResource(R.string.share_artist_content_desc)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Details Section
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Playback Group (only if has songs)
            if (artist.songCount > 0) {
                // Shuffle
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = topShape,
                    onClick = {
                        coroutineScope.launch {
                            val songs = withContext(Dispatchers.IO) {
                                database
                                    .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                    .first()
                                    .map { it.toMediaItem() }
                                    .shuffled()
                            }
                            playerConnection.playQueue(
                                ListQueue(
                                    title = artist.artist.name,
                                    items = songs
                                )
                            )
                        }
                        onDismiss()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = stringResource(R.string.shuffle_icon_content_desc)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.shuffle_label),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            stringResource(R.string.play_in_random_order),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(1.dp))

                // Play Next
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = middleShape,
                    onClick = {
                        coroutineScope.launch {
                            val songs = withContext(Dispatchers.IO) {
                                database
                                    .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                    .first()
                                    .map { it.toMediaItem() }
                            }
                            playerConnection.playNext(songs)
                        }
                        onDismiss()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.playlist_play),
                        contentDescription = stringResource(R.string.play_next_icon_content_desc)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.play_next_label),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            stringResource(R.string.play_after_current),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(1.dp))

                // Add to Queue
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = middleShape,
                    onClick = {
                        coroutineScope.launch {
                            val songs = withContext(Dispatchers.IO) {
                                database
                                    .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                    .first()
                                    .map { it.toMediaItem() }
                            }
                            playerConnection.addToQueue(songs)
                        }
                        onDismiss()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = stringResource(R.string.add_to_queue_icon_content_desc)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.add_to_queue_label),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            stringResource(R.string.add_to_queue_end),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(1.dp))

                // Start Radio
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = bottomShape,
                    onClick = {
                        coroutineScope.launch {
                            val songs = withContext(Dispatchers.IO) {
                                database
                                    .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                    .first()
                            }
                            if (songs.isNotEmpty()) {
                                onDismiss()
                                playerConnection.playQueue(YouTubeQueue.radio(songs.first().toMediaMetadata()))
                            }
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.radio),
                        contentDescription = stringResource(R.string.start_radio_icon_content_desc)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.start_radio_label),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            stringResource(R.string.play_similar_songs_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Info and Social Group
            // Artist Details
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = if (artist.artist.isYouTubeArtist) topShape else singleShape,
                onClick = {
                    onDismiss()
                    bottomSheetPageState.show {
                        ShowMediaInfo(artist.id)
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = stringResource(R.string.info_icon_content_desc)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.artist_details),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(R.string.view_information),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Share (only if YouTube artist)
            if (artist.artist.isYouTubeArtist) {
                Spacer(modifier = Modifier.height(1.dp))

                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = bottomShape,
                    onClick = {
                        onDismiss()
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "https://music.youtube.com/channel/${artist.id}"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = stringResource(R.string.share_icon_content_desc)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.share_label),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            stringResource(R.string.share_artist_link),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
