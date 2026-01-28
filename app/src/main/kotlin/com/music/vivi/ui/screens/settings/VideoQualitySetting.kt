package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.constants.VideoQuality
import com.music.vivi.constants.VideoQualityDefaultValue
import com.music.vivi.constants.VideoQualityKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.constants.EnableVideoModeKey
import com.music.vivi.constants.EnableVideoModeDefaultValue
import com.music.vivi.constants.WiFiFastModeKey
import com.music.vivi.constants.WiFiFastModeDefaultValue
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.update.settingstyle.Material3ExpressiveSettingsGroup
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoQualitySetting(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: (() -> Unit)? = null,
) {
    val (videoQuality, onVideoQualityChange) = rememberEnumPreference(
        key = VideoQualityKey,
        defaultValue = VideoQuality.valueOf(VideoQualityDefaultValue)
    )
    val (enableVideoMode, onEnableVideoModeChange) = rememberPreference(
        key = EnableVideoModeKey,
        defaultValue = EnableVideoModeDefaultValue
    )
    val (wifiFastMode, onWifiFastModeChange) = rememberPreference(
        key = WiFiFastModeKey,
        defaultValue = WiFiFastModeDefaultValue
    )
    // Dark/Light mode logic for icons (reused from ContentSettings logic)
    val (settingsShapeTertiary, _) = rememberPreference(com.music.vivi.constants.SettingsShapeColorTertiaryKey, false)
    val (darkMode, _) = rememberEnumPreference(
        com.music.vivi.constants.DarkModeKey,
        defaultValue = com.music.vivi.ui.screens.settings.DarkMode.AUTO
    )
    val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val useDarkTheme = androidx.compose.runtime.remember(darkMode, isSystemInDarkTheme) {
        if (darkMode == com.music.vivi.ui.screens.settings.DarkMode.AUTO) isSystemInDarkTheme else darkMode == com.music.vivi.ui.screens.settings.DarkMode.ON
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
                    title = { Text(stringResource(R.string.video_quality)) },
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.video_quality_description),
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
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.videocam),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.enable_video_mode),
                                            subtitle = stringResource(R.string.enable_video_mode_description),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = enableVideoMode,
                                        onCheckedChange = onEnableVideoModeChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            },
                            {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.wifi_proxy),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.wifi_fast_mode),
                                            subtitle = stringResource(R.string.wifi_fast_mode_description),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = wifiFastMode,
                                        onCheckedChange = onWifiFastModeChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            }
                        )
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = VideoQuality.values().map { quality ->
                            {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onVideoQualityChange(quality) }
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = quality.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (quality == videoQuality) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                    
                                    RadioButton(
                                        selected = quality == videoQuality,
                                        onClick = null, // Handled by Row clickable
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary,
                                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
