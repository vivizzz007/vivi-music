/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import android.content.Context
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.AutoBackupEnabledKey
import com.music.vivi.constants.AutoBackupWeeklyKey
import com.music.vivi.constants.AutoBackupBeforeUpdateKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.Material3SettingsGroup
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.component.ModernSwitch
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.formatFileSize
import com.music.vivi.utils.AutoBackupHelper
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.BackupRestoreViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoBackupSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: BackupRestoreViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val (autoBackupEnabled, onAutoBackupEnabledChange) = rememberPreference(
        AutoBackupEnabledKey,
        defaultValue = true
    )
    val (autoBackupWeekly, onAutoBackupWeeklyChange) = rememberPreference(
        AutoBackupWeeklyKey,
        defaultValue = false
    )
    val (autoBackupBeforeUpdate, onAutoBackupBeforeUpdateChange) = rememberPreference(
        AutoBackupBeforeUpdateKey,
        defaultValue = true
    )

    var backupsList by remember { mutableStateOf(emptyList<File>()) }
    var backupToDelete by remember { mutableStateOf<File?>(null) }
    var backupToRestore by remember { mutableStateOf<File?>(null) }

    fun reloadBackups() {
        backupsList = AutoBackupHelper.getAutoBackups(context)
    }

    LaunchedEffect(Unit) {
        reloadBackups()
    }

    LaunchedEffect(autoBackupEnabled, autoBackupWeekly) {
        AutoBackupHelper.updateWeeklyBackupWork(context, autoBackupEnabled && autoBackupWeekly)
    }

    // Large capsule banner background color animation
    val containerColor by animateColorAsState(
        targetValue = if (autoBackupEnabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        label = "containerColor"
    )

    val contentColor = if (autoBackupEnabled) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        // Description text
        Text(
            text = stringResource(R.string.automatic_backup_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
        )

        // Main Switch Card (Capsule shape)
        Card(
            onClick = { onAutoBackupEnabledChange(!autoBackupEnabled) },
            shape = RoundedCornerShape(50),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.enable_automatic_backup),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                ModernSwitch(
                    checked = autoBackupEnabled,
                    onCheckedChange = onAutoBackupEnabledChange
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Checklist Settings Group
        Material3SettingsGroup(
            title = stringResource(R.string.options),
            items = listOf(
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.weekly_backup)) },
                    description = { Text(stringResource(R.string.weekly_backup_desc)) },
                    trailingContent = {
                        Checkbox(
                            checked = autoBackupWeekly,
                            onCheckedChange = null,
                            enabled = autoBackupEnabled
                        )
                    },
                    enabled = autoBackupEnabled,
                    onClick = {
                        onAutoBackupWeeklyChange(!autoBackupWeekly)
                    }
                ),
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.backup_before_update)) },
                    description = { Text(stringResource(R.string.backup_before_update_desc)) },
                    trailingContent = {
                        Checkbox(
                            checked = autoBackupBeforeUpdate,
                            onCheckedChange = null,
                            enabled = autoBackupEnabled
                        )
                    },
                    enabled = autoBackupEnabled,
                    onClick = {
                        onAutoBackupBeforeUpdateChange(!autoBackupBeforeUpdate)
                    }
                )
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stored Automatic Backups Group
        Material3SettingsGroup(
            title = stringResource(R.string.stored_backups),
            items = if (backupsList.isEmpty()) {
                listOf(
                    Material3SettingsItem(
                        title = { Text(stringResource(R.string.no_stored_backups)) },
                        enabled = false
                    )
                )
            } else {
                backupsList.map { backupFile ->
                    val (dateStr, typeStr) = parseBackupFilename(backupFile, context)
                    Material3SettingsItem(
                        title = { Text(dateStr) },
                        description = { Text("$typeStr • ${formatFileSize(backupFile.length())}") },
                        onClick = {
                            backupToRestore = backupFile
                        },
                        trailingContent = {
                            androidx.compose.material3.IconButton(
                                onClick = { backupToDelete = backupFile }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.delete),
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(36.dp))
    }

    // Top Bar
    TopAppBar(
        title = { Text(stringResource(R.string.automatic_backup)) },
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

    // Delete Confirmation Dialog
    backupToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { backupToDelete = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_backup_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        AutoBackupHelper.deleteBackup(context, file)
                        reloadBackups()
                        backupToDelete = null
                    }
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToDelete = null }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        )
    }

    // Restore Confirmation Dialog
    backupToRestore?.let { file ->
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            title = { Text(stringResource(R.string.action_restore)) },
            text = { Text(stringResource(R.string.restore_backup_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreFromFile(context, file)
                        backupToRestore = null
                    }
                ) {
                    Text(stringResource(R.string.action_restore))
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToRestore = null }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        )
    }
}

private fun parseBackupFilename(file: File, context: Context): Pair<String, String> {
    val name = file.name
    val timestampRegex = Regex("""(\d{8}_\d{6})\.backup$""")
    val timestampMatch = timestampRegex.find(name)
    val formattedTime = if (timestampMatch != null) {
        val ts = timestampMatch.groupValues[1]
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            val dateTime = LocalDateTime.parse(ts, formatter)
            val displayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a")
            dateTime.format(displayFormatter)
        } catch (e: Exception) {
            ts
        }
    } else {
        "Unknown Date"
    }

    val type = when {
        name.contains("before_update") -> {
            val startIdx = name.indexOf("before_update_") + "before_update_".length
            val endIdx = name.lastIndexOf('_')
            val versionStr = if (startIdx in 0 until endIdx) {
                name.substring(startIdx, endIdx)
            } else {
                ""
            }
            if (versionStr.isNotEmpty()) {
                "${context.getString(R.string.backup_type_before_update)} ($versionStr)"
            } else {
                context.getString(R.string.backup_type_before_update)
            }
        }
        else -> context.getString(R.string.backup_type_weekly)
    }

    return Pair(formattedTime, type)
}
