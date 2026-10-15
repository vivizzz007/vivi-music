/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.music.vivi.R
import com.music.vivi.audio.AudioPulse
import com.music.vivi.ui.utils.LiveMeshConfig
import com.music.vivi.ui.utils.LowResolutionLayer
import com.music.vivi.ui.utils.liveMeshPeriodMillis
import com.music.vivi.ui.utils.reactiveZoom

@Composable
internal fun LiveMeshPreview(
    config: LiveMeshConfig,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "liveMeshPreview")
    val spin = if (config.reverseDirection) -1f else 1f

    val anchorRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = -360f * spin,
        animationSpec = infiniteRepeatable(
            animation = tween(liveMeshPeriodMillis(80000, config.speed), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anchorPreview"
    )
    val fastRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f * spin,
        animationSpec = infiniteRepeatable(
            animation = tween(liveMeshPeriodMillis(40000, config.speed), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fastPreview"
    )
    val slowRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f * spin,
        animationSpec = infiniteRepeatable(
            animation = tween(liveMeshPeriodMillis(60000, config.speed), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slowPreview"
    )

    val colorFilter = remember(config.saturation) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(config.saturation) })
    }
    val artwork = painterResource(R.drawable.weeknd)
    val blurPreview = { radiusDp: Float -> radiusDp * config.blurStrength / config.renderScale }

    Box(
        modifier = modifier
            .graphicsLayer {
                val zoom = reactiveZoom(
                    baseZoom = config.zoom,
                    beat = AudioPulse.beat(),
                    lyric = AudioPulse.lyric(),
                    beatReactivity = config.beatReactivity,
                    lyricReactivity = config.lyricReactivity,
                )
                scaleX = zoom
                scaleY = zoom
            }
    ) {
        LowResolutionLayer(factor = config.renderScale) {
            Image(
                painter = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurPreview(100f).dp)
                    .graphicsLayer { rotationZ = anchorRotation }
            )
            Image(
                painter = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                alignment = Alignment.TopStart,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurPreview(120f).dp)
                    .graphicsLayer {
                        rotationZ = fastRotation
                        alpha = config.layer2Alpha
                    }
            )
            Image(
                painter = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                alignment = Alignment.BottomEnd,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurPreview(120f).dp)
                    .graphicsLayer {
                        rotationZ = slowRotation
                        alpha = config.layer3Alpha
                    }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = config.darkTint))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = config.bottomShade))
                    )
                )
        )
    }
}
