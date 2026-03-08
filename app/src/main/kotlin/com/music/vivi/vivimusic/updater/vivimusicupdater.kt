/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.vivimusic.updater


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil3.compose.AsyncImage
import com.music.vivi.BuildConfig
import com.music.vivi.R
import com.music.vivi.vivimusic.updater.downloadmanager.CustomDownloadManager
import com.music.vivi.vivimusic.updater.downloadmanager.DownloadNotificationManager
import com.music.vivi.vivimusic.updater.downloadmanager.UpdateDownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateScreen(navController: NavHostController) {
    // State variables
    var updateAvailable by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }
    var updateMessageVersion by remember { mutableStateOf("") }
    // changelog
    var changelog by remember { mutableStateOf("") }
    var updateDescription by remember { mutableStateOf<String?>(null) } // Add this new state
    var updateImage by remember { mutableStateOf<String?>(null) } // display image

    var isChecking by remember { mutableStateOf(true) }
    var fetchError by remember { mutableStateOf(false) }
    // new downloadmanager
    val downloadManager = remember { CustomDownloadManager() }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }

    var appSize by remember { mutableStateOf("") }
    var apkUrl by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var isDownloadComplete by remember { mutableStateOf(false) }
    var isInstalling by remember { mutableStateOf(false) }
    var installProgress by remember { mutableStateOf(0f) }
    var showUpdateDetails by rememberSaveable { mutableStateOf(false) }
    var lastCheckedTime by remember { mutableStateOf("") }
    var releaseDate by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val currentVersion = BuildConfig.VERSION_NAME
    val coroutineScope = rememberCoroutineScope()
    val pixelBlue = MaterialTheme.colorScheme.primary
    if (isSystemInDarkTheme()) Color(0xFF3C4043) else Color(0xFFE0E0E0)
    val autoUpdateCheckEnabled = getAutoUpdateCheckSetting(context)

    var checkingProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        DownloadNotificationManager.initialize(context)
    }

    // Observe WorkManager for download progress
    LaunchedEffect(updateAvailable) {
        if (updateAvailable) {
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
                            downloadError = context.getString(R.string.download_failed)
                        }
                        WorkInfo.State.CANCELLED -> {
                            isDownloading = false
                            downloadProgress = 0f
                        }
                        else -> {}
                    }
                }
        }
    }

    LaunchedEffect(isChecking) {
        if (isChecking) {
            checkingProgress = 0f
            while (isChecking) {
                checkingProgress = (checkingProgress + 0.01f) % 1f
                delay(50L)
            }
        }
    }

    LaunchedEffect(Unit) {
        lastCheckedTime = getLastCheckedTime(context)
    }

    fun getCurrentTimestamp(): String {
        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a")
        return currentTime.format(formatter)
    }

    fun triggerUpdateCheck() {
        isChecking = true
        fetchError = false
        updateMessage = ""
        changelog = ""
        updateAvailable = false
        releaseDate = ""
        coroutineScope.launch {
            delay(6500L)
            checkForUpdate(
                context = context,
                onSuccess = {
                        latestVersion,
                        isAvailable,
                        latestChangelog,
                        latestSize,
                        latestReleaseDate,
                        latestDescription,
                        latestImage,
                        latestApkUrl,
                    ->
                    isChecking = false
                    lastCheckedTime = getCurrentTimestamp()
                    saveLastCheckedTime(context, lastCheckedTime)
                    
                    updateAvailable = isAvailable
                    saveUpdateAvailableState(context, isAvailable)
                    
                    if (isAvailable) {
                        updateMessage = "New Update Available!"
                        updateMessageVersion = latestVersion
                        changelog = latestChangelog
                        updateDescription = latestDescription
                        updateImage = latestImage
                        appSize = latestSize
                        releaseDate = latestReleaseDate
                        apkUrl = latestApkUrl
                    } else {
                        updateMessage = "You're already up to date."
                        updateMessageVersion = latestVersion
                    }
                },
                onError = {
                    isChecking = false
                    lastCheckedTime = getCurrentTimestamp()
                    saveLastCheckedTime(context, lastCheckedTime)
                    // If check fails, we might still have a previously recorded available update
                    // but we won't overwrite it to false here just in case they are offline.
                    fetchError = true
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (autoUpdateCheckEnabled) {
            triggerUpdateCheck()
        } else {
            isChecking = false
            updateAvailable = false
            updateMessage = "Automatic update check is disabled."
            fetchError = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_content_desc),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more_options),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
//                        DropdownMenuItem(
//                            text = { Text(stringResource(R.string.settings_title)) },
//                            onClick = {
//                                showMenu = false
//                                navController.navigate("settings/experimental")
//                            }
//                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.changelog)) },
                            onClick = {
                                showMenu = false
                                navController.navigate("settings/changelog")
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        if (isInstalling) {
            // Installing screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.installing_system_update),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 44.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(32.dp))

                LinearWavyProgressIndicator(
                    progress = { installProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.making_update_ready),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.your_app_getting_better),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                val annotatedString = buildAnnotatedString {
                    val version = updateMessageVersion.ifEmpty { currentVersion }
                    val url = "https://github.com/vivizzz007/vivi-music"
                    val text = stringResource(R.string.update_includes_features, version, url)
                    val urlIndex = text.indexOf(url)
                    append(text.substring(0, urlIndex))
                    pushStringAnnotation(tag = "URL", annotation = url)
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(url)
                    }
                    pop()
                    append(text.substring(urlIndex + url.length))
                }
                @Suppress("DEPRECATION")
                ClickableText(
                    text = annotatedString,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                context.startActivity(intent)
                            }
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.whats_new),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (changelog.isNotEmpty()) {
                    val changelogItems = changelog.split("\n").filter { it.isNotBlank() }
                    changelogItems.forEach { item ->
                        val urls = item.extractUrls()
                        val annotatedText = buildAnnotatedString {
                            append(item.trim())

                            // Add URL annotations
                            urls.forEach { (range, url) ->
                                addStringAnnotation(
                                    tag = "URL",
                                    annotation = url,
                                    start = range.first,
                                    end = range.last + 1
                                )
                                addStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = TextDecoration.Underline
                                    ),
                                    start = range.first,
                                    end = range.last + 1
                                )
                            }
                        }

                        // ADD BULLET POINT
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(8.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            @Suppress("DEPRECATION")
                            ClickableText(
                                text = annotatedText,
                                onClick = { offset ->
                                    annotatedText.getStringAnnotations("URL", offset, offset)
                                        .firstOrNull()?.let { annotation ->
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(annotation.item)
                                            )
                                            context.startActivity(intent)
                                        }
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 24.sp
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
                // Pause button at bottom - improved styling
                TextButton(
                    onClick = {
                        isInstalling = false
                        installProgress = 0f
                    },
                    modifier = Modifier
                        .height(48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.pause),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            // Main update screen
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))

                        // Main title
                        when {
                            updateAvailable -> {
                                Text(
//                                    text = "App update\navailable",
                                    text = stringResource(R.string.update_available),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = 44.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            isChecking -> {
                                Text(
                                    text = stringResource(R.string.checking_for_updates),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = 44.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            fetchError -> {
                                Text(
                                    text = stringResource(R.string.cant_check_updates),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = 44.sp
                                    ),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            else -> {
                                Text(
                                    text = stringResource(R.string.app_up_to_date),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 44.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        // Content based on state
                        when {
                            updateAvailable -> {
                                Text(
                                    text = stringResource(R.string.version, updateMessageVersion),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                if (releaseDate.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.released, releaseDate),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                // sync with the image
                                updateImage?.let { imageUrl ->
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = stringResource(R.string.update_preview),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.FillWidth
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // downloading

                                if (isDownloading) {
                                    Spacer(modifier = Modifier.height(15.dp))

                                    val thickStrokeWidth = with(LocalDensity.current) { 8.dp.toPx() }
                                    val thickStroke = remember(thickStrokeWidth) {
                                        Stroke(width = thickStrokeWidth, cap = StrokeCap.Round)
                                    }

                                    LinearWavyProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(22.dp),
                                        stroke = thickStroke,
                                        trackStroke = thickStroke,
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(
                                            R.string.downloading_percent,
                                            (downloadProgress * 100).toInt()
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // ADD DESCRIPTION HERE (if available)
                                updateDescription?.let { desc ->
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 24.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

//                                    val annotatedString = buildAnnotatedString {
//                                        append("Your app will update to ${updateMessageVersion}.\nLearn more ")
//                                        pushStringAnnotation(tag = "URL", annotation = "https://github.com/vivizzz007/vivi-music/releases")
//                                        withStyle(
//                                            style = SpanStyle(
//                                                color = MaterialTheme.colorScheme.primary,
//                                                textDecoration = TextDecoration.Underline
//                                            )
//                                        ) {
//                                            append("here")
//                                        }
//                                        pop()
//                                        append(".")
//                                    }
//                                    @Suppress("DEPRECATION")
//                                    ClickableText(
//                                        text = annotatedString,
//                                        onClick = { offset ->
//                                            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
//                                                .firstOrNull()?.let { annotation ->
//                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
//                                                    context.startActivity(intent)
//                                                }
//                                        },
//                                        style = MaterialTheme.typography.bodyLarge.copy(
//                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                            lineHeight = 24.sp
//                                        )
//                                    )

                                // Progress bar for downloading
                                // Progress bar for downloading
                                downloadError?.let { error ->
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.errorContainer,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.download_failed),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = error,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
//                                Spacer(modifier = Modifier.height(15.dp))
//                                Divider(
//                                    modifier = Modifier.padding(vertical = 8.dp),
//                                    color = MaterialTheme.colorScheme.outlineVariant,
//                                    thickness = 2.dp
//                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = stringResource(R.string.new_features_include),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                // //

                                // For the "Update Available" screen changelog section (around line 470):
                                if (changelog.isNotEmpty()) {
                                    val changelogItems = changelog.split("\n").filter { it.isNotBlank() }
                                    changelogItems.forEach { item ->
                                        val urls = item.extractUrls()
                                        val annotatedText = buildAnnotatedString {
                                            append(item.trim())

                                            // Add URL annotations
                                            urls.forEach { (range, url) ->
                                                addStringAnnotation(
                                                    tag = "URL",
                                                    annotation = url,
                                                    start = range.first,
                                                    end = range.last + 1
                                                )
                                                addStyle(
                                                    style = SpanStyle(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        textDecoration = TextDecoration.Underline
                                                    ),
                                                    start = range.first,
                                                    end = range.last + 1
                                                )
                                            }
                                        }

                                        // ADD BULLET POINT
                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 6.dp)
                                                    .size(8.dp)
                                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            )
                                            @Suppress("DEPRECATION")
                                            ClickableText(
                                                text = annotatedText,
                                                onClick = { offset ->
                                                    annotatedText.getStringAnnotations("URL", offset, offset)
                                                        .firstOrNull()?.let { annotation ->
                                                            val intent = Intent(
                                                                Intent.ACTION_VIEW,
                                                                Uri.parse(annotation.item)
                                                            )
                                                            context.startActivity(intent)
                                                        }
                                                },
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    lineHeight = 24.sp
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                // //
                                Spacer(modifier = Modifier.height(15.dp))
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    thickness = 2.dp
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = stringResource(R.string.mobile_network_warning),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 24.sp
                                )
                                Spacer(modifier = Modifier.height(40.dp))
                                Text(
                                    text = stringResource(R.string.update_size, appSize),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            isChecking -> {
                                val thickStrokeWidth = with(LocalDensity.current) { 8.dp.toPx() }
                                val thickStroke = remember(thickStrokeWidth) {
                                    Stroke(width = thickStrokeWidth, cap = StrokeCap.Round)
                                }

                                LinearWavyProgressIndicator(
                                    progress = { checkingProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(22.dp),
                                    stroke = thickStroke,
                                    trackStroke = thickStroke,
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                            fetchError -> {
                                Text(
                                    text = stringResource(R.string.check_internet_connection),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            else -> {
                                Text(
                                    text = stringResource(R.string.vivi_music_version, currentVersion),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (lastCheckedTime.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.last_checked, lastCheckedTime),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(100.dp)) // Space for bottom button
                    }
                }
// Bottom button with background container
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 39.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                when {
                                    updateAvailable && !isDownloading && !isDownloadComplete -> {
                                        // Start download with WorkManager
                                        val urlToDownload = apkUrl ?: "https://github.com/vivizzz007/vivi-music/releases/download/$updateMessageVersion/vivi.apk"

                                        val downloadRequest = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
                                            .setInputData(
                                                workDataOf(
                                                    "apk_url" to urlToDownload,
                                                    "version" to updateMessageVersion,
                                                    "file_size" to appSize
                                                )
                                            )
                                            .addTag("update_download")
                                            .build()

                                        WorkManager.getInstance(context).enqueueUniqueWork(
                                            "update_download",
                                            ExistingWorkPolicy.REPLACE,
                                            downloadRequest
                                        )

                                        isDownloading = true
                                        downloadProgress = 0f
                                        downloadError = null
                                    }
                                    isDownloading -> {
                                        WorkManager.getInstance(context).cancelUniqueWork("update_download")
                                        isDownloading = false
                                        downloadProgress = 0f
                                        DownloadNotificationManager.cancelNotification()
                                    }

                                    isDownloadComplete -> {
                                        downloadedFile?.let { file ->
                                            // Check if app can install unknown apps
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                if (!context.packageManager.canRequestPackageInstalls()) {
                                                    // Need to request permission
                                                    val intent = Intent(
                                                        android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
                                                    ).apply {
                                                        data = Uri.parse("package:${context.packageName}")
                                                    }
                                                    context.startActivity(intent)
                                                    return@let
                                                }
                                            }

                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.FileProvider",
                                                file
                                            )
                                            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "application/vnd.android.package-archive")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            ContextCompat.startActivity(context, installIntent, null)
                                        }
                                    }

                                    !isChecking && !updateAvailable -> {
                                        triggerUpdateCheck()
                                        updateMessage = ""
                                    }
                                }
                            },
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDownloading) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                contentColor = if (isDownloading) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onPrimary
                                }
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                text = when {
                                    updateAvailable && !isDownloading && !isDownloadComplete -> stringResource(
                                        R.string.action_download
                                    )
                                    isDownloading -> stringResource(R.string.pause)
                                    isDownloadComplete -> stringResource(R.string.install)
                                    else -> stringResource(R.string.check_for_update)
                                },
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
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

fun getUpdateAvailableState(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_UPDATE_AVAILABLE, false)
}

fun saveUpdateAvailableState(context: Context, available: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_UPDATE_AVAILABLE, available).apply()
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

private fun formatGitHubDate(githubDate: String): String = try {
    val githubFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    val displayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a")
    val dateTime = LocalDateTime.parse(githubDate, githubFormatter)
    dateTime.format(displayFormatter)
} catch (e: Exception) {
    githubDate
}

// Robust version comparison: returns true if latestVersion > currentVersion
fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
    val latestVersionClean = latestVersion.removePrefix("b").removePrefix("v")
    val currentVersionClean = currentVersion.removePrefix("b").removePrefix("v")

    val latestParts = latestVersionClean.split(".").map { it.toIntOrNull() ?: 0 }
    val currentParts = currentVersionClean.split(".").map { it.toIntOrNull() ?: 0 }
    
    // Compare version numbers
    for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
        val latest = latestParts.getOrElse(i) { 0 }
        val current = currentParts.getOrElse(i) { 0 }
        when {
            latest > current -> return true
            latest < current -> return false
        }
    }
    
    // If numbers are equal, check if one is beta and the other is not
    if (latestVersionClean == currentVersionClean) {
        val latestIsBeta = latestVersion.startsWith("b")
        val currentIsBeta = currentVersion.startsWith("b")
        // Stable is "newer" (better) than beta of the same version
        if (currentIsBeta && !latestIsBeta) return true
    }
    
    return false
}

// Fetches ALL releases, finds the latest version > current, and returns its info
suspend fun checkForUpdate(
    context: Context,
    onSuccess: (tag: String, isAvailable: Boolean, changelog: String, size: String, date: String, description: String?, imageUrl: String?, apkUrl: String?) -> Unit,
    onError: () -> Unit,
) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/vivizzz007/vivi-music/releases")
            val json = url.openStream().bufferedReader().use { it.readText() }
            val releases = JSONArray(json)
            
            val currentVersion = BuildConfig.VERSION_NAME
            val betaEnabled = getBetaUpdatesSetting(context)

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
                    if (currentIsBeta && targetIsStable) {
                        shouldShow = true
                    } else if (isDifferentVersion && targetIsStable) {
                        // Also show if current is a newer unofficial stable (e.g. built locally as 5.0.7)
                        // but user wants the official stable 5.0.6.
                        shouldShow = true
                    }
                }

                if (shouldShow) {
                    val tagWithPrefix = targetRelease.getString("tag_name")
                    val displayTag = tagWithPrefix

                    // FETCH CHANGELOG.JSON FROM RELEASE ASSETS
                    var changelog = ""
                    var description: String? = null
                    var imageUrl: String? = null
                    try {
                        val changelogUrl =
                            URL("https://github.com/vivizzz007/vivi-music/releases/download/$tagWithPrefix/changelog.json")
                        val changelogJson = changelogUrl.openStream().bufferedReader().use { it.readText() }
                        val changelogData = JSONObject(changelogJson)

                        description = changelogData.optString("description").takeIf { it.isNotEmpty() }
                        imageUrl = changelogData.optString("image").takeIf { it.isNotEmpty() }

                        val changelogArray = changelogData.getJSONArray("changelog")
                        changelog = buildString {
                            for (j in 0 until changelogArray.length()) {
                                appendLine(changelogArray.getString(j))
                            }
                            val warning = changelogData.optString("warning").takeIf { it.isNotEmpty() }
                            if (!warning.isNullOrBlank()) {
                                appendLine("")
                                appendLine(warning)
                            }
                        }.trim()
                    } catch (e: Exception) {
                        changelog = targetRelease.optString("body", context.getString(R.string.no_changelog_available))
                    }

                    val publishedAt = targetRelease.getString("published_at")
                    val formattedReleaseDate = formatGitHubDate(publishedAt)
                    val assets = targetRelease.getJSONArray("assets")

                    var apkSizeInMB = ""
                    var apkDownloadUrl = ""
                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        val assetName = asset.getString("name")
                        if (assetName.endsWith(".apk")) {
                            val apkSizeInBytes = asset.getLong("size")
                            apkSizeInMB = String.format("%.1f", apkSizeInBytes / (1024.0 * 1024.0))
                            apkDownloadUrl = asset.getString("browser_download_url")
                            break
                        }
                    }

                    if (apkDownloadUrl.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            onSuccess(displayTag, true, changelog, apkSizeInMB, formattedReleaseDate, description, imageUrl, apkDownloadUrl)
                        }
                        return@withContext
                    }
                }
            }

            // No update found or APK missing
            withContext(Dispatchers.Main) {
                onSuccess(currentVersion, false, "", "", "", null, null, null)
            }
        } catch (e: Exception) {
            Log.e("UpdateCheck", "Error checking for updates: ${e.message}", e)
            withContext(Dispatchers.Main) { onError() }
        }
    }
}
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
