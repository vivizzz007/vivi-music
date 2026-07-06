package com.music.vivi.ui.component

import android.graphics.BlurMaskFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.CompositingStrategy
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.constants.LyricsRomanizeAsMainKey
import com.music.vivi.lyrics.LyricsEntry
import com.music.vivi.ui.screens.settings.LyricsPosition
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

data class HyphenGroupWord(
    val pos: Int,
    val groupSize: Int,
    val isLast: Boolean,
    val groupStartMs: Long,
    val groupEndMs: Long
)

// Local wrapper that adds hasTrailingSpace for hyphen-split logic
private data class MetroWordTimestamp(
    val text: String,
    val startTime: Double,
    val endTime: Double,
    val hasTrailingSpace: Boolean
)

private fun String.containsRtl(): Boolean {
    for (c in this) {
        val directionality = Character.getDirectionality(c).toInt()
        if (directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT.toInt() ||
            directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC.toInt()
        ) {
            return true
        }
    }
    return false
}

private fun String.containsComplexScript(): Boolean {
    for (c in this) {
        val code = c.toInt()
        if (code in 0x0900..0x0FFF || // Devanagari, Bengali, Gurmukhi, Gujarati, Oriya, Tamil, Telugu, Kannada, Malayalam, Sinhala, Thai, Lao, Tibetan
            code in 0x1000..0x109F || // Myanmar
            code in 0x1780..0x17FF || // Khmer
            code in 0x0600..0x06FF || // Arabic
            code in 0x0750..0x077F || // Arabic Supplement
            code in 0x08A0..0x08FF || // Arabic Extended-A
            code in 0xFB50..0xFDFF || // Arabic Presentation Forms-A
            code in 0xFE70..0xFEFF    // Arabic Presentation Forms-B
        ) {
            return true
        }
    }
    return false
}

private fun String.toGraphemeClusters(): List<String> {
    if (isEmpty()) return emptyList()
    val result = mutableListOf<String>()
    val it = java.text.BreakIterator.getCharacterInstance()
    it.setText(this)
    var start = it.first()
    var end = it.next()
    while (end != java.text.BreakIterator.DONE) {
        result.add(substring(start, end))
        start = end
        end = it.next()
    }
    return result
}


