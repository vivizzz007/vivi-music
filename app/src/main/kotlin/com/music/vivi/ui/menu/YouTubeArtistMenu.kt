package com.music.vivi.ui.menu

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.music.innertube.models.ArtistItem
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.db.entities.ArtistEntity
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.NewAction
import com.music.vivi.ui.component.NewActionGrid
import com.music.vivi.ui.component.YouTubeListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeArtistMenu(
    artist: ArtistItem,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val libraryArtist by database.artist(artist.id).collectAsState(initial = null)

    YouTubeListItem(
        item = artist,
        trailingContent = {},
    )

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    // Enhanced Action Grid using NewMenuComponents
    NewActionGrid(
        actions = buildList {
            // Start Radio button
            artist.radioEndpoint?.let { watchEndpoint ->
                add(
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.radio),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.start_radio),
                        onClick = {
                            playerConnection.playQueue(YouTubeQueue(watchEndpoint))
                            onDismiss()
                        }
                    )
                )
            }

            // Shuffle button
            artist.shuffleEndpoint?.let { watchEndpoint ->
                add(
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.shuffle),
                        onClick = {
                            playerConnection.playQueue(YouTubeQueue(watchEndpoint))
                            onDismiss()
                        }
                    )
                )
            }

            // Share button
            add(
                NewAction(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.share),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    text = stringResource(R.string.share),
                    onClick = {
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, artist.shareLink)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                        onDismiss()
                    }
                )
            )
        },
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
    )

    LazyColumn(
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        // Subscribe/Subscribed button
        item {
            ListItem(
                headlineContent = { 
                    Text(text = if (libraryArtist?.artist?.bookmarkedAt != null) stringResource(R.string.subscribed) else stringResource(R.string.subscribe))
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(
                            if (libraryArtist?.artist?.bookmarkedAt != null) {
                                R.drawable.subscribed
                            } else {
                                R.drawable.subscribe
                            }
                        ),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
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
            )
        }
    }
}
