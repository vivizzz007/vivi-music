package com.music.vivi.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.vivi.sync.SyncedPlaylist
import com.music.vivi.sync.SyncedSong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

/**
 * JSON-backed store for the desktop's local playlists.
 *
 * Playlists are the same [SyncedPlaylist] objects that travel over the sync
 * protocol, so there is no mapping between a "local" and a "wire" type. A
 * deleted playlist stays in the store as a tombstone (`deleted = true`) until
 * it is pruned, so deletions can propagate to the paired phone.
 */
object PlaylistStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    private val file = File(System.getProperty("user.home"), ".vivimusic/playlists.json").apply {
        parentFile?.mkdirs()
    }

    private val _all = MutableStateFlow(load())
    val all: StateFlow<List<SyncedPlaylist>> = _all.asStateFlow()

    /** Active (non-deleted) playlists, most recently updated first. */
    val active: List<SyncedPlaylist>
        get() = _all.value.filter { !it.deleted }.sortedByDescending { it.updatedAt }

    fun get(id: String): SyncedPlaylist? = _all.value.firstOrNull { it.id == id && !it.deleted }

    fun create(name: String): SyncedPlaylist {
        val p = SyncedPlaylist(
            id = newId(),
            name = name.trim(),
            updatedAt = System.currentTimeMillis(),
        )
        _all.value = _all.value + p
        persist()
        return p
    }

    fun rename(id: String, name: String) {
        _all.value = _all.value.map { p ->
            if (p.id == id && !p.deleted) p.copy(name = name.trim(), updatedAt = System.currentTimeMillis()) else p
        }
        persist()
    }

    fun delete(id: String) {
        _all.value = _all.value.map { p ->
            if (p.id == id) p.copy(deleted = true, updatedAt = System.currentTimeMillis()) else p
        }
        persist()
    }

    fun addSongs(id: String, songs: List<SyncedSong>) {
        _all.value = _all.value.map { p ->
            if (p.id == id && !p.deleted) {
                val existing = p.songs.map { it.id }.toSet()
                val added = songs.filter { it.id !in existing }
                p.copy(songs = p.songs + added, updatedAt = System.currentTimeMillis())
            } else p
        }
        persist()
    }

    fun removeSong(id: String, songId: String) {
        _all.value = _all.value.map { p ->
            if (p.id == id && !p.deleted) {
                p.copy(songs = p.songs.filter { it.id != songId }, updatedAt = System.currentTimeMillis())
            } else p
        }
        persist()
    }

    /**
     * Merges a remote playlist list into the store with last-write-wins per
     * playlist id: the copy with the newer [SyncedPlaylist.updatedAt] wins.
     * Local playlists missing from [remote] are kept (they will be pushed back).
     */
    fun applyRemote(remote: List<SyncedPlaylist>) {
        if (remote.isEmpty()) return
        val merged = _all.value.associateBy { it.id }.toMutableMap()
        for (r in remote) {
            val existing = merged[r.id]
            if (existing == null || r.updatedAt > existing.updatedAt) {
                merged[r.id] = r
            }
        }
        _all.value = merged.values.toList()
        persist()
    }

    /** Full state (active + recent tombstones) for the sync snapshot. */
    fun toSynced(): List<SyncedPlaylist> {
        // Prune tombstones older than 30 days so the wire list stays bounded.
        val cutoff = System.currentTimeMillis() - 30L * 24 * 3600 * 1000
        return _all.value.filterNot { it.deleted && it.updatedAt < cutoff }
    }

    private fun newId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val suffix = (1..8).map { chars.random() }.joinToString("")
        return "LP$suffix"
    }

    private fun load(): List<SyncedPlaylist> = try {
        if (file.exists()) json.decodeFromString(file.readText()) else emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    private fun persist() {
        try {
            file.writeText(json.encodeToString(_all.value))
        } catch (_: Exception) {
            // best-effort
        }
    }
}

