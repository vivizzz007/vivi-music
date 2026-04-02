package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.Material3SettingsGroup
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.screens.settings.rememberHighlightScrollHandler

import com.music.vivi.ui.utils.backToMain
import com.music.vivi.vivimusic.updater.getAutoUpdateCheckSetting
import com.music.vivi.vivimusic.updater.saveAutoUpdateCheckSetting
import com.music.vivi.vivimusic.updater.getUpdateAvailableState
import com.music.vivi.vivimusic.updater.saveUpdateAvailableState
import com.music.vivi.vivimusic.updater.getUpdateNotificationsSetting
import com.music.vivi.vivimusic.updater.saveUpdateNotificationsSetting
import android.widget.Toast
import androidx.compose.ui.res.pluralStringResource
import com.music.vivi.vivimusic.updater.getDownloadedApkCount
import com.music.vivi.vivimusic.updater.clearDownloadedApks
import com.music.vivi.vivimusic.updater.getBetaUpdatesSetting
import com.music.vivi.vivimusic.updater.saveBetaUpdatesSetting
import androidx.compose.material3.MaterialTheme
import com.music.vivi.BuildConfig

//here b5.0.1 must be used for the beta tag

/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    highlightKey: String? = null
) {
    val context = LocalContext.current
    var autoUpdateEnabled by remember { mutableStateOf(getAutoUpdateCheckSetting(context)) }
    var updateNotificationsEnabled by remember { mutableStateOf(getUpdateNotificationsSetting(context)) }
    var betaUpdatesEnabled by remember { mutableStateOf(getBetaUpdatesSetting(context)) }
    val isUpdateAvailable = getUpdateAvailableState(context) && autoUpdateEnabled
    var apkCount by remember { mutableStateOf(getDownloadedApkCount(context)) }


    val scrollState = rememberScrollState()


    val (_, onHighlightPosition) = rememberHighlightScrollHandler(scrollState, highlightKey)



    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
    ) {
        Material3SettingsGroup(
            highlightKey = highlightKey,
            onHighlightPositionFound = onHighlightPosition,
            title = stringResource(R.string.app_updates_title),
            items = listOf(
                Material3SettingsItem(
                    settingKey = "system_update",
                    icon = painterResource(R.drawable.network_update),
                    title = { Text(stringResource(R.string.system_update)) },
                    description = {
                        if (isUpdateAvailable) {
                            Text(
                                text = stringResource(R.string.update_available),
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(stringResource(R.string.app_update_uptodate))
                        }
                    },
                    onClick = {
                        val isFoss = !BuildConfig.CAST_AVAILABLE
                        if (isFoss) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/vivizzz007/vivi-music"))
                            context.startActivity(intent)
                        } else {
                            navController.navigate("update")
                        }
                    }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.info),
                    title = {
                        Text(stringResource(R.string.version, BuildConfig.VERSION_NAME))
                    },
                    description = {
                        val arch = BuildConfig.ARCHITECTURE
                        val variant = if (BuildConfig.CAST_AVAILABLE) "GMS" else "FOSS"
                        Text("$arch - $variant")
                    }
                ),
                
                Material3SettingsItem(
                    settingKey = "auto_update_check",
                    icon = painterResource(R.drawable.update),
                    title = { Text(stringResource(R.string.auto_update_check)) },
                    description = { Text(stringResource(R.string.auto_update_check_subtitle)) },
                    trailingContent = {
                        Switch(
                            checked = autoUpdateEnabled,
                            onCheckedChange = { enabled ->
                                autoUpdateEnabled = enabled
                                saveAutoUpdateCheckSetting(context, enabled)
                                if (!enabled) {
                                    saveUpdateAvailableState(context, false)
                                }
                            },
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (autoUpdateEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = {
                        autoUpdateEnabled = !autoUpdateEnabled
                        saveAutoUpdateCheckSetting(context, autoUpdateEnabled)
                        if (!autoUpdateEnabled) {
                            saveUpdateAvailableState(context, false)
                        }
                    }
                ),

                Material3SettingsItem(
                    settingKey = "update_notifications",
                    icon = painterResource(R.drawable.notification),
                    title = { Text(stringResource(R.string.update_notifications)) },
                    description = { Text(stringResource(R.string.update_notifications_subtitle)) },
                    trailingContent = {
                        Switch(
                            checked = updateNotificationsEnabled,
                            onCheckedChange = { enabled ->
                                updateNotificationsEnabled = enabled
                                saveUpdateNotificationsSetting(context, enabled)
                            },
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (updateNotificationsEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = {
                        updateNotificationsEnabled = !updateNotificationsEnabled
                        saveUpdateNotificationsSetting(context, updateNotificationsEnabled)
                    }
                ),

                Material3SettingsItem(
                    settingKey = "beta_updates",
                    icon = painterResource(R.drawable.biotech),
                    title = { Text(stringResource(R.string.beta_updates)) },
                    description = { Text(stringResource(R.string.beta_updates_subtitle)) },
                    trailingContent = {
                        Switch(
                            checked = betaUpdatesEnabled,
                            onCheckedChange = { enabled ->
                                betaUpdatesEnabled = enabled
                                saveBetaUpdatesSetting(context, enabled)
                            },
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (betaUpdatesEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = {
                        betaUpdatesEnabled = !betaUpdatesEnabled
                        saveBetaUpdatesSetting(context, betaUpdatesEnabled)
                    }
                ),

                Material3SettingsItem(
                    icon = painterResource(R.drawable.delete),
                    title = { Text(stringResource(R.string.clear_downloaded_updates)) },
                    description = { 
                        Text(
                            text = if (apkCount == 0) {
                                stringResource(R.string.clear_downloaded_updates_desc)
                            } else {
                                pluralStringResource(R.plurals.n_apk_found, apkCount, apkCount)
                            }
                        )
                    },
                    onClick = {
                        if (apkCount > 0) {
                            if (clearDownloadedApks(context)) {
                                apkCount = 0
                                Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to delete some files", Toast.LENGTH_SHORT).show()
                                apkCount = getDownloadedApkCount(context)
                            }
                        }
                    }
                )

//                Material3SettingsItem(
//                    settingKey = "namespace",
//                    icon = painterResource(R.drawable.info),
//                    title = { Text(stringResource(R.string.namespace)) },
//                    description = { Text(BuildConfig.APPLICATION_ID) }
//                )

            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Material3SettingsGroup(
            highlightKey = highlightKey,
            onHighlightPositionFound = onHighlightPosition,
            title = stringResource(R.string.changelog),
            items = listOf(
                Material3SettingsItem(
                    settingKey = "changelog",
                    icon = painterResource(R.drawable.history),
                    title = { Text(stringResource(R.string.changelog)) },
                    description = { Text(stringResource(R.string.view_version_history)) },
                    onClick = { navController.navigate("settings/changelog") }
                ),
                Material3SettingsItem(
                    settingKey = "commits",
                    icon = painterResource(R.drawable.commit),
                    title = { Text(stringResource(R.string.commits)) },
                    description = { Text(stringResource(R.string.view_commit_history)) },
                    onClick = { navController.navigate("settings/commits") }
                )
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.update_settings_title)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        }
    )
}
