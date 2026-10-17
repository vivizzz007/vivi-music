/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.music.vivi.R
import com.music.vivi.ui.utils.LiveMeshConfig
import com.music.vivi.ui.utils.clamped
import kotlin.math.roundToInt

@Composable
internal fun LiveMeshSettingsDialog(
    title: String,
    initial: LiveMeshConfig,
    onDismiss: () -> Unit,
    onConfirm: (LiveMeshConfig) -> Unit,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    var showResetConfirm by remember { mutableStateOf(false) }

    if (showResetConfirm) {
        DefaultDialog(
            onDismiss = { showResetConfirm = false },
            icon = { Icon(painterResource(R.drawable.refresh), contentDescription = null) },
            title = { Text(stringResource(R.string.live_mesh_reset_title)) },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    TextButton(onClick = { showResetConfirm = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Button(onClick = {
                        draft = LiveMeshConfig.Default
                        showResetConfirm = false
                    }) {
                        Text(stringResource(R.string.reset))
                    }
                }
            },
        ) {
            Text(
                text = stringResource(R.string.live_mesh_reset_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LiveMeshSection(stringResource(R.string.live_mesh_section_look)) {
                    LiveMeshSlider(
                        label = stringResource(R.string.live_mesh_blur),
                        description = stringResource(R.string.live_mesh_blur_desc),
                        value = draft.blurStrength,
                        range = LiveMeshConfig.BLUR_STRENGTH_MIN..LiveMeshConfig.BLUR_STRENGTH_MAX,
                        onValueChange = { draft = draft.copy(blurStrength = it) }
                    )
                    LiveMeshSlider(
                        label = stringResource(R.string.live_mesh_saturation),
                        description = stringResource(R.string.live_mesh_saturation_desc),
                        value = draft.saturation,
                        range = LiveMeshConfig.SATURATION_MIN..LiveMeshConfig.SATURATION_MAX,
                        onValueChange = { draft = draft.copy(saturation = it) }
                    )
                    LiveMeshSlider(
                        label = stringResource(R.string.live_mesh_dark_tint),
                        description = stringResource(R.string.live_mesh_dark_tint_desc),
                        value = draft.darkTint,
                        range = LiveMeshConfig.DARK_TINT_MIN..LiveMeshConfig.DARK_TINT_MAX,
                        onValueChange = { draft = draft.copy(darkTint = it) }
                    )
                    LiveMeshSlider(
                        label = stringResource(R.string.live_mesh_bottom_shade),
                        description = stringResource(R.string.live_mesh_bottom_shade_desc),
                        value = draft.bottomShade,
                        range = LiveMeshConfig.BOTTOM_SHADE_MIN..LiveMeshConfig.BOTTOM_SHADE_MAX,
                        onValueChange = { draft = draft.copy(bottomShade = it) }
                    )
                    LiveMeshSlider(
                        label = stringResource(R.string.live_mesh_layer2_alpha),
                        description = stringResource(R.string.live_mesh_layer2_alpha_desc),
                        value = draft.layer2Alpha,
                        range = LiveMeshConfig.LAYER_ALPHA_MIN..LiveMeshConfig.LAYER_ALPHA_MAX,
                        onValueChange = { draft = draft.copy(layer2Alpha = it) }
                    )
                    LiveMeshSlider(
                        label = stringResource(R.string.live_mesh_layer3_alpha),
                        description = stringResource(R.string.live_mesh_layer3_alpha_desc),
                        value = draft.layer3Alpha,
                        range = LiveMeshConfig.LAYER_ALPHA_MIN..LiveMeshConfig.LAYER_ALPHA_MAX,
                        onValueChange = { draft = draft.copy(layer3Alpha = it) }
                    )
                    LiveMeshSlider(
                        label = stringResource(R.string.live_mesh_zoom),
                        description = stringResource(R.string.live_mesh_zoom_desc),
                        value = draft.zoom,
                        range = LiveMeshConfig.ZOOM_MIN..LiveMeshConfig.ZOOM_MAX,
                        onValueChange = { draft = draft.copy(zoom = it) }
                    )
                }

                LiveMeshSection(stringResource(R.string.live_mesh_section_motion)) {
                    LiveMeshSlider(
                        label = stringResource(R.string.live_mesh_speed),
                        description = stringResource(R.string.live_mesh_speed_desc),
                        value = draft.speed,
                        range = LiveMeshConfig.SPEED_MIN..LiveMeshConfig.SPEED_MAX,
                        onValueChange = { draft = draft.copy(speed = it) }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.live_mesh_reverse),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.live_mesh_reverse_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = draft.reverseDirection,
                            onCheckedChange = { draft = draft.copy(reverseDirection = it) }
                        )
                    }
                }

                LiveMeshSection(stringResource(R.string.live_mesh_section_quality)) {
                    LiveMeshSlider(
                        label = stringResource(R.string.live_mesh_render_scale),
                        description = stringResource(R.string.live_mesh_render_scale_desc),
                        value = draft.renderScale,
                        range = LiveMeshConfig.RENDER_SCALE_MIN..LiveMeshConfig.RENDER_SCALE_MAX,
                        onValueChange = { draft = draft.copy(renderScale = it) }
                    )
                    LiveMeshSlider(
                        label = stringResource(R.string.live_mesh_texture_size),
                        description = stringResource(R.string.live_mesh_texture_size_desc),
                        value = draft.textureSize.toFloat(),
                        range = LiveMeshConfig.TEXTURE_SIZE_MIN.toFloat()..LiveMeshConfig.TEXTURE_SIZE_MAX.toFloat(),
                        onValueChange = { draft = draft.copy(textureSize = it.roundToInt()) }
                    )
                }

                LiveMeshSection(stringResource(R.string.live_mesh_section_reactivity)) {
                    LiveMeshSlider(
                        label = stringResource(R.string.live_mesh_beat_reactivity),
                        description = stringResource(R.string.live_mesh_beat_reactivity_desc),
                        value = draft.beatReactivity,
                        range = LiveMeshConfig.REACTIVITY_MIN..LiveMeshConfig.REACTIVITY_MAX,
                        onValueChange = { draft = draft.copy(beatReactivity = it) }
                    )
                    LiveMeshSlider(
                        label = stringResource(R.string.live_mesh_lyric_reactivity),
                        description = stringResource(R.string.live_mesh_lyric_reactivity_desc),
                        value = draft.lyricReactivity,
                        range = LiveMeshConfig.REACTIVITY_MIN..LiveMeshConfig.REACTIVITY_MAX,
                        onValueChange = { draft = draft.copy(lyricReactivity = it) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draft.clamped()) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { showResetConfirm = true }) {
                    Text(stringResource(R.string.reset))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        }
    )
}

@Composable
private fun LiveMeshSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LiveMeshSectionHeader(title)
        LiveMeshSurface(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun LiveMeshSlider(
    label: String,
    description: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "%.2f".format(value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}
