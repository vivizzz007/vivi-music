/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalSyncUtils
import com.music.vivi.R
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.SpotifySessionKey
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.extensions.isSyncEnabled
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.logging.Logger

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
    onPlaylistCreated: ((String) -> Unit)? = null,
) {
    val database = LocalDatabase.current
    val syncUtils = LocalSyncUtils.current
    val coroutineScope = rememberCoroutineScope()
    var syncedPlaylist by remember { mutableStateOf(false) }
    var syncedSpotify by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isSignedIn = innerTubeCookie.isNotEmpty()
    val (spotifySession) = rememberPreference(SpotifySessionKey, "")
    val isSpotifySignedIn = spotifySession.isNotEmpty()

    TextFieldDialog(
        icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = null) },
        title = { Text(text = stringResource(R.string.create_playlist)) },
        initialTextFieldValue = TextFieldValue(initialTextFieldValue ?: ""),
        onDismiss = onDismiss,
        onDone = { playlistName ->
            coroutineScope.launch(Dispatchers.IO) {
                val browseId = if (syncedPlaylist && isSignedIn) {
                    YouTube.createPlaylist(playlistName)
                } else if (syncedPlaylist) {
                    Logger.getLogger("CreatePlaylistDialog").warning("Not signed in")
                    return@launch
                } else null

                val playlistEntity = PlaylistEntity(
                    name = playlistName,
                    browseId = browseId,
                    bookmarkedAt = LocalDateTime.now(),
                    isEditable = true,
                )
                
                database.query {
                    insert(playlistEntity)
                }

                if (syncedSpotify && isSpotifySignedIn) {
                    syncUtils.createLinkedSpotifyPlaylist(playlistEntity.id, playlistName)
                }

//                onPlaylistCreated?.invoke(playlistEntity.id)
                withContext(Dispatchers.Main) {
                    onPlaylistCreated?.invoke(playlistEntity.id)
                }
            }
        },
        extraContent = {
            if (allowSyncing) {
                Column {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 40.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.sync_playlist),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = stringResource(R.string.allows_for_sync_witch_youtube),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(0.7f)
                            )
                        }
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Switch(
                                checked = syncedPlaylist,
                                onCheckedChange = {
                                    val isYtmSyncEnabled = context.isSyncEnabled()
                                    if (!isSignedIn && !syncedPlaylist) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.not_logged_in_youtube),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else if (!isYtmSyncEnabled) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.sync_disabled),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        syncedPlaylist = !syncedPlaylist
                                    }
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 40.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.sync_to_spotify),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = stringResource(R.string.sync_to_spotify_desc),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(0.7f)
                            )
                        }
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Switch(
                                checked = syncedSpotify,
                                onCheckedChange = {
                                    if (!isSpotifySignedIn && !syncedSpotify) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.spotify_not_connected),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        syncedSpotify = !syncedSpotify
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}
