package com.music.vivi.ui.screens.equalizer.axion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.vivi.R
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularEqControl(
    bass: Float, mid: Float, treble: Float,
    enabled: Boolean,
    onBassChange: (Float) -> Unit,
    onMidChange: (Float) -> Unit,
    onTrebleChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val outline = MaterialTheme.colorScheme.outline // Switched from outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainerLow // Switched from surfaceBright
    val labelStyle = TextStyle(fontSize = 11.sp, color = onSurface)
    val valueStyle = TextStyle(fontSize = 13.sp, color = onSurface)

    val labelMid = stringResource(R.string.eq_label_mid)
    val labelBass = stringResource(R.string.eq_label_bass)
    val labelTreble = stringResource(R.string.eq_label_treble)

    var activeAxis by remember { mutableIntStateOf(-1) }
    val currentBassChange by rememberUpdatedState(onBassChange)
    val currentMidChange by rememberUpdatedState(onMidChange)
    val currentTrebleChange by rememberUpdatedState(onTrebleChange)

    val angles = remember { doubleArrayOf(-PI / 2, -PI / 2 + 2 * PI / 3, -PI / 2 + 4 * PI / 3) }

    fun calcValueForAxis(pos: Offset, w: Float, h: Float, axisIdx: Int): Float {
        val cx = w / 2f
        val cy = h / 2f
        val baseR = w / 2f * 0.35f
        val angle = angles[axisIdx]
        val axDx = cos(angle).toFloat()
        val axDy = sin(angle).toFloat()
        val dot = (pos.x - cx) * axDx + (pos.y - cy) * axDy
        return ((dot / baseR - 1f) / 0.8f * 10f).coerceIn(-10f, 10f)
    }

    fun findNearestAxis(pos: Offset, w: Float, h: Float): Int {
        val cx = w / 2f
        val cy = h / 2f
        val touchAngle = atan2((pos.y - cy).toDouble(), (pos.x - cx).toDouble())
        var bestIdx = 0
        var bestDiff = Double.MAX_VALUE
        for (i in 0..2) {
            val diff = atan2(sin(touchAngle - angles[i]), cos(touchAngle - angles[i]))
            val absDiff = kotlin.math.abs(diff)
            if (absDiff < bestDiff) { bestDiff = absDiff; bestIdx = i }
        }
        val dist = kotlin.math.hypot((pos.x - cx).toDouble(), (pos.y - cy).toDouble())
        return if (dist < w * 0.48f) bestIdx else -1
    }

    fun dispatchToAxis(axisIdx: Int, value: Float) {
        when (axisIdx) {
            0 -> currentMidChange(value)
            1 -> currentBassChange(value)
            2 -> currentTrebleChange(value)
        }
    }

    Canvas(
        modifier = modifier
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val axis = findNearestAxis(down.position, size.width.toFloat(), size.height.toFloat())
                    if (axis < 0) return@awaitEachGesture
                    down.consume()

                    activeAxis = axis
                    val v = calcValueForAxis(down.position, size.width.toFloat(), size.height.toFloat(), axis)
                    dispatchToAxis(axis, v)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        change.consume()
                        
                        val dragV = calcValueForAxis(change.position, size.width.toFloat(), size.height.toFloat(), axis)
                        dispatchToAxis(axis, dragV)
                    }
                    activeAxis = -1
                }
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = size.width / 2f * 0.9f
        val baseR = size.width / 2f * 0.35f

        drawCircle(surfaceContainer, outerR, Offset(cx, cy))
        drawCircle(outline.copy(alpha = 0.25f), outerR, Offset(cx, cy), style = Stroke(1.5f))

        val values = floatArrayOf(mid, bass, treble)
        val labels = arrayOf(labelMid, labelBass, labelTreble)

        for (i in 0..2) {
            val ex = cx + outerR * 0.88f * cos(angles[i]).toFloat()
            val ey = cy + outerR * 0.88f * sin(angles[i]).toFloat()
            drawLine(outline.copy(alpha = 0.2f), Offset(cx, cy), Offset(ex, ey), strokeWidth = 1f)

            for (dot in 1..7) {
                val dr = baseR * (0.3f + dot * 0.2f)
                val dx = cx + dr * cos(angles[i]).toFloat()
                val dy = cy + dr * sin(angles[i]).toFloat()
                drawCircle(outline.copy(alpha = 0.5f), 3.5f, Offset(dx, dy))
            }
        }

        drawCircle(outline.copy(alpha = 0.1f), baseR, Offset(cx, cy), style = Stroke(0.8f))

        val segments = 72
        val wavePath = Path()
        for (s in 0..segments) {
            val t = s.toFloat() / segments
            val segAngle = t * 2.0 * PI - PI / 2

            var r = baseR
            for (i in 0..2) {
                val norm = (values[i] / 10f).coerceIn(-1f, 1f)
                val influence = baseR * norm * 0.8f
                val diff = segAngle - angles[i]
                val wrap = atan2(sin(diff), cos(diff))
                val w = cos(wrap * 0.75).toFloat().coerceAtLeast(0f)
                r += influence * w * w
            }

            val px = cx + r * cos(segAngle).toFloat()
            val py = cy + r * sin(segAngle).toFloat()
            if (s == 0) wavePath.moveTo(px, py) else wavePath.lineTo(px, py)
        }
        wavePath.close()

        drawPath(wavePath, primaryContainer.copy(alpha = 0.12f))
        drawPath(wavePath, primary.copy(alpha = 0.4f), style = Stroke(2f, cap = StrokeCap.Round))

        val points = Array(3) { i ->
            val norm = (values[i] / 10f).coerceIn(-1f, 1f)
            val r = baseR * (1f + norm * 0.8f)
            Offset(cx + r * cos(angles[i]).toFloat(), cy + r * sin(angles[i]).toFloat())
        }

        for (i in 0..2) {
            val isActive = (activeAxis == i)
            drawCircle(primary.copy(alpha = if (isActive) 0.2f else 0.1f), if (isActive) 32f else 26f, points[i])
            drawCircle(if (isActive) primary else onSurface, if (isActive) 18f else 14f, points[i])
        }

        for (i in 0..2) {
            val sign = if (values[i] >= 0) "+" else ""
            val label = textMeasurer.measure(labels[i], labelStyle)
            val value = textMeasurer.measure("${sign}${values[i].toInt()}", valueStyle)
            val labelR = outerR * 0.78f
            val lx = cx + labelR * cos(angles[i]).toFloat()
            val ly = cy + labelR * sin(angles[i]).toFloat()

            drawText(label, onSurface.copy(alpha = 0.6f), Offset(lx - label.size.width / 2f, ly - label.size.height - 2f))
            drawText(value, onSurface, Offset(lx - value.size.width / 2f, ly + 2f))
        }
    }
}
