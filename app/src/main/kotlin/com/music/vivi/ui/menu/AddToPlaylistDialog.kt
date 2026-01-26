package com.music.vivi.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.music.innertube.YouTube
import com.music.innertube.utils.parseCookieString
import com.music.vivi.LocalDatabase
import com.music.vivi.R
import com.music.vivi.constants.AddToPlaylistSortDescendingKey
import com.music.vivi.constants.AddToPlaylistSortTypeKey
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.PlaylistSortType
import com.music.vivi.db.entities.Playlist
import com.music.vivi.ui.component.CreatePlaylistDialog
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.ListDialog
import com.music.vivi.ui.component.SortHeader
import com.music.vivi.ui.component.media.playlists.PlaylistListItem
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.PlaylistsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A dialog for adding songs to a local playlist.
 * Shows a list of existing playlists and an option to create a new one.
 * Handles duplicate checking.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddToPlaylistDialog(
    isVisible: Boolean,
    allowSyncing: Boolean = true,
    initialTextFieldValue: String? = null,
    songsToCheck: List<String>? = null,
    onGetSong: suspend (Playlist) -> List<String>, // list of song ids. Songs should be inserted to database in this function.
    onDismiss: () -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    val playlists: List<Playlist> by viewModel.allPlaylists.collectAsState()

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

    // Check for existing songs in playlists
    LaunchedEffect(playlists, songsToCheck) {
        if (songsToCheck != null && playlists.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val foundIn = mutableSetOf<String>()
                playlists.forEach { playlist ->
                    val dupes = database.playlistDuplicates(playlist.id, songsToCheck)
                    if (dupes.isNotEmpty()) {
                        foundIn.add(playlist.id)
                    }
                }
                containingPlaylists = foundIn
            }
        }
    }

    if (isVisible) {
        ListDialog(
            onDismiss = onDismiss
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilledTonalButton(
                        modifier = Modifier
                            .height(64.dp)
                            .fillMaxWidth(0.8f),
                        shape = CircleShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        onClick = {
                            showCreatePlaylistDialog = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.add),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.create_playlist),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            item {
                val (sortType, onSortTypeChange) = rememberEnumPreference(
                    AddToPlaylistSortTypeKey,
                    PlaylistSortType.CREATE_DATE
                )
                val (sortDescending, onSortDescendingChange) = rememberPreference(AddToPlaylistSortDescendingKey, true)

                SortHeader(
                    sortType = sortType,
                    sortDescending = sortDescending,
                    onSortTypeChange = onSortTypeChange,
                    onSortDescendingChange = onSortDescendingChange,
                    sortTypeText = {
                        when (it) {
                            PlaylistSortType.CREATE_DATE -> R.string.sort_by_create_date
                            PlaylistSortType.NAME -> R.string.sort_by_name
                            PlaylistSortType.SONG_COUNT -> R.string.sort_by_song_count
                            PlaylistSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                        }
                    }
                )
            }

            items(playlists) { playlist ->
                val isAlreadyAdded = containingPlaylists.contains(playlist.id)

                PlaylistListItem(
                    playlist = playlist,
                    modifier = Modifier.clickable {
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
                    },
                    trailingContent = {
                        if (isAlreadyAdded) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.check),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )
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
