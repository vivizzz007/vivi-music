package com.music.vivi.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.vivi.constants.AppleMusicLyricsBlurKey
import com.music.vivi.constants.LyricsLetterByLetterAnimationKey
import com.music.vivi.lyrics.LyricsEntry
import com.music.vivi.ui.screens.settings.LyricsPosition
import com.music.vivi.utils.rememberPreference

/**
 * Displays a single line of lyrics.
 * Handles synchronization, word-by-word animation (karaoke style), active state styling, and romanization.
 */
@OptIn(ExperimentalTextApi::class, ExperimentalLayoutApi::class)
@Composable
public fun LyricsLine(
    entry: LyricsEntry,
    nextEntryTime: Long?,
    currentTime: Long,
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
    isWordForWord: Boolean,
    isScrolling: Boolean,
    isAutoScrollActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val (isLetterByLetter, _) = rememberPreference(LyricsLetterByLetterAnimationKey, false)

    val duration = remember(entry.time, nextEntryTime) {
        if (nextEntryTime != null) nextEntryTime - entry.time else 4000L
    }

    // Heuristic: Active highlighting should span about 95% of the duration for tighter feel
    val activeDuration = remember(duration) {
        (duration * 0.95).toLong().coerceAtLeast(300L)
    }

    // Segment the line into words with their own time windows
    val wordData = remember(entry.text, entry.words, activeDuration) {
        if (!entry.words.isNullOrEmpty()) {
            // Use precise ELRC word timestamps
            entry.words.mapIndexed { index, wordEntry ->
                val wordStart = (wordEntry.time - entry.time).coerceAtLeast(0L)
                val wordEnd = if (wordEntry.duration != null) {
                    wordStart + wordEntry.duration
                } else if (index < entry.words.lastIndex) {
                    (entry.words[index + 1].time - entry.time).coerceAtLeast(wordStart + 50L)
                } else {
                    activeDuration
                }
                Triple(wordEntry.text, wordStart, wordEnd)
            }
        } else {
            // Estimation for standard LRC based on word character counts
            val words = entry.text.split(" ")
            val totalChars = entry.text.length

            var accumulatedTime = 0L
            words.mapIndexed { index, word ->
                val wordLength = word.length
                val includeSpace = index < words.lastIndex
                val charCount = if (includeSpace) wordLength + 1 else wordLength

                val wordStart = accumulatedTime
                val wordDuration = if (totalChars >
                    0
                ) {
                    (activeDuration * charCount.toFloat() / totalChars).toLong()
                } else {
                    activeDuration
                }
                val wordEnd = wordStart + wordDuration

                accumulatedTime += wordDuration

                Triple(word, wordStart, wordEnd)
            }
        }
    }

    // Apple Music Style: Blur inactive lines
    // Apple Music Style: Blur inactive lines
    val (appleMusicLyricsBlur) = rememberPreference(AppleMusicLyricsBlurKey, true)
    val blurRadius by animateFloatAsState(
        targetValue = if (!appleMusicLyricsBlur ||
            !isAutoScrollActive ||
            isActive ||
            !isSynced ||
            isSelectionModeActive
        ) {
            0f
        } else {
            6f
        },
        animationSpec = tween(durationMillis = 600),
        label = "blur"
    )

    val itemModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp)) // Rounder corners
        .combinedClickable(
            enabled = true,
            onClick = onClick,
            onLongClick = onLongClick
        )
        .background(
            if (isSelected && isSelectionModeActive) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            } else {
                Color.Transparent
            }
        )
        .padding(horizontal = 24.dp, vertical = (lineSpacing + 4).dp) // Slightly more breathing room
        .graphicsLayer {
            val targetAlpha = when {
                !isSynced || (isSelectionModeActive && isSelected) -> 1f
                isActive -> 1f
                distanceFromCurrent == 1 -> 0.5f // High contrast
                distanceFromCurrent == 2 -> 0.25f
                else -> 0.15f
            }
            alpha = targetAlpha

            // Smooth scaling for active line
            val targetScale = when {
                !isSynced || isActive -> 1f
                distanceFromCurrent == 1 -> 0.9f
                else -> 0.8f
            }
            scaleX = targetScale
            scaleY = targetScale
        }
        .blur(blurRadius.dp)

    Column(
        modifier = itemModifier,
        horizontalAlignment = when (lyricsTextPosition) {
            LyricsPosition.LEFT -> Alignment.Start
            LyricsPosition.CENTER -> Alignment.CenterHorizontally
            LyricsPosition.RIGHT -> Alignment.End
        }
    ) {
        if (entry.isInstrumental) {
            InstrumentalDots(
                dotColor = textColor,
                modifier = Modifier.alpha(if (isActive) 1f else 0.4f),
                horizontalArrangement = when (lyricsTextPosition) {
                    LyricsPosition.LEFT -> Arrangement.Start
                    LyricsPosition.CENTER -> Arrangement.Center
                    LyricsPosition.RIGHT -> Arrangement.End
                }
            )
        } else if (isActive && isSynced) {
            if (isWordForWord && entry.words != null) {
                // Apple Music Style: Smooth Karaoke Fill
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = when (lyricsTextPosition) {
                        LyricsPosition.LEFT -> Arrangement.Start
                        LyricsPosition.CENTER -> Arrangement.Center
                        LyricsPosition.RIGHT -> Arrangement.End
                    }
                ) {
                    wordData.forEachIndexed { index, (word, startRelative, endRelative) ->
                        val lineRelTime = (currentTime - entry.time).coerceAtLeast(0L)

                        // Smooth "Karaoke" fill animation
                        val wordProgress by remember(lineRelTime, startRelative, endRelative) {
                            derivedStateOf {
                                when {
                                    lineRelTime >= endRelative -> 1f
                                    lineRelTime < startRelative -> 0f
                                    else -> {
                                        val wordDur = endRelative - startRelative
                                        if (wordDur <= 0L) {
                                            1f
                                        } else {
                                            (lineRelTime - startRelative).toFloat() / wordDur
                                        }
                                    }
                                }
                            }
                        }
                        // Perfect Apple Music "Filling" Style
                        val shaderStyle = remember(wordProgress, textColor) {
                            if (wordProgress >= 1f) {
                                null // Fully sung: use standard solid color
                            } else if (wordProgress <= 0f) {
                                null // Not sung: use standard dim color
                            } else {
                                // In progress: scale gradient to the word width
                                object : androidx.compose.ui.graphics.ShaderBrush() {
                                    override fun createShader(
                                        size: androidx.compose.ui.geometry.Size,
                                    ): androidx.compose.ui.graphics.Shader {
                                        val width = size.width
                                        val p = wordProgress.coerceIn(0f, 1f)
                                        // Create a hard-ish edge gradient for the fill effect
                                        // Active color -> Active color at p -> Inactive color at p+fade -> Inactive color
                                        return androidx.compose.ui.graphics.LinearGradientShader(
                                            from = Offset.Zero,
                                            to = Offset(width, 0f),
                                            colors = listOf(
                                                textColor,
                                                textColor,
                                                textColor.copy(alpha = 0.3f),
                                                textColor.copy(alpha = 0.3f)
                                            ),
                                            colorStops = listOf(0f, p, (p + 0.15f).coerceAtMost(1f), 1f)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = if (index != wordData.lastIndex) "$word " else word,
                            fontSize = textSize.sp,
                            style = if (shaderStyle !=
                                null
                            ) {
                                TextStyle(brush = shaderStyle)
                            } else {
                                androidx.compose.material3.LocalTextStyle.current
                            },
                            color = if (shaderStyle !=
                                null
                            ) {
                                Color.Unspecified
                            } else if (wordProgress >=
                                1f
                            ) {
                                textColor
                            } else {
                                textColor.copy(alpha = 0.3f)
                            },
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }
            } else if (isLetterByLetter && entry.words != null) {
                // Letter by Letter Animation
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = when (lyricsTextPosition) {
                        LyricsPosition.LEFT -> Arrangement.Start
                        LyricsPosition.CENTER -> Arrangement.Center
                        LyricsPosition.RIGHT -> Arrangement.End
                    }
                ) {
                    wordData.forEachIndexed { index, (word, startRelative, endRelative) ->
                        val lineRelTime = (currentTime - entry.time).coerceAtLeast(0L)
                        val wordDuration = endRelative - startRelative

                        Row {
                            word.forEachIndexed { charIndex, char ->
                                val charProgress by remember(
                                    lineRelTime,
                                    startRelative,
                                    endRelative,
                                    charIndex,
                                    word.length
                                ) {
                                    derivedStateOf {
                                        val charDuration = if (word.isNotEmpty()) wordDuration / word.length else 0L
                                        val charStart = startRelative + (charIndex * charDuration)
                                        val charEnd = charStart + charDuration

                                        when {
                                            lineRelTime >= charEnd -> 1f
                                            lineRelTime < charStart -> 0f
                                            else -> {
                                                if (charDuration <= 0L) {
                                                    1f
                                                } else {
                                                    (lineRelTime - charStart).toFloat() / charDuration
                                                }
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = char.toString(),
                                    fontSize = textSize.sp,
                                    color = textColor.copy(
                                        alpha = if (charProgress >=
                                            1f
                                        ) {
                                            1f
                                        } else {
                                            0.3f + (0.7f * charProgress)
                                        }
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                )
                            }
                            if (index != wordData.lastIndex) {
                                Text(text = " ", fontSize = textSize.sp)
                            }
                        }
                    }
                }
            } else {
                // Sentence Animation: Simple Line Highlighting (No word-level effects)
                Text(
                    text = entry.text,
                    fontSize = textSize.sp,
                    color = textColor, // Full active color
                    textAlign = when (lyricsTextPosition) {
                        LyricsPosition.LEFT -> TextAlign.Left
                        LyricsPosition.CENTER -> TextAlign.Center
                        LyricsPosition.RIGHT -> TextAlign.Right
                    },
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // Inactive line
            Text(
                text = entry.text,
                fontSize = textSize.sp,
                color = textColor.copy(alpha = 1f), // Alpha handled by graphicsLayer
                textAlign = when (lyricsTextPosition) {
                    LyricsPosition.LEFT -> TextAlign.Left
                    LyricsPosition.CENTER -> TextAlign.Center
                    LyricsPosition.RIGHT -> TextAlign.Right
                },
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Romanized text
        if (showRomanized) {
            val romanizedText by entry.romanizedTextFlow.collectAsState()
            romanizedText?.let { romanized ->
                Text(
                    text = romanized,
                    fontSize = (textSize * 0.65f).sp,
                    color = textColor.copy(alpha = 0.6f),
                    textAlign = when (lyricsTextPosition) {
                        LyricsPosition.LEFT -> TextAlign.Left
                        LyricsPosition.CENTER -> TextAlign.Center
                        LyricsPosition.RIGHT -> TextAlign.Right
                    },
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                    modifier = Modifier.padding(top = 2.dp).fillMaxWidth()
                )
            }
        }
    }
}
