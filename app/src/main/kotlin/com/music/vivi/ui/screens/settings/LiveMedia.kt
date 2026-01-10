package com.music.vivi.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.constants.*
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.update.settingstyle.Material3ExpressiveSettingsGroup
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.utils.rememberPreference
import androidx.compose.animation.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

enum class PillContent {
    TITLE, ELAPSED, REMAINING
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LiveMedia(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val (settingsShapeTertiary, _) = rememberPreference(SettingsShapeColorTertiaryKey, false)
    val (darkMode, _) = rememberPreference(DarkModeKey, "AUTO")

    val iconBgColor = if (settingsShapeTertiary) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
    }
    val iconStyleColor = if (settingsShapeTertiary) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }

    val (enabled, onEnabledChange) = rememberPreference(LiveMediaEnabledKey, defaultValue = false)
    val (showAlbumArt, onShowAlbumArtChange) = rememberPreference(LiveMediaShowAlbumArtKey, defaultValue = true)
    val (showArtistName, onShowArtistNameChange) = rememberPreference(LiveMediaShowArtistNameKey, defaultValue = true)
    val (showAlbumName, onShowAlbumNameChange) = rememberPreference(LiveMediaShowAlbumNameKey, defaultValue = true)
    val (showActionButtons, onShowActionButtonsChange) = rememberPreference(LiveMediaShowActionButtonsKey, defaultValue = true)
    val (showProgress, onShowProgressChange) = rememberPreference(LiveMediaShowProgressKey, defaultValue = true)
    val (showTimestamp, onShowTimestampChange) = rememberPreference(LiveMediaShowTimestampKey, defaultValue = false)
    val (hideOnQs, onHideOnQsChange) = rememberPreference(LiveMediaHideOnQsOpenKey, defaultValue = true)
    val (pillContent, onPillContentChange) = rememberPreference(LiveMediaPillContentKey, defaultValue = PillContent.TITLE.name)

    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasAccessibilityPermission by remember { mutableStateOf(false) }

    fun checkPermissions() {
        hasNotificationPermission = isNotificationListenerEnabled(context)
        hasAccessibilityPermission = isAccessibilityServiceEnabled(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val scrollState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
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
                                painterResource(R.drawable.arrow_back),
                                contentDescription = null
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 32.dp
                )
            ) {
                item {
                    Spacer(modifier = Modifier.height(25.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.live_media),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.live_media_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = listOf(
                            {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = { Icon(painterResource(R.drawable.update), null, modifier = Modifier.size(22.dp)) },
                                            title = stringResource(R.string.live_media),
                                            subtitle = if (enabled) stringResource(R.string.enabled) else stringResource(R.string.disabled),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = enabled,
                                        onCheckedChange = onEnabledChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            }
                        )
                    )
                }

                if (android.os.Build.VERSION.SDK_INT < 36) {
                    item {
                        androidx.compose.material3.Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painterResource(R.drawable.warning),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.live_media_compatibility_message),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text(
                            stringResource(R.string.permissions).uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                        )

                        Material3ExpressiveSettingsGroup(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            items = listOf(
                                {
                                    ModernInfoItem(
                                        icon = {
                                            Icon(
                                                painterResource(if (hasNotificationPermission) R.drawable.check else R.drawable.notifications),
                                                null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        title = stringResource(R.string.notification_listener_permission_needed),
                                        subtitle = if (hasNotificationPermission) stringResource(R.string.granted) else stringResource(R.string.notification_listener_permission_desc),
                                        iconBackgroundColor = if (hasNotificationPermission) MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (enabled) 0.4f else 0.2f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = if (enabled) 0.15f else 0.08f),
                                        iconContentColor = if (hasNotificationPermission) MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.38f) else MaterialTheme.colorScheme.error.copy(alpha = if (enabled) 1f else 0.38f),
                                        modifier = Modifier.clickable(enabled = enabled) {
                                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                        }
                                    )
                                },
                                {
                                    ModernInfoItem(
                                        icon = {
                                            Icon(
                                                painterResource(if (hasAccessibilityPermission) R.drawable.check else R.drawable.accessibility),
                                                null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        title = stringResource(R.string.accessibility_permission_needed),
                                        subtitle = if (hasAccessibilityPermission) stringResource(R.string.granted) else stringResource(R.string.accessibility_permission_desc),
                                        iconBackgroundColor = if (hasAccessibilityPermission) MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (enabled) 0.4f else 0.2f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = if (enabled) 0.15f else 0.08f),
                                        iconContentColor = if (hasAccessibilityPermission) MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.38f) else MaterialTheme.colorScheme.error.copy(alpha = if (enabled) 1f else 0.38f),
                                        modifier = Modifier.clickable(enabled = enabled) {
                                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                        }
                                    )
                                }
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            stringResource(R.string.notification_body).uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                        )

                        Material3ExpressiveSettingsGroup(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            items = listOf(
                                {
                                    ToggleSettingItem(
                                        title = stringResource(R.string.show_album_art),
                                        subtitle = stringResource(R.string.show_album_art_desc),
                                        icon = R.drawable.image,
                                        checked = showAlbumArt,
                                        onCheckedChange = onShowAlbumArtChange,
                                        iconBgColor = iconBgColor,
                                        iconStyleColor = iconStyleColor,
                                        enabled = enabled
                                    )
                                },
                                {
                                    ToggleSettingItem(
                                        title = stringResource(R.string.show_artist_name),
                                        subtitle = stringResource(R.string.show_artist_name_desc),
                                        icon = R.drawable.person,
                                        checked = showArtistName,
                                        onCheckedChange = onShowArtistNameChange,
                                        iconBgColor = iconBgColor,
                                        iconStyleColor = iconStyleColor,
                                        enabled = enabled
                                    )
                                },
                                {
                                    ToggleSettingItem(
                                        title = stringResource(R.string.show_album_name),
                                        subtitle = stringResource(R.string.show_album_name_desc),
                                        icon = R.drawable.album,
                                        checked = showAlbumName,
                                        onCheckedChange = onShowAlbumNameChange,
                                        iconBgColor = iconBgColor,
                                        iconStyleColor = iconStyleColor,
                                        enabled = enabled
                                    )
                                },
                                {
                                    ToggleSettingItem(
                                        title = stringResource(R.string.show_action_buttons),
                                        subtitle = stringResource(R.string.show_action_buttons_desc),
                                        icon = R.drawable.play_arrow,
                                        checked = showActionButtons,
                                        onCheckedChange = onShowActionButtonsChange,
                                        iconBgColor = iconBgColor,
                                        iconStyleColor = iconStyleColor,
                                        enabled = enabled
                                    )
                                },
                                {
                                    ToggleSettingItem(
                                        title = stringResource(R.string.show_progress),
                                        subtitle = stringResource(R.string.show_progress_desc),
                                        icon = R.drawable.linear_scale,
                                        checked = showProgress,
                                        onCheckedChange = onShowProgressChange,
                                        iconBgColor = iconBgColor,
                                        iconStyleColor = iconStyleColor,
                                        enabled = enabled
                                    )
                                },
                                {
                                    ToggleSettingItem(
                                        title = stringResource(R.string.show_timestamps),
                                        subtitle = stringResource(R.string.show_timestamps_desc),
                                        icon = R.drawable.timer,
                                        checked = showTimestamp,
                                        onCheckedChange = onShowTimestampChange,
                                        iconBgColor = iconBgColor,
                                        iconStyleColor = iconStyleColor,
                                        enabled = enabled
                                    )
                                },
                                {
                                    ToggleSettingItem(
                                        title = stringResource(R.string.hide_on_qs),
                                        subtitle = stringResource(R.string.hide_on_qs_desc),
                                        icon = R.drawable.expand_more,
                                        checked = hideOnQs,
                                        onCheckedChange = onHideOnQsChange,
                                        iconBgColor = iconBgColor,
                                        iconStyleColor = iconStyleColor,
                                        enabled = enabled
                                    )
                                }
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            stringResource(R.string.status_bar_pill).uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                        )
                        Material3ExpressiveSettingsGroup(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            items = listOf(
                                {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        ModernInfoItem(
                                            icon = { Icon(painterResource(R.drawable.short_text), null, modifier = Modifier.size(22.dp)) },
                                            title = stringResource(R.string.status_bar_pill),
                                            subtitle = stringResource(R.string.pill_content_desc),
                                            iconBackgroundColor = if (enabled) iconBgColor else iconBgColor.copy(alpha = 0.5f),
                                            iconContentColor = if (enabled) iconStyleColor else iconStyleColor.copy(alpha = 0.38f)
                                        )

                                        val options = PillContent.entries
                                        val labels = listOf(
                                            stringResource(R.string.pill_song_title),
                                            stringResource(R.string.pill_elapsed_time),
                                            stringResource(R.string.pill_remaining_time)
                                        )

                                        FlowRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 64.dp, bottom = 12.dp, end = 20.dp),
                                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            options.forEachIndexed { index, value ->
                                                ToggleButton(
                                                    checked = pillContent == value.name,
                                                    onCheckedChange = { onPillContentChange(value.name) },
                                                    enabled = enabled,
                                                    colors = ToggleButtonDefaults.toggleButtonColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                                                        checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                                    ),
                                                    shapes = when (index) {
                                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                                    },
                                                    modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                                                ) {
                                                    Text(labels[index], style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun ToggleSettingItem(
    title: String,
    subtitle: String,
    icon: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconBgColor: Color,
    iconStyleColor: Color,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            ModernInfoItem(
                icon = { Icon(painterResource(icon), null, modifier = Modifier.size(22.dp)) },
                title = title,
                subtitle = subtitle,
                iconBackgroundColor = if (enabled) iconBgColor else iconBgColor.copy(alpha = 0.5f),
                iconContentColor = if (enabled) iconStyleColor else iconStyleColor.copy(alpha = 0.38f)
            )
        }
        ModernSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.padding(end = 20.dp)
        )
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat?.contains(pkgName) == true
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    return flat?.contains("$pkgName/") == true
}
