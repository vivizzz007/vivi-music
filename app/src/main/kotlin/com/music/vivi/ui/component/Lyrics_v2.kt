/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.runtime.*
import com.music.vivi.audio.AudioPulse
import com.music.vivi.audio.LYRIC_LINE_STRENGTH
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.LocalListenTogetherManager
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.text.Layout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.music.vivi.utils.ComposeToImage
import com.music.vivi.ui.component.LyricsImageCard
import com.music.vivi.ui.component.LyricsBackgroundStyle

sealed class LyricsListItemV2 {
    data class Line(val index: Int, val entry: LyricsEntry) : LyricsListItemV2()
    data class Indicator(val index: Int, val gapMs: Long, val gapStartMs: Long, val gapEndMs: Long) : LyricsListItemV2()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LyricsV2(
    mediaMetadata: MediaMetadata?,
    showLyrics: Boolean,
    positionProvider: () -> Long,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onControlsVisibilityChange: (Boolean) -> Unit = {}
) {
    if (!showLyrics) return

    val playerConnection = LocalPlayerConnection.current ?: return

    var isSelectionModeActive by remember { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var showShareSheet by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val toggleSelection: (Int) -> Unit = { index ->
        if (selectedIndices.contains(index)) {
            selectedIndices.remove(index)
            if (selectedIndices.isEmpty()) {
                isSelectionModeActive = false
                onControlsVisibilityChange(true)
            }
        } else {
            selectedIndices.add(index)
        }
    }

    val startSelection: (Int) -> Unit = { index ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!isSelectionModeActive) {
            isSelectionModeActive = true
            selectedIndices.clear()
            selectedIndices.add(index)
            onControlsVisibilityChange(false)
        } else {
            toggleSelection(index)
        }
    }

    val exitSelection: () -> Unit = {
        isSelectionModeActive = false
        selectedIndices.clear()
        onControlsVisibilityChange(true)
    }

    BackHandler(enabled = isSelectionModeActive) {
        exitSelection()
    }

    LaunchedEffect(mediaMetadata?.id) {
        if (isSelectionModeActive) {
            exitSelection()
        }
    }
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost

    val scrollLyrics by rememberPreference(LyricsScrollKey, true)
    
    val romanizeJapaneseLyrics by rememberPreference(LyricsRomanizeJapaneseKey, true)
    val romanizeKoreanLyrics by rememberPreference(LyricsRomanizeKoreanKey, true)
    val romanizeRussianLyrics by rememberPreference(LyricsRomanizeRussianKey, true)
    val romanizeUkrainianLyrics by rememberPreference(LyricsRomanizeUkrainianKey, true)
    val romanizeSerbianLyrics by rememberPreference(LyricsRomanizeSerbianKey, true)
    val romanizeBulgarianLyrics by rememberPreference(LyricsRomanizeBulgarianKey, true)
    val romanizeBelarusianLyrics by rememberPreference(LyricsRomanizeBelarusianKey, true)
    val romanizeKyrgyzLyrics by rememberPreference(LyricsRomanizeKyrgyzKey, true)
    val romanizeMacedonianLyrics by rememberPreference(LyricsRomanizeMacedonianKey, true)
    val romanizeCyrillicByLine by rememberPreference(LyricsRomanizeCyrillicByLineKey, false)
    val romanizeAsMain by rememberPreference(LyricsRomanizeAsMainKey, false)
    val romanizeChineseLyrics by rememberPreference(LyricsRomanizeChineseKey, true)
    val romanizeHindiLyrics by rememberPreference(LyricsRomanizeHindiKey, true)
    val romanizePunjabiLyrics by rememberPreference(LyricsRomanizePunjabiKey, true)

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
        defaultValue = com.music.vivi.constants.PlayerBackgroundStyle.GRADIENT
    )

