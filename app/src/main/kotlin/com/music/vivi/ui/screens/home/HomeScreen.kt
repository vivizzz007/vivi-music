package com.music.vivi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.utils.parseCookieString
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.Artist
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.Song
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.LocalAlbumRadio
import com.music.vivi.playback.queues.YouTubeAlbumRadio
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.ui.component.HideOnScrollFAB
import com.music.vivi.ui.component.LocalBottomSheetPageState
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.home.*
import com.music.vivi.ui.menu.YouTubeSongMenu
import com.music.vivi.ui.utils.GridSnapLayoutInfoProvider
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.YouTube
import com.music.innertube.utils.completed
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.db.entities.PlaylistSongMap
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.update.networkmoniter.NetworkConnectivityObserver
import com.music.vivi.utils.ImmutableList
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import kotlinx.coroutines.flow.map

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
/**
 * The main Dashboard/Home screen of the app.
 *
 * Displays:
 * - **Quick Picks**: Recently played or recommended songs.
 * - **Listen History**: Recently played songs/videos.
 * - **Suggested Albums/Artists**: Recommendations based on listening habits.
 *
 * **Performance Note**:
 * Uses strictly typed [lazy] lists (LazyColumn/LazyRow) with [contentType] and [key] parameters
 * to ensure efficient recycling and minimizing recompositions during scrolling.
 */
