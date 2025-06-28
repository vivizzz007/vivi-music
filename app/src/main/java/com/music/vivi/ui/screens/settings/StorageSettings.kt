package com.music.vivi.ui.screens.settings

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.music.vivi.BuildConfig
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.MaxImageCacheSizeKey
import com.music.vivi.constants.MaxSongCacheSizeKey
import com.music.vivi.extensions.tryOrNull
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.ListPreference
import com.music.vivi.ui.component.PreferenceEntry
import com.music.vivi.ui.component.PreferenceGroupTitle
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.formatFileSize
import com.music.vivi.utils.TranslationHelper
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@SuppressLint("PrivateResource")
@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
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
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(key = MaxImageCacheSizeKey, defaultValue = 512)
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(key = MaxSongCacheSizeKey, defaultValue = 1024)

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

    var imageCacheSize by remember {
        mutableLongStateOf(imageDiskCache.size)
    }
    var playerCacheSize by remember {
        mutableLongStateOf(tryOrNull { playerCache.cacheSpace } ?: 0)
    }
    var downloadCacheSize by remember {
        mutableLongStateOf(tryOrNull { downloadCache.cacheSpace } ?: 0)
    }

    var showClearAllDownloadsDialog by remember {
        mutableStateOf(false)
    }

    var showClearSongCacheDialog by remember {
        mutableStateOf(false)
    }

    var showClearImagesCacheDialog by remember {
        mutableStateOf(false)
    }

    var showClearTranslationModels by remember {
        mutableStateOf(false)
    }

    if (showClearAllDownloadsDialog) {
        DefaultDialog(
            onDismiss = { showClearAllDownloadsDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.clear_all_downloads_dialog),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showClearAllDownloadsDialog = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showClearAllDownloadsDialog = false
                        coroutineScope.launch(Dispatchers.IO) {
                            downloadCache.keys.forEach { key ->
                                downloadCache.removeResource(key)
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    if (showClearSongCacheDialog) {
        DefaultDialog(
            onDismiss = { showClearSongCacheDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.clear_song_cache_dialog),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showClearSongCacheDialog = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showClearSongCacheDialog = false
                        coroutineScope.launch(Dispatchers.IO) {
                            downloadCache.keys.forEach { key ->
                                downloadCache.removeResource(key)
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    if (showClearImagesCacheDialog) {
        DefaultDialog(
            onDismiss = { showClearImagesCacheDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.clear_images_cache_dialog),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showClearImagesCacheDialog = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showClearImagesCacheDialog = false
                        coroutineScope.launch(Dispatchers.IO) {
                            downloadCache.keys.forEach { key ->
                                downloadCache.removeResource(key)
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    if (showClearTranslationModels) {
        DefaultDialog(
            onDismiss = { showClearTranslationModels = false },
            content = {
                Text(
                    text = stringResource(R.string.clear_translation_models),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showClearTranslationModels = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showClearTranslationModels = false
                        coroutineScope.launch(Dispatchers.IO) {
                            TranslationHelper.clearModels()
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
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

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))

        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            // Lottie Animation in Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.storage))
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        }

        PreferenceGroupTitle(title = stringResource(R.string.downloaded_songs))

        // Downloaded Songs Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.size_used, formatFileSize(downloadCacheSize)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                PreferenceEntry(
                    title = { Text(stringResource(R.string.clear_all_downloads)) },
                    onClick = { showClearAllDownloadsDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        PreferenceGroupTitle(title = stringResource(R.string.song_cache))

        // Song Cache Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (maxSongCacheSize != 0) {
                    if (maxSongCacheSize == -1) {
                        Text(
                            text = stringResource(R.string.size_used, formatFileSize(playerCacheSize)),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { (playerCacheSize.toFloat() / (maxSongCacheSize * 1024 * 1024L)).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )

                        Text(
                            text = stringResource(R.string.size_used, "${formatFileSize(playerCacheSize)} / ${formatFileSize(maxSongCacheSize * 1024 * 1024L)}"),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                ListPreference(
                    title = { Text(stringResource(R.string.max_cache_size)) },
                    selectedValue = maxSongCacheSize,
                    values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, -1),
                    valueText = {
                        when (it) {
                            0 -> stringResource(R.string.off)
                            -1 -> stringResource(R.string.unlimited)
                            else -> formatFileSize(it * 1024 * 1024L)
                        }
                    },
                    onValueSelected = onMaxSongCacheSizeChange,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                PreferenceEntry(
                    title = { Text(stringResource(R.string.clear_song_cache)) },
                    onClick = { showClearSongCacheDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        PreferenceGroupTitle(title = stringResource(R.string.image_cache))

        // Image Cache Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (maxImageCacheSize != 0) {
                    if (maxImageCacheSize == -1) {
                        Text(
                            text = stringResource(R.string.size_used, formatFileSize(imageCacheSize)),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { (imageCacheSize.toFloat() / (maxImageCacheSize * 1024 * 1024L)).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )

                        Text(
                            text = stringResource(
                                R.string.size_used,
                                "${formatFileSize(imageCacheSize)} / ${formatFileSize(maxImageCacheSize * 1024 * 1024L)}"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                ListPreference(
                    title = { Text(stringResource(R.string.max_cache_size)) },
                    selectedValue = maxImageCacheSize,
                    values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, -1),
                    valueText = {
                        when (it) {
                            0 -> stringResource(R.string.off)
                            -1 -> stringResource(R.string.unlimited)
                            else -> formatFileSize(it * 1024 * 1024L)
                        }
                    },
                    onValueSelected = onMaxImageCacheSizeChange,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                PreferenceEntry(
                    title = { Text(stringResource(R.string.clear_image_cache)) },
                    onClick = { showClearImagesCacheDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (BuildConfig.FLAVOR != "foss") {
            PreferenceGroupTitle(title = stringResource(R.string.translation_models))

            // Translation Models Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.clear_translation_models)) },
                    onClick = { showClearTranslationModels = true },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.storage)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}
