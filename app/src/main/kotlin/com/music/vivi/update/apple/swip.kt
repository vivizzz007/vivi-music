package com.music.vivi.update.apple

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

fun Modifier.SwipeGesture(
    enabled: Boolean = true,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    swipeThreshold: Float = 100f,
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
                        onSwipeLeft()
                    } else {
                        onSwipeRight()
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
