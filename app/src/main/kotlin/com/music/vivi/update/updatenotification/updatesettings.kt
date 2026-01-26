package com.music.vivi.update.updatenotification

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.constants.CheckForUpdatesKey
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.KEY_SHOW_UPDATE_NOTIFICATION
import com.music.vivi.constants.SettingsShapeColorTertiaryKey
import com.music.vivi.constants.ShowUpdateNotificationKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.screens.KEY_AUTO_UPDATE_CHECK
import com.music.vivi.ui.screens.PREFS_NAME
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.experiment.getUpdateCheckInterval
import com.music.vivi.update.experiment.saveUpdateCheckInterval
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.update.settingstyle.Material3ExpressiveSettingsGroup
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.update.viewmodelupdate.UpdateViewModel
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference

// new view model to check update
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateInfoScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    updateViewModel: UpdateViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    // State for preferences
    val (checkForUpdates, onCheckForUpdatesChange) = rememberPreference(CheckForUpdatesKey, true)
    val (showUpdateNotification, onShowUpdateNotificationChange) = rememberPreference(ShowUpdateNotificationKey, true)

    val (settingsShapeTertiary, _) = rememberPreference(SettingsShapeColorTertiaryKey, false)
    val (darkMode, _) = rememberEnumPreference(
        DarkModeKey,
        defaultValue = DarkMode.AUTO
    )
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkMode, isSystemInDarkTheme) {
        if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
    }

    val (iconBgColor, iconStyleColor) = if (settingsShapeTertiary) {
        if (useDarkTheme) {
            Pair(
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.onTertiary
            )
        } else {
            Pair(
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    } else {
        Pair(
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.primary
        )
    }

    var updateCheckInterval by remember { mutableStateOf(getUpdateCheckInterval(context)) }
    var showIntervalDialog by remember { mutableStateOf(false) }

    // Function to handle automatic update check toggle
    fun onAutoUpdateCheckChange(newValue: Boolean) {
        onCheckForUpdatesChange(newValue)

        // Save to SharedPreferences
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean(KEY_AUTO_UPDATE_CHECK, newValue).apply()

        updateViewModel.refreshUpdateStatus()

        // Refresh background check schedule
        UpdateNotificationManager.schedulePeriodicUpdateCheck(context)

        // Show feedback
        val message = if (newValue) {
            context.getString(R.string.auto_updates_enabled_toast)
        } else {
            context.getString(R.string.auto_updates_disabled_toast)
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
            context.getString(R.string.notifications_enabled_toast)
        } else {
            context.getString(R.string.notifications_disabled_toast)
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Function to handle interval change
    fun onIntervalChange(newInterval: Int) {
        updateCheckInterval = newInterval
        saveUpdateCheckInterval(context, newInterval)

        // Refresh background check schedule
        UpdateNotificationManager.schedulePeriodicUpdateCheck(context)

        val message = if (newInterval < 24) {
            context.getString(R.string.interval_set_format_hours, newInterval)
        } else {
            val days = newInterval / 24
            context.getString(R.string.interval_set_format_days, days)
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
            Spacer(modifier = Modifier.height(20.dp))

            // Main Title
            Text(
                text = stringResource(R.string.update_settings_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 40.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Manage Section
            Text(
                text = stringResource(R.string.manage_header).uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)
            )

            val manageItems = remember(checkForUpdates, iconBgColor, iconStyleColor) {
                buildList<@Composable () -> Unit> {
                    add {
                        ModernInfoItem(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.automatic),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            title = stringResource(R.string.check_for_updates),
                            subtitle = stringResource(R.string.check_for_updates_desc),
                            onClick = { onAutoUpdateCheckChange(!checkForUpdates) },
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor,
                            trailingContent = {
                                ModernSwitch(
                                    checked = checkForUpdates,
                                    onCheckedChange = { onAutoUpdateCheckChange(it) }
                                )
                            }
                        )
                    }
                }
            }

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items = manageItems
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Notifications Section
            Text(
                text = stringResource(R.string.notification_header).uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)
            )

            val notificationItems =
                remember(showUpdateNotification, checkForUpdates, updateCheckInterval, iconBgColor, iconStyleColor) {
                    buildList<@Composable () -> Unit> {
                        add {
                            ModernInfoItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.notification),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                title = stringResource(R.string.show_notification),
                                subtitle = stringResource(R.string.show_notification_desc),
                                onClick = { if (checkForUpdates) onNotificationToggle(!showUpdateNotification) },
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                modifier = Modifier.alpha(if (checkForUpdates) 1f else 0.5f),
                                trailingContent = {
                                    ModernSwitch(
                                        checked = showUpdateNotification,
                                        onCheckedChange = { onNotificationToggle(it) },
                                        enabled = checkForUpdates
                                    )
                                }
                            )
                        }
                        add {
                            ModernInfoItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.history),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                title = stringResource(R.string.update_check_interval),
                                subtitle = if (updateCheckInterval < 24) {
                                    stringResource(
                                        if (updateCheckInterval ==
                                            1
                                        ) {
                                            R.string.interval_every_hour
                                        } else {
                                            R.string.interval_every_hours
                                        },
                                        updateCheckInterval
                                    )
                                } else {
                                    val days = updateCheckInterval / 24
                                    stringResource(
                                        if (days ==
                                            1
                                        ) {
                                            R.string.interval_every_day
                                        } else {
                                            R.string.interval_every_days
                                        },
                                        days
                                    )
                                },
                                onClick = { if (checkForUpdates) showIntervalDialog = true },
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor,
                                modifier = Modifier.alpha(if (checkForUpdates) 1f else 0.5f)
                            )
                        }
                    }
                }

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items = notificationItems
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.home_screen).uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)
            )

            val (showNewsIcon, onShowNewsIconChange) = rememberPreference(
                com.music.vivi.constants.ShowNewsIconKey,
                true
            )

            val appearanceItems = remember(showNewsIcon, iconBgColor, iconStyleColor) {
                buildList<@Composable () -> Unit> {
                    add {
                        ModernInfoItem(
                            icon = {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Filled.NewReleases, // Use the icon itself for clarity
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            title = stringResource(R.string.show_news_icon),
                            subtitle = stringResource(R.string.show_news_icon_desc),
                            onClick = { onShowNewsIconChange(!showNewsIcon) },
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor,
                            trailingContent = {
                                ModernSwitch(
                                    checked = showNewsIcon,
                                    onCheckedChange = { onShowNewsIconChange(it) }
                                )
                            }
                        )
                    }
                }
            }

            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items = appearanceItems
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Interval Picker Dialog
    if (showIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showIntervalDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.update_check_interval),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    // Change the range and display
                    Text(
                        text = if (updateCheckInterval < 24) {
                            stringResource(R.string.interval_check_format_hours, updateCheckInterval)
                        } else {
                            val days = updateCheckInterval / 24
                            stringResource(R.string.interval_check_format_days, days)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Slider(
                        value = updateCheckInterval.toFloat(),
                        onValueChange = { updateCheckInterval = it.toInt() },
                        valueRange = 1f..168f, // Changed: 1 hour to 7 days (168 hours)
                        steps = 166, // Changed: 167 values minus 1
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.one_hour),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.seven_days),
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
                    Text(stringResource(R.string.ok_caps))
                }
            },
            dismissButton = {
                TextButton(onClick = { showIntervalDialog = false }) {
                    Text(stringResource(R.string.cancel_caps))
                }
            }
        )
    }
}
