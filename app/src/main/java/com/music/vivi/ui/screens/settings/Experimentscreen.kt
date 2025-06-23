package com.music.vivi.ui.screens.settings

import android.content.ActivityNotFoundException
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.music.vivi.ui.theme.viviTheme
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
import androidx.compose.foundation.rememberScrollState

import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

import androidx.compose.material.icons.filled.Delete // or use another available icon
import com.music.vivi.MainActivity

@Composable
fun SettingsListItem(
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingContent?.let {
                Box(modifier = Modifier.padding(end = 16.dp)) {
                    it()
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                value?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            trailingContent?.let {
                Box(modifier = Modifier.padding(start = 16.dp)) {
                    it()
                }
            }
        }
    }
}


// Your imports here
// ...

private const val PREFS_NAME = "app_settings"
private const val KEY_AUTO_UPDATE_CHECK = "auto_update_check_enabled"
private const val KEY_BETA_UPDATER_ENABLED = "beta_updater_enabled"
private const val KEY_UPDATE_CHECK_INTERVAL = "update_check_interval"
private const val DEFAULT_UPDATE_CHECK_INTERVAL = 4 // hours

private fun getUpdateCheckInterval(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_UPDATE_CHECK_INTERVAL, DEFAULT_UPDATE_CHECK_INTERVAL)
}

private fun saveUpdateCheckInterval(context: Context, hours: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_UPDATE_CHECK_INTERVAL, hours).apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentalSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var autoUpdateCheckEnabled by remember { mutableStateOf(getAutoUpdateCheckSetting(context)) }
    var betaUpdaterEnabled by remember { mutableStateOf(getBetaUpdaterSetting(context)) }
    var updateCheckInterval by remember { mutableStateOf(getUpdateCheckInterval(context)) }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.experimental))
    val partyComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.gmailfeedback))
    val confettiComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.githubfeedback))

    val resetSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showResetSheet by remember { mutableStateOf(false) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    val intervalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showIntervalSheet by remember { mutableStateOf(false) }

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
                Text("Send Feedback", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                Text("Choose how you want to send your feedback.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = {
                    sendFeedbackEmail(context)
                    coroutineScope.launch {
                        bottomSheetState.hide(); showBottomSheet = false
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LottieAnimation(composition = partyComposition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(40.dp).padding(end = 8.dp))
                        Text("Send Email")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(onClick = {
                    openGitHubIssues(context)
                    coroutineScope.launch {
                        bottomSheetState.hide(); showBottomSheet = false
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LottieAnimation(composition = confettiComposition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(40.dp).padding(end = 8.dp))
                        Text("Report on GitHub")
                    }
                }
            }
        }
    }

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
                Text("Set Update Check Interval", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("How often should we check for updates?", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = updateCheckInterval.toFloat(),
                    onValueChange = { updateCheckInterval = it.toInt() },
                    valueRange = 1f..24f,
                    steps = 23,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("$updateCheckInterval hours", modifier = Modifier.align(Alignment.CenterHorizontally))

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = {
                        coroutineScope.launch {
                            intervalSheetState.hide()
                            showIntervalSheet = false
                        }
                    }, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }

                    Button(onClick = {
                        saveUpdateCheckInterval(context, updateCheckInterval)
                        coroutineScope.launch {
                            intervalSheetState.hide()
                            showIntervalSheet = false
                        }
                    }, modifier = Modifier.weight(1f)) {
                        Text("Save")
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Experimental Features") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsListItem(
                title = "Auto Check for Updates",
                value = if (autoUpdateCheckEnabled) "Enabled" else "Disabled",
                onClick = {
                    autoUpdateCheckEnabled = !autoUpdateCheckEnabled
                    saveAutoUpdateCheckSetting(context, autoUpdateCheckEnabled)
                },
                trailingContent = {
                    Switch(
                        checked = autoUpdateCheckEnabled,
                        onCheckedChange = { isChecked ->
                            autoUpdateCheckEnabled = isChecked
                            saveAutoUpdateCheckSetting(context, isChecked)
                        }
                    )
                }
            )

            if (autoUpdateCheckEnabled) {
                SettingsListItem(
                    title = "Update Check Interval",
                    value = "$updateCheckInterval hours",
                    onClick = {
                        coroutineScope.launch {
                            showIntervalSheet = true
                            intervalSheetState.show()
                        }
                    }
                )
            }

            SettingsListItem(
                title = "Beta Updater",
                value = if (betaUpdaterEnabled) "Enabled" else "Disabled",
                trailingContent = {
                    Switch(
                        checked = betaUpdaterEnabled,
                        onCheckedChange = { isChecked ->
                            betaUpdaterEnabled = isChecked
                            saveBetaUpdaterSetting(context, isChecked)
                        }
                    )
                }
            )

            if (betaUpdaterEnabled) {
                SettingsListItem(
                    title = "Beta-Updater",
                    value = "Enabled",
                    onClick = {
                        navController.navigate("settings/dpi")
                    }
                )
            }

            SettingsListItem(
                title = "Reset App",
                value = "Clear all settings & data",
                onClick = {
                    coroutineScope.launch {
                        showResetSheet = true
                        resetSheetState.show()
                    }
                },
                trailingContent = {
                    Icon(Icons.Default.Delete, contentDescription = "Reset App")
                }
            )

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
                        Text("Reset App?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("If you face bug do it. This will reset all your settings, clear cache, and remove local data. This action cannot be undone.", style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        resetSheetState.hide()
                                        showResetSheet = false
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Close")
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
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reset")
                            }
                        }
                    }
                }
            }

            SettingsListItem(
                title = "Debug Mode",
                value = "Not Enabled",
                onClick = { }
            )

            SettingsListItem(
                title = "Send Feedback",
                value = "Gmail and Github",
                onClick = {
                    coroutineScope.launch {
                        showBottomSheet = true
                        bottomSheetState.show()
                    }
                },
                trailingContent = {
                    Icon(imageVector = Icons.Default.Feedback, contentDescription = "Send Feedback")
                }
            )
        }
    }
}

@Composable
private fun SheetDragHandle() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .size(width = 40.dp, height = 4.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
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

fun saveAutoUpdateCheckSetting(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
        putBoolean(KEY_AUTO_UPDATE_CHECK, enabled)
        apply()
    }
}

fun getAutoUpdateCheckSetting(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_AUTO_UPDATE_CHECK, true)
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