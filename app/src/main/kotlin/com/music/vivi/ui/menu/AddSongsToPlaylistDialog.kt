/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.menu

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.LocalSyncUtils
import com.music.vivi.R
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.PlaylistSong
import com.music.vivi.constants.ArtistSongSortType
import com.music.vivi.models.MediaMetadata
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.utils.makeTimeString
import com.music.vivi.ui.utils.resize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class RecommendedSong(
    val metadata: MediaMetadata,
    val source: String, // "Playlist", "Followed Artist"
)

@Serializable
private data class CachedSongDto(
    val id: String,
    val title: String,
    val artistNames: List<String>,
    val artistIds: List<String?> = emptyList(),
    val duration: Int,
    val thumbnailUrl: String? = null,
    val albumName: String? = null,
    val albumId: String? = null,
    val source: String,
)

@Serializable
private data class RecommendedSongsCache(
    val timestamp: Long,
    val songs: List<CachedSongDto>,
)

private val recJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

private fun loadCachedRecommendations(cacheFile: File): List<RecommendedSong>? {
    return runCatching {
        if (!cacheFile.exists()) return null
        val text = cacheFile.readText()
        val cache = recJson.decodeFromString<RecommendedSongsCache>(text)
        if (System.currentTimeMillis() - cache.timestamp < 24 * 60 * 60 * 1000L) {
            cache.songs.map { dto ->
                RecommendedSong(
                    metadata = MediaMetadata(
                        id = dto.id,
                        title = dto.title,
                        artists = dto.artistNames.mapIndexed { idx, name ->
                            MediaMetadata.Artist(id = dto.artistIds.getOrNull(idx), name = name)
                        },
                        duration = dto.duration,
                        thumbnailUrl = dto.thumbnailUrl,
                        album = dto.albumName?.let { MediaMetadata.Album(id = dto.albumId ?: "", title = it) }
                    ),
                    source = dto.source
                )
            }
        } else {
            null
        }
    }.getOrNull()
}

private fun saveCachedRecommendations(cacheFile: File, songs: List<RecommendedSong>) {
    runCatching {
        val dtos = songs.map { rec ->
            CachedSongDto(
                id = rec.metadata.id,
                title = rec.metadata.title,
                artistNames = rec.metadata.artists.map { it.name },
                artistIds = rec.metadata.artists.map { it.id },
                duration = rec.metadata.duration,
                thumbnailUrl = rec.metadata.thumbnailUrl,
                albumName = rec.metadata.album?.title,
                albumId = rec.metadata.album?.id,
                source = rec.source
            )
        }
        val cache = RecommendedSongsCache(
            timestamp = System.currentTimeMillis(),
            songs = dtos
        )
        cacheFile.writeText(recJson.encodeToString(cache))
    }
}

private fun normalizeTitleForPlaylist(rawTitle: String): String {
    var title = rawTitle.lowercase(Locale.ROOT)
    // Remove content inside brackets/parentheses: (official music video), [live], (feat. xyz), etc.
    title = title.replace(Regex("\\[.*?\\]|\\(.*?\\)"), "")
    // Remove common trailing or standalone noise words
    title = title.replace(Regex("(?i)\\b(official\\s*(music)?\\s*video|official\\s*audio|lyric(s)?\\s*video|lyrics|live\\s*performance|live\\s*version|live|remastered|remaster|visualizer|video\\s*clip|clip\\s*officiel|hd|4k|audio|feat\\.?|ft\\.?)\\b"), "")
    // Remove leading track numbers like "01 - " or "1. "
    title = title.replace(Regex("^[0-9]+[.\\-\\s]+"), "")
    // Replace punctuation/delimiters with space
    title = title.replace(Regex("[\\-_|:;~•]"), " ")
    // Strip non-letter non-digit non-space
    title = title.replace(Regex("[^\\p{L}\\p{Nd}\\s]"), "")
    // Collapse whitespace
    return title.replace(Regex("\\s+"), " ").trim()
}

