package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.vivi.R
import com.music.vivi.update.settingstyle.Material3ExpressiveSettingsGroup
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.updatesreen.UpdateStatus

@Composable
fun SettingsListContent(
    updateStatus: UpdateStatus,
    currentVersion: String,
    accountName: String,
    accountEmail: String,
    accountImageUrl: String?,
    isLoggedIn: Boolean,
    iconBgColor: Color,
    iconStyleColor: Color,
    onNavigate: (String) -> Unit
) {
    Material3ExpressiveSettingsGroup(
        modifier = Modifier.fillMaxWidth(),
        items = listOf(
            {
                // App Updates
                ModernInfoItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.NewReleases,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = when (updateStatus) {
                                is UpdateStatus.UpdateAvailable -> MaterialTheme.colorScheme.error
                                is UpdateStatus.Loading -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                is UpdateStatus.Disabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                else -> LocalContentColor.current
                            }
                        )
                    },
                    title = when (updateStatus) {
                        is UpdateStatus.UpdateAvailable -> stringResource(R.string.update_available_title)
                        is UpdateStatus.Loading -> stringResource(R.string.appupdate)
                        is UpdateStatus.Disabled -> stringResource(R.string.appupdate)
                        else -> stringResource(R.string.appupdate)
                    },
                    subtitle = when (updateStatus) {
                        is UpdateStatus.Disabled -> {
                            stringResource(R.string.automatic_check_disabled, currentVersion)
                        }
                        is UpdateStatus.UpdateAvailable -> {
                            stringResource(
                                R.string.version_now_available,
                                (updateStatus as UpdateStatus.UpdateAvailable).latestVersion
                            )
                        }
                        is UpdateStatus.Loading -> {
                            stringResource(R.string.checking_for_updates)
                        }
                        else -> {
                            stringResource(R.string.current_version, currentVersion)
                        }
                    },
                    titleColor = when (updateStatus) {
                        is UpdateStatus.UpdateAvailable -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    subtitleColor = when (updateStatus) {
                        is UpdateStatus.UpdateAvailable -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        is UpdateStatus.Loading -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        is UpdateStatus.Disabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    onClick = { onNavigate("settings/software_updates") },
                    showArrow = true,
                    iconBackgroundColor = iconBgColor,
                    iconContentColor = iconStyleColor,
                    arrowColor = when (updateStatus) {
                        is UpdateStatus.UpdateAvailable -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            },
            {
                // Account
                val accountTitle = when {
                    isLoggedIn && accountName.isNotBlank() -> accountName
                    else -> stringResource(R.string.account)
                }

                val accountSubtitle = if (isLoggedIn && accountEmail.isNotBlank()) {
                    accountEmail
                } else {
                    stringResource(R.string.manage_account_preferences)
                }

                ModernInfoItem(
                    icon = {
                        if (isLoggedIn && accountImageUrl != null) {
                            AsyncImage(
                                model = accountImageUrl,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(
                                    if (isLoggedIn) R.drawable.person else R.drawable.account
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    title = accountTitle,
                    subtitle = accountSubtitle,
                    // FIX: Route angepasst, damit sie mit NavigationBuilder übereinstimmt
                    onClick = { onNavigate("settings/account_settings") }, 
                    showArrow = true,
                    iconBackgroundColor = iconBgColor,
                    iconContentColor = iconStyleColor
                )
            },
            {
                // Appearance
                ModernInfoItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.palette),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    title = stringResource(R.string.appearance),
                    subtitle = stringResource(R.string.customize_theme_display_settings),
                    onClick = { onNavigate("settings/appearance") },
                    showArrow = true,
                    iconBackgroundColor = iconBgColor,
                    iconContentColor = iconStyleColor
                )
            },
            {
                // Player & Audio
                ModernInfoItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    title = stringResource(R.string.player_and_audio),
                    subtitle = stringResource(R.string.audio_quality_playback_settings),
                    onClick = { onNavigate("settings/player") },
                    showArrow = true,
                    iconBackgroundColor = iconBgColor,
                    iconContentColor = iconStyleColor
                )
            },
            {
                // Content
                ModernInfoItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.language),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    title = stringResource(R.string.content),
                    subtitle = stringResource(R.string.language_content_preferences),
                    onClick = { onNavigate("settings/content") },
                    showArrow = true,
                    iconBackgroundColor = iconBgColor,
                    iconContentColor = iconStyleColor
                )
            },
            {
                // Power Saver
                ModernInfoItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.BatteryFull,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    title = stringResource(R.string.power_saver),
                    subtitle = stringResource(R.string.power_saver_subtitle),
                    onClick = { onNavigate("settings/power_saver") },
                    showArrow = true,
                    iconBackgroundColor = iconBgColor,
                    iconContentColor = iconStyleColor
                )
            },
            {
                // Privacy
                ModernInfoItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.security),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    title = stringResource(R.string.privacy),
                    subtitle = stringResource(R.string.privacy_security_settings),
                    onClick = { onNavigate("settings/privacy") },
                    showArrow = true,
                    iconBackgroundColor = iconBgColor,
                    iconContentColor = iconStyleColor
                )
            },
            {
                // Storage
                ModernInfoItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.storage),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    title = stringResource(R.string.storage),
                    subtitle = stringResource(R.string.manage_storage_downloads),
                    onClick = { onNavigate("settings/storage") },
                    showArrow = true,
                    iconBackgroundColor = iconBgColor,
                    iconContentColor = iconStyleColor
                )
            },
            {
                // Backup & Restore
                ModernInfoItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.restore),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    title = stringResource(R.string.backup_restore),
                    subtitle = stringResource(R.string.backup_restore_data),
                    onClick = { onNavigate("settings/backup_restore") },
                    showArrow = true,
                    iconBackgroundColor = iconBgColor,
                    iconContentColor = iconStyleColor
                )
            },
            {
                // About
                ModernInfoItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.rocket),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    title = stringResource(R.string.about),
                    subtitle = stringResource(R.string.app_information_legal),
                    onClick = { onNavigate("settings/about") },
                    showArrow = true,
                    iconBackgroundColor = iconBgColor,
                    iconContentColor = iconStyleColor
                )
            }
        )
    )
}
