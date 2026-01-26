package com.music.vivi.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.music.vivi.R

/**
 * A sleek Volume Slider inspired by Android 16 design.
 * Features an animated color change on the icon and smooth progress indication.
 */
@Composable
public fun Android16VolumeSlider(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    iconPainter: Painter = painterResource(R.drawable.volume_up),
) {
    var width by remember { mutableFloatStateOf(0f) }
    val animatedVolume by animateFloatAsState(targetValue = volume, label = "volume_animation")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(CircleShape)
            .background(inactiveColor)
            .onPlaced { width = it.size.width.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (width > 0) {
                        onVolumeChange((offset.x / width).coerceIn(0f, 1f))
                    }
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    if (width > 0) {
                        onVolumeChange((change.position.x / width).coerceIn(0f, 1f))
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Progress Fill
        Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            drawRect(
                color = activeColor,
                size = size.copy(width = size.width * animatedVolume)
            )
        }

        // Icon with color switching based on progress
        Box(
            modifier = Modifier
                .padding(start = 20.dp)
                .size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            val iconOnColor = MaterialTheme.colorScheme.onPrimary
            val iconOffColor = MaterialTheme.colorScheme.onSurfaceVariant

            // Threshold-based color change for simplicity and high performance
            val iconTint by animateColorAsState(
                targetValue = if (animatedVolume > 0.15f) iconOnColor else iconOffColor,
                label = "icon_tint"
            )

            Icon(
                painter = iconPainter,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
