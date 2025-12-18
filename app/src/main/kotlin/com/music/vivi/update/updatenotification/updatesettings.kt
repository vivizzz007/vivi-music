package com.music.vivi.update.updatenotification


import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.constants.CheckForUpdatesKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.screens.KEY_AUTO_UPDATE_CHECK
import com.music.vivi.ui.screens.PREFS_NAME
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.experiment.getUpdateCheckInterval
import com.music.vivi.update.experiment.saveUpdateCheckInterval
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.update.viewmodelupdate.UpdateViewModel
import com.music.vivi.utils.rememberPreference

import androidx.hilt.navigation.compose.hiltViewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateInfoScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    updateViewModel: UpdateViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // State for preferences
    val (checkForUpdates, onCheckForUpdatesChange) = rememberPreference(CheckForUpdatesKey, true)
    val (showUpdateNotification, onShowUpdateNotificationChange) = rememberPreference(ShowUpdateNotificationKey, true)
    var updateCheckInterval by remember { mutableStateOf(getUpdateCheckInterval(context)) }
    var showIntervalDialog by remember { mutableStateOf(false) }

    // Function to handle automatic update check toggle
    fun onAutoUpdateCheckChange(newValue: Boolean) {
        onCheckForUpdatesChange(newValue)

        // Save to SharedPreferences
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean(KEY_AUTO_UPDATE_CHECK, newValue).apply()

        updateViewModel.refreshUpdateStatus()

        // Show feedback
        val message = if (newValue) {
            "Automatic update checks enabled"
        } else {
            "Automatic update checks disabled"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Function to handle notification toggle
    fun onNotificationToggle(newValue: Boolean) {
        onShowUpdateNotificationChange(newValue)

        // Save to SharedPreferences
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean(KEY_SHOW_UPDATE_NOTIFICATION, newValue).apply()

        // Show feedback
        val message = if (newValue) {
            "Update notifications enabled"
        } else {
            "Update notifications disabled"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Function to handle interval change
    fun onIntervalChange(newInterval: Int) {
        updateCheckInterval = newInterval
        saveUpdateCheckInterval(context, newInterval)

        Toast.makeText(
            context,
            if (newInterval < 24) {
                "Update check interval set to $newInterval ${if (newInterval == 1) "hour" else "hours"}"
            } else {
                val days = newInterval / 24
                "Update check interval set to $days ${if (days == 1) "day" else "days"}"
            },
            Toast.LENGTH_SHORT
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Main Title
            Text(
                text = "Update Settings",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Manage Section
            Text(
                text = "Manage",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
            )

            // Auto Update Check Card - Standalone rounded
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAutoUpdateCheckChange(!checkForUpdates) }
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.check_for_updates),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Automatically check for new updates",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    ModernSwitch(
                        checked = checkForUpdates,
                        onCheckedChange = { onAutoUpdateCheckChange(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Notifications Section
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
            )

            // Show Update Notification Card - Top rounded
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = checkForUpdates) {
                            onNotificationToggle(!showUpdateNotification)
                        }
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .alpha(if (checkForUpdates) 1f else 0.5f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show notification",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Get notified when updates are available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    ModernSwitch(
                        checked = showUpdateNotification,
                        onCheckedChange = { onNotificationToggle(it) },
                        enabled = checkForUpdates
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Update Check Interval Card - Bottom rounded
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = checkForUpdates) { showIntervalDialog = true }
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .alpha(if (checkForUpdates) 1f else 0.5f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Update check interval",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (updateCheckInterval < 24) {
                                "Every $updateCheckInterval ${if (updateCheckInterval == 1) "hour" else "hours"}"
                            } else {
                                val days = updateCheckInterval / 24
                                "Every $days ${if (days == 1) "day" else "days"}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Interval Picker Dialog
    if (showIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showIntervalDialog = false },
            title = {
                Text(
                    text = "Update check interval",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    // Change the range and display
                    Text(
                        text = if (updateCheckInterval < 24) {
                            "Check every $updateCheckInterval ${if (updateCheckInterval == 1) "hour" else "hours"}"
                        } else {
                            val days = updateCheckInterval / 24
                            "Check every $days ${if (days == 1) "day" else "days"}"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Slider(
                        value = updateCheckInterval.toFloat(),
                        onValueChange = { updateCheckInterval = it.toInt() },
                        valueRange = 1f..168f,  // Changed: 1 hour to 7 days (168 hours)
                        steps = 166,  // Changed: 167 values minus 1
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1 hour",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "7 days",  // Changed
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onIntervalChange(updateCheckInterval)
                        showIntervalDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIntervalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Constants
const val KEY_SHOW_UPDATE_NOTIFICATION = "show_update_notification"
val ShowUpdateNotificationKey = booleanPreferencesKey("show_update_notification")
//