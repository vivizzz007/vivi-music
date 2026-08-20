package com.music.vivi.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.lyrics.LyricsTranslationHelper
import com.music.vivi.utils.rememberPreference
import com.music.vivi.constants.*
import androidx.compose.foundation.shape.RoundedCornerShape
import com.music.vivi.R
import com.music.vivi.db.entities.LyricsEntity
import com.music.vivi.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.music.vivi.lyrics.LyricsEntry
import com.music.vivi.lyrics.LyricsUtils
import com.music.vivi.models.MediaMetadata
import com.music.vivi.ui.component.MetroLyricsLine
import com.music.vivi.ui.screens.settings.LyricsPosition
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class LyricsListItemV2 {
    data class Line(val index: Int, val entry: LyricsEntry) : LyricsListItemV2()
    data class Indicator(val index: Int, val gapMs: Long, val gapStartMs: Long, val gapEndMs: Long) : LyricsListItemV2()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LyricsV2(
    mediaMetadata: MediaMetadata?,
    showLyrics: Boolean,
    positionProvider: () -> Long,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    if (!showLyrics) return

    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)

    val openRouterApiKey by rememberPreference(OpenRouterApiKey, "")
    val deeplApiKey by rememberPreference(DeeplApiKey, "")
    val aiProvider by rememberPreference(AiProviderKey, "OpenRouter")
    val openRouterBaseUrl by rememberPreference(OpenRouterBaseUrlKey, "https://openrouter.ai/api/v1/chat/completions")
    val openRouterModel by rememberPreference(OpenRouterModelKey, "google/gemini-2.5-flash-lite")
    val translateLanguage by rememberPreference(TranslateLanguageKey, "en")
    val translateMode by rememberPreference(TranslateModeKey, "Literal")
    val deeplFormality by rememberPreference(DeeplFormalityKey, "default")

    val translationStatus by LyricsTranslationHelper.status.collectAsState()
    val hasActiveTranslations by LyricsTranslationHelper.hasActiveTranslations.collectAsState()

    val playerBackground by com.music.vivi.utils.rememberEnumPreference(
        key = com.music.vivi.constants.PlayerBackgroundStyleKey,
        defaultValue = com.music.vivi.constants.PlayerBackgroundStyle.DEFAULT
    )

    val adaptivePrimary =
        if (playerBackground == com.music.vivi.constants.PlayerBackgroundStyle.DEFAULT) MaterialTheme.colorScheme.onSurface else Color.White
    val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        LyricsTranslationHelper.setCompositionActive(true)
        onDispose {
            LyricsTranslationHelper.setCompositionActive(false)
            LyricsTranslationHelper.cancelTranslation()
        }
    }

    LaunchedEffect(mediaMetadata?.id, currentLyrics) {
        if (mediaMetadata != null && currentLyrics == null) {
            delay(500)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.music.vivi.di.LyricsHelperEntryPoint::class.java
                    )
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val fetchedLyricsWithProvider = lyricsHelper.getLyrics(mediaMetadata)
                    database.query {
                        upsert(
                            LyricsEntity(
                                mediaMetadata.id,
                                fetchedLyricsWithProvider.lyrics,
                                fetchedLyricsWithProvider.provider
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Handle error silently
                }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            lyrics == null -> {
                CircularWavyProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            lyrics == LYRICS_NOT_FOUND -> {
                Text(
                    text = "Lyrics not found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = adaptivePrimary.copy(alpha = 0.5f)
                )
            }

            else -> {
                val lines = remember(lyrics) {
                    if (lyrics.startsWith("[")) {
                        LyricsUtils.parseLyrics(lyrics).map { entry ->
                            LyricsEntry(
                                entry.time,
                                entry.text,
                                entry.words,
                                agent = entry.agent,
                                isBackground = entry.isBackground
                            )
                        }
                    } else {
                        // Plain text fallback
                        lyrics.split("\n").map { line ->
                            LyricsEntry(0L, line, emptyList())
                        }
                    }
                }
                
                LaunchedEffect(lines, currentLyrics, translateLanguage, translateMode) {
                    if (lines.isNotEmpty() && currentLyrics != null) {
                        LyricsTranslationHelper.loadTranslationsFromDatabase(
                            lyrics = lines,
                            lyricsEntity = currentLyrics,
                            targetLanguage = translateLanguage,
                            mode = translateMode
                        )
                    }
                }
                
                LaunchedEffect(showLyrics, lines.size) {
                    LyricsTranslationHelper.manualTrigger.collect {
                        val effectiveApiKey = if (aiProvider == "DeepL") deeplApiKey else openRouterApiKey
                        if (showLyrics && lines.isNotEmpty() && effectiveApiKey.isNotBlank()) {
                            LyricsTranslationHelper.translateLyrics(
                                lyrics = lines,
                                targetLanguage = translateLanguage,
                                apiKey = openRouterApiKey,
                                baseUrl = openRouterBaseUrl,
                                model = openRouterModel,
                                mode = translateMode,
                                scope = coroutineScope,
                                context = context,
                                provider = aiProvider,
                                deeplApiKey = deeplApiKey,
                                deeplFormality = deeplFormality,
                                useStreaming = true,
                                songId = mediaMetadata?.id ?: "",
                                database = database
                            )
                        } else if (effectiveApiKey.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.ai_api_key_required), Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    LyricsTranslationHelper.clearTranslationsTrigger.collect {
                        lines.forEach { it.translatedTextFlow.value = null }
                    }
                }

                // If plain text (not synced), just render a static list
                if (lines.isNotEmpty() && lines.all { it.time == 0L }) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 32.dp,
                            bottom = 120.dp,
                            start = 24.dp,
                            end = 24.dp
                        )
                    ) {
                        items(lines.size) { index ->
                            Text(
                                text = lines[index].text,
                                style = MaterialTheme.typography.headlineSmall,
                                color = adaptivePrimary,
                                modifier = Modifier.padding(vertical = 12.dp),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                    return
                }

                // Subscribed synced lyrics
                val listState = rememberLazyListState()
                val isDragged by listState.interactionSource.collectIsDraggedAsState()
                var isAutoScrollEnabled by remember { mutableStateOf(true) }
                var lastManualScrollTime by remember { mutableLongStateOf(0L) }

                LaunchedEffect(isDragged) {
                    if (isDragged) {
                        isAutoScrollEnabled = false
                        lastManualScrollTime = System.currentTimeMillis()
                    }
                }

                // Resume auto-scroll after 3 seconds of inactivity
                LaunchedEffect(isAutoScrollEnabled, lastManualScrollTime) {
                    if (!isAutoScrollEnabled) {
                        delay(3000L)
                        isAutoScrollEnabled = true
                    }
                }

                var currentPosition by remember { mutableLongStateOf(positionProvider()) }
                LaunchedEffect(Unit) {
                    while (true) {
                        currentPosition = positionProvider()
                        delay(50)
                    }
                }

                val currentLineIndex = remember(currentPosition, lines) {
                    LyricsUtils.findCurrentLineIndex(lines, currentPosition)
                }

                val mergedLyricsList = remember(lines) {
                    val result = mutableListOf<LyricsListItemV2>()
                    if (lines.isEmpty()) return@remember result

                    lines.forEachIndexed { i, entry ->
                        if (entry.text.isNotBlank()) {
                            result.add(LyricsListItemV2.Line(i, entry))
                        }
                        if (i < lines.size - 1) {
                            val nextStart = lines[i + 1].time
                            val currentEnd = if (!entry.words.isNullOrEmpty()) {
                                (entry.words.last().endTime * 1000).toLong()
                            } else if (entry.text.isBlank()) {
                                entry.time
                            } else {
                                null
                            }

                            if (currentEnd != null && currentEnd < nextStart) {
                                val gap = nextStart - currentEnd
                                if (gap > 4000L) {
                                    result.add(
                                        LyricsListItemV2.Indicator(
                                            i,
                                            gap,
                                            currentEnd,
                                            nextStart
                                        )
                                    )
                                }
                            }
                        }
                    }
                    result
                }

                BoxWithConstraints(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                ) {
                    val targetTopRatio = 0.32f
                    val topPadding = (maxHeight * targetTopRatio) + contentPadding.calculateTopPadding()
                    val bottomPadding = (maxHeight * (1f - targetTopRatio)) + contentPadding.calculateBottomPadding()

                    // Smooth Spring Scroll Physics (Anchored comfortably at ~32% Top Viewport Position)
                    LaunchedEffect(currentLineIndex, isAutoScrollEnabled) {
                        if (currentLineIndex != -1 && isAutoScrollEnabled) {
                            val targetIndex = maxOf(0, currentLineIndex)
                            try {
                                val itemInfo =
                                    listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
                                if (itemInfo != null) {
                                    val viewportHeight =
                                        listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
                                    val targetTopOffset = viewportHeight * targetTopRatio
                                    val center =
                                        listState.layoutInfo.viewportStartOffset + targetTopOffset
                                    val itemCenter = itemInfo.offset + itemInfo.size / 2
                                    val offset = itemCenter - center

                                    if (kotlin.math.abs(offset) > 5) {
                                        listState.animateScrollBy(
                                            value = offset.toFloat(),
                                            animationSpec = androidx.compose.animation.core.spring(
                                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                            )
                                        )
                                    }
                                } else {
                                    val distance =
                                        kotlin.math.abs(targetIndex - listState.firstVisibleItemIndex)
                                    if (distance > 15) {
                                        listState.scrollToItem(targetIndex)
                                    } else {
                                        listState.animateScrollToItem(targetIndex, 0)
                                    }
                                }
                            } catch (e: Exception) {
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                compositingStrategy =
                                    androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        0.0f to Color.Transparent,
                                        0.15f to Color.Black,
                                        0.8f to Color.Black,
                                        1.0f to Color.Transparent
                                    ),
                                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                                )
                            },
                        // Top-anchored content padding
                        contentPadding = PaddingValues(
                            top = topPadding,
                            bottom = bottomPadding
                        )
                    ) {
                        items(mergedLyricsList.size) { listIndex ->
                            val item = mergedLyricsList[listIndex]
                            when (item) {
                                is LyricsListItemV2.Line -> {
                                    val index = item.index
                                    val line = item.entry
                                    val isActive = index == currentLineIndex
                                    val isPassed = index < currentLineIndex
                                    val distanceFromActive =
                                        kotlin.math.abs(index - currentLineIndex)

                                    // Pop-In Spring Animation for Newly Active Line
                                    val popInScale =
                                        remember { androidx.compose.animation.core.Animatable(1f) }
                                    LaunchedEffect(isActive) {
                                        if (isActive) {
                                            popInScale.snapTo(0.96f)
                                            popInScale.animateTo(
                                                targetValue = 1f,
                                                animationSpec = androidx.compose.animation.core.spring(
                                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                                )
                                            )
                                        }
                                    }

                                    // Distance-based alpha calculation
                                    val targetLineAlpha = when {
                                        isActive -> 1.0f
                                        !isAutoScrollEnabled -> {
                                            when (distanceFromActive) {
                                                1 -> 0.72f
                                                2 -> 0.56f
                                                3 -> 0.40f
                                                else -> 0.28f
                                            }
                                        }

                                        distanceFromActive == 1 -> 0.52f
                                        distanceFromActive == 2 -> 0.30f
                                        distanceFromActive == 3 -> 0.18f
                                        else -> 0.10f
                                    }

                                    val animatedLineScale by androidx.compose.animation.core.animateFloatAsState(
                                        targetValue = if (isActive) 1.0f else 0.95f,
                                        animationSpec = tween(
                                            durationMillis = 166,
                                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                                        ),
                                        label = "v2LineScale"
                                    )

                                    val animatedLineAlpha by androidx.compose.animation.core.animateFloatAsState(
                                        targetValue = targetLineAlpha,
                                        animationSpec = tween(
                                            durationMillis = if (isActive) 330 else 500,
                                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                                        ),
                                        label = "v2LineAlpha"
                                    )

                                    val textColor by animateColorAsState(
                                        targetValue = when {
                                            isActive -> adaptivePrimary
                                            isPassed -> adaptivePrimary.copy(alpha = 0.6f)
                                            else -> adaptivePrimary.copy(alpha = 0.4f)
                                        },
                                        animationSpec = tween(400),
                                        label = "lyricsColorSync"
                                    )

                                    val nextStart =
                                        (mergedLyricsList.getOrNull(listIndex + 1) as? LyricsListItemV2.Line)?.entry?.time

                                    val finalScale = animatedLineScale * popInScale.value

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer {
                                                scaleX = finalScale
                                                scaleY = finalScale
                                                alpha = animatedLineAlpha
                                            }
                                    ) {
                                        MetroLyricsLine(
                                            entry = line,
                                            nextEntryTime = nextStart,
                                            effectivePlaybackPosition = currentPosition,
                                            lyricsOffset = 0L,
                                            isSynced = true,
                                            isActive = isActive,
                                            distanceFromCurrent = distanceFromActive,
                                            lyricsTextPosition = LyricsPosition.LEFT,
                                            textColor = textColor,
                                            showRomanized = false,
                                            showTranslated = hasActiveTranslations,
                                            onClick = {
                                                playerConnection.player.seekTo(line.time)
                                            },
                                            onLongClick = {},
                                            isSelected = false,
                                            isSelectionModeActive = false,
                                            isAutoScrollActive = isAutoScrollEnabled,
                                            expressiveAccent = adaptivePrimary,
                                            bgVisible = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        )
                                    }
                                }

                                is LyricsListItemV2.Indicator -> {
                                    val indicatorVisible =
                                        currentPosition >= item.gapStartMs && currentPosition <= item.gapEndMs - 650L
                                    IntervalIndicator(
                                        gapStartMs = item.gapStartMs,
                                        gapEndMs = item.gapEndMs - 650L,
                                        currentPositionMs = currentPosition,
                                        visible = indicatorVisible,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 32.dp)
                                            .wrapContentWidth(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .zIndex(1f)
                            .padding(top = 56.dp)
                    ) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                when (val status = translationStatus) {
                                    is LyricsTranslationHelper.TranslationStatus.Translating -> {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            ),
                                            shape = RoundedCornerShape(16.dp),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                androidx.compose.material3.CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    text = stringResource(R.string.ai_translating_lyrics),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                    is LyricsTranslationHelper.TranslationStatus.Error -> {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer
                                            ),
                                            shape = RoundedCornerShape(16.dp),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.error),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = status.message,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                    is LyricsTranslationHelper.TranslationStatus.Success -> {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                            ),
                                            shape = RoundedCornerShape(16.dp),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.check),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = stringResource(R.string.ai_lyrics_translated),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                        }
                                    }
                                    is LyricsTranslationHelper.TranslationStatus.Idle -> {
                                        // No status display
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                    ) {
                        AnimatedVisibility(
                            visible = !isAutoScrollEnabled,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut()
                        ) {
                            FilledTonalButton(onClick = {
                                isAutoScrollEnabled = true
                                if (currentLineIndex != -1) {
                                    coroutineScope.launch {
                                        try {
                                            listState.animateScrollToItem(
                                                maxOf(
                                                    0,
                                                    currentLineIndex - 2
                                                )
                                            )
                                        } catch (e: Exception) {
                                        }
                                    }
                                }
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.sync),
                                    contentDescription = stringResource(R.string.auto_scroll),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(R.string.auto_scroll))
                            }
                        }
                    }
                }
            }
        }
    }
}

