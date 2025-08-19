package com.music.vivi.ui.screens

import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.music.vivi.BuildConfig
import org.json.JSONObject
import java.io.File
import java.net.URL
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning


import androidx.compose.material.icons.filled.SystemUpdate
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(navController: NavHostController) {
    // State variables
    var updateAvailable by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }
    var updateMessageVersion by remember { mutableStateOf("") }
    var changelog by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(true) }
    var fetchError by remember { mutableStateOf(false) }
    var appSize by remember { mutableStateOf("") }
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
    val progressBarBackground = if (isSystemInDarkTheme()) Color(0xFF3C4043) else Color(0xFFE0E0E0)
    val autoUpdateCheckEnabled = getAutoUpdateCheckSetting(context)

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
            delay(1500L)
            checkForUpdate(
                onSuccess = { latestVersion, latestChangelog, latestSize, latestReleaseDate ->
                    isChecking = false
                    lastCheckedTime = getCurrentTimestamp()
                    saveLastCheckedTime(context, lastCheckedTime)
                    if (isNewerVersion(latestVersion, currentVersion)) {
                        updateAvailable = true
                        updateMessage = "New Update Available!"
                        updateMessageVersion = latestVersion
                        changelog = latestChangelog
                        appSize = latestSize
                        releaseDate = latestReleaseDate
                    } else {
                        updateAvailable = false
                        updateMessage = "You're already up to date."
                    }
                },
                onError = {
                    isChecking = false
                    lastCheckedTime = getCurrentTimestamp()
                    saveLastCheckedTime(context, lastCheckedTime)
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
                    Text(
                        "System Update",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showMenu = false
                                navController.navigate("settings/experimental")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Changelog") },
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
                Spacer(modifier = Modifier.height(24.dp)) // Reduced from 40dp
                // System update icon for installing
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, // Use theme color instead of fixed yellow
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(24.dp)) // Reduced from 40dp
                Text(
                    text = "Installing system\nupdate...",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 44.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(32.dp))
                // Progress bar
                LinearProgressIndicator(
                    progress = installProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp), // Increased thickness
                    color = MaterialTheme.colorScheme.primary, // Use theme color
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Optimizing your device, this may take a while",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Your Pixel is getting even better...",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                val annotatedString = buildAnnotatedString {
                    append("This update includes new Pixel features and the latest from Android ${updateMessageVersion.ifEmpty { currentVersion }}, making your device even more helpful. Learn more at ")
                    pushStringAnnotation(tag = "URL", annotation = "https://g.co/pixel/community")
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("g.co/pixel/community")
                    }
                    pop()
                    append(".")
                }
                ClickableText(
                    text = annotatedString,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                ContextCompat.startActivity(context, intent, null)
                            }
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "What's new?",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Display changelog during installation
                // In the updateAvailable section, replace the changelog display code with:
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

                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            ClickableText(
                                text = annotatedText,
                                onClick = { offset ->
                                    annotatedText.getStringAnnotations("URL", offset, offset)
                                        .firstOrNull()?.let { annotation ->
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(annotation.item)
                                            )
                                            ContextCompat.startActivity(context, intent, null)
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
                        text = "Pause",
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
                        Spacer(modifier = Modifier.height(24.dp)) // Reduced from 40dp
                        // System update icon (more authentic to Pixel)
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp)) // Reduced from 40dp to 20dp
                        // Main title
                        when {
                            updateAvailable -> {
                                Text(
//                                    text = "App update\navailable",
                                    text = "Update Available",
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
                                    text = "Checking for\nupdates...",
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
                                    text = "Can't check for\nupdates",
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
                                    text = "Your App is\nup to date",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Normal,
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
                                    text = "Version ${updateMessageVersion}",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                if (releaseDate.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Released: $releaseDate",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
//                                Spacer(modifier = Modifier.height(8.dp))
//                                Text(
//                                    text = "Get the latest version.",
//                                    style = MaterialTheme.typography.bodyLarge,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                val annotatedString = buildAnnotatedString {
                                    append("Your app will update to ${updateMessageVersion}.\nLearn more ")
                                    pushStringAnnotation(tag = "URL", annotation = "https://github.com/vivizzz007/vivi-music/releases")
                                    withStyle(
                                        style = SpanStyle(
                                            color = MaterialTheme.colorScheme.primary,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    ) {
                                        append("here")
                                    }
                                    pop()
                                    append(".")
                                }
                                ClickableText(
                                    text = annotatedString,
                                    onClick = { offset ->
                                        annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                            .firstOrNull()?.let { annotation ->
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                                ContextCompat.startActivity(context, intent, null)
                                            }
                                    },
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 24.sp
                                    )
                                )
                                // Progress bar for downloading
                                if (isDownloading) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    LinearProgressIndicator(
                                        progress = downloadProgress,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp), // Increased thickness
                                        color = MaterialTheme.colorScheme.primary, // Use theme color
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Downloading... ${(downloadProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(15.dp))
                                Divider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    thickness = 2.dp
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = "New features include:",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                // Display changelog
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

                                        Row(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 20.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            ClickableText(
                                                text = annotatedText,
                                                onClick = { offset ->
                                                    annotatedText.getStringAnnotations("URL", offset, offset)
                                                        .firstOrNull()?.let { annotation ->
                                                            val intent = Intent(
                                                                Intent.ACTION_VIEW,
                                                                Uri.parse(annotation.item)
                                                            )
                                                            ContextCompat.startActivity(context, intent, null)
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
                                Spacer(modifier = Modifier.height(15.dp))
                                Divider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    thickness = 2.dp
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = "Downloading updates over a mobile network or while roaming may cause additional charges.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 24.sp
                                )
                                Spacer(modifier = Modifier.height(40.dp))
                                Text(
                                    text = "Update size: $appSize MB",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                            }
                            isChecking -> {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp), // Increased thickness
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                            fetchError -> {
                                Text(
                                    text = "Check your internet connection and try again.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            else -> {
                                Text(
                                    text = "VIVI MUSIC version $currentVersion",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (lastCheckedTime.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Last checked: $lastCheckedTime",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(100.dp)) // Space for bottom button
                    }
                }
//button updater
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
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 39.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Button(
                            onClick = {
                                when {
                                    updateAvailable && !isDownloading && !isDownloadComplete -> {
                                        // Start download
                                        isDownloading = true
                                        downloadProgress = 0f
                                        val apkUrl = "https://github.com/vivizzz007/vivi-music/releases/download/v$updateMessageVersion/vivi.apk"
                                        downloadApk(
                                            context = context,
                                            apkUrl = apkUrl,
                                            onProgress = { progress ->
                                                downloadProgress = progress
                                            },
                                            onDownloadComplete = {
                                                isDownloading = false
                                                isDownloadComplete = true
                                                // Don't reset to check for update state
                                            }
                                        )
                                    }
                                    isDownloading -> {
                                        // Pause download
                                        isDownloading = false
                                        downloadProgress = 0f
                                        // Reset to initial update available state
                                    }
                                    isDownloadComplete -> {
                                        // Install the APK
                                        val file = File(
                                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                                            "vivi.apk"
                                        )
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
                                    !isChecking && !updateAvailable -> {
                                        // Only trigger update check when no update is available
                                        triggerUpdateCheck()
                                        updateMessage = ""
                                    }
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp,
                                focusedElevation = 2.dp,
                                hoveredElevation = 3.dp
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .widthIn(min = 120.dp)
                        ) {
                            Text(
                                text = when {
                                    updateAvailable && !isDownloading && !isDownloadComplete -> "Download"
                                    isDownloading -> "Pause"
                                    isDownloadComplete -> "Install"
                                    else -> "Check for update"
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

// Utility functions for SharedPreferences
private const val PREFS_NAME = "app_settings"
private const val KEY_AUTO_UPDATE_CHECK = "auto_update_check_enabled"
private const val KEY_LAST_CHECKED_TIME = "last_checked_time"

fun getAutoUpdateCheckSetting(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_AUTO_UPDATE_CHECK, true)
}

fun saveLastCheckedTime(context: Context, timestamp: String) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putString(KEY_LAST_CHECKED_TIME, timestamp).apply()
}

fun getLastCheckedTime(context: Context): String {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getString(KEY_LAST_CHECKED_TIME, "") ?: ""
}

private fun formatGitHubDate(githubDate: String): String {
    return try {
        val githubFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val displayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a")
        val dateTime = LocalDateTime.parse(githubDate, githubFormatter)
        dateTime.format(displayFormatter)
    } catch (e: Exception) {
        githubDate
    }
}


private fun downloadApk(
    context: Context,
    apkUrl: String,
    onProgress: (Float) -> Unit,
    onDownloadComplete: () -> Unit
) {
    val request = DownloadManager.Request(Uri.parse(apkUrl))
        .setDestinationInExternalFilesDir(
            context,
            Environment.DIRECTORY_DOWNLOADS,
            "vivi.apk"
        )
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setTitle("Downloading Vivi Music Update")
        .setDescription("Version ${BuildConfig.VERSION_NAME}")
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadId = downloadManager.enqueue(request)

    CoroutineScope(Dispatchers.IO).launch {
        var downloading = true
        while (downloading) {
            val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
            cursor?.use {
                if (it.moveToFirst()) {
                    val bytesDownloaded = it.getLong(
                        it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    )
                    val bytesTotal = it.getLong(
                        it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    )

                    if (bytesTotal > 0) {
                        val progress = (bytesDownloaded.toFloat() / bytesTotal.toFloat())
                            .coerceIn(0f, 1f)
                        withContext(Dispatchers.Main) {
                            onProgress(progress)
                        }
                    }

                    val status = it.getInt(
                        it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                    )
                    if (status == DownloadManager.STATUS_SUCCESSFUL ||
                        status == DownloadManager.STATUS_FAILED) {
                        downloading = false
                    }
                }
            }
            delay(500)
        }
        withContext(Dispatchers.Main) {
            onDownloadComplete()
        }
    }
}

// Robust version comparison: returns true if latestVersion > currentVersion
private fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
    val latestParts = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }
    val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
        val latest = latestParts.getOrElse(i) { 0 }
        val current = currentParts.getOrElse(i) { 0 }
        when {
            latest > current -> return true
            latest < current -> return false
        }
    }
    return false
}

// Fetches ALL releases, finds the latest version > current, and returns its info
private suspend fun checkForUpdate(
    onSuccess: (String, String, String, String) -> Unit,
    onError: () -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/vivizzz007/vivi-music/releases")
            val json = url.openStream().bufferedReader().use { it.readText() }
            val releases = JSONArray(json)
            var foundRelease: JSONObject? = null

            val currentVersion = BuildConfig.VERSION_NAME

            // Find the highest version > currentVersion
            for (i in 0 until releases.length()) {
                val release = releases.getJSONObject(i)
                val tag = release.getString("tag_name").removePrefix("v")
                if (!tag.matches(Regex("""\d+(\.\d+){1,2}"""))) continue
                if (isNewerVersion(tag, currentVersion)) {
                    if (foundRelease == null || isNewerVersion(tag, foundRelease.getString("tag_name").removePrefix("v"))) {
                        foundRelease = release
                    }
                }
            }

            if (foundRelease != null) {
                val tag = foundRelease.getString("tag_name").removePrefix("v")
                val changelog = foundRelease.optString("body", "")
                val publishedAt = foundRelease.getString("published_at")
                val formattedReleaseDate = formatGitHubDate(publishedAt)
                val assets = foundRelease.getJSONArray("assets")
                if (assets.length() > 0) {
                    val apkAsset = assets.getJSONObject(0)
                    val apkSizeInBytes = apkAsset.getLong("size")
                    val apkSizeInMB = String.format("%.1f", apkSizeInBytes / (1024.0 * 1024.0))
                    withContext(Dispatchers.Main) {
                        onSuccess(tag, changelog, apkSizeInMB, formattedReleaseDate)
                    }
                } else {
                    withContext(Dispatchers.Main) { onError() }
                }
            } else {
                // No update found; fallback to latest for message
                val latest = if (releases.length() > 0) releases.getJSONObject(0) else null
                if (latest != null) {
                    val tag = latest.getString("tag_name").removePrefix("v")
                    val changelog = latest.optString("body", "")
                    val publishedAt = latest.getString("published_at")
                    val formattedReleaseDate = formatGitHubDate(publishedAt)
                    val assets = latest.getJSONArray("assets")
                    val apkSizeInMB = if (assets.length() > 0)
                        String.format("%.1f", assets.getJSONObject(0).getLong("size") / (1024.0 * 1024.0))
                    else ""
                    withContext(Dispatchers.Main) {
                        onSuccess(tag, changelog, apkSizeInMB, formattedReleaseDate)
                    }
                } else {
                    withContext(Dispatchers.Main) { onError() }
                }
            }
        } catch (e: Exception) {
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

