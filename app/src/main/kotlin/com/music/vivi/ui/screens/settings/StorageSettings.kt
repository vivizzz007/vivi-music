package com.music.vivi.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.annotation.ExperimentalCoilApi
import coil3.imageLoader
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.MaxImageCacheSizeKey
import com.music.vivi.constants.MaxSongCacheSizeKey
import com.music.vivi.constants.SettingsShapeColorTertiaryKey
import com.music.vivi.extensions.tryOrNull
import com.music.vivi.ui.component.ActionPromptDialog
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.RoundedCheckbox
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.formatFileSize
import com.music.vivi.update.settingstyle.Material3ExpressiveSettingsGroup
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Screen for managing app storage usage.
 * Displays storage breakdown (downloads, song cache, image cache) and allows clearing caches.
 */
@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StorageSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: (() -> Unit)? = null,
) {
    val (settingsShapeTertiary, _) = rememberPreference(SettingsShapeColorTertiaryKey, false)
    val (darkMode, _) = rememberEnumPreference(
        DarkModeKey,
        defaultValue = DarkMode.AUTO
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkMode, isSystemInDarkTheme) {
        if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
    }

    val (iconBgColor, iconStyleColor) = if (settingsShapeTertiary) {
        if (useDarkTheme) {
            Pair(
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.onTertiary
            )
        } else {
            Pair(
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    } else {
        Pair(
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.primary
        )
    }

    val context = LocalContext.current
    val imageDiskCache = context.imageLoader.diskCache ?: return
    val playerCache = LocalPlayerConnection.current?.service?.playerCache ?: return
    val downloadCache = LocalPlayerConnection.current?.service?.downloadCache ?: return

    val coroutineScope = rememberCoroutineScope()
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(
        key = MaxImageCacheSizeKey,
        defaultValue = 512
    )
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(
        key = MaxSongCacheSizeKey,
        defaultValue = 1024
    )
    var clearCacheDialog by remember { mutableStateOf(false) }
    var clearDownloads by remember { mutableStateOf(false) }
    var clearImageCacheDialog by remember { mutableStateOf(false) }

    var imageCacheSize by remember {
        mutableStateOf(imageDiskCache.size)
    }
    var playerCacheSize by remember {
        mutableStateOf(tryOrNull { playerCache.cacheSpace } ?: 0)
    }
    var downloadCacheSize by remember {
        mutableStateOf(tryOrNull { downloadCache.cacheSpace } ?: 0)
    }
    val imageCacheProgress by animateFloatAsState(
        targetValue = (imageCacheSize.toFloat() / imageDiskCache.maxSize).coerceIn(0f, 1f),
        label = "imageCacheProgress"
    )
    val playerCacheProgress by animateFloatAsState(
        targetValue = (playerCacheSize.toFloat() / (maxSongCacheSize * 1024 * 1024L)).coerceIn(
            0f,
            1f
        ),
        label = "playerCacheProgress"
    )

    // Calculate total storage usage
    val totalStorageUsed = downloadCacheSize + playerCacheSize + imageCacheSize
    val totalStorageAvailable = (maxSongCacheSize * 1024 * 1024L) + imageDiskCache.maxSize + downloadCacheSize
    val totalStorageProgress by animateFloatAsState(
        targetValue = (totalStorageUsed.toFloat() / totalStorageAvailable).coerceIn(0f, 1f),
        label = "totalStorageProgress"
    )

    LaunchedEffect(maxImageCacheSize) {
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

    val scrollState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { /* Text(stringResource(R.string.storage)) */ },
                    navigationIcon = {
                        IconButton(
                            onClick = { onBack?.invoke() ?: navController.navigateUp() },
                            onLongClick = navController::backToMain
                        ) {
                            Icon(
                                painterResource(R.drawable.arrow_back),
                                contentDescription = null
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 32.dp
                )
            ) {
                item {
                    Spacer(modifier = Modifier.height(25.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.storage_settings),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.manage_storage_cache),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Total Storage Overview Section
                item {
                    Text(
                        text = stringResource(R.string.storage_overview),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = listOf {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.total_storage_used),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${formatFileSize(
                                            totalStorageUsed
                                        )} / ${formatFileSize(totalStorageAvailable)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Total Storage Progress Bar
                                LinearProgressIndicator(
                                    progress = { totalStorageProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Storage Breakdown
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StorageBreakdownItem(
                                        title = stringResource(R.string.downloads_section),
                                        size = downloadCacheSize,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    StorageBreakdownItem(
                                        title = stringResource(R.string.song_cache_section),
                                        size = playerCacheSize,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    StorageBreakdownItem(
                                        title = stringResource(R.string.image_cache_section),
                                        size = imageCacheSize,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    )
                }

                // Downloaded Songs Section
                item {
                    Text(
                        text = stringResource(R.string.downloads_section),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = listOf(
                            {
                                // Downloads Info
                                ModernInfoItem(
                                    icon = {
                                        Icon(
                                            painterResource(R.drawable.download),
                                            null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    title = stringResource(R.string.downloaded_songs_title),
                                    subtitle = "${formatFileSize(downloadCacheSize)} used",
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            },
                            {
                                // Clear All Downloads
                                ModernInfoItem(
                                    icon = {
                                        Icon(
                                            painterResource(R.drawable.clear_all),
                                            null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    title = stringResource(R.string.clear_all_downloads),
                                    subtitle = stringResource(R.string.remove_all_downloaded_songs),
                                    onClick = { clearDownloads = true },
                                    showArrow = true,
                                    showSettingsIcon = true,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )

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
                            }
                        )
                    )
                }

                // Song Cache Section
                item {
                    Text(
                        text = stringResource(R.string.song_cache_section),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    var showCacheSizeDialog by remember { mutableStateOf(false) }
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = listOf(
                            {
                                // Cache Size Info with Progress Bar
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(R.string.cache_usage),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (maxSongCacheSize == -1) {
                                                formatFileSize(playerCacheSize)
                                            } else {
                                                "${formatFileSize(playerCacheSize)} / ${formatFileSize(
                                                    maxSongCacheSize * 1024 * 1024L
                                                )}"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (maxSongCacheSize != 0 && maxSongCacheSize != -1) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            progress = { playerCacheProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            },
                            {
                                // Max Cache Size
                                ModernInfoItem(
                                    icon = {
                                        Icon(painterResource(R.drawable.storage), null, modifier = Modifier.size(22.dp))
                                    },
                                    title = stringResource(R.string.max_cache_size),
                                    subtitle = when (maxSongCacheSize) {
                                        0 -> stringResource(R.string.disable)
                                        -1 -> stringResource(R.string.unlimited)
                                        else -> formatFileSize(maxSongCacheSize * 1024 * 1024L)
                                    },
                                    onClick = { showCacheSizeDialog = true },
                                    showArrow = true,
                                    showSettingsIcon = true,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )

                                if (showCacheSizeDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showCacheSizeDialog = false },
                                        title = { Text(stringResource(R.string.max_cache_size)) },
                                        text = {
                                            Column {
                                                listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, -1).forEach { value ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                onMaxSongCacheSizeChange(value)
                                                                showCacheSizeDialog = false
                                                            }
                                                            .padding(vertical = 12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        RoundedCheckbox(
                                                            checked = value == maxSongCacheSize,
                                                            onCheckedChange = {
                                                                onMaxSongCacheSizeChange(value)
                                                                showCacheSizeDialog = false
                                                            }
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = when (value) {
                                                                0 -> stringResource(R.string.disable)
                                                                -1 -> stringResource(R.string.unlimited)
                                                                else -> formatFileSize(value * 1024 * 1024L)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(onClick = { showCacheSizeDialog = false }) {
                                                Text(stringResource(R.string.cancel))
                                            }
                                        }
                                    )
                                }
                            },
                            {
                                // Clear Song Cache
                                ModernInfoItem(
                                    icon = {
                                        Icon(painterResource(R.drawable.delete), null, modifier = Modifier.size(22.dp))
                                    },
                                    title = stringResource(R.string.clear_song_cache),
                                    subtitle = stringResource(R.string.free_up_cached_song_data),
                                    onClick = { clearCacheDialog = true },
                                    showArrow = true,
                                    showSettingsIcon = true,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )

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
                            }
                        )
                    )
                }

                // Image Cache Section
                item {
                    Text(
                        text = stringResource(R.string.image_cache_section),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    var showImageCacheSizeDialog by remember { mutableStateOf(false) }
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = listOf(
                            {
                                // Image Cache Info with Progress Bar
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(R.string.cache_usage),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${formatFileSize(
                                                imageCacheSize
                                            )} / ${formatFileSize(imageDiskCache.maxSize)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (maxImageCacheSize > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            progress = { imageCacheProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            },
                            {
                                // Max Image Cache Size
                                ModernInfoItem(
                                    icon = {
                                        Icon(
                                            painterResource(R.drawable.music_note),
                                            null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    title = stringResource(R.string.max_cache_size),
                                    subtitle = when (maxImageCacheSize) {
                                        0 -> stringResource(R.string.disable)
                                        else -> formatFileSize(maxImageCacheSize * 1024 * 1024L)
                                    },
                                    onClick = { showImageCacheSizeDialog = true },
                                    showArrow = true,
                                    showSettingsIcon = true,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )

                                if (showImageCacheSizeDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showImageCacheSizeDialog = false },
                                        title = { Text(stringResource(R.string.max_cache_size)) },
                                        text = {
                                            Column {
                                                listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192).forEach { value ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                onMaxImageCacheSizeChange(value)
                                                                showImageCacheSizeDialog = false
                                                            }
                                                            .padding(vertical = 12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        RoundedCheckbox(
                                                            checked = value == maxImageCacheSize,
                                                            onCheckedChange = {
                                                                onMaxImageCacheSizeChange(value)
                                                                showImageCacheSizeDialog = false
                                                            }
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = when (value) {
                                                                0 -> stringResource(R.string.disable)
                                                                else -> formatFileSize(value * 1024 * 1024L)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(onClick = { showImageCacheSizeDialog = false }) {
                                                Text(stringResource(R.string.cancel))
                                            }
                                        }
                                    )
                                }
                            },
                            {
                                // Clear Image Cache
                                ModernInfoItem(
                                    icon = {
                                        Icon(painterResource(R.drawable.delete), null, modifier = Modifier.size(22.dp))
                                    },
                                    title = stringResource(R.string.clear_image_cache),
                                    subtitle = stringResource(R.string.free_up_cached_images),
                                    onClick = { clearImageCacheDialog = true },
                                    showArrow = true,
                                    showSettingsIcon = true,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )

                                if (clearImageCacheDialog) {
                                    ActionPromptDialog(
                                        title = stringResource(R.string.clear_image_cache),
                                        onDismiss = { clearImageCacheDialog = false },
                                        onConfirm = {
                                            coroutineScope.launch(Dispatchers.IO) {
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
                            }
                        )
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun StorageBreakdownItem(title: String, size: Long, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = formatFileSize(size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
