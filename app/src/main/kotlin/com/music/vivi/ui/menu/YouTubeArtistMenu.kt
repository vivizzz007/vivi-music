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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.music.innertube.models.ArtistItem
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.db.entities.ArtistEntity
import com.music.vivi.playback.queues.YouTubeQueue
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun YouTubeArtistMenu(
    artist: ArtistItem,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val libraryArtist by database.artist(artist.id).collectAsState(initial = null)

    // Design variables
    val evenCornerRadiusElems = 26.dp
    val artistArtShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = evenCornerRadiusElems, smoothnessAsPercentBR = 60, cornerRadiusBR = evenCornerRadiusElems,
        smoothnessAsPercentTL = 60, cornerRadiusTL = evenCornerRadiusElems, smoothnessAsPercentBL = 60,
        cornerRadiusBL = evenCornerRadiusElems, smoothnessAsPercentTR = 60
    )
    val radioButtonShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = evenCornerRadiusElems, smoothnessAsPercentBR = 60, cornerRadiusBR = evenCornerRadiusElems,
        smoothnessAsPercentTL = 60, cornerRadiusTL = evenCornerRadiusElems, smoothnessAsPercentBL = 60,
        cornerRadiusBL = evenCornerRadiusElems, smoothnessAsPercentTR = 60
    )

    // Subscribe state tracking
    val isSubscribed = libraryArtist?.artist?.bookmarkedAt != null

    val subscribeButtonCornerRadius by animateDpAsState(
        targetValue = if (isSubscribed) evenCornerRadiusElems else 60.dp,
        animationSpec = tween(durationMillis = 300), label = "SubscribeCornerAnimation"
    )
    val subscribeButtonContainerColor by animateColorAsState(
        targetValue = if (isSubscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 300), label = "SubscribeContainerColorAnimation"
    )
    val subscribeButtonContentColor by animateColorAsState(
        targetValue = if (isSubscribed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 300), label = "SubscribeContentColorAnimation"
    )

    val subscribeButtonShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = subscribeButtonCornerRadius,
        smoothnessAsPercentBR = 60,
        cornerRadiusBR = subscribeButtonCornerRadius,
        smoothnessAsPercentTL = 60,
        cornerRadiusTL = subscribeButtonCornerRadius,
        smoothnessAsPercentBL = 60,
        cornerRadiusBL = subscribeButtonCornerRadius,
        smoothnessAsPercentTR = 60
    )

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Spacer(modifier = Modifier.height(1.dp))

        // Header Row - Artist Image and Name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = artist.thumbnail,
                contentDescription = "Artist Image",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
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
                        text = artist.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Light
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Artist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Header subscribe button
            FilledTonalIconButton(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 6.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = {
                    database.query {
                        val libraryArtist = libraryArtist
                        if (libraryArtist != null) {
                            update(libraryArtist.artist.toggleLike())
                        } else {
                            insert(
                                ArtistEntity(
                                    id = artist.id,
                                    name = artist.title,
                                    channelId = artist.channelId,
                                    thumbnailUrl = artist.thumbnail,
                                ).toggleLike()
                            )
                        }
                    }
                }
            ) {
                Icon(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    painter = painterResource(
                        if (isSubscribed) R.drawable.subscribed else R.drawable.subscribe
                    ),
                    contentDescription = if (isSubscribed) "Unsubscribe" else "Subscribe",
                    tint = if (isSubscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row - Dynamic based on available endpoints
        val hasRadio = artist.radioEndpoint != null
        val hasShuffle = artist.shuffleEndpoint != null

        if (hasRadio || hasShuffle) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Radio Button (if available)
                if (hasRadio) {
                    artist.radioEndpoint?.let { watchEndpoint ->
                        MediumExtendedFloatingActionButton(
                            modifier = Modifier
                                .weight(if (hasShuffle) 0.5f else 0.75f)
                                .fillMaxHeight(),
                            onClick = {
                                playerConnection.playQueue(YouTubeQueue(watchEndpoint))
                                onDismiss()
                            },
                            elevation = FloatingActionButtonDefaults.elevation(0.dp),
                            shape = radioButtonShape,
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.radio),
                                    contentDescription = "Radio"
                                )
                            },
                            text = {
                                Text(
                                    modifier = Modifier.padding(end = 10.dp),
                                    text = "Radio",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }

                // Shuffle Button (if available)
                if (hasShuffle) {
                    artist.shuffleEndpoint?.let { watchEndpoint ->
                        if (hasRadio) {
                            FilledTonalIconButton(
                                modifier = Modifier
                                    .weight(0.25f)
                                    .fillMaxHeight(),
                                onClick = {
                                    playerConnection.playQueue(YouTubeQueue(watchEndpoint))
                                    onDismiss()
                                },
                                shape = CircleShape
                            ) {
                                Icon(
                                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = "Shuffle"
                                )
                            }
                        } else {
                            MediumExtendedFloatingActionButton(
                                modifier = Modifier
                                    .weight(0.75f)
                                    .fillMaxHeight(),
                                onClick = {
                                    playerConnection.playQueue(YouTubeQueue(watchEndpoint))
                                    onDismiss()
                                },
                                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                                shape = radioButtonShape,
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.shuffle),
                                        contentDescription = "Shuffle"
                                    )
                                },
                                text = {
                                    Text(
                                        modifier = Modifier.padding(end = 10.dp),
                                        text = "Shuffle",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
                                    )
                                }
                            )
                        }
                    }
                }

                // Subscribe Button
                FilledIconButton(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight(),
                    onClick = {
                        database.query {
                            val libraryArtist = libraryArtist
                            if (libraryArtist != null) {
                                update(libraryArtist.artist.toggleLike())
                            } else {
                                insert(
                                    ArtistEntity(
                                        id = artist.id,
                                        name = artist.title,
                                        channelId = artist.channelId,
                                        thumbnailUrl = artist.thumbnail,
                                    ).toggleLike()
                                )
                            }
                        }
                    },
                    shape = subscribeButtonShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = subscribeButtonContainerColor,
                        contentColor = subscribeButtonContentColor
                    )
                ) {
                    Icon(
                        modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                        painter = painterResource(
                            if (isSubscribed) R.drawable.subscribed else R.drawable.subscribe
                        ),
                        contentDescription = if (isSubscribed) "Unsubscribe" else "Subscribe",
                        tint = if (isSubscribed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Share Button
                FilledTonalIconButton(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight(),
                    onClick = {
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, artist.shareLink)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                        onDismiss()
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

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Details Section
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Radio Button (as list item if available)
            artist.radioEndpoint?.let { watchEndpoint ->
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = CircleShape,
                    onClick = {
                        playerConnection.playQueue(YouTubeQueue(watchEndpoint))
                        onDismiss()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.radio),
                        contentDescription = "Radio icon"
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Start Radio",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Play similar songs",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Shuffle Button (as list item if available)
            artist.shuffleEndpoint?.let { watchEndpoint ->
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = CircleShape,
                    onClick = {
                        playerConnection.playQueue(YouTubeQueue(watchEndpoint))
                        onDismiss()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = "Shuffle icon"
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Shuffle",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Play in random order",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Subscribe/Unsubscribe Button
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = CircleShape,
                onClick = {
                    database.query {
                        val libraryArtist = libraryArtist
                        if (libraryArtist != null) {
                            update(libraryArtist.artist.toggleLike())
                        } else {
                            insert(
                                ArtistEntity(
                                    id = artist.id,
                                    name = artist.title,
                                    channelId = artist.channelId,
                                    thumbnailUrl = artist.thumbnail,
                                ).toggleLike()
                            )
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(
                        if (isSubscribed) R.drawable.subscribed else R.drawable.subscribe
                    ),
                    contentDescription = "Subscribe icon"
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isSubscribed) "Subscribed" else "Subscribe",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (isSubscribed) "Following this artist" else "Follow this artist",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Share Button (as list item)
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = CircleShape,
                onClick = {
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, artist.shareLink)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                    onDismiss()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.share),
                    contentDescription = "Share icon"
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Share",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Share this artist",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}