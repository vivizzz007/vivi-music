/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.menu

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.music.vivi.LocalDatabase
import com.music.vivi.R
import com.music.vivi.db.entities.PlaylistEntity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.music.vivi.ui.component.TextFieldDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun ImportPlaylistDialog(
    isVisible: Boolean,
    onGetSong: suspend () -> List<String>, // list of song ids. Songs should be inserted to database in this function.
    playlistTitle: String,
    onDismiss: () -> Unit,
) {
    val database = LocalDatabase.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val textFieldValue by remember { mutableStateOf(TextFieldValue(text = playlistTitle)) }
    var songIds by remember {
        mutableStateOf<List<String>?>(null) // list is not saveable
    }

    if (isVisible) {
        TextFieldDialog(
            icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = null) },
            title = { Text(text = stringResource(R.string.import_playlist)) },
            initialTextFieldValue = textFieldValue,
            autoFocus = false,
            onDismiss = onDismiss,
            onDone = { finalName ->
                val newPlaylist = PlaylistEntity(
                    name = finalName
                )

                lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    database.withTransaction {
                        insert(newPlaylist)
                    }
                    val playlist = database.playlist(newPlaylist.id).firstOrNull()

                    if (playlist != null) {
                        songIds = onGetSong()
                        database.addSongToPlaylist(playlist, songIds!!)
                    }
                    // Dialog already dismissed by TextFieldDialog before onDone; avoid second onDismiss().
                }
            }
        )
    }
}
