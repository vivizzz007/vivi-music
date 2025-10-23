package com.music.vivi.update.experiment



import android.app.NotificationManager
import android.content.ActivityNotFoundException
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.music.vivi.R
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.compose.material.icons.filled.Feedback
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch
import android.util.Log
import java.io.File


import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.music.vivi.MainActivity
import com.music.vivi.ui.screens.getAutoUpdateCheckSetting
import com.music.vivi.update.mordernswitch.ModernSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL


@Composable
fun SettingsListItem(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    isHighlighted: Boolean = false,
    isLast: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (subtitle != null) 72.dp else 64.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        color = if (isHighlighted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        }
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingContent?.let {
                    Box(modifier = Modifier.padding(end = 16.dp)) {
                        it()
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    subtitle?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }

                trailingContent?.let {
                    Box(modifier = Modifier.padding(start = 12.dp)) {
                        it()
                    }
                }
            }

            if (!isLast) {
                Divider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                content()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Utility functions for clearing APKs
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentalSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val titleAlpha by remember {
        derivedStateOf {
            1f - (scrollState.value / 200f).coerceIn(0f, 1f)
        }
    }

    val titleScale by remember {
        derivedStateOf {
            1f - (scrollState.value / 400f).coerceIn(0f, 0.3f)
        }
    }

    var autoUpdateCheckEnabled by remember { mutableStateOf(getAutoUpdateCheckSetting(context)) }
    var betaUpdaterEnabled by remember { mutableStateOf(getBetaUpdaterSetting(context)) }
    var updateCheckInterval by remember { mutableStateOf(getUpdateCheckInterval(context)) }
    var selectedApkVariant by remember { mutableStateOf(getSelectedApkVariant(context)) }
    var downloadedApksCount by remember { mutableStateOf(0) }
    var downloadedApksSize by remember { mutableStateOf(0L) }

    val resetSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showResetSheet by remember { mutableStateOf(false) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    val intervalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showIntervalSheet by remember { mutableStateOf(false) }

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
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().apply()

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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
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
                                        .padding(vertical = 6.dp)
                                        .clickable {
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
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

    // Update Interval Bottom Sheet
    if (showIntervalSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    intervalSheetState.hide()
                    showIntervalSheet = false
                }
            },
            sheetState = intervalSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    "Update check interval",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose how often to check for app updates automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "$updateCheckInterval hours",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Slider(
                            value = updateCheckInterval.toFloat(),
                            onValueChange = { updateCheckInterval = it.toInt() },
                            valueRange = 1f..24f,
                            steps = 23,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                intervalSheetState.hide()
                                showIntervalSheet = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            saveUpdateCheckInterval(context, updateCheckInterval)
                            coroutineScope.launch {
                                intervalSheetState.hide()
                                showIntervalSheet = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save")
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .graphicsLayer {
                            alpha = titleAlpha
                            scaleX = titleScale
                            scaleY = titleScale
                        }
                ) {
                    Text(
                        text = "Experimental features",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                SettingsSection("Updates") {
                    SettingsListItem(
                        title = "APK variant",
                        subtitle = selectedApkVariant,
                        onClick = {
                            coroutineScope.launch {
                                showVariantSheet = true
                                variantSheetState.show()
                            }
                        },
                        isLast = true,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection("Storage") {
                    SettingsListItem(
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
                        isLast = true,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear APKs",
                                tint = if (downloadedApksCount > 0) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection("Beta features") {
                    SettingsListItem(
                        title = "Beta updater",
                        subtitle = "Automatically applies updates to more features for improved functionality",
                        isHighlighted = false,
                        isLast = !betaUpdaterEnabled,
                        trailingContent = {
                            ModernSwitch(
                                checked = betaUpdaterEnabled,
                                onCheckedChange = { isChecked ->
                                    betaUpdaterEnabled = isChecked
                                    saveBetaUpdaterSetting(context, isChecked)
                                }
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection("Support") {
                    SettingsListItem(
                        title = "Send feedback",
                        subtitle = "Email or GitHub issue",
                        onClick = {
                            coroutineScope.launch {
                                showBottomSheet = true
                                bottomSheetState.show()
                            }
                        },
                        isLast = false,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.Feedback,
                                contentDescription = "Send Feedback",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )

                    SettingsListItem(
                        title = "Reset app",
                        subtitle = "Clear all settings and data",
                        onClick = {
                            coroutineScope.launch {
                                showResetSheet = true
                                resetSheetState.show()
                            }
                        },
                        isLast = true,
                        trailingContent = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Reset App",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(50.dp))
            }
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

fun saveBetaUpdaterSetting(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
        putBoolean(KEY_BETA_UPDATER_ENABLED, enabled)
        apply()
    }
}

fun getBetaUpdaterSetting(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_BETA_UPDATER_ENABLED, false)
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
const val KEY_BETA_UPDATER_ENABLED = "beta_updater_enabled"
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