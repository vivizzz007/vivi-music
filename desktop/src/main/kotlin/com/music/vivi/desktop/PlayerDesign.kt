package com.music.vivi.desktop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/** Player layout variant (ported from the mobile player-design toggles). */
enum class PlayerDesign(val key: String) {
    CLASSIC("classic"),
    NEW("new"),
    V2("v2"),
    EXPRESSIVE("expressive");

    companion object {
        fun from(key: String?): PlayerDesign = entries.firstOrNull { it.key == key } ?: CLASSIC
    }
}

/** Background style behind the full player (the mobile has these variants). */
enum class PlayerBackgroundStyle(val key: String) {
    CANVAS("canvas"),
    GRADIENT("gradient"),
    BLUR("blur"),
    GLOW("glow"),
    APPLE_MUSIC("apple_music"),
    LIVE_MESH("live_mesh");

    companion object {
        fun from(key: String?): PlayerBackgroundStyle = entries.firstOrNull { it.key == key } ?: CANVAS
    }
}

/** Mini-player style (port of the mobile mini-player variants). */
enum class MiniPlayerStyle(val key: String) {
    STANDARD("standard"),
    APPLE("apple"),
    OUTLINE("outline"),
    PURE_BLACK("pure_black");

    companion object {
        fun from(key: String?): MiniPlayerStyle = entries.firstOrNull { it.key == key } ?: STANDARD
    }
}

/** Resolves the art size + title-overlay for a [PlayerDesign] variant. */
data class PlayerDesignMetrics(val artSize: Dp, val overlayTitle: Boolean, val artCorner: Dp)

fun PlayerDesign.metrics(): PlayerDesignMetrics = when (this) {
    PlayerDesign.CLASSIC -> PlayerDesignMetrics(200.dp, false, 12.dp)
    PlayerDesign.NEW -> PlayerDesignMetrics(240.dp, false, 16.dp)
    PlayerDesign.V2 -> PlayerDesignMetrics(280.dp, true, 20.dp)
    PlayerDesign.EXPRESSIVE -> PlayerDesignMetrics(320.dp, true, 24.dp)
}

/**
 * Full-screen background behind the Player, honoring the selected
 * [PlayerBackgroundStyle] (canvas / gradient / blur / glow / apple music /
 * live mesh).
 */
@Composable
fun PlayerBackground(
    style: PlayerBackgroundStyle,
    bgUrl: String?,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().clipToBounds()) {
        when (style) {
            PlayerBackgroundStyle.CANVAS -> CanvasBackground(bgUrl, Modifier.fillMaxSize())
            PlayerBackgroundStyle.GRADIENT -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to accent.copy(alpha = 0.45f),
                                0.6f to accent.copy(alpha = 0.15f),
                                1f to Color.Transparent,
                            )
                        )
                )
            }
            PlayerBackgroundStyle.BLUR -> {
                if (bgUrl != null) {
                    AsyncImage(
                        model = bgUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(48.dp),
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )
            }
            PlayerBackgroundStyle.GLOW -> {
                val transition = rememberInfiniteTransition(label = "glow")
                val pulse by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 0.6f,
                    animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
                    label = "glowPulse",
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accent.copy(alpha = pulse),
                                    Color.Transparent,
                                    Color.Transparent,
                                ),
                                center = Offset(0.5f, 0.4f),
                                radius = 1400f,
                            )
                        )
                )
            }
            PlayerBackgroundStyle.APPLE_MUSIC -> {
                if (bgUrl != null) {
                    AsyncImage(
                        model = bgUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(36.dp).graphicsLayer { scaleX = 1.15f; scaleY = 1.15f },
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.35f),
                                1f to Color.Black.copy(alpha = 0.8f),
                            )
                        )
                )
            }
            PlayerBackgroundStyle.LIVE_MESH -> LiveMeshBackground(accent)
        }
    }
}

/** Animated multi-blob gradient (live mesh). */
@Composable
private fun BoxScope.LiveMeshBackground(accent: Color) {
    val transition = rememberInfiniteTransition(label = "mesh")
    val dx by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "meshDx",
    )
    val dy by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "meshDy",
    )
    val dz by transition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "meshDz",
    )
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = size.width * dx; translationY = size.height * dy }
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.5f), Color.Transparent),
                        radius = 1200f,
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = size.width * -dx * 0.8f; translationY = size.height * dz }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6A4BFF).copy(alpha = 0.4f),
                            Color.Transparent,
                        ),
                        radius = 1000f,
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = size.width * dy; translationY = size.height * -dx * 0.7f }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00BFA5).copy(alpha = 0.35f),
                            Color.Transparent,
                        ),
                        radius = 900f,
                    )
                )
        )
    }
}

/**
 * Album artwork that slowly rotates when [rotating] is enabled (the mobile
 * "rotating thumbnail" option).
 */
@Composable
fun PlayerThumbnail(
    url: String?,
    size: Dp,
    corner: Dp,
    rotating: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!rotating) {
        Box(
            modifier
                .size(size)
                .clip(RoundedCornerShape(corner))
                .background(Color.Black.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Thumbnail(url, Modifier.fillMaxSize())
        }
        return
    }
    val transition = rememberInfiniteTransition(label = "rotatingArt")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation",
    )
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                rotationZ = angle
                // Slight breathing scale while rotating.
                scaleX = 1f + 0.02f * kotlin.math.sin(angle * kotlin.math.PI / 180.0).toFloat()
                scaleY = 1f + 0.02f * kotlin.math.sin(angle * kotlin.math.PI / 180.0).toFloat()
            }
            .clip(RoundedCornerShape(corner))
            .background(Color.Black.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Thumbnail(url, Modifier.fillMaxSize())
    }
}
