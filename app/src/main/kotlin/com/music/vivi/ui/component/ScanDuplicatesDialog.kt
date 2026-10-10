/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalSyncUtils
import com.music.vivi.R
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.PlaylistSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * A set of playlist entries that look like the same song. The entries are ordered by their position in the playlist.
 *
 * [likelySame] is false when the entries share a title and artist but their lengths differ noticeably, which usually
 * means two genuinely different songs (e.g. two different tracks that are both called "Intro").
 */
private data class DuplicateGroup(
    val entries: List<PlaylistSong>,
    val likelySame: Boolean,
)

private const val SAME_SONG_DURATION_TOLERANCE_SECONDS = 4

private fun findDuplicateGroups(songs: List<PlaylistSong>): List<DuplicateGroup> {
    if (songs.size < 2) return emptyList()

    // Union-find over the playlist entries: two entries end up in the same group when they share
    // the song id or the normalised "title|artists" signature.
    val parent = IntArray(songs.size) { it }
    fun find(x: Int): Int {
        var root = x
        while (parent[root] != root) root = parent[root]
        var node = x
        while (parent[node] != root) {
            val next = parent[node]
            parent[node] = root
            node = next
        }
        return root
    }
    fun union(a: Int, b: Int) {
        val ra = find(a)
        val rb = find(b)
        if (ra != rb) parent[rb] = ra
    }

    val byId = HashMap<String, Int>()
    val bySignature = HashMap<String, Int>()
    songs.forEachIndexed { index, entry ->
        byId[entry.song.id]?.let { union(it, index) } ?: run { byId[entry.song.id] = index }

        val title = entry.song.song.title.lowercase().trim()
        val artists = entry.song.artists.joinToString(" ") { it.name }.lowercase().trim()
        val signature = "$title|$artists"
        if (signature.length > 3) {
            bySignature[signature]?.let { union(it, index) } ?: run { bySignature[signature] = index }
        }
    }

    return songs.indices
        .groupBy { find(it) }
        .values
        .filter { it.size > 1 }
        .map { indices ->
            val entries = indices.map { songs[it] }.sortedBy { it.map.position }
            val firstDuration = entries.first().song.song.duration
            val likelySame = entries.all { entry ->
                entry.song.id == entries.first().song.id ||
                    firstDuration <= 0 ||
                    entry.song.song.duration <= 0 ||
                    abs(entry.song.song.duration - firstDuration) <= SAME_SONG_DURATION_TOLERANCE_SECONDS
            }
            DuplicateGroup(entries = entries, likelySame = likelySame)
        }
        .sortedBy { it.entries.first().map.position }
}

private fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return "--:--"
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
fun ScanDuplicatesDialog(
    playlist: Playlist,
    songs: List<PlaylistSong>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val syncUtils = LocalSyncUtils.current
    val coroutineScope = rememberCoroutineScope()

    val groups = remember(songs) { findDuplicateGroups(songs) }

    if (groups.isEmpty()) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, context.getString(R.string.no_duplicates_found), Toast.LENGTH_SHORT).show()
            onDismiss()
        }
        return
    }

    // Ids of the playlist entries (PlaylistSongMap.id) that are ticked for removal. Out of the box every copy but the
    // first one is ticked, except for groups that look like different songs — those are only listed.
    fun recommendedSelection(): List<Int> = groups
        .filter { it.likelySame }
        .flatMap { group -> group.entries.drop(1).map { it.map.id } }

    val selected = remember(groups) { mutableStateListOf<Int>().apply { addAll(recommendedSelection()) } }

    DefaultDialog(
        onDismiss = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.content_copy),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(text = stringResource(R.string.duplicates_found_title))
        },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
            TextButton(
                enabled = selected.isNotEmpty(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                onClick = {
                    // Remove from the bottom of the playlist upwards: removing an entry shifts every later entry up by
                    // one, so going bottom-up keeps the remaining stored positions valid.
                    val toRemove = groups
                        .flatMap { it.entries }
                        .filter { it.map.id in selected }
                        .sortedByDescending { it.map.position }
                    val count = toRemove.size
                    coroutineScope.launch(Dispatchers.IO) {
                        for (entry in toRemove) {
                            val map = entry.map
                            syncUtils.markSongRemovedFromPlaylist(
                                playlistId = playlist.id,
                                browseId = playlist.playlist.browseId,
                                songId = map.songId,
                                setVideoId = map.setVideoId
                            )
                            database.transaction {
                                move(playlist.id, map.position, Int.MAX_VALUE)
                                delete(map.copy(position = Int.MAX_VALUE))
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.duplicates_removed_toast, count),
                                Toast.LENGTH_SHORT
                            ).show()
                            onDismiss()
                        }
                    }
                }
            ) {
                Text(text = stringResource(R.string.remove_duplicates_count, selected.size))
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = pluralStringResource(R.plurals.duplicate_groups_found, groups.size, groups.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        selected.clear()
                        selected.addAll(groups.flatMap { group -> group.entries.drop(1).map { it.map.id } })
                    }
                ) {
                    Text(text = stringResource(R.string.duplicates_select_all_but_first))
                }
                TextButton(onClick = { selected.clear() }) {
                    Text(text = stringResource(R.string.duplicates_clear_selection))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groups, key = { group -> group.entries.first().map.id }) { group ->
                    DuplicateGroupCard(
                        group = group,
                        isSelected = { id -> id in selected },
                        onToggle = { id ->
                            if (id in selected) selected.remove(id) else selected.add(id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    isSelected: (Int) -> Boolean,
    onToggle: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = pluralStringResource(R.plurals.duplicate_copies, group.entries.size, group.entries.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        if (!group.likelySame) {
            Text(
                text = stringResource(R.string.duplicates_different_length_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        group.entries.forEach { entry ->
            val id = entry.map.id
            val checked = isSelected(id)
            val song = entry.song
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(id) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { onToggle(id) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.error,
                        checkmarkColor = MaterialTheme.colorScheme.onError
                    )
                )
                AsyncImage(
                    model = song.song.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val details = listOfNotNull(
                        song.album?.title,
                        formatDuration(song.song.duration),
                        stringResource(R.string.duplicates_position, entry.map.position + 1)
                    ).joinToString(" • ")
                    Text(
                        text = details,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(if (checked) R.string.duplicates_remove else R.string.duplicates_keep),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (checked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}
