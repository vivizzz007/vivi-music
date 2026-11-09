/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.vivi.utils.SyncStatus

/**
 * Computes a continuous, fluid sinusoidal wave intensity (0f..1f) across a normalized progress cycle.
 */
private fun computeWaveIntensity(progress: Float, peakCenter: Float): Float {
    var diff = kotlin.math.abs(progress - peakCenter)
    if (diff > 0.5f) diff = 1f - diff
    val normalized = (diff / 0.25f).coerceIn(0f, 1f)
    return 0.5f * (1f + kotlin.math.cos(normalized * Math.PI).toFloat())
}

@Composable
private fun rememberSegmentBlink(status: SyncStatus): Float {
    val blinkAnim = remember { Animatable(1f) }

    LaunchedEffect(status) {
        if (status is SyncStatus.Syncing) {
            // Category needs an update: blink 2 times then proceed to fluid flow
            repeat(2) {
                blinkAnim.animateTo(0.2f, tween(140, easing = LinearEasing))
                blinkAnim.animateTo(1.0f, tween(140, easing = LinearEasing))
            }
        } else {
            blinkAnim.snapTo(1f)
        }
    }

    return blinkAnim.value
}

/**
 * 3-segment curved library sync indicator positioned directly on the right side of the round avatar image.
 *
 * Behaviors:
 * - Loads already at 100% if library was previously synced (lastSyncTime > 0L).
 * - Immediately shows fluid progress wave if background sync is actively running.
 * - When user triggers sync and everything is 100%, all 3 segments blink together 3 times.
 * - When a segment needs update, it blinks 2 times then displays the fluid wave while syncing.
 */
