package com.music.vivi.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.* // Changed from material to material3
import androidx.compose.material.icons.Icons

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp // For more control over font sizes
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.compose.ui.text.TextStyle
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import kotlinx.coroutines.Job
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import androidx.compose.foundation.BorderStroke
import java.time.Instant
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.window.DialogProperties
import java.time.ZoneId
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PixelUpdaterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            // Pixel-style typography retained as it doesn't affect colors
            headlineSmall = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 32.sp
            ),
            titleLarge = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 28.sp
            ),
            bodyLarge = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 24.sp
            ),
            bodyMedium = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp
            ),
            labelLarge = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp
            )
        ),
        content = content
    )
}

@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelUpdaterUI(navController: NavHostController) {
    var updateStatus by remember { mutableStateOf("Your system is up to date") }
    var showProgressBar by remember { mutableStateOf(false) }
    var updateProgress by remember { mutableStateOf(0f) }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var apkFilePath by remember { mutableStateOf<File?>(null) }
    var preReleaseChangeLog by remember { mutableStateOf<String?>(null) }
    var preReleaseDownloadUrl by remember { mutableStateOf<String?>(null) }
    var preReleaseName by remember { mutableStateOf<String?>(null) }
    var preReleaseTag by remember { mutableStateOf<String?>(null) }
    var preReleaseSize by remember { mutableStateOf<Long?>(null) }
    var preReleaseUploadDate by remember { mutableStateOf<String?>(null) }
    var hasUpdate by remember { mutableStateOf(false) }
    var showInstallDialog by remember { mutableStateOf(false) }
    var apkToInstall by remember { mutableStateOf<File?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var isDownloadPaused by remember { mutableStateOf(false) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    val defaultDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val context = LocalContext.current
    val packageInfo = remember { context.packageManager.getPackageInfo(context.packageName, 0) }
    val versionName = packageInfo.versionName ?: "Unknown"
    val versionCode = packageInfo.longVersionCode.toString()
    val currentVersion = "Version $versionName · Build $versionCode"
    val coroutineScope = rememberCoroutineScope()
    val githubReleasesUrl = "https://api.github.com/repos/vivizzz007/vivi-music/releases"
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            showInstallDialog = true
        } else {
            updateStatus = "Install permission denied. Redirecting to settings..."
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                updateStatus = "Failed to open settings: ${e.localizedMessage}"
                e.printStackTrace()
            }
        }
    }

    fun isSameVersion(currentVersionCode: String, newTag: String?): Boolean {
        return newTag?.removePrefix("v")?.equals(currentVersionCode) == true
    }

    fun installApk(file: File) {
        if (file.name != "vivibeta.apk") {
            updateStatus = "Only vivibeta.apk can be installed"
            return
        }

        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = apkUri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }

            context.startActivity(installIntent)
            updateStatus = "Opening installer for vivibeta.apk..."
        } catch (e: ActivityNotFoundException) {
            updateStatus = "No app found to install APK"
        } catch (e: Exception) {
            updateStatus = "Installation failed: ${e.localizedMessage}"
            e.printStackTrace()
        }
    }

    fun checkAndRequestInstallPermission(file: File) {
        if (file.name != "vivibeta.apk") return

        apkToInstall = file
        val packageManager = context.packageManager
        if (!packageManager.canRequestPackageInstalls()) {
            permissionLauncher.launch(Manifest.permission.REQUEST_INSTALL_PACKAGES)
        } else {
            showInstallDialog = true
        }
    }

    fun checkViviBetaApk() {
        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val viviBetaApk = File(downloadsDir, "vivibeta.apk")

        downloadsDir?.listFiles()?.forEach { file ->
            if (file.name.endsWith(".apk") && file.name != "vivibeta.apk") {
                file.delete()
            }
        }

        if (viviBetaApk.exists() && !isSameVersion(versionCode, preReleaseTag)) {
            updateStatus = "Vivibeta.apk found, ready to install"
            apkFilePath = viviBetaApk
            hasUpdate = true
        } else {
            apkFilePath = null
            hasUpdate = false
            if (isSameVersion(versionCode, preReleaseTag)) {
                updateStatus = "Your system is up to date"
                viviBetaApk.delete()
            }
        }
    }

    fun fetchReleaseData() {
        isCheckingUpdates = true
        updateStatus = "Checking for updates..."

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(githubReleasesUrl).build()
                val response = client.newCall(request).execute()
                val json = response.body?.string()

                if (json != null) {
                    val releasesArray = JSONArray(json)
                    var found = false
                    for (i in 0 until releasesArray.length()) {
                        val release = releasesArray.getJSONObject(i)
                        if (release.getBoolean("prerelease")) {
                            val assets = release.getJSONArray("assets")
                            if (assets.length() > 0) {
                                val asset = assets.getJSONObject(0)
                                preReleaseDownloadUrl = asset.getString("browser_download_url")
                                preReleaseChangeLog = release.optString("body", "Performance improvements and bug fixes.")
                                preReleaseName = release.getString("name")
                                preReleaseTag = release.getString("tag_name")
                                preReleaseSize = asset.getLong("size")
                                preReleaseUploadDate = release.getString("published_at").let {
                                    try {
                                        val parsedDate = Instant.parse(it).atZone(ZoneId.systemDefault()).toLocalDate()
                                        DateTimeFormatter.ofPattern("MMM d, yyyy").format(parsedDate)
                                    } catch (e: Exception) {
                                        "Unknown"
                                    }
                                }
                                found = true
                                break
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        updateStatus = if (found && !isSameVersion(versionCode, preReleaseTag)) {
                            hasUpdate = true
                            "System update available"
                        } else {
                            hasUpdate = false
                            checkViviBetaApk()
                            if (apkFilePath != null) "vivibeta.apk found, ready to install" else "Your system is up to date"
                        }
                        isCheckingUpdates = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        updateStatus = "Couldn't check for updates"
                        isCheckingUpdates = false
                        checkViviBetaApk()
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    updateStatus = "Couldn't check for updates"
                    isCheckingUpdates = false
                    checkViviBetaApk()
                }
            }
        }
    }

    // Check for APK and fetch release data on composable entry
    LaunchedEffect(Unit) {
        checkViviBetaApk()
        if (apkFilePath != null && preReleaseTag == null) {
            fetchReleaseData()
        }
    }

    if (showInstallDialog) {
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            ),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.SystemUpdate,
                    contentDescription = "Update",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Install Update",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    Text(
                        "vivibeta.apk",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Version: ${preReleaseTag ?: "Unknown"} (Current: $versionCode)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Size: ${preReleaseSize?.let { "%.2f MB".format(it / 1_000_000.0) } ?: "Unknown"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Uploaded: ${preReleaseUploadDate ?: "Unknown"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "You are installing a beta version \n please backup before installing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        showInstallDialog = false
                        apkToInstall?.let { installApk(it) }
                        checkViviBetaApk()
                    },
                    modifier = Modifier.padding(end = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Install")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showInstallDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "System update",
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp, fontWeight = FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }, modifier = Modifier.padding(start = 4.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(30.dp))

                Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.SystemUpdate,
                        contentDescription = if (hasUpdate) "Update available" else "Up to date",
                        tint = if (hasUpdate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(Modifier.height(48.dp))

                Text(
                    updateStatus,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 32.sp, fontWeight = FontWeight.Normal, lineHeight = 40.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(32.dp))

                if (showProgressBar) {
                    LinearProgressIndicator(
                        progress = updateProgress,
                        modifier = Modifier.fillMaxWidth().height(4.dp).padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Downloading: ${(updateProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                }

                if (!isDownloading || isDownloadPaused) {
                    preReleaseChangeLog?.let { changelog ->
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Version: ${preReleaseTag ?: "Unknown"}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Size: ${preReleaseSize?.let { "%.2f MB".format(it / 1_000_000.0) } ?: "Unknown"}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Uploaded: ${preReleaseUploadDate ?: defaultDate}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            val bulletPoints = changelog.split("\n")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() && listOf("-", "*", "•").any { marker -> it.startsWith(marker) } }
                                .map { it.removePrefix("-").removePrefix("*").removePrefix("•").trim() }

                            if (bulletPoints.isNotEmpty()) {
                                bulletPoints.forEach { point ->
                                    Row {
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = point,
                                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = changelog,
                                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    Text(
                        currentVersion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                if (!isCheckingUpdates && !hasUpdate && !isDownloading) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Last checked: ${DateTimeFormatter.ofPattern("MMM d, h:mm a").format(LocalDateTime.now())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(40.dp))
            }

            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    when {
                        preReleaseDownloadUrl == null && !isCheckingUpdates && apkFilePath == null -> {
                            Button(
                                onClick = {
                                    fetchReleaseData()
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                if (isCheckingUpdates) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text("Checking for updates...", style = MaterialTheme.typography.labelLarge)
                                } else {
                                    Text("Check for update", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }

                        preReleaseDownloadUrl != null && apkFilePath == null -> {
                            if (isDownloading) {
                                Button(
                                    onClick = {
                                        isDownloadPaused = true
                                        downloadJob?.cancel()
                                        isDownloading = false
                                        updateStatus = "Download paused"
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    )
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = "Pause")
                                    Spacer(Modifier.width(8.dp))
                                    Text("Pause download", style = MaterialTheme.typography.labelLarge)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (!showProgressBar || isDownloadPaused) {
                                            showProgressBar = true
                                            isDownloading = true
                                            isDownloadPaused = false
                                            updateStatus = if (isDownloadPaused) "Resuming download..." else "Downloading update..."

                                            downloadJob = coroutineScope.launch(Dispatchers.IO) {
                                                try {
                                                    val client = OkHttpClient()
                                                    val request = Request.Builder()
                                                        .url(preReleaseDownloadUrl!!)
                                                        .header("Range", "bytes=${(updateProgress * 100).toInt()}-")
                                                        .build()

                                                    val response = client.newCall(request).execute()
                                                    if (!response.isSuccessful) {
                                                        withContext(Dispatchers.Main) {
                                                            updateStatus = "Download failed"
                                                            showProgressBar = false
                                                            isDownloading = false
                                                        }
                                                        return@launch
                                                    }

                                                    val body = response.body
                                                    val contentLength = body?.contentLength() ?: -1L
                                                    val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "vivibeta.apk")

                                                    if (apkFile.exists()) apkFile.delete()

                                                    body?.byteStream()?.use { input ->
                                                        FileOutputStream(apkFile).use { output ->
                                                            val buffer = ByteArray(8 * 1024)
                                                            var bytesRead: Int
                                                            var total = (updateProgress * 100).toLong()
                                                            while (input.read(buffer).also { bytesRead = it } != -1 && isActive) {
                                                                output.write(buffer, 0, bytesRead)
                                                                total += bytesRead
                                                                if (contentLength > 0) {
                                                                    withContext(Dispatchers.Main) {
                                                                        updateProgress = (total.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                                                                    }
                                                                }
                                                            }
                                                            output.flush()
                                                        }
                                                    }

                                                    withContext(Dispatchers.Main) {
                                                        if (isActive) {
                                                            if (!isSameVersion(versionCode, preReleaseTag)) {
                                                                updateStatus = "Download complete"
                                                                apkFilePath = apkFile
                                                                updateProgress = 1f
                                                                showProgressBar = false
                                                                isDownloading = false
                                                                isDownloadPaused = false
                                                                hasUpdate = true
                                                            } else {
                                                                updateStatus = "You already have this version installed"
                                                                apkFile.delete()
                                                                showProgressBar = false
                                                                isDownloading = false
                                                                isDownloadPaused = false
                                                                hasUpdate = false
                                                            }
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    if (e is CancellationException) {
                                                        withContext(Dispatchers.Main) {
                                                            updateStatus = "Download paused"
                                                        }
                                                    } else {
                                                        withContext(Dispatchers.Main) {
                                                            updateStatus = "Download failed"
                                                            showProgressBar = false
                                                            isDownloading = false
                                                            isDownloadPaused = false
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    if (isDownloadPaused) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                                        Spacer(Modifier.width(8.dp))
                                        Text("Resume download", style = MaterialTheme.typography.labelLarge)
                                    } else {
                                        Text("Download", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }

                            if (!showProgressBar && !isDownloading) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Update will install when your device restarts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        apkFilePath != null -> {
                            Button(
                                onClick = {
                                    apkFilePath?.let { file ->
                                        checkAndRequestInstallPermission(file)
                                        updateStatus = "Preparing to install Vivibeta.apk..."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Install", style = MaterialTheme.typography.labelLarge)
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Click install to open the system installer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}