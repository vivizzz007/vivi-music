package com.music.vivi.update.experiment

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.MainActivity
import com.music.vivi.R
import com.music.vivi.update.settingstyle.ModernInfoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentalSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    var selectedApkVariant by remember { mutableStateOf(getSelectedApkVariant(context)) }
    var downloadedApksCount by remember { mutableStateOf(0) }
    var downloadedApksSize by remember { mutableStateOf(0L) }

    val resetSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showResetSheet by remember { mutableStateOf(false) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    val variantSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showVariantSheet by remember { mutableStateOf(false) }

    // Load APK info on launch
    LaunchedEffect(Unit) {
        val (count, size) = getDownloadedApksInfo(context)
        downloadedApksCount = count
        downloadedApksSize = size
    }

    fun resetApp(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE).edit().clear().apply()

        try {
            context.cacheDir?.deleteRecursively()
        } catch (_: Exception) {}

        try {
            File(context.applicationInfo.dataDir).listFiles()?.forEach { file ->
                if (file.name != "lib") file.deleteRecursively()
            }
        } catch (_: Exception) {}

        context.deleteDatabase("your_database_name.db")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    // APK Variant Selection Bottom Sheet
    if (showVariantSheet) {
        var apkVariants by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
        var isLoadingVariants by remember { mutableStateOf(true) }
        var loadError by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            isLoadingVariants = true
            loadError = false
            try {
                apkVariants = fetchAvailableApkVariants()
                isLoadingVariants = false
            } catch (e: Exception) {
                loadError = true
                isLoadingVariants = false
                apkVariants = listOf(DEFAULT_APK_VARIANT to "Universal (Recommended)")
            }
        }

        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    variantSheetState.hide()
                    showVariantSheet = false
                }
            },
            sheetState = variantSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    "Select APK variant",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose which APK file to download during updates. These are the available variants from the latest release.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                when {
                    isLoadingVariants -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Fetching available APK variants...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    loadError -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Unable to fetch APK variants",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Using default variant: vivi.apk",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    else -> {
                        if (apkVariants.isEmpty()) {
                            Text(
                                "No APK variants found in the latest release.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            apkVariants.forEach { (variant, description) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    onClick = {
                                        selectedApkVariant = variant
                                        saveSelectedApkVariant(context, variant)
                                        coroutineScope.launch {
                                            variantSheetState.hide()
                                            showVariantSheet = false
                                        }
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedApkVariant == variant) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        }
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                variant,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = if (selectedApkVariant == variant) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (selectedApkVariant == variant) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                        if (selectedApkVariant == variant) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Feedback Bottom Sheet
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    bottomSheetState.hide()
                    showBottomSheet = false
                }
            },
            sheetState = bottomSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    "Send Feedback",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "Choose how you want to send your feedback.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        sendFeedbackEmail(context)
                        coroutineScope.launch {
                            bottomSheetState.hide()
                            showBottomSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text("Send Email", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        openGitHubIssues(context)
                        coroutineScope.launch {
                            bottomSheetState.hide()
                            showBottomSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text("Report on GitHub", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Reset App Bottom Sheet
    if (showResetSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    resetSheetState.hide()
                    showResetSheet = false
                }
            },
            sheetState = resetSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    "Reset app data?",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This will clear all settings, cache, and local data. The app will restart and you'll need to set up everything again. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                resetSheetState.hide()
                                showResetSheet = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            resetApp(context)
                            coroutineScope.launch {
                                resetSheetState.hide()
                                showResetSheet = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset", color = MaterialTheme.colorScheme.onError)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.largeTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Experimental",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Advanced settings and experimental features",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Spacer(modifier = Modifier.height(24.dp))

            // Updates Section
            Text(
                text = "Updates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // Settings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    // APK Variant
                    ModernInfoItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.surfaceTint
                            )
                        },
                        title = "APK variant",
                        subtitle = selectedApkVariant,
                        onClick = {
                            coroutineScope.launch {
                                showVariantSheet = true
                                variantSheetState.show()
                            }
                        },
                        showArrow = true,
                        showSettingsIcon = true
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Storage Section
            Text(
                text = "Storage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // Storage Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    // Clear APKs
                    ModernInfoItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = if (downloadedApksCount > 0) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.surfaceTint
                                }
                            )
                        },
                        title = "Clear downloaded APKs",
                        subtitle = if (downloadedApksCount > 0) {
                            val sizeInMB = String.format("%.1f", downloadedApksSize / (1024.0 * 1024.0))
                            "$downloadedApksCount file(s) · $sizeInMB MB"
                        } else {
                            "No downloaded files"
                        },
                        onClick = {
                            if (downloadedApksCount > 0) {
                                val success = clearDownloadedApks(context)
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "APK files cleared successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    val (count, size) = getDownloadedApksInfo(context)
                                    downloadedApksCount = count
                                    downloadedApksSize = size
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to clear APK files",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "No APK files to clear",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        showArrow = true,
                        showSettingsIcon = true
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Support Section
            Text(
                text = "Support",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // Support Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    // Send Feedback
                    ModernInfoItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Feedback,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.surfaceTint
                            )
                        },
                        title = "Send feedback",
                        subtitle = "Email or GitHub issue",
                        onClick = {
                            coroutineScope.launch {
                                showBottomSheet = true
                                bottomSheetState.show()
                            }
                        },
                        showArrow = true,
                        showSettingsIcon = true
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // Reset App
                    ModernInfoItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        title = "Reset app",
                        subtitle = "Clear all settings and data",
                        onClick = {
                            coroutineScope.launch {
                                showResetSheet = true
                                resetSheetState.show()
                            }
                        },
                        showArrow = true,
                        showSettingsIcon = true,
                        iconBackgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SheetDragHandle() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .size(width = 32.dp, height = 4.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}

// Utility functions
fun clearDownloadedApks(context: Context): Boolean {
    return try {
        val downloadsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "")
        var deletedCount = 0
        var totalSize = 0L

        downloadsDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".apk")) {
                totalSize += file.length()
                if (file.delete()) {
                    deletedCount++
                }
            }
        }

        Log.d("ClearAPK", "Cleared $deletedCount APK files, freed ${totalSize / (1024.0 * 1024.0)} MB")
        true
    } catch (e: Exception) {
        Log.e("ClearAPK", "Error clearing APKs: ${e.message}")
        false
    }
}

