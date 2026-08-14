package com.music.vivi.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.pages.AlbumPage
import com.music.innertube.pages.ArtistPage
import com.music.innertube.pages.HistoryPage
import com.music.innertube.pages.PlaylistPage

@Composable
fun AlbumScreen(
    browseId: String,
    language: String,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onPlayAll: (List<SongItem>) -> Unit,
) {
    var page by remember { mutableStateOf<AlbumPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(browseId) {
        YouTube.album(browseId).fold(
            onSuccess = { page = it },
            onFailure = { error = it.message },
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        when {
            error != null -> ErrorBox(language, error)
            page == null -> LoadingBox(language)
            else -> {
                val album = page!!.album
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Thumbnail(album.thumbnail, Modifier.size(128.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(album.title, style = MaterialTheme.typography.headlineMedium)
                        val artists = album.artists?.joinToString(", ") { it.name }.orEmpty()
                        if (artists.isNotBlank()) {
                            Text(
                                artists,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        album.year?.let { Text(it.toString(), style = MaterialTheme.typography.bodyMedium) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Localization.get(language, "songs"), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { onPlayAll(page!!.songs) }) { Text(Localization.get(language, "play_all")) }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(page!!.songs, key = { it.id }) { song ->
                        SongRow(song, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) })
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistScreen(
    browseId: String,
    language: String,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
) {
    var page by remember { mutableStateOf<ArtistPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(browseId) {
        YouTube.artist(browseId).fold(
            onSuccess = { page = it },
            onFailure = { error = it.message },
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        when {
            error != null -> ErrorBox(language, error)
            page == null -> LoadingBox(language)
            else -> {
                val artist = page!!.artist
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Thumbnail(artist.thumbnail, Modifier.size(128.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(artist.title, style = MaterialTheme.typography.headlineMedium)
                        page!!.subscriberCountText?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    page!!.sections.forEach { section ->
                        item(key = "header-${section.title}") {
                            Text(section.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                        }
                        val songs = section.items.filterIsInstance<SongItem>()
                        val others = section.items.filterNot { it is SongItem }
                        if (songs.isNotEmpty()) {
                            items(songs, key = { "song-${it.id}" }) { song ->
                                SongRow(song, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) })
                            }
                        }
                        if (others.isNotEmpty()) {
                            item(key = "carousel-${section.title}") {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(others, key = { it.id }) { item ->
                                        YtItemCard(item = item, onClick = { onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistScreen(
    playlistId: String,
    language: String,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onPlayAll: (List<SongItem>) -> Unit,
) {
    var page by remember { mutableStateOf<PlaylistPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(playlistId) {
        YouTube.playlist(playlistId).fold(
            onSuccess = { page = it },
            onFailure = { error = it.message },
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        when {
            error != null -> ErrorBox(language, error)
            page == null -> LoadingBox(language)
            else -> {
                val playlist = page!!.playlist
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Thumbnail(playlist.thumbnail, Modifier.size(128.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(playlist.title, style = MaterialTheme.typography.headlineMedium)
                        playlist.author?.let {
                            Text(it.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        playlist.songCountText?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { onPlayAll(page!!.songs) }) { Text(Localization.get(language, "play_all")) }
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(page!!.songs, key = { it.id }) { song ->
                        SongRow(song, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) })
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(language: String) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(Localization.get(language, "library"), style = MaterialTheme.typography.headlineMedium)
        Text(
            Localization.get(language, "library_placeholder"),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
fun HistoryScreen(
    language: String,
    onBack: () -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
) {
    var page by remember { mutableStateOf<HistoryPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        YouTube.musicHistory().fold(
            onSuccess = { page = it },
            onFailure = { error = it.message },
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Text(Localization.get(language, "history"), style = MaterialTheme.typography.headlineMedium)
        when {
            error != null -> ErrorBox(language, error)
            page == null -> LoadingBox(language)
            else -> {
                val sections = page!!.sections.orEmpty()
                if (sections.isEmpty()) {
                    Text(
                        Localization.get(language, "history_empty"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                } else {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                        sections.forEach { section ->
                            item(key = "header-${section.title}") {
                                Text(
                                    section.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                                )
                            }
                            items(section.songs, key = { "song-${it.id}" }) { song ->
                                SongRow(song, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) })
                            }
                        }
                    }
                }
            }
        }
    }
}
