/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.extensions

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

fun Modifier.SwipeGesture(
    enabled: Boolean = true,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    swipeThreshold: Float = 100f
): Modifier = if (enabled) {
    this.pointerInput(Unit) {
        var totalDrag = 0f

        detectHorizontalDragGestures(
            onDragStart = {
                totalDrag = 0f
            },
            onDragEnd = {
                if (abs(totalDrag) > swipeThreshold) {
                    if (totalDrag > 0) {
                        onSwipeRight()
                    } else {
                        onSwipeLeft()
                    }
                }
                totalDrag = 0f
            },
            onDragCancel = {
                totalDrag = 0f
            },
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                totalDrag += dragAmount
            }
        )
    }
} else {
    this
}