private fun isSongInPlaylist(
    songId: String,
    title: String,
    artists: List<String>,
    existingSongIds: Set<String>,
    currentSongs: List<PlaylistSong>,
): Boolean {
    if (existingSongIds.contains(songId)) return true
    val normTitle = normalizeTitleForPlaylist(title)
    if (normTitle.length < 2) return false
    val candidateArtists = artists.map { it.lowercase(Locale.ROOT).trim() }.filter { it.isNotEmpty() }

    return currentSongs.any { existing ->
        val existingNormTitle = normalizeTitleForPlaylist(existing.song.title)
        if (existingNormTitle.length < 2) return@any false

        val exactMatch = normTitle == existingNormTitle
        val substringMatch = (normTitle.length >= 4 && existingNormTitle.length >= 4) &&
                (normTitle.contains(existingNormTitle) || existingNormTitle.contains(normTitle))

        if (!exactMatch && !substringMatch) return@any false

        val existingArtists: List<String> = existing.song.artists.map { it.name.lowercase(Locale.ROOT).trim() }.filter { it.isNotEmpty() }
        if (candidateArtists.isEmpty() || existingArtists.isEmpty()) {
            true
        } else {
            candidateArtists.any { ca: String ->
                existingArtists.any { ea: String ->
                    ca == ea || ca.contains(ea) || ea.contains(ca)
                }
            }
        }
    }
}


private data class PrecomputedSong(
    val id: String,
    val normTitle: String,
    val artists: List<String>,
)

