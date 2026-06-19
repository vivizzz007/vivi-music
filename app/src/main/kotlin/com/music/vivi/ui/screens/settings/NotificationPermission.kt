/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.EnableNotificationsKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.Material3SettingsGroup
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.component.ModernSwitch
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberPreference
import com.music.vivi.vivimusic.updater.getUpdateNotificationsSetting
import com.music.vivi.vivimusic.updater.saveUpdateNotificationsSetting
import com.music.vivi.vivimusic.updater.getDownloadNotificationsSetting
import com.music.vivi.vivimusic.updater.saveDownloadNotificationsSetting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPermission(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current

    // Detect system permission state dynamically
    var hasSystemPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    // Refresh system permission state when screen is resumed
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasSystemPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val (notificationsEnabled, onNotificationsEnabledChange) = rememberPreference(
        EnableNotificationsKey,
        defaultValue = true
    )

    // Combined state for checked switch
    val isNotificationsActive = hasSystemPermission && notificationsEnabled

    var updateNotificationsEnabled by remember {
        mutableStateOf(getUpdateNotificationsSetting(context))
    }

    var downloadNotificationsEnabled by remember {
        mutableStateOf(getDownloadNotificationsSetting(context))
    }

    DisposableEffect(context) {
        val sharedPrefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "update_notifications") {
                updateNotificationsEnabled = getUpdateNotificationsSetting(context)
            } else if (key == "download_notifications") {
                downloadNotificationsEnabled = getDownloadNotificationsSetting(context)
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        // Sync initial state
        updateNotificationsEnabled = getUpdateNotificationsSetting(context)
        downloadNotificationsEnabled = getDownloadNotificationsSetting(context)
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasSystemPermission = isGranted
            if (isGranted) {
                onNotificationsEnabledChange(true)
            }
        }
    )

    val onToggleChange: (Boolean) -> Unit = { checked ->
        if (checked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasSystemPermission) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onNotificationsEnabledChange(true)
            }
        } else {
            // Allow disabling notifications locally in-app
            onNotificationsEnabledChange(false)
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {

        // Description text
        Text(
            text = stringResource(R.string.notification_settings_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
        )

        // Large capsule banner for main toggle
        val containerColor by animateColorAsState(
            targetValue = if (isNotificationsActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
            label = "containerColor"
        )

        val contentColor = if (isNotificationsActive) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Card(
            onClick = { onToggleChange(!isNotificationsActive) },
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
                    text = stringResource(R.string.enable_notifications),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                ModernSwitch(
                    checked = isNotificationsActive,
                    onCheckedChange = onToggleChange
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Checkbox settings group for updates and download notifications
        Material3SettingsGroup(
            title = stringResource(R.string.notification_settings),
            items = listOf(
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.update_notifications)) },
                    description = { Text(stringResource(R.string.update_notifications_subtitle)) },
                    trailingContent = {
                        Checkbox(
                            checked = updateNotificationsEnabled,
                            onCheckedChange = null,
                            enabled = isNotificationsActive
                        )
                    },
                    enabled = isNotificationsActive,
                    onClick = {
                        val newValue = !updateNotificationsEnabled
                        updateNotificationsEnabled = newValue
                        saveUpdateNotificationsSetting(context, newValue)
                    }
                ),
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.download_notifications)) },
                    description = { Text(stringResource(R.string.download_notifications_desc)) },
                    trailingContent = {
                        Checkbox(
                            checked = downloadNotificationsEnabled,
                            onCheckedChange = null,
                            enabled = isNotificationsActive
                        )
                    },
                    enabled = isNotificationsActive,
                    onClick = {
                        val newValue = !downloadNotificationsEnabled
                        downloadNotificationsEnabled = newValue
                        saveDownloadNotificationsSetting(context, newValue)
                    }
                )
            )
        )

        Spacer(modifier = Modifier.height(36.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.notification_settings)) },
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
