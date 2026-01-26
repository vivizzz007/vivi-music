package com.music.vivi.ui.screens.playlist

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.db.entities.PlaylistSongMap
import com.music.vivi.extensions.metadata
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.DraggableScrollbar
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.media.youtube.YouTubeGridItem
import com.music.vivi.ui.component.media.youtube.YouTubeListItem
import com.music.vivi.ui.component.shimmer.ListItemPlaceHolder
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.menu.SelectionMediaMetadataMenu
import com.music.vivi.ui.menu.YouTubeAlbumMenu
import com.music.vivi.ui.menu.YouTubeArtistMenu
import com.music.vivi.ui.menu.YouTubePlaylistMenu
import com.music.vivi.ui.menu.YouTubeSongMenu
import com.music.vivi.ui.utils.ItemWrapper
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.OnlinePlaylistViewModel
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlin.collections.isNotEmpty

/**
 * Screen for displaying an Online (YouTube Music) playlist.
 * Fetches playlist details and songs from the network.
 * Allows playing, saving (to local DB), and downloading.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    playlistId: String? = null,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
    onBack: () -> Unit = { navController.navigateUp() },
) {
    if (playlistId != null) {
        LaunchedEffect(playlistId) {
            viewModel.setPlaylistId(playlistId)
        }
    }
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val relatedItems by viewModel.relatedItems.collectAsStateWithLifecycle()
    val playlist by viewModel.playlist.collectAsState()
    val songs: List<com.music.innertube.models.SongItem> by viewModel.playlistSongs.collectAsState()
    val dbPlaylist: com.music.vivi.db.entities.Playlist? by viewModel.dbPlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val error by viewModel.error.collectAsState()

    var selection by remember {
        mutableStateOf(false)
    }
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
// fixed issue explicit
    val filteredSongs =
        remember(songs, query, hideExplicit) {
            // Add hideExplicit here
            val searchFiltered = if (query.text.isEmpty()) {
                songs.mapIndexed { index, song -> index to song }
            } else {
                songs
                    .mapIndexed { index, song -> index to song }
                    .filter { (_, song) ->
                        song.title.contains(query.text, ignoreCase = true) ||
                            song.artists.fastAny {
                                it.name.contains(
                                    query.text,
                                    ignoreCase = true
                                )
                            }
                    }
            }

            // Apply explicit content filter
            if (hideExplicit) {
                searchFiltered.filter { (_, song) -> !song.explicit }
            } else {
                searchFiltered
            }
        }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (selection) {
        BackHandler {
            selection = false
        }
    }

    val wrappedSongs = remember(filteredSongs) {
        filteredSongs.map { item -> ItemWrapper(item) }
    }.toMutableStateList()

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 400
        }
    }

    // Download functionality
    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.id]?.state == Download.STATE_QUEUED ||
                            downloads[it.id]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    // Calculate parallax effect for blurred background
    val parallaxOffset by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                -lazyListState.firstVisibleItemScrollOffset * 0.5f
            } else {
                -400f // Move out of view when scrolled past header
            }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (songs.size >= 5 && lastVisibleIndex != null && lastVisibleIndex >= songs.size - 5) {
                    viewModel.loadMoreSongs()
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Blurred background image that moves with parallax
        playlist?.thumbnail?.let { thumbnailUrl ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp) // Extra height for parallax movement
                    .offset(y = parallaxOffset.dp)
                    .blur(radius = 20.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .alpha(0.6f)
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient overlay to blend with the surface color
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.surface
                                ),
                                startY = 0f,
                                endY = 800f
                            )
                        )
                )
            }
        }

        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
                .asPaddingValues(),
            modifier = Modifier.fillMaxSize()
        ) {
            playlist.let { playlist ->
                if (isLoading) {
                    // Loading shimmer with new design
                    item(key = "loading_indicator") {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(bottom = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ContainedLoadingIndicator()
                        }
                    }
                } else if (playlist != null) {
                    if (!isSearching) {
                        item(key = "playlist_header") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Transparent),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(Modifier.height(50.dp)) // Space for top app bar

                                // Playlist Artwork - Large and centered
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 48.dp)
                                ) {
                                    AsyncImage(
                                        model = playlist.thumbnail,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(Modifier.height(32.dp))

                                // Playlist Title with Logo Icon
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.playlist_new_vivi),
                                        contentDescription = null,
                                        modifier = Modifier.size(30.dp), // 20dp icon size was changed to 30dp
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = playlist.title,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(Modifier.height(24.dp))

                                // Action Buttons Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Favorite Button
                                    if (playlist.id != "LM") {
                                        Surface(
                                            onClick = {
                                                if (dbPlaylist?.playlist == null) {
                                                    database.transaction {
                                                        val playlistEntity = PlaylistEntity(
                                                            name = playlist.title,
                                                            browseId = playlist.id,
                                                            thumbnailUrl = playlist.thumbnail,
                                                            isEditable = playlist.isEditable,
                                                            playEndpointParams = playlist.playEndpoint?.params,
                                                            shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                                            radioEndpointParams = playlist.radioEndpoint?.params
                                                        ).toggleLike()
                                                        insert(playlistEntity)
                                                        songs.map(SongItem::toMediaMetadata)
                                                            .onEach(::insert)
                                                            .mapIndexed { index, song ->
                                                                PlaylistSongMap(
                                                                    songId = song.id,
                                                                    playlistId = playlistEntity.id,
                                                                    position = index
                                                                )
                                                            }
                                                            .forEach(::insert)
                                                    }
                                                } else {
                                                    database.transaction {
                                                        val currentPlaylist = dbPlaylist!!.playlist
                                                        update(currentPlaylist, playlist)
                                                        update(currentPlaylist.toggleLike())
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(24.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 12.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    painter = painterResource(
                                                        if (dbPlaylist?.playlist?.bookmarkedAt != null) {
                                                            R.drawable.favorite
                                                        } else {
                                                            R.drawable.favorite_border
                                                        }
                                                    ),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) {
                                                        MaterialTheme.colorScheme.error
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = if (dbPlaylist?.playlist?.bookmarkedAt !=
                                                        null
                                                    ) {
                                                        stringResource(R.string.saved)
                                                    } else {
                                                        stringResource(R.string.save)
                                                    },
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    // Play Button
                                    Surface(
                                        onClick = {
                                            if (songs.isNotEmpty()) {
                                                if (isPlaying && mediaMetadata?.album?.id == playlist.id) {
                                                    playerConnection.player.pause()
                                                } else if (mediaMetadata?.album?.id == playlist.id) {
                                                    playerConnection.player.play()
                                                } else {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = playlist.title,
                                                            items = songs.map { it.toMediaItem() }
                                                        )
                                                    )
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painter = painterResource(
                                                    if (isPlaying && mediaMetadata?.album?.id == playlist.id) {
                                                        R.drawable.pause
                                                    } else {
                                                        R.drawable.play
                                                    }
                                                ),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = if (isPlaying && mediaMetadata?.album?.id == playlist.id) {
                                                    stringResource(R.string.pause)
                                                } else {
                                                    stringResource(R.string.play)
                                                },
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }

                                    // Share Button
                                    Surface(
                                        onClick = {
                                            val intent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    context.getString(
                                                        R.string.check_out_playlist_share,
                                                        playlist.title,
                                                        "https://music.youtube.com/playlist?list=${playlist.id}"
                                                    )
                                                )
                                            }
                                            context.startActivity(Intent.createChooser(intent, null))
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.share),
                                                contentDescription = stringResource(
                                                    R.string.share_playlist_content_desc
                                                ),
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(24.dp))

                                // Playlist Info
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    playlist.author?.let { artist ->
                                        Text(
                                            buildAnnotatedString {
                                                append(stringResource(R.string.by_text))
                                                if (artist.id != null) {
                                                    val link = LinkAnnotation.Clickable(artist.id!!) {
                                                        navController.navigate("artist/${artist.id!!}")
                                                    }
                                                    withLink(link) {
                                                        append(artist.name)
                                                    }
                                                } else {
                                                    append(artist.name)
                                                }
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 32.dp)
                                        )
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    // Playlist Description
                                    Text(
                                        text = buildString {
                                            append(playlist.title)
                                            append(" ")
                                            append(context.getString(R.string.is_a_playlist))
                                            playlist.author?.name?.let {
                                                append(" ")
                                                append(context.getString(R.string.by_text))
                                                append(it)
                                            }
                                            append(". ")
                                            append(
                                                playlist.songCountText
                                                    ?: context.getString(R.string.this_collection_text)
                                            )
                                            append(" ")
                                            append(context.getString(R.string.playlist_description_suffix))
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.padding(horizontal = 32.dp),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(Modifier.height(24.dp))

                                // Additional action buttons
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        ButtonGroupDefaults.ConnectedSpaceBetween
                                    )
                                ) {
                                    // Download Button
                                    ToggleButton(
                                        checked =
                                        downloadState == Download.STATE_COMPLETED ||
                                            downloadState == Download.STATE_DOWNLOADING,
                                        onCheckedChange = {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED, Download.STATE_DOWNLOADING -> {
                                                    songs.forEach { song ->
                                                        DownloadService.sendRemoveDownload(
                                                            context,
                                                            ExoDownloadService::class.java,
                                                            song.id,
                                                            false
                                                        )
                                                    }
                                                }
                                                else -> {
                                                    songs.forEach { song ->
                                                        val downloadRequest =
                                                            DownloadRequest
                                                                .Builder(song.id, song.id.toUri())
                                                                .setCustomCacheKey(song.id)
                                                                .setData(song.title.toByteArray())
                                                                .build()
                                                        DownloadService.sendAddDownload(
                                                            context,
                                                            ExoDownloadService::class.java,
                                                            downloadRequest,
                                                            false
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    ) {
                                        when (downloadState) {
                                            Download.STATE_COMPLETED -> {
                                                Icon(
                                                    painter = painterResource(R.drawable.offline),
                                                    contentDescription = stringResource(R.string.saved),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Download.STATE_DOWNLOADING -> {
                                                CircularProgressIndicator(
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(16.dp),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            else -> {
                                                Icon(
                                                    painter = painterResource(R.drawable.download),
                                                    contentDescription = stringResource(R.string.save),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                        Text(
                                            text = when (downloadState) {
                                                Download.STATE_COMPLETED -> stringResource(R.string.saved)
                                                Download.STATE_DOWNLOADING -> stringResource(R.string.saving)
                                                else -> stringResource(R.string.save)
                                            },
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }

                                    // Shuffle Button
                                    playlist.shuffleEndpoint?.let {
                                        ToggleButton(
                                            checked = false,
                                            onCheckedChange = {
                                                val shuffledSongs = songs.map { it.toMediaItem() }.shuffled()
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = playlist.title,
                                                        items = shuffledSongs
                                                    )
                                                )
                                            },
                                            modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                            shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.shuffle),
                                                contentDescription = stringResource(R.string.shuffle_content_desc),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                            Text(
                                                stringResource(R.string.shuffle_label),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }

                                    // More Options Button
                                    ToggleButton(
                                        checked = false,
                                        onCheckedChange = {
                                            menuState.show {
                                                YouTubePlaylistMenu(
                                                    playlist = playlist,
                                                    songs = songs,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                    selectAction = { selection = true },
                                                    canSelect = true
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = stringResource(R.string.more_options_content_desc),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                        Text(
                                            stringResource(R.string.more_label),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }

                                Spacer(Modifier.height(24.dp))
                            }
                        }

                        // Solid background for the rest of the content
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                Spacer(Modifier.height(1.dp))
                            }
                        }
                    }

                    if (songs.isEmpty() && !isLoading && error == null) {
                        // Show empty playlist message when playlist is loaded but has no songs
                        item(key = "empty_playlist") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.empty_playlist),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.empty_playlist_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Replace the songs_container item (lines 680-750) with this:

                    if (!wrappedSongs.isNullOrEmpty()) {
                        // Add a spacer for the rounded container top
                        item(key = "songs_list_start") {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        itemsIndexed(
                            items = wrappedSongs,
                            key = { _, songWrapper -> songWrapper.item.second.id }
                        ) { index, songWrapper ->
                            val isFirst = index == 0
                            val isLast = index == wrappedSongs.size - 1
                            val isActive = songWrapper.item.second.id == mediaMetadata?.id

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                val cornerRadius = remember { 24.dp }

                                val topShape = remember(cornerRadius) {
                                    AbsoluteSmoothCornerShape(
                                        cornerRadiusTR = cornerRadius,
                                        smoothnessAsPercentBR = 0,
                                        cornerRadiusBR = 0.dp,
                                        smoothnessAsPercentTL = 60,
                                        cornerRadiusTL = cornerRadius,
                                        smoothnessAsPercentBL = 0,
                                        cornerRadiusBL = 0.dp,
                                        smoothnessAsPercentTR = 60
                                    )
                                }
                                val middleShape = remember { RectangleShape }
                                val bottomShape = remember(cornerRadius) {
                                    AbsoluteSmoothCornerShape(
                                        cornerRadiusTR = 0.dp,
                                        smoothnessAsPercentBR = 60,
                                        cornerRadiusBR = cornerRadius,
                                        smoothnessAsPercentTL = 0,
                                        cornerRadiusTL = 0.dp,
                                        smoothnessAsPercentBL = 60,
                                        cornerRadiusBL = cornerRadius,
                                        smoothnessAsPercentTR = 0
                                    )
                                }
                                val singleShape = remember(cornerRadius) {
                                    AbsoluteSmoothCornerShape(
                                        cornerRadiusTR = cornerRadius,
                                        smoothnessAsPercentBR = 60,
                                        cornerRadiusBR = cornerRadius,
                                        smoothnessAsPercentTL = 60,
                                        cornerRadiusTL = cornerRadius,
                                        smoothnessAsPercentBL = 60,
                                        cornerRadiusBL = cornerRadius,
                                        smoothnessAsPercentTR = 60
                                    )
                                }

                                val shape = remember(isFirst, isLast, cornerRadius) {
                                    when {
                                        isFirst && isLast -> singleShape
                                        isFirst -> topShape
                                        isLast -> bottomShape
                                        else -> middleShape
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(ListItemHeight)
                                        .clip(shape)
                                        .background(
                                            if (isActive) {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainer
                                            }
                                        )
                                ) {
                                    YouTubeListItem(
                                        item = songWrapper.item.second,
                                        isActive = isActive,
                                        isPlaying = isPlaying,
                                        isSelected = songWrapper.isSelected,
                                        inSelectionMode = selection,
                                        onSelectionChange = { songWrapper.isSelected = it },
                                        drawHighlight = false,
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    menuState.show {
                                                        YouTubeSongMenu(
                                                            song = songWrapper.item.second,
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
                                            .fillMaxSize()
                                            .combinedClickable(
                                                onClick = {
                                                    if (!selection) {
                                                        if (songWrapper.item.second.id == mediaMetadata?.id) {
                                                            playerConnection.player.togglePlayPause()
                                                        } else {
                                                            playerConnection.playQueue(
                                                                ListQueue(
                                                                    title = playlist.title,
                                                                    items = filteredSongs.map {
                                                                        it.second.toMediaItem()
                                                                    },
                                                                    startIndex = index
                                                                )
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
                                                }
                                            )
                                    )
                                }

                                // Add 3dp spacer between items (except after last)
                                if (!isLast) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                }
                            }
                        }
// Add bottom spacing after the last song
                        item(key = "songs_list_end") {
                            Spacer(modifier = Modifier.height(18.dp)) // 24
                        }
                    }

                    if (relatedItems.isNotEmpty()) {
                        item(key = "related_header") {
                            Column {
                                NavigationTitle(
                                    title = stringResource(R.string.you_might_also_like),
                                    modifier = Modifier.animateItem()
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                            }
                        }

                        item(key = "related_list") {
                            LazyRow(
                                contentPadding = WindowInsets.systemBars.only(
                                    WindowInsetsSides.Horizontal
                                ).asPaddingValues()
                            ) {
                                items(
                                    items = relatedItems,
                                    key = { "related_${it.id}" }
                                ) { item ->
                                    YouTubeGridItem(
                                        item = item,
                                        isActive = when (item) {
                                            is SongItem -> mediaMetadata?.id == item.id
                                            is AlbumItem -> mediaMetadata?.album?.id == item.id
                                            else -> false
                                        },
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    when (item) {
                                                        is SongItem ->
                                                            playerConnection.playQueue(
                                                                YouTubeQueue(
                                                                    WatchEndpoint(videoId = item.id),
                                                                    item.toMediaMetadata()
                                                                )
                                                            )

                                                        is AlbumItem -> navController.navigate("album/${item.id}")
                                                        is ArtistItem -> navController.navigate("artist/${item.id}")
                                                        is PlaylistItem -> navController.navigate(
                                                            "online_playlist/${item.id}"
                                                        )
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        when (item) {
                                                            is SongItem ->
                                                                YouTubeSongMenu(
                                                                    song = item,
                                                                    navController = navController,
                                                                    onDismiss = menuState::dismiss
                                                                )

                                                            is AlbumItem ->
                                                                YouTubeAlbumMenu(
                                                                    albumItem = item,
                                                                    navController = navController,
                                                                    onDismiss = menuState::dismiss
                                                                )

                                                            is ArtistItem ->
                                                                YouTubeArtistMenu(
                                                                    artist = item,
                                                                    onDismiss = menuState::dismiss
                                                                )

                                                            is PlaylistItem ->
                                                                YouTubePlaylistMenu(
                                                                    playlist = item,
                                                                    coroutineScope = coroutineScope,
                                                                    onDismiss = menuState::dismiss
                                                                )
                                                        }
                                                    }
                                                }
                                            )
                                            .animateItem()
                                    )
                                }
                            }
                        }

                        item(key = "related_end") {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    if (viewModel.continuation != null && songs.isNotEmpty() && isLoadingMore) {
                        item(key = "loading_more") {
                            ShimmerHost {
                                repeat(2) {
                                    ListItemPlaceHolder()
                                }
                            }
                        }
                    }
                } else {
                    // Show error state when playlist is null and there's an error
                    item(key = "error_state") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (error != null) {
                                    stringResource(R.string.error_unknown)
                                } else {
                                    stringResource(R.string.playlist_not_found)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = if (error !=
                                    null
                                ) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (error != null) {
                                    error!!
                                } else {
                                    stringResource(R.string.playlist_not_found_desc)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (error != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        viewModel.retry()
                                    }
                                ) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }
                }
            }
        }

        DraggableScrollbar(
            modifier = Modifier
                .padding(
                    LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
                        .asPaddingValues()
                )
                .align(Alignment.CenterEnd),
            scrollState = lazyListState,
            headerItems = 1
        )

        // Top App Bar
        TopAppBar(
            title = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    Text(
                        text = pluralStringResource(R.plurals.n_song, count, count),
                        style = MaterialTheme.typography.titleLarge
                    )
                } else if (isSearching) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                } else {
                    Text(playlist?.title.orEmpty())
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (isSearching) {
                            isSearching = false
                            query = TextFieldValue()
                        } else if (selection) {
                            selection = false
                        } else {
                            onBack()
                        }
                    },
                    onLongClick = {
                        if (!isSearching && !selection) {
                            navController.backToMain()
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (selection) R.drawable.close else R.drawable.arrow_back
                        ),
                        contentDescription = null
                    )
                }
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    IconButton(
                        onClick = {
                            if (count == wrappedSongs.size) {
                                wrappedSongs.forEach { it.isSelected = false }
                            } else {
                                wrappedSongs.forEach { it.isSelected = true }
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all
                            ),
                            contentDescription = null
                        )
                    }
                    IconButton(
                        onClick = {
                            menuState.show {
                                SelectionMediaMetadataMenu(
                                    songSelection = wrappedSongs.filter { it.isSelected }
                                        .map { it.item.second.toMediaItem().metadata!! },
                                    onDismiss = menuState::dismiss,
                                    clearAction = { selection = false },
                                    currentItems = emptyList()
                                )
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                } else if (!isSearching) {
                    // Keep search button in top app bar actions
                    IconButton(
                        onClick = { isSearching = true }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null
                        )
                    }
                }
            },
            colors = if (transparentAppBar && !selection && !isSearching) {
                TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            } else {
                TopAppBarDefaults.topAppBarColors()
            },
            scrollBehavior = scrollBehavior
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime))
                .align(Alignment.BottomCenter)
        )
    }
}