/** List of the user's local playlists with create / rename / delete actions. */
@Composable
fun LocalPlaylistsScreen(
    language: String,
    onBack: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
) {
    val playlists by PlaylistStore.all.collectAsState()
    val active = playlists.filter { !it.deleted }.sortedByDescending { it.updatedAt }

    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<SyncedPlaylist?>(null) }
    var deleteTarget by remember { mutableStateOf<SyncedPlaylist?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Localization.get(language, "playlists"), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            Button(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(Localization.get(language, "new_playlist"))
            }
        }

        if (active.isEmpty()) {
            Text(
                Localization.get(language, "no_playlists"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(active, key = { it.id }) { p ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenPlaylist(p.id) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                Localization.get(language, "song_count").replace("%d", p.songs.size.toString()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { renameTarget = p }) {
                            Icon(Icons.Filled.Edit, contentDescription = Localization.get(language, "rename"))
                        }
                        IconButton(onClick = { deleteTarget = p }) {
                            Icon(Icons.Filled.Delete, contentDescription = Localization.get(language, "delete"), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        PlaylistNameDialog(
            language = language,
            initialName = "",
            confirmLabel = Localization.get(language, "create"),
            onConfirm = { name -> PlaylistStore.create(name) },
            onDismiss = { showCreate = false },
        )
    }
    renameTarget?.let { target ->
        PlaylistNameDialog(
            language = language,
            initialName = target.name,
            confirmLabel = Localization.get(language, "save"),
            onConfirm = { name -> PlaylistStore.rename(target.id, name) },
            onDismiss = { renameTarget = null },
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(Localization.get(language, "delete_playlist")) },
            text = { Text(Localization.get(language, "delete_playlist_confirm")) },
            confirmButton = {
                TextButton(onClick = {
                    PlaylistStore.delete(target.id)
                    deleteTarget = null
                }) { Text(Localization.get(language, "delete")) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(Localization.get(language, "cancel")) }
            },
        )
    }
}

/** A single local playlist: playable song list with per-song remove. */
@Composable
fun LocalPlaylistScreen(
    playlistId: String,
    language: String,
    onBack: () -> Unit,
    onPlay: (SyncedSong) -> Unit,
    onPlayAll: (List<SyncedSong>) -> Unit,
) {
    val playlists by PlaylistStore.all.collectAsState()
    val playlist = playlists.firstOrNull { it.id == playlistId && !it.deleted }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        if (playlist == null) {
            Text(
                Localization.get(language, "playlist_not_found"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
            return@Column
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(playlist.name, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (playlist.songs.isNotEmpty()) {
                OutlinedButton(onClick = { onPlayAll(playlist.songs) }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Localization.get(language, "play_all"))
                }
            }
        }

        if (playlist.songs.isEmpty()) {
            Text(
                Localization.get(language, "empty_playlist"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(playlist.songs, key = { it.id }) { song ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPlay(song) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Thumbnail(song.thumbnail, Modifier.size(44.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            "✕",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { PlaylistStore.removeSong(playlist.id, song.id) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Dialog to add [song] to one of the existing playlists (or create a new one). */
@Composable
fun AddToPlaylistDialog(
    language: String,
    song: SyncedSong,
    onDismiss: () -> Unit,
) {
    val playlists by PlaylistStore.all.collectAsState()
    val active = playlists.filter { !it.deleted }.sortedByDescending { it.updatedAt }
    var showCreate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Localization.get(language, "add_to_playlist")) },
        text = {
            Column {
                TextButton(onClick = { showCreate = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Localization.get(language, "new_playlist"))
                }
                LazyColumn {
                    items(active, key = { it.id }) { p ->
                        val contains = p.songs.any { it.id == song.id }
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                PlaylistStore.addSongs(p.id, listOf(song))
                                onDismiss()
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                p.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (contains) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (contains) {
                                Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(Localization.get(language, "cancel")) }
        },
    )

    if (showCreate) {
        PlaylistNameDialog(
            language = language,
            initialName = "",
            confirmLabel = Localization.get(language, "create"),
            onConfirm = { name ->
                PlaylistStore.addSongs(PlaylistStore.create(name).id, listOf(song))
                showCreate = false
                onDismiss()
            },
            onDismiss = { showCreate = false },
        )
    }
}

@Composable
private fun PlaylistNameDialog(
    language: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Localization.get(language, "playlist_name")) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(Localization.get(language, "playlist_name")) },
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name)
                        onDismiss()
                    }
                },
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Localization.get(language, "cancel")) }
        },
    )
}

