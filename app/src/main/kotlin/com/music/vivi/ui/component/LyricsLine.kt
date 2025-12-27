package com.music.vivi.ui.component

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.vivi.lyrics.LyricsEntry
import com.music.vivi.ui.screens.settings.LyricsPosition

@OptIn(ExperimentalTextApi::class, ExperimentalLayoutApi::class)
@Composable
fun LyricsLine(
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
    modifier: Modifier = Modifier
) {
    val duration = remember(entry.time, nextEntryTime) {
        if (nextEntryTime != null) nextEntryTime - entry.time else 4000L
    }

    // Heuristic: Active highlighting should span about 90% of the duration
    val activeDuration = remember(duration) {
        (duration * 0.9).toLong().coerceAtLeast(500L)
    }

    // Segment the line into words with their own time windows
    val wordData = remember(entry.text, entry.words, activeDuration) {
        if (entry.words != null && entry.words.isNotEmpty()) {
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
                val wordDuration = if (totalChars > 0) (activeDuration * charCount.toFloat() / totalChars).toLong() else activeDuration
                val wordEnd = wordStart + wordDuration

                accumulatedTime += wordDuration

                Triple(word, wordStart, wordEnd)
            }
        }
    }

    val itemModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .combinedClickable(
            enabled = true,
            onClick = onClick,
            onLongClick = onLongClick
        )
        .background(
            if (isSelected && isSelectionModeActive)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else Color.Transparent
        )
        .padding(horizontal = 24.dp, vertical = lineSpacing.dp)
        .graphicsLayer {
            val targetAlpha = when {
                !isSynced || (isSelectionModeActive && isSelected) -> 1f
                isActive -> 1f
                distanceFromCurrent == 1 -> 0.6f
                distanceFromCurrent == 2 -> 0.3f
                distanceFromCurrent >= 3 -> 0.15f
                else -> 0.1f
            }
            alpha = targetAlpha

            val targetScale = when {
                !isSynced || isActive -> 1f
                distanceFromCurrent == 1 -> 0.98f
                else -> 0.95f
            }
            scaleX = targetScale
            scaleY = targetScale
        }

    Column(
        modifier = itemModifier,
        horizontalAlignment = when (lyricsTextPosition) {
            LyricsPosition.LEFT -> Alignment.Start
            LyricsPosition.CENTER -> Alignment.CenterHorizontally
            LyricsPosition.RIGHT -> Alignment.End
        }
    ) {
        if (isActive && isSynced) {
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

                    if (isWordForWord) {
                        // Discrete word-for-word highlighting (no animation)
                        val isWordActive = lineRelTime >= startRelative
                        val wordColor = if (isWordActive) textColor else textColor.copy(alpha = 0.25f)

                        Text(
                            // Add trailing space back for all but last word
                            text = if (index != wordData.lastIndex) "$word " else word,
                            fontSize = textSize.sp, // Use dynamic size
                            color = wordColor,
                            style = TextStyle(
                                // Subtle shadow for better pop on vibrant backgrounds
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.15f),
                                    offset = Offset(0f, 2f),
                                    blurRadius = 4f
                                )
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp // Modern tight tracking
                        )
                    } else {
                        // Smooth "Glow" animation
                        // Calculate progress for THIS specific word (0 to 1)
                        val wordProgress by remember(lineRelTime, startRelative, endRelative) {
                            derivedStateOf {
                                when {
                                    lineRelTime >= endRelative -> 1f
                                    lineRelTime < startRelative -> 0f
                                    else -> {
                                        val wordDur = endRelative - startRelative
                                        if (wordDur <= 0L) 1f
                                        else (lineRelTime - startRelative).toFloat() / wordDur
                                    }
                                }
                            }
                        }

                        // Premium Brush: Soft Leading Edge (Glow Effect)
                        val brush = remember(wordProgress, textColor) {
                            val activeColor = textColor
                            val inactiveColor = textColor.copy(alpha = 0.25f) // More dim for future words
                            val edgeWidth = 0.25f // Wider edge for a softer, glowy fill

                            when {
                                wordProgress <= 0f -> Brush.linearGradient(colors = listOf(inactiveColor, inactiveColor))
                                wordProgress >= 1f -> Brush.linearGradient(colors = listOf(activeColor, activeColor))
                                else -> {
                                    val stops = mutableListOf<Pair<Float, Color>>()

                                    // Soft gradient from filled to empty
                                    val fillEnd = (wordProgress - edgeWidth / 2).coerceAtLeast(0f)
                                    val glowEnd = (wordProgress + edgeWidth / 2).coerceAtMost(1f)

                                    if (fillEnd > 0f) {
                                        stops.add(0f to activeColor)
                                        stops.add(fillEnd to activeColor)
                                    }

                                    // The "Glow" bridge
                                    stops.add(fillEnd to activeColor)
                                    stops.add(glowEnd to inactiveColor)

                                    if (glowEnd < 1f) {
                                        stops.add(glowEnd to inactiveColor)
                                        stops.add(1f to inactiveColor)
                                    }

                                    Brush.horizontalGradient(
                                        colorStops = stops.toTypedArray(),
                                        tileMode = TileMode.Clamp
                                    )
                                }
                            }
                        }

                        Text(
                            // Add trailing space back for all but last word
                            text = if (index != wordData.lastIndex) "$word " else word,
                            fontSize = textSize.sp, // Use dynamic size
                            style = TextStyle(
                                brush = brush,
                                // Subtle shadow for better pop on vibrant backgrounds
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.15f),
                                    offset = Offset(0f, 2f),
                                    blurRadius = 4f
                                )
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp // Modern tight tracking
                        )
                    }
                }
            }
        } else {
            // Inactive line
            Text(
                text = entry.text,
                fontSize = textSize.sp,
                color = textColor,
                textAlign = when (lyricsTextPosition) {
                    LyricsPosition.LEFT -> TextAlign.Left
                    LyricsPosition.CENTER -> TextAlign.Center
                    LyricsPosition.RIGHT -> TextAlign.Right
                },
                fontWeight = FontWeight.ExtraBold,
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
                    fontSize = (textSize * 0.7f).sp, // Scaled romanized text
                    color = textColor.copy(alpha = 0.6f),
                    textAlign = when (lyricsTextPosition) {
                        LyricsPosition.LEFT -> TextAlign.Left
                        LyricsPosition.CENTER -> TextAlign.Center
                        LyricsPosition.RIGHT -> TextAlign.Right
                    },
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
                )
            }
        }
    }
}