@Composable
internal fun HomeScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

    // Network connectivity monitoring
    val context = LocalContext.current
    val networkObserver = remember { NetworkConnectivityObserver(context) }

    // This will only trigger when network status ACTUALLY changes
    LaunchedEffect(Unit) {
        var previousStatus: NetworkConnectivityObserver.NetworkStatus? = null

        networkObserver.observe().collect { currentStatus ->
            // Only refresh if transitioning from Lost/Unavailable to Available
            if (currentStatus == NetworkConnectivityObserver.NetworkStatus.Available &&
                (
                    previousStatus == NetworkConnectivityObserver.NetworkStatus.Lost ||
                        previousStatus == NetworkConnectivityObserver.NetworkStatus.Unavailable
                    )
            ) {
                viewModel.refresh()
            }
            previousStatus = currentStatus
        }
    }

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()

    val allLocalItems by viewModel.allLocalItems.collectAsState()
    val allYtItems by viewModel.allYtItems.collectAsState()
    val selectedChip by viewModel.selectedChip.collectAsState()

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val isMoodAndGenresLoading = isLoading && explorePage?.moodAndGenres == null
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val url = if (isLoggedIn) accountImageUrl else null

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    // Modern Playlist Card Section (Targeting ONLY "Mixed" for you sections)
    val availableSections = remember(homePage) {
        homePage?.sections?.filter { section ->
            section.title?.contains("Mixed", ignoreCase = true) == true
        }?.filter { it.items.any { item -> item is PlaylistItem } }
            ?.distinctBy { section ->
                section.items.filterIsInstance<PlaylistItem>().firstOrNull()?.id
            }
            .orEmpty()
    }

    // Persistent cache for carousel tracks to avoid re-fetching on swipe
    val carouselTracksCache = remember { mutableMapOf<String, List<SongItem>>() }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { lazylistState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val len = lazylistState.layoutInfo.totalItemsCount
                if (lastVisibleIndex != null && lastVisibleIndex >= len - 3) {
                    viewModel.loadMoreYouTubeItems(homePage?.continuation)
                }
            }
    }

    if (selectedChip != null) {
        BackHandler {
            // if a chip is selected, go back to the normal homepage first
            viewModel.toggleChip(selectedChip)
        }
    }

    // Grid items logic moved to HomeGridHelpers.kt

    LaunchedEffect(quickPicks) {
        quickPicksLazyGridState.scrollToItem(0)
    }

    LaunchedEffect(forgottenFavorites) {
        forgottenFavoritesLazyGridState.scrollToItem(0)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh
            ),
        contentAlignment = Alignment.TopStart
    ) {
        val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
        val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
        val quickPicksSnapLayoutInfoProvider = remember(quickPicksLazyGridState) {
            GridSnapLayoutInfoProvider(
                lazyGridState = quickPicksLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }
        val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
            GridSnapLayoutInfoProvider(
                lazyGridState = forgottenFavoritesLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }

        LazyColumn(
            state = lazylistState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            item {
                homePage?.chips?.let { chips ->
                    HomeChipsRow(
                        chips = chips,
                        selectedChip = selectedChip,
                        onChipClick = { viewModel.toggleChip(it) }
                    )
                }
            }

            quickPicks?.takeIf { it.isNotEmpty() }?.let { quickPicks ->
                item(key = "quick_picks") {
                    HomeQuickPicks(
                        quickPicks = ImmutableList(quickPicks),
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        maxWidth = maxWidth,
                        navController = navController,
                        modifier = Modifier
                    )
                }
            }

            keepListening?.takeIf { it.isNotEmpty() }?.let { keepListening ->
                item(key = "keep_listening") {
                    HomeKeepListening(
                        keepListening = ImmutableList(keepListening),
                        isPlaying = isPlaying,
                        activeId = mediaMetadata?.id,
                        activeAlbumId = mediaMetadata?.album?.id,
                        navController = navController,
                        scope = scope,
                        modifier = Modifier
                    )
                }
            }

            accountPlaylists?.takeIf { it.isNotEmpty() }?.let { accountPlaylists ->
                item(key = "account_playlists") {
                    HomeAccountPlaylists(
                        accountPlaylists = ImmutableList(accountPlaylists),
                        accountName = accountName,
                        accountImageUrl = url,
                        activeId = mediaMetadata?.id,
                        activeAlbumId = mediaMetadata?.album?.id,
                        isPlaying = isPlaying,
                        navController = navController,
                        scope = scope,
                        modifier = Modifier
                    )
                }
            }

            forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { forgottenFavorites ->
                item(key = "forgotten_favorites") {
                    HomeForgottenFavorites(
                        forgottenFavorites = ImmutableList(forgottenFavorites),
                        mediaMetadataId = mediaMetadata?.id,
                        isPlaying = isPlaying,
                        maxWidth = maxWidth,
                        navController = navController,
                        modifier = Modifier
                    )
                }
            }

//the mordern card for the playlist showing
            similarRecommendations?.let {
                homeSimilarRecommendations(
                    similarRecommendations = ImmutableList(it),
                    activeId = mediaMetadata?.id,
                    activeAlbumId = mediaMetadata?.album?.id,
                    isPlaying = isPlaying,
                    navController = navController,
                    scope = scope
                )
            }

            if (availableSections.isNotEmpty()) {
                item(key = "modern_playlist_carousel") {
                    val pagerState = rememberPagerState(pageCount = { availableSections.size })

                    Column {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            pageSpacing = 16.dp,
                            verticalAlignment = Alignment.Top
                        ) { page ->
                            val section = availableSections[page]
                            val playlist = section.items.filterIsInstance<PlaylistItem>().firstOrNull() ?: return@HorizontalPager

                            // Use cached songs if available, otherwise use what's in the section
                            val sectionSongs = section.items.filterIsInstance<SongItem>().take(3)
                            val cachedSongs = carouselTracksCache[playlist.id] ?: emptyList()
                            val songs = if (sectionSongs.isNotEmpty()) sectionSongs else cachedSongs

                            val dbPlaylist by database.playlistByBrowseId(playlist.id).collectAsState(initial = null)
                            val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
                            val isPaused by playerConnection.isPlaying.map { !it }.collectAsState(initial = true)

                            ModernPlaylistCard(
                                playlist = playlist,
                                tracks = songs,
                                 isPlaying = isPlaying && mediaMetadata?.album?.id == playlist.id,
                                activeMediaId = mediaMetadata?.id,
                                isPaused = isPaused,
                                 onPlayClick = {
                                     val isPlaylistPlaying = isPlaying && mediaMetadata?.album?.id == playlist.id
                                     if (isPlaylistPlaying) {
                                         playerConnection.player.togglePlayPause()
                                     } else {
                                         if (songs.isNotEmpty()) {
                                             playerConnection.playQueue(
                                                 ListQueue(
                                                     title = playlist.title,
                                                     items = songs.map { it.toMediaItem() }
                                                 )
                                             )
                                         } else {
                                             playlist.playEndpoint?.let {
                                                 playerConnection.playQueue(YouTubeQueue(it))
                                             }
                                         }
                                     }
                                 },
                                 onRadioClick = {
                                     val endpoint = playlist.radioEndpoint
                                         ?: playlist.shuffleEndpoint
                                         ?: playlist.playEndpoint

                                     if (endpoint != null) {
                                         playerConnection.playQueue(YouTubeQueue(endpoint))
                                     } else if (songs.isNotEmpty()) {
                                         playerConnection.playQueue(YouTubeQueue.radio(songs.random().toMediaMetadata()))
                                     } else {
                                         playerConnection.playQueue(YouTubeQueue(WatchEndpoint(playlistId = playlist.id)))
                                     }
                                },
                                onTrackClick = { track ->
                                    playerConnection.playQueue(
                                        YouTubeQueue(
                                            track.endpoint ?: WatchEndpoint(videoId = track.id),
                                            track.toMediaMetadata()
                                        )
                                    )
                                },
                                onTrackMenuClick = { track ->
                                    menuState.show {
                                        YouTubeSongMenu(
                                            song = track,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                },
                                onHeaderClick = {
                                    navController.navigate("online_playlist/${playlist.id}")
                                },
                                onTracksFetched = { fetched ->
                                    carouselTracksCache[playlist.id] = fetched
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            homePage?.sections?.let { sections ->
                homeInnertubeSections(
                    sections = ImmutableList(sections),
                    activeId = mediaMetadata?.id,
                    activeAlbumId = mediaMetadata?.album?.id,
                    isPlaying = isPlaying,
                    navController = navController,
                    scope = scope
                )
            }

            homeLoadingShimmer(
                isLoading = isLoading,
                homePage = homePage
            )

            if (selectedChip == null) {
                homeMoodAndGenres(
                    moodAndGenres = explorePage?.moodAndGenres?.let { ImmutableList(it) },
                    isLoading = isMoodAndGenresLoading,
                    navController = navController
                )
            }
        }

        HideOnScrollFAB(
            visible = allLocalItems.isNotEmpty() || allYtItems.isNotEmpty(),
            lazyListState = lazylistState,
            icon = R.drawable.shuffle,
            onClick = {
                val local = when {
                    allLocalItems.isNotEmpty() && allYtItems.isNotEmpty() -> Random.nextFloat() < 0.5
                    allLocalItems.isNotEmpty() -> true
                    else -> false
                }
                scope.launch(Dispatchers.Main) {
                    if (local) {
                        when (val luckyItem = allLocalItems.random()) {
                            is Song -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                            is Album -> {
                                val albumWithSongs = withContext(Dispatchers.IO) {
                                    database.albumWithSongs(luckyItem.id).first()
                                }
                                albumWithSongs?.let {
                                    playerConnection.playQueue(LocalAlbumRadio(it))
                                }
                            }
                            is Artist -> {}
                            is Playlist -> {}
                        }
                    } else {
                        when (val luckyItem = allYtItems.random()) {
                            is SongItem -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                            is AlbumItem -> playerConnection.playQueue(YouTubeAlbumRadio(luckyItem.playlistId))
                            is ArtistItem -> luckyItem.radioEndpoint?.let {
                                playerConnection.playQueue(YouTubeQueue(it))
                            }
                            is PlaylistItem -> luckyItem.playEndpoint?.let {
                                playerConnection.playQueue(YouTubeQueue(it))
                            }
                        }
                    }
                }
            }
        )

        val refreshProgress by animateFloatAsState(
            targetValue = if (isRefreshing) 1f else pullRefreshState.distanceFraction.coerceIn(0f, 1f),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessVeryLow
            ),
            label = "refresh_progress"
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues())
        ) {
            AnimatedVisibility(
                visible = isRefreshing || pullRefreshState.distanceFraction > 0,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                if (isRefreshing) {
                    ContainedLoadingIndicator(
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else {
                    ContainedLoadingIndicator(
                        progress = { refreshProgress },
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}
