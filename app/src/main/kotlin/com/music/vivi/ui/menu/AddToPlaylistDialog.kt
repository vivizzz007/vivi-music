package com.music.vivi.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import com.music.innertube.utils.parseCookieString
import com.music.vivi.LocalDatabase
import com.music.vivi.R
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.ListThumbnailSize
import com.music.vivi.db.entities.Playlist
import com.music.vivi.ui.component.CreatePlaylistDialog
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.ListDialog
import com.music.vivi.ui.component.PlaylistThumbnail
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@Composable
fun AddToPlaylistDialog(
    isVisible: Boolean,
    allowSyncing: Boolean = true,
    initialTextFieldValue: String? = null,
    songsToCheck: List<String>? = null,
    onGetSong: suspend (Playlist) -> List<String>, // list of song ids. Songs should be inserted to database in this function.
    onDismiss: () -> Unit,
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    var playlists by remember {
        mutableStateOf(emptyList<Playlist>())
    }
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    var showCreatePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showDuplicateDialog by remember {
        mutableStateOf(false)
    }
    var selectedPlaylist by remember {
        mutableStateOf<Playlist?>(null)
    }
    var songIds by remember {
        mutableStateOf<List<String>?>(null) // list is not saveable
    }
    var duplicates by remember {
        mutableStateOf(emptyList<String>())
    }

    // State to hold IDs of playlists that already contain the song(s)
    var containingPlaylists by remember {
        mutableStateOf<Set<String>>(emptySet())
    }

    LaunchedEffect(Unit) {
        database.editablePlaylistsByCreateDateAsc().collect {
            playlists = it.asReversed()
        }
    }

    // Check for existing songs in playlists
    LaunchedEffect(playlists, songsToCheck) {
        if (songsToCheck != null && playlists.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val foundIn = mutableSetOf<String>()
                playlists.forEach { playlist ->
                    // Check if *all* songsToCheck are in this playlist (or maybe *any*? "Already in playlist" usually implies duplicates)
                    // Let's check for intersection. If unchecking songs isn't possible here, knowing even one exists is useful.
                    // For the warning "Song A is available in Playlist X", we should probably show it if *any* song is there?
                    // But duplicates logic in 'database.playlistDuplicates' returns list of duplicates.
                    // Using that here efficiently.
                    val dupes = database.playlistDuplicates(playlist.id, songsToCheck)
                    if (dupes.isNotEmpty()) {
                        foundIn.add(playlist.id)
                    }
                }
                containingPlaylists = foundIn
            }
        }
    }

    val cornerRadius = 16.dp
    val buttonShape = remember {
        AbsoluteSmoothCornerShape(
            cornerRadiusTR = cornerRadius, smoothnessAsPercentBR = 60, cornerRadiusBR = cornerRadius,
            smoothnessAsPercentTL = 60, cornerRadiusTL = cornerRadius, smoothnessAsPercentBL = 60,
            cornerRadiusBL = cornerRadius, smoothnessAsPercentTR = 60
        )
    }

    if (isVisible) {
        ListDialog(
            onDismiss = onDismiss,
        ) {
            item {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .heightIn(min = 66.dp),
                    shape = buttonShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    onClick = {
                        showCreatePlaylistDialog = true
                    }
                ) {
                    Image(
                        painter = painterResource(R.drawable.add),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.create_playlist),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            items(playlists) { playlist ->
                val autoPlaylist = false // playlists in this dialog are editable, so usually not auto-playlists
                val isAlreadyAdded = containingPlaylists.contains(playlist.id)

                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .heightIn(min = 72.dp),
                    shape = buttonShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isAlreadyAdded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    onClick = {
                        selectedPlaylist = playlist
                        coroutineScope.launch(Dispatchers.IO) {
                            if (songIds == null) {
                                songIds = onGetSong(playlist)
                            }
                            duplicates = database.playlistDuplicates(playlist.id, songIds!!)
                            if (duplicates.isNotEmpty()) {
                                showDuplicateDialog = true
                            } else {
                                onDismiss()
                                database.addSongToPlaylist(playlist, songIds!!)

                                playlist.playlist.browseId?.let { plist ->
                                    songIds?.forEach {
                                        YouTube.addToPlaylist(plist, it)
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlaylistThumbnail(
                            thumbnails = playlist.thumbnails,
                            size = 56.dp, // Slightly smaller than standard list to fit button padding
                            placeHolder = {
                                Icon(
                                    painter = painterResource(R.drawable.queue_music),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            shape = AbsoluteSmoothCornerShape(
                                cornerRadiusTR = 12.dp, smoothnessAsPercentBR = 60, cornerRadiusBR = 12.dp,
                                smoothnessAsPercentTL = 60, cornerRadiusTL = 12.dp, smoothnessAsPercentBL = 60,
                                cornerRadiusBL = 12.dp, smoothnessAsPercentTR = 60
                            )
                        )

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.playlist.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            val subtitle = if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
                                pluralStringResource(
                                    R.plurals.n_song,
                                    playlist.playlist.remoteSongCount,
                                    playlist.playlist.remoteSongCount
                                )
                            } else {
                                pluralStringResource(
                                    R.plurals.n_song,
                                    playlist.songCount,
                                    playlist.songCount
                                )
                            }
                            
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        if (isAlreadyAdded) {
                            Spacer(Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(R.string.already_in_playlist),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Icon(
                                    painter = painterResource(R.drawable.check),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing
        )
    }

    // duplicate songs warning
    if (showDuplicateDialog) {
        DefaultDialog(
            title = { Text(stringResource(R.string.duplicates)) },
            buttons = {
                TextButton(
                    onClick = {
                        showDuplicateDialog = false
                        onDismiss()
                        database.transaction {
                            addSongToPlaylist(
                                selectedPlaylist!!,
                                songIds!!.filter {
                                    !duplicates.contains(it)
                                }
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.skip_duplicates))
                }

                TextButton(
                    onClick = {
                        showDuplicateDialog = false
                        onDismiss()
                        database.transaction {
                            addSongToPlaylist(selectedPlaylist!!, songIds!!)
                        }
                    }
                ) {
                    Text(stringResource(R.string.add_anyway))
                }

                TextButton(
                    onClick = {
                        showDuplicateDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showDuplicateDialog = false
            }
        ) {
            Text(
                text = if (duplicates.size == 1) {
                    stringResource(R.string.duplicates_description_single)
                } else {
                    stringResource(R.string.duplicates_description_multiple, duplicates.size)
                },
                textAlign = TextAlign.Start,
                modifier = Modifier.align(Alignment.Start)
            )
        }
    }
}
