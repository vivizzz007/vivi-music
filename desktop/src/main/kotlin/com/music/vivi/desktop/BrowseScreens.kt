package com.music.vivi.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.pages.HomePage
import com.music.innertube.pages.SearchSummary
import com.music.innertube.pages.SearchSummaryPage

@Composable
fun HomeScreen(
    language: String,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
) {
    var home by remember { mutableStateOf<HomePage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        YouTube.home().fold(
            onSuccess = { home = it },
            onFailure = { error = it.message },
        )
    }

    when {
        error != null -> ErrorBox(language, error)
        home == null -> LoadingBox(language)
        else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            val sections = home!!.sections
            sections.forEach { section ->
                item(key = "header-${section.title}") {
                    Text(
                        section.title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    )
                }
                item(key = "row-${section.title}") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(section.items, key = { it.id }) { item ->
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

@Composable
fun SearchScreen(
    language: String,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var page by remember { mutableStateOf<SearchSummaryPage?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isEmpty()) {
            page = null
            error = null
            loading = false
            return@LaunchedEffect
        }
        loading = true
        YouTube.searchSummary(q).fold(
            onSuccess = { page = it; error = null },
            onFailure = { error = it.message },
        )
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(Localization.get(language, "search_placeholder")) },
        )

        val result = page
        when {
            error != null -> ErrorBox(language, error)
            query.isBlank() -> Text(
                Localization.get(language, "search"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
            loading && result == null -> LoadingBox(language)
            result != null -> LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                result.summaries.forEach { summary ->
                    item(key = "header-${summary.title}") {
                        Text(
                            summary.title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        )
                    }
                    item(key = "body-${summary.title}") {
                        SummaryBody(summary, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong, onAddToQueue)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryBody(
    summary: SearchSummary,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
) {
    val songs = summary.items.filterIsInstance<SongItem>()
    val others = summary.items.filterNot { it is SongItem }

    Column {
        songs.forEach { song ->
            SongRow(song, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) })
        }
        if (others.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(others, key = { it.id }) { item ->
                    YtItemCard(item = item, onClick = { onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) })
                }
            }
        }
    }
}