fun getDownloadedApksInfo(context: Context): Pair<Int, Long> {
    return try {
        val downloadsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "")
        var count = 0
        var totalSize = 0L

        downloadsDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".apk")) {
                count++
                totalSize += file.length()
            }
        }

        Pair(count, totalSize)
    } catch (e: Exception) {
        Pair(0, 0L)
    }
}

private fun openGitHubIssues(context: Context) {
    try {
        val url = "https://github.com/vivizzz007/vivi-music/issues"
        val githubIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        Log.d("ExperimentalSettings", "Attempting to open: $url")

        val chooserIntent = Intent.createChooser(githubIntent, "Open with")

        try {
            context.startActivity(chooserIntent)
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(githubIntent)
            } catch (e: ActivityNotFoundException) {
                Log.e("ExperimentalSettings", "No browser installed", e)
                Toast.makeText(context, "Please install a web browser", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        Log.e("ExperimentalSettings", "Error opening GitHub: ${e.message}", e)
        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

private fun sendFeedbackEmail(context: Context) {
    try {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:mkmdevilmi@gmail.com?subject=${Uri.encode("Feedback for Vivi Music App")}&body=${Uri.encode("Please write your feedback here...")}")
        }

        if (emailIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(emailIntent)
        } else {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("mkmdevilmi@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Feedback for Vivi Music App")
                putExtra(Intent.EXTRA_TEXT, "Please write your feedback here...")
            }

            if (fallbackIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(Intent.createChooser(fallbackIntent, "Choose Email App"))
            } else {
                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to open email app: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun getSelectedApkVariant(context: Context): String {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_SELECTED_APK_VARIANT, DEFAULT_APK_VARIANT) ?: DEFAULT_APK_VARIANT
}

fun saveSelectedApkVariant(context: Context, variant: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_SELECTED_APK_VARIANT, variant)
        .apply()
}

const val PREFS_NAME = "app_settings"
const val KEY_AUTO_UPDATE_CHECK = "auto_update_check_enabled"
const val KEY_UPDATE_CHECK_INTERVAL = "update_check_interval"
const val KEY_SELECTED_APK_VARIANT = "selected_apk_variant"
const val DEFAULT_UPDATE_CHECK_INTERVAL = 4
const val DEFAULT_APK_VARIANT = "vivi.apk"

fun getUpdateCheckInterval(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_UPDATE_CHECK_INTERVAL, DEFAULT_UPDATE_CHECK_INTERVAL)
}

fun saveUpdateCheckInterval(context: Context, hours: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_UPDATE_CHECK_INTERVAL, hours).apply()
}

suspend fun fetchAvailableApkVariants(): List<Pair<String, String>> {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/vivizzz007/vivi-music/releases/latest")
            val json = url.openStream().bufferedReader().use { it.readText() }
            val release = JSONObject(json)
            val assets = release.getJSONArray("assets")
            val apkList = mutableListOf<Pair<String, String>>()

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk")) {
                    val sizeInBytes = asset.getLong("size")
                    val sizeInMB = String.format("%.1f", sizeInBytes / (1024.0 * 1024.0))
                    val description = when {
                        name == "vivi.apk" -> "Universal - $sizeInMB MB (Recommended)"
                        name.contains("arm64-v8a") -> "ARM64 - $sizeInMB MB (Modern phones)"
                        name.contains("armeabi-v7a") -> "ARM - $sizeInMB MB (Older phones)"
                        name.contains("x86_64") -> "x86_64 - $sizeInMB MB (Intel tablets)"
                        name.contains("x86") -> "x86 - $sizeInMB MB (Emulators)"
                        else -> "$sizeInMB MB"
                    }
                    apkList.add(name to description)
                }
            }

            // Sort to put vivi.apk first
            apkList.sortedBy { if (it.first == "vivi.apk") 0 else 1 }
        } catch (e: Exception) {
            Log.e("ExperimentalSettings", "Error fetching APK variants: ${e.message}")
            listOf(DEFAULT_APK_VARIANT to "Universal (Recommended)")
        }
    }
}