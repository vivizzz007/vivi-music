package com.music.vivi.ui.menu


import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.music.vivi.playback.queues.ListQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArtistMenu(
    originalArtist: Artist,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val artistState = database.artist(originalArtist.id).collectAsState(initial = originalArtist)
    val artist = artistState.value ?: originalArtist

    // Design variables from original SongMenu
    val evenCornerRadiusElems = 26.dp
    val albumArtShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = evenCornerRadiusElems, smoothnessAsPercentBR = 60, cornerRadiusBR = evenCornerRadiusElems,
        smoothnessAsPercentTL = 60, cornerRadiusTL = evenCornerRadiusElems, smoothnessAsPercentBL = 60,
        cornerRadiusBL = evenCornerRadiusElems, smoothnessAsPercentTR = 60
    )
    val playButtonShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = evenCornerRadiusElems, smoothnessAsPercentBR = 60, cornerRadiusBR = evenCornerRadiusElems,
        smoothnessAsPercentTL = 60, cornerRadiusTL = evenCornerRadiusElems, smoothnessAsPercentBL = 60,
        cornerRadiusBL = evenCornerRadiusElems, smoothnessAsPercentTR = 60
    )

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
                contentDescription = "Artist Art",
                modifier = Modifier
                    .size(80.dp)
                    .clip(albumArtShape),
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
                        text = "${artist.songCount} songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

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
                },
            ) {
                Icon(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    painter = painterResource(if (artist.artist.bookmarkedAt != null) R.drawable.subscribed else R.drawable.subscribe),
                    contentDescription = if (artist.artist.bookmarkedAt != null) "Unsubscribe" else "Subscribe",
                    tint = if (artist.artist.bookmarkedAt != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Play Button
            if (artist.songCount > 0) {
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
                                    items = songs,
                                ),
                            )
                        }
                        onDismiss()
                    },
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    shape = playButtonShape,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = "Play"
                        )
                    },
                    text = {
                        Text(
                            modifier = Modifier.padding(end = 10.dp),
                            text = "Play"
                        )
                    }
                )
            }

            // Shuffle Button
            if (artist.songCount > 0) {
                FilledIconButton(
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
                                    items = songs,
                                ),
                            )
                        }
                        onDismiss()
                    },
                    shape = playButtonShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = "Shuffle"
                    )
                }
            }

            // Share Button
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
                    shape = CircleShape
                ) {
                    Icon(
                        modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                        painter = painterResource(R.drawable.share),
                        contentDescription = "Share artist"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Details Section
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Subscribe/Unsubscribe Button
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = CircleShape,
                onClick = {
                    database.transaction {
                        update(artist.artist.toggleLike())
                    }
                }
            ) {
                Icon(
                    painter = painterResource(if (artist.artist.bookmarkedAt != null) R.drawable.subscribed else R.drawable.subscribe),
                    contentDescription = "Subscribe icon"
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (artist.artist.bookmarkedAt != null) "Subscribed" else "Subscribe",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        if (artist.artist.bookmarkedAt != null) "Unsubscribe from artist" else "Subscribe to artist",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}