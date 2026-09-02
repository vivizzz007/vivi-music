package com.music.vivi.vivimusic

import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

val MaterialStarShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val baseWidth = 865.0807f
        val baseHeight = 865.0807f

        val path = Path().apply {
            moveTo(403.3913f, 8.7356f)
            cubicTo(421.0787f, -2.9119f, 444.002f, -2.9119f, 461.6894f, 8.7356f)
            lineTo(518.743f, 46.3066f)
            cubicTo(528.2839f, 52.5895f, 539.5995f, 55.6215f, 551.0036f, 54.9508f)
            lineTo(619.1989f, 50.9402f)
            cubicTo(640.3404f, 49.6968f, 660.1926f, 61.1585f, 669.6865f, 80.0892f)
            lineTo(700.3109f, 141.1534f)
            cubicTo(705.4321f, 151.365f, 713.7157f, 159.6486f, 723.9273f, 164.7699f)
            lineTo(784.9915f, 195.3942f)
            cubicTo(803.9222f, 204.8881f, 815.3839f, 224.7403f, 814.1406f, 245.8818f)
            lineTo(810.1299f, 314.0771f)
            cubicTo(809.4593f, 325.4812f, 812.4913f, 336.7969f, 818.7742f, 346.3378f)
            lineTo(856.3451f, 403.3913f)
            cubicTo(867.9926f, 421.0787f, 867.9927f, 444.002f, 856.3452f, 461.6894f)
            lineTo(818.7742f, 518.743f)
            cubicTo(812.4913f, 528.2839f, 809.4593f, 539.5995f, 810.1299f, 551.0036f)
            lineTo(814.1406f, 619.1989f)
            cubicTo(815.3839f, 640.3404f, 803.9223f, 660.1926f, 784.9916f, 669.6865f)
            lineTo(723.9274f, 700.3109f)
            cubicTo(713.7158f, 705.4321f, 705.4321f, 713.7157f, 700.3109f, 723.9273f)
            lineTo(669.6866f, 784.9915f)
            cubicTo(660.1926f, 803.9222f, 640.3404f, 815.3839f, 619.1989f, 814.1406f)
            lineTo(551.0036f, 810.1299f)
            cubicTo(539.5995f, 809.4593f, 528.2839f, 812.4913f, 518.743f, 818.7742f)
            lineTo(461.6894f, 856.3451f)
            cubicTo(444.0021f, 867.9926f, 421.0787f, 867.9927f, 403.3914f, 856.3452f)
            lineTo(346.3378f, 818.7742f)
            cubicTo(336.7969f, 812.4913f, 325.4812f, 809.4593f, 314.0771f, 810.1299f)
            lineTo(245.8818f, 814.1406f)
            cubicTo(224.7404f, 815.3839f, 204.8882f, 803.9223f, 195.3942f, 784.9916f)
            lineTo(164.7699f, 723.9274f)
            cubicTo(159.6486f, 713.7158f, 151.365f, 705.4321f, 141.1534f, 700.3109f)
            lineTo(80.0892f, 669.6866f)
            cubicTo(61.1585f, 660.1926f, 49.6968f, 640.3404f, 50.9402f, 619.199f)
            lineTo(54.9508f, 551.0036f)
            cubicTo(55.6215f, 539.5995f, 52.5895f, 528.2839f, 46.3066f, 518.743f)
            lineTo(8.7356f, 461.6894f)
            cubicTo(-2.9119f, 444.0021f, -2.9119f, 421.0787f, 8.7356f, 403.3914f)
            lineTo(46.3066f, 346.3378f)
            cubicTo(52.5895f, 336.7969f, 55.6215f, 325.4813f, 54.9508f, 314.0771f)
            lineTo(50.9402f, 245.8818f)
            cubicTo(49.6968f, 224.7404f, 61.1585f, 204.8882f, 80.0892f, 195.3942f)
            lineTo(141.1534f, 164.7699f)
            cubicTo(151.365f, 159.6486f, 159.6486f, 151.365f, 164.7699f, 141.1534f)
            lineTo(195.3942f, 80.0892f)
            cubicTo(204.8882f, 61.1585f, 224.7403f, 49.6968f, 245.8818f, 50.9402f)
            lineTo(314.0771f, 54.9508f)
            cubicTo(325.4813f, 55.6215f, 336.7969f, 52.5895f, 346.3378f, 46.3066f)
            lineTo(403.3913f, 8.7356f)
            close()
        }

        return Outline.Generic(
            path.asAndroidPath().apply {
                transform(Matrix().apply {
                    setScale(size.width / baseWidth, size.height / baseHeight)
                })
            }.asComposePath()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViviVolumeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = tween(100),
        label = "sliderValue"
    )

    Slider(
        value = animatedValue,
        onValueChange = onValueChange,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        modifier = modifier,
        interactionSource = interactionSource,
        thumb = { sliderState ->
            val range = sliderState.valueRange.endInclusive - sliderState.valueRange.start
            val fraction = if (range > 0f) {
                (sliderState.value - sliderState.valueRange.start) / range
            } else 0f
            
            val thumbColor = MaterialTheme.colorScheme.onPrimaryContainer
            
            Spacer(
                Modifier
                    .zIndex(100f)
                    .rotate(1080f * fraction)
                    .size(26.dp)
                    .background(thumbColor, MaterialStarShape)
            )
        },
        track = { sliderState ->
            val range = sliderState.valueRange.endInclusive - sliderState.valueRange.start
            val fraction = if (range > 0f) {
                (sliderState.value - sliderState.valueRange.start) / range
            } else 0f
            
            val activeTrackColor = MaterialTheme.colorScheme.primaryContainer
            val inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                val isRtl = layoutDirection == LayoutDirection.Rtl
                val activeStart = if (isRtl) 1f - fraction else 0f
                val activeEnd = if (isRtl) 1f - 0f else fraction
                
                drawFancyTrackSegment(
                    startFraction = 0f,
                    endFraction = 1f,
                    color = inactiveTrackColor,
                    shape = CircleShape
                )
                
                drawFancyTrackSegment(
                    startFraction = activeStart,
                    endFraction = activeEnd,
                    color = activeTrackColor,
                    shape = CircleShape
                )
            }
        }
    )
}

private fun DrawScope.drawFancyTrackSegment(
    startFraction: Float,
    endFraction: Float,
    color: Color,
    shape: Shape
) {
    val capRadius = size.height / 2f
    val left = size.width * startFraction.coerceIn(0f, 1f) - capRadius
    val right = size.width * endFraction.coerceIn(0f, 1f) + capRadius
    val width = right - left

    if (width <= 0f) return

    val outline = shape.createOutline(
        size = Size(width = width, height = size.height),
        layoutDirection = layoutDirection,
        density = this
    )

    translate(left = left) {
        when (outline) {
            is Outline.Rectangle -> drawRect(color = color, topLeft = outline.rect.topLeft, size = outline.rect.size)
            is Outline.Rounded -> drawPath(path = Path().apply { addRoundRect(outline.roundRect) }, color = color)
            is Outline.Generic -> drawPath(path = outline.path, color = color)
        }
    }
}
