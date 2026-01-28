package com.music.vivi.ui.screens.settings.video

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.constants.VideoQuality
import com.music.vivi.constants.VideoQualityKey
import com.music.vivi.constants.VideoQualityDefaultValue
import com.music.vivi.constants.WiFiFastModeKey
import com.music.vivi.constants.WiFiFastModeDefaultValue
import com.music.vivi.ui.component.IconButton
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.update.settingstyle.Material3ExpressiveSettingsGroup
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoQualitySettings(
    navController: NavController
) {
    val (videoQuality, onVideoQualityChange) = rememberEnumPreference(
        key = VideoQualityKey,
        defaultValue = VideoQuality.valueOf(VideoQualityDefaultValue)
    )
    
    val (wifiFastMode, onWiFiFastModeChange) = rememberPreference(
        key = WiFiFastModeKey,
        defaultValue = WiFiFastModeDefaultValue
    )
    
    val iconBgColor = MaterialTheme.colorScheme.secondaryContainer
    val iconStyleColor = MaterialTheme.colorScheme.onSecondaryContainer
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.video_quality)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                Text(
                    text = stringResource(R.string.video_quality).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )
            }
            
            item {
                Material3ExpressiveSettingsGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    items = listOf {
                        ExposedDropdownMenuBox(
                            expanded = false,
                            onExpandedChange = { },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = videoQuality.name,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text(stringResource(R.string.video_quality)) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = false)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = false,
                                onDismissRequest = { }
                            ) {
                                VideoQuality.values().forEach { quality ->
                                    DropdownMenuItem(
                                        text = { Text(quality.name) },
                                        onClick = { onVideoQualityChange(quality) }
                                    )
                                }
                            }
                        }
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                Text(
                    text = stringResource(R.string.wifi_fast_mode).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )
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
                                onCheckedChange = onWiFiFastModeChange,
                                modifier = Modifier.padding(end = 20.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}