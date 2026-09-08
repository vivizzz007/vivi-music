/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

object SettingTargetManager {
    var targetSettingKey: String? by mutableStateOf(null)
}

/**
 * Modifier that monitors SettingTargetManager.targetSettingKey.
 * When the key matches, it smoothly scrolls the container so this setting item
 * is vertically centered in the middle of the screen, and pulses a highlight animation.
 */
fun Modifier.settingTarget(
    key: String,
    scrollState: ScrollState
): Modifier = composed {
    val coroutineScope = rememberCoroutineScope()
    var isHighlighted by remember(key) { mutableStateOf(false) }

    val highlightAlpha by animateFloatAsState(
        targetValue = if (isHighlighted) 0.35f else 0f,
        animationSpec = if (isHighlighted) tween(150) else tween(1400, easing = LinearOutSlowInEasing),
        label = "settingHighlightAlpha"
    )

    val highlightColor = MaterialTheme.colorScheme.primary

    Modifier
        .onGloballyPositioned { coordinates ->
            if (SettingTargetManager.targetSettingKey == key) {
                SettingTargetManager.targetSettingKey = null
                isHighlighted = true
                coroutineScope.launch {
                    // Small delay to let initial layout settle and software keyboard dismiss
                    delay(120)
                    val root = coordinates.findRootCoordinates()
                    val windowHeight = root.size.height
                    val positionInRoot = coordinates.positionInRoot()
                    val itemCenterY = positionInRoot.y + coordinates.size.height / 2f
                    val screenCenterY = windowHeight / 2f
                    val delta = itemCenterY - screenCenterY
                    val targetScroll = (scrollState.value + delta).roundToInt().coerceIn(0, scrollState.maxValue)
                    scrollState.animateScrollTo(
                        targetScroll,
                        animationSpec = tween(600, easing = FastOutSlowInEasing)
                    )
                    delay(1200)
                    isHighlighted = false
                }
            }
        }
        .drawBehind {
            if (highlightAlpha > 0.01f) {
                drawRoundRect(
                    color = highlightColor.copy(alpha = highlightAlpha),
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                )
            }
        }
}