object AddToPlaylistCache {
    val playlistRecommendations = mutableMapOf<String, List<RecommendedSong>>()
    var globalRecommendations: List<RecommendedSong> = emptyList()
    var lastSearchQuery: String = ""
    var lastSearchSongs: List<MediaMetadata> = emptyList()
    var lastSearchArtists: List<ArtistItem> = emptyList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongsToPlaylistDialog(
    playlist: Playlist,
    currentSongs: List<PlaylistSong>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val syncUtils = LocalSyncUtils.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val existingSongIds = remember(currentSongs) {
        currentSongs.map { it.song.id }.toSet()
    }
    val precomputedExisting = remember(currentSongs) {
        currentSongs.map { ps ->
            PrecomputedSong(
                id = ps.song.id,
                normTitle = normalizeTitleForPlaylist(ps.song.title),
                artists = ps.song.artists.map { it.name.lowercase(Locale.ROOT).trim() }.filter { it.isNotEmpty() }
            )
        }
    }
    val checkInPlaylist: (String, String, List<String>) -> Boolean = remember(existingSongIds, precomputedExisting) {
        { id, title, artists ->
            if (existingSongIds.contains(id)) true
            else {
                val normTitle = normalizeTitleForPlaylist(title)
                if (normTitle.length < 2) false
                else {
                    val candidateArtists = artists.map { it.lowercase(Locale.ROOT).trim() }.filter { it.isNotEmpty() }
                    precomputedExisting.any { existing ->
                        if (existing.normTitle.length < 2) false
                        else {
                            val exactMatch = normTitle == existing.normTitle
                            val substringMatch = (normTitle.length >= 4 && existing.normTitle.length >= 4) &&
                                    (normTitle.contains(existing.normTitle) || existing.normTitle.contains(normTitle))
                            if (!exactMatch && !substringMatch) false
                            else if (candidateArtists.isEmpty() || existing.artists.isEmpty()) true
                            else {
                                candidateArtists.any { ca ->
                                    existing.artists.any { ea ->
                                        ca == ea || ca.contains(ea) || ea.contains(ca)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val playerConnection = LocalPlayerConnection.current
    val currentMediaMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }
    val isPlaying by playerConnection?.isEffectivelyPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var previewDurationMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isPlaying, currentMediaMetadata?.id) {
        if (isPlaying && currentMediaMetadata != null) {
            while (isActive) {
                currentPositionMs = playerConnection?.player?.currentPosition?.coerceAtLeast(0L) ?: 0L
                val rawDuration = playerConnection?.player?.duration ?: 0L
                if (rawDuration > 0 && rawDuration != androidx.media3.common.C.TIME_UNSET) {
                    previewDurationMs = rawDuration
                } else if ((currentMediaMetadata?.duration ?: 0) > 0) {
                    previewDurationMs = (currentMediaMetadata?.duration ?: 0) * 1000L
                }
                delay(150L)
            }
        } else {
            currentPositionMs = playerConnection?.player?.currentPosition?.coerceAtLeast(0L) ?: 0L
            if ((currentMediaMetadata?.duration ?: 0) > 0) {
                previewDurationMs = (currentMediaMetadata?.duration ?: 0) * 1000L
            }
        }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val selectedSongs = remember { mutableStateMapOf<String, MediaMetadata>() }

    // Search tab state
    var searchQuery by rememberSaveable { mutableStateOf(AddToPlaylistCache.lastSearchQuery) }
    var isSearching by remember { mutableStateOf(false) }
    var searchSongResults by remember { mutableStateOf<List<MediaMetadata>>(AddToPlaylistCache.lastSearchSongs) }
    var searchArtistResults by remember { mutableStateOf<List<ArtistItem>>(AddToPlaylistCache.lastSearchArtists) }
    var selectedArtistName by remember { mutableStateOf<String?>(null) }
    var artistSongsList by remember { mutableStateOf<List<MediaMetadata>>(emptyList()) }
    var isLoadingArtistSongs by remember { mutableStateOf(false) }

    // Suggestions state
    var searchSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var searchHistoryList by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        database.searchHistory().collect { history ->
            searchHistoryList = history.map { it.query }.distinct().take(10)
        }
    }

    LaunchedEffect(searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.length >= 2) {
            try {
                searchSuggestions = YouTube.searchSuggestions(trimmed).getOrNull()?.queries.orEmpty()
            } catch (_: Exception) {
                searchSuggestions = emptyList()
            }
        } else {
            searchSuggestions = emptyList()
        }
    }

    // Recommended tab state
    val initialCachedRecs = remember {
        AddToPlaylistCache.playlistRecommendations[playlist.id]
            ?: AddToPlaylistCache.globalRecommendations.takeIf { it.isNotEmpty() }
            ?: runCatching {
                loadCachedRecommendations(File(context.cacheDir, "rec_cache_${playlist.id}.json"))
            }.getOrNull().orEmpty()
    }
    var recommendedSongs by remember { mutableStateOf(initialCachedRecs) }
    var isLoadingRecommendations by remember { mutableStateOf(recommendedSongs.isEmpty()) }
    var recommendationFilter by rememberSaveable { mutableStateOf("All") }

    fun performSearch(queryText: String) {
        val q = queryText.trim()
        if (q.isEmpty()) return
        isSearching = true
        selectedArtistName = null
        artistSongsList = emptyList()

        scope.launch(Dispatchers.IO) {
            val songsList = mutableListOf<MediaMetadata>()

            // 1. Local database search
            try {
                val localSongs = database.searchSongs(q, 20).first()
                localSongs.forEach { song ->
                    songsList.add(song.toMediaMetadata())
                }
            } catch (_: Exception) {}

            // 2. YouTube online song search
            try {
                val onlineResult = YouTube.search(q, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                onlineResult?.items?.filterIsInstance<SongItem>()?.forEach { songItem ->
                    if (songsList.none { it.id == songItem.id }) {
                        songsList.add(songItem.toMediaMetadata())
                    }
                }
            } catch (_: Exception) {}

            // 3. YouTube online artist search
            val artists = try {
                YouTube.search(q, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()
                    ?.items?.filterIsInstance<ArtistItem>().orEmpty()
            } catch (_: Exception) {
                emptyList()
            }

            withContext(Dispatchers.Main) {
                searchSongResults = songsList
                searchArtistResults = artists
                isSearching = false
                AddToPlaylistCache.lastSearchQuery = q
                AddToPlaylistCache.lastSearchSongs = songsList
                AddToPlaylistCache.lastSearchArtists = artists
            }
        }
    }

    fun loadArtistSongs(artist: ArtistItem) {
        selectedArtistName = artist.title
        isLoadingArtistSongs = true
        scope.launch(Dispatchers.IO) {
            val songs = mutableListOf<MediaMetadata>()
            try {
                // Online artist page sections
                val page = YouTube.artist(artist.id).getOrNull()
                page?.sections?.flatMap { it.items }?.filterIsInstance<SongItem>()?.forEach {
                    songs.add(it.toMediaMetadata())
                }
            } catch (_: Exception) {}

            try {
                // Local artist songs
                val localSongs = database.artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true).first()
                localSongs.forEach { song ->
                    if (songs.none { it.id == song.id }) {
                        songs.add(song.toMediaMetadata())
                    }
                }
            } catch (_: Exception) {}

            withContext(Dispatchers.Main) {
                artistSongsList = songs.distinctBy { it.id }
                isLoadingArtistSongs = false
            }
        }
    }

    fun loadRecommendations(forceRefresh: Boolean = false) {
        isLoadingRecommendations = true
        scope.launch(Dispatchers.IO) {
            val cacheFile = File(context.cacheDir, "rec_cache_${playlist.id}.json")
            if (!forceRefresh) {
                val cached = loadCachedRecommendations(cacheFile)
                if (cached != null && cached.isNotEmpty()) {
                    val filtered = cached.filterNot { rec ->
                        val artists = rec.metadata.artists.map { it.name }
                        checkInPlaylist(rec.metadata.id, rec.metadata.title, artists)
                    }
                    AddToPlaylistCache.playlistRecommendations[playlist.id] = filtered
                    if (filtered.isNotEmpty()) {
                        AddToPlaylistCache.globalRecommendations = filtered
                    }
                    withContext(Dispatchers.Main) {
                        recommendedSongs = filtered
                        isLoadingRecommendations = false
                    }
                    return@launch
                }
            }

            val recList = mutableListOf<RecommendedSong>()
            val seenIds = existingSongIds.toMutableSet()
            val seenNormalizedTitles = mutableSetOf<String>()

            // 1. Based on Playlist (Algorithmic radio recommendations from playlist tracks)
            if (currentSongs.isNotEmpty()) {
                val seedSongs = currentSongs.shuffled().take(4)
                for (seed in seedSongs) {
                    try {
                        val radioEndpoint = WatchEndpoint(videoId = seed.song.id, playlistId = "RDAMVM${seed.song.id}")
                        val nextResult = YouTube.next(radioEndpoint).getOrNull()
                            ?: YouTube.next(WatchEndpoint(videoId = seed.song.id)).getOrNull()

                        nextResult?.items?.filterIsInstance<SongItem>()?.forEach { songItem ->
                            val artists = songItem.artists.map { it.name }
                            val normTitle = normalizeTitleForPlaylist(songItem.title)
                            if (!checkInPlaylist(songItem.id, songItem.title, artists) &&
                                seenIds.add(songItem.id) &&
                                (normTitle.length < 3 || seenNormalizedTitles.add(normTitle))
                            ) {
                                recList.add(RecommendedSong(songItem.toMediaMetadata(), "Playlist"))
                            }
                        }

                        nextResult?.relatedEndpoint?.let { relatedEndpoint ->
                            val relatedPage = YouTube.related(relatedEndpoint).getOrNull()
                            relatedPage?.songs?.forEach { songItem ->
                                val artists = songItem.artists.map { it.name }
                                val normTitle = normalizeTitleForPlaylist(songItem.title)
                                if (!checkInPlaylist(songItem.id, songItem.title, artists) &&
                                    seenIds.add(songItem.id) &&
                                    (normTitle.length < 3 || seenNormalizedTitles.add(normTitle))
                                ) {
                                    recList.add(RecommendedSong(songItem.toMediaMetadata(), "Playlist"))
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }

            // 2. Based on Followed Artists
            try {
                val bookmarkedArtists = database.artistsBookmarkedByNameAsc().first()
                for (artist in bookmarkedArtists.shuffled().take(3)) {
                    try {
                        if (artist.artist.isYouTubeArtist) {
                            val page = YouTube.artist(artist.id).getOrNull()
                            page?.sections?.flatMap { it.items }?.filterIsInstance<SongItem>()?.forEach { songItem ->
                                val artists = songItem.artists.map { it.name }
                                val normTitle = normalizeTitleForPlaylist(songItem.title)
                                if (!checkInPlaylist(songItem.id, songItem.title, artists) &&
                                    seenIds.add(songItem.id) &&
                                    (normTitle.length < 3 || seenNormalizedTitles.add(normTitle))
                                ) {
                                    recList.add(RecommendedSong(songItem.toMediaMetadata(), "Followed Artist"))
                                }
                            }
                        } else {
                            database.artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true).first().forEach { song ->
                                val artists = song.artists.map { it.name }
                                val normTitle = normalizeTitleForPlaylist(song.song.title)
                                if (!checkInPlaylist(song.id, song.song.title, artists) &&
                                    seenIds.add(song.id) &&
                                    (normTitle.length < 3 || seenNormalizedTitles.add(normTitle))
                                ) {
                                    recList.add(RecommendedSong(song.toMediaMetadata(), "Followed Artist"))
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}

            saveCachedRecommendations(cacheFile, recList)
            AddToPlaylistCache.playlistRecommendations[playlist.id] = recList
            if (recList.isNotEmpty()) {
                AddToPlaylistCache.globalRecommendations = recList
            }

            withContext(Dispatchers.Main) {
                recommendedSongs = recList
                isLoadingRecommendations = false
            }
        }
    }

    // Load recommendations once with daily cache
    LaunchedEffect(Unit) {
        loadRecommendations(forceRefresh = false)
    }

    // Pre-fetch stream URLs in the background for recommended songs
    LaunchedEffect(recommendedSongs) {
        if (recommendedSongs.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                recommendedSongs.take(8).forEach { rec ->
                    playerConnection?.prefetchSong(rec.metadata.id)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(0.9f)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.add_to_playlist),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = playlist.playlist.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (selectedSongs.isNotEmpty()) {
                    TextButton(onClick = { selectedSongs.clear() }) {
                        Text(stringResource(R.string.clear))
                    }
                }
            }

            // Tabs: Search vs Recommended
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.search)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.tab_recommended)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.star),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (selectedTab == 0) {
                    // Search Tab Content
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                if (it.length >= 3) {
                                    performSearch(it)
                                }
                            },
                            placeholder = { Text(stringResource(R.string.search)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.search),
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        searchSongResults = emptyList()
                                        searchArtistResults = emptyList()
                                        selectedArtistName = null
                                        artistSongsList = emptyList()
                                    }) {
                                        Icon(
                                            painter = painterResource(R.drawable.close),
                                            contentDescription = null
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                focusManager.clearFocus()
                                performSearch(searchQuery)
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Suggestions Row (live search suggestions or recent searches)
                        val suggestions = if (searchQuery.isNotBlank()) searchSuggestions else searchHistoryList
                        if (suggestions.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(suggestions) { itemText ->
                                    SuggestionChip(
                                        onClick = {
                                            searchQuery = itemText
                                            focusManager.clearFocus()
                                            performSearch(itemText)
                                        },
                                        label = {
                                            Text(
                                                text = itemText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        icon = {
                                            Icon(
                                                painter = painterResource(
                                                    if (searchQuery.isNotBlank()) R.drawable.search
                                                    else R.drawable.history
                                                ),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        if (isSearching) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                // Artist chips row
                                if (searchArtistResults.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = stringResource(R.string.view_artist),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                        )
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(searchArtistResults) { artist ->
                                                FilterChip(
                                                    selected = selectedArtistName == artist.title,
                                                    onClick = {
                                                        if (selectedArtistName == artist.title) {
                                                            selectedArtistName = null
                                                            artistSongsList = emptyList()
                                                        } else {
                                                            loadArtistSongs(artist)
                                                        }
                                                    },
                                                    label = { Text(artist.title) },
                                                    leadingIcon = {
                                                        Icon(
                                                            painter = painterResource(R.drawable.artist),
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }

                                // Artist selected songs sub-list
                                if (selectedArtistName != null) {
                                    item {
                                        Text(
                                            text = "Songs by $selectedArtistName",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                        )
                                    }
                                    if (isLoadingArtistSongs) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    } else {
                                        items(artistSongsList, key = { "artist_${it.id}" }) { song ->
                                            val inPlaylist = checkInPlaylist(song.id, song.title, song.artists.map { it.name })
                                            val isChecked = selectedSongs.containsKey(song.id)
                                            val isSongPlaying = isPlaying && currentMediaMetadata?.id == song.id
                                            val isCurrentPreview = currentMediaMetadata?.id == song.id

                                            SongSelectRow(
                                                song = song,
                                                badgeText = null,
                                                inPlaylist = inPlaylist,
                                                checked = isChecked,
                                                isPlaying = isSongPlaying,
                                                isCurrentPreview = isCurrentPreview,
                                                currentPositionMs = if (isCurrentPreview) currentPositionMs else 0L,
                                                durationMs = if (isCurrentPreview && previewDurationMs > 0) previewDurationMs else song.duration * 1000L,
                                                onPreviewClick = {
                                                    if (currentMediaMetadata?.id == song.id) {
                                                        playerConnection?.togglePlayPause()
                                                    } else {
                                                        playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = song.id), song))
                                                    }
                                                },
                                                onSeekTo = { pos ->
                                                    playerConnection?.player?.seekTo(pos)
                                                },
                                                onCheckedChange = { checked ->
                                                    if (checked) selectedSongs[song.id] = song
                                                    else selectedSongs.remove(song.id)
                                                }
                                            )
                                        }
                                    }
                                    item {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            thickness = 0.5.dp
                                        )
                                    }
                                }

                                // General search songs
                                if (searchSongResults.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = stringResource(R.string.songs),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                        )
                                    }
                                    items(searchSongResults, key = { it.id }) { song ->
                                        val inPlaylist = checkInPlaylist(song.id, song.title, song.artists.map { it.name })
                                        val isChecked = selectedSongs.containsKey(song.id)
                                        val isSongPlaying = isPlaying && currentMediaMetadata?.id == song.id
                                        val isCurrentPreview = currentMediaMetadata?.id == song.id

                                        SongSelectRow(
                                            song = song,
                                            badgeText = null,
                                            inPlaylist = inPlaylist,
                                            checked = isChecked,
                                            isPlaying = isSongPlaying,
                                            isCurrentPreview = isCurrentPreview,
                                            currentPositionMs = if (isCurrentPreview) currentPositionMs else 0L,
                                            durationMs = if (isCurrentPreview && previewDurationMs > 0) previewDurationMs else song.duration * 1000L,
                                            onPreviewClick = {
                                                if (currentMediaMetadata?.id == song.id) {
                                                    playerConnection?.togglePlayPause()
                                                } else {
                                                    playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = song.id), song))
                                                }
                                            },
                                            onSeekTo = { pos ->
                                                playerConnection?.player?.seekTo(pos)
                                            },
                                            onCheckedChange = { checked ->
                                                if (checked) selectedSongs[song.id] = song
                                                else selectedSongs.remove(song.id)
                                            }
                                        )
                                    }
                                } else if (searchQuery.isBlank()) {
                                    if (isLoadingRecommendations) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                                    Spacer(Modifier.height(12.dp))
                                                    Text(
                                                        text = stringResource(R.string.finding_recommendations),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    } else if (recommendedSongs.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = stringResource(R.string.suggested_for_playlist),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                            )
                                        }
                                        items(recommendedSongs, key = { "search_rec_${it.metadata.id}" }) { rec ->
                                            val song = rec.metadata
                                            val inPlaylist = checkInPlaylist(song.id, song.title, song.artists.map { it.name })
                                            val isChecked = selectedSongs.containsKey(song.id)
                                            val isSongPlaying = isPlaying && currentMediaMetadata?.id == song.id
                                            val isCurrentPreview = currentMediaMetadata?.id == song.id

                                            RecommendationBigCard(
                                                song = song,
                                                sourceBadge = if (rec.source == "Playlist") "From Playlist" else rec.source,
                                                inPlaylist = inPlaylist,
                                                checked = isChecked,
                                                isPlaying = isSongPlaying,
                                                isCurrentPreview = isCurrentPreview,
                                                currentPositionMs = if (isCurrentPreview) currentPositionMs else 0L,
                                                durationMs = if (isCurrentPreview && previewDurationMs > 0) previewDurationMs else song.duration * 1000L,
                                                onPreviewClick = {
                                                    if (currentMediaMetadata?.id == song.id) {
                                                        playerConnection?.togglePlayPause()
                                                    } else {
                                                        playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = song.id), song))
                                                    }
                                                },
                                                onSeekTo = { pos ->
                                                    playerConnection?.player?.seekTo(pos)
                                                },
                                                onCheckedChange = { checked ->
                                                    if (checked) selectedSongs[song.id] = song
                                                    else selectedSongs.remove(song.id)
                                                }
                                            )
                                        }
                                    }
                                } else if (searchQuery.isNotBlank() && !isSearching && searchArtistResults.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = stringResource(R.string.no_results_found),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Recommended Tab Content
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Filter Chips and Refresh Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(end = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val filters = listOf("All", "Playlist", "Followed Artist")
                                items(filters) { filterName ->
                                    FilterChip(
                                        selected = recommendationFilter == filterName,
                                        onClick = { recommendationFilter = filterName },
                                        label = {
                                            Text(
                                                when (filterName) {
                                                    "All" -> "All"
                                                    "Playlist" -> "From Playlist"
                                                    "Followed Artist" -> "Followed Artists"
                                                    else -> filterName
                                                }
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }

                            IconButton(
                                onClick = { loadRecommendations(forceRefresh = true) },
                                enabled = !isLoadingRecommendations,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.sync),
                                    contentDescription = "Refresh recommendations",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (isLoadingRecommendations) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "Finding recommendations...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            val filteredRecs = remember(recommendedSongs, recommendationFilter) {
                                if (recommendationFilter == "All") recommendedSongs
                                else recommendedSongs.filter { it.source == recommendationFilter }
                            }

                            if (filteredRecs.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No recommendations available right now.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    items(filteredRecs, key = { "${it.source}_${it.metadata.id}" }) { rec ->
                                        val song = rec.metadata
                                        val inPlaylist = checkInPlaylist(song.id, song.title, song.artists.map { it.name })
                                        val isChecked = selectedSongs.containsKey(song.id)
                                        val isSongPlaying = isPlaying && currentMediaMetadata?.id == song.id
                                        val isCurrentPreview = currentMediaMetadata?.id == song.id

                                        RecommendationBigCard(
                                            song = song,
                                            sourceBadge = if (rec.source == "Playlist") "From Playlist" else rec.source,
                                            inPlaylist = inPlaylist,
                                            checked = isChecked,
                                            isPlaying = isSongPlaying,
                                            isCurrentPreview = isCurrentPreview,
                                            currentPositionMs = if (isCurrentPreview) currentPositionMs else 0L,
                                            durationMs = if (isCurrentPreview && previewDurationMs > 0) previewDurationMs else song.duration * 1000L,
                                            onPreviewClick = {
                                                if (currentMediaMetadata?.id == song.id) {
                                                    playerConnection?.togglePlayPause()
                                                } else {
                                                    playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = song.id), song))
                                                }
                                            },
                                            onSeekTo = { pos ->
                                                playerConnection?.player?.seekTo(pos)
                                            },
                                            onCheckedChange = { checked ->
                                                if (checked) selectedSongs[song.id] = song
                                                else selectedSongs.remove(song.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Sticky Bar
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }

                Button(
                    enabled = selectedSongs.isNotEmpty(),
                    onClick = {
                        val songsToAdd = selectedSongs.values.toList()
                        scope.launch(Dispatchers.IO) {
                            database.transaction {
                                songsToAdd.forEach { mediaMetadata ->
                                    insert(mediaMetadata)
                                }
                            }
                            val songIds = songsToAdd.map { it.id }
                            database.addSongToPlaylist(playlist, songIds)
                            songIds.forEach { syncUtils.clearSongTombstone(playlist.id, it) }
                            downloadUtil.autoDownloadIfPlaylistDownloaded(playlist.id, songIds)

                            scope.launch {
                                syncUtils.syncLocalPlaylistToSpotify(playlist.id, isAutoSync = true)
                            }

                            playlist.playlist.browseId?.let { browseId ->
                                songIds.forEach { songId ->
                                    YouTube.addToPlaylist(browseId, songId)
                                }
                            }
                        }
                        Toast.makeText(
                            context,
                            "Added ${songsToAdd.size} songs to ${playlist.playlist.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                        onDismiss()
                    }
                ) {
                    Text(
                        if (selectedSongs.isEmpty()) {
                            stringResource(R.string.add_to_an_playlist)
                        } else {
                            "${stringResource(R.string.add_to_an_playlist)} (${selectedSongs.size})"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewScrubber(
    currentPositionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPos by remember { mutableFloatStateOf(0f) }

    val effectiveDuration = durationMs.coerceAtLeast(1000L).toFloat()
    val displayPos = if (isScrubbing) scrubPos else currentPositionMs.toFloat().coerceIn(0f, effectiveDuration)

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Slider(
            value = displayPos.coerceIn(0f, effectiveDuration),
            valueRange = 0f..effectiveDuration,
            onValueChange = { value ->
                isScrubbing = true
                scrubPos = value
            },
            onValueChangeFinished = {
                isScrubbing = false
                onSeekTo(scrubPos.toLong())
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = makeTimeString(displayPos.toLong()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = makeTimeString(durationMs.coerceAtLeast(0L)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecommendationBigCard(
    song: MediaMetadata,
    sourceBadge: String,
    inPlaylist: Boolean,
    checked: Boolean,
    isPlaying: Boolean,
    isCurrentPreview: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onPreviewClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !inPlaylist) {
                onCheckedChange(!checked)
            },
        shape = RoundedCornerShape(16.dp),
        color = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (checked) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 16:9 Thumbnail with Overlay Play Button
                Box(
                    modifier = Modifier
                        .width(104.dp)
                        .height(58.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onPreviewClick() },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl?.resize(240, 135),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay play/pause button circle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = if (isCurrentPreview && isPlaying) 0.65f else 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(if (isCurrentPreview && isPlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = if (isCurrentPreview && isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Song Title & Artist info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = song.artists.joinToString { it.name }.ifEmpty { "Unknown Artist" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        ) {
                            Text(
                                text = sourceBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                        if (song.duration > 0) {
                            Text(
                                text = makeTimeString(song.duration * 1000L),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Checkbox to select
                Checkbox(
                    checked = checked || inPlaylist,
                    enabled = !inPlaylist,
                    onCheckedChange = { onCheckedChange(it) }
                )
            }

            // Expanding scrubber when preview is playing or active on this song
            if (isCurrentPreview) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(4.dp))
                PreviewScrubber(
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    onSeekTo = onSeekTo
                )
            }
        }
    }
}

@Composable
private fun SongSelectRow(
    song: MediaMetadata,
    badgeText: String?,
    inPlaylist: Boolean,
    checked: Boolean,
    isPlaying: Boolean,
    isCurrentPreview: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onPreviewClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !inPlaylist) {
                onCheckedChange(!checked)
            },
        color = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail with click-to-preview overlay
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onPreviewClick() },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl?.resize(120, 120),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isCurrentPreview && isPlaying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.pause),
                                contentDescription = "Pause",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.artists.joinToString { it.name }.ifEmpty { "Unknown Artist" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (song.duration > 0) {
                            Text(
                                text = " • ${makeTimeString(song.duration * 1000L)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (badgeText != null || inPlaylist) {
                        Spacer(Modifier.height(2.dp))
                        Row {
                            if (inPlaylist) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                ) {
                                    Text(
                                        text = "In playlist",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            } else if (badgeText != null) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Listen into song preview button
                IconButton(
                    onClick = onPreviewClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(if (isCurrentPreview && isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = if (isCurrentPreview && isPlaying) "Pause preview" else "Preview song",
                        tint = if (isCurrentPreview && isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Checkbox
                Checkbox(
                    checked = checked || inPlaylist,
                    enabled = !inPlaylist,
                    onCheckedChange = { onCheckedChange(it) }
                )
            }

            // Expanding scrubber when previewing
            if (isCurrentPreview) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(2.dp))
                PreviewScrubber(
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    onSeekTo = onSeekTo
                )
            }
        }
    }
}

