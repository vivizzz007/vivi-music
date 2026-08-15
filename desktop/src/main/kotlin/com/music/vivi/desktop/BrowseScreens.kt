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
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.pages.BrowseResult
import com.music.innertube.pages.HomePage
import com.music.innertube.pages.MoodAndGenres
import com.music.innertube.pages.SearchResult
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
    var filterItems by remember { mutableStateOf<List<YTItem>?>(null) }
    var selectedFilter by remember { mutableStateOf<YouTube.SearchFilter?>(null) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var focused by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Results: summary when "All", filtered results when a chip is selected.
    LaunchedEffect(query, selectedFilter) {
        val q = query.trim()
        if (q.isEmpty()) {
            page = null
            filterItems = null
            error = null
            loading = false
            return@LaunchedEffect
        }
        loading = true
        val filter = selectedFilter
        if (filter == null) {
            YouTube.searchSummary(q).fold(
                onSuccess = { page = it; filterItems = null; error = null },
                onFailure = { error = it.message },
            )
        } else {
            YouTube.search(q, filter).fold(
                onSuccess = { page = null; filterItems = it.items.distinctBy { item -> item.id }; error = null },
                onFailure = { error = it.message },
            )
        }
        loading = false
    }

    // Live suggestions while typing (hidden once results are shown).
    LaunchedEffect(query, focused) {
        val q = query.trim()
        if (!focused || q.isEmpty() || page != null || filterItems != null) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        suggestions = YouTube.searchSuggestions(q).getOrNull()?.queries.orEmpty().take(6)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            singleLine = true,
            placeholder = { Text(Localization.get(language, "search_placeholder")) },
        )

        // Filter chips (All / Songs / Videos / Albums / Artists / Playlists).
        val filters = listOf(
            null to Localization.get(language, "filter_all"),
            YouTube.SearchFilter.FILTER_SONG to Localization.get(language, "filter_songs"),
            YouTube.SearchFilter.FILTER_VIDEO to Localization.get(language, "filter_videos"),
            YouTube.SearchFilter.FILTER_ALBUM to Localization.get(language, "filter_albums"),
            YouTube.SearchFilter.FILTER_ARTIST to Localization.get(language, "filter_artists"),
            YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST to Localization.get(language, "filter_playlists"),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(filters.size) { i ->
                val (filter, label) = filters[i]
                val selected = selectedFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { selectedFilter = if (selected) null else filter },
                    label = { Text(label) },
                )
            }
        }

        // Suggestion list.
        if (focused && query.isNotBlank() && suggestions.isNotEmpty() && page == null && filterItems == null) {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    suggestions.forEach { s ->
                        Text(
                            s,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { query = s; focused = false }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }

        val result = page
        when {
            error != null -> ErrorBox(language, error)
            query.isBlank() -> Text(
                Localization.get(language, "search"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
            loading && result == null && filterItems == null -> LoadingBox(language)
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
                if (result.summaries.isEmpty()) {
                    item {
                        Text(
                            Localization.get(language, "no_results_found"),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
            }
            filterItems != null -> {
                val results = filterItems!!
                if (results.all { it is SongItem }) {
                    LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                        items(results.filterIsInstance<SongItem>(), key = { it.id }) { song ->
                            SongRow(song, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) })
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(160.dp),
                        modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        gridItems(results, key = { it.id }) { item ->
                            YtItemCard(
                                item = item,
                                width = null,
                                onClick = { onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) },
                            )
                        }
                    }
                }
                if (results.isEmpty()) {
                    Text(
                        Localization.get(language, "no_results_found"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
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
