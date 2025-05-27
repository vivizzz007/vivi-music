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
import android.widget.Toast
import androidx.compose.material.icons.filled.Feedback
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch
import android.util.Log
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
    val coroutineScope = rememberCoroutineScope()

    var autoUpdateCheckEnabled by remember { mutableStateOf(getAutoUpdateCheckSetting(context)) }
    var betaUpdaterEnabled by remember { mutableStateOf(getBetaUpdaterSetting(context)) }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.experimental))
    val partyComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.gmailfeedback))
    val confettiComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.githubfeedback))

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    bottomSheetState.hide()
                    showBottomSheet = false
                }
            },
            sheetState = bottomSheetState,
            dragHandle = {
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Send Feedback",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text("Choose how you want to send your feedback.", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        sendFeedbackEmail(context)
                        coroutineScope.launch {
                            bottomSheetState.hide()
                            showBottomSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LottieAnimation(
                            composition = partyComposition,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier
                                .size(40.dp)
                                .padding(end = 8.dp)
                        )
                        Text("Send Email")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        openGitHubIssues(context)
                        coroutineScope.launch {
                            bottomSheetState.hide()
                            showBottomSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LottieAnimation(
                            composition = confettiComposition,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier
                                .size(40.dp)
                                .padding(end = 8.dp)
                        )
                        Text("Report on GitHub")
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

//            SettingsListItem(
//                title = "Beta Updater",
//                value = if (betaUpdaterEnabled) "Enabled" else "Disabled",
//                trailingContent = {
//                    Switch(
//                        checked = betaUpdaterEnabled,
//                        onCheckedChange = { isChecked ->
//                            betaUpdaterEnabled = isChecked
//                            saveBetaUpdaterSetting(context, isChecked)
//                        }
//                    )
//                }
//            )

            if (betaUpdaterEnabled) {
                SettingsListItem(
                    title = "Beta-Updater",
                    value = "Enabled",
                    onClick = {
                        navController.navigate("settings/dpi") //settings/dpi
                    }
                )
            }

//            Spacer(modifier = Modifier.height(8.dp))

//            SettingsListItem(
//                title = "Beta Updater",
//                value = "Beta update here",
//                onClick = {
//                    navController.navigate("") //settings/pixel_updater
//                }
//            )

//            SettingsListItem(
//                title = "Debug Mode",
//                value = "Not Enabled",
//                onClick = { }
//            )

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
                    Icon(
                        imageVector = Icons.Default.Feedback,
                        contentDescription = "Send Feedback"
                    )
                }
            )
        }
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

private const val PREFS_NAME = "app_settings"
private const val KEY_AUTO_UPDATE_CHECK = "auto_update_check_enabled"
private const val KEY_BETA_UPDATER_ENABLED = "beta_updater_enabled"

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

fun saveBetaUpdaterSetting(context: Context, enabled: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    with(sharedPrefs.edit()) {
        putBoolean(KEY_BETA_UPDATER_ENABLED, enabled)
        apply()
    }
}

fun getBetaUpdaterSetting(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_BETA_UPDATER_ENABLED, false)
}