/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.vivimusic.updater


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.music.vivi.BuildConfig
import com.music.vivi.R
import com.music.vivi.vivimusic.updater.downloadmanager.UpdateDownloadWorker
import com.music.vivi.vivimusic.updater.downloadmanager.DownloadNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.component.ErrorSnackbar
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.foundation.layout.aspectRatio
import coil3.compose.AsyncImage
import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.view.ViewGroup
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.text.AnnotatedString



@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<ViviUpdateStatus>(ViviUpdateStatus.NoUpdate(BuildConfig.VERSION_NAME)) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var isDownloadComplete by remember { mutableStateOf(false) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val currentVersion = BuildConfig.VERSION_NAME
    val autoUpdateCheckEnabled = getAutoUpdateCheckSetting(context)

    LaunchedEffect(Unit) {
        DownloadNotificationManager.initialize(context)
    }

    // Observe WorkManager for download progress
    LaunchedEffect(Unit) {
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData("update_download")
            .observeForever { workInfos ->
                val workInfo = workInfos?.firstOrNull() ?: return@observeForever

                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        isDownloading = true
                        downloadProgress = workInfo.progress.getFloat("progress", 0f)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        isDownloading = false
                        isDownloadComplete = true
                        val filePath = workInfo.outputData.getString("file_path")
                        if (filePath != null) {
                            downloadedFile = File(filePath)
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        isDownloading = false
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.download_failed))
                        }
                    }
                    WorkInfo.State.CANCELLED -> {
                        isDownloading = false
                        downloadProgress = 0f
                    }
                    else -> {}
                }
            }
    }

    // Check if downloaded file still exists
    LaunchedEffect(isDownloadComplete, downloadedFile) {
        if (isDownloadComplete && downloadedFile != null) {
            if (!downloadedFile!!.exists()) {
                isDownloadComplete = false
                downloadedFile = null
                downloadProgress = 0f
            }
        }
    }

    fun triggerUpdateCheck() {
        status = ViviUpdateStatus.Checking
        scope.launch {
            // Add a small delay for visual feedback as in Med
            delay(1000L)
            checkForUpdate(
                context = context,
                onSuccess = { tag, isAvailable, changelog, size, date, description, imageUrl, apkUrl, origin ->
                    saveLastCheckedTime(context, LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a")))
                    saveUpdateAvailableState(context, isAvailable)
                    if (origin != null) saveUpdateOrigin(context, origin)
                    status = if (isAvailable) {
                        ViviUpdateStatus.Available(
                            version = tag,
                            changelog = changelog,
                            size = size,
                            releaseDate = date,
                            description = description,
                            imageUrl = imageUrl,
                            apkUrl = apkUrl,
                            updateOrigin = origin
                        )
                    } else {
                        ViviUpdateStatus.NoUpdate(tag)
                    }
                },
                onError = {
                    status = ViviUpdateStatus.Error(context.getString(R.string.cant_check_updates))
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (autoUpdateCheckEnabled) {
            triggerUpdateCheck()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Icon(
                        painter = painterResource(R.drawable.mobile_update),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
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
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val currentStatus = status) {
                        is ViviUpdateStatus.Idle, is ViviUpdateStatus.Checking, is ViviUpdateStatus.NoUpdate, is ViviUpdateStatus.Error -> {
                            val btnText = if (currentStatus is ViviUpdateStatus.Error) stringResource(R.string.try_again) else stringResource(R.string.check_for_update)
                            Button(
                                onClick = { triggerUpdateCheck() },
                                enabled = currentStatus !is ViviUpdateStatus.Checking && !isDownloading,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Text(
                                    text = btnText,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }

                        is ViviUpdateStatus.Available -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (!isDownloading || isDownloadComplete) {
                                    Button(
                                        onClick = {
                                            if (isDownloadComplete) {
                                                val file = downloadedFile
                                                if (file == null || !file.exists()) {
                                                    isDownloadComplete = false
                                                    downloadedFile = null
                                                    downloadProgress = 0f
                                                    return@Button
                                                }
                                                file.let { f ->
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        if (!context.packageManager.canRequestPackageInstalls()) {
                                                            val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                                                data = Uri.parse("package:${context.packageName}")
                                                            }
                                                            context.startActivity(intent)
                                                            return@let
                                                        }
                                                    }
                                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)
                                                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(uri, "application/vnd.android.package-archive")
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    ContextCompat.startActivity(context, installIntent, null)
                                                }
                                            } else {
                                                val targetApkName = if (BuildConfig.FLAVOR.contains("foss", ignoreCase = true)) "izzydroid-universal-foss-release.apk" else "vivi.apk"
                                                val urlToDownload = currentStatus.apkUrl ?: "https://github.com/pwpp08/vivi-music/releases/download/${currentStatus.version}/$targetApkName"
                                                
                                                if (BuildConfig.FLAVOR.contains("foss", ignoreCase = true)) {
                                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToDownload))
                                                    ContextCompat.startActivity(context, browserIntent, null)
                                                } else {
                                                    val downloadRequest = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
                                                        .setInputData(workDataOf("apk_url" to urlToDownload, "version" to currentStatus.version, "file_size" to currentStatus.size))
                                                        .addTag("update_download")
                                                        .build()
                                                    WorkManager.getInstance(context).enqueueUniqueWork("update_download", ExistingWorkPolicy.REPLACE, downloadRequest)
                                                    isDownloading = true
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(56.dp)
                                    ) {
                                        Text(
                                            text = if (isDownloadComplete) stringResource(R.string.install) else "Download and install",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (isDownloading && !isDownloadComplete) {
                                            WorkManager.getInstance(context).cancelUniqueWork("update_download")
                                            isDownloading = false
                                        } else {
                                            navController.navigateUp()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                ) {
                                    Text(
                                        text = if (isDownloading && !isDownloadComplete) "Pause" else stringResource(R.string.later),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { ErrorSnackbar(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .widthIn(max = 700.dp)
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    val titleText = when (status) {
                        is ViviUpdateStatus.Checking -> "Checking for update..."
                        is ViviUpdateStatus.Available -> if (isDownloading) "Installing system update..." else "System update\navailable"
                        is ViviUpdateStatus.NoUpdate -> "Your app is up to date"
                        is ViviUpdateStatus.Error -> "Update error"
                        else -> "System update"
                    }
                    Text(
                        text = titleText,
                        style = if (status is ViviUpdateStatus.Checking || status is ViviUpdateStatus.Error) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    )
                    
                    val currentStatus = status
                    if (currentStatus is ViviUpdateStatus.Available) {
                        val rawVersion = currentStatus.version
                        val displayVer = when {
                            rawVersion.startsWith("b", ignoreCase = true) -> rawVersion.uppercase()
                            rawVersion.startsWith("v", ignoreCase = true) -> rawVersion.uppercase()
                            else -> "V${rawVersion.uppercase()}"
                        }
                        Text(
                            text = "VIVI MUSIC $displayVer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Size: ${currentStatus.size} MB",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )
                    }
                }

                if (status is ViviUpdateStatus.Checking) {
                    item {
                        val strokeWidthPx = with(LocalDensity.current) { 6.dp.toPx() }
                        val customStroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearWavyProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                stroke = customStroke,
                                trackStroke = customStroke
                            )
                        }
                    }
                } else if (status is ViviUpdateStatus.Available && isDownloading) {
                    item {
                        val strokeWidthPx = with(LocalDensity.current) { 6.dp.toPx() }
                        val customStroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearWavyProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                progress = { downloadProgress },
                                stroke = customStroke,
                                trackStroke = customStroke
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Downloading and installing update",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    val contentModifier = Modifier.fillMaxWidth()

                    Box(
                        modifier = contentModifier,
                        contentAlignment = Alignment.TopStart
                    ) {
                        AnimatedContent(
                            targetState = status,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                            },
                            label = "statusTransition"
                        ) { currentStatus ->
                            when (currentStatus) {
                                is ViviUpdateStatus.Checking -> {
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(
                                            text = "Please wait while we check for the latest features.",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                is ViviUpdateStatus.NoUpdate -> {
                                    val checkTime = remember { LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }
                                    Column(
                                        horizontalAlignment = Alignment.Start,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val rawVersion = currentStatus.version
                                        val displayVer = when {
                                            rawVersion.startsWith("b", ignoreCase = true) -> rawVersion.uppercase()
                                            rawVersion.startsWith("v", ignoreCase = true) -> rawVersion.uppercase()
                                            else -> "V${rawVersion.uppercase()}"
                                        }
                                        Text(
                                            text = "VIVI MUSIC VERSION $displayVer",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Build version: ${BuildConfig.VERSION_CODE}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Text(
                                            text = "Last successful check for update:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = checkTime,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Text(
                                            text = "\"Donations help make project development & updates faster!\"",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontStyle = FontStyle.Italic
                                        )
                                    }
                                }

                                is ViviUpdateStatus.Error -> {
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(
                                            text = currentStatus.message,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                is ViviUpdateStatus.Available -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                         currentStatus.updateOrigin?.let { originText ->
                                             Surface(
                                                 color = MaterialTheme.colorScheme.primaryContainer,
                                                 shape = RoundedCornerShape(12.dp),
                                                 modifier = Modifier.padding(bottom = 12.dp)
                                             ) {
                                                 Row(
                                                     verticalAlignment = Alignment.CenterVertically,
                                                     modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                 ) {
                                                     Icon(
                                                         painter = painterResource(R.drawable.info),
                                                         contentDescription = null,
                                                         tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                         modifier = Modifier.size(16.dp)
                                                     )
                                                     Spacer(modifier = Modifier.size(6.dp))
                                                     Text(
                                                         text = originText,
                                                         style = MaterialTheme.typography.labelMedium,
                                                         fontWeight = FontWeight.SemiBold,
                                                         color = MaterialTheme.colorScheme.onPrimaryContainer
                                                     )
                                                 }
                                             }
                                         }
                                         if (!currentStatus.imageUrl.isNullOrBlank()) {
                                             AsyncImage(
                                                 model = currentStatus.imageUrl,
                                                 contentDescription = null,
                                                 modifier = Modifier
                                                     .fillMaxWidth()
                                                     .height(200.dp)
                                                     .clip(RoundedCornerShape(24.dp)),
                                                 contentScale = ContentScale.Crop
                                             )
                                             Spacer(modifier = Modifier.height(16.dp))
                                         }
                                         val desc = currentStatus.description ?: ""
                                         if (desc.isNotBlank()) {
                                             val primaryColor = MaterialTheme.colorScheme.primary
                                             val annotatedText = desc.trim().parseMarkdownAndUrls(primaryColor)
                                            ClickableText(
                                                text = annotatedText,
                                                onClick = { offset ->
                                                    annotatedText.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                                                        ContextCompat.startActivity(context, Intent(Intent.ACTION_VIEW, Uri.parse(it.item)), null)
                                                    }
                                                },
                                                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                        if (currentStatus.changelog.isNotEmpty()) {
                                            currentStatus.changelog.forEach { section ->
                                                if (section.title.isNotBlank()) {
                                                    Text(
                                                        text = section.title,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onBackground,
                                                        modifier = Modifier.padding(bottom = 16.dp)
                                                    )
                                                }
                                                if (!section.description.isNullOrBlank()) {
                                                    val primaryColor = MaterialTheme.colorScheme.primary
                                                    val annotatedText = section.description.trim().parseMarkdownAndUrls(primaryColor)
                                                    ClickableText(
                                                        text = annotatedText,
                                                        onClick = { offset ->
                                                            annotatedText.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                                                                ContextCompat.startActivity(context, Intent(Intent.ACTION_VIEW, Uri.parse(it.item)), null)
                                                            }
                                                        },
                                                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                                    )
                                                }
                                                section.items.filter { it.isNotBlank() }.forEach { item ->
                                                    val primaryColor = MaterialTheme.colorScheme.primary
                                                    val annotatedText = "• ${item.trim()}".parseMarkdownAndUrls(primaryColor)
                                                    ClickableText(
                                                        text = annotatedText,
                                                        onClick = { offset ->
                                                            annotatedText.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                                                                ContextCompat.startActivity(context, Intent(Intent.ACTION_VIEW, Uri.parse(it.item)), null)
                                                            }
                                                        },
                                                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                                    )
                                                }
                                                // Dynamic SDUI blocks rendered beautifully
                                                section.blocks?.filter { b ->
                                                    when (b.type) {
                                                        "image", "video" -> !b.url.isNullOrBlank()
                                                        "row", "column" -> !b.children.isNullOrEmpty()
                                                        else -> true
                                                    }
                                                }?.forEach { block ->
                                                    RenderSDUIBlock(block = block)
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                                is ViviUpdateStatus.Idle -> {}
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}


// Utility functions for SharedPreferences uses now view model
const val PREFS_NAME = "settings"
const val KEY_AUTO_UPDATE_CHECK = "auto_update_check"
const val KEY_LAST_CHECKED_TIME = "last_checked_time"
const val KEY_BETA_UPDATES = "beta_updates"
const val KEY_UPDATE_AVAILABLE = "update_available"
const val KEY_LAST_INSTALLED_VERSION = "last_installed_version"

fun getUpdateAvailableState(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val lastInstalled = sharedPrefs.getString(KEY_LAST_INSTALLED_VERSION, null)
    val currentVersion = BuildConfig.VERSION_NAME
    if (lastInstalled != currentVersion) {
        sharedPrefs.edit()
            .putString(KEY_LAST_INSTALLED_VERSION, currentVersion)
            .putBoolean(KEY_UPDATE_AVAILABLE, false)
            .apply()
        return false
    }
    return sharedPrefs.getBoolean(KEY_UPDATE_AVAILABLE, false)
}

fun saveUpdateAvailableState(context: Context, available: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_UPDATE_AVAILABLE, available).apply()
}

const val KEY_UPDATE_ORIGIN = "update_origin"

fun saveUpdateOrigin(context: Context, origin: String) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putString(KEY_UPDATE_ORIGIN, origin).apply()
}

fun getUpdateOrigin(context: Context): String? {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getString(KEY_UPDATE_ORIGIN, null)
}

fun getAutoUpdateCheckSetting(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_AUTO_UPDATE_CHECK, true)
}

fun saveAutoUpdateCheckSetting(context: Context, enabled: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_AUTO_UPDATE_CHECK, enabled).apply()
}

const val KEY_UPDATE_NOTIFICATIONS = "update_notifications"

fun getUpdateNotificationsSetting(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_UPDATE_NOTIFICATIONS, true)
}

fun saveUpdateNotificationsSetting(context: Context, enabled: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_UPDATE_NOTIFICATIONS, enabled).apply()
}

const val KEY_DOWNLOAD_NOTIFICATIONS = "download_notifications"

fun getDownloadNotificationsSetting(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_DOWNLOAD_NOTIFICATIONS, true)
}

fun saveDownloadNotificationsSetting(context: Context, enabled: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_DOWNLOAD_NOTIFICATIONS, enabled).apply()
}

fun saveLastCheckedTime(context: Context, timestamp: String) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putString(KEY_LAST_CHECKED_TIME, timestamp).apply()
}

fun getLastCheckedTime(context: Context): String {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getString(KEY_LAST_CHECKED_TIME, "") ?: ""
}

fun getBetaUpdatesSetting(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_BETA_UPDATES, false)
}

fun saveBetaUpdatesSetting(context: Context, enabled: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_BETA_UPDATES, enabled).apply()
}

// ──────────────────────────────────────────────────────────────────────────
// 9 PM daily gate for beta/nightly update checks
// ──────────────────────────────────────────────────────────────────────────
const val KEY_LAST_NIGHTLY_CHECK_DAY = "last_nightly_check_day"

/**
 * Returns true only if BOTH conditions are met:
 *  1. The current local time is 9:00 PM (21:00) or later.
 *  2. The nightly update check has NOT already run today.
 *
 * This prevents the nightly CI check from firing on every single app launch.
 * Once it returns true and the check runs, call [markNightlyCheckDone] so
 * subsequent launches today are silently skipped.
 */
fun shouldRunNightlyCheck(context: Context): Boolean {
    val now = java.time.LocalTime.now()
    val ninepm = java.time.LocalTime.of(21, 0)
    if (now.isBefore(ninepm)) return false          // before 9 PM — skip
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val todayEpochDay = java.time.LocalDate.now().toEpochDay()
    val lastCheckedDay = prefs.getLong(KEY_LAST_NIGHTLY_CHECK_DAY, -1L)
    return lastCheckedDay != todayEpochDay           // already ran today — skip
}

/**
 * Records that the nightly check ran today so [shouldRunNightlyCheck] returns
 * false for all remaining launches until midnight.
 */
fun markNightlyCheckDone(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putLong(KEY_LAST_NIGHTLY_CHECK_DAY, java.time.LocalDate.now().toEpochDay())
        .apply()
}

private fun formatGitHubDate(githubDate: String): String = try {
    val githubFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    val displayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a")
    val dateTime = LocalDateTime.parse(githubDate, githubFormatter)
    dateTime.format(displayFormatter)
} catch (e: Exception) {
    githubDate
}

fun parseVersionParts(version: String): List<Int> {
    val clean = version.trim()
        .removePrefix("v")
        .removePrefix("V")
        .removePrefix("b")
        .removePrefix("B")
        .substringBefore("-")
        .substringBefore("+")
        .substringBefore("_")
    return clean.split(".").map { it.toIntOrNull() ?: 0 }
}

fun compareVersionTags(v1: String, v2: String): Int {
    val parts1 = parseVersionParts(v1)
    val parts2 = parseVersionParts(v2)
    val maxParts = maxOf(parts1.size, parts2.size)
    for (i in 0 until maxParts) {
        val p1 = parts1.getOrElse(i) { 0 }
        val p2 = parts2.getOrElse(i) { 0 }
        if (p1 != p2) {
            return p1.compareTo(p2)
        }
    }
    val run1 = Regex("-r(\\d+)").find(v1)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val run2 = Regex("-r(\\d+)").find(v2)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    if (run1 != run2) {
        return run1.compareTo(run2)
    }
    // If numeric parts and run equal: stable (starts with v or number) is considered newer than beta (starts with b)
    val b1 = v1.startsWith("b", ignoreCase = true)
    val b2 = v2.startsWith("b", ignoreCase = true)
    if (b1 != b2) {
        return if (b2) 1 else -1 // stable > beta
    }
    return v1.compareTo(v2, ignoreCase = true)
}

// Robust version comparison: returns true if latestVersion > currentVersion
fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
    return compareVersionTags(latestVersion, currentVersion) > 0
}


// Fetches ALL releases, finds the latest version > current, and returns its info
suspend fun checkForUpdate(
    context: Context,
    onSuccess: (tag: String, isAvailable: Boolean, changelog: List<ChangelogSection>, size: String, date: String, description: String?, imageUrl: String?, apkUrl: String?, updateOrigin: String?) -> Unit,
    onError: () -> Unit,
) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/pwpp08/vivi-music/releases")
            val json = url.openStream().bufferedReader().use { it.readText() }
            val releases = JSONArray(json)
            
            val currentVersion = BuildConfig.VERSION_NAME
            val betaEnabled = getBetaUpdatesSetting(context)


            var isNightlyUpdate = false
            var nightlyRunObject: JSONObject? = null
            val nightlyChangelog = mutableListOf<String>()

            if (betaEnabled) {
                try {
                    val nightlyUrl = URL("https://api.github.com/repos/pwpp08/vivi-music/actions/workflows/nightly.yml/runs?status=success&per_page=100")
                    val nightlyJson = nightlyUrl.openStream().bufferedReader().use { it.readText() }
                    val nightlyData = JSONObject(nightlyJson)
                    val runs = nightlyData.optJSONArray("workflow_runs")
                    if (runs != null && runs.length() > 0) {
                        val firstRun = runs.getJSONObject(0)
                        val runUpdatedAt = firstRun.getString("updated_at")
                        val githubFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                        val runTime = java.time.LocalDateTime.parse(runUpdatedAt, githubFormatter)
                        val runTimeEpoch = runTime.toInstant(java.time.ZoneOffset.UTC).toEpochMilli()

                        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        val currentAppInstallTime = packageInfo.lastUpdateTime

                        val sharedPreferences = context.getSharedPreferences("update_settings", Context.MODE_PRIVATE)
                        if (!BuildConfig.IS_NIGHTLY) {
                            sharedPreferences.edit().remove("last_installed_nightly_run").apply()
                        }
                        val lastInstalledRun = sharedPreferences.getInt("last_installed_nightly_run", -1)
                        val runNumber = firstRun.getInt("run_number")

                        val isNewer = if (lastInstalledRun != -1) {
                            runNumber > lastInstalledRun
                        } else if (!BuildConfig.IS_NIGHTLY) {
                            true
                        } else {
                            runTimeEpoch > (currentAppInstallTime + 300_000)
                        }

                        if (isNewer) {
                            isNightlyUpdate = true
                            nightlyRunObject = firstRun

                            // Collect commits from runs that are newer than the currently installed nightly run
                            for (i in 0 until runs.length()) {
                                val run = runs.getJSONObject(i)
                                val rNum = run.optInt("run_number", -1)
                                val shouldInclude = if (lastInstalledRun != -1) {
                                    rNum > lastInstalledRun
                                } else {
                                    i < 15
                                }
                                if (shouldInclude) {
                                    val headCommit = run.optJSONObject("head_commit")
                                    val commitMessage = headCommit?.optString("message")
                                    if (!commitMessage.isNullOrBlank()) {
                                        val subjectLine = commitMessage.lineSequence().firstOrNull { it.isNotBlank() } ?: commitMessage
                                        nightlyChangelog.add("r$rNum: $subjectLine")
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("UpdateCheck", "Error checking nightly updates: ${e.message}", e)
                }
            }

            if (isNightlyUpdate && nightlyRunObject != null) {
                val runNumber = nightlyRunObject.getInt("run_number")
                val runUpdatedAt = nightlyRunObject.getString("updated_at")
                val displayTag = "nightly-r$runNumber"
                
                val triggeringActor = nightlyRunObject.optJSONObject("triggering_actor")?.optString("login")
                    ?: nightlyRunObject.optJSONObject("actor")?.optString("login") ?: ""
                val event = nightlyRunObject.optString("event")
                val headCommit = nightlyRunObject.optJSONObject("head_commit")
                val commitAuthor = headCommit?.optJSONObject("author")?.optString("name") ?: ""
                val commitMsg = headCommit?.optString("message") ?: ""

                val origin = when {
                    triggeringActor.equals("pwpp08", ignoreCase = true) || commitAuthor.equals("pwpp08", ignoreCase = true) ->
                        "Pushed by you (pwpp08)"
                    event == "pull_request" || commitMsg.contains("pull request", ignoreCase = true) || commitMsg.contains("Merge pull request", ignoreCase = true) ->
                        "Pull request by @${triggeringActor.ifBlank { commitAuthor }.ifBlank { "contributor" }}"
                    else ->
                        "Developer update (@${triggeringActor.ifBlank { "vivi" }})"
                }

                val changelogList = mutableListOf<ChangelogSection>()
                if (nightlyChangelog.isEmpty()) {
                    val headCommitObj = nightlyRunObject.optJSONObject("head_commit")
                    val commitMessage = headCommitObj?.optString("message") ?: "New features and bug fixes"
                    val subjectLine = commitMessage.lineSequence().firstOrNull { it.isNotBlank() } ?: commitMessage
                    nightlyChangelog.add("r$runNumber: $subjectLine")
                }
                changelogList.add(ChangelogSection(context.getString(R.string.changelog), nightlyChangelog))
                
                val formattedReleaseDate = formatGitHubDate(runUpdatedAt)
                val apkDownloadUrl = "https://nightly.link/pwpp08/vivi-music/workflows/nightly.yml/main/vivi-music-gms-nightly.zip"
                
                withContext(Dispatchers.Main) {
                    onSuccess(displayTag, true, changelogList, "~30", formattedReleaseDate, "Bleeding-edge nightly build from main branch.", null, apkDownloadUrl, origin)
                }
                return@withContext
            }

            var bestStableRelease: JSONObject? = null
            var bestOverallRelease: JSONObject? = null

            for (i in 0 until releases.length()) {
                val release = releases.getJSONObject(i)
                val tagName = release.getString("tag_name")
                val isBeta = tagName.startsWith("b")
                
                // Track best stable
                if (!isBeta) {
                    if (bestStableRelease == null || isNewerVersion(tagName, bestStableRelease.getString("tag_name"))) {
                        bestStableRelease = release
                    }
                }
                
                // Track best overall
                if (bestOverallRelease == null || isNewerVersion(tagName, bestOverallRelease.getString("tag_name"))) {
                    bestOverallRelease = release
                }
            }

            // Select the target release based on user preference
            val targetRelease = if (betaEnabled) bestOverallRelease else bestStableRelease

            if (targetRelease != null) {
                val targetTagName = targetRelease.getString("tag_name")
                val isNewer = isNewerVersion(targetTagName, currentVersion)
                
                // Track Switch Logic:
                // If the user has disabled beta updates, we should offer the latest stable release
                // even if it's technically a lower version number than their current beta/custom build.
                // This allows users to correctly "roll back" to the stable track.
                val currentIsBeta = currentVersion.startsWith("b")
                val targetIsStable = targetTagName.startsWith("v")
                
                // Compare version numbers ignoring prefixes
                val currentClean = currentVersion.removePrefix("b").removePrefix("v")
                val targetClean = targetTagName.removePrefix("b").removePrefix("v")
                val isDifferentVersion = currentClean != targetClean
                
                var shouldShow = isNewer
                if (!shouldShow && !betaEnabled) {
                    // Logic: If I'm on a Beta (b5.0.7) and latest stable is v5.0.6, 
                    // and I just turned OFF beta, I want to see v5.0.6.
                    if (currentIsBeta && targetIsStable && currentClean == targetClean) {
                        shouldShow = true
                    }
                }

                if (shouldShow) {
                    val tagWithPrefix = targetRelease.getString("tag_name")
                    val displayTag = tagWithPrefix

                    // FETCH CHANGELOG URL FROM RELEASE ASSETS DYNAMICALLY BASED ON CHANNEL
                    val changelogList = mutableListOf<ChangelogSection>()
                    var description: String? = null
                    var imageUrl: String? = null
                    try {
                        val releaseAssets = targetRelease.optJSONArray("assets")
                        val changelogAssetUrl = if (releaseAssets != null) {
                            (0 until releaseAssets.length()).map { releaseAssets.getJSONObject(it) }
                                .firstOrNull { it.getString("name") == "changelog.json" }
                                ?.optString("browser_download_url")
                        } else null
                        val changelogUrl = if (!changelogAssetUrl.isNullOrEmpty()) {
                            URL("$changelogAssetUrl?t=${System.currentTimeMillis()}")
                        } else {
                            URL("https://github.com/pwpp08/vivi-music/releases/download/$tagWithPrefix/changelog.json?t=${System.currentTimeMillis()}")
                        }
                        val changelogJson = changelogUrl.openStream().bufferedReader().use { it.readText() }
                        val changelogData = JSONObject(changelogJson)

                        description = changelogData.optString("description").takeIf { it.isNotEmpty() }
                        imageUrl = changelogData.optString("image").takeIf { it.isNotEmpty() }

                        val changelogArray = changelogData.getJSONArray("changelog")
                        for (j in 0 until changelogArray.length()) {
                            val sectionObj = changelogArray.getJSONObject(j)
                            val title = sectionObj.optString("title", "")
                            val secDesc = sectionObj.optString("description").takeIf { it.isNotBlank() }
                            val itemsArray = sectionObj.optJSONArray("items")
                            val itemsList = mutableListOf<String>()
                            if (itemsArray != null) {
                                for (k in 0 until itemsArray.length()) {
                                    itemsList.add(itemsArray.getString(k))
                                }
                            }
                            
                            val blocksArray = sectionObj.optJSONArray("blocks")
                            var blocksList: List<SDUIBlock>? = null
                            if (blocksArray != null) {
                                val sduiList = mutableListOf<SDUIBlock>()
                                for (k in 0 until blocksArray.length()) {
                                    sduiList.add(parseSDUIBlock(blocksArray.getJSONObject(k)))
                                }
                                blocksList = sduiList
                            }
                            
                            changelogList.add(ChangelogSection(title, itemsList, secDesc, blocksList))
                        }
                    } catch (e: Exception) {
                        // Fallback: Parse body as markdown release notes
                        val body = targetRelease.optString("body", "").trim()
                        if (body.isNotEmpty()) {
                            val lines = body.lines().map { it.trim() }.filter { it.isNotEmpty() }
                            val bullets = lines.filter { it.startsWith("-") || it.startsWith("*") }
                                .map { it.removePrefix("-").removePrefix("*").trim() }
                            if (bullets.isNotEmpty()) {
                                description = lines.firstOrNull { !it.startsWith("-") && !it.startsWith("*") && !it.startsWith("#") }
                                changelogList.add(ChangelogSection(context.getString(R.string.changelog), bullets))
                            } else {
                                changelogList.add(ChangelogSection(context.getString(R.string.changelog), lines))
                            }
                        } else {
                            changelogList.add(ChangelogSection(context.getString(R.string.changelog), listOf(context.getString(R.string.no_changelog_available))))
                        }
                    }

                    val publishedAt = targetRelease.getString("published_at")
                    val formattedReleaseDate = formatGitHubDate(publishedAt)
                    val assets = targetRelease.getJSONArray("assets")

                    var apkSizeInMB = ""
                    var apkDownloadUrl = ""
                    var fallbackApkSizeInMB = ""
                    var fallbackApkUrl = ""
                    val isFoss = BuildConfig.FLAVOR.contains("foss", ignoreCase = true)
                    val expectedApkName = if (isFoss) "izzydroid-universal-foss-release.apk" else "vivi.apk"
                    val currentFlavor = if (isFoss) "foss" else "gms"

                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        val assetName = asset.getString("name")
                        val lowerName = assetName.lowercase()
                        if (lowerName.endsWith(".apk")) {
                            val apkSizeInBytes = asset.getLong("size")
                            val sizeStr = String.format(java.util.Locale.US, "%.1f", apkSizeInBytes / (1024.0 * 1024.0))
                            val downloadUrl = asset.getString("browser_download_url")

                            if (assetName == expectedApkName || lowerName.contains(currentFlavor) || assetName == "vivi.apk") {
                                apkSizeInMB = sizeStr
                                apkDownloadUrl = downloadUrl
                                break
                            } else if (fallbackApkUrl.isEmpty()) {
                                fallbackApkSizeInMB = sizeStr
                                fallbackApkUrl = downloadUrl
                            }
                        }
                    }

                    if (apkDownloadUrl.isEmpty() && fallbackApkUrl.isNotEmpty()) {
                        apkDownloadUrl = fallbackApkUrl
                        apkSizeInMB = fallbackApkSizeInMB
                    }

                    var origin = "Official update"
                    try {
                        val commitUrl = URL("https://api.github.com/repos/pwpp08/vivi-music/commits?sha=$tagWithPrefix&per_page=1")
                        val commitJson = commitUrl.openStream().bufferedReader().use { it.readText() }
                        val commitArray = JSONArray(commitJson)
                        if (commitArray.length() > 0) {
                            val cObj = commitArray.getJSONObject(0)
                            val cAuthorLogin = cObj.optJSONObject("author")?.optString("login") ?: ""
                            val cCommitterLogin = cObj.optJSONObject("committer")?.optString("login") ?: ""
                            val cAuthorName = cObj.optJSONObject("commit")?.optJSONObject("author")?.optString("name") ?: ""
                            val cMessage = cObj.optJSONObject("commit")?.optString("message") ?: ""

                            origin = when {
                                cAuthorLogin.equals("pwpp08", ignoreCase = true) || cCommitterLogin.equals("pwpp08", ignoreCase = true) || cAuthorName.equals("pwpp08", ignoreCase = true) ->
                                    "Pushed by you (pwpp08)"
                                cMessage.contains("pull request", ignoreCase = true) || cMessage.contains("Merge pull request", ignoreCase = true) ->
                                    "Pull request"
                                else ->
                                    "App developer update"
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("UpdateCheck", "Could not fetch commit origin: ${e.message}")
                        val relAuthor = targetRelease.optJSONObject("author")?.optString("login") ?: ""
                        origin = if (relAuthor.equals("pwpp08", ignoreCase = true)) {
                            "Pushed by you (pwpp08)"
                        } else {
                            "App developer update"
                        }
                    }

                    if (apkDownloadUrl.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            onSuccess(displayTag, true, changelogList, apkSizeInMB, formattedReleaseDate, description, imageUrl, apkDownloadUrl, origin)
                        }
                        return@withContext
                    }
                }
            }

            // No update found or APK missing
            withContext(Dispatchers.Main) {
                onSuccess(currentVersion, false, emptyList(), "", "", null, null, null, null)
            }
        } catch (e: Exception) {
            Log.e("UpdateCheck", "Error checking for updates: ${e.message}", e)
            withContext(Dispatchers.Main) { onError() }
        }
    }
}

// Backwards-compatible overload for 8-arg callers
suspend fun checkForUpdate(
    context: Context,
    onSuccess: (tag: String, isAvailable: Boolean, changelog: List<ChangelogSection>, size: String, date: String, description: String?, imageUrl: String?, apkUrl: String?) -> Unit,
    onError: () -> Unit,
) = checkForUpdate(
    context = context,
    onSuccess = { tag, isAvailable, changelog, size, date, description, imageUrl, apkUrl, _ ->
        onSuccess(tag, isAvailable, changelog, size, date, description, imageUrl, apkUrl)
    },
    onError = onError
)
fun String.extractUrls(): List<Pair<IntRange, String>> {
    val urlPattern = Pattern.compile(
        "(?:^|[\\s])((https?://|www\\.|pic\\.)[\\w-]+(\\.[\\w-]+)+([/?].*)?)"
    )
    val matcher = urlPattern.matcher(this)
    val urlList = mutableListOf<Pair<IntRange, String>>()

    while (matcher.find()) {
        val url = matcher.group(1)?.trim() ?: continue
        val range = IntRange(matcher.start(1), matcher.end(1) - 1)
        // Ensure URL has proper scheme
        val fullUrl = if (url.startsWith("http")) url else "https://$url"
        urlList.add(range to fullUrl)
    }

    return urlList
}

// Helper to dynamically parse robust HTML payloads and raw URLs flawlessly into Compose UI strings
fun String.parseMarkdownAndUrls(primaryColor: Color): AnnotatedString {
    val cleanHtml = this.replace("\n", "<br>") // ensure plain newlines break correctly too just in case
    val spanned = HtmlCompat.fromHtml(cleanHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)
    
    return buildAnnotatedString {
        val plainText = spanned.toString()
        append(plainText)
        
        // Map Native HTML Spans to Compose Spans
        spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            when (span) {
                is StyleSpan -> {
                    when (span.style) {
                        Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                        Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                        Typeface.BOLD_ITALIC -> addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), start, end)
                    }
                }
                is URLSpan -> {
                    addStyle(SpanStyle(color = primaryColor, textDecoration = TextDecoration.Underline), start, end)
                    addStringAnnotation("URL", span.url, start, end)
                }
                is RelativeSizeSpan -> {
                    addStyle(SpanStyle(fontSize = TextUnit(span.sizeChange, TextUnitType.Em)), start, end)
                }
                is ForegroundColorSpan -> {
                    addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
                }
            }
        }
        
        // Safely extract raw text-based URLs that didn't use an <a href> tag natively
        val rawUrls = plainText.extractUrls()
        rawUrls.forEach { (range, url) ->
            addStyle(
                SpanStyle(color = primaryColor, textDecoration = TextDecoration.Underline),
                range.first, range.last + 1
            )
            addStringAnnotation("URL", url, range.first, range.last + 1)
        }
    }
}

// JSON Block parser
fun parseSDUIBlock(json: JSONObject): SDUIBlock {
    val type = json.optString("type", "text")
    val url = json.optString("url").takeIf { it.isNotEmpty() }
    
    var modifierParams: SDUIModifier? = null
    if (json.has("modifier")) {
        val modJson = json.getJSONObject("modifier")
        modifierParams = SDUIModifier(
            weight = if (modJson.has("weight")) modJson.getDouble("weight").toFloat() else null,
            heightDp = if (modJson.has("height")) modJson.getInt("height") else null,
            fillMaxWidth = modJson.optBoolean("fillMaxWidth", false),
            paddingDp = if (modJson.has("padding")) modJson.getInt("padding") else null,
            aspectRatio = if (modJson.has("aspectRatio")) modJson.getDouble("aspectRatio").toFloat() else null
        )
    }
    
    var children: List<SDUIBlock>? = null
    if (json.has("children")) {
        val childrenArray = json.getJSONArray("children")
        val parsedList = mutableListOf<SDUIBlock>()
        for (i in 0 until childrenArray.length()) {
            parsedList.add(parseSDUIBlock(childrenArray.getJSONObject(i)))
        }
        children = parsedList
    }
    
    return SDUIBlock(type, url, modifierParams, children)
}

// Dynamic Jetpack Compose layout renderer
@Composable
fun RowScope.RowSDUIItem(child: SDUIBlock) {
    val w = child.modifierParams?.weight
    val childModifier = if (w != null) Modifier.weight(w) else Modifier
    RenderSDUIBlock(child, parentModifier = childModifier)
}

@Composable
fun RenderSDUIBlock(block: SDUIBlock, parentModifier: Modifier = Modifier) {
    var mod = parentModifier
    block.modifierParams?.let { params ->
        if (params.fillMaxWidth) mod = mod.fillMaxWidth()
        if (params.heightDp != null) mod = mod.height(params.heightDp.dp)
        if (params.paddingDp != null) mod = mod.padding(params.paddingDp.dp)
        if (params.aspectRatio != null) mod = mod.aspectRatio(params.aspectRatio)
    }

    when (block.type) {
        "row" -> {
            if (block.children.isNullOrEmpty()) return
            Row(modifier = mod.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                block.children?.forEach { child -> RowSDUIItem(child) }
            }
        }
        "image" -> {
           if (block.url.isNullOrBlank()) return
           AsyncImage(
                model = block.url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = mod.clip(RoundedCornerShape(12.dp))
            )
        }
        "video" -> {
            if (block.url.isNullOrBlank()) return
            val context = LocalContext.current
            val exoPlayer = remember {
                ExoPlayer.Builder(context).build().apply {
                    val mediaItem = MediaItem.fromUri(block.url ?: "")
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                    repeatMode = Player.REPEAT_MODE_ALL
                }
            }
            DisposableEffect(exoPlayer) {
                onDispose { exoPlayer.release() }
            }
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false // looping aesthetics
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = mod.clip(RoundedCornerShape(12.dp))
            )
        }
    }
}
