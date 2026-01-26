package com.music.vivi.ui.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/**
 * Applies a fading edge effect to the component.
 * Useful for scrollable containers to indicate more content.
 *
 * @param left Width of the fade on the left edge.
 * @param top Height of the fade on the top edge.
 * @param right Width of the fade on the right edge.
 * @param bottom Height of the fade on the bottom edge.
 */
fun Modifier.fadingEdge(left: Dp? = null, top: Dp? = null, right: Dp? = null, bottom: Dp? = null) =
    graphicsLayer(alpha = 0.99f)
        .drawWithCache {
            val topBrush = top?.let {
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = it.toPx()
                )
            }
            val bottomBrush = bottom?.let {
                Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = size.height - it.toPx(),
                    endY = size.height
                )
            }
            val leftBrush = left?.let {
                Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = 0f,
                    endX = it.toPx()
                )
            }
            val rightBrush = right?.let {
                Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startX = size.width - it.toPx(),
                    endX = size.width
                )
            }

            onDrawWithContent {
                drawContent()
                topBrush?.let { drawRect(brush = it, blendMode = BlendMode.DstIn) }
                bottomBrush?.let { drawRect(brush = it, blendMode = BlendMode.DstIn) }
                leftBrush?.let { drawRect(brush = it, blendMode = BlendMode.DstIn) }
                rightBrush?.let { drawRect(brush = it, blendMode = BlendMode.DstIn) }
            }
        }

fun Modifier.fadingEdge(horizontal: Dp? = null, vertical: Dp? = null) = fadingEdge(
    left = horizontal,
    right = horizontal,
    top = vertical,
    bottom = vertical
)
