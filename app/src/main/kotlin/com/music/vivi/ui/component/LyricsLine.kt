package com.music.vivi.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    modifier: Modifier = Modifier
) {
    val duration = remember(entry.time, nextEntryTime) {
        if (nextEntryTime != null) nextEntryTime - entry.time else 3000L
    }
    
    // Compensate for LyricsUtils.findCurrentLineIndex 300ms lookahead offset
    // plus a small safety buffer to ensure words finish animating before the line switches.
    val activeDuration = remember(duration) {
         (duration - 450L).coerceAtLeast(duration / 2)
    }

    // Split words and calculate their simulated timing
    // Structure: List of (Word, StartRelative, EndRelative)
    val wordData = remember(entry.text, activeDuration) {
        val words = entry.text.split(" ")
        val totalLength = entry.text.length
        
        var currentOffset = 0L
        words.mapIndexed { index, word ->
            // Include trailing space in calculation if not last word to keep pacing natural
            val wordLengthWithSpace = if (index != words.lastIndex) word.length + 1 else word.length
            
            // Advance offset by word + space (approx)
            val wordStart = currentOffset
            val durationWeight = wordLengthWithSpace.toFloat() / totalLength
            // Ensure we don't exceed activeDuration
            val segmentDuration = (activeDuration * durationWeight).toLong()
            val wordEnd = wordStart + segmentDuration
            
            currentOffset += segmentDuration
            
            Triple(word, wordStart, wordEnd)
        }
    }

    val itemModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .combinedClickable(
            enabled = true,
            onClick = onClick,
            onLongClick = onLongClick
        )
        .background(
            if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else Color.Transparent
        )
        .padding(horizontal = 24.dp, vertical = 8.dp)
        .graphicsLayer {
            val targetAlpha = when {
                !isSynced || (isSelectionModeActive && isSelected) -> 1f
                isActive -> 1f
                distanceFromCurrent == 1 -> 0.7f
                distanceFromCurrent == 2 -> 0.4f
                else -> 0.2f
            }
            alpha = targetAlpha
            
            val targetScale = when {
                !isSynced || isActive -> 1f
                distanceFromCurrent == 1 -> 0.95f
                distanceFromCurrent >= 2 -> 0.9f
                else -> 1f
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
        // Main Lyrics Text
        if (isActive && isSynced) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = when (lyricsTextPosition) {
                    LyricsPosition.LEFT -> androidx.compose.foundation.layout.Arrangement.Start
                    LyricsPosition.CENTER -> androidx.compose.foundation.layout.Arrangement.Center
                    LyricsPosition.RIGHT -> androidx.compose.foundation.layout.Arrangement.End
                }
            ) {
                wordData.forEachIndexed { index, (word, startRelative, endRelative) ->
                     // Current time relative to line start
                     // Add small offset (150ms) to make animation feel snappier/lead the audio slightly
                    val lineRelTime = (currentTime - entry.time + 150L).coerceAtLeast(0L)
                    
                    // Word progress: 0f to 1f
                    val wordProgress by remember(lineRelTime, startRelative, endRelative) {
                        derivedStateOf {
                            when {
                                lineRelTime >= endRelative -> 1f // Passed
                                lineRelTime < startRelative -> 0f // Future
                                else -> {
                                    val dur = endRelative - startRelative
                                    if (dur <= 0) 1f else (lineRelTime - startRelative).toFloat() / dur
                                }
                            }
                        }
                    }
                    
                    val brush = remember(wordProgress, textColor) {
                        val filled = textColor
                        val empty = textColor.copy(alpha = 0.3f)
                        val edge = 0.1f // Reduced edge for faster visual onset
                        
                        // We scale the progress so that the "Filled" section (stopStart) reaches 1.0 
                        // exactly when wordProgress reaches 1.0.
                        // visualProgress goes from 0 to (1 + edge)
                        val visualProgress = wordProgress * (1f + edge)
                        
                        val stopEnd = visualProgress
                        val stopStart = visualProgress - edge

                        // Create gradient based on these stops
                        val stops = mutableListOf<Pair<Float, Color>>()

                        if (stopEnd <= 0f) {
                            // Fully empty
                             Brush.linearGradient(colors = listOf(empty, empty))
                        } else if (stopStart >= 1f) {
                            // Fully filled
                             Brush.linearGradient(colors = listOf(filled, filled))
                        } else {
                            if (stopStart > 0f) {
                                // Filled part start from 0 to stopStart
                                stops.add(0f to filled)
                                stops.add(stopStart to filled)
                            } else {
                                // Transition started before 0. We need to find color at 0.
                                // Interpolate from Filled (at stopStart) to Empty (at stopEnd)
                                // Distance of 0 from stopStart is (-stopStart)
                                // Total distance is edge.
                                val ratio = (-stopStart / edge).coerceIn(0f, 1f)
                                // ratio 0 -> Filled, ratio 1 -> Empty
                                // Interpolate alpha
                                val startAlpha = 1f + (0.3f - 1f) * ratio
                                stops.add(0f to filled.copy(alpha = startAlpha))
                            }
                            
                            if (stopEnd < 1f) {
                                // Transition ends at stopEnd, after that Empty
                                stops.add(stopEnd to empty)
                                stops.add(1f to empty)
                            } else {
                                // Transition continues past 1f
                                // Interpolate color at 1f
                                val ratio = ((1f - stopStart) / edge).coerceIn(0f, 1f)
                                val endAlpha = 1f + (0.3f - 1f) * ratio
                                stops.add(1f to filled.copy(alpha = endAlpha))
                            }
                            
                             Brush.horizontalGradient(
                                *stops.toTypedArray(),
                                tileMode = TileMode.Clamp
                            )
                        }
                    }

                    Text(
                        text = if (index != wordData.lastIndex) word + " " else word,
                        fontSize = 24.sp,
                        style = TextStyle(brush = brush),
                        fontWeight = FontWeight.ExtraBold,
                        // Ensure we don't clip the gradient if it's slightly off
                        modifier = Modifier.graphicsLayer { alpha = 1f } 
                    )
                }
            }
        } else {
            // Standard inactive text (single Text composable for better performance when not animating)
            Text(
                text = entry.text,
                fontSize = 24.sp,
                color = if (isActive && isSynced) {
                    textColor 
                } else {
                    textColor.copy(alpha = 0.8f) 
                },
                textAlign = when (lyricsTextPosition) {
                    LyricsPosition.LEFT -> TextAlign.Left
                    LyricsPosition.CENTER -> TextAlign.Center
                    LyricsPosition.RIGHT -> TextAlign.Right
                },
                fontWeight = if (isActive && isSynced) FontWeight.ExtraBold else FontWeight.Bold
            )
        }

        if (showRomanized) {
            val romanizedText by entry.romanizedTextFlow.collectAsState()
            romanizedText?.let { romanized ->
                Text(
                    text = romanized,
                    fontSize = 18.sp,
                    color = textColor.copy(alpha = 0.8f),
                    textAlign = when (lyricsTextPosition) {
                        LyricsPosition.LEFT -> TextAlign.Left
                        LyricsPosition.CENTER -> TextAlign.Center
                        LyricsPosition.RIGHT -> TextAlign.Right
                    },
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
