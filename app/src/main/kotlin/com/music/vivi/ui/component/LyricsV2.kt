/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 * 
 * Ported and adapted from ArchiveTune-dev (LyricsV2.kt)
 */

package com.music.vivi.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.vivi.lyrics.LyricsEntry
import com.music.vivi.lyrics.WordTimestamp
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LyricsLineV2(
    entry: LyricsEntry,
    isActive: Boolean,
    isPast: Boolean,
    effectivePlaybackPosition: Long,
    expressiveAccent: Color,
    inactiveAlpha: Float,
    baseFontSize: Float,
    lineHeight: Float,
    agentAlignment: Alignment.Horizontal,
    agentTextAlign: TextAlign,
) {
    val arrangement = when (agentTextAlign) {
        TextAlign.Center -> Arrangement.Center
        TextAlign.Right -> Arrangement.End
        else -> Arrangement.Start
    }

    val words = entry.words ?: emptyList()

    if (words.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = arrangement,
            verticalArrangement = Arrangement.spacedBy(
                // Use a capped spacing for internal wrapping to prevent "sentence break-off"
                with(LocalDensity.current) { (baseFontSize * ( (lineHeight / baseFontSize).coerceAtMost(1.3f) - 1f)).sp.toDp() }
            )
        ) {
            words.forEachIndexed { wordIndex, word ->
                AnimatedWordV2(
                    word = word,
                    isLineActive = isActive,
                    isLinePast = isPast,
                    effectivePlaybackPosition = effectivePlaybackPosition,
                    expressiveAccent = expressiveAccent,
                    inactiveAlpha = inactiveAlpha,
                    fontSize = if (entry.isBackground) baseFontSize * 0.85f else baseFontSize,
                    lineHeight = lineHeight.coerceAtMost(baseFontSize * 1.3f),
                    isBackground = entry.isBackground
                )
                if (wordIndex < words.size - 1) {
                    Text(
                        text = " ",
                        fontSize = baseFontSize.sp,
                        lineHeight = lineHeight.coerceAtMost(baseFontSize * 1.3f).sp
                    )
                }
            }
        }
    } else {
        // Fallback for lines without word timings (e.g. standard LRC without word sync)
        Text(
            text = entry.text,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = if (entry.isBackground) (baseFontSize * 0.85f).sp else baseFontSize.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                fontStyle = if (entry.isBackground) FontStyle.Italic else FontStyle.Normal,
                lineHeight = lineHeight.coerceAtMost(baseFontSize * 1.3f).sp,
            ),
            color = expressiveAccent.copy(alpha = if (isActive) 1f else inactiveAlpha),
            textAlign = agentTextAlign,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun AnimatedWordV2(
    word: WordTimestamp,
    isLineActive: Boolean,
    isLinePast: Boolean,
    effectivePlaybackPosition: Long,
    expressiveAccent: Color,
    inactiveAlpha: Float,
    fontSize: Float,
    lineHeight: Float,
    isBackground: Boolean,
) {
    val wordStartMs = (word.startTime * 1000).toLong()
    val wordEndMs = (word.endTime * 1000).toLong()
    val wordDuration = (wordEndMs - wordStartMs).coerceAtLeast(1L)

    val isWordComplete = isLinePast || effectivePlaybackPosition >= wordEndMs
    val isWordActive = isLineActive && effectivePlaybackPosition in wordStartMs until wordEndMs

    // Perfect linear progress [0..1]
    val progress = when {
        isWordComplete -> 1f
        !isLineActive || effectivePlaybackPosition <= wordStartMs -> 0f
        else -> ((effectivePlaybackPosition - wordStartMs).toFloat() / wordDuration).coerceIn(0f, 1f)
    }

    // Bounce and Float animation
    val sinProgress = sin(progress * PI).toFloat()
    val wordScale = 1f + (0.015f * sinProgress)
    
    val targetFloat = if (isWordActive) -4f * sinProgress else 0f
    val floatOffset by animateFloatAsState(
        targetValue = targetFloat,
        animationSpec = tween(
            durationMillis = if (isWordActive) 50 else 350,
            easing = FastOutSlowInEasing
        ),
        label = "WordFloatOffset"
    )

    // Glow intensity
    val glowProgress = (progress * 2f).coerceAtMost(1f)
    val glowAlpha = if (isWordActive) glowProgress * 0.45f else 0f
    val glowRadius = if (isWordActive) glowProgress * 12f else 0f

    val density = LocalDensity.current
    val fontWeight = if (isLineActive) FontWeight.Bold else FontWeight.SemiBold

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationY = floatOffset * density.density
                scaleX = wordScale
                scaleY = wordScale
            }
    ) {
        // Layer 1: Base text (always dimmed)
        Text(
            text = word.text,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = fontSize.sp,
                fontWeight = fontWeight,
                fontStyle = if (isBackground) FontStyle.Italic else FontStyle.Normal,
                lineHeight = lineHeight.sp,
            ),
            color = expressiveAccent.copy(alpha = if (isBackground) inactiveAlpha * 0.7f else inactiveAlpha),
        )

        // Layer 2: Filled overlay with liquid sweep mask + glow
        if (isWordComplete || isWordActive) {
            Text(
                text = word.text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = fontSize.sp,
                    fontWeight = fontWeight,
                    fontStyle = if (isBackground) FontStyle.Italic else FontStyle.Normal,
                    lineHeight = lineHeight.sp,
                    shadow = if (glowAlpha > 0f) {
                        Shadow(
                            color = expressiveAccent.copy(alpha = glowAlpha),
                            offset = Offset.Zero,
                            blurRadius = glowRadius.coerceAtLeast(1f),
                        )
                    } else null,
                ),
                color = expressiveAccent.copy(alpha = if (isBackground) 0.75f else 1f),
                modifier = if (isWordActive) {
                    Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val edgeWidth = 8.dp.toPx()
                            val center = (size.width + edgeWidth * 2) * progress - edgeWidth
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Black, Color.Transparent),
                                    startX = center - edgeWidth,
                                    endX = center + edgeWidth,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                } else Modifier
            )
        }
    }
}
