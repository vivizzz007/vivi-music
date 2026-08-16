package com.music.vivi.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/** Global non-invasive toast/snackbar for one-shot feedback (copy, …). */
object DesktopSnackbar {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()
    fun show(message: String) { _events.tryEmit(message) }
}

/**
 * In-memory like / library state for the current session. The desktop has no
 * Room database like the Android app, so the toggled state lives here for the
 * session; the authoritative state stays on the YouTube account.
 */
object SongActions {
    private val liked = mutableStateMapOf<String, Boolean>()
    private val inLibrary = mutableStateMapOf<String, Boolean>()

    fun isLiked(id: String): Boolean = liked[id] ?: false

    fun isInLibrary(song: SongItem): Boolean =
        inLibrary[song.id] ?: (song.libraryRemoveToken != null)

    fun setLiked(id: String, value: Boolean) { liked[id] = value }
    fun setInLibrary(id: String, value: Boolean) { inLibrary[id] = value }
}

fun copyToClipboard(text: String) {
    runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }
}

/** "⋮" context menu for a song: like, library, add-to-playlist and share. */
@Composable
fun SongMenu(
    song: SongItem,
    language: String,
    onAddToPlaylist: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val liked = SongActions.isLiked(song.id)
    val inLibrary = SongActions.isInLibrary(song)

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = Localization.get(language, "more"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(Localization.get(language, if (liked) "unlike" else "like")) },
                leadingIcon = {
                    Icon(
                        if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (liked) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    )
                },
                onClick = {
                    expanded = false
                    val next = !liked
                    SongActions.setLiked(song.id, next)
                    scope.launch { YouTube.likeVideo(song.id, next) }
                },
            )
            DropdownMenuItem(
                text = { Text(Localization.get(language, if (inLibrary) "remove_from_library" else "add_to_library")) },
                leadingIcon = {
                    Icon(
                        if (inLibrary) Icons.Filled.LibraryAddCheck else Icons.Filled.LibraryAdd,
                        contentDescription = null,
                    )
                },
                onClick = {
                    expanded = false
                    val next = !inLibrary
                    SongActions.setInLibrary(song.id, next)
                    scope.launch { YouTube.toggleSongLibrary(song.id, addToLibrary = next) }
                },
            )
            if (onAddToPlaylist != null) {
                DropdownMenuItem(
                    text = { Text(Localization.get(language, "add_to_playlist")) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onAddToPlaylist()
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(Localization.get(language, "share")) },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                onClick = {
                    expanded = false
                    copyToClipboard(song.shareLink)
                    DesktopSnackbar.show(Localization.get(language, "copied_to_clipboard"))
                },
            )
        }
    }
}
