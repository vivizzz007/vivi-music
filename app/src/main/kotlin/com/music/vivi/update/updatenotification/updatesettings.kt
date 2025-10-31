package com.music.vivi.update.updatenotification


import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.music.vivi.utils.rememberPreference


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UpdateInfoScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current

    // State for preferences
    val (checkForUpdates, onCheckForUpdatesChange) = rememberPreference(CheckForUpdatesKey, true)
    val (showUpdateNotification, onShowUpdateNotificationChange) = rememberPreference(ShowUpdateNotificationKey, true)
    var updateCheckInterval by remember { mutableStateOf(getUpdateCheckInterval(context)) }

    // Function to handle automatic update check toggle
    fun onAutoUpdateCheckChange(newValue: Boolean) {
        onCheckForUpdatesChange(newValue)

        // Save to SharedPreferences
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean(KEY_AUTO_UPDATE_CHECK, newValue).apply()

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
            "Update check interval set to $newInterval ${if (newInterval == 1) "hour" else "hours"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
//                    Text(
//                        text = stringResource(R.string.updater),
//                        style = MaterialTheme.typography.headlineSmall,
//                        fontWeight = FontWeight.Normal
//                    )
                },
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
                    containerColor = MaterialTheme.colorScheme.background
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Update Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Automatically check for new app updates",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Auto Update Check Toggle Card
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
                    Text(
                        text = stringResource(R.string.check_for_updates),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ModernSwitch(
                        checked = checkForUpdates,
                        onCheckedChange = { onAutoUpdateCheckChange(it) }
                    )
                }
            }

            // Show Update Notification Toggle Card
            AnimatedVisibility(visible = checkForUpdates) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

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
                                .clickable { onNotificationToggle(!showUpdateNotification) }
                                .padding(horizontal = 24.dp, vertical = 20.dp),
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
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            ModernSwitch(
                                checked = showUpdateNotification,
                                onCheckedChange = { onNotificationToggle(it) }
                            )
                        }
                    }
                }
            }

            // Update Check Interval Card (only shown when automatic check is enabled)
            AnimatedVisibility(visible = checkForUpdates) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "Update check interval",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Check every $updateCheckInterval ${if (updateCheckInterval == 1) "hour" else "hours"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = when (updateCheckInterval) {
                                    1 -> "$updateCheckInterval hour"
                                    else -> "$updateCheckInterval hours"
                                },
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Slider(
                                value = updateCheckInterval.toFloat(),
                                onValueChange = { updateCheckInterval = it.toInt() },
                                onValueChangeFinished = { onIntervalChange(updateCheckInterval) },
                                valueRange = 1f..24f,
                                steps = 22,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

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
                                    text = "24 hours",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Add these constants where you have your other preference keys
const val KEY_SHOW_UPDATE_NOTIFICATION = "show_update_notification"
val ShowUpdateNotificationKey = booleanPreferencesKey("show_update_notification")