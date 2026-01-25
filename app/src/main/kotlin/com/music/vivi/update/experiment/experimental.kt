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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // Crash log states
    var crashLogs by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedCrashLog by remember { mutableStateOf<File?>(null) }
    var showCrashLogSheet by remember { mutableStateOf(false) }
    val crashLogSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCrashLogViewerSheet by remember { mutableStateOf(false) }
    val crashLogViewerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Load APK info and crash logs on launch
    LaunchedEffect(Unit) {
        val (count, size) = getDownloadedApksInfo(context)
        downloadedApksCount = count
        downloadedApksSize = size
        crashLogs = CrashLogHandler.getAllCrashLogs(context)
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
                    stringResource(R.string.select_apk_variant),
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
                                    stringResource(R.string.unable_to_fetch_apk_variants),
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
                                                contentDescription = stringResource(R.string.selected),
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
                    stringResource(R.string.send_feedback),
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
                        Text(stringResource(R.string.send_email), style = MaterialTheme.typography.labelLarge)
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
                        Text(stringResource(R.string.report_on_github), style = MaterialTheme.typography.labelLarge)
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
                    stringResource(R.string.reset_app_data_question),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.reset_app_data_description),
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
                        Text(stringResource(R.string.cancel))
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
                        Text(stringResource(R.string.reset), color = MaterialTheme.colorScheme.onError)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Crash Logs List Bottom Sheet
    if (showCrashLogSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    crashLogSheetState.hide()
                    showCrashLogSheet = false
                }
            },
            sheetState = crashLogSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.crash_logs_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    if (crashLogs.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                CrashLogHandler.clearAllCrashLogs(context)
                                crashLogs = emptyList()
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.all_crash_logs_cleared),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(R.string.clear_all), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (crashLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.no_crash_logs_found),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.app_hasnt_crashed_recently),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        "${crashLogs.size} crash log(s) found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        crashLogs.forEach { logFile ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    selectedCrashLog = logFile
                                    showCrashLogViewerSheet = true
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BugReport,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            logFile.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            formatFileDate(context, logFile.lastModified()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            CrashLogHandler.deleteCrashLog(logFile)
                                            crashLogs = CrashLogHandler.getAllCrashLogs(context)
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.crash_log_deleted),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.delete_content_desc),
                                            tint = MaterialTheme.colorScheme.error
                                        )
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

    // Crash Log Viewer Bottom Sheet
    if (showCrashLogViewerSheet && selectedCrashLog != null) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    crashLogViewerSheetState.hide()
                    showCrashLogViewerSheet = false
                    selectedCrashLog = null
                }
            },
            sheetState = crashLogViewerSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            selectedCrashLog!!.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            formatFileDate(context, selectedCrashLog!!.lastModified()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            shareLogFile(context, selectedCrashLog!!)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.share_label), style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = {
                            CrashLogHandler.deleteCrashLog(selectedCrashLog!!)
                            crashLogs = CrashLogHandler.getAllCrashLogs(context)
                            Toast.makeText(
                                context,
                                context.getString(R.string.crash_log_deleted),
                                Toast.LENGTH_SHORT
                            ).show()
                            coroutineScope.launch {
                                crashLogViewerSheetState.hide()
                                showCrashLogViewerSheet = false
                                selectedCrashLog = null
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.delete_button_menu), style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            selectedCrashLog!!.readText(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
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
                    text = stringResource(R.string.experimental),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.advanced_settings_experimental),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Spacer(modifier = Modifier.height(24.dp))

            // Updates Section using SplicedColumnGroup pattern
            SplicedColumnGroup(
                title = stringResource(R.string.updates_title),
                content = listOf {
                    ModernInfoItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.surfaceTint
                            )
                        },
                        title = stringResource(R.string.apk_variant),
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
            )

            Spacer(Modifier.height(16.dp))

            // Storage Section using SplicedColumnGroup pattern
            SplicedColumnGroup(
                title = stringResource(R.string.storage_section),
                content = listOf {
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
                        title = stringResource(R.string.clear_downloaded_apks),
                        subtitle = if (downloadedApksCount > 0) {
                            val sizeInMB = String.format("%.1f", downloadedApksSize / (1024.0 * 1024.0))
                            "$downloadedApksCount file(s) · $sizeInMB MB"
                        } else {
                            stringResource(R.string.no_downloaded_files)
                        },
                        onClick = {
                            if (downloadedApksCount > 0) {
                                val success = clearDownloadedApks(context)
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.apk_files_cleared_successfully),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    val (count, size) = getDownloadedApksInfo(context)
                                    downloadedApksCount = count
                                    downloadedApksSize = size
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.failed_to_clear_apk_files),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.no_apk_files_to_clear),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        showArrow = true,
                        showSettingsIcon = true
                    )
                }
            )

            Spacer(Modifier.height(16.dp))

            // Debugging Section
            SplicedColumnGroup(
                title = stringResource(R.string.debugging_section),
                content = listOf(
                    {
                        ModernInfoItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (crashLogs.isNotEmpty()) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.surfaceTint
                                    }
                                )
                            },
                            title = stringResource(R.string.crash_logs),
                            subtitle = if (crashLogs.isNotEmpty()) {
                                "${crashLogs.size} crash log(s) available"
                            } else {
                                context.getString(R.string.no_crashes_detected)
                            },
                            onClick = {
                                coroutineScope.launch {
                                    crashLogs = CrashLogHandler.getAllCrashLogs(context)
                                    showCrashLogSheet = true
                                    crashLogSheetState.show()
                                }
                            },
                            showArrow = true,
                            showSettingsIcon = true
                        )
                    },
                    {
                        ModernInfoItem(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.folder_crash),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.surfaceTint
                                )
                            },
                            title = stringResource(R.string.open_crash_logs_folder),
                            subtitle = stringResource(R.string.crash_logs_folder_path),
                            onClick = {
                                openCrashLogsFolder(context)
                            },
                            showArrow = true,
                            showSettingsIcon = true
                        )
                    }
                )
            )

            Spacer(Modifier.height(16.dp))

            // Support Section using SplicedColumnGroup pattern with multiple items
            SplicedColumnGroup(
                title = stringResource(R.string.support_section),
                content = listOf(
                    {
                        ModernInfoItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Feedback,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.surfaceTint
                                )
                            },
                            title = stringResource(R.string.send_feedback_title),
                            subtitle = stringResource(R.string.email_or_github_issue),
                            onClick = {
                                coroutineScope.launch {
                                    showBottomSheet = true
                                    bottomSheetState.show()
                                }
                            },
                            showArrow = true,
                            showSettingsIcon = true
                        )
                    },
                    {
                        ModernInfoItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            title = stringResource(R.string.reset_app),
                            subtitle = stringResource(R.string.clear_all_settings_and_data),
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
                )
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SplicedColumnGroup(modifier: Modifier = Modifier, title: String = "", content: List<@Composable () -> Unit>) {
    if (content.isEmpty()) return

    val cornerRadius = 16.dp
    val connectionRadius = 5.dp

    // Define shapes for different positions
    val topShape = RoundedCornerShape(
        topStart = cornerRadius,
        topEnd = cornerRadius,
        bottomStart = connectionRadius,
        bottomEnd = connectionRadius
    )
    val middleShape = RoundedCornerShape(connectionRadius)
    val bottomShape = RoundedCornerShape(
        topStart = connectionRadius,
        topEnd = connectionRadius,
        bottomStart = cornerRadius,
        bottomEnd = cornerRadius
    )
    val singleShape = RoundedCornerShape(cornerRadius)

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Group title
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }

        // The container for setting items
        Column(
            modifier = Modifier.clip(
                // Clip the whole column to ensure content stays within the rounded bounds
                if (content.size == 1) singleShape else RoundedCornerShape(cornerRadius)
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            content.forEachIndexed { index, itemContent ->
                // Determine the shape based on the item's position
                val shape = when {
                    content.size == 1 -> singleShape
                    index == 0 -> topShape
                    index == content.size - 1 -> bottomShape
                    else -> middleShape
                }

                // Apply background with the correct shape to the item
                Column(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer, shape)
                ) {
                    itemContent()
                }
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

// Utility functions
fun clearDownloadedApks(context: Context): Boolean = try {
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

fun getDownloadedApksInfo(context: Context): Pair<Int, Long> = try {
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

private fun openGitHubIssues(context: Context) {
    try {
        val url = "https://github.com/vivizzz007/vivi-music/issues"
        val githubIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        Log.d("ExperimentalSettings", "Attempting to open: $url")

        val chooserIntent = Intent.createChooser(githubIntent, context.getString(R.string.open_with))

        try {
            context.startActivity(chooserIntent)
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(githubIntent)
            } catch (e: ActivityNotFoundException) {
                Log.e("ExperimentalSettings", "No browser installed", e)
                Toast.makeText(
                    context,
                    context.getString(R.string.please_install_web_browser),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    } catch (e: Exception) {
        Log.e("ExperimentalSettings", "Error opening GitHub: ${e.message}", e)
        Toast.makeText(
            context,
            context.getString(R.string.error_message, e.localizedMessage ?: ""),
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun sendFeedbackEmail(context: Context) {
    try {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data =
                Uri.parse(
                    "mailto:mkmdevilmi@gmail.com?subject=${Uri.encode(
                        context.getString(R.string.feedback_for_vivi_music_app)
                    )}&body=${Uri.encode("Please write your feedback here...")}"
                )
        }

        if (emailIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(emailIntent)
        } else {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("mkmdevilmi@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.feedback_for_vivi_music_app))
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.please_write_feedback))
            }

            if (fallbackIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(
                    Intent.createChooser(fallbackIntent, context.getString(R.string.choose_email_app))
                )
            } else {
                Toast.makeText(context, context.getString(R.string.no_email_app_found), Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.unable_to_open_email_app, e.message ?: ""),
            Toast.LENGTH_SHORT
        ).show()
    }
}

fun getSelectedApkVariant(context: Context): String = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    .getString(KEY_SELECTED_APK_VARIANT, DEFAULT_APK_VARIANT) ?: DEFAULT_APK_VARIANT

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

suspend fun fetchAvailableApkVariants(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
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

private fun formatFileDate(context: Context, timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diff = now.time - timestamp

    return when {
        diff < 60000 -> context.getString(R.string.just_now)
        diff < 3600000 -> "${diff / 60000} minutes ago"
        diff < 86400000 -> "${diff / 3600000} hours ago"
        diff < 604800000 -> "${diff / 86400000} days ago"
        else -> SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(date)
    }
}

private fun shareLogFile(context: Context, file: File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.crash_log_subject, file.name))
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.crash_log_attached))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_crash_log)))
    } catch (e: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.error_sharing_log, e.message ?: ""),
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun openCrashLogsFolder(context: Context) {
    try {
        val crashLogsDir = CrashLogHandler.getCrashLogsDir(context)

        // Try using FileProvider to open the directory
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.FileProvider",
            crashLogsDir
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "resource/folder")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Fallback 1: try without specific type but with Uri
            val fallback1 = Intent(Intent.ACTION_VIEW).apply {
                setData(uri)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(fallback1)
            } catch (e2: ActivityNotFoundException) {
                // Fallback 2: Show path and copy to clipboard
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText(
                    context.getString(R.string.crash_log_path),
                    crashLogsDir.absolutePath
                )
                clipboard.setPrimaryClip(clip)

                Toast.makeText(
                    context,
                    "No file manager found. Path copied to clipboard:\n${crashLogsDir.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Error opening folder: ${e.message}",
            Toast.LENGTH_SHORT
        ).show()
    }
}
