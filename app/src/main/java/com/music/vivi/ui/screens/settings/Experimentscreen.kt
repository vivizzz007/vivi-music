package com.music.vivi.ui.screens.settings

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentalSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var autoUpdateCheckEnabled by remember {
        mutableStateOf(getAutoUpdateCheckSetting(context))
    }
    var betaUpdaterEnabled by remember {
        mutableStateOf(getBetaUpdaterSetting(context))
    }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.experimental))

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

            // Beta Updater Toggle
            SettingsListItem(
                title = "Beta Updater",
                value = if (betaUpdaterEnabled) "Enabled" else "Disabled",
                onClick = {
                    // Navigate to "settings/update"
                    navController.navigate("settings/update")
                },
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

            // Conditionally show "Updater Beta Vivi"
            if (betaUpdaterEnabled) {
                SettingsListItem(
                    title = "Updater Beta Vivi",
                    value = "Enabled", // Or any relevant value
                    onClick = {
                        // IMPORTANT CHANGE HERE: Navigate when "Updater Beta Vivi" is clicked
                        navController.navigate("settings/pixel_updater")
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsListItem(
                title = "Beta Feature X",
                value = "Coming Soon",
                onClick = { /* Handle click for Beta Feature X */ }
            )

            SettingsListItem(
                title = "Debug Mode",
                value = "Not Enabled",
                onClick = { /* Handle click for Debug Mode */ }
            )
        }
    }
}

// --- Functions to save/load settings using SharedPreferences ---

private const val PREFS_NAME = "app_settings"
private const val KEY_AUTO_UPDATE_CHECK = "auto_update_check_enabled"
private const val KEY_BETA_UPDATER_ENABLED = "beta_updater_enabled" // New key for beta updater

fun saveAutoUpdateCheckSetting(context: Context, enabled: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    with(sharedPrefs.edit()) {
        putBoolean(KEY_AUTO_UPDATE_CHECK, enabled)
        apply()
    }
}

fun getAutoUpdateCheckSetting(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_AUTO_UPDATE_CHECK, true)
}

// New functions for Beta Updater setting
fun saveBetaUpdaterSetting(context: Context, enabled: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    with(sharedPrefs.edit()) {
        putBoolean(KEY_BETA_UPDATER_ENABLED, enabled)
        apply()
    }
}

fun getBetaUpdaterSetting(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_BETA_UPDATER_ENABLED, false) // Default to false (disabled)
}

@Preview(showBackground = true)
@Composable
fun ExperimentalSettingsScreenPreview() {
    viviTheme {
        ExperimentalSettingsScreen(navController = rememberNavController())
    }
}