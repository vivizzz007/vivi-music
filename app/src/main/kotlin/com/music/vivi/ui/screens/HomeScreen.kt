package com.music.vivi.ui.screens

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YTItem
import com.music.innertube.utils.parseCookieString
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.GridThumbnailHeight
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.constants.ListThumbnailSize
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.Artist
import com.music.vivi.db.entities.LocalItem
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.LocalAlbumRadio
import com.music.vivi.playback.queues.YouTubeAlbumRadio
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.AlbumGridItem
import com.music.vivi.ui.component.ArtistGridItem
import com.music.vivi.ui.component.ChipsRow
import com.music.vivi.ui.component.HideOnScrollFAB
import com.music.vivi.ui.component.LocalBottomSheetPageState
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.SongGridItem
import com.music.vivi.ui.component.SongListItem
import com.music.vivi.ui.component.YouTubeGridItem
import com.music.vivi.ui.component.shimmer.GridItemPlaceHolder
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.component.shimmer.TextPlaceholder
import com.music.vivi.ui.menu.AlbumMenu
import com.music.vivi.ui.menu.ArtistMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.menu.YouTubeAlbumMenu
import com.music.vivi.ui.menu.YouTubeArtistMenu
import com.music.vivi.ui.menu.YouTubePlaylistMenu
import com.music.vivi.ui.menu.YouTubeSongMenu
import com.music.vivi.ui.utils.SnapLayoutInfoProvider
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable

fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

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
            viewModel.toggleChip(selectedChip)
        }
    }

    val localGridItem: @Composable (LocalItem) -> Unit = {
        when (it) {
            is Song -> SongGridItem(
                song = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (it.id == mediaMetadata?.id) {
                                playerConnection.player.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    YouTubeQueue.radio(it.toMediaMetadata()),
                                )
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            menuState.show {
                                SongMenu(
                                    originalSong = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
                isActive = it.id == mediaMetadata?.id,
                isPlaying = isPlaying,
            )

            is Album -> AlbumGridItem(
                album = it,
                isActive = it.id == mediaMetadata?.album?.id,
                isPlaying = isPlaying,
                coroutineScope = scope,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            navController.navigate("album/${it.id}")
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                AlbumMenu(
                                    originalAlbum = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
            )

            is Artist -> ArtistGridItem(
                artist = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            navController.navigate("artist/${it.id}")
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            menuState.show {
                                ArtistMenu(
                                    originalArtist = it,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
            )

            is Playlist -> {}
        }
    }

    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        YouTubeGridItem(
            item = item,
            isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
            isPlaying = isPlaying,
            coroutineScope = scope,
            thumbnailRatio = 1f,
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        when (item) {
                            is SongItem -> playerConnection.playQueue(
                                YouTubeQueue(
                                    item.endpoint ?: WatchEndpoint(
                                        videoId = item.id
                                    ), item.toMediaMetadata()
                                )
                            )

                            is AlbumItem -> navController.navigate("album/${item.id}")
                            is ArtistItem -> navController.navigate("artist/${item.id}")
                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            when (item) {
                                is SongItem -> YouTubeSongMenu(
                                    song = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is AlbumItem -> YouTubeAlbumMenu(
                                    albumItem = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is ArtistItem -> YouTubeArtistMenu(
                                    artist = item,
                                    onDismiss = menuState::dismiss
                                )

                                is PlaylistItem -> YouTubePlaylistMenu(
                                    playlist = item,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    }
                )
        )
    }

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
            SnapLayoutInfoProvider(
                lazyGridState = quickPicksLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }
        val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
            SnapLayoutInfoProvider(
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
                ChipsRow(
                    chips = homePage?.chips?.mapNotNull { it to it.title } ?: emptyList(),
                    currentValue = selectedChip,
                    onValueUpdate = {
                        viewModel.toggleChip(it)
                    }
                )
            }

            // Find the New Releases section early for conditional placement
            val newReleasesSection = homePage?.sections?.firstOrNull {
                it.title.contains("release", ignoreCase = true) ||
                        it.title.contains("new", ignoreCase = true)
            }

            // QUICK PICKS SECTION - Only show if available
            quickPicks?.takeIf { it.isNotEmpty() }?.let { quickPicks ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.quick_picks),
                        modifier = Modifier.animateItem()
                    )
                }

                item {
                    LazyHorizontalGrid(
                        state = quickPicksLazyGridState,
                        rows = GridCells.Fixed(4),
                        flingBehavior = rememberSnapFlingBehavior(quickPicksSnapLayoutInfoProvider),
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ListItemHeight * 4)
                            .animateItem()
                    ) {
                        items(
                            items = quickPicks,
                            key = { it.id }
                        ) { originalSong ->
                            val song by database.song(originalSong.id)
                                .collectAsState(initial = originalSong)

                            SongListItem(
                                song = song!!,
                                showInLibraryIcon = true,
                                isActive = song!!.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                isSwipeable = false,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song!!,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .width(horizontalLazyGridItemWidth)
                                    .combinedClickable(
                                        onClick = {
                                            if (song!!.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(YouTubeQueue.radio(song!!.toMediaMetadata()))
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song!!,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    )
                            )
                        }
                    }
                }

                // NEW RELEASES SECTION - Placed right after Quick Picks if Quick Picks is shown
                newReleasesSection?.let { section ->
                    item {
                        NavigationTitle(
                            title = section.title,
                            label = section.label,
                            thumbnail = section.thumbnail?.let { thumbnailUrl ->
                                {
                                    val shape =
                                        if (section.endpoint?.isArtistEndpoint == true) CircleShape else RoundedCornerShape(
                                            ThumbnailCornerRadius
                                        )
                                    AsyncImage(
                                        model = thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(ListThumbnailSize)
                                            .clip(shape)
                                    )
                                }
                            },
                            onClick = section.endpoint?.browseId?.let { browseId ->
                                if (homePage != null) {
                                    {
                                        when (browseId) {
                                            "FEmusic_moods_and_genres" -> navController.navigate("mood_and_genres")
                                            "FEmusic_charts" -> navController.navigate("charts_screen")
                                            else -> navController.navigate("browse/$browseId")
                                        }
                                    }
                                } else {
                                    null
                                }
                            },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            items(section.items) { item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }
            }

            // If Quick Picks is empty but we have New Releases, show them at the top
            if (quickPicks.isNullOrEmpty() && newReleasesSection != null) {
                item {
                    NavigationTitle(
                        title = newReleasesSection.title,
                        label = newReleasesSection.label,
                        thumbnail = newReleasesSection.thumbnail?.let { thumbnailUrl ->
                            {
                                val shape =
                                    if (newReleasesSection.endpoint?.isArtistEndpoint == true) CircleShape else RoundedCornerShape(
                                        ThumbnailCornerRadius
                                    )
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(ListThumbnailSize)
                                        .clip(shape)
                                )
                            }
                        },
                        onClick = newReleasesSection.endpoint?.browseId?.let { browseId ->
                            if (homePage != null) {
                                {
                                    when (browseId) {
                                        "FEmusic_moods_and_genres" -> navController.navigate("mood_and_genres")
                                        "FEmusic_charts" -> navController.navigate("charts_screen")
                                        else -> navController.navigate("browse/$browseId")
                                    }
                                }
                            } else {
                                null
                            }
                        },
                        modifier = Modifier.animateItem()
                    )
                }

                item {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier.animateItem()
                    ) {
                        items(newReleasesSection.items) { item ->
                            ytGridItem(item)
                        }
                    }
                }
            }

            keepListening?.takeIf { it.isNotEmpty() }?.let { keepListening ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.keep_listening),
                        modifier = Modifier.animateItem()
                    )
                }

                item {
                    val rows = if (keepListening.size > 6) 2 else 1
                    LazyHorizontalGrid(
                        state = rememberLazyGridState(),
                        rows = GridCells.Fixed(rows),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((GridThumbnailHeight + with(LocalDensity.current) {
                                MaterialTheme.typography.bodyLarge.lineHeight.toDp() * 2 +
                                        MaterialTheme.typography.bodyMedium.lineHeight.toDp() * 2
                            }) * rows)
                            .animateItem()
                    ) {
                        items(keepListening) {
                            localGridItem(it)
                        }
                    }
                }
            }

            accountPlaylists?.takeIf { it.isNotEmpty() }?.let { accountPlaylists ->
                item {
                    NavigationTitle(
                        label = stringResource(R.string.your_youtube_playlists),
                        title = accountName,
                        thumbnail = {
                            if (url != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(url)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .diskCacheKey(url)
                                        .crossfade(true)
                                        .build(),
                                    placeholder = painterResource(id = R.drawable.person),
                                    error = painterResource(id = R.drawable.person),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(ListThumbnailSize)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.person),
                                    contentDescription = null,
                                    modifier = Modifier.size(ListThumbnailSize)
                                )
                            }
                        },
                        onClick = {
                            navController.navigate("account")
                        },
                        modifier = Modifier.animateItem()
                    )
                }

                item {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier.animateItem()
                    ) {
                        items(
                            items = accountPlaylists,
                            key = { it.id },
                        ) { item ->
                            ytGridItem(item)
                        }
                    }
                }
            }

            forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { forgottenFavorites ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.forgotten_favorites),
                        modifier = Modifier.animateItem()
                    )
                }

                item {
                    val rows = min(4, forgottenFavorites.size)
                    LazyHorizontalGrid(
                        state = forgottenFavoritesLazyGridState,
                        rows = GridCells.Fixed(rows),
                        flingBehavior = rememberSnapFlingBehavior(
                            forgottenFavoritesSnapLayoutInfoProvider
                        ),
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ListItemHeight * rows)
                            .animateItem()
                    ) {
                        items(
                            items = forgottenFavorites,
                            key = { it.id }
                        ) { originalSong ->
                            val song by database.song(originalSong.id)
                                .collectAsState(initial = originalSong)

                            SongListItem(
                                song = song!!,
                                showInLibraryIcon = true,
                                isActive = song!!.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                isSwipeable = false,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song!!,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .width(horizontalLazyGridItemWidth)
                                    .combinedClickable(
                                        onClick = {
                                            if (song!!.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(YouTubeQueue.radio(song!!.toMediaMetadata()))
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song!!,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    )
                            )
                        }
                    }
                }
            }

            similarRecommendations?.forEach {
                item {
                    NavigationTitle(
                        label = stringResource(R.string.similar_to),
                        title = it.title.title,
                        thumbnail = it.title.thumbnailUrl?.let { thumbnailUrl ->
                            {
                                val shape =
                                    if (it.title is Artist) CircleShape else RoundedCornerShape(
                                        ThumbnailCornerRadius
                                    )
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(ListThumbnailSize)
                                        .clip(shape)
                                )
                            }
                        },
                        onClick = {
                            when (it.title) {
                                is Song -> navController.navigate("album/${it.title.album!!.id}")
                                is Album -> navController.navigate("album/${it.title.id}")
                                is Artist -> navController.navigate("artist/${it.title.id}")
                                is Playlist -> {}
                            }
                        },
                        modifier = Modifier.animateItem()
                    )
                }

                item {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier.animateItem()
                    ) {
                        items(it.items) { item ->
                            ytGridItem(item)
                        }
                    }
                }
            }

            // REMAINING HOME PAGE SECTIONS (excluding the new releases section that was already shown)
            homePage?.sections?.filterNot {
                it.title.contains("release", ignoreCase = true) ||
                        it.title.contains("new", ignoreCase = true)
            }?.forEach {
                item {
                    NavigationTitle(
                        title = it.title,
                        label = it.label,
                        thumbnail = it.thumbnail?.let { thumbnailUrl ->
                            {
                                val shape =
                                    if (it.endpoint?.isArtistEndpoint == true) CircleShape else RoundedCornerShape(
                                        ThumbnailCornerRadius
                                    )
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(ListThumbnailSize)
                                        .clip(shape)
                                )
                            }
                        },
                        onClick = it.endpoint?.browseId?.let { browseId ->
                            if (homePage != null) {
                                {
                                    when (browseId) {
                                        "FEmusic_moods_and_genres" -> navController.navigate("mood_and_genres")
                                        "FEmusic_charts" -> navController.navigate("charts_screen")
                                        else -> navController.navigate("browse/$browseId")
                                    }
                                }
                            } else {
                                null
                            }
                        },
                        modifier = Modifier.animateItem()
                    )
                }

                item {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier.animateItem()
                    ) {
                        items(it.items) { item ->
                            ytGridItem(item)
                        }
                    }
                }
            }

            if (isLoading || homePage?.continuation != null && homePage?.sections?.isNotEmpty() == true) {
                item {
                    ShimmerHost(
                        modifier = Modifier.animateItem()
                    ) {
                        TextPlaceholder(
                            height = 36.dp,
                            modifier = Modifier
                                .padding(12.dp)
                                .width(250.dp),
                        )
                        LazyRow {
                            items(4) {
                                GridItemPlaceHolder()
                            }
                        }
                    }
                }
            }

            explorePage?.moodAndGenres?.let { moodAndGenres ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.mood_and_genres),
                        onClick = {
                            navController.navigate("mood_and_genres")
                        },
                        modifier = Modifier.animateItem()
                    )
                }
                item {
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(4),
                        contentPadding = PaddingValues(6.dp),
                        modifier = Modifier
                            .height((MoodAndGenresButtonHeight + 12.dp) * 4 + 12.dp)
                            .animateItem()
                    ) {
                        items(moodAndGenres) {
                            MoodAndGenresButton(
                                title = it.title,
                                onClick = {
                                    navController.navigate("youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}")
                                },
                                modifier = Modifier
                                    .padding(6.dp)
                                    .width(180.dp)
                            )
                        }
                    }
                }
            }

            if (isMoodAndGenresLoading) {
                item {
                    ShimmerHost(
                        modifier = Modifier.animateItem()
                    ) {
                        TextPlaceholder(
                            height = 36.dp,
                            modifier = Modifier
                                .padding(vertical = 12.dp, horizontal = 12.dp)
                                .width(250.dp),
                        )

                        repeat(4) {
                            Row {
                                repeat(2) {
                                    TextPlaceholder(
                                        height = MoodAndGenresButtonHeight,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp)
                                            .width(200.dp)
                                    )
                                }
                            }
                        }
                    }
                }
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

        Indicator(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}