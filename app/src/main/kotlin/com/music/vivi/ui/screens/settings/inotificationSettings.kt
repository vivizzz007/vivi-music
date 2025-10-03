package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.music.vivi.BuildConfig
import com.music.vivi.R
import com.music.vivi.constants.CheckForUpdatesKey
import com.music.vivi.constants.UpdateNotificationsEnabledKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.utils.rememberPreference
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.music.vivi.ui.screens.KEY_AUTO_UPDATE_CHECK
import com.music.vivi.ui.screens.PREFS_NAME
import com.music.vivi.update.experiment.SheetDragHandle
import com.music.vivi.update.experiment.getUpdateCheckInterval
import com.music.vivi.update.experiment.saveUpdateCheckInterval
import com.music.vivi.update.updatenotification.UpdateChecker
import com.music.vivi.update.updatenotification.saveUpdateNotificationPreference
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdaterScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State for preferences
    val (checkForUpdates, onCheckForUpdatesChange) = rememberPreference(CheckForUpdatesKey, true)
    val (updateNotifications, onUpdateNotificationsChange) = rememberPreference(UpdateNotificationsEnabledKey, true)
    var updateCheckInterval by remember { mutableStateOf(getUpdateCheckInterval(context)) }

    // Bottom sheet state
    val intervalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showIntervalSheet by remember { mutableStateOf(false) }

    // Function to handle automatic update check toggle
    fun onAutoUpdateCheckChange(newValue: Boolean) {
        onCheckForUpdatesChange(newValue)

        // Save to SharedPreferences
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean(KEY_AUTO_UPDATE_CHECK, newValue).apply()

        // Schedule or cancel update checks based on the new value
        if (newValue) {
            UpdateChecker.scheduleUpdateCheck(context, updateCheckInterval.toLong())
            Toast.makeText(context, "Automatic update checks enabled", Toast.LENGTH_SHORT).show()
        } else {
            UpdateChecker.cancelUpdateChecks(context)
            Toast.makeText(context, "Automatic update checks disabled", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to handle notification preference changes
    fun onUpdateNotificationsChangeWithSave(newValue: Boolean) {
        onUpdateNotificationsChange(newValue)
        saveUpdateNotificationPreference(context, newValue)

        val message = if (newValue) {
            "Update notifications enabled"
        } else {
            "Update notifications disabled"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Bottom Sheet for interval selection
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
                            text = when (updateCheckInterval) {
                                1 -> "$updateCheckInterval hour"
                                else -> "$updateCheckInterval hours"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Slider(
                            value = updateCheckInterval.toFloat(),
                            onValueChange = { updateCheckInterval = it.toInt() },
                            valueRange = 1f..24f,
                            steps = 22, // 24 - 2 = 22 (excluding start and end)
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

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            // Reset to saved value
                            updateCheckInterval = getUpdateCheckInterval(context)
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

                            // Reschedule with new interval if auto-update is enabled
                            if (checkForUpdates) {
                                UpdateChecker.scheduleUpdateCheck(context, updateCheckInterval.toLong())
                                Toast.makeText(
                                    context,
                                    "Update check interval set to $updateCheckInterval ${if (updateCheckInterval == 1) "hour" else "hours"}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.updater),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Normal
                    )
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

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "Update notification",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Main settings card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    // Automatic Update Check
                    SettingItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.update),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (checkForUpdates) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        },
                        title = stringResource(R.string.check_for_updates),
                        subtitle = "Automatically check for new updates",
                        trailing = {
                            ModernSwitch(
                                checked = checkForUpdates,
                                onCheckedChange = { onAutoUpdateCheckChange(it) }
                            )
                        },
                        onClick = {
                            onAutoUpdateCheckChange(!checkForUpdates)
                        }
                    )

                    // Update Check Interval (only shown when automatic check is enabled)
                    if (checkForUpdates) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingItem(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.shedule),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            title = "Update check interval",
                            subtitle = "Every $updateCheckInterval ${if (updateCheckInterval == 1) "hour" else "hours"}",
                            onClick = {
                                coroutineScope.launch {
                                    showIntervalSheet = true
                                    intervalSheetState.show()
                                }
                            },
                            trailing = {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }

                    // Update Notifications (only shown when automatic check is enabled)
                    if (checkForUpdates) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingItem(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.notification),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (updateNotifications) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            },
                            title = stringResource(R.string.update_notifications),
                            subtitle = "Show notifications when updates are available",
                            trailing = {
                                ModernSwitch(
                                    checked = updateNotifications,
                                    onCheckedChange = { onUpdateNotificationsChangeWithSave(it) },
                                    enabled = checkForUpdates
                                )
                            },
                            onClick = {
                                if (checkForUpdates) {
                                    onUpdateNotificationsChangeWithSave(!updateNotifications)
                                }
                            }
                        )

                        // Manual Check Now button
//                        HorizontalDivider(
//                            modifier = Modifier.padding(start = 56.dp),
//                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
//                        )
//
//                        SettingItem(
//                            icon = {
//                                Icon(
//                                    painter = painterResource(R.drawable.refresh),
//                                    contentDescription = null,
//                                    modifier = Modifier.size(24.dp),
//                                    tint = MaterialTheme.colorScheme.primary
//                                )
//                            },
//                            title = "Check for updates now",
//                            subtitle = "Manually check for the latest version",
//                            onClick = {
//                                UpdateChecker.checkForUpdateNow(context)
//                                Toast.makeText(
//                                    context,
//                                    "Checking for updates...",
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                            },
//                            trailing = {
//                                Icon(
//                                    imageVector = Icons.Default.ChevronRight,
//                                    contentDescription = null,
//                                    tint = MaterialTheme.colorScheme.primary
//                                )
//                            }
//                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Information card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.info),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "About Update Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    InfoItem(
                        title = "Automatic update check",
                        description = "When enabled, the app will check for updates at the specified interval"
                    )

                    InfoItem(
                        title = "Update check interval",
                        description = "Set how frequently the app checks for new versions (1-24 hours)"
                    )

                    InfoItem(
                        title = "Update notifications",
                        description = "Get notified when new versions are available for download"
                    )

//                    InfoItem(
//                        title = "Check now",
//                        description = "Instantly check for updates without waiting for the scheduled check"
//                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current version",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoItem(
    title: String,
    description: String
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "•",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun SettingItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor
                )
            }
        }

        trailing?.invoke()
    }
}