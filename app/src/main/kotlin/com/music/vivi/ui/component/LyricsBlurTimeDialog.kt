/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.music.vivi.R
import kotlin.math.roundToInt

@Composable
internal fun LyricsBlurTimeDialog(
    titleRes: Int,
    descRes: Int,
    initialMs: Int,
    minMs: Int,
    maxMs: Int,
    unitSuffix: String,
    stepSize: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var draft by remember(initialMs) { mutableFloatStateOf(initialMs.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(descRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${draft.roundToInt()}$unitSuffix",
                    style = MaterialTheme.typography.titleMedium,
                )
                Slider(
                    value = draft,
                    onValueChange = { draft = it },
                    valueRange = minMs.toFloat()..maxMs.toFloat(),
                    steps = (maxMs - minMs) / stepSize - 1,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draft.roundToInt()) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
