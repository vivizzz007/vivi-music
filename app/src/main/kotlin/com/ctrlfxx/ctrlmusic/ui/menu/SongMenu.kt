/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.ctrlfxx.ctrlmusic.ui.menu

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.ctrlfxx.ctrlmusic.LocalDatabase
import com.ctrlfxx.ctrlmusic.LocalDownloadUtil
import com.ctrlfxx.ctrlmusic.LocalListenTogetherManager
import com.ctrlfxx.ctrlmusic.LocalPlayerConnection
import com.ctrlfxx.ctrlmusic.LocalSyncUtils
import com.ctrlfxx.ctrlmusic.R
import com.ctrlfxx.ctrlmusic.constants.ListItemHeight
import com.ctrlfxx.ctrlmusic.constants.ListThumbnailSize
import com.ctrlfxx.ctrlmusic.db.entities.ArtistEntity
import com.ctrlfxx.ctrlmusic.db.entities.Event
import com.ctrlfxx.ctrlmusic.db.entities.SpeedDialItem
import com.ctrlfxx.ctrlmusic.db.entities.PlaylistSong
import com.ctrlfxx.ctrlmusic.db.entities.Song
import com.ctrlfxx.ctrlmusic.extensions.toMediaItem
import com.ctrlfxx.ctrlmusic.models.toMediaMetadata
import com.ctrlfxx.ctrlmusic.playback.ExoDownloadService
import com.ctrlfxx.ctrlmusic.playback.queues.YouTubeQueue
import com.ctrlfxx.ctrlmusic.ui.component.ListDialog
import com.ctrlfxx.ctrlmusic.ui.component.LocalBottomSheetPageState
import com.ctrlfxx.ctrlmusic.ui.component.Material3MenuGroup
import com.ctrlfxx.ctrlmusic.ui.component.Material3MenuItemData
import com.ctrlfxx.ctrlmusic.ui.component.NewAction
import com.ctrlfxx.ctrlmusic.ui.component.NewActionGrid
import com.ctrlfxx.ctrlmusic.ui.component.SongListItem
import com.ctrlfxx.ctrlmusic.ui.component.TextFieldDialog
import com.ctrlfxx.ctrlmusic.utils.listItemShape
import com.ctrlfxx.ctrlmusic.ui.utils.ShowMediaInfo
import com.ctrlfxx.ctrlmusic.viewmodels.CachePlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun SongMenu(
    originalSong: Song,
    event: Event? = null,
    navController: NavController,
    playlistSong: PlaylistSong? = null,
    playlistBrowseId: String? = null,
    onDismiss: () -> Unit,
    isFromCache: Boolean = false,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val songState = database.song(originalSong.id).collectAsState(initial = originalSong)
    val song = songState.value ?: originalSong
    val download by LocalDownloadUtil.current.getDownload(originalSong.id)
        .collectAsState(initial = null)
    val downloadUtil = LocalDownloadUtil.current
    var isSavingToPhone by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val syncUtils = LocalSyncUtils.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val scope = rememberCoroutineScope()
    var refetchIconDegree by remember { mutableFloatStateOf(0f) }

    val cacheViewModel = hiltViewModel<CachePlaylistViewModel>()

    val rotationAnimation by animateFloatAsState(
        targetValue = refetchIconDegree,
        animationSpec = tween(durationMillis = 800),
        label = "",
    )

    val isPinned by database.speedDialDao.isPinned(song.id).collectAsState(initial = false)

    val orderedArtists by produceState(initialValue = emptyList<ArtistEntity>(), song) {
        withContext(Dispatchers.IO) {
            val artistMaps = database.songArtistMap(song.id).sortedBy { it.position }
            val sorted = artistMaps.mapNotNull { map ->
                song.artists.firstOrNull { it.id == map.artistId }
            }
            value = sorted
        }
    }

    var showEditDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val TextFieldValueSaver: Saver<TextFieldValue, *> = Saver(
        save = { it.text },
        restore = { text -> TextFieldValue(text, TextRange(text.length)) }
    )

    var titleField by rememberSaveable(stateSaver = TextFieldValueSaver) {
        mutableStateOf(TextFieldValue(song.song.title))
    }

    var artistField by rememberSaveable(stateSaver = TextFieldValueSaver) {
        mutableStateOf(TextFieldValue(song.artists.firstOrNull()?.name.orEmpty()))
    }

    if (showEditDialog) {
        TextFieldDialog(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = null
                )
            },
            title = {
                Text(text = stringResource(R.string.edit_song))
            },
            textFields = listOf(
                stringResource(R.string.song_title) to titleField,
                stringResource(R.string.artist_name) to artistField
            ),
            onTextFieldsChange = { index, newValue ->
                if (index == 0) titleField = newValue
                else artistField = newValue
            },
            onDoneMultiple = { values ->
                val newTitle = values[0]
                val newArtist = values[1]

                coroutineScope.launch {
                    database.query {
                        update(song.song.copy(title = newTitle))
                        val artist = song.artists.firstOrNull()
                        if (artist != null) {
                            update(artist.copy(name = newArtist))
                        }
                    }

                    showEditDialog = false
                    onDismiss()
                }
            },
            onDismiss = { showEditDialog = false }
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { browseId ->
                    YouTube.addToPlaylist(browseId, song.id)
                }
            }
            listOf(song.id)
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        },
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.already_in_playlist)) },
                    leadingContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier = Modifier.clickable { showErrorPlaylistAddDialog = false },
                )
            }

            items(listOf(song)) { song ->
                SongListItem(song = song)
            }
        }
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(
                items = song.artists.distinctBy { it.id },
                key = { it.id },
            ) { artist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(ListItemHeight)
                        .clickable {
                            navController.navigate("artist/${artist.id}")
                            showSelectArtistDialog = false
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp),
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = artist.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(ListThumbnailSize)
                                .clip(CircleShape),
                        )
                    }
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }

    SongListItem(
        song = song,
        badges = {},
        shape = listItemShape(0, 2),
        backgroundColor = Color.Transparent,
        trailingContent = {
            IconButton(
                onClick = {
                    val s = song.song.toggleLike()
                    database.query {
                        update(s)
                    }
                    syncUtils.likeSong(s)
                },
            ) {
                Icon(
                    painter = painterResource(if (song.song.liked) R.drawable.favorite else R.drawable.favorite_border),
                    tint = if (song.song.liked) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    contentDescription = null,
                )
            }
        },
    )

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    val bottomSheetPageState = LocalBottomSheetPageState.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost

    LazyColumn(
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item {
            NewActionGrid(
                actions = listOf(
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.edit),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.edit),
                        onClick = { showEditDialog = true }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.playlist_add),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.add_to_an_playlist),
                        onClick = { showChoosePlaylistDialog = true }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.share),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.share),
                        onClick = {
                            onDismiss()
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${song.id}")
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    )
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
            )
        }
        item {
            Material3MenuGroup(
                expressive = true,
                items = listOfNotNull(
                    if (listenTogetherManager != null && listenTogetherManager.isInRoom && !listenTogetherManager.isHost) {
                        Material3MenuItemData(
                            title = { Text(text = stringResource(R.string.suggest_to_host)) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.queue_music),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                val durationMs = if (song.song.duration > 0) song.song.duration.toLong() * 1000 else 180000L
                                val trackInfo = com.ctrlfxx.ctrlmusic.listentogether.TrackInfo(
                                    id = song.id,
                                    title = song.song.title,
                                    artist = orderedArtists.joinToString(", ") { it.name },
                                    album = song.song.albumName,
                                    duration = durationMs,
                                    thumbnail = song.thumbnailUrl
                                )
                                listenTogetherManager.suggestTrack(trackInfo)
                                onDismiss()
                            }
                        )
                    } else null,
                    if (!isGuest) {
                        Material3MenuItemData(
                            title = { Text(text = stringResource(R.string.start_radio)) },
                            description = { Text(text = stringResource(R.string.start_radio_desc)) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.radio),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                onDismiss()
                                playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                            }
                        )
                    } else null,
                    if (!isGuest) {
                        Material3MenuItemData(
                            title = { Text(text = stringResource(R.string.play_next)) },
                            description = { Text(text = stringResource(R.string.play_next_desc)) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.playlist_play),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                onDismiss()
                                playerConnection.playNext(song.toMediaItem())
                            }
                        )
                    } else null,
                    if (!isGuest) {
                        Material3MenuItemData(
                            title = { Text(text = stringResource(R.string.add_to_queue)) },
                            description = { Text(text = stringResource(R.string.add_to_queue_desc)) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.queue_music),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                onDismiss()
                                playerConnection.addToQueue(song.toMediaItem())
                            }
                        )
                    } else null
                )
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            Material3MenuGroup(
                expressive = true,
                items = buildList {
                    add(
                        Material3MenuItemData(
                            title = { 
                                Text(
                                    text = if (isPinned) "Unpin from Speed dial" else "Pin to Speed dial" 
                                ) 
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(if (isPinned) R.drawable.remove else R.drawable.add),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    if (isPinned) {
                                        database.speedDialDao.delete(song.id)
                                    } else {
                                        database.speedDialDao.insert(
                                            SpeedDialItem(
                                                id = song.id,
                                                title = song.song.title,
                                                subtitle = song.artists.joinToString(", ") { it.name },
                                                thumbnailUrl = song.song.thumbnailUrl,
                                                type = "SONG",
                                                explicit = song.song.explicit
                                            )
                                        )
                                    }
                                }
                                onDismiss()
                            }
                        )
                    )
                    add(
                        Material3MenuItemData(
                            title = {
                                Text(
                                    text = stringResource(
                                        if (song.song.inLibrary == null) R.string.add_to_library
                                        else R.string.remove_from_library
                                    )
                                )
                            },
                            description = { Text(text = stringResource(R.string.add_to_library_desc)) },
                            icon = {
                                Icon(
                                    painter = painterResource(
                                        if (song.song.inLibrary == null) R.drawable.library_add
                                        else R.drawable.library_add_check
                                    ),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                val currentSong = song.song
                                val isInLibrary = currentSong.inLibrary != null
                                val token =
                                    if (isInLibrary) currentSong.libraryRemoveToken else currentSong.libraryAddToken

                                token?.let {
                                    coroutineScope.launch {
                                        YouTube.feedback(listOf(it))
                                    }
                                }

                                database.query {
                                    update(song.song.toggleLibrary())
                                }
                            }
                        )
                    )
                    if (event != null) {
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.remove_from_history)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.delete),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    onDismiss()
                                    database.query {
                                        delete(event)
                                    }
                                }
                            )
                        )
                    }
                    if (playlistSong != null) {
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.remove_from_playlist)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.delete),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    database.transaction {
                                        coroutineScope.launch {
                                            playlistBrowseId?.let { playlistId ->
                                                if (playlistSong.map.setVideoId != null) {
                                                    YouTube.removeFromPlaylist(
                                                        playlistId,
                                                        playlistSong.map.songId,
                                                        playlistSong.map.setVideoId
                                                    )
                                                }
                                            }
                                        }
                                        move(
                                            playlistSong.map.playlistId,
                                            playlistSong.map.position,
                                            Int.MAX_VALUE
                                        )
                                        delete(playlistSong.map.copy(position = Int.MAX_VALUE))
                                    }
                                    onDismiss()
                                }
                            )
                        )
                    }
                    if (isFromCache) {
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.remove_from_cache)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.delete),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    onDismiss()
                                    cacheViewModel.removeSongFromCache(song.id)
                                }
                            )
                        )
                    }
                }
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            Material3MenuGroup(
                expressive = true,
                items = buildList {
                    add(
                    when (download?.state) {
                        Download.STATE_COMPLETED -> {
                            Material3MenuItemData(
                                title = {
                                    Text(
                                        text = stringResource(R.string.remove_download)
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.offline),
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        song.id,
                                        false,
                                    )
                                }
                            )
                        }
                        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.downloading)) },
                                icon = {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                },
                                onClick = {
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        song.id,
                                        false,
                                    )
                                }
                            )
                        }
                        else -> {
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.action_download)) },
                                description = { Text(text = stringResource(R.string.download_desc)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.download),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    val downloadRequest =
                                        DownloadRequest
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
                            )
                        }
                    }
                    )
                    if (download?.state == Download.STATE_COMPLETED) {
                        add(
                            Material3MenuItemData(
                                title = {
                                    Text(
                                        text = stringResource(
                                            if (isSavingToPhone) R.string.saving_to_phone else R.string.save_to_phone
                                        )
                                    )
                                },
                                description = { Text(text = stringResource(R.string.save_to_phone_desc)) },
                                icon = {
                                    if (isSavingToPhone) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(R.drawable.download),
                                            contentDescription = null,
                                        )
                                    }
                                },
                                onClick = {
                                    if (!isSavingToPhone) {
                                        isSavingToPhone = true
                                        val appContext = context.applicationContext
                                        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
                                        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                            val result = com.ctrlfxx.ctrlmusic.playback.SongExporter.exportToMusic(
                                                context = appContext,
                                                downloadCache = downloadUtil.downloadCache,
                                                mediaId = song.id,
                                                title = song.song.title,
                                                artist = song.artists.joinToString { it.name },
                                                album = song.album?.title,
                                            )
                                            isSavingToPhone = false
                                            val message = result.fold(
                                                onSuccess = { appContext.getString(R.string.saved_to_phone) },
                                                onFailure = {
                                                    appContext.getString(
                                                        R.string.save_to_phone_failed,
                                                        it.message ?: it.javaClass.simpleName
                                                    )
                                                }
                                            )
                                            android.widget.Toast.makeText(appContext, message, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        )
                    }
                }
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            Material3MenuGroup(
                expressive = true,
                items = buildList {
                    add(
                        Material3MenuItemData(
                            title = { Text(text = stringResource(R.string.view_artist)) },
                            description = { Text(text = song.artists.joinToString { it.name }) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.artist),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                if (song.artists.size == 1) {
                                    navController.navigate("artist/${song.artists[0].id}")
                                    onDismiss()
                                } else {
                                    showSelectArtistDialog = true
                                }
                            }
                        )
                    )
                    val albumId = song.song.albumId?.takeIf { it.isNotBlank() } ?: song.album?.id?.takeIf { it.isNotBlank() }
                    val albumName = song.song.albumName ?: song.album?.title
                    
                    if (!albumId.isNullOrBlank()) {
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.view_album)) },
                                description = albumName?.let { name ->
                                    { Text(text = name) }
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.album),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    onDismiss()
                                    navController.navigate("album/$albumId")
                                }
                            )
                        )
                    }
                    add(
                        Material3MenuItemData(
                            title = { Text(text = stringResource(R.string.refetch)) },
                            description = { Text(text = stringResource(R.string.refetch_desc)) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.sync),
                                    contentDescription = null,
                                    modifier = Modifier.graphicsLayer(rotationZ = rotationAnimation),
                                )
                            },
                            onClick = {
                                refetchIconDegree -= 360
                                scope.launch(Dispatchers.IO) {
                                    YouTube.queue(listOf(song.id)).onSuccess {
                                        val newSong = it.firstOrNull()
                                        if (newSong != null) {
                                            database.transaction {
                                                update(song, newSong.toMediaMetadata())
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    )
                    add(
                        Material3MenuItemData(
                            title = { Text(text = stringResource(R.string.details)) },
                            description = { Text(text = stringResource(R.string.details_desc)) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.info),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                onDismiss()
                                bottomSheetPageState.show {
                                    ShowMediaInfo(song.id)
                                }
                            }
                        )
                    )
                }
            )
        }
    }
}
