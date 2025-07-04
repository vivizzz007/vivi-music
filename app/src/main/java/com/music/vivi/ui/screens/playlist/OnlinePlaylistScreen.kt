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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.LocalSyncUtils
import com.music.vivi.R
import com.music.vivi.constants.AlbumThumbnailSize
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.db.entities.PlaylistSongMap
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.AutoResizeText
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.EmptyPlaceholder
import com.music.vivi.ui.component.FontSizeRange
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.YouTubeListItem
import com.music.vivi.ui.component.shimmer.ButtonPlaceholder
import com.music.vivi.ui.component.shimmer.ListItemPlaceHolder
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.component.shimmer.TextPlaceholder
import com.music.vivi.ui.menu.YouTubePlaylistMenu
import com.music.vivi.ui.menu.YouTubeSongMenu
import com.music.vivi.ui.menu.YouTubeSongSelectionMenu
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.OnlinePlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch




import android.graphics.Bitmap

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable

import androidx.compose.material3.Divider

import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter





@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()

    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val syncUtils = LocalSyncUtils.current

    // States for share bottom sheets
    var showShareOptionsSheet by remember { mutableStateOf(false) }
    var showQrCodeSheet by remember { mutableStateOf(false) }
    val shareLink = remember { playlist?.shareLink }

    // QR Code generation
    val qrCodeBitmap = remember(shareLink) {
        shareLink?.let { link ->
            try {
                val bitMatrix = QRCodeWriter().encode(
                    link,
                    BarcodeFormat.QR_CODE,
                    512,
                    512
                )
                val width = bitMatrix.width
                val height = bitMatrix.height
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                    for (x in 0 until width) {
                        for (y in 0 until height) {
                            setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK
                            else android.graphics.Color.WHITE)
                        }
                    }
                }.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val filteredSongs = remember(songs, query, hideExplicit) {
        songs.mapIndexed { index, song -> index to song }
            .filter { (_, song) ->
                (!hideExplicit || !song.explicit) && (query.text.isEmpty() ||
                        song.title.contains(query.text, ignoreCase = true) ||
                        song.artists.any { it.name.contains(query.text, ignoreCase = true) })
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
    }

    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    val downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, playlist?.title!!),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false }
                ) {
                    Text(text = stringResource(R.string.cancels))  // Your own cancel string if defined
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        database.transaction {
                            dbPlaylist?.id?.let { clearPlaylist(it) }
                        }

                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false
                            )
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<Int>, Int>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()
        ) {
            if (filteredSongs.isEmpty() && isSearching) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.search_icon,
                        text = stringResource(R.string.no_results_found)
                    )
                }
            }
            playlist.let { playlist ->
                if (playlist != null) {
                    if (!isSearching) {
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(12.dp)
                            ) {
                                AsyncImage(
                                    model = playlist.thumbnail,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(AlbumThumbnailSize)
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                        .align(alignment = Alignment.CenterHorizontally)
                                )

                                Spacer(Modifier.width(12.dp))

                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AutoResizeText(
                                        text = playlist.title,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSizeRange = FontSizeRange(16.sp, 22.sp)
                                    )

                                    playlist.author?.let { artist ->
                                        Text(buildAnnotatedString {
                                            withStyle(
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Normal,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                ).toSpanStyle()
                                            ) {
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
                                            }
                                        })
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    Row {
                                        if (playlist.id != "LM") {
                                            Button(
                                                onClick = {
                                                    if (dbPlaylist?.playlist == null) {
                                                        database.transaction {
                                                            val playlistEntity = PlaylistEntity(
                                                                name = playlist.title,
                                                                browseId = playlist.id,
                                                                isEditable = playlist.isEditable,
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
                                                            update(dbPlaylist!!.playlist.toggleLike())
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(4.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            ) {
                                                Icon(
                                                    painter = painterResource(
                                                        if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border
                                                    ),
                                                    contentDescription = null
                                                )
                                            }
                                        }

                                        if (dbPlaylist != null) {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED -> {
                                                    Button(
                                                        onClick = {
                                                            showRemoveDownloadDialog = true
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(4.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                    ) {
                                                        Icon(
                                                            painterResource(R.drawable.offline),
                                                            contentDescription = null
                                                        )
                                                    }
                                                }

                                                Download.STATE_DOWNLOADING -> {
                                                    Button(
                                                        onClick = {
                                                            songs.forEach { song ->
                                                                DownloadService.sendRemoveDownload(
                                                                    context,
                                                                    ExoDownloadService::class.java,
                                                                    song.id,
                                                                    false
                                                                )
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(4.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                    ) {
                                                        CircularProgressIndicator(
                                                            strokeWidth = 2.dp,
                                                            modifier = Modifier.size(24.dp),
                                                            color = MaterialTheme.colorScheme.surfaceContainer
                                                        )
                                                    }
                                                }

                                                else -> {
                                                    Button(
                                                        onClick = {
                                                            viewModel.viewModelScope.launch(
                                                                Dispatchers.IO
                                                            ) {
                                                                syncUtils.syncPlaylist(
                                                                    playlist.id,
                                                                    dbPlaylist!!.id
                                                                )
                                                            }

                                                            songs.forEach { song ->
                                                                val downloadRequest =
                                                                    DownloadRequest.Builder(
                                                                        song.id,
                                                                        song.id.toUri()
                                                                    )
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
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(4.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                    ) {
                                                        Icon(
                                                            painterResource(R.drawable.download),
                                                            contentDescription = null
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                playerConnection.addToQueue(
                                                    items = songs.map { it.toMediaItem() }
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(4.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.queue_music),
                                                contentDescription = null
                                            )
                                        }

                                        if (playlist.id != "LM") {
                                            Button(
                                                onClick = {
                                                    showShareOptionsSheet = true
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(4.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.shares),
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = {
                                            playerConnection.playQueue(YouTubeQueue(playlist.shuffleEndpoint))
                                        },
                                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.shuffle),
                                            contentDescription = null,
                                            modifier = Modifier.size(ButtonDefaults.IconSize)
                                        )
                                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                        Text(stringResource(R.string.shuffle))
                                    }

                                    playlist.radioEndpoint?.let { radioEndpoint ->
                                        Button(
                                            onClick = {
                                                playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                                            },
                                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.radio),
                                                contentDescription = null,
                                                modifier = Modifier.size(ButtonDefaults.IconSize)
                                            )
                                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                            Text(stringResource(R.string.radio))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(
                        items = filteredSongs,
                        key = { (index, _) -> index }
                    ) { (index, song) ->
                        val onCheckedChange: (Boolean) -> Unit = {
                            if (it) {
                                selection.add(index)
                            } else {
                                selection.remove(index)
                            }
                        }
                        if (index == 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.n_song,
                                        filteredSongs.size,
                                        filteredSongs.size
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }

                        YouTubeListItem(
                            item = song,
                            isActive = mediaMetadata?.id == song.id,
                            isPlaying = isPlaying,
                            trailingContent = {
                                if (inSelectMode) {
                                    Checkbox(
                                        checked = index in selection,
                                        onCheckedChange = onCheckedChange
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                YouTubeSongMenu(
                                                    song = song,
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
                                }
                            },
                            modifier = Modifier
                                .combinedClickable(
                                    enabled = !hideExplicit || !song.explicit,
                                    onClick = {
                                        if (inSelectMode) {
                                            onCheckedChange(index !in selection)
                                        } else if (song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue(
                                                    song.endpoint ?: WatchEndpoint(videoId = song.id),
                                                    song.toMediaMetadata()
                                                )
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        if (!inSelectMode) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            inSelectMode = true
                                            onCheckedChange(true)
                                        }
                                    }
                                )
                                .alpha(if (hideExplicit && song.explicit) 0.3f else 1f)
                                .animateItem()
                        )
                    }
                } else {
                    item {
                        ShimmerHost {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(
                                        modifier = Modifier
                                            .size(AlbumThumbnailSize)
                                            .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                            .background(MaterialTheme.colorScheme.onSurface)
                                    )

                                    Spacer(Modifier.width(16.dp))

                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        TextPlaceholder()
                                        TextPlaceholder()
                                        TextPlaceholder()
                                    }
                                }

                                Spacer(Modifier.padding(8.dp))

                                Row {
                                    ButtonPlaceholder(Modifier.weight(1f))

                                    Spacer(Modifier.width(12.dp))

                                    ButtonPlaceholder(Modifier.weight(1f))
                                }
                            }

                            repeat(6) {
                                ListItemPlaceHolder()
                            }
                        }
                    }
                }
            }
        }

        // Share Options Bottom Sheet
        if (showShareOptionsSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showShareOptionsSheet = false
                }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.share_playlist),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )

                    // Share via link
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showShareOptionsSheet = false
                                val intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareLink)
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                            }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.link_icon),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = stringResource(R.string.share_link))
                    }

                    Divider()

                    // Share via QR code
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showShareOptionsSheet = false
                                showQrCodeSheet = true
                            }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.qr_code_icon),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = stringResource(R.string.share_qr_code))
                    }
                }
            }
        }

        // QR Code Bottom Sheet
        if (showQrCodeSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showQrCodeSheet = false
                    showShareOptionsSheet = true
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.share_via_qr),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (qrCodeBitmap != null) {
                        Image(
                            bitmap = qrCodeBitmap,
                            contentDescription = stringResource(R.string.qr_code),
                            modifier = Modifier.size(200.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = playlist?.title ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = shareLink ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showQrCodeSheet = false
                            showShareOptionsSheet = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }
            }
        }

        TopAppBar(
            title = {
                if (inSelectMode) {
                    Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size))
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
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        trailingIcon = {
                            if (query.text.isNotEmpty()) {
                                IconButton(
                                    onClick = { query = TextFieldValue("") }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.close),
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    )
                } else {
                    if (showTopBarTitle) Text(playlist?.title.orEmpty())
                }
            },
            navigationIcon = {
                if (inSelectMode) {
                    IconButton(onClick = onExitSelectionMode) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (isSearching) {
                                isSearching = false
                                query = TextFieldValue()
                            } else {
                                navController.navigateUp()
                            }
                        },
                        onLongClick = {
                            if (!isSearching) {
                                navController.backToMain()
                            }
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.back_icon),
                            contentDescription = null
                        )
                    }
                }
            },
            actions = {
                if (inSelectMode) {
                    Checkbox(
                        checked = selection.size == filteredSongs.size,
                        onCheckedChange = {
                            if (selection.size == filteredSongs.size) {
                                selection.clear()
                                if (selection.size == songs.size) {
                                    selection.clear()
                                }
                            } else {
                                selection.clear()
                                selection.addAll(filteredSongs.map { it.first })
                            }
                        }
                    )
                    IconButton(
                        enabled = selection.isNotEmpty(),
                        onClick = {
                            menuState.show {
                                YouTubeSongSelectionMenu(
                                    selection = selection.mapNotNull { songs.getOrNull(it) },
                                    onDismiss = menuState::dismiss,
                                    onExitSelectionMode = onExitSelectionMode
                                )
                            }
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                } else if (!isSearching) {
                    IconButton(
                        onClick = { isSearching = true }
                    ) {
                        Icon(
                            painterResource(R.drawable.search_icon),
                            contentDescription = null
                        )
                    }
                }
                if (!isSearching && !inSelectMode) {
                    IconButton(
                        onClick = {
                            menuState.show {
                                YouTubePlaylistMenu(
                                    playlist = playlist!!,
                                    songs = songs,
                                    coroutineScope = coroutineScope,
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
                }
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
