/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.vivi.constants.AppleMusicLyricsBlurKey
import com.music.vivi.lyrics.LyricsEntry
import com.music.vivi.ui.screens.settings.LyricsPosition
import com.music.vivi.utils.rememberPreference

/**
 * Exact Apple Music style lyrics animation ported from vivi-music-5.0.3
 * Features:
 * - Word-by-word filling gradient animation
 * - Blur effect for inactive lines (configurable)
 * - Proportional alpha and scaling based on distance from current line
 * - Balanced typography and Material 3 expressive accents
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ViviMusicLyricsLine(
    entry: LyricsEntry,
    nextEntryTime: Long?,
    effectivePlaybackPosition: Long,
    isSynced: Boolean,
    isActive: Boolean,
    distanceFromCurrent: Int,
    lyricsTextPosition: LyricsPosition,
    textColor: Color,
    showRomanized: Boolean,
    textSize: Float,
    lineSpacing: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    isAutoScrollActive: Boolean,
    expressiveAccent: Color,
    modifier: Modifier = Modifier
) {
    val (appleMusicLyricsBlur) = rememberPreference(AppleMusicLyricsBlurKey, true)

    val targetBlur = if (!appleMusicLyricsBlur || !isAutoScrollActive || isActive || !isSynced || isSelectionModeActive) {
        0f
    } else {
        // Progressive blur: further away = more blur for a premium look
        when (distanceFromCurrent) {
            1 -> 0f
            2 -> 0f
            3 -> 2f
            4 -> 4f
            else -> 6f
        }
    }

    val animatedBlur by animateFloatAsState(
        targetValue = targetBlur,
        animationSpec = tween(durationMillis = 1000), label = "blur"
    )

    val duration = remember(entry.time, nextEntryTime) {
        if (nextEntryTime != null) nextEntryTime - entry.time else 4000L
    }

    // Heuristic: Active highlighting should span about 95% of the duration for tighter feel
    val activeDuration = remember(duration) {
        (duration * 0.95).toLong().coerceAtLeast(300L)
    }

    // Segment the line into words with their own time windows
    val wordData = remember(entry.text, entry.words, activeDuration) {
        val isHindiText = com.music.vivi.lyrics.LyricsUtils.isHindi(entry.text)
        if (!isHindiText && entry.words != null && entry.words.isNotEmpty()) {
            // Use precise word timestamps if available
            entry.words.mapIndexed { index, word ->
                val wordStart = ((word.startTime * 1000).toLong() - entry.time).coerceAtLeast(0L)
                val wordEnd = ((word.endTime * 1000).toLong() - entry.time).coerceAtLeast(wordStart + 50L)
                Triple(word.text, wordStart, wordEnd)
            }
        } else {
            // Estimation based on character counts for standard LRC
            val words = entry.text.split(" ").filter { it.isNotEmpty() }
            if (words.isEmpty()) {
                listOf(Triple(entry.text, 0L, activeDuration))
            } else {
                val totalChars = entry.text.length
                var accumulatedTime = 0L
                words.mapIndexed { index, word ->
                    val wordLength = word.length
                    val includeSpace = index < words.size - 1
                    val charCount = if (includeSpace) wordLength + 1 else wordLength
                    val wordStart = accumulatedTime
                    val wordDur = if (totalChars > 0) (activeDuration * charCount.toFloat() / totalChars).toLong() else activeDuration
                    val wordEnd = wordStart + wordDur
                    accumulatedTime += wordDur
                    Triple(word, wordStart, wordEnd)
                }
            }
        }
    }

    val targetAlpha = when {
        !isSynced || (isSelectionModeActive && isSelected) -> 1f
        isActive -> 1f
        distanceFromCurrent == 1 -> 0.65f // Increased from 0.45f
        distanceFromCurrent == 2 -> 0.45f // Increased from 0.25f
        else -> 0.35f // Increased from 0.15f
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 300),
        label = "lineAlpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = tween(durationMillis = 400),
        label = "lineScale"
    )

    val itemModifier = modifier
        .fillMaxWidth()
        .graphicsLayer {
            this.alpha = animatedAlpha
            this.scaleX = scale
            this.scaleY = scale
        }
        .clip(RoundedCornerShape(16.dp))
        .combinedClickable(
            enabled = true,
            onClick = onClick,
            onLongClick = onLongClick
        )
        .background(
            if (isSelected && isSelectionModeActive)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else Color.Transparent
        )
        .padding(horizontal = 24.dp, vertical = (8 * lineSpacing).dp)
        .blur(animatedBlur.dp)

    // Multi-singer support: determine alignment based on agent
    val agentAlignment = when {
        entry.isBackground -> Alignment.CenterHorizontally
        entry.agent == "v1" -> Alignment.Start
        entry.agent == "v2" -> Alignment.End
        entry.agent == "v1000" -> Alignment.CenterHorizontally
        else -> when (lyricsTextPosition) {
            LyricsPosition.LEFT -> Alignment.Start
            LyricsPosition.CENTER -> Alignment.CenterHorizontally
            LyricsPosition.RIGHT -> Alignment.End
        }
    }

    val agentTextAlign = when {
        entry.isBackground -> TextAlign.Center
        entry.agent == "v1" -> TextAlign.Left
        entry.agent == "v2" -> TextAlign.Right
        entry.agent == "v1000" -> TextAlign.Center
        else -> when (lyricsTextPosition) {
            LyricsPosition.LEFT -> TextAlign.Left
            LyricsPosition.CENTER -> TextAlign.Center
            LyricsPosition.RIGHT -> TextAlign.Right
        }
    }

    Column(
        modifier = itemModifier,
        horizontalAlignment = agentAlignment
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = when (agentTextAlign) {
                TextAlign.Center -> Arrangement.Center
                TextAlign.Right -> Arrangement.End
                else -> Arrangement.Start
            },
            verticalArrangement = Arrangement.spacedBy(
                // Use a capped spacing for internal wrapping to prevent "sentence break-off"
                with(LocalDensity.current) { (textSize * (lineSpacing.coerceAtMost(1.3f) - 1f)).sp.toDp() }
            )
        ) {
            wordData.forEachIndexed { index, (wordText, startRelative, endRelative) ->
                val lineRelTime = (effectivePlaybackPosition - entry.time).coerceAtLeast(0L)
                val wordDuration = (endRelative - startRelative).coerceAtLeast(1L)
                
                val progress by animateFloatAsState(
                    targetValue = when {
                        lineRelTime >= endRelative -> 1f
                        lineRelTime < startRelative -> 0f
                        else -> (lineRelTime - startRelative).toFloat() / wordDuration
                    },
                    animationSpec = tween(durationMillis = 150, easing = androidx.compose.animation.core.LinearEasing),
                    label = "wordProgress"
                )

                val finalFontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold

                // Single Layer Rendering with Dynamic ShaderBrush
                // This guarantees perfect alignment because the text is measured and drawn only once.
                Text(
                    text = wordText,
                    fontSize = textSize.sp,
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            0.0f to textColor,
                            (progress - 0.05f).coerceAtLeast(0f) to textColor,
                            (progress + 0.05f).coerceAtMost(1f) to textColor.copy(alpha = 0.45f),
                            1.0f to textColor.copy(alpha = 0.45f)
                        ),
                        fontWeight = finalFontWeight,
//                        letterSpacing = (-0.5).sp,
                        // Cap internal line height for wrapped words
                        lineHeight = (textSize * lineSpacing.coerceAtMost(1.3f)).sp,
                        textAlign = agentTextAlign,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = textColor.copy(alpha = 0.6f * progress),
                            offset = Offset.Zero,
                            blurRadius = (12f * progress).coerceAtLeast(0.1f)
                        )
                    )
                )
                if (index != wordData.lastIndex) {
                    Text(
                        text = " ",
                        fontSize = textSize.sp,
                        color = textColor.copy(alpha = if (lineRelTime >= endRelative) 1f else 0.45f), // Increased from 0.35f
                        lineHeight = (textSize * lineSpacing.coerceAtMost(1.3f)).sp,
                        style = TextStyle(
                            shadow = if (lineRelTime >= endRelative) {
                                androidx.compose.ui.graphics.Shadow(
                                    color = textColor.copy(alpha = 0.3f),
                                    offset = Offset.Zero,
                                    blurRadius = 6f
                                )
                            } else null
                        )
                    )
                }
            }
        }

        // Romanized text support
        if (showRomanized) {
            val romanizedText by entry.romanizedTextFlow.collectAsState()
            romanizedText?.let { romanized ->
                Text(
                    text = romanized,
                    fontSize = (textSize * 0.65f).sp,
                    color = textColor.copy(alpha = 0.6f),
                    textAlign = agentTextAlign,
                    fontWeight = FontWeight.SemiBold,
//                    letterSpacing = (-0.2).sp,
                    modifier = Modifier.padding(top = 2.dp).fillMaxWidth(),
                    lineHeight = (textSize * 0.65f * lineSpacing.coerceAtMost(1.3f)).sp
                )
            }
        }
    }
}
