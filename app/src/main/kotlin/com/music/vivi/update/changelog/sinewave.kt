package com.music.vivi.update.changelog

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SineWaveLine(
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    alpha: Float = 1f,
    strokeWidth: Dp = 2.dp,
    amplitude: Dp = 8.dp,
    waves: Float = 2f,
    phase: Float = 0f,
    animate: Boolean? = false,
    animationDurationMillis: Int = 2000,
    samples: Int = 400,
    cap: StrokeCap = StrokeCap.Round,
) {
    val density = LocalDensity.current

    val infiniteTransition = rememberInfiniteTransition(label = "SineWaveAnimation")

    val animatedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseAnimation"
    )

    val currentPhase = if (animate == true) animatedPhase else phase

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f

        val strokePx = with(density) { strokeWidth.toPx() }
        val ampPx = with(density) { amplitude.toPx() }

        if (w <= 0f || samples < 2) return@Canvas

        val path = Path().apply {
            val step = w / (samples - 1)
            moveTo(0f, centerY + (ampPx * sin(currentPhase)))
            for (i in 1 until samples) {
                val x = i * step
                val theta = (x / w) * (2f * PI.toFloat() * waves) + currentPhase
                val y = centerY + ampPx * sin(theta)
                lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokePx,
                cap = cap,
                join = StrokeJoin.Round
            ),
            alpha = alpha
        )
    }
}