    val adaptivePrimary =
        if (playerBackground == com.music.vivi.constants.PlayerBackgroundStyle.DEFAULT) MaterialTheme.colorScheme.onSurface else Color.White
    val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val lyricsOffset = (currentSong?.song?.lyricsOffset ?: 0).toLong()
    val showRomanized = currentSong?.song?.romanizeLyrics ?: true

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
                ContainedLoadingIndicator()
            }

            lyrics == LYRICS_NOT_FOUND -> {
                Text(
                    text = "Lyrics not found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = adaptivePrimary.copy(alpha = 0.5f)
                )
            }

            else -> {
                val lines = remember(lyrics, coroutineScope) {
                    if (lyrics.startsWith("[")) {
                        val parsedLines = LyricsUtils.parseLyrics(lyrics)

                        val isRussianLyrics = romanizeRussianLyrics && !romanizeCyrillicByLine && LyricsUtils.isRussian(lyrics)
                        val isUkrainianLyrics = romanizeUkrainianLyrics && !romanizeCyrillicByLine && LyricsUtils.isUkrainian(lyrics)
                        val isSerbianLyrics = romanizeSerbianLyrics && !romanizeCyrillicByLine && LyricsUtils.isSerbian(lyrics)
                        val isBulgarianLyrics = romanizeBulgarianLyrics && !romanizeCyrillicByLine && LyricsUtils.isBulgarian(lyrics)
                        val isBelarusianLyrics = romanizeBelarusianLyrics && !romanizeCyrillicByLine && LyricsUtils.isBelarusian(lyrics)
                        val isKyrgyzLyrics = romanizeKyrgyzLyrics && !romanizeCyrillicByLine && LyricsUtils.isKyrgyz(lyrics)
                        val isMacedonianLyrics = romanizeMacedonianLyrics && !romanizeCyrillicByLine && LyricsUtils.isMacedonian(lyrics)

                        parsedLines.map { entry ->
                            val newEntry = LyricsEntry(entry.time, entry.text, entry.words, agent = entry.agent, isBackground = entry.isBackground)

                            if (romanizeJapaneseLyrics && LyricsUtils.isJapanese(entry.text) && !LyricsUtils.isChinese(entry.text)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeJapanese(entry.text) }
                            }
                            if (romanizeKoreanLyrics && LyricsUtils.isKorean(entry.text)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeKorean(entry.text) }
                            }
                            if (romanizeRussianLyrics && (if (romanizeCyrillicByLine) LyricsUtils.isRussian(entry.text) else isRussianLyrics)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeCyrillic(entry.text) }
                            }
                            else if (romanizeUkrainianLyrics && (if (romanizeCyrillicByLine) LyricsUtils.isUkrainian(entry.text) else isUkrainianLyrics)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeCyrillic(entry.text) }
                            }
                            else if (romanizeSerbianLyrics && (if (romanizeCyrillicByLine) LyricsUtils.isSerbian(entry.text) else isSerbianLyrics)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeCyrillic(entry.text) }
                            }
                            else if (romanizeBulgarianLyrics && (if (romanizeCyrillicByLine) LyricsUtils.isBulgarian(entry.text) else isBulgarianLyrics)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeCyrillic(entry.text) }
                            }
                            else if (romanizeBelarusianLyrics && (if (romanizeCyrillicByLine) LyricsUtils.isBelarusian(entry.text) else isBelarusianLyrics)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeCyrillic(entry.text) }
                            }
                            else if (romanizeKyrgyzLyrics && (if (romanizeCyrillicByLine) LyricsUtils.isKyrgyz(entry.text) else isKyrgyzLyrics)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeCyrillic(entry.text) }
                            }
                            else if (romanizeMacedonianLyrics && (if (romanizeCyrillicByLine) LyricsUtils.isMacedonian(entry.text) else isMacedonianLyrics)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeCyrillic(entry.text) }
                            }
                            else if (romanizeChineseLyrics && LyricsUtils.isChinese(entry.text)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeChinese(entry.text) }
                            }
                            else if (romanizeHindiLyrics && LyricsUtils.isHindi(entry.text)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeHindi(entry.text) }
                            }
                            else if (romanizePunjabiLyrics && LyricsUtils.isPunjabi(entry.text)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizePunjabi(entry.text) }
                            }
                            newEntry
                        }
                    } else {
                        val isRussianLyrics = romanizeRussianLyrics && !romanizeCyrillicByLine && LyricsUtils.isRussian(lyrics)
                        val isUkrainianLyrics = romanizeUkrainianLyrics && !romanizeCyrillicByLine && LyricsUtils.isUkrainian(lyrics)
                        val isSerbianLyrics = romanizeSerbianLyrics && !romanizeCyrillicByLine && LyricsUtils.isSerbian(lyrics)
                        val isBulgarianLyrics = romanizeBulgarianLyrics && !romanizeCyrillicByLine && LyricsUtils.isBulgarian(lyrics)
                        val isBelarusianLyrics = romanizeBelarusianLyrics && !romanizeCyrillicByLine && LyricsUtils.isBelarusian(lyrics)
                        val isKyrgyzLyrics = romanizeKyrgyzLyrics && !romanizeCyrillicByLine && LyricsUtils.isKyrgyz(lyrics)
                        val isMacedonianLyrics = romanizeMacedonianLyrics && !romanizeCyrillicByLine && LyricsUtils.isMacedonian(lyrics)

                        lyrics.split("\n").mapIndexed { index, line ->
                            val newEntry = LyricsEntry(index * 100L, line, emptyList())

                            if (romanizeJapaneseLyrics && LyricsUtils.isJapanese(line) && !LyricsUtils.isChinese(line)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeJapanese(line) }
                            }
                            if (romanizeKoreanLyrics && LyricsUtils.isKorean(line)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeKorean(line) }
                            }
                            if (romanizeRussianLyrics && (if (romanizeCyrillicByLine) LyricsUtils.isRussian(line) else isRussianLyrics)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeCyrillic(line) }
                            }
                            else if (romanizeUkrainianLyrics && (if (romanizeCyrillicByLine) LyricsUtils.isUkrainian(line) else isUkrainianLyrics)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeCyrillic(line) }
                            }
                            else if (romanizeSerbianLyrics && (if (romanizeCyrillicByLine) LyricsUtils.isSerbian(line) else isSerbianLyrics)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeCyrillic(line) }
                            }
                            else if (romanizeBulgarianLyrics && (if (romanizeCyrillicByLine) LyricsUtils.isBulgarian(line) else isBulgarianLyrics)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeCyrillic(line) }
                            }
                            else if (romanizeBelarusianLyrics && (if (romanizeCyrillicByLine) LyricsUtils.isBelarusian(line) else isBelarusianLyrics)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeCyrillic(line) }
                            }
                            else if (romanizeKyrgyzLyrics && (if (romanizeCyrillicByLine) LyricsUtils.isKyrgyz(line) else isKyrgyzLyrics)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeCyrillic(line) }
                            }
                            else if (romanizeMacedonianLyrics && (if (romanizeCyrillicByLine) LyricsUtils.isMacedonian(line) else isMacedonianLyrics)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeCyrillic(line) }
                            }
                            else if (romanizeChineseLyrics && LyricsUtils.isChinese(line)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeChinese(line) }
                            }
                            else if (romanizeHindiLyrics && LyricsUtils.isHindi(line)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizeHindi(line) }
                            }
                            else if (romanizePunjabiLyrics && LyricsUtils.isPunjabi(line)) {
                                coroutineScope.launch { newEntry.romanizedTextFlow.value = LyricsUtils.romanizePunjabi(line) }
                            }
                            newEntry
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

                val isLyricsProviderShown = currentLyrics?.provider != null && currentLyrics?.provider != "Unknown"

                // If plain text (not synced), render selectable list
                if (lines.isNotEmpty() && lines.all { it.time == 0L }) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = 32.dp,
                                bottom = if (isSelectionModeActive) 140.dp else 120.dp,
                                start = 24.dp,
                                end = 24.dp
                            )
                        ) {
                            if (isLyricsProviderShown) {
                                item {
                                    Text(
                                        text = "Lyrics from ${currentLyrics?.provider}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = adaptivePrimary.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                    )
                                }
                            }
                            items(lines.size) { index ->
                                val isSelected = selectedIndices.contains(index)
                                Text(
                                    text = lines[index].text,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary else adaptivePrimary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .combinedClickable(
                                            onClick = {
                                                if (isSelectionModeActive) {
                                                    toggleSelection(index)
                                                }
                                            },
                                            onLongClick = {
                                                startSelection(index)
                                            }
                                        )
                                        .background(
                                            if (isSelected && isSelectionModeActive)
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }

                        LyricsSelectionFloatingBox(
                            visible = isSelectionModeActive,
                            selectedCount = selectedIndices.size,
                            onCancel = exitSelection,
                            onShare = {
                                if (selectedIndices.isNotEmpty()) {
                                    showShareSheet = true
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp)
                                .zIndex(10f)
                        )
                    }

                    if (showShareSheet && mediaMetadata != null) {
                        val selectedLyricsText = selectedIndices.sorted()
                            .mapNotNull { lines.getOrNull(it)?.text }
                            .joinToString("\n")

                        LyricsShareBottomSheet(
                            mediaMetadata = mediaMetadata,
                            lyrics = selectedLyricsText,
                            onDismissRequest = {
                                showShareSheet = false
                            }
                        )
                    }
                    return
                }

                // lazy list state moved below to allow initializing with playback line index

                var currentPosition by remember { mutableLongStateOf(positionProvider()) }
                
                LaunchedEffect(Unit) {
                    var lastPlayerPos = playerConnection.player.currentPosition
                    var lastUpdateTime = System.currentTimeMillis()
                    
                    while (true) {
                       withFrameMillis {
                            val now = System.currentTimeMillis()
                            val playerPos = playerConnection.player.currentPosition
                            val isPlaying = playerConnection.player.isPlaying
                            val providerPos = positionProvider()
                            
                            if (kotlin.math.abs(providerPos - playerPos) > 1000L) {
                                currentPosition = providerPos
                                lastPlayerPos = playerPos
                                lastUpdateTime = now
                            } else {
                                if (playerPos != lastPlayerPos || !isPlaying) {
                                    lastPlayerPos = playerPos
                                    lastUpdateTime = now
                                }
                                val elapsed = now - lastUpdateTime
                                currentPosition = lastPlayerPos + (if (isPlaying) elapsed else 0L)
                            }
                        }
                    }
                }

                // Scroll anchor: single line index used to keep the list auto-scrolling
                val currentLineIndex by remember(lines, lyricsOffset) {
                    derivedStateOf { LyricsUtils.findCurrentLineIndex(lines, currentPosition + lyricsOffset) }
                }

                LaunchedEffect(currentLineIndex) {
                    if (currentLineIndex != -1) AudioPulse.triggerLyricLine(LYRIC_LINE_STRENGTH)
                }

                // Active set: all lines currently being sung (handles v1 + bg sharing same timestamp)
                val activeLineIndices by remember(lines, lyricsOffset) {
                    derivedStateOf { LyricsUtils.findActiveLineIndices(lines, currentPosition + lyricsOffset) }
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
                val rowLineIndices = remember(mergedLyricsList) {
                    mergedLyricsList.map { (it as? LyricsListItemV2.Line)?.index }
                }

                val providerOffset = if (isLyricsProviderShown) 1 else 0

                val initialIndex = remember(currentLineIndex, lines) {
                    maxOf(0, currentLineIndex - 1) + providerOffset
                }

                // Subscribed synced lyrics
                val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
                val isDragged by listState.interactionSource.collectIsDraggedAsState()
                var isAutoScrollEnabled by remember { mutableStateOf(scrollLyrics) }
                var lastManualScrollTime by remember { mutableLongStateOf(0L) }

                LaunchedEffect(scrollLyrics) { 
                    if (!isDragged && !isGuest) {
                        isAutoScrollEnabled = scrollLyrics 
                    }
                }

                LaunchedEffect(isDragged) {
                    if (isDragged && !isGuest) {
                        isAutoScrollEnabled = false
                        lastManualScrollTime = System.currentTimeMillis()
                    }
                }

                LaunchedEffect(isAutoScrollEnabled, lastManualScrollTime) {
                    if (!isAutoScrollEnabled) {
                        delay(3000L)
                        isAutoScrollEnabled = true
                    }
                }

                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val screenHeight = configuration.screenHeightDp.dp
                val estimatedLyricsAreaHeight = screenHeight * 0.6f

                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                ) {
                    val targetTopRatio = 0.30f
                    val topPadding = (estimatedLyricsAreaHeight * targetTopRatio) + contentPadding.calculateTopPadding()
                    val bottomPadding = (estimatedLyricsAreaHeight * (1f - targetTopRatio)) + contentPadding.calculateBottomPadding()

                    // Smooth Spring Scroll Physics (Anchored comfortably at ~32% Top Viewport Position)
                    LaunchedEffect(currentLineIndex, isAutoScrollEnabled, mergedLyricsList) {
                        if (currentLineIndex != -1 && isAutoScrollEnabled && !isGuest) {
                            val actualListIndex = listRowFor(rowLineIndices, currentLineIndex)
                            val targetIndex = maxOf(0, actualListIndex) + providerOffset
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
                                        0.12f to Color.Black,
                                        0.82f to Color.Black,
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
                        if (isLyricsProviderShown) {
                            item {
                                Text(
                                    text = "Lyrics from ${currentLyrics?.provider}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = adaptivePrimary.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 12.dp)
                                )
                            }
                        }
                        items(mergedLyricsList.size) { listIndex ->
                            val item = mergedLyricsList[listIndex]
                            when (item) {
                                is LyricsListItemV2.Line -> {
                                    val index = item.index
                                    val line = item.entry
                                    val isActive = index in activeLineIndices
                                    val distanceFromActive =
                                        kotlin.math.abs(index - currentLineIndex)

                                    val nextStart =
                                        (mergedLyricsList.getOrNull(listIndex + 1) as? LyricsListItemV2.Line)?.entry?.time

                                    MetroLyricsLine(
                                        entry = line,
                                        nextEntryTime = nextStart,
                                        // Only lines near the active one read the per-frame position, so far lines stay still while scrolling.
                                        effectivePlaybackPosition = if (followsPlaybackPosition(isActive, distanceFromActive)) currentPosition else 0L,
                                        lyricsOffset = lyricsOffset,
                                        isSynced = true,
                                        isActive = isActive,
                                        distanceFromCurrent = distanceFromActive,
                                        lyricsTextPosition = LyricsPosition.LEFT,
                                        textColor = adaptivePrimary,
                                        showRomanized = showRomanized,
                                        showTranslated = hasActiveTranslations,
                                        onClick = {
                                            if (isSelectionModeActive) {
                                                toggleSelection(index)
                                            } else if (!isGuest) {
                                                playerConnection.player.seekTo(line.time - lyricsOffset)
                                            }
                                        },
                                        onLongClick = {
                                            startSelection(index)
                                        },
                                        isSelected = selectedIndices.contains(index),
                                        isSelectionModeActive = isSelectionModeActive,
                                        isAutoScrollActive = (isAutoScrollEnabled || isGuest) && !isSelectionModeActive,
                                        expressiveAccent = adaptivePrimary,
                                        bgVisible = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    )
                                }

                                is LyricsListItemV2.Indicator -> {
                                    val effectivePos = currentPosition + lyricsOffset
                                    val indicatorVisible =
                                        effectivePos >= item.gapStartMs && effectivePos <= item.gapEndMs - 650L
                                    IntervalIndicator(
                                        gapStartMs = item.gapStartMs,
                                        gapEndMs = item.gapEndMs - 650L,
                                        currentPositionMs = effectivePos,
                                        visible = indicatorVisible,
                                        color = adaptivePrimary,
                                        modifier = Modifier
                                            .fillMaxWidth()
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
                            visible = !isAutoScrollEnabled && !isSelectionModeActive,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut()
                        ) {
                            FilledTonalButton(onClick = {
                                isAutoScrollEnabled = true
                                if (currentLineIndex != -1) {
                                    coroutineScope.launch {
                                        try {
                                            val rowIndex = listRowFor(rowLineIndices, currentLineIndex)
                                            listState.animateScrollToItem(syncScrollIndex(rowIndex, providerOffset))
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

                    LyricsSelectionFloatingBox(
                        visible = isSelectionModeActive,
                        selectedCount = selectedIndices.size,
                        onCancel = exitSelection,
                        onShare = {
                            if (selectedIndices.isNotEmpty()) {
                                showShareSheet = true
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .zIndex(10f)
                    )
                }
            }
        }
    }

    if (showShareSheet && mediaMetadata != null) {
        val rawLyrics = currentLyrics?.lyrics
        val lines = remember(rawLyrics) {
            rawLyrics?.let { LyricsUtils.parseLyrics(it) } ?: emptyList()
        }
        val selectedLyricsText = selectedIndices.sorted()
            .mapNotNull { lines.getOrNull(it)?.text }
            .joinToString("\n")

        LyricsShareBottomSheet(
            mediaMetadata = mediaMetadata,
            lyrics = selectedLyricsText,
            onDismissRequest = {
                showShareSheet = false
                exitSelection()
            }
        )
    }
}

@Composable
private fun LyricsSelectionFloatingBox(
    visible: Boolean,
    selectedCount: Int,
    onCancel: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(350, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(350)),
        exit = slideOutVertically(tween(250, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(250)),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.wrapContentSize()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = "Cancel",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                )

                Button(
                    onClick = onShare,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.share),
                        contentDescription = "Share",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share ($selectedCount)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsShareBottomSheet(
    mediaMetadata: MediaMetadata,
    lyrics: String,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isGeneratingImage by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 6.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.share_lyrics),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Blur Card Preview (Dynamic Height: expands as more lyrics lines are selected)
            LyricsImageCard(
                lyricText = lyrics,
                mediaMetadata = mediaMetadata,
                backgroundStyle = LyricsBackgroundStyle.BLUR,
                backgroundColor = Color.Transparent,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Save button
                FilledTonalButton(
                    onClick = {
                        if (isGeneratingImage) return@FilledTonalButton
                        isGeneratingImage = true
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val image = ComposeToImage.createLyricsImage(
                                    context = context,
                                    mediaMetadata = mediaMetadata,
                                    lyrics = lyrics,
                                    backgroundStyle = LyricsBackgroundStyle.BLUR,
                                    backgroundColor = android.graphics.Color.TRANSPARENT,
                                    textColor = android.graphics.Color.WHITE,
                                    secondaryTextColor = android.graphics.Color.WHITE,
                                    lyricsAlignment = Layout.Alignment.ALIGN_NORMAL
                                )
                                val timestamp = System.currentTimeMillis()
                                val filename = "lyrics_$timestamp"
                                ComposeToImage.saveBitmapAsFile(context, image, filename)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Image saved to Pictures/vivimusic",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.failed_to_create_image, e.message),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } finally {
                                isGeneratingImage = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = !isGeneratingImage
                ) {
                    if (isGeneratingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.download),
                            contentDescription = stringResource(R.string.save),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.save),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Share button
                Button(
                    onClick = {
                        if (isGeneratingImage) return@Button
                        isGeneratingImage = true
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val image = ComposeToImage.createLyricsImage(
                                    context = context,
                                    mediaMetadata = mediaMetadata,
                                    lyrics = lyrics,
                                    backgroundStyle = LyricsBackgroundStyle.BLUR,
                                    backgroundColor = android.graphics.Color.TRANSPARENT,
                                    textColor = android.graphics.Color.WHITE,
                                    secondaryTextColor = android.graphics.Color.WHITE,
                                    lyricsAlignment = Layout.Alignment.ALIGN_NORMAL
                                )
                                val timestamp = System.currentTimeMillis()
                                val filename = "lyrics_$timestamp"
                                val uri = ComposeToImage.saveBitmapAsFile(context, image, filename)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        context.getString(R.string.share_lyrics)
                                    )
                                )
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.failed_to_create_image, e.message),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } finally {
                                isGeneratingImage = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = !isGeneratingImage
                ) {
                    if (isGeneratingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.share),
                            contentDescription = stringResource(R.string.share),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.share),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
