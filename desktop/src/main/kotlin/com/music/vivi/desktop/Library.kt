package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.pages.LibraryPage

/**
 * Library with tabs for the signed-in user's liked songs, albums, artists and
 * playlists. Prompts for login when there is no session cookie.
 */
@Composable
fun LibraryScreen(
    language: String,
    isLoggedIn: Boolean,
    onOpenLogin: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(Localization.get(language, "library"), style = MaterialTheme.typography.headlineMedium)

        if (!isLoggedIn) {
            Text(
                Localization.get(language, "library_login_prompt"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
            Button(onClick = onOpenLogin, modifier = Modifier.padding(top = 12.dp)) {
                Text(Localization.get(language, "login"))
            }
            return@Column
        }

        val tabs = listOf("songs", "albums", "artists", "playlists")
        val browseIds = listOf(
            "FEmusic_liked_videos",
            "FEmusic_liked_albums",
            "FEmusic_library_corpus_artists",
            "FEmusic_liked_playlists",
        )
        var selectedTab by remember { mutableStateOf(0) }
        var page by remember { mutableStateOf<LibraryPage?>(null) }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(selectedTab) {
            loading = true
            error = null
            page = null
            YouTube.library(browseIds[selectedTab]).fold(
                onSuccess = { page = it; loading = false },
                onFailure = { error = it.message; loading = false },
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEachIndexed { i, key ->
                val selected = i == selectedTab
                Box(
                    Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { selectedTab = i }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        Localization.get(language, key),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        when {
            error != null -> ErrorBox(language, error)
            loading || page == null -> LoadingBox(language)
            else -> {
                val items = page!!.items
                if (items.isEmpty()) {
                    Text(
                        Localization.get(language, "library_empty"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                } else if (selectedTab == 0) {
                    LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                        items(items.filterIsInstance<SongItem>(), key = { it.id }) { song ->
                            SongRow(song, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) })
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(140.dp),
                        modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items, key = { it.id }) { item ->
                            Box(Modifier.fillMaxWidth()) {
                                YtItemCard(
                                    item = item,
                                    onClick = { onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