@Composable
fun AccountSyncIndicator(
    artistsStatus: SyncStatus,
    playlistsStatus: SyncStatus,
    albumsStatus: SyncStatus,
    modifier: Modifier = Modifier,
    lastSyncTime: Long = 0L,
    syncTriggerCount: Int = 0,
    avatarSize: Dp = 56.dp,
    strokeThickness: Dp = 8.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f),
    errorColor: Color = MaterialTheme.colorScheme.error,
    avatarContent: @Composable () -> Unit,
) {
    val isAnySyncing = artistsStatus is SyncStatus.Syncing ||
            playlistsStatus is SyncStatus.Syncing ||
            albumsStatus is SyncStatus.Syncing

    val isLoadedBefore = lastSyncTime > 0L

    val isSegment1Complete = artistsStatus is SyncStatus.Completed || (isLoadedBefore && artistsStatus is SyncStatus.Idle)
    val isSegment2Complete = playlistsStatus is SyncStatus.Completed || (isLoadedBefore && playlistsStatus is SyncStatus.Idle)
    val isSegment3Complete = albumsStatus is SyncStatus.Completed || (isLoadedBefore && albumsStatus is SyncStatus.Idle)

    val isAll100Percent = isSegment1Complete && isSegment2Complete && isSegment3Complete && !isAnySyncing

    // Unison 3-blink animation when everything is 100% and user triggers sync
    val unisonBlinkAnim = remember { Animatable(1f) }
    var lastHandledTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(syncTriggerCount) {
        if (syncTriggerCount > 0 && syncTriggerCount != lastHandledTrigger) {
            lastHandledTrigger = syncTriggerCount
            if (isAll100Percent) {
                repeat(3) {
                    unisonBlinkAnim.animateTo(0.2f, tween(130, easing = LinearEasing))
                    unisonBlinkAnim.animateTo(1.0f, tween(130, easing = LinearEasing))
                }
            }
        }
    }

    // Individual 2-blink when a category needs update / enters syncing
    val blink1 = rememberSegmentBlink(artistsStatus)
    val blink2 = rememberSegmentBlink(playlistsStatus)
    val blink3 = rememberSegmentBlink(albumsStatus)

    // Master fluid clock for syncing wave (active whenever background or foreground sync is happening)
    val infiniteTransition = rememberInfiniteTransition(label = "syncFluidTransition")
    val fluidProgress by if (isAnySyncing) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1300, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "fluidFlowProgress"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val wave1 = if (isAnySyncing) computeWaveIntensity(fluidProgress, 0.18f) else 0f
    val wave2 = if (isAnySyncing) computeWaveIntensity(fluidProgress, 0.50f) else 0f
    val wave3 = if (isAnySyncing) computeWaveIntensity(fluidProgress, 0.82f) else 0f

    // Calculate colors and opacities
    val targetColor1 = when {
        artistsStatus is SyncStatus.Error -> errorColor
        isSegment1Complete || artistsStatus is SyncStatus.Syncing -> activeColor
        else -> trackColor
    }
    val animatedColor1 by animateColorAsState(targetColor1, tween(500), label = "c1")
    val alpha1 = when {
        artistsStatus is SyncStatus.Error -> 0.90f
        artistsStatus is SyncStatus.Syncing -> (0.35f + 0.65f * wave1) * blink1
        isSegment1Complete -> 0.95f
        else -> 0.25f
    } * unisonBlinkAnim.value

    val targetColor2 = when {
        playlistsStatus is SyncStatus.Error -> errorColor
        isSegment2Complete || playlistsStatus is SyncStatus.Syncing -> activeColor
        else -> trackColor
    }
    val animatedColor2 by animateColorAsState(targetColor2, tween(500), label = "c2")
    val alpha2 = when {
        playlistsStatus is SyncStatus.Error -> 0.90f
        playlistsStatus is SyncStatus.Syncing -> (0.35f + 0.65f * wave2) * blink2
        isSegment2Complete -> 0.95f
        else -> 0.25f
    } * unisonBlinkAnim.value

    val targetColor3 = when {
        albumsStatus is SyncStatus.Error -> errorColor
        isSegment3Complete || albumsStatus is SyncStatus.Syncing -> activeColor
        else -> trackColor
    }
    val animatedColor3 by animateColorAsState(targetColor3, tween(500), label = "c3")
    val alpha3 = when {
        albumsStatus is SyncStatus.Error -> 0.90f
        albumsStatus is SyncStatus.Syncing -> (0.35f + 0.65f * wave3) * blink3
        isSegment3Complete -> 0.95f
        else -> 0.25f
    } * unisonBlinkAnim.value

    val containerWidth = avatarSize + 18.dp
    val containerHeight = avatarSize + 18.dp

    Box(
        modifier = modifier.size(width = containerWidth, height = containerHeight),
        contentAlignment = Alignment.CenterStart
    ) {
        // Round Avatar positioned at CenterStart
        Box(
            modifier = Modifier.size(avatarSize)
        ) {
            avatarContent()
        }

        // 3-segment curved gauge along the right contour of the avatar
        Canvas(modifier = Modifier.fillMaxSize()) {
            val avatarRadius = (avatarSize / 2).toPx()
            val centerX = avatarRadius
            val centerY = size.height / 2f
            val center = Offset(centerX, centerY)

            val strokeWidth = strokeThickness.toPx()
            val halfStroke = strokeWidth / 2f
            val gaugeRadius = avatarRadius + 4.dp.toPx() + halfStroke

            // Segment sweep and gap angles
            val sweep = 28f
            val gap = 5.5f

            val startAngleMiddle = -(sweep / 2f)                  // -14f .. +14f (Middle / Playlists)
            val startAngleTop = startAngleMiddle - gap - sweep    // -47.5f .. -19.5f (Top / Artists)
            val startAngleBottom = startAngleMiddle + sweep + gap // +19.5f .. +47.5f (Bottom / Albums)

            // 1. Top segment: Artists (Rounded top start, flat bottom end)
            drawSegmentPath(
                center = center,
                gaugeRadius = gaugeRadius,
                halfStroke = halfStroke,
                startAngle = startAngleTop,
                sweepAngle = sweep,
                hasRoundStart = true,
                hasRoundEnd = false,
                color = animatedColor1.copy(alpha = alpha1)
            )

            // 2. Middle segment: Playlists (Flat top end, flat bottom end)
            drawSegmentPath(
                center = center,
                gaugeRadius = gaugeRadius,
                halfStroke = halfStroke,
                startAngle = startAngleMiddle,
                sweepAngle = sweep,
                hasRoundStart = false,
                hasRoundEnd = false,
                color = animatedColor2.copy(alpha = alpha2)
            )

            // 3. Bottom segment: Liked Albums (Flat top start, rounded bottom end)
            drawSegmentPath(
                center = center,
                gaugeRadius = gaugeRadius,
                halfStroke = halfStroke,
                startAngle = startAngleBottom,
                sweepAngle = sweep,
                hasRoundStart = false,
                hasRoundEnd = true,
                color = animatedColor3.copy(alpha = alpha3)
            )
        }
    }
}

