package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.constants.PowerSaverKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.update.settingstyle.Material3ExpressiveSettingsGroup
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference

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
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.power_saver),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.power_saver_description),
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
                        items = listOf {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = {
                                            Icon(
                                                androidx.compose.material.icons.Icons.Filled.BatteryFull,
                                                null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        title = stringResource(R.string.enable_power_saver),
                                        subtitle = stringResource(R.string.power_saver_subtitle),
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor
                                    )
                                }
                                ModernSwitch(
                                    checked = powerSaver,
                                    onCheckedChange = onPowerSaverChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }
                        }
                    )
                }

                if (powerSaver) {
                    item {
                        Text(
                            text = stringResource(R.string.actions).uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                        )
                    }

                    item {
                        val (powerSaverPureBlack, onPowerSaverPureBlackChange) = rememberPreference(com.music.vivi.constants.PowerSaverPureBlackKey, defaultValue = true)
                        val (powerSaverHighRefresh, onPowerSaverHighRefreshChange) = rememberPreference(com.music.vivi.constants.PowerSaverHighRefreshRateKey, defaultValue = true)
                        val (powerSaverDiscord, onPowerSaverDiscordChange) = rememberPreference(com.music.vivi.constants.PowerSaverDiscordKey, defaultValue = true)
                        val (powerSaverLastFM, onPowerSaverLastFMChange) = rememberPreference(com.music.vivi.constants.PowerSaverLastFMKey, defaultValue = true)
                        val (powerSaverLyrics, onPowerSaverLyricsChange) = rememberPreference(com.music.vivi.constants.PowerSaverLyricsKey, defaultValue = true)
                        val (powerSaverPauseOnZeroVolume, onPowerSaverPauseOnZeroVolumeChange) = rememberPreference(com.music.vivi.constants.PowerSaverPauseOnZeroVolumeKey, defaultValue = true)

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
                                                icon = { Icon(painterResource(R.drawable.contrast), null, modifier = Modifier.size(22.dp)) },
                                                title = stringResource(R.string.power_saver_pure_black),
                                                subtitle = stringResource(R.string.use_pure_black_dark_theme),
                                                iconBackgroundColor = iconBgColor,
                                                iconContentColor = iconStyleColor
                                            )
                                        }
                                        ModernSwitch(checked = powerSaverPureBlack, onCheckedChange = onPowerSaverPureBlackChange, modifier = Modifier.padding(end = 20.dp))
                                    }
                                },
                                {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernInfoItem(
                                                icon = { Icon(painterResource(R.drawable.palette), null, modifier = Modifier.size(22.dp)) },
                                                title = stringResource(R.string.power_saver_high_refresh),
                                                subtitle = stringResource(R.string.high_refresh_rate_desc),
                                                iconBackgroundColor = iconBgColor,
                                                iconContentColor = iconStyleColor
                                            )
                                        }
                                        ModernSwitch(checked = powerSaverHighRefresh, onCheckedChange = onPowerSaverHighRefreshChange, modifier = Modifier.padding(end = 20.dp))
                                    }
                                },
                                {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernInfoItem(
                                                icon = { Icon(painterResource(R.drawable.discord), null, modifier = Modifier.size(22.dp)) },
                                                title = stringResource(R.string.power_saver_discord),
                                                subtitle = stringResource(R.string.enable_discord_rpc),
                                                iconBackgroundColor = iconBgColor,
                                                iconContentColor = iconStyleColor
                                            )
                                        }
                                        ModernSwitch(checked = powerSaverDiscord, onCheckedChange = onPowerSaverDiscordChange, modifier = Modifier.padding(end = 20.dp))
                                    }
                                },
                                {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernInfoItem(
                                                icon = { Icon(painterResource(R.drawable.music_note), null, modifier = Modifier.size(22.dp)) },
                                                title = stringResource(R.string.power_saver_lastfm),
                                                subtitle = stringResource(R.string.lastfm_scrobbling),
                                                iconBackgroundColor = iconBgColor,
                                                iconContentColor = iconStyleColor
                                            )
                                        }
                                        ModernSwitch(checked = powerSaverLastFM, onCheckedChange = onPowerSaverLastFMChange, modifier = Modifier.padding(end = 20.dp))
                                    }
                                },
                                {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernInfoItem(
                                                icon = { Icon(painterResource(R.drawable.lyrics), null, modifier = Modifier.size(22.dp)) },
                                                title = stringResource(R.string.power_saver_lyrics),
                                                subtitle = stringResource(R.string.search_lyrics),
                                                iconBackgroundColor = iconBgColor,
                                                iconContentColor = iconStyleColor
                                            )
                                        }
                                        ModernSwitch(checked = powerSaverLyrics, onCheckedChange = onPowerSaverLyricsChange, modifier = Modifier.padding(end = 20.dp))
                                    }
                                },
                                {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernInfoItem(
                                                icon = { Icon(painterResource(R.drawable.volume_off), null, modifier = Modifier.size(22.dp)) },
                                                title = stringResource(R.string.power_saver_pause_on_zero_volume),
                                                subtitle = stringResource(R.string.pause_on_zero_volume_description),
                                                iconBackgroundColor = iconBgColor,
                                                iconContentColor = iconStyleColor
                                            )
                                        }
                                        ModernSwitch(checked = powerSaverPauseOnZeroVolume, onCheckedChange = onPowerSaverPauseOnZeroVolumeChange, modifier = Modifier.padding(end = 20.dp))
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
