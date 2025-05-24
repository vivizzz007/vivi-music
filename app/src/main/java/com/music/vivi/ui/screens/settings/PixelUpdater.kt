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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
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

import androidx.compose.ui.text.TextStyle

@Composable
fun PixelUpdaterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val lightColors = lightColorScheme(
        primary = Color(0xFF1A73E8), // Pixel blue
        onPrimary = Color.White,
        secondary = Color(0xFF0F9D58), // Pixel green
        onSecondary = Color.White,
        background = Color(0xFFFEFEFE), // Pure white background
        surface = Color(0xFFFEFEFE),
        surfaceVariant = Color(0xFFF5F5F5),
        onSurface = Color(0xFF1F1F1F),
        onSurfaceVariant = Color(0xFF5F6368),
        outline = Color(0xFFDADCE0),
        outlineVariant = Color(0xFFE8EAED)
    )

    val darkColors = darkColorScheme(
        primary = Color(0xFF8AB4F8),
        onPrimary = Color(0xFF1A1A1A),
        secondary = Color(0xFF81C995),
        onSecondary = Color(0xFF1A1A1A),
        background = Color(0xFF0F0F0F),
        surface = Color(0xFF0F0F0F),
        surfaceVariant = Color(0xFF1A1A1A),
        onSurface = Color(0xFFE8EAED),
        onSurfaceVariant = Color(0xFF9AA0A6),
        outline = Color(0xFF5F6368),
        outlineVariant = Color(0xFF3C4043)
    )

    val colorScheme = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColors
        else -> lightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            // Pixel-style typography
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
    var hasUpdate by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val packageInfo = remember { context.packageManager.getPackageInfo(context.packageName, 0) }
    val versionName = packageInfo.versionName ?: "Unknown"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode.toString()
    else @Suppress("DEPRECATION") packageInfo.versionCode.toString()
    val currentVersion = "Version $versionName · Build $versionCode"

    val coroutineScope = rememberCoroutineScope()
    val githubReleasesUrl = "https://api.github.com/repos/vivizzz007/vivi-music/releases"

    fun installApk(file: File) {
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",  // Changed from .fileprovider to .provider
                file
            )

            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = apkUri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                // Don't add NEW_TASK flag here
            }

            context.startActivity(installIntent)
            updateStatus = "Opening installer..."
        } catch (e: ActivityNotFoundException) {
            updateStatus = "No app found to install APK"
        } catch (e: Exception) {
            updateStatus = "Installation failed: ${e.localizedMessage}"
            e.printStackTrace()
        }
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
                    Spacer(Modifier.height(32.dp))
                }

                preReleaseChangeLog?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                }

                Text(currentVersion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

                if (!isCheckingUpdates && !hasUpdate) {
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
                        preReleaseDownloadUrl == null && !isCheckingUpdates -> {
                            Button(
                                onClick = {
                                    isCheckingUpdates = true
                                    updateStatus = "Checking for updates..."
                                    showProgressBar = false
                                    updateProgress = 0f
                                    apkFilePath = null
                                    preReleaseChangeLog = null
                                    preReleaseDownloadUrl = null
                                    preReleaseName = null
                                    preReleaseTag = null
                                    hasUpdate = false

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
                                                            found = true
                                                            break
                                                        }
                                                    }
                                                }
                                                withContext(Dispatchers.Main) {
                                                    updateStatus = if (found) {
                                                        hasUpdate = true
                                                        "System update available"
                                                    } else {
                                                        hasUpdate = false
                                                        "Your system is up to date"
                                                    }
                                                    isCheckingUpdates = false
                                                }
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    updateStatus = "Couldn't check for updates"
                                                    isCheckingUpdates = false
                                                }
                                            }
                                        } catch (_: Exception) {
                                            withContext(Dispatchers.Main) {
                                                updateStatus = "Couldn't check for updates"
                                                isCheckingUpdates = false
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
                            Button(
                                onClick = {
                                    if (!showProgressBar) {
                                        showProgressBar = true
                                        updateStatus = "Downloading update..."
                                        updateProgress = 0f
                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                val client = OkHttpClient()
                                                val response = client.newCall(Request.Builder().url(preReleaseDownloadUrl!!).build()).execute()
                                                if (!response.isSuccessful) {
                                                    withContext(Dispatchers.Main) {
                                                        updateStatus = "Download failed"
                                                        showProgressBar = false
                                                    }
                                                    return@launch
                                                }

                                                val body = response.body
                                                val contentLength = body?.contentLength() ?: -1L
                                                val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
                                                if (apkFile.exists()) apkFile.delete()

                                                body?.byteStream()?.use { input ->
                                                    FileOutputStream(apkFile).use { output ->
                                                        val buffer = ByteArray(8 * 1024)
                                                        var bytesRead: Int
                                                        var total = 0L
                                                        while (input.read(buffer).also { bytesRead = it } != -1) {
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
                                                    updateStatus = "Download complete"
                                                    apkFilePath = apkFile
                                                    updateProgress = 1f
                                                    showProgressBar = false
                                                }
                                            } catch (_: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    updateStatus = "Download failed"
                                                    showProgressBar = false
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
                                Text("Download", style = MaterialTheme.typography.labelLarge)
                            }

                            if (!showProgressBar) {
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
                                        installApk(file)
                                        updateStatus = "Opening system installer..."
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