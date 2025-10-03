package com.music.vivi.ui.screens

import android.health.connect.datatypes.units.Velocity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.lerp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.AlbumThumbnailSize
import com.music.vivi.constants.AppBarHeight
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.AlbumWithSongs
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.LocalAlbumRadio
import com.music.vivi.ui.component.AlbumSongButtonItem

import com.music.vivi.ui.component.AutoResizeText
import com.music.vivi.ui.component.FontSizeRange
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.SongListItem
import com.music.vivi.ui.component.YouTubeGridItem
import com.music.vivi.ui.component.shimmer.ButtonPlaceholder
import com.music.vivi.ui.component.shimmer.ListItemPlaceHolder
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.component.shimmer.TextPlaceholder
import com.music.vivi.ui.menu.AlbumMenu
import com.music.vivi.ui.menu.SelectionSongMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.menu.YouTubeAlbumMenu
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.ItemWrapper
import com.music.vivi.ui.utils.fadingEdge
import com.music.vivi.ui.utils.resize
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.AlbumViewModel
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val density = LocalDensity.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()

    val playlistId by viewModel.playlistId.collectAsState()
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val otherVersions by viewModel.otherVersions.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val lazyListState = rememberLazyListState()

    val wrappedSongs = remember(albumWithSongs, hideExplicit) {
        val filteredSongs = if (hideExplicit) {
            albumWithSongs?.songs?.filter { !it.song.explicit } ?: emptyList()
        } else {
            albumWithSongs?.songs ?: emptyList()
        }
        filteredSongs.map { item -> ItemWrapper(item) }.toMutableStateList()
    }

    var selection by remember { mutableStateOf(false) }

    if (selection) {
        BackHandler {
            selection = false
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableStateOf(Download.STATE_STOPPED) }
    var downloadProgress by remember { mutableStateOf(0f) }

    // Use player's shuffle state for UI
    val isShuffleActive by remember(shuffleModeEnabled) {
        derivedStateOf { shuffleModeEnabled }
    }

    val isCurrentAlbumPlaying by remember(mediaMetadata, albumWithSongs) {
        derivedStateOf {
            // Check if any song from the current album is playing
            albumWithSongs?.songs?.any { it.id == mediaMetadata?.id } ?: false
        }
    }

    LaunchedEffect(albumWithSongs) {
        val songs = albumWithSongs?.songs?.map { it.id }
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            val completedSongs = songs.count { downloads[it]?.state == Download.STATE_COMPLETED }
            val totalSongs = songs.size
            downloadProgress = if (totalSongs > 0) completedSongs.toFloat() / totalSongs else 0f

            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.any {
                        downloads[it]?.state == Download.STATE_QUEUED ||
                                downloads[it]?.state == Download.STATE_DOWNLOADING
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    // Button shapes
    val mediumButtonShape = RoundedCornerShape(12.dp)
    val largeButtonShape = RoundedCornerShape(16.dp)

    // Collapsing header setup
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = 400.dp

    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

    val topBarHeight = remember { Animatable(maxTopBarHeightPx) }
    var collapseFraction by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(topBarHeight.value) {
        collapseFraction = 1f - ((topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)).coerceIn(0f, 1f)
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val isScrollingDown = delta < 0

                if (!isScrollingDown && (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0)) {
                    return Offset.Zero
                }

                val previousHeight = topBarHeight.value
                val newHeight = (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = newHeight - previousHeight

                if (consumed.roundToInt() != 0) {
                    coroutineScope.launch {
                        topBarHeight.snapTo(newHeight)
                    }
                }

                val canConsumeScroll = !(isScrollingDown && newHeight == minTopBarHeightPx)
                return if (canConsumeScroll) Offset(0f, consumed) else Offset.Zero
            }
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val shouldExpand = topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
            val canExpand = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0

            val targetValue = if (shouldExpand && canExpand) {
                maxTopBarHeightPx
            } else {
                minTopBarHeightPx
            }

            if (topBarHeight.value != targetValue) {
                coroutineScope.launch {
                    topBarHeight.animateTo(targetValue, spring(stiffness = Spring.StiffnessMedium))
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(
                top = currentTopBarHeightDp,
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
            ),
        ) {
            val albumWithSongs = albumWithSongs
            if (albumWithSongs != null && albumWithSongs.songs.isNotEmpty()) {
                // Songs List in Box Container
                if (!wrappedSongs.isNullOrEmpty()) {
                    item(key = "songs_box") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Songs (${wrappedSongs.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                            )

                            wrappedSongs.forEachIndexed { index, songWrapper ->
                                AlbumSongButtonItem(
                                    song = songWrapper.item,
                                    albumIndex = index + 1,
                                    isActive = songWrapper.item.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    isSelected = songWrapper.isSelected && selection,
                                    onMenuClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = songWrapper.item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                if (!selection) {
                                                    val isThisSongActive = songWrapper.item.id == mediaMetadata?.id

                                                    if (isThisSongActive) {
                                                        playerConnection.player.togglePlayPause()
                                                    } else {
                                                        playerConnection.service.getAutomix(playlistId)
                                                        playerConnection.playQueue(
                                                            LocalAlbumRadio(
                                                                albumWithSongs.copy(
                                                                    songs = if (isShuffleActive) {
                                                                        albumWithSongs.songs.shuffled()
                                                                    } else {
                                                                        albumWithSongs.songs
                                                                    }
                                                                ),
                                                                startIndex = if (isShuffleActive) 0 else index
                                                            ),
                                                        )
                                                    }
                                                } else {
                                                    songWrapper.isSelected = !songWrapper.isSelected
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                if (!selection) {
                                                    selection = true
                                                }
                                                wrappedSongs.forEach { it.isSelected = false }
                                                songWrapper.isSelected = true
                                            },
                                        )
                                )
                            }
                        }
                    }

                }

                // Other Versions Section
                if (otherVersions.isNotEmpty()) {
                    item {
                        NavigationTitle(
                            title = stringResource(R.string.other_versions),
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                        ) {
                            items(
                                items = otherVersions.distinctBy { it.id },
                                key = { it.id },
                            ) { item ->
                                YouTubeGridItem(
                                    item = item,
                                    isActive = mediaMetadata?.album?.id == item.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = coroutineScope,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = { navController.navigate("album/${item.id}") },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeAlbumMenu(
                                                        albumItem = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }
                        }
                    }
                }
            } else {
                // Loading shimmer
                item(key = "shimmer") {
                    ShimmerHost {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shimmer()
                                    .background(MaterialTheme.colorScheme.onSurface)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            TextPlaceholder(
                                height = 36.dp,
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .padding(bottom = 8.dp)
                            )
                            TextPlaceholder(
                                height = 24.dp,
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .padding(bottom = 4.dp)
                            )
                            TextPlaceholder(
                                height = 20.dp,
                                modifier = Modifier
                                    .fillMaxWidth(0.2f)
                                    .padding(bottom = 16.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(3) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .shimmer()
                                            .background(
                                                MaterialTheme.colorScheme.onSurface,
                                                RoundedCornerShape(24.dp)
                                            )
                                    )
                                    if (it < 2) Spacer(modifier = Modifier.width(8.dp))
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                ButtonPlaceholder(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(48.dp)
                                )
                            }
                        }

                        repeat(6) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            }
        }

        // Collapsing Header
        CollapsingAlbumHeader(
            album = albumWithSongs,
            songsCount = wrappedSongs?.size ?: 0,
            collapseFraction = collapseFraction,
            headerHeight = currentTopBarHeightDp,
            selection = selection,
            isShuffleActive = isShuffleActive,
            onShuffleToggle = {
                playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
            },
            isCurrentAlbumPlaying = isCurrentAlbumPlaying,
            isPlaying = isPlaying,
            downloadState = downloadState,
            downloadProgress = downloadProgress,
            onBackPressed = {
                if (selection) {
                    selection = false
                } else {
                    navController.navigateUp()
                }
            },
            onMoreClick = {
                albumWithSongs?.let { album ->
                    menuState.show {
                        AlbumMenu(
                            originalAlbum = Album(album.album, album.artists),
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )
                    }
                }
            },
            onLikeClick = {
                albumWithSongs?.let { album ->
                    database.query {
                        update(album.album.toggleLike())
                    }
                }
            },
            onDownloadClick = {
                albumWithSongs?.let { album ->
                    when (downloadState) {
                        Download.STATE_COMPLETED, Download.STATE_DOWNLOADING -> {
                            album.songs.forEach { song ->
                                DownloadService.sendRemoveDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    song.id,
                                    false,
                                )
                            }
                        }
                        else -> {
                            album.songs.forEach { song ->
                                val downloadRequest = DownloadRequest
                                    .Builder(song.id, song.id.toUri())
                                    .setCustomCacheKey(song.id)
                                    .setData(song.song.title.toByteArray())
                                    .build()
                                DownloadService.sendAddDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    downloadRequest,
                                    false,
                                )
                            }
                        }
                    }
                }
            },
            onPlayClick = {
                albumWithSongs?.let { album ->
                    // Check if any song from this album is currently playing
                    val isAnySongFromAlbumPlaying = album.songs.any { it.id == mediaMetadata?.id }

                    if (isAnySongFromAlbumPlaying) {
                        // If a song from this album is playing, just toggle play/pause
                        playerConnection.player.togglePlayPause()
                    } else {
                        // If no song from this album is playing, start playing the album
                        playerConnection.service.getAutomix(playlistId)
                        playerConnection.playQueue(
                            LocalAlbumRadio(
                                album.copy(
                                    songs = if (isShuffleActive) {
                                        album.songs.shuffled()
                                    } else {
                                        album.songs
                                    }
                                )
                            ),
                        )
                    }
                }
            },
            navController = navController,
            mediumButtonShape = mediumButtonShape,
            largeButtonShape = largeButtonShape
        )
    }
}

@Composable
private fun CollapsingAlbumHeader(
    album: AlbumWithSongs?,
    songsCount: Int,
    collapseFraction: Float,
    headerHeight: Dp,
    selection: Boolean,
    isShuffleActive: Boolean,
    onShuffleToggle: () -> Unit,
    isCurrentAlbumPlaying: Boolean,
    isPlaying: Boolean,
    downloadState: Int,
    downloadProgress: Float,
    onBackPressed: () -> Unit,
    onMoreClick: () -> Unit,
    onLikeClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onPlayClick: () -> Unit,
    navController: NavController,
    mediumButtonShape: Shape,
    largeButtonShape: Shape
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundAlpha = collapseFraction
    val headerContentAlpha = 1f - (collapseFraction * 2).coerceAtMost(1f)
    val controlsAlpha = 1f - (collapseFraction * 1.5f).coerceAtMost(1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .background(surfaceColor.copy(alpha = backgroundAlpha))
    ) {
        if (album != null) {
            // Header Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = headerContentAlpha }
            ) {
                AsyncImage(
                    model = album.album.thumbnailUrl?.resize(1200, 1200),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.5f to Color.Transparent,
                                    0.85f to MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                    1f to MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
            }

            // Top bar content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Back button at top
                FilledIconButton(
                    modifier = Modifier
                        .padding(start = 12.dp, top = 4.dp),
                    onClick = onBackPressed,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Icon(
                        painter = painterResource(if (selection) R.drawable.close else R.drawable.arrow_back),
                        contentDescription = null
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Title and info section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .graphicsLayer { alpha = headerContentAlpha }
                ) {
                    Text(
                        text = album.album.title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 32.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Text(
                        buildAnnotatedString {
                            withStyle(
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Normal,
                                ).toSpanStyle()
                            ) {
                                album.artists.fastForEachIndexed { index, artist ->
                                    val link = LinkAnnotation.Clickable(artist.id) {
                                        navController.navigate("artist/${artist.id}")
                                    }
                                    withLink(link) {
                                        append(artist.name)
                                    }
                                    if (index != album.artists.lastIndex) {
                                        append(", ")
                                    }
                                }
                            }
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )

                    if (album.album.year != null) {
                        Text(
                            text = "${album.album.year} • $songsCount songs",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Control buttons at bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .graphicsLayer { alpha = controlsAlpha },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledIconButton(
                            onClick = onMoreClick,
                            modifier = Modifier.size(48.dp),
                            shape = mediumButtonShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }

                        FilledIconButton(
                            onClick = onLikeClick,
                            modifier = Modifier.size(48.dp),
                            shape = mediumButtonShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (album.album.bookmarkedAt != null) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                contentColor = if (album.album.bookmarkedAt != null) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (album.album.bookmarkedAt != null) {
                                        R.drawable.favorite
                                    } else {
                                        R.drawable.favorite_border
                                    }
                                ),
                                contentDescription = null,
                            )
                        }

                        FilledIconButton(
                            onClick = onDownloadClick,
                            modifier = Modifier.size(48.dp),
                            shape = mediumButtonShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            if (downloadState == Download.STATE_DOWNLOADING) {
                                CircularProgressIndicator(
                                    progress = { downloadProgress },
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Icon(
                                    painter = painterResource(
                                        when (downloadState) {
                                            Download.STATE_COMPLETED -> R.drawable.offline
                                            else -> R.drawable.download
                                        }
                                    ),
                                    contentDescription = null,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = onShuffleToggle,
                            modifier = Modifier.size(48.dp),
                            shape = largeButtonShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (isShuffleActive) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                contentColor = if (isShuffleActive) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        FilledTonalButton(
                            onClick = onPlayClick,
                            shape = largeButtonShape,
                            modifier = Modifier
                                .height(48.dp)
                                .widthIn(min = 120.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isCurrentAlbumPlaying && isPlaying) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                contentColor = if (isCurrentAlbumPlaying && isPlaying) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isCurrentAlbumPlaying && isPlaying) R.drawable.pause else R.drawable.play
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    if (isCurrentAlbumPlaying && isPlaying) R.string.pause else R.string.play
                                ),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}