private fun DrawScope.drawSegmentPath(
    center: Offset,
    gaugeRadius: Float,
    halfStroke: Float,
    startAngle: Float,
    sweepAngle: Float,
    hasRoundStart: Boolean,
    hasRoundEnd: Boolean,
    color: Color,
) {
    val path = buildSegmentPath(
        center = center,
        gaugeRadius = gaugeRadius,
        halfStroke = halfStroke,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        hasRoundStart = hasRoundStart,
        hasRoundEnd = hasRoundEnd
    )
    drawPath(path = path, color = color)
}

/**
 * Builds a single contiguous filled Path for an arc segment.
 * The outward rounded cap is drawn as a true geometric continuation with zero double-draw.
 */
private fun buildSegmentPath(
    center: Offset,
    gaugeRadius: Float,
    halfStroke: Float,
    startAngle: Float,
    sweepAngle: Float,
    hasRoundStart: Boolean,
    hasRoundEnd: Boolean,
): Path {
    val endAngle = startAngle + sweepAngle
    val rOuter = gaugeRadius + halfStroke
    val rInner = gaugeRadius - halfStroke

    val outerRect = Rect(center.x - rOuter, center.y - rOuter, center.x + rOuter, center.y + rOuter)
    val innerRect = Rect(center.x - rInner, center.y - rInner, center.x + rInner, center.y + rInner)

    val radStart = Math.toRadians(startAngle.toDouble())
    val capStartCenter = Offset(
        center.x + gaugeRadius * kotlin.math.cos(radStart).toFloat(),
        center.y + gaugeRadius * kotlin.math.sin(radStart).toFloat()
    )
    val capStartRect = Rect(
        capStartCenter.x - halfStroke,
        capStartCenter.y - halfStroke,
        capStartCenter.x + halfStroke,
        capStartCenter.y + halfStroke
    )

    val radEnd = Math.toRadians(endAngle.toDouble())
    val capEndCenter = Offset(
        center.x + gaugeRadius * kotlin.math.cos(radEnd).toFloat(),
        center.y + gaugeRadius * kotlin.math.sin(radEnd).toFloat()
    )
    val capEndRect = Rect(
        capEndCenter.x - halfStroke,
        capEndCenter.y - halfStroke,
        capEndCenter.x + halfStroke,
        capEndCenter.y + halfStroke
    )

    val pInnerStart = Offset(
        center.x + rInner * kotlin.math.cos(radStart).toFloat(),
        center.y + rInner * kotlin.math.sin(radStart).toFloat()
    )

    return Path().apply {
        moveTo(pInnerStart.x, pInnerStart.y)

        // Outward rounded cap at start
        if (hasRoundStart) {
            arcTo(capStartRect, startAngle + 180f, 180f, false)
        }

        // Outer arc forward
        arcTo(outerRect, startAngle, sweepAngle, false)

        // Outward rounded cap at end
        if (hasRoundEnd) {
            arcTo(capEndRect, endAngle, 180f, false)
        }

        // Inner arc backward
        arcTo(innerRect, endAngle, -sweepAngle, false)

        close()
    }
}
