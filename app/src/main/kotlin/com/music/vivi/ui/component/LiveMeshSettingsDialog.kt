/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.music.vivi.R
import com.music.vivi.ui.utils.LiveMeshConfig
import com.music.vivi.ui.utils.clamped

@Composable
internal fun LiveMeshSettingsDialog(
    initial: LiveMeshConfig,
    onDismiss: () -> Unit,
    onConfirm: (LiveMeshConfig) -> Unit,
) {
    var draft by remember(initial) { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.live_mesh_settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LiveMeshSlider(
                    label = stringResource(R.string.live_mesh_blur),
                    value = draft.blurStrength,
                    range = LiveMeshConfig.BLUR_STRENGTH_MIN..LiveMeshConfig.BLUR_STRENGTH_MAX,
                    onValueChange = { draft = draft.copy(blurStrength = it) }
                )
                LiveMeshSlider(
                    label = stringResource(R.string.live_mesh_saturation),
                    value = draft.saturation,
                    range = LiveMeshConfig.SATURATION_MIN..LiveMeshConfig.SATURATION_MAX,
                    onValueChange = { draft = draft.copy(saturation = it) }
                )
                LiveMeshSlider(
                    label = stringResource(R.string.live_mesh_speed),
                    value = draft.speed,
                    range = LiveMeshConfig.SPEED_MIN..LiveMeshConfig.SPEED_MAX,
                    onValueChange = { draft = draft.copy(speed = it) }
                )
                LiveMeshSlider(
                    label = stringResource(R.string.live_mesh_dark_tint),
                    value = draft.darkTint,
                    range = LiveMeshConfig.DARK_TINT_MIN..LiveMeshConfig.DARK_TINT_MAX,
                    onValueChange = { draft = draft.copy(darkTint = it) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draft.clamped()) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { draft = LiveMeshConfig.Default }) {
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
private fun LiveMeshSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "%.2f".format(value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}
