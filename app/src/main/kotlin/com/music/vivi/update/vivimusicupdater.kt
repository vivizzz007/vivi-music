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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning


import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import com.music.vivi.update.experiment.getBetaUpdaterSetting
import com.music.vivi.update.experiment.getSelectedApkVariant
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.net.HttpURLConnection
import java.util.regex.Pattern

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(navController: NavHostController) {
    // State variables
    var updateAvailable by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }
    var updateMessageVersion by remember { mutableStateOf("") }
    var changelog by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(true) }
    var appSize by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var isDownloadComplete by remember { mutableStateOf(false) }
    var isInstalling by remember { mutableStateOf(false) }
    var installProgress by remember { mutableStateOf(0f) }
    var lastCheckedTime by remember { mutableStateOf("") }
    var releaseDate by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    // Animation states
    var showVersionText by remember { mutableStateOf(false) }
    var showViviText by remember { mutableStateOf(true) }
    var showUpdateCard by remember { mutableStateOf(false) }
    var showProgressBar by remember { mutableStateOf(false) }
    var showUpdateDetectedAnimation by remember { mutableStateOf(false) }
    var showCheckmark by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val currentVersion = BuildConfig.VERSION_NAME
    val coroutineScope = rememberCoroutineScope()
    val autoUpdateCheckEnabled = getAutoUpdateCheckSetting(context)
    val betaUpdaterEnabled = getBetaUpdaterSetting(context)

    // Animation specs
    val viviTextAlpha by animateFloatAsState(
        targetValue = if (showViviText) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
    )

    val versionTextAlpha by animateFloatAsState(
        targetValue = if (showVersionText) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
    )

    val updateCardAlpha by animateFloatAsState(
        targetValue = if (showUpdateCard) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
    )

    val progressBarAlpha by animateFloatAsState(
        targetValue = if (showProgressBar) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing)
    )

    val updateDetectedAlpha by animateFloatAsState(
        targetValue = if (showUpdateDetectedAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
    )

    val checkmarkScale by animateFloatAsState(
        targetValue = if (showCheckmark) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 200f
        )
    )

    // Animation for button text
    val buttonText by animateColorAsState(
        targetValue = when {
            updateAvailable && !isDownloading && !isDownloadComplete -> MaterialTheme.colorScheme.onPrimary
            isDownloading -> MaterialTheme.colorScheme.onPrimary
            isDownloadComplete -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onPrimary
        },
        animationSpec = tween(durationMillis = 300)
    )

    // Scale animation for buttons
    val buttonScale by animateFloatAsState(
        targetValue = if (isChecking) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
    )

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
        updateMessage = ""
        changelog = ""
        updateAvailable = false
        releaseDate = ""
        showUpdateCard = false
        showProgressBar = true
        showUpdateDetectedAnimation = false
        showCheckmark = false

        // Start animation sequence
        showViviText = true
        showVersionText = false

        coroutineScope.launch {
            // Show VIVI MUSIC text briefly
            delay(800L)

            // Animate to version text
            showViviText = false
            delay(300L)
            showVersionText = true

            // Continue with update check
            delay(400L)

            checkForUpdate(
                isBetaEnabled = betaUpdaterEnabled,
                onSuccess = { latestVersion, latestChangelog, latestSize, latestReleaseDate ->
                    isChecking = false
                    showProgressBar = false
                    lastCheckedTime = getCurrentTimestamp()
                    saveLastCheckedTime(context, lastCheckedTime)
                    if (isNewerVersion(latestVersion, currentVersion)) {
                        // Hide version info when update is detected
                        showVersionText = false
                        showViviText = false

                        // Show "Update Detected!" animation
                        launch {
                            showUpdateDetectedAnimation = true
                            delay(1500L)
                            showUpdateDetectedAnimation = false
                            delay(200L)
                            showUpdateCard = true
                        }
                        updateAvailable = true
                        updateMessage = "New Update Available!"
                        updateMessageVersion = latestVersion
                        changelog = latestChangelog
                        appSize = latestSize
                        releaseDate = latestReleaseDate
                    } else {
                        updateAvailable = false
                        updateMessage = "You're already up to date."
                        // Show version text when done
                        showVersionText = true
                        showViviText = false
                    }
                },
                onError = {
                    isChecking = false
                    showProgressBar = false
                    lastCheckedTime = getCurrentTimestamp()
                    saveLastCheckedTime(context, lastCheckedTime)
                    updateAvailable = false
                    updateMessage = "You're already up to date."
                    // Show version text when done
                    showVersionText = true
                    showViviText = false
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
            showVersionText = true
            showViviText = false
        }
    }

    // Animate download progress
    LaunchedEffect(isDownloading) {
        if (isDownloading) {
            // Simulate download progress animation
            for (progress in 0..100 step 2) {
                if (!isDownloading) break
                downloadProgress = progress / 100f
                delay(100L)
            }
            if (downloadProgress >= 1f) {
                isDownloadComplete = true
                isDownloading = false
            }
        }
    }

    Scaffold(
        topBar = {
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeOut()
            ) {
                TopAppBar(
                    title = {
                        // Optional: Add animated title if needed
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.navigateUp() },
                            modifier = Modifier.scale(buttonScale)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.scale(buttonScale)
                        ) {
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
                                text = {
                                    Text("Settings")
                                },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("settings/experimental")
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Changelog")
                                },
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
        },
        bottomBar = {
            // BOTTOM BUTTON - This ensures it's always visible and properly positioned
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 90.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            when {
                                updateAvailable && !isDownloading && !isDownloadComplete -> {
                                    navController.navigate("update/details")
                                }
                                isDownloading -> {
                                    isDownloading = false
                                    downloadProgress = 0f
                                }
                                isDownloadComplete -> {
                                    val selectedVariant = getSelectedApkVariant(context)
                                    val file = File(
                                        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                                        selectedVariant
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .scale(buttonScale)
                            .animateContentSize()
                    ) {
                        AnimatedContent(
                            targetState = when {
                                updateAvailable && !isDownloading && !isDownloadComplete -> "Update Now"
                                isDownloading -> "Pause"
                                isDownloadComplete -> "Install"
                                else -> "Check for update"
                            },
                            transitionSpec = {
                                slideInVertically { height -> height } + fadeIn() with
                                        slideOutVertically { height -> -height } + fadeOut()
                            }
                        ) { targetText ->
                            Text(
                                text = targetText,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = buttonText
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                // TOP CENTER CONTENT - VIVI MUSIC, Version, Last Checked
                // Only show when NO update is available
                if (!updateAvailable) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // VIVI MUSIC text with animation (during initial load)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showViviText && !isChecking,
                            enter = scaleIn(
                                initialScale = 0.8f,
                                animationSpec = tween(durationMillis = 600)
                            ) + fadeIn(),
                            exit = scaleOut(
                                targetScale = 1.2f,
                                animationSpec = tween(durationMillis = 400)
                            ) + fadeOut(),
                            modifier = Modifier.alpha(viviTextAlpha)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "VIVI MUSIC",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 42.sp,
                                        letterSpacing = 2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Your app is up to date",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Version text with animation (after check completes)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showVersionText && !isChecking,
                            enter = scaleIn(
                                initialScale = 0.8f,
                                animationSpec = tween(durationMillis = 600)
                            ) + fadeIn(),
                            exit = scaleOut() + fadeOut(),
                            modifier = Modifier.alpha(versionTextAlpha)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "VIVI MUSIC",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 42.sp,
                                        letterSpacing = 2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentVersion,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your app is up to date",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Checking for updates state
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isChecking,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "VIVI MUSIC",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 42.sp,
                                        letterSpacing = 2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Checking for updates...",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Checking state progress bar with animation
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isChecking && showProgressBar,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(durationMillis = 400)
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(durationMillis = 300)
                        ) + fadeOut(),
                        modifier = Modifier.alpha(progressBarAlpha)
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    // Last checked time with animation
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isChecking && showVersionText && lastCheckedTime.isNotEmpty(),
                        enter = fadeIn(
                            animationSpec = tween(durationMillis = 600, delayMillis = 200)
                        ),
                        exit = fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Last checked: $lastCheckedTime",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // "Update Detected!" animation overlay - Shows at top when update is found
                if (updateAvailable) {
                    Spacer(modifier = Modifier.height(24.dp))

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showUpdateDetectedAnimation,
                        enter = scaleIn(
                            initialScale = 0.5f,
                            animationSpec = spring(
                                dampingRatio = 0.6f,
                                stiffness = 200f
                            )
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 400)
                        ),
                        exit = scaleOut(
                            targetScale = 1.2f,
                            animationSpec = tween(durationMillis = 400)
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 400)
                        ),
                        modifier = Modifier.alpha(updateDetectedAlpha)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Animated icon or badge
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Update Detected!",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Version $updateMessageVersion",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Update Card - Only show when update is available
                if (updateAvailable) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showUpdateCard,
                        enter = slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                        ) + fadeIn() + scaleIn(
                            initialScale = 0.9f,
                            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { it / 2 },
                            animationSpec = tween(durationMillis = 400)
                        ) + fadeOut() + scaleOut(
                            targetScale = 0.9f,
                            animationSpec = tween(durationMillis = 400)
                        ),
                        modifier = Modifier.alpha(updateCardAlpha)
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(24.dp))

                            // Main heading for update available with animation
                            androidx.compose.animation.AnimatedVisibility(
                                visible = updateAvailable,
                                enter = slideInHorizontally(
                                    initialOffsetX = { -it },
                                    animationSpec = tween(durationMillis = 500)
                                ) + fadeIn(),
                                exit = slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(durationMillis = 300)
                                ) + fadeOut()
                            ) {
                                Text(
                                    text = "An update is available",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 450.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .animateContentSize(
                                        animationSpec = spring(
                                            dampingRatio = 0.8f,
                                            stiffness = 300f
                                        )
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF002147)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .background(Color(0xFF002147))
                                        .padding(32.dp)
                                ) {
                                    Column {
                                        // App name/branding with animation
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = true,
                                            enter = slideInHorizontally(
                                                initialOffsetX = { -it },
                                                animationSpec = tween(durationMillis = 600)
                                            ) + fadeIn(),
                                            modifier = Modifier
                                        ) {
                                            Text(
                                                text = "VIVI MUSIC",
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 36.sp,
                                                    letterSpacing = 1.5.sp
                                                ),
                                                color = Color.White
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Version info with staggered animation
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = true,
                                            enter = slideInHorizontally(
                                                initialOffsetX = { -it },
                                                animationSpec = tween(
                                                    durationMillis = 600,
                                                    delayMillis = 100
                                                )
                                            ) + fadeIn(),
                                        ) {
                                            Text(
                                                text = "$currentVersion → $updateMessageVersion | $appSize MB",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))

                                        // Release date with animation
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = true,
                                            enter = slideInHorizontally(
                                                initialOffsetX = { -it },
                                                animationSpec = tween(
                                                    durationMillis = 600,
                                                    delayMillis = 200
                                                )
                                            ) + fadeIn(),
                                        ) {
                                            Text(
                                                text = " $releaseDate ",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(48.dp))

                                        // Updated apps section with animation
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = true,
                                            enter = slideInHorizontally(
                                                initialOffsetX = { -it },
                                                animationSpec = tween(
                                                    durationMillis = 600,
                                                    delayMillis = 300
                                                )
                                            ) + fadeIn(),
                                        ) {
                                            Column {
                                                Text(
                                                    text = "What's new",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = Color.White.copy(alpha = 0.9f)
                                                )

                                                Spacer(modifier = Modifier.height(16.dp))

                                                // Show first 2 changelog items as preview
                                                if (changelog.isNotEmpty()) {
                                                    val changelogItems = changelog.split("\n")
                                                        .filter { it.isNotBlank() }
                                                        .take(5)

                                                    changelogItems.forEachIndexed { index, item ->
                                                        androidx.compose.animation.AnimatedVisibility(
                                                            visible = true,
                                                            enter = slideInVertically(
                                                                initialOffsetY = { it / 2 },
                                                                animationSpec = tween(
                                                                    durationMillis = 400,
                                                                    delayMillis = 400 + (index * 100)
                                                                )
                                                            ) + fadeIn(),
                                                            modifier = Modifier.padding(vertical = 2.dp)
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.Top
                                                            ) {
                                                                Text(
                                                                    text = "• ",
                                                                    color = Color.White.copy(alpha = 0.9f),
                                                                    style = MaterialTheme.typography.bodyMedium
                                                                )
                                                                Text(
                                                                    text = item.trim().take(60) + if (item.length > 60) "..." else "",
                                                                    color = Color.White.copy(alpha = 0.9f),
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(40.dp))

                                        // Learn what's new link with animation
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = true,
                                            enter = slideInHorizontally(
                                                initialOffsetX = { -it },
                                                animationSpec = tween(
                                                    durationMillis = 600,
                                                    delayMillis = 800
                                                )
                                            ) + fadeIn(),
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .clickable { navController.navigate("update/details") }
                                                    .animateContentSize(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Learn what's new",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ArrowForward,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Download progress with smooth animation
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isDownloading,
                                enter = slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = tween(durationMillis = 400)
                                ) + fadeIn(),
                                exit = slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(durationMillis = 300)
                                ) + fadeOut(),
                                modifier = Modifier.alpha(progressBarAlpha)
                            ) {
                                Column {
                                    LinearProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Downloading... ${(downloadProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Additional info with fade animation
                            androidx.compose.animation.AnimatedVisibility(
                                visible = updateAvailable,
                                enter = fadeIn(
                                    animationSpec = tween(
                                        durationMillis = 600,
                                        delayMillis = 400
                                    )
                                ),
                                exit = fadeOut()
                            ) {
                                Text(
                                    text = "Downloading updates over a mobile network or while roaming may cause additional charges.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// Utility functions for SharedPreferences
const val PREFS_NAME = "app_settings"
const val KEY_AUTO_UPDATE_CHECK = "auto_update_check_enabled"
const val KEY_LAST_CHECKED_TIME = "last_checked_time"

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

fun downloadApk(
    context: Context,
    apkUrl: String,
    onProgress: (Float) -> Unit,
    onDownloadComplete: () -> Unit
) {
    val selectedVariant = getSelectedApkVariant(context)
    val request = DownloadManager.Request(Uri.parse(apkUrl))
        .setDestinationInExternalFilesDir(
            context,
            Environment.DIRECTORY_DOWNLOADS,
            selectedVariant
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

fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
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

suspend fun checkForUpdate(
    isBetaEnabled: Boolean,
    onSuccess: (String, String, String, String) -> Unit,
    onError: () -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            withTimeout(8000L) { // ⚠️ ADD TIMEOUT
                val url = URL("https://api.github.com/repos/vivizzz007/vivi-music/releases")
                val connection = url.openConnection() as HttpURLConnection // ⚠️ USE HttpURLConnection
                connection.connectTimeout = 5000 // ⚠️ ADD CONNECTION TIMEOUT
                connection.readTimeout = 5000     // ⚠️ ADD READ TIMEOUT

                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val releases = JSONArray(json)
                var foundRelease: JSONObject? = null

                val currentVersion = BuildConfig.VERSION_NAME
                val tagPrefix = if (isBetaEnabled) "b" else "v"

                for (i in 0 until releases.length()) {
                    val release = releases.getJSONObject(i)
                    val tag = release.getString("tag_name")

                    if (!tag.startsWith(tagPrefix)) continue

                    val versionNumber = tag.removePrefix(tagPrefix)

                    if (!versionNumber.matches(Regex("""\d+(\.\d+){1,2}"""))) continue

                    if (isBetaEnabled) {
                        val minBetaVersion = "1.1.1"
                        if (!isNewerVersion(versionNumber, minBetaVersion) && versionNumber != minBetaVersion) {
                            continue
                        }
                    }

                    if (isNewerVersion(versionNumber, currentVersion)) {
                        if (foundRelease == null ||
                            isNewerVersion(versionNumber, foundRelease.getString("tag_name").removePrefix(tagPrefix))) {
                            foundRelease = release
                        }
                    }
                }

                if (foundRelease != null) {
                    val tag = foundRelease.getString("tag_name").removePrefix(tagPrefix)
                    val changelog = foundRelease.optString("body", "")
                    val publishedAt = foundRelease.getString("published_at")
                    val formattedReleaseDate = formatGitHubDate(publishedAt)
                    val assets = foundRelease.getJSONArray("assets")

                    var apkAsset: JSONObject? = null
                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        val assetName = asset.getString("name")
                        if (isBetaEnabled) {
                            if (assetName == "vivi-beta.apk") {
                                apkAsset = asset
                                break
                            } else if (assetName == "vivi.apk" && apkAsset == null) {
                                apkAsset = asset
                            }
                        } else {
                            if (assetName == "vivi.apk") {
                                apkAsset = asset
                                break
                            }
                        }
                    }

                    if (apkAsset != null) {
                        val apkSizeInBytes = apkAsset.getLong("size")
                        val apkSizeInMB = String.format("%.1f", apkSizeInBytes / (1024.0 * 1024.0))
                        withContext(Dispatchers.Main) {
                            onSuccess(tag, changelog, apkSizeInMB, formattedReleaseDate)
                        }
                    } else {
                        withContext(Dispatchers.Main) { onError() }
                    }
                } else {
                    val latest = if (releases.length() > 0) releases.getJSONObject(0) else null
                    if (latest != null) {
                        val tag = latest.getString("tag_name")
                        if (tag.startsWith(tagPrefix)) {
                            val versionNumber = tag.removePrefix(tagPrefix)
                            val changelog = latest.optString("body", "")
                            val publishedAt = latest.getString("published_at")
                            val formattedReleaseDate = formatGitHubDate(publishedAt)
                            val assets = latest.getJSONArray("assets")

                            var apkAsset: JSONObject? = null
                            for (j in 0 until assets.length()) {
                                val asset = assets.getJSONObject(j)
                                val assetName = asset.getString("name")
                                if (isBetaEnabled) {
                                    if (assetName == "vivi-beta.apk") {
                                        apkAsset = asset
                                        break
                                    } else if (assetName == "vivi.apk" && apkAsset == null) {
                                        apkAsset = asset
                                    }
                                } else {
                                    if (assetName == "vivi.apk") {
                                        apkAsset = asset
                                        break
                                    }
                                }
                            }

                            val apkSizeInMB = if (apkAsset != null)
                                String.format("%.1f", apkAsset.getLong("size") / (1024.0 * 1024.0))
                            else ""
                            withContext(Dispatchers.Main) {
                                onSuccess(versionNumber, changelog, apkSizeInMB, formattedReleaseDate)
                            }
                        } else {
                            withContext(Dispatchers.Main) { onError() }
                        }
                    } else {
                        withContext(Dispatchers.Main) { onError() }
                    }
                }

                connection.disconnect() // ⚠️ DISCONNECT
            }
        } catch (e: TimeoutCancellationException) {
            withContext(Dispatchers.Main) { onError() }
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
        val fullUrl = if (url.startsWith("http")) url else "https://$url"
        urlList.add(range to fullUrl)
    }

    return urlList
}