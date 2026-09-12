/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay

/**
 * Apple Music style lyrics animation — ViviMusic_1
 * Features:
 * - Word-by-word fill with spring easing (FastOutSlowInEasing)
 * - Spring-based scale with gentle bounce on activation
 * - Y-offset entrance animation when a line becomes active
 * - Sentence linger: active state held 180ms after line change for softer handoff
 * - Softer curved inactive alpha falloff (0.75 → 0.50 → 0.30 → 0.20)
 * - Faster blur transition (600ms FastOutSlowInEasing)
 * - Space glyphs animated in sync with preceding word progress
 * - No-word-timestamp fallback: whole sentence sweeps at once
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
    showTranslated: Boolean,
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

    // ── Sentence linger ────────────────────────────────────────────────────────
    // Keep the "active" state alive for 180 ms after the line deactivates so
    // the last word can finish its fill animation before cross-fading away.
    val lingeredIsActive = remember { mutableStateOf(false) }
    LaunchedEffect(isActive) {
        if (isActive) {
            lingeredIsActive.value = true
        } else {
            delay(180L)
            lingeredIsActive.value = false
        }
    }

    // ── Blur ──────────────────────────────────────────────────────────────────
    val targetBlur = if (!appleMusicLyricsBlur || !isAutoScrollActive || isActive || !isSynced || isSelectionModeActive) {
        0f
    } else {
        // Progressive blur: further away = more blur
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
        // Faster, more reactive blur — 600 ms instead of 1000 ms
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "blur"
    )

    // ── Line duration for sentence-level timing ────────────────────────────────
    val duration = remember(entry.time, nextEntryTime) {
        if (nextEntryTime != null) nextEntryTime - entry.time else 4000L
    }
    val activeDuration = remember(duration) {
        (duration * 0.95).toLong().coerceAtLeast(300L)
    }

    // ── Word data ─────────────────────────────────────────────────────────────
    val hasWordTimestamps = entry.words != null && entry.words.isNotEmpty() &&
            !com.music.vivi.lyrics.LyricsUtils.isHindi(entry.text)

    val wordData = remember(entry.text, entry.words, activeDuration) {
        if (hasWordTimestamps) {
            // Use precise word timestamps if available
            entry.words!!.mapIndexed { _, word ->
                val wordStart = ((word.startTime * 1000).toLong() - entry.time).coerceAtLeast(0L)
                val wordEnd = ((word.endTime * 1000).toLong() - entry.time).coerceAtLeast(wordStart + 50L)
                Triple(word.text, wordStart, wordEnd)
            }
        } else {
            // No word timestamps — null signals sentence-level fallback below
            null
        }
    }

    // ── Alpha falloff — soft curve ────────────────────────────────────────────
    val targetAlpha = when {
        !isSynced || (isSelectionModeActive && isSelected) -> 1f
        lingeredIsActive.value -> 1f
        distanceFromCurrent == 1 -> 0.75f   // was 0.65f
        distanceFromCurrent == 2 -> 0.50f   // was 0.45f
        distanceFromCurrent == 3 -> 0.30f   // was 0.35f
        else -> 0.20f                        // was 0.35f
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        // Slightly faster alpha cross-fade (250 ms) with smooth easing
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "lineAlpha"
    )

    // ── Scale ──────────────────────────────────────────────────────────────
    // Smooth tween — no bounce. Active line gently grows.
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
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

    // ── Agent alignment ───────────────────────────────────────────────────────
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
        if (wordData != null) {
            // ── WORD-BY-WORD mode — Ocean Wave ────────────────────────────────
            // ONE global wave progress (0→1) sweeps continuously from the first
            // word to the last. Each word just reads where the wave front sits
            // within its own bounds, creating a single fluid motion across the
            // whole sentence instead of per-word independent fills.

            val globalEnd = remember(wordData) {
                wordData.last().third.coerceAtLeast(1L) // endRelative of the last word
            }
            val lineRelTime = (effectivePlaybackPosition - entry.time).coerceAtLeast(0L)

            val rawGlobalWave = (lineRelTime.toFloat() / globalEnd.toFloat()).coerceIn(0f, 1f)

            // Animate as one smooth value — tight follow (80ms) so the wave
            // tracks time accurately while staying fluid.
            val globalWave by animateFloatAsState(
                targetValue = rawGlobalWave,
                animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
                label = "globalWaveProgress"
            )

            // Wave-front feather width: 12% of each word's local span.
            // Wider = softer, more ocean-like edge.
            val waveFeather = 0.12f

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = when (agentTextAlign) {
                    TextAlign.Center -> Arrangement.Center
                    TextAlign.Right -> Arrangement.End
                    else -> Arrangement.Start
                },
                verticalArrangement = Arrangement.spacedBy(
                    with(LocalDensity.current) {
                        (textSize * (lineSpacing.coerceAtMost(1.3f) - 1f)).sp.toDp()
                    }
                )
            ) {
                wordData.forEachIndexed { index, (wordText, startRelative, endRelative) ->
                    // Map global wave position into this word's local 0→1 space.
                    val wordStartFrac = startRelative.toFloat() / globalEnd
                    val wordEndFrac = endRelative.toFloat() / globalEnd
                    val wordSpan = (wordEndFrac - wordStartFrac).coerceAtLeast(0.001f)

                    // Local word progress: 0 = wave hasn't reached this word yet,
                    // 1 = wave has fully passed through this word.
                    val wordLocalProgress = ((globalWave - wordStartFrac) / wordSpan)
                        .coerceIn(0f, 1f)

                    // Glow tracks fill intensity
                    val glowAlpha = 0.6f * wordLocalProgress
                    val glowRadius = (12f * wordLocalProgress).coerceAtLeast(0.1f)

                    val finalFontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold

                    // ── Wave brush — trailing feather only ───────────────────
                    // Apple Music: content BEFORE the wave = fully bright.
                    // Content AFTER the wave = fully dim.
                    // Only the wave FRONT itself has a soft feather edge.
                    // No leading feather — that's what was causing the first-letter
                    // highlight bug (fillEdgeStart clamped to 0 when wave barely entered).
                    val waveFront = wordLocalProgress
                    val waveTail = (wordLocalProgress + waveFeather).coerceAtMost(1f)

                    val wordBrush = when {
                        // Wave hasn't reached this word yet → pure dim, no gradient
                        wordLocalProgress <= 0f -> Brush.horizontalGradient(
                            colors = listOf(
                                textColor.copy(alpha = 0.45f),
                                textColor.copy(alpha = 0.45f)
                            )
                        )
                        // Wave has fully passed this word → pure bright, no gradient
                        wordLocalProgress >= 1f -> Brush.horizontalGradient(
                            colors = listOf(textColor, textColor)
                        )
                        // Wave is actively crossing through → trailing-feather gradient
                        else -> Brush.horizontalGradient(
                            0f        to textColor,                      // solid bright (filled)
                            waveFront to textColor,                      // bright right at the front
                            waveTail  to textColor.copy(alpha = 0.45f),  // soft fade to dim
                            1f        to textColor.copy(alpha = 0.45f)   // solid dim (unfilled)
                        )
                    }

                    Text(
                        text = wordText,
                        fontSize = textSize.sp,
                        style = TextStyle(
                            brush = wordBrush,
                            fontWeight = finalFontWeight,
                            lineHeight = (textSize * lineSpacing.coerceAtMost(1.3f)).sp,
                            textAlign = agentTextAlign,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = textColor.copy(alpha = glowAlpha),
                                offset = Offset.Zero,
                                blurRadius = glowRadius
                            )
                        )
                    )

                    if (index != wordData.lastIndex) {
                        // Space glyph follows the wave front — fully bright once the
                        // wave has passed this word, dim while it's still ahead.
                        val spaceAlpha = (0.45f + 0.55f * wordLocalProgress).coerceIn(0.45f, 1f)
                        Text(
                            text = " ",
                            fontSize = textSize.sp,
                            color = textColor.copy(alpha = spaceAlpha),
                            lineHeight = (textSize * lineSpacing.coerceAtMost(1.3f)).sp,
                            style = TextStyle(
                                shadow = if (wordLocalProgress >= 1f) {
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
        } else {
            // ── SENTENCE-LEVEL fallback ───────────────────────────────────────
            // No word timestamps → highlight the whole sentence at once, Apple-style.
            val targetSentenceAlpha = if (isActive || lingeredIsActive.value) 1f else 0.45f

            val sentenceAlpha by animateFloatAsState(
                targetValue = targetSentenceAlpha,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                ),
                label = "sentenceAlpha"
            )

            val finalFontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold

            Text(
                text = entry.text,
                fontSize = textSize.sp,
                color = textColor.copy(alpha = sentenceAlpha),
                style = TextStyle(
                    fontWeight = finalFontWeight,
                    lineHeight = (textSize * lineSpacing.coerceAtMost(1.3f)).sp,
                    textAlign = agentTextAlign,
                    shadow = if (sentenceAlpha > 0.45f) {
                        val factor = (sentenceAlpha - 0.45f) / 0.55f
                        androidx.compose.ui.graphics.Shadow(
                            color = textColor.copy(alpha = 0.5f * factor),
                            offset = Offset.Zero,
                            blurRadius = (10f * factor).coerceAtLeast(0.1f)
                        )
                    } else null
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ── Romanized text ────────────────────────────────────────────────────
        if (showRomanized) {
            val romanizedText by entry.romanizedTextFlow.collectAsState()
            romanizedText?.let { romanized ->
                Text(
                    text = romanized,
                    fontSize = (textSize * 0.65f).sp,
                    color = textColor.copy(alpha = 0.6f),
                    textAlign = agentTextAlign,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp).fillMaxWidth(),
                    lineHeight = (textSize * 0.65f * lineSpacing.coerceAtMost(1.3f)).sp
                )
            }
        }

        // ── Translated text ───────────────────────────────────────────────────
        if (showTranslated) {
            val translatedText by entry.translatedTextFlow.collectAsState()
            translatedText?.let { translated ->
                Text(
                    text = translated,
                    fontSize = (textSize * 0.7f).sp,
                    color = textColor.copy(alpha = 0.8f),
                    textAlign = agentTextAlign,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                    lineHeight = (textSize * 0.7f * lineSpacing.coerceAtMost(1.3f)).sp
                )
            }
        }
    }
}
