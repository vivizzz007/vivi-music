// package com.music.vivi.update
//
// import android.content.Intent
// import android.net.Uri
// import android.os.Environment
// import androidx.compose.foundation.background
// import androidx.compose.foundation.layout.*
// import androidx.compose.foundation.rememberScrollState
// import androidx.compose.foundation.shape.RoundedCornerShape
// import androidx.compose.foundation.text.ClickableText
// import androidx.compose.foundation.verticalScroll
// import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.filled.ArrowBack
// import androidx.compose.material3.*
// import androidx.compose.runtime.*
// import androidx.compose.ui.Alignment
// import androidx.compose.ui.Modifier
// import androidx.compose.ui.draw.clip
// import androidx.compose.ui.platform.LocalContext
// import androidx.compose.ui.text.SpanStyle
// import androidx.compose.ui.text.buildAnnotatedString
// import androidx.compose.ui.text.font.FontWeight
// import androidx.compose.ui.text.style.TextAlign
// import androidx.compose.ui.text.style.TextDecoration
// import androidx.compose.ui.unit.dp
// import androidx.compose.ui.unit.sp
// import androidx.core.content.ContextCompat
// import androidx.core.content.FileProvider
// import androidx.navigation.NavHostController
// import com.music.vivi.BuildConfig
// import com.music.vivi.ui.screens.checkForUpdate
// //import com.music.vivi.ui.screens.downloadApk
// import com.music.vivi.ui.screens.extractUrls
// import com.music.vivi.update.experiment.getBetaUpdaterSetting
// import com.music.vivi.update.experiment.getSelectedApkVariant
// import kotlinx.coroutines.launch
// import java.io.File
//
// @OptIn(ExperimentalMaterial3Api::class)
// @Composable
// fun UpdateDetailsScreen(navController: NavHostController) {
//    var updateVersion by remember { mutableStateOf("") }
//    var changelog by remember { mutableStateOf("") }
//    var appSize by remember { mutableStateOf("") }
//    var releaseDate by remember { mutableStateOf("") }
//    var isLoading by remember { mutableStateOf(true) }
//    var isDownloading by remember { mutableStateOf(false) }
//    var downloadProgress by remember { mutableStateOf(0f) }
//    var isDownloadComplete by remember { mutableStateOf(false) }
//    val context = LocalContext.current
//    val currentVersion = BuildConfig.VERSION_NAME
//    val betaUpdaterEnabled = getBetaUpdaterSetting(context)
//    val coroutineScope = rememberCoroutineScope()
//
//    // Fetch update data when screen loads
// //    LaunchedEffect(Unit) {
// //        coroutineScope.launch {
// //            checkForUpdate(
// //                isBetaEnabled = betaUpdaterEnabled,
// //                onSuccess = { latestVersion, latestChangelog, latestSize, latestReleaseDate ->
// //                    updateVersion = latestVersion
// //                    changelog = latestChangelog
// //                    appSize = latestSize
// //                    releaseDate = latestReleaseDate
// //                    isLoading = false
// //                },
// //                onError = {
// //                    isLoading = false
// //                }
// //            )
// //        }
// //    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {},
//                navigationIcon = {
//                    IconButton(onClick = { navController.navigateUp() }) {
//                        Icon(
//                            Icons.Default.ArrowBack,
//                            contentDescription = "Back",
//                            tint = MaterialTheme.colorScheme.onSurface
//                        )
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.surface,
//                    titleContentColor = MaterialTheme.colorScheme.onSurface
//                )
//            )
//        }
//    ) { paddingValues ->
//        if (isLoading) {
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(paddingValues),
//                contentAlignment = Alignment.Center
//            ) {
//                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
//            }
//        } else {
//            Box(modifier = Modifier.fillMaxSize()) {
//                Column(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .background(MaterialTheme.colorScheme.surface)
//                        .padding(paddingValues)
//                        .verticalScroll(rememberScrollState())
//                        .padding(horizontal = 24.dp)
//                ) {
//                    Spacer(modifier = Modifier.height(32.dp))
// // VIVI MUSIC title - styled like "Xiaomi HyperOS"
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.Center,
//                        verticalAlignment = Alignment.Bottom
//                    ) {
//                        Text(
//                            text = "VIVI MUSIC",
//                            style = MaterialTheme.typography.headlineLarge.copy(
//                                fontWeight = FontWeight.Bold,
//                                fontSize = 28.sp,
//                                letterSpacing = 0.sp
//                            ),
//                            color = MaterialTheme.colorScheme.onSurface
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.height(24.dp))
//
//                    // Large version number (first digit only)
//                    val majorVersion = updateVersion.split(".").firstOrNull() ?: updateVersion
//                    Text(
//                        text = majorVersion,
//                        style = MaterialTheme.typography.displayLarge.copy(
//                            fontWeight = FontWeight.Light,
//                            fontSize = 120.sp,
//                            letterSpacing = (-4).sp
//                        ),
//                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
//                        textAlign = TextAlign.Center,
//                        modifier = Modifier.fillMaxWidth()
//                    )
//
//                    Spacer(modifier = Modifier.height(8.dp))
//
//                    // Full version and size
//                    Text(
//                        text = "$updateVersion | VIVI MUSIC | $appSize MB",
//                        style = MaterialTheme.typography.bodyMedium.copy(
//                            letterSpacing = 0.5.sp
//                        ),
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        textAlign = TextAlign.Center,
//                        modifier = Modifier.fillMaxWidth()
//                    )
//
//                    Spacer(modifier = Modifier.height(32.dp))
//
//                    // Download progress
//                    if (isDownloading) {
//                        LinearProgressIndicator(
//                            progress = downloadProgress,
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(6.dp)
//                                .clip(RoundedCornerShape(3.dp)),
//                            color = MaterialTheme.colorScheme.primary,
//                            trackColor = MaterialTheme.colorScheme.surfaceVariant
//                        )
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Text(
//                            text = "Downloading... ${(downloadProgress * 100).toInt()}%",
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                        Spacer(modifier = Modifier.height(24.dp))
//                    }
//
//                    // Changelog title
//                    Text(
//                        text = "Change Log",
//                        style = MaterialTheme.typography.titleMedium.copy(
//                            fontWeight = FontWeight.Bold
//                        ),
//                        color = MaterialTheme.colorScheme.onSurface
//                    )
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    // Changelog content - simple text without cards
//                    if (changelog.isNotEmpty()) {
//                        val changelogItems = changelog.split("\n").filter { it.isNotBlank() }
//                        changelogItems.forEach { item ->
//                            val urls = item.extractUrls()
//                            val annotatedText = buildAnnotatedString {
//                                append(item.trim())
//
//                                urls.forEach { (range, url) ->
//                                    addStringAnnotation(
//                                        tag = "URL",
//                                        annotation = url,
//                                        start = range.first,
//                                        end = range.last + 1
//                                    )
//                                    addStyle(
//                                        style = SpanStyle(
//                                            color = MaterialTheme.colorScheme.primary,
//                                            textDecoration = TextDecoration.Underline
//                                        ),
//                                        start = range.first,
//                                        end = range.last + 1
//                                    )
//                                }
//                            }
//
//                            ClickableText(
//                                text = annotatedText,
//                                onClick = { offset ->
//                                    annotatedText.getStringAnnotations("URL", offset, offset)
//                                        .firstOrNull()?.let { annotation ->
//                                            val intent = Intent(
//                                                Intent.ACTION_VIEW,
//                                                Uri.parse(annotation.item)
//                                            )
//                                            ContextCompat.startActivity(context, intent, null)
//                                        }
//                                },
//                                style = MaterialTheme.typography.bodyMedium.copy(
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                    lineHeight = 22.sp
//                                ),
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(vertical = 4.dp)
//                            )
//                        }
//                    } else {
//                        Text(
//                            text = "No changelog available",
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.height(24.dp))
//
//                    // Info text
//                    Text(
//                        text = "Downloading updates over a mobile network or while roaming may cause additional charges.",
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
//                        lineHeight = 18.sp
//                    )
//
//                    Spacer(modifier = Modifier.height(120.dp))
//                }
//
//                Box(
//                    modifier = Modifier
//                        .align(Alignment.BottomCenter)
//                        .fillMaxWidth()
//                        .padding(bottom = 90.dp),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Button(
//                        onClick = {
//                            when {
//                                !isDownloading && !isDownloadComplete -> {
//                                    // Start download
//                                    isDownloading = true
//                                    downloadProgress = 0f
//                                    val selectedVariant = getSelectedApkVariant(context)
//                                    val apkUrl = if (betaUpdaterEnabled) {
//                                        "https://github.com/vivizzz007/vivi-music/releases/download/b$updateVersion/vivi-beta.apk"
//                                    } else {
//                                        "https://github.com/vivizzz007/vivi-music/releases/download/v$updateVersion/vivi.apk"
//                                    }
//                                    downloadApk(
//                                        context = context,
//                                        apkUrl = apkUrl,
//                                        onProgress = { progress ->
//                                            downloadProgress = progress
//                                        },
//                                        onDownloadComplete = {
//                                            isDownloading = false
//                                            isDownloadComplete = true
//                                        }
//                                    )
//                                }
//                                isDownloading -> {
//                                    // Pause download
//                                    isDownloading = false
//                                    downloadProgress = 0f
//                                }
//                                isDownloadComplete -> {
//                                    // Install the APK
//                                    val selectedVariant = getSelectedApkVariant(context)
//                                    val file = File(
//                                        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
//                                        selectedVariant
//                                    )
//                                    val uri = FileProvider.getUriForFile(
//                                        context,
//                                        "${context.packageName}.FileProvider",
//                                        file
//                                    )
//                                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
//                                        setDataAndType(uri, "application/vnd.android.package-archive")
//                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                                    }
//                                    ContextCompat.startActivity(context, installIntent, null)
//                                }
//                            }
//                        },
//                        shape = RoundedCornerShape(24.dp),
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = MaterialTheme.colorScheme.primary,
//                            contentColor = MaterialTheme.colorScheme.onPrimary
//                        ),
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(52.dp)
//                            .padding(horizontal = 16.dp)
//                    ) {
//                        Text(
//                            text = when {
//                                !isDownloading && !isDownloadComplete -> "Download update"
//                                isDownloading -> "Pause"
//                                isDownloadComplete -> "Install"
//                                else -> "Download update"
//                            },
//                            style = MaterialTheme.typography.labelLarge.copy(
//                                fontWeight = FontWeight.Medium
//                            )
//                        )
//                    }
//                }
//            }
//        }
//    }
// }
