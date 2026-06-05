/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.constants.CanvasSource
import com.music.vivi.constants.CanvasSourceKey
import com.music.vivi.constants.CanvasThumbnailAnimationKey
import com.music.vivi.ui.component.ExpressiveIconButton
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.Material3SettingsGroup
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.component.ModernSwitch
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasSelection(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (canvasThumbnailAnimation, onCanvasThumbnailAnimationChange) = rememberPreference(
        CanvasThumbnailAnimationKey,
        defaultValue = true
    )
    val (canvasSource, onCanvasSourceChange) = rememberEnumPreference(
        CanvasSourceKey,
        defaultValue = CanvasSource.AUTO
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vivimusic_canvas)) },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 16.dp, end = 8.dp)) {
                        ExpressiveIconButton(
                            onClick = navController::navigateUp,
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Description text
            Text(
                text = stringResource(R.string.vivimusic_canvas_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Large capsule banner for main toggle
            val containerColor by animateColorAsState(
                targetValue = if (canvasThumbnailAnimation) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                label = "containerColor"
            )

            val contentColor = if (canvasThumbnailAnimation) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Card(
                onClick = { onCanvasThumbnailAnimationChange(!canvasThumbnailAnimation) },
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
                        text = stringResource(R.string.use_canvas),
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor
                    )
                    ModernSwitch(
                        checked = canvasThumbnailAnimation,
                        onCheckedChange = onCanvasThumbnailAnimationChange
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Options settings group
            Material3SettingsGroup(
                title = stringResource(R.string.canvas_source),
                items = listOf(
                    Material3SettingsItem(
                        leadingContent = {
                            RadioButton(
                                selected = canvasSource == CanvasSource.AUTO,
                                onClick = null,
                                enabled = canvasThumbnailAnimation
                            )
                        },
                        title = { Text(stringResource(R.string.canvas_source_auto)) },
                        description = { Text(stringResource(R.string.canvas_source_auto_desc)) },
                        enabled = canvasThumbnailAnimation,
                        onClick = { onCanvasSourceChange(CanvasSource.AUTO) }
                    ),
                    Material3SettingsItem(
                        leadingContent = {
                            RadioButton(
                                selected = canvasSource == CanvasSource.APPLE_MUSIC,
                                onClick = null,
                                enabled = canvasThumbnailAnimation
                            )
                        },
                        title = { Text(stringResource(R.string.canvas_source_apple_music)) },
                        description = { Text(stringResource(R.string.canvas_source_apple_music_desc)) },
                        enabled = canvasThumbnailAnimation,
                        onClick = { onCanvasSourceChange(CanvasSource.APPLE_MUSIC) }
                    ),
                    Material3SettingsItem(
                        leadingContent = {
                            RadioButton(
                                selected = canvasSource == CanvasSource.VIVIMUSIC,
                                onClick = null,
                                enabled = canvasThumbnailAnimation
                            )
                        },
                        title = { Text(stringResource(R.string.canvas_source_vivimusic)) },
                        description = { Text(stringResource(R.string.canvas_source_vivimusic_desc)) },
                        enabled = canvasThumbnailAnimation,
                        onClick = { onCanvasSourceChange(CanvasSource.VIVIMUSIC) }
                    )
                )
            )
        }
    }
}
