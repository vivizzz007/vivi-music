package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.constants.PowerSaverKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference

/**
 * Screen for configuring power saving options.
 * Allows disabling animations, high refresh rate, and background services to save battery.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PowerSaverSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: (() -> Unit)? = null,
) {
    val (settingsShapeTertiary, _) = rememberPreference(com.music.vivi.constants.SettingsShapeColorTertiaryKey, false)
    val (darkMode, _) = rememberEnumPreference(
        com.music.vivi.constants.DarkModeKey,
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

    val (powerSaver, onPowerSaverChange) = rememberPreference(PowerSaverKey, defaultValue = false)
    val scrollState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.power_saver)) },
                navigationIcon = {
                    IconButton(
                        onClick = { onBack?.invoke() ?: navController.navigateUp() },
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
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.power_saver_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ModernInfoItem(
                                icon = {
                                    Icon(
                                        androidx.compose.material.icons.Icons.Filled.BatteryFull, // Need to ensure this icon exists or use generic
                                        null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                title = stringResource(R.string.enable_power_saver),
                                subtitle = stringResource(R.string.power_saver_subtitle),
                                iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        ModernSwitch(
                            checked = powerSaver,
                            onCheckedChange = onPowerSaverChange,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    if (powerSaver) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Statistic Card
                        val savedPower = 20 +
                            // Base
                            (
                                if (rememberPreference(
                                        com.music.vivi.constants.PowerSaverPureBlackKey,
                                        true
                                    ).value
                                ) {
                                    15
                                } else {
                                    0
                                }
                                ) +
                            (
                                if (rememberPreference(
                                        com.music.vivi.constants.PowerSaverHighRefreshRateKey,
                                        true
                                    ).value
                                ) {
                                    15
                                } else {
                                    0
                                }
                                ) +
                            (
                                if (rememberPreference(
                                        com.music.vivi.constants.PowerSaverAnimationsKey,
                                        true
                                    ).value
                                ) {
                                    10
                                } else {
                                    0
                                }
                                ) +
                            (
                                if (rememberPreference(
                                        com.music.vivi.constants.PowerSaverDiscordKey,
                                        true
                                    ).value
                                ) {
                                    5
                                } else {
                                    0
                                }
                                ) +
                            (
                                if (rememberPreference(
                                        com.music.vivi.constants.PowerSaverLastFMKey,
                                        true
                                    ).value
                                ) {
                                    5
                                } else {
                                    0
                                }
                                ) +
                            (
                                if (rememberPreference(
                                        com.music.vivi.constants.PowerSaverLyricsKey,
                                        true
                                    ).value
                                ) {
                                    5
                                } else {
                                    0
                                }
                                )

                        androidx.compose.material3.Card(
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.estimated_power_saved),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "~$savedPower%",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = "Actions",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val (powerSaverPureBlack, onPowerSaverPureBlackChange) = rememberPreference(
                            com.music.vivi.constants.PowerSaverPureBlackKey,
                            defaultValue = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.pure_black),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(R.string.use_pure_black_dark_theme), // This string now contains the OLED recommendation
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            ModernSwitch(checked = powerSaverPureBlack, onCheckedChange = onPowerSaverPureBlackChange)
                        }

                        val (powerSaverHighRefresh, onPowerSaverHighRefreshChange) = rememberPreference(
                            com.music.vivi.constants.PowerSaverHighRefreshRateKey,
                            defaultValue = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Disable High Refresh Rate",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            ModernSwitch(
                                checked = powerSaverHighRefresh,
                                onCheckedChange = onPowerSaverHighRefreshChange
                            )
                        }

                        val (powerSaverAnimations, onPowerSaverAnimationsChange) = rememberPreference(
                            com.music.vivi.constants.PowerSaverAnimationsKey,
                            defaultValue = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.disable_animations),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(R.string.disable_animations_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            ModernSwitch(checked = powerSaverAnimations, onCheckedChange = onPowerSaverAnimationsChange)
                        }

                        val (powerSaverDiscord, onPowerSaverDiscordChange) = rememberPreference(
                            com.music.vivi.constants.PowerSaverDiscordKey,
                            defaultValue = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Disable Discord RPC",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            ModernSwitch(checked = powerSaverDiscord, onCheckedChange = onPowerSaverDiscordChange)
                        }

                        val (powerSaverLastFM, onPowerSaverLastFMChange) = rememberPreference(
                            com.music.vivi.constants.PowerSaverLastFMKey,
                            defaultValue = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Disable Last.fm Scrobbling",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            ModernSwitch(checked = powerSaverLastFM, onCheckedChange = onPowerSaverLastFMChange)
                        }

                        val (powerSaverLyrics, onPowerSaverLyricsChange) = rememberPreference(
                            com.music.vivi.constants.PowerSaverLyricsKey,
                            defaultValue = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Disable Lyrics Fetching",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            ModernSwitch(checked = powerSaverLyrics, onCheckedChange = onPowerSaverLyricsChange)
                        }

                        val (powerSaverPauseOnZeroVolume, onPowerSaverPauseOnZeroVolumeChange) = rememberPreference(
                            com.music.vivi.constants.PowerSaverPauseOnZeroVolumeKey,
                            defaultValue = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pause on Zero Volume",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            ModernSwitch(
                                checked = powerSaverPauseOnZeroVolume,
                                onCheckedChange = onPowerSaverPauseOnZeroVolumeChange
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