@Composable
fun MetroLyricsLine(
    entry: LyricsEntry,
    nextEntryTime: Long?,
    effectivePlaybackPosition: Long,
    lyricsOffset: Long = 0L,
    isSynced: Boolean,
    isActive: Boolean,
    distanceFromCurrent: Int,
    lyricsTextPosition: LyricsPosition,
    textColor: Color,
    showRomanized: Boolean,
    showTranslated: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    isAutoScrollActive: Boolean,
    expressiveAccent: Color,
    bgVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val (romanizeAsMain) = rememberPreference(LyricsRomanizeAsMainKey, false)
    
    val romanizedTextState by entry.romanizedTextFlow.collectAsState()
    val isRomanizedAvailable = romanizedTextState != null
    
    val mainTextRaw = if (showRomanized && romanizeAsMain && isRomanizedAvailable) romanizedTextState else entry.text
    val subTextRaw = if (showRomanized && romanizeAsMain && isRomanizedAvailable) entry.text else if (showRomanized) romanizedTextState else null
    
    val mainText = if (entry.isBackground) mainTextRaw?.removePrefix("(")?.removeSuffix(")") ?: "" else mainTextRaw ?: ""
    val subText = if (entry.isBackground) subTextRaw?.removePrefix("(")?.removeSuffix(")") else subTextRaw

    val focusedAlpha = if (entry.isBackground) 0.5f else 0.3f
    val activeAlpha = 1f
    
    val targetAlpha = when {
        !isSynced || (isSelectionModeActive && isSelected) -> 1f
        entry.isBackground || isActive -> activeAlpha
        isAutoScrollActive && distanceFromCurrent >= 0 -> {
            when (distanceFromCurrent) {
                0 -> focusedAlpha
                1, 2 -> 0.2f
                3 -> 0.15f
                4 -> 0.1f
                else -> 0.08f
            }
        }
        else -> 0.2f
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 300),
        label = "lineAlpha"
    )

    val lyricsTextSize = 36f
    val lyricsLineSpacing = 1.3f

    val itemModifier = modifier
        .fillMaxWidth()
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
        .padding(
            start = 24.dp,
            end = 24.dp,
            top = if (entry.isBackground) 0.dp else (8 * lyricsLineSpacing).dp,
            bottom = if (entry.isBackground) 2.dp else (8 * lyricsLineSpacing).dp
        )

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

    val lyricStyle = TextStyle(
        fontSize = if (entry.isBackground) (lyricsTextSize * 0.7f).sp else lyricsTextSize.sp,
        fontWeight = FontWeight.Bold,
        fontStyle = if (entry.isBackground) FontStyle.Italic else FontStyle.Normal,
        lineHeight = if (entry.isBackground) (lyricsTextSize * 0.7f * lyricsLineSpacing).sp else (lyricsTextSize * lyricsLineSpacing).sp,
        letterSpacing = (-0.5).sp,
        textAlign = agentTextAlign,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both
        )
    )

    @Composable
    fun LyricContent() {
        Column(
            modifier = itemModifier,
            horizontalAlignment = agentAlignment
        ) {
            val wordList = entry.words
            val effectiveWords: List<MetroWordTimestamp> = if (wordList != null && wordList.isNotEmpty() && (!showRomanized || !romanizeAsMain || !isRomanizedAvailable || mainText == entry.text)) {
                wordList.mapIndexed { idx, word ->
                    MetroWordTimestamp(
                        text = word.text,
                        startTime = word.startTime,
                        endTime = word.endTime,
                        hasTrailingSpace = idx < wordList.size - 1
                    )
                }
            } else {
                remember(mainText, entry.time) {
                    val words = mainText.split(Regex("\\s+")).filter { it.isNotBlank() }
                    val wordDurationSec = 0.18
                    val wordStaggerSec = 0.03
                    val startTimeSec = entry.time / 1000.0
                    words.mapIndexed { idx, wordText ->
                        MetroWordTimestamp(
                            text = wordText,
                            startTime = startTimeSec + (idx * wordStaggerSec),
                            endTime = startTimeSec + (idx * wordStaggerSec) + wordDurationSec,
                            hasTrailingSpace = idx < words.size - 1
                        )
                    }
                }
            }

            val baseLineColor = expressiveAccent.copy(alpha = if (entry.isBackground) focusedAlpha else animatedAlpha)
            
            if (isSynced && effectiveWords.isNotEmpty() && (isActive || distanceFromCurrent <= 3) && mainText.isNotEmpty()) {
                WordLevelCanvasLyrics(
                    mainText = mainText,
                    words = effectiveWords,
                    isActiveLine = isActive,
                    effectivePlaybackPosition = effectivePlaybackPosition,
                    lyricsOffset = lyricsOffset,
                    lyricStyle = lyricStyle,
                    lineColor = if (isActive && !entry.isBackground) expressiveAccent.copy(alpha = 1f) else baseLineColor,
                    expressiveAccent = expressiveAccent,
                    isBackground = entry.isBackground,
                    focusedAlpha = focusedAlpha,
                    alignment = agentTextAlign
                )
            } else {
                Text(
                    text = mainText,
                    style = lyricStyle.copy(color = if (isActive && !entry.isBackground) expressiveAccent else baseLineColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (subText != null) {
                Text(
                    text = subText,
                    fontSize = 18.sp,
                    color = baseLineColor.copy(alpha = 0.6f),
                    textAlign = agentTextAlign,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp).fillMaxWidth(),
                    lineHeight = (18 * lyricsLineSpacing.coerceAtMost(1.3f)).sp
                )
            }

            if (showTranslated) {
                val translatedText by entry.translatedTextFlow.collectAsState()
                translatedText?.let { translated ->
                    Text(
                        text = translated,
                        fontSize = 16.sp,
                        color = expressiveAccent.copy(alpha = 0.8f),
                        textAlign = agentTextAlign,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                        lineHeight = (16 * lyricsLineSpacing.coerceAtMost(1.3f)).sp
                    )
                }
            }
        }
    }

    if (entry.isBackground) {
        AnimatedVisibility(
            visible = bgVisible,
            enter = fadeIn(tween(durationMillis = 250, delayMillis = 100)),
            exit = fadeOut(tween(250))
        ) {
            LyricContent()
        }
    } else {
        LyricContent()
    }
}

@Composable
private fun WordLevelCanvasLyrics(
    mainText: String,
    words: List<MetroWordTimestamp>,
    isActiveLine: Boolean,
    effectivePlaybackPosition: Long,
    lyricsOffset: Long = 0L,
    lyricStyle: TextStyle,
    lineColor: Color,
    expressiveAccent: Color,
    isBackground: Boolean,
    focusedAlpha: Float,
    alignment: TextAlign
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val glowPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
        }
    }
    
    // Read playerConnection directly — same as Metrolist. This avoids the 8ms polling
    // lag from effectivePlaybackPosition which is only updated by the Lyrics.kt loop.
    val playerConnection = LocalPlayerConnection.current

    var smoothPosition by remember { mutableLongStateOf(effectivePlaybackPosition + lyricsOffset) }
    
    LaunchedEffect(isActiveLine) {
        if (isActiveLine && playerConnection != null) {
            var lastPlayerPos = playerConnection.player.currentPosition
            var lastUpdateTime = System.currentTimeMillis()
            
            while (isActive) {
                withFrameMillis {
                    val now = System.currentTimeMillis()
                    val playerPos = playerConnection.player.currentPosition
                    val currentlyPlaying = playerConnection.player.isPlaying
                    
                    if (playerPos != lastPlayerPos || !currentlyPlaying) {
                        lastPlayerPos = playerPos
                        lastUpdateTime = now
                    }
                    
                    val elapsed = now - lastUpdateTime
                    smoothPosition = lastPlayerPos + lyricsOffset + (if (currentlyPlaying) elapsed else 0L)
                }
            }
        }
    }

    LaunchedEffect(effectivePlaybackPosition, isActiveLine) {
        if (!isActiveLine) {
            smoothPosition = effectivePlaybackPosition
        }
    }

    val (effectiveWords, effectiveToOriginalIdx) = remember(words, isBackground) {
        words.flatMapIndexed { originalIdx, word ->
            val shouldSplit = word.text.contains('-') && word.text.length > 1 &&
                (!word.hasTrailingSpace || words.size == 1)
            if (shouldSplit) {
                val segments = mutableListOf<String>()
                var start = 0
                for (i in 0 until word.text.length) {
                    if (word.text[i] == '-') {
                        segments.add(word.text.substring(start, i + 1))
                        start = i + 1
                    }
                }
                if (start < word.text.length) {
                    segments.add(word.text.substring(start))
                }

                if (segments.size > 1) {
                    val totalDuration = word.endTime - word.startTime
                    val segmentDuration = totalDuration / segments.size
                    segments.mapIndexed { index, segmentText ->
                        MetroWordTimestamp(
                            text = segmentText,
                            startTime = word.startTime + index * segmentDuration,
                            endTime = word.startTime + (index + 1) * segmentDuration,
                            hasTrailingSpace = if (index == segments.size - 1) word.hasTrailingSpace else false
                        ) to originalIdx
                    }
                } else listOf(word to originalIdx)
            } else listOf(word to originalIdx)
        }.let { data -> data.map { it.first } to data.map { it.second } }
    }

    val graphemeClusters = remember(mainText) { mainText.toGraphemeClusters() }
    val clusterCount = graphemeClusters.size
    
    val clusterCharOffsets = remember(mainText) {
        IntArray(clusterCount).also { offsets ->
            var charOffset = 0
            graphemeClusters.forEachIndexed { i, cluster ->
                offsets[i] = charOffset
                charOffset += cluster.length
            }
        }
    }

    val charToWordData = remember(mainText, effectiveWords, isBackground, graphemeClusters, clusterCharOffsets) {
        val wordIdxMap = IntArray(clusterCount) { -1 }
        val charInWordMap = IntArray(clusterCount)
        val wordLenMap = IntArray(clusterCount) { 1 }
        var currentPos = 0
        var clCursor = 0
        effectiveWords.forEachIndexed { wordIdx, word ->
            val rawWordText = word.text.let { 
                if (isBackground) {
                    var t = it
                    if (wordIdx == 0) t = t.removePrefix("(")
                    if (wordIdx == effectiveWords.size - 1) t = t.removeSuffix(")")
                    t
                } else it
            }
            val indexInMain = mainText.indexOf(rawWordText, currentPos)
            if (indexInMain != -1) {
                val wordEndInMain = indexInMain + rawWordText.length
                while (clCursor < clusterCount && clusterCharOffsets[clCursor] < indexInMain) {
                    clCursor++
                }
                val wordClusterIndices = mutableListOf<Int>()
                while (clCursor < clusterCount && clusterCharOffsets[clCursor] < wordEndInMain) {
                    wordClusterIndices.add(clCursor)
                    clCursor++
                }
                val wordClusterLen = wordClusterIndices.size
                wordClusterIndices.forEachIndexed { posInWord, clIdx ->
                    wordIdxMap[clIdx] = wordIdx
                    charInWordMap[clIdx] = posInWord
                    wordLenMap[clIdx] = wordClusterLen
                }
                if (clCursor < clusterCount && clusterCharOffsets[clCursor] == wordEndInMain && 
                    wordEndInMain < mainText.length && mainText[wordEndInMain] == ' ') {
                    val spaceClIdx = clCursor
                    wordIdxMap[spaceClIdx] = wordIdx
                    charInWordMap[spaceClIdx] = wordClusterLen
                    wordLenMap[spaceClIdx] = wordClusterLen + 1
                    clCursor++
                }
                currentPos = wordEndInMain
            }
        }
        Triple(wordIdxMap, charInWordMap, wordLenMap)
    }

    val hyphenGroupData = remember(effectiveWords) {
        val map = mutableMapOf<Int, HyphenGroupWord>()
        var currentGroup = mutableListOf<Int>()
        effectiveWords.forEachIndexed { wordIdx, word ->
            currentGroup.add(wordIdx)
            if (!word.text.endsWith("-")) {
                if (currentGroup.size > 1) {
                    val groupSize = currentGroup.size
                    val groupStartMs = (effectiveWords[currentGroup.first()].startTime * 1000).toLong()
                    val groupEndMs = (word.endTime * 1000).toLong()
                    currentGroup.forEachIndexed { pos, idx ->
                        map[idx] = HyphenGroupWord(pos, groupSize, pos == groupSize - 1, groupStartMs, groupEndMs)
                    }
                }
                currentGroup = mutableListOf()
            }
        }
        map
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxWidthPx = constraints.maxWidth
        val layoutResult = remember(mainText, maxWidthPx, lyricStyle) {
            textMeasurer.measure(
                text = mainText,
                style = lyricStyle,
                constraints = Constraints(minWidth = maxWidthPx, maxWidth = maxWidthPx),
                softWrap = true
            )
        }
        
        val letterLayouts = remember(mainText, lyricStyle) {
            graphemeClusters.map { cluster -> textMeasurer.measure(cluster, lyricStyle) }
        }
        
        val isRtlText = remember(mainText) { mainText.containsRtl() }
        val isComplexScript = remember(mainText) { mainText.containsComplexScript() }
        
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(with(density) { layoutResult.size.height.toDp() })
            .graphicsLayer(
                clip = false,
                compositingStrategy = CompositingStrategy.Offscreen
            )
        ) {
            if (mainText.isEmpty()) return@Canvas
            if (!isActiveLine) {
                drawText(layoutResult, color = lineColor)
            } else {
                if (isRtlText) {
                    val (wordIdxMap, _, _) = charToWordData
                    val wordFactors = effectiveWords.map { word ->
                        val wStartMs = (word.startTime * 1000).toLong()
                        val wEndMs = (word.endTime * 1000).toLong()
                        val isWordSung = smoothPosition > wEndMs
                        val isWordActive = smoothPosition in wStartMs..wEndMs
                        val sungFactor = if (isWordSung) 1f 
                                        else if (isWordActive) ((smoothPosition - wStartMs).toFloat() / (wEndMs - wStartMs).coerceAtLeast(1)).coerceIn(0f, 1f)
                                        else 0f
                        Triple(sungFactor, isWordSung, isWordActive)
                    }

                    drawText(layoutResult, color = lineColor.copy(alpha = focusedAlpha))

                    effectiveWords.indices.forEach { wIdx ->
                        val (sungFactor, isWordSung, isWordActive) = wordFactors[wIdx]
                        
                        var left = Float.MAX_VALUE
                        var right = Float.MIN_VALUE
                        var top = Float.MAX_VALUE
                        var bottom = Float.MIN_VALUE
                        var found = false

                        for (i in 0 until clusterCount) {
                            if (wordIdxMap[i] == wIdx) {
                                val charOffset = clusterCharOffsets[i]
                                val bounds = layoutResult.getBoundingBox(charOffset)
                                left = minOf(left, bounds.left)
                                right = maxOf(right, bounds.right)
                                top = minOf(top, bounds.top)
                                bottom = maxOf(bottom, bounds.bottom)
                                found = true
                            }
                        }

                        if (found) {
                            if (isWordSung) {
                                clipRect(left = left, top = top, right = right, bottom = bottom) {
                                    drawText(layoutResult, color = expressiveAccent)
                                }
                            } else if (isWordActive && sungFactor > 0f) {
                                clipRect(left = left, top = top, right = right, bottom = bottom) {
                                    drawText(layoutResult, color = expressiveAccent.copy(alpha = focusedAlpha + (1f - focusedAlpha) * sungFactor))
                                }
                            }
                        }
                    }
                    return@Canvas
                }

                if (isComplexScript) {
                    val (wordIdxMap, _, _) = charToWordData
                    val wordFactors = effectiveWords.map { word ->
                        val wStartMs = (word.startTime * 1000).toLong()
                        val wEndMs = (word.endTime * 1000).toLong()
                        val isWordSung = smoothPosition > wEndMs
                        val isWordActive = smoothPosition in wStartMs..wEndMs
                        val sungFactor = if (isWordSung) 1f 
                                        else if (isWordActive) ((smoothPosition - wStartMs).toFloat() / (wEndMs - wStartMs).coerceAtLeast(1)).coerceIn(0f, 1f)
                                        else 0f
                        Triple(sungFactor, isWordSung, isWordActive)
                    }

                    drawText(layoutResult, color = lineColor.copy(alpha = focusedAlpha))

                    effectiveWords.indices.forEach { wIdx ->
                        val (sungFactor, isWordSung, isWordActive) = wordFactors[wIdx]
                        
                        var left = Float.MAX_VALUE
                        var right = Float.MIN_VALUE
                        var top = Float.MAX_VALUE
                        var bottom = Float.MIN_VALUE
                        var found = false

                        for (i in 0 until clusterCount) {
                            if (wordIdxMap[i] == wIdx) {
                                val charOffset = clusterCharOffsets[i]
                                val bounds = layoutResult.getBoundingBox(charOffset)
                                left = minOf(left, bounds.left)
                                right = maxOf(right, bounds.right)
                                top = minOf(top, bounds.top)
                                bottom = maxOf(bottom, bounds.bottom)
                                found = true
                            }
                        }

                        if (found) {
                            if (isWordSung) {
                                clipRect(left = left, top = top, right = right, bottom = bottom) {
                                    drawText(layoutResult, color = expressiveAccent)
                                }
                            } else if (isWordActive && sungFactor > 0f) {
                                val wordWidth = right - left
                                val sweepX = left + wordWidth * sungFactor
                                if (sweepX > left) {
                                    clipRect(left = left, top = top, right = sweepX, bottom = bottom) {
                                        drawText(layoutResult, color = expressiveAccent)
                                    }
                                }
                                if (sweepX < right) {
                                    clipRect(left = sweepX, top = top, right = right, bottom = bottom) {
                                        drawText(layoutResult, color = lineColor.copy(alpha = focusedAlpha))
                                    }
                                }
                            }
                        }
                    }
                    return@Canvas
                }

                val (wordIdxMap, charInWordMap, wordLenMap) = charToWordData
                val wordFactors = effectiveWords.map { word ->
                    val wStartMs = (word.startTime * 1000).toLong()
                    val wEndMs = (word.endTime * 1000).toLong()
                    val isWordSung = smoothPosition > wEndMs
                    val isWordActive = smoothPosition in wStartMs..wEndMs
                    val sungFactor = if (isWordSung) 1f 
                                    else if (isWordActive) ((smoothPosition - wStartMs).toFloat() / (wEndMs - wStartMs).coerceAtLeast(1)).coerceIn(0f, 1f)
                                    else 0f
                    Triple(sungFactor, word, isWordSung)
                }

                val wordWobbles = FloatArray(words.size)
                words.forEachIndexed { wordIdx, word ->
                    val startMs = (word.startTime * 1000).toLong()
                    val timeSinceStart = (smoothPosition - startMs).toFloat()
                    val wobble = if (timeSinceStart in 0f..750f) {
                        if (timeSinceStart < 125f) timeSinceStart / 125f
                        else (1f - (timeSinceStart - 125f) / 625f).coerceAtLeast(0f)
                    } else 0f
                    wordWobbles[wordIdx] = wobble
                }

                val lineCurrentPushes = FloatArray(layoutResult.lineCount)
                val lineTotalPushes = FloatArray(layoutResult.lineCount)
                
                for (i in 0 until clusterCount) {
                    val charOffset = clusterCharOffsets[i]
                    val lineIdx = layoutResult.getLineForOffset(charOffset)
                    val wordIdx = wordIdxMap[i]
                    val originalWordIdx = if (wordIdx != -1) effectiveToOriginalIdx[wordIdx] else -1
                    val (sungFactor, wordItem, isWordSung) = if (wordIdx != -1) wordFactors[wordIdx] else Triple(0f, null, false)
                    val wobble = if (originalWordIdx != -1) wordWobbles[originalWordIdx] else 0f
                    var crescendoDeltaX = 0f
                    val groupWord = if (wordIdx != -1) hyphenGroupData[wordIdx] else null
                    if (groupWord != null) {
                        val p = sungFactor
                        val timeSinceEnd = (smoothPosition - groupWord.groupEndMs).toFloat()
                        val exitDuration = 600f
                        val pOut = (timeSinceEnd / exitDuration).coerceIn(0f, 1f)
                        val peakScale = 0.06f
                        val decay = 2.5f
                        val freq = 10.0f
                        val baseScalePerSegment = 0.012f
                        if (pOut > 0f) {
                            val baseAtEnd = groupWord.pos * baseScalePerSegment
                            val totalAtEnd = baseAtEnd + peakScale
                            crescendoDeltaX = totalAtEnd * exp(-decay * pOut) * cos(freq * pOut * PI.toFloat()) * (1f - pOut)
                        } else if (groupWord.isLast) {
                            val base = groupWord.pos * baseScalePerSegment
                            val springPart = peakScale * (1f - exp(-decay * p) * cos(freq * p * PI.toFloat()) * (1f - p))
                            crescendoDeltaX = base + springPart
                        } else {
                            val boost = if (p > 0f) 0.02f * (1f - p) else 0f
                            crescendoDeltaX = (groupWord.pos * baseScalePerSegment) + boost
                        }
                    }

                    val charLp = if (wordItem != null) {
                        val sMs = wordItem.startTime * 1000
                        val dur = (wordItem.endTime * 1000 - wordItem.startTime * 1000).coerceAtLeast(100.0)
                        val wProg = (smoothPosition.toDouble() - sMs) / dur
                        val cInW = charInWordMap[i].toDouble()
                        val wLen = wordLenMap[i].toDouble()
                        ((wProg - cInW / wLen) * wLen).coerceIn(0.0, 1.0).toFloat()
                    } else 0f

                    val nudgeScale = if (wordItem != null && !isWordSung && sungFactor > 0f) {
                        0.038f * sin(charLp * PI.toFloat()) * exp(-3f * charLp)
                    } else 0f

                    val charScaleX = 1f + (wobble * 0.025f) + crescendoDeltaX + (nudgeScale * 0.3f)
                    val charBounds = layoutResult.getBoundingBox(charOffset)
                    lineTotalPushes[lineIdx] += charBounds.width * (charScaleX - 1f)
                }

                for (i in 0 until clusterCount) {
                    val charOffset = clusterCharOffsets[i]
                    val lineIdx = layoutResult.getLineForOffset(charOffset)
                    val charBounds = layoutResult.getBoundingBox(charOffset)
                    val wordIdx = wordIdxMap[i]
                    val originalWordIdx = if (wordIdx != -1) effectiveToOriginalIdx[wordIdx] else -1
                    
                    val alignShift = when(alignment) {
                        TextAlign.Center -> -lineTotalPushes[lineIdx] / 2f
                        TextAlign.Right -> -lineTotalPushes[lineIdx]
                        else -> 0f
                    }
                    
                    val (sungFactor, wordItem, isWordSung) = if (wordIdx != -1) wordFactors[wordIdx] else Triple(0f, null, false)
                    val wobble = if (originalWordIdx != -1) wordWobbles[originalWordIdx] else 0f
                    val wobbleX = wobble * 0.025f
                    val wobbleY = wobble * 0.015f
                    
                    val charLp = if (wordItem != null) {
                        val sMs = wordItem.startTime * 1000
                        val dur = (wordItem.endTime * 1000 - wordItem.startTime * 1000).coerceAtLeast(100.0)
                        val wProg = (smoothPosition.toDouble() - sMs) / dur
                        val cInW = charInWordMap[i].toDouble()
                        val wLen = wordLenMap[i].toDouble()
                        ((wProg - cInW / wLen) * wLen).coerceIn(0.0, 1.0).toFloat()
                    } else 0f

                    val shouldGlow = wordItem != null && !isWordSung && sungFactor > 0.001f

                    var crescendoDeltaX = 0f
                    var crescendoDeltaY = 0f
                    val groupWord = if (wordIdx != -1) hyphenGroupData[wordIdx] else null
                    if (groupWord != null) {
                        val p = sungFactor
                        val timeSinceEnd = (smoothPosition - groupWord.groupEndMs).toFloat()
                        val exitDuration = 600f
                        val pOut = (timeSinceEnd / exitDuration).coerceIn(0f, 1f)
                        val peakScale = 0.06f
                        val decay = 3.5f
                        val freq = 5.0f
                        val baseScalePerSegment = 0.012f
                        if (pOut > 0f) {
                            val baseAtEnd = groupWord.pos * baseScalePerSegment
                            val totalAtEnd = baseAtEnd + peakScale
                            val springOut = totalAtEnd * exp(-decay * pOut) * cos(freq * pOut * PI.toFloat()) * (1f - pOut)
                            crescendoDeltaX = springOut
                            crescendoDeltaY = springOut
                        } else if (groupWord.isLast) {
                            val base = groupWord.pos * baseScalePerSegment
                            val springPart = peakScale * (1f - exp(-decay * p) * cos(freq * p * PI.toFloat()) * (1f - p))
                            crescendoDeltaX = base + springPart
                            crescendoDeltaY = base + springPart
                        } else {
                            val boost = if (p > 0f) 0.02f * (1f - p) else 0f
                            val base = (groupWord.pos * baseScalePerSegment) + boost
                            crescendoDeltaX = base
                            crescendoDeltaY = base
                        }
                    }

                    val nudgeStrength = 0.038f
                    val nudgeScale = if (wordItem != null && !isWordSung && sungFactor > 0f) {
                        nudgeStrength * sin(charLp * PI.toFloat()) * exp(-3f * charLp)
                    } else 0f
                    
                    val charScaleX = 1f + wobbleX + crescendoDeltaX + nudgeScale * 0.3f
                    val charScaleY = 1f + wobbleY + crescendoDeltaY + nudgeScale

                    withTransform({
                        var waveOffset = 0f
                        if (groupWord != null) {
                            val wallTime = System.currentTimeMillis()
                            val adjSmoothPos = smoothPosition
                            val timeInGroup = (adjSmoothPos - groupWord.groupStartMs).toFloat()
                            val timeToGroupEnd = (groupWord.groupEndMs - adjSmoothPos).toFloat()
                            val waveFade = (timeInGroup / 200f).coerceIn(0f, 1f) * (timeToGroupEnd / 200f).coerceIn(0f, 1f)
                            if (waveFade > 0.01f) {
                                val waveSpeed = 0.006f
                                val waveHeight = 3.24f
                                val phaseOffset = i * 0.4f
                                waveOffset = sin(wallTime * waveSpeed + phaseOffset) * waveHeight * waveFade
                            }
                        }

                        translate(left = alignShift + lineCurrentPushes[lineIdx] + charBounds.left, top = charBounds.top + waveOffset)
                        if (wordIdx != -1) {
                            scale(
                                charScaleX,
                                charScaleY,
                                pivot = Offset(charBounds.width / 2f, charBounds.height)
                            )
                        }
                    }) {
                        if (shouldGlow) {
                            val sMs = wordItem!!.startTime * 1000
                            val eMs = wordItem.endTime * 1000
                            val dur = eMs - sMs
                            val wordLenText = wordItem.text.length.coerceAtLeast(1)
                            val impactRatio = dur.toFloat() / wordLenText
                            val fadeFactor = (sungFactor * 5f).coerceIn(0f, 1f) * ((1f - sungFactor) * 8f).coerceIn(0f, 1f)
                            val impactFactor = (((impactRatio - 100f) / 250f).coerceIn(0f, 1f) * 0.6f + ((dur.toFloat() - 300f) / 1500f).coerceIn(0f, 1f) * 0.4f).coerceIn(0f, 1f) * fadeFactor
                            if (impactFactor > 0.01f) {
                                val glowAlpha = (0.35f * impactFactor).coerceIn(0f, 0.4f)
                                val baseGlowRadius = 12.dp.toPx() * impactFactor                                                                                    
                                drawIntoCanvas { canvas ->
                                    glowPaint.maskFilter = BlurMaskFilter(baseGlowRadius, BlurMaskFilter.Blur.NORMAL)
                                    glowPaint.color = expressiveAccent.copy(alpha = glowAlpha).toArgb()
                                    glowPaint.textSize = lyricStyle.fontSize.toPx()
                                    glowPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                    canvas.nativeCanvas.drawText(letterLayouts[i].layoutInput.text.text, 0f, letterLayouts[i].firstBaseline, glowPaint)
                                }
                            }
                        }
                        val baseAlpha = if (isWordSung || charLp > 0.99f) 1f else (focusedAlpha + (1f - focusedAlpha) * sungFactor)
                        drawText(letterLayouts[i], color = expressiveAccent.copy(alpha = if (wordIdx == -1) focusedAlpha else baseAlpha))
                        if (!isWordSung && charLp > 0f && charLp < 1f) {
                            val fXL = charBounds.width * charLp
                            val eW = (charBounds.width * 0.45f).coerceAtLeast(1f)
                            val sWL = (fXL - eW).coerceAtLeast(0f)
                            if (sWL > 0f) {
                                clipRect(left = 0f, top = 0f, right = sWL, bottom = charBounds.height) { drawText(letterLayouts[i], color = expressiveAccent) }
                            }
                            for (j in 0 until 12) {
                                val start = sWL + (j * eW / 12f)
                                val end = (sWL + ((j + 1) * eW / 12f) + 0.5f).coerceAtMost(fXL)
                                if (end > start) {
                                    clipRect(left = start, top = 0f, right = end, bottom = charBounds.height) { drawText(letterLayouts[i], color = expressiveAccent.copy(alpha = 1f - (j + 0.5f) / 12f)) }
                                }
                            }
                        }
                    }
                    lineCurrentPushes[lineIdx] += charBounds.width * (charScaleX - 1f)
                }
            }
        }
    }
}
