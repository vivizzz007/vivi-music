package com.music.vivi.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
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
import com.music.innertube.pages.BrowseResult
import com.music.innertube.pages.HomePage
import com.music.innertube.pages.MoodAndGenres
import com.music.innertube.pages.SearchSummary
import com.music.innertube.pages.SearchSummaryPage

@Composable
fun HomeScreen(
    language: String,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onPlayAll: (List<SongItem>) -> Unit,
    onOpenBrowse: (String, String?) -> Unit,
) {
    var home by remember { mutableStateOf<HomePage?>(null) }
    var moodAndGenres by remember { mutableStateOf<List<MoodAndGenres>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedChip by remember { mutableStateOf<HomePage.Chip?>(null) }

    LaunchedEffect(selectedChip) {
        val params = selectedChip?.endpoint?.params
        YouTube.home(params = params).fold(
            onSuccess = { home = it; error = null },
            onFailure = { error = it.message },
        )
    }

    LaunchedEffect(Unit) {
        YouTube.moodAndGenres().fold(
            onSuccess = { moodAndGenres = it },
            onFailure = { /* mood & genres is optional; keep the rest of Home working */ },
        )
    }

    when {
        error != null && home == null -> ErrorBox(language, error)
        home == null -> LoadingBox(language)
        else -> LazyColumn(Modifier.fillMaxSize()) {
            val page = home!!

            if (!page.chips.isNullOrEmpty()) {
                item(key = "chips") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        items(page.chips!!.filter { !it.title.equals("Podcasts", ignoreCase = true) }, key = { it.title }) { chip ->
                            val selected = selectedChip?.title == chip.title
                            FilterChip(
                                selected = selected,
                                onClick = { selectedChip = if (selected) null else chip },
                                label = { Text(chip.title) },
                            )
                        }
                    }
                }
            }

            page.sections.forEachIndexed { index, section ->
                val songs = section.items.filterIsInstance<SongItem>()
                val isSongsOnly = section.items.isNotEmpty() && songs.size == section.items.size

                item(key = "header-$index-${section.title}") {
                    SectionHeader(
                        title = section.title,
                        label = section.label,
                        language = language,
                        onClick = section.endpoint?.let { ep -> { onOpenBrowse(ep.browseId, ep.params) } },
                        onPlayAll = if (isSongsOnly && songs.isNotEmpty()) {
                            { onPlayAll(songs.distinctBy { it.id }) }
                        } else null,
                    )
                }

                item(key = "content-$index-${section.title}") {
                    if (isSongsOnly) {
                        // Songs-only section (e.g. Quick picks) renders as a horizontal
                        // list of song rows, matching the Android Home screen.
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        ) {
                            items(songs.distinctBy { it.id }, key = { it.id }) { song ->
                                Box(Modifier.width(320.dp)) {
                                    SongRow(song = song, onClick = { onPlaySong(song) })
                                }
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        ) {
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

            val moodItems = moodAndGenres?.flatMap { it.items }
            if (!moodItems.isNullOrEmpty()) {
                item(key = "mood_header") {
                    SectionHeader(title = Localization.get(language, "mood_and_genres"), language = language)
                }
                item(key = "mood_list") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        items(moodItems, key = { it.endpoint.browseId + it.title }) { item ->
                            MoodAndGenresButton(
                                title = item.title,
                                onClick = { onOpenBrowse(item.endpoint.browseId, item.endpoint.params) },
                                modifier = Modifier.width(180.dp),
                            )
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Box(Modifier.fillMaxWidth().padding(bottom = 16.dp))
            }
        }
    }
}

/** Generic browse screen (mood/genre pages, "More" endpoints, etc.). */
@Composable
fun BrowseScreen(
    browseId: String,
    params: String?,
    language: String,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
) {
    var result by remember { mutableStateOf<BrowseResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(browseId, params) {
        YouTube.browse(browseId, params).fold(
            onSuccess = { result = it; error = null },
            onFailure = { error = it.message },
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        when {
            error != null -> ErrorBox(language, error)
            result == null -> LoadingBox(language)
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                result!!.items.forEach { section ->
                    if (!section.title.isNullOrBlank()) {
                        item(key = "header-${section.title}", span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader(title = section.title!!, language = language)
                        }
                    }
                    section.items.forEach { ytItem ->
                        item(key = ytItem.id) {
                            YtItemCard(
                                item = ytItem,
                                width = null,
                                onClick = { onItemClick(ytItem, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) },
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
