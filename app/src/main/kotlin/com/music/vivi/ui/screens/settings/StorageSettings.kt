package com.music.vivi.ui.screens.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.annotation.ExperimentalCoilApi
import coil3.imageLoader
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.MaxImageCacheSizeKey
import com.music.vivi.constants.MaxSongCacheSizeKey
import com.music.vivi.extensions.tryOrNull
import com.music.vivi.ui.component.ActionPromptDialog
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.ListPreference
import com.music.vivi.ui.component.PreferenceEntry
import com.music.vivi.ui.component.PreferenceGroupTitle
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.formatFileSize
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
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

    // Calculate total used space
    val totalUsedSpace = imageCacheSize + playerCacheSize + downloadCacheSize
    val totalMaxSpace = imageDiskCache.maxSize + (maxSongCacheSize * 1024 * 1024L)
    val usageProgress = (totalUsedSpace.toFloat() / totalMaxSpace).coerceIn(0f, 1f)

    // Connect scroll behavior to the LazyColumn
    val scrollState = rememberLazyListState()
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                scrollBehavior.state.contentOffset = scrollState.firstVisibleItemScrollOffset.toFloat()
                return Offset.Zero
            }
        }
    }

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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = scrollState, // Connect scroll state
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .nestedScroll(nestedScrollConnection), // Connect nested scroll
            contentPadding = PaddingValues(top = 100.dp, bottom = 32.dp, start = 24.dp, end = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Large Storage text that scrolls with content
            item {
                Text(
                    text = "Storage",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            // Main Storage Overview - No Card, Just Direct Content
            item {
                Column(
                    modifier = Modifier.padding(top = 24.dp, bottom = 32.dp)
                ) {
                    // Large storage number
                    Text(
                        text = formatFileSize(totalUsedSpace).split(" ")[0],
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Light,
                            lineHeight = 72.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${formatFileSize(totalUsedSpace).split(" ")[1]} used",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${formatFileSize(totalMaxSpace)} total",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { usageProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            // Free up space - Single rounded card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .clickable {
                            // Clear all caches at once
                            coroutineScope.launch(Dispatchers.IO) {
                                // Clear downloads
                                downloadCache.keys.forEach { key ->
                                    downloadCache.removeResource(key)
                                }
                                // Clear song cache
                                playerCache.keys.forEach { key ->
                                    playerCache.removeResource(key)
                                }
                                // Clear image cache
                                imageDiskCache.clear()
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.clear_all),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Free up space",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Clear all downloads, song cache, and image cache",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Storage categories - Direct list items, no cards
            item {
                StorageCategoryItem(
                    icon = R.drawable.media_session_service_notification_ic_music_note,
                    title = "Downloaded Songs",
                    size = formatFileSize(downloadCacheSize),
                    progress = if (totalUsedSpace > 0) (downloadCacheSize.toFloat() / totalUsedSpace) else 0f,
                    onClick = { clearDownloads = true }
                )
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    thickness = 1.dp
                )
            }

            item {
                StorageCategoryItem(
                    icon = R.drawable.media_session_service_notification_ic_music_note,
                    title = "Song Cache",
                    size = formatFileSize(playerCacheSize),
                    progress = if (totalUsedSpace > 0) (playerCacheSize.toFloat() / totalUsedSpace) else 0f,
                    onClick = { clearCacheDialog = true }
                )
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    thickness = 1.dp
                )
            }

            item {
                StorageCategoryItem(
                    icon = R.drawable.media_session_service_notification_ic_music_note,
                    title = "Image Cache",
                    size = formatFileSize(imageCacheSize),
                    progress = if (totalUsedSpace > 0) (imageCacheSize.toFloat() / totalUsedSpace) else 0f,
                    onClick = { clearImageCacheDialog = true }
                )
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    thickness = 1.dp
                )
            }

            // Grouped cache settings in a card-like design with dividers
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Cache Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Song Cache
                    ListPreference(
                        title = { Text("Max Song Cache Size") },
                        selectedValue = maxSongCacheSize,
                        values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, -1),
                        valueText = {
                            when (it) {
                                0 -> "Disabled"
                                -1 -> "Unlimited"
                                else -> formatFileSize(it * 1024 * 1024L)
                            }
                        },
                        onValueSelected = onMaxSongCacheSizeChange,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Image Cache
                    ListPreference(
                        title = { Text("Max Image Cache Size") },
                        selectedValue = maxImageCacheSize,
                        values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192),
                        valueText = {
                            when (it) {
                                0 -> "Disabled"
                                else -> formatFileSize(it * 1024 * 1024L)
                            }
                        },
                        onValueSelected = onMaxImageCacheSizeChange,
                    )
                }
            }
        }

        TopAppBar(
            title = { },
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
            },
            scrollBehavior = scrollBehavior,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    // Dialogs remain the same
    if (clearDownloads) {
        ActionPromptDialog(
            title = "Clear All Downloads",
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
                Text(text = "This will clear all downloaded songs.")
            }
        )
    }

    if (clearCacheDialog) {
        ActionPromptDialog(
            title = "Clear Song Cache",
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
                Text(text = "This will clear all cached songs.")
            }
        )
    }

    if (clearImageCacheDialog) {
        ActionPromptDialog(
            title = "Clear Image Cache",
            onDismiss = { clearImageCacheDialog = false },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    imageDiskCache.clear()
                }
                clearImageCacheDialog = false
            },
            onCancel = { clearImageCacheDialog = false },
            content = {
                Text(text = "This will clear all cached images.")
            }
        )
    }
}

@Composable
private fun StorageCategoryItem(
    @DrawableRes icon: Int,
    title: String,
    size: String,
    progress: Float,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(20.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = size,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (progress > 0f) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .padding(start = 44.dp), // Align with text
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun ActionPromptDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    content()
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onConfirm) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}