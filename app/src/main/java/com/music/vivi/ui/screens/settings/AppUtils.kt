package com.music.vivi.ui.screens

import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import com.music.vivi.R
import android.content.Intent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tune

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

import java.util.Date
import java.util.Locale

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
    var showUpdateDetails by rememberSaveable { mutableStateOf(false) }
<<<<<<< HEAD
    var lastCheckedTime by remember { mutableStateOf("") } // For last checked timestamp
    var releaseDate by remember { mutableStateOf("") } // New state for release date
    var showMenu by remember { mutableStateOf(false) } // For 3-dot menu
=======
    var lastCheckedTime by remember { mutableStateOf("") }
    var releaseDate by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
>>>>>>> 426be3ed (updated code to 2.0.5)
    val changelogVisibility = remember { MutableTransitionState(false) }
    changelogVisibility.targetState = showUpdateDetails

    val context = LocalContext.current
    val currentVersion = BuildConfig.VERSION_NAME
    val coroutineScope = rememberCoroutineScope()

    val pixelBlue = Color(0xFF1A73E8)
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
<<<<<<< HEAD
        updateAvailable = false // Reset update status
        releaseDate = "" // Reset release date
=======
        updateAvailable = false
        releaseDate = ""
>>>>>>> 426be3ed (updated code to 2.0.5)
        coroutineScope.launch {
            delay(1500L)
            checkForUpdate(
                onSuccess = { latestVersion, latestChangelog, latestSize, latestReleaseDate ->
<<<<<<< HEAD
                    isChecking = false // Set to false when check completes
                    lastCheckedTime = getCurrentTimestamp() // Update timestamp
                    saveLastCheckedTime(context, lastCheckedTime) // Save to SharedPreferences
=======
                    isChecking = false
                    lastCheckedTime = getCurrentTimestamp()
                    saveLastCheckedTime(context, lastCheckedTime)
>>>>>>> 426be3ed (updated code to 2.0.5)
                    if (isNewerVersion(latestVersion, currentVersion)) {
                        updateAvailable = true
                        updateMessage = "New Update Available!"
                        updateMessageVersion = latestVersion
                        changelog = latestChangelog
                        appSize = latestSize
<<<<<<< HEAD
                        releaseDate = latestReleaseDate // Set release date
=======
                        releaseDate = latestReleaseDate
>>>>>>> 426be3ed (updated code to 2.0.5)
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

    // Trigger update check based on autoUpdateCheckEnabled
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
                    IconButton(
                        onClick = {
                            if (showUpdateDetails) {
                                showUpdateDetails = false
                            } else {
                                navController.navigateUp()
                            }
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
<<<<<<< HEAD
                    // 3-dot menu button
=======
>>>>>>> 426be3ed (updated code to 2.0.5)
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
<<<<<<< HEAD

                    // Dropdown menu
=======
>>>>>>> 426be3ed (updated code to 2.0.5)
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Preference") },
                            onClick = {
                                showMenu = false
                                navController.navigate("settings/experimental")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null
                                )
                            }
                        )
<<<<<<< HEAD

=======
>>>>>>> 426be3ed (updated code to 2.0.5)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.height(170.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 75.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isDownloading) {
                        when {
                            updateAvailable && !showUpdateDetails -> {
                                Button(
                                    onClick = { showUpdateDetails = true },
                                    shape = RoundedCornerShape(30.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = pixelBlue,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                ) {
                                    Text("Update", fontSize = 16.sp)
                                }
                            }
                            updateAvailable && showUpdateDetails && !isDownloadComplete -> {
                                Button(
                                    onClick = {
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
                                            }
                                        )
                                    },
                                    shape = RoundedCornerShape(30.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = pixelBlue,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                ) {
                                    Text("Download Update", fontSize = 16.sp)
                                }
                            }
                            isDownloadComplete -> {
                                Button(
                                    onClick = {
                                        val file = File(
                                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                                            "vivi.apk"
                                        )
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            file
                                        )
                                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/vnd.android.package-archive")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        ContextCompat.startActivity(context, installIntent, null)
                                    },
                                    shape = RoundedCornerShape(30.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = pixelBlue,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                ) {
                                    Text("Install Update", fontSize = 16.sp)
                                }
                            }
                            else -> {
                                Button(
                                    onClick = {
                                        triggerUpdateCheck()
                                        updateMessage = ""
                                    },
                                    shape = RoundedCornerShape(30.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = pixelBlue,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                ) {
                                    Text("Check for Updates", fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

                    if (updateAvailable) {
                        Text(
                            text = "VIVI MUSIC UPDATE",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Version $updateMessageVersion",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
<<<<<<< HEAD
                        // Display release date when update is available
=======
>>>>>>> 426be3ed (updated code to 2.0.5)
                        if (releaseDate.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Released: $releaseDate",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        AnimatedVisibility(visible = updateAvailable && isDownloading && !isDownloadComplete) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val animatedProgress = animateFloatAsState(
                                        targetValue = downloadProgress,
                                        animationSpec = tween(durationMillis = 300, easing = LinearEasing)
                                    ).value
                                    for (i in 0 until 20) {
                                        val segmentProgress = (i + 1) / 20f
                                        val isActive = animatedProgress >= segmentProgress
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isActive) pixelBlue else progressBarBackground)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${(downloadProgress * 100).toInt()}%",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(35.dp))
                        if (!showUpdateDetails) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showUpdateDetails = true }
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "What's new",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp,
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "What's new",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(180f)
                                )
                            }
                            Spacer(modifier = Modifier.height(35.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Important! ⚠",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Please do not press the back button during the download or installation process. Doing so may interrupt the update and could lead to unexpected issues.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        AnimatedVisibility(
                            visibleState = changelogVisibility,
                            enter = slideInVertically(
                                animationSpec = tween(durationMillis = 400, easing = EaseOut),
                                initialOffsetY = { it }
                            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                            exit = slideOutVertically(
                                animationSpec = tween(durationMillis = 300, easing = EaseIn),
                                targetOffsetY = { it }
                            ) + fadeOut(animationSpec = tween(durationMillis = 200))
                        ) {
                            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Vivi Music $updateMessageVersion",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Start
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Size: $appSize MB",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Text(
                                    text = "What's New",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Text(
                                    text = changelog,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp,
                                    textAlign = TextAlign.Start
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    } else {
                        Text(
                            text = "VIVI MUSIC",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Version $currentVersion",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
<<<<<<< HEAD
                        // Display last checked timestamp when no update is available
=======
>>>>>>> 426be3ed (updated code to 2.0.5)
                        if (lastCheckedTime.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Last checked: $lastCheckedTime",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.updateon))
                        val progress by animateLottieCompositionAsState(
                            composition,
                            iterations = LottieConstants.IterateForever
                        )

                        LottieAnimation(
                            composition,
                            progress,
                            modifier = Modifier
                                .size(350.dp)
                                .padding(top = 16.dp, bottom = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(15.dp))

                        if (!isChecking && !isDownloading) {
                            Text(
                                text = updateMessage,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (isChecking && !isDownloading && !fetchError) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Checking for updates...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = pixelBlue,
                                trackColor = progressBarBackground
                            )
                        }
                    } else if (fetchError && !isDownloading) {
                        Text(
                            text = "Failed to check for update. Please try again.",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )
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

<<<<<<< HEAD
// Helper function to format GitHub date to readable format
=======
>>>>>>> 426be3ed (updated code to 2.0.5)
private fun formatGitHubDate(githubDate: String): String {
    return try {
        val githubFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val displayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a")
        val dateTime = LocalDateTime.parse(githubDate, githubFormatter)
        dateTime.format(displayFormatter)
    } catch (e: Exception) {
<<<<<<< HEAD
        githubDate // Return original if parsing fails
    }
}

// Existing utility functions (unchanged)
private fun getCurrentMonth(): String {
    return SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
=======
        githubDate
    }
>>>>>>> 426be3ed (updated code to 2.0.5)
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
<<<<<<< HEAD
    onSuccess: (String, String, String, String) -> Unit, // Added fourth parameter for release date
=======
    onSuccess: (String, String, String, String) -> Unit,
>>>>>>> 426be3ed (updated code to 2.0.5)
    onError: () -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/vivizzz007/vivi-music/releases")
            val json = url.openStream().bufferedReader().use { it.readText() }
            val releases = JSONArray(json)
            var foundRelease: JSONObject? = null

<<<<<<< HEAD
            val tag = jsonObject.getString("tag_name").removePrefix("v")
            val changelog = jsonObject.getString("body")
            val publishedAt = jsonObject.getString("published_at") // Get release date
            val formattedReleaseDate = formatGitHubDate(publishedAt) // Format the date
=======
            val currentVersion = BuildConfig.VERSION_NAME
>>>>>>> 426be3ed (updated code to 2.0.5)

            // Find the highest version > currentVersion
            for (i in 0 until releases.length()) {
                val release = releases.getJSONObject(i)
                val tag = release.getString("tag_name").removePrefix("v")
                // Only consider tags that look like a semantic version
                if (!tag.matches(Regex("""\d+(\.\d+){1,2}"""))) continue
                if (isNewerVersion(tag, currentVersion)) {
                    if (foundRelease == null || isNewerVersion(tag, foundRelease.getString("tag_name").removePrefix("v"))) {
                        foundRelease = release
                    }
                }
            }

<<<<<<< HEAD
                withContext(Dispatchers.Main) {
                    onSuccess(tag, changelog, apkSizeInMB, formattedReleaseDate) // Pass release date
=======
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
>>>>>>> 426be3ed (updated code to 2.0.5)
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