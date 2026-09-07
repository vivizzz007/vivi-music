/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.ui.component.DefaultDialog
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.annotation.ExperimentalCoilApi
import coil3.imageLoader
import android.widget.Toast
import androidx.compose.material3.Switch
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.MaxImageCacheSizeKey
import com.music.vivi.constants.MaxSongCacheSizeKey
import com.music.vivi.constants.SaveDownloadsToPublicFolderKey
import com.music.vivi.extensions.tryOrNull
import com.music.vivi.ui.component.ActionPromptDialog
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.ExpressiveSettingGroup
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.formatFileSize
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okio.ByteString.Companion.encodeUtf8
import timber.log.Timber
import kotlin.math.roundToInt

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class, DelicateCoilApi::class)
@Composable
fun StorageSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val imageDiskCache = context.imageLoader.diskCache ?: return
    val playerCache = LocalPlayerConnection.current?.service?.playerCache ?: return
    val downloadCache = LocalPlayerConnection.current?.service?.downloadCache ?: return

    val coroutineScope = rememberCoroutineScope()
    val songCacheString = stringResource(R.string.song_cache).lowercase()
    val imageCacheString = stringResource(R.string.image_cache).lowercase()
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(
        key = MaxImageCacheSizeKey,
        defaultValue = 512
    )
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(
        key = MaxSongCacheSizeKey,
        defaultValue = 1024
    )
    val (saveDownloadsToPublic, onSaveDownloadsToPublicChange) = rememberPreference(
        key = SaveDownloadsToPublicFolderKey,
        defaultValue = false
    )
    val downloadUtil = LocalDownloadUtil.current

    var clearDownloads by remember { mutableStateOf(false) }
    var clearCacheDialog by remember { mutableStateOf(false) }
    var clearImageCacheDialog by remember { mutableStateOf(false) }
    android.util.Log.d("StorageSettings", "Recomposed: clearImageCacheDialog = $clearImageCacheDialog")

    // State for the confirmation dialog
    var showCacheWarningDialog by remember { mutableStateOf(false) }
    var cacheType by remember { mutableStateOf("") }
    var cacheUsage by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var onConfirmAction by remember { mutableStateOf<() -> Unit>({}) }

    var showExportDialog by remember { mutableStateOf(false) }
    val allPlaylists by database.playlistsByNameAsc().collectAsState(initial = emptyList())
    val allAlbums by database.albumsByNameAsc().collectAsState(initial = emptyList())
    val selectedPlaylists = remember { mutableStateListOf<String>() }
    val selectedAlbums = remember { mutableStateListOf<String>() }


    var imageCacheSize by remember {
        androidx.compose.runtime.mutableLongStateOf(imageDiskCache.size)
    }
    var playerCacheSize by remember {
        androidx.compose.runtime.mutableLongStateOf(tryOrNull { playerCache.cacheSpace } ?: 0)
    }
    var downloadCacheSize by remember {
        mutableLongStateOf(tryOrNull { downloadCache.cacheSpace } ?: 0)
    }
    val imageCacheProgress by animateFloatAsState(
        targetValue = (imageCacheSize.toFloat() / (maxImageCacheSize * 1024 * 1024L)).coerceIn(
            0f,
            1f
        ),
        label = "imageCacheProgress",
    )
    val playerCacheProgress by animateFloatAsState(
        targetValue = (playerCacheSize.toFloat() / (maxSongCacheSize * 1024 * 1024L)).coerceIn(
            0f,
            1f
        ),
        label = "playerCacheProgress",
    )

    LaunchedEffect(maxImageCacheSize) {
        SingletonImageLoader.reset()
        if (maxImageCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                imageDiskCache.clear()
            }
        }
    }
    LaunchedEffect(maxSongCacheSize) {
        if (maxSongCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                playerCache.keys.forEach { key ->
                    playerCache.removeResource(key)
                }
            }
        }
    }

    LaunchedEffect(imageDiskCache) {
        while (isActive) {
            delay(500)
            imageCacheSize = imageDiskCache.size
        }
    }
    LaunchedEffect(playerCache) {
        while (isActive) {
            delay(500)
            playerCacheSize = tryOrNull { playerCache.cacheSpace } ?: 0
        }
    }
    LaunchedEffect(downloadCache) {
        while (isActive) {
            delay(500)
            downloadCacheSize = tryOrNull { downloadCache.cacheSpace } ?: 0
        }
    }

    if (clearDownloads) {
        ActionPromptDialog(
            title = stringResource(R.string.clear_all_downloads),
            onDismiss = { clearDownloads = false },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    downloadCache.keys.forEach { key ->
                        downloadCache.removeResource(key)
                    }
                }
                clearDownloads = false
            },
            onCancel = { clearDownloads = false },
            content = {
                Text(text = stringResource(R.string.clear_downloads_dialog))
            }
        )
    }
    if (clearCacheDialog) {
        ActionPromptDialog(
            title = stringResource(R.string.clear_song_cache),
            onDismiss = { clearCacheDialog = false },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    playerCache.keys.forEach { key ->
                        playerCache.removeResource(key)
                    }
                }
                clearCacheDialog = false
            },
            onCancel = { clearCacheDialog = false },
            content = {
                Text(text = stringResource(R.string.clear_song_cache_dialog))
            }
        )
    }
    if (clearImageCacheDialog) {
        ActionPromptDialog(
            title = stringResource(R.string.clear_image_cache),
            onDismiss = { clearImageCacheDialog = false },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    val urlsToPreserve = mutableSetOf<String>()
                    val downloadedSongs = try {
                        database.downloadedSongsByNameAsc().first()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    downloadedSongs.forEach { song ->
                        song.song.thumbnailUrl?.let { urlsToPreserve.add(it.encodeUtf8().sha256().hex()) }
                        song.album?.thumbnailUrl?.let { urlsToPreserve.add(it.encodeUtf8().sha256().hex()) }
                    }
                    val directory = imageDiskCache.directory.toFile()
                    if (directory.exists() && directory.isDirectory) {
                        directory.listFiles()?.forEach { file ->
                            if (file.isFile && !file.name.startsWith("journal")) {
                                val isPreserved = urlsToPreserve.any { hash -> file.name.startsWith(hash) }
                                if (!isPreserved) {
                                    file.delete()
                                }
                            }
                        }
                    }
                    imageDiskCache.clear()
                }
                clearImageCacheDialog = false
            },
            onCancel = { clearImageCacheDialog = false },
            content = {
                Text(text = stringResource(R.string.clear_image_cache_dialog))
            }
        )
    }

    // Confirmation Dialog
    if (showCacheWarningDialog) {
        AlertDialog(
            onDismissRequest = { showCacheWarningDialog = false },
            title = { Text(stringResource(R.string.cache_size_warning_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.cache_size_warning_message,
                        formatFileSize(cacheUsage),
                        cacheType
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmAction()
                        showCacheWarningDialog = false
                    }
                ) {
                    Text(
                        stringResource(R.string.cache_size_warning_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCacheWarningDialog = false }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        )
    }

    if (showExportDialog) {
        DefaultDialog(
            onDismiss = { showExportDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.select_items_to_export),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    if (allPlaylists.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.playlists),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(allPlaylists) { playlist ->
                            val isSelected = playlist.id in selectedPlaylists
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) selectedPlaylists.remove(playlist.id)
                                        else selectedPlaylists.add(playlist.id)
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedPlaylists.add(playlist.id)
                                        else selectedPlaylists.remove(playlist.id)
                                    }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(
                                        text = playlist.playlist.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${playlist.songCount} songs",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (allAlbums.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.albums),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(allAlbums) { album ->
                            val isSelected = album.id in selectedAlbums
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) selectedAlbums.remove(album.id)
                                        else selectedAlbums.add(album.id)
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedAlbums.add(album.id)
                                        else selectedAlbums.remove(album.id)
                                    }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(
                                        text = album.album.title,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = album.artists.joinToString(", ") { it.name }.ifBlank { "Unknown Artist" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            buttons = {
                TextButton(
                    onClick = {
                        val totalSelected = selectedPlaylists.size + selectedAlbums.size
                        val totalAvailable = allPlaylists.size + allAlbums.size
                        if (totalSelected == totalAvailable) {
                            selectedPlaylists.clear()
                            selectedAlbums.clear()
                        } else {
                            selectedPlaylists.clear()
                            selectedPlaylists.addAll(allPlaylists.map { it.id })
                            selectedAlbums.clear()
                            selectedAlbums.addAll(allAlbums.map { it.id })
                        }
                    }
                ) {
                    val allSelected = (selectedPlaylists.size + selectedAlbums.size) == (allPlaylists.size + allAlbums.size) && (allPlaylists.isNotEmpty() || allAlbums.isNotEmpty())
                    Text(if (allSelected) "Deselect All" else "Select All")
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showExportDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
                val selectedCount = selectedPlaylists.size + selectedAlbums.size
                TextButton(
                    enabled = selectedCount > 0,
                    onClick = {
                        val pList = selectedPlaylists.toList()
                        val aList = selectedAlbums.toList()
                        showExportDialog = false
                        coroutineScope.launch(Dispatchers.IO) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Exporting $selectedCount items to phone storage...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            for (pId in pList) {
                                val songs = database.playlistSongs(pId).firstOrNull() ?: emptyList()
                                downloadUtil.exportAlbumToPublicStorage(songs.map { it.song.id })
                            }
                            for (aId in aList) {
                                val songs = database.albumSongs(aId).firstOrNull() ?: emptyList()
                                downloadUtil.exportAlbumToPublicStorage(songs.map { it.id })
                            }
                        }
                    }
                ) {
                    Text("${stringResource(R.string.export_selected)} ($selectedCount)")
                }
            }
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )
        ExpressiveSettingGroup(
            title = stringResource(R.string.storage),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.storage),
                    title = { Text(stringResource(R.string.downloaded_songs)) },
                    trailingContent = {
                        Text(text = formatFileSize(downloadCacheSize))
                    }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.clear_all),
                    title = { Text(stringResource(R.string.clear_all_downloads)) },
                    onClick = {
                        clearDownloads = true
                    }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.download),
                    title = { Text(stringResource(R.string.save_downloads_to_public_folder)) },
                    description = { Text(stringResource(R.string.save_downloads_to_public_folder_desc)) },
                    trailingContent = {
                        Switch(
                            checked = saveDownloadsToPublic,
                            onCheckedChange = onSaveDownloadsToPublicChange
                        )
                    },
                    onClick = {
                        onSaveDownloadsToPublicChange(!saveDownloadsToPublic)
                    }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.download),
                    title = { Text(stringResource(R.string.export_downloads_to_device)) },
                    description = { Text(stringResource(R.string.export_downloads_to_device_desc)) },
                    onClick = {
                        downloadUtil.exportAllDownloadedSongs()
                        Toast.makeText(context, context.getString(R.string.exporting_downloads), Toast.LENGTH_SHORT).show()
                    }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.storage),
                    title = { Text(stringResource(R.string.export_playlists_to_phone)) },
                    description = { Text(stringResource(R.string.export_playlists_to_phone_desc)) },
                    onClick = {
                        showExportDialog = true
                    }
                )
            )
        )

        ExpressiveSettingGroup(
            title = stringResource(R.string.song_cache),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.cached),
                    title = { Text(stringResource(R.string.max_song_cache_size)) },
                    description = {
                        val songCacheValues =
                            remember { listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, -1) }
                        Column {
                            Text(
                                text = when (maxSongCacheSize) {
                                    0 -> stringResource(R.string.disable)
                                    -1 -> stringResource(R.string.unlimited)
                                    else -> formatFileSize(maxSongCacheSize * 1024 * 1024L)
                                }
                            )
                            Slider(
                                value = songCacheValues.indexOf(maxSongCacheSize).toFloat(),
                                onValueChange = {
                                    val newValue = songCacheValues[it.roundToInt()]
                                    val newLimitInBytes = if (newValue == -1) {
                                        Long.MAX_VALUE
                                    } else {
                                        newValue * 1024 * 1024L
                                    }

                                    if (newLimitInBytes < playerCacheSize) {
                                        cacheUsage = playerCacheSize
                                        cacheType = songCacheString
                                        onConfirmAction = { onMaxSongCacheSizeChange(newValue) }
                                        showCacheWarningDialog = true
                                    } else {
                                        onMaxSongCacheSizeChange(newValue)
                                    }
                                },
                                steps = songCacheValues.size - 2,
                                valueRange = 0f..(songCacheValues.size - 1).toFloat()
                            )
                            LinearProgressIndicator(
                                progress = { playerCacheProgress },
                                modifier = Modifier.fillMaxWidth(),
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.padding(2.dp))
                            Text(
                                text = if (maxSongCacheSize == -1) {
                                    formatFileSize(playerCacheSize)
                                } else {
                                    "${formatFileSize(playerCacheSize)} / ${
                                        formatFileSize(
                                            maxSongCacheSize * 1024 * 1024L
                                        )
                                    }"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                             )
                        }
                    }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.clear_all),
                    title = { Text(stringResource(R.string.clear_song_cache)) },
                    onClick = {
                        clearCacheDialog = true
                    }
                )
            )
        )

        ExpressiveSettingGroup(
            title = stringResource(R.string.image_cache),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.manage_search),
                    title = { Text(stringResource(R.string.max_image_cache_size)) },
                    description = {
                        val imageCacheValues =
                            remember { listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192) }
                        Column {
                            Text(
                                text = when (maxImageCacheSize) {
                                    0 -> stringResource(R.string.disable)
                                    else -> formatFileSize(maxImageCacheSize * 1024 * 1024L)
                                }
                            )
                            Slider(
                                value = imageCacheValues.indexOf(maxImageCacheSize).toFloat(),
                                onValueChange = {
                                    val newValue = imageCacheValues[it.roundToInt()]
                                    val newLimitInBytes = newValue * 1024 * 1024L

                                    if (newLimitInBytes < imageCacheSize) {
                                        cacheUsage = imageCacheSize
                                        cacheType = imageCacheString
                                        onConfirmAction = { onMaxImageCacheSizeChange(newValue) }
                                        showCacheWarningDialog = true
                                    } else {
                                        onMaxImageCacheSizeChange(newValue)
                                    }
                                },
                                steps = imageCacheValues.size - 2,
                                valueRange = 0f..(imageCacheValues.size - 1).toFloat()
                            )
                            LinearProgressIndicator(
                                progress = { imageCacheProgress },
                                modifier = Modifier.fillMaxWidth(),
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.padding(2.dp))
                            Text(
                                text = "${formatFileSize(imageCacheSize)} / ${
                                    formatFileSize(
                                        maxImageCacheSize * 1024 * 1024L
                                    )
                                }",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.clear_all),
                    title = { Text(stringResource(R.string.clear_image_cache)) },
                    onClick = {
                        android.util.Log.d("StorageSettings", "Clear image cache button clicked!")
                        clearImageCacheDialog = true
                    }
                )
            )
        )
        Spacer(Modifier.padding(bottom = 30.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.storage)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}
