/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.ui.component

import android.annotation.SuppressLint
import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
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
import kotlinx.coroutines.isActive
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

import androidx.compose.ui.layout.layout
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.music.vivi.ui.utils.fadingEdge
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

private const val LYRICS_ANCHOR_RATIO = 0.35f
private val LYRICS_ITEM_FALLBACK_HEIGHT_DP = 68.dp
private val LYRICS_ITEM_GAP_DP = 16.dp
private val LYRICS_FADE_TOP_DP = 130.dp
private val LYRICS_FADE_BOTTOM_DP = 160.dp
private const val LYRICS_STAGGER_DELAY_PER_DISTANCE = 20
private const val LYRICS_STAGGER_DELAY_MAX_MS = 200
private val INSTRUMENTAL_REGEX = Regex("^[.\\s…~♪\\-—•*·]+$")

private fun String.isInstrumentalPlaceholder(): Boolean {
    val trimmed = this.trim()
    return trimmed.isEmpty() || INSTRUMENTAL_REGEX.matches(trimmed)
}

sealed class LyricsListItemV2 {
    data class Line(val index: Int, val entry: LyricsEntry) : LyricsListItemV2()
    data class Indicator(
        val afterLineIndex: Int,
        val gapMs: Long,
        val gapStartMs: Long,
        val gapEndMs: Long
    ) : LyricsListItemV2()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LyricsV2(
    mediaMetadata: MediaMetadata?,
    showLyrics: Boolean,
    positionProvider: () -> Long,
    sliderPositionProvider: () -> Long? = { null },
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

    // Keep screen on while lyrics are visible
    DisposableEffect(showLyrics) {
        val activity = context as? Activity
        if (showLyrics) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
                            val cleanText = if (entry.text.isInstrumentalPlaceholder()) "" else entry.text
                            val newEntry = LyricsEntry(entry.time, cleanText, entry.words, agent = entry.agent, isBackground = entry.isBackground)

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
                        }.let {
                            listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + it
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
                            val cleanText = if (line.isInstrumentalPlaceholder()) "" else line
                            val newEntry = LyricsEntry(index * 100L, cleanText, emptyList())

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
                                val lineText = lines[index].text
                                if (lineText.isBlank() || lineText.isInstrumentalPlaceholder()) return@items
                                val isSelected = selectedIndices.contains(index)
                                Text(
                                    text = lineText,
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

                var currentPlaybackPosition by remember { mutableLongStateOf(0L) }
                var currentLineIndex by remember { mutableIntStateOf(-1) }
                var activeLineIndices by remember { mutableStateOf(emptySet<Int>()) }
                var deferredCurrentLineIndex by rememberSaveable { mutableIntStateOf(0) }
                var scrollTargetIndex by rememberSaveable { mutableIntStateOf(-1) }
                var previousScrollActiveIndices by remember { mutableStateOf(emptySet<Int>()) }
                var lastMainMaxSeen by remember(lyrics, lines) { mutableIntStateOf(-1) }
                var isSeeking by remember { mutableStateOf(false) }
                var isAutoScrollEnabled by rememberSaveable { mutableStateOf(scrollLyrics) }
                var lastPreviewTime by rememberSaveable { mutableLongStateOf(0L) }

                val mergedLyricsList = remember(lines) {
                    val result = mutableListOf<LyricsListItemV2>()
                    if (lines.isEmpty()) return@remember result

                    lines.forEachIndexed { i, entry ->
                        val isPlaceholder = entry.text.isBlank() || entry.text.isInstrumentalPlaceholder()
                        if (!isPlaceholder) {
                            result.add(LyricsListItemV2.Line(i, entry))
                        }
                        if (i < lines.size - 1) {
                            val nextStart = lines[i + 1].time
                            val currentEnd = if (!entry.words.isNullOrEmpty()) {
                                (entry.words.last().endTime * 1000).toLong()
                            } else if (isPlaceholder) {
                                entry.time
                            } else {
                                val estimatedDuration = (entry.text.length * 200L).coerceIn(2000L, 4500L)
                                if (nextStart - entry.time > estimatedDuration + 4000L) {
                                    entry.time + estimatedDuration
                                } else {
                                    null
                                }
                            }

                            if (currentEnd != null && currentEnd < nextStart) {
                                val gap = nextStart - currentEnd
                                if (gap > 4000L) {
                                    result.add(
                                        LyricsListItemV2.Indicator(
                                            afterLineIndex = i,
                                            gapMs = gap,
                                            gapStartMs = currentEnd,
                                            gapEndMs = nextStart
                                        )
                                    )
                                }
                            }
                        }
                    }
                    result
                }

                val activeListIndex by remember(mergedLyricsList, deferredCurrentLineIndex, currentPlaybackPosition) {
                    derivedStateOf {
                        val activeIndicatorIndex = mergedLyricsList.indexOfFirst {
                            it is LyricsListItemV2.Indicator &&
                                currentPlaybackPosition >= it.gapStartMs &&
                                currentPlaybackPosition <= it.gapEndMs - 650L
                        }
                        if (activeIndicatorIndex != -1) {
                            activeIndicatorIndex
                        } else {
                            mergedLyricsList.indexOfFirst {
                                (it is LyricsListItemV2.Line && it.index == deferredCurrentLineIndex) ||
                                (it is LyricsListItemV2.Indicator && it.afterLineIndex == deferredCurrentLineIndex)
                            }.coerceAtLeast(0)
                        }
                    }
                }

                var userManualOffset by remember { mutableFloatStateOf(0f) }
                var flingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
                val velocityTracker = remember { VelocityTracker() }
                val decayAnimSpec = remember { exponentialDecay<Float>(frictionMultiplier = 1.8f) }
                val itemHeights = remember(lyrics, mergedLyricsList) { mutableStateMapOf<Int, Int>() }
                var isInitialLayout by remember(lyrics, mergedLyricsList) { mutableStateOf(true) }

                LaunchedEffect(showLyrics, lyrics, mergedLyricsList.size) {
                    if (showLyrics && mergedLyricsList.isNotEmpty()) {
                        isInitialLayout = true
                        snapshotFlow {
                            val h = itemHeights.toMap()
                            val windowStart = (activeListIndex - 8).coerceAtLeast(0)
                            val windowEnd = (activeListIndex + 12).coerceAtMost(mergedLyricsList.size - 1)
                            (windowStart..windowEnd).all { h.containsKey(it) }
                        }.first { it }
                        delay(150)
                        isInitialLayout = false
                    }
                }

                LaunchedEffect(activeListIndex) {
                    if (mergedLyricsList.isNotEmpty()) {
                        val h = itemHeights.toMap()
                        val windowStart = (activeListIndex - 3).coerceAtLeast(0)
                        val windowEnd = (activeListIndex + 3).coerceAtMost(mergedLyricsList.size - 1)
                        val needsMeasurement = (windowStart..windowEnd).any { !h.containsKey(it) }
                        if (needsMeasurement) {
                            isInitialLayout = true
                            snapshotFlow {
                                val hh = itemHeights.toMap()
                                (windowStart..windowEnd).all { hh.containsKey(it) }
                            }.first { it }
                            isInitialLayout = false
                        }
                    }
                }

                LaunchedEffect(lyrics, lines) {
                    isAutoScrollEnabled = scrollLyrics
                    scrollTargetIndex = -1
                    deferredCurrentLineIndex = 0
                    previousScrollActiveIndices = emptySet()
                    lastMainMaxSeen = -1
                    userManualOffset = 0f
                }

                LaunchedEffect(lyrics, lines) {
                    if (lyrics.isNullOrEmpty() || !lyrics.startsWith("[")) {
                        currentLineIndex = -1
                        activeLineIndices = emptySet()
                        return@LaunchedEffect
                    }
                    var lastPlayerPos = playerConnection.player.currentPosition
                    var lastUpdateTime = System.currentTimeMillis()
                    var lastPlaybackPosition = 0L
                    val hasWordTimings = lines.any { !it.words.isNullOrEmpty() }

                    while (isActive) {
                        withFrameNanos { _ -> }
                        val now = System.currentTimeMillis()
                        val sliderPos = sliderPositionProvider()
                        isSeeking = sliderPos != null

                        val position = if (isSeeking) {
                            sliderPos!!
                        } else {
                            val providerPos = positionProvider()
                            val playerPos = playerConnection.player.currentPosition
                            val isPlaying = playerConnection.player.isPlaying
                            if (kotlin.math.abs(providerPos - playerPos) > 1000L) {
                                lastPlayerPos = playerPos
                                lastUpdateTime = now
                                providerPos
                            } else {
                                if (playerPos != lastPlayerPos || !isPlaying) {
                                    lastPlayerPos = playerPos
                                    lastUpdateTime = now
                                }
                                val elapsed = now - lastUpdateTime
                                lastPlayerPos + (if (isPlaying) elapsed else 0L)
                            }
                        }

                        currentPlaybackPosition = position
                        val effectivePosition = position + lyricsOffset

                        val initialActiveIndices = LyricsUtils.findActiveLineIndices(lines, effectivePosition)
                        val scrollActiveIndicesRaw = LyricsUtils.findActiveLineIndices(lines, effectivePosition + (if (hasWordTimings) 0L else 250L))

                        val scrollActiveIndices = scrollActiveIndicesRaw.toMutableSet()
                        for (i in scrollActiveIndicesRaw) {
                            if (lines.getOrNull(i)?.isBackground == true) {
                                for (j in i - 1 downTo 0) {
                                    if (lines.getOrNull(j)?.isBackground == false) {
                                        scrollActiveIndices.add(j)
                                        break
                                    }
                                }
                            }
                        }

                        val newActiveIndices = initialActiveIndices.toMutableSet()
                        for (i in initialActiveIndices) {
                            if (lines.getOrNull(i)?.isBackground == true) {
                                for (j in i - 1 downTo 0) {
                                    if (lines.getOrNull(j)?.isBackground == false) {
                                        newActiveIndices.add(j)
                                        break
                                    }
                                }
                            }
                        }

                        val scrollMax = scrollActiveIndices
                            .filter { !it.let { lines[it].isBackground } }
                            .maxOrNull() ?: (scrollActiveIndices.maxOrNull() ?: -1)

                        // Detect seek backwards or slider scrub
                        val isBackwardSeek = position < lastPlaybackPosition - 1500L
                        if (isSeeking || isBackwardSeek) {
                            lastMainMaxSeen = scrollMax
                            if (scrollMax != -1) {
                                scrollTargetIndex = scrollMax
                                deferredCurrentLineIndex = scrollMax
                            }
                        }
                        lastPlaybackPosition = position

                        val isCurrentTargetStillActive = scrollTargetIndex in scrollActiveIndices
                        val anyStillActive = scrollActiveIndices.isNotEmpty()

                        val shouldScroll = when {
                            isSeeking -> true
                            !isCurrentTargetStillActive && anyStillActive && scrollMax > scrollTargetIndex -> true
                            !isCurrentTargetStillActive && !anyStillActive && previousScrollActiveIndices.isNotEmpty() -> true
                            scrollTargetIndex == -1 && anyStillActive -> true
                            previousScrollActiveIndices.isEmpty() && anyStillActive && scrollMax > scrollTargetIndex -> true
                            else -> false
                        }

                        if (shouldScroll) {
                            val targetToScroll = when {
                                isSeeking -> scrollMax
                                !isCurrentTargetStillActive && anyStillActive -> scrollMax
                                !isCurrentTargetStillActive && !anyStillActive -> {
                                    (lastMainMaxSeen + 1 until lines.size).firstOrNull {
                                        !it.let { lines[it].isBackground }
                                    } ?: scrollTargetIndex
                                }
                                else -> scrollMax
                            }
                            if (targetToScroll != -1 && (isSeeking || targetToScroll > scrollTargetIndex || isBackwardSeek)) {
                                scrollTargetIndex = targetToScroll
                                if (isSeeking) {
                                    deferredCurrentLineIndex = targetToScroll
                                }
                            }
                        }

                        if (scrollMax > lastMainMaxSeen && scrollMax != -1) {
                            lastMainMaxSeen = scrollMax
                        }

                        previousScrollActiveIndices = scrollActiveIndices
                        activeLineIndices = newActiveIndices
                        currentLineIndex = scrollMax
                    }
                }

                LaunchedEffect(scrollTargetIndex, isAutoScrollEnabled) {
                    if (scrollTargetIndex != -1 && isAutoScrollEnabled) {
                        deferredCurrentLineIndex = scrollTargetIndex
                    }
                }

                LaunchedEffect(isSeeking, lastPreviewTime) {
                    if (isSeeking) {
                        lastPreviewTime = 0L
                        isAutoScrollEnabled = true
                    } else if (lastPreviewTime != 0L) {
                        delay(4000L)
                        lastPreviewTime = 0L
                        isAutoScrollEnabled = true
                    }
                }

                val density = LocalDensity.current

                BoxWithConstraints(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize().padding(bottom = 12.dp)
                ) {
                    val maxHeightPx = constraints.maxHeight.toFloat()
                    val anchorY = maxHeightPx * LYRICS_ANCHOR_RATIO
                    val lineHeightPx = with(density) { LYRICS_ITEM_FALLBACK_HEIGHT_DP.toPx() }
                    val constraintLineHeightPx = with(density) { 120.dp.toPx() }

                    val positions = remember(itemHeights.toMap(), activeListIndex, mergedLyricsList) {
                        val map = mutableMapOf<Int, Float>()
                        if (activeListIndex == -1 || mergedLyricsList.isEmpty()) return@remember map

                        map[activeListIndex] = 0f
                        var currentY = 0f
                        val indicatorHeightPx = with(density) { 72.dp.toPx() }
                        for (i in activeListIndex - 1 downTo 0) {
                            val item = mergedLyricsList[i]
                            val height = itemHeights[i]?.toFloat() ?: (if (item is LyricsListItemV2.Indicator) indicatorHeightPx else lineHeightPx)
                            val noGap = (item as? LyricsListItemV2.Line)?.entry?.isBackground == true || item is LyricsListItemV2.Indicator
                            currentY -= (height + if (noGap) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() })
                            map[i] = currentY
                        }
                        currentY = 0f
                        for (i in activeListIndex until mergedLyricsList.size - 1) {
                            val currentItem = mergedLyricsList[i]
                            val nextItem = mergedLyricsList[i + 1]
                            val height = itemHeights[i]?.toFloat() ?: (if (currentItem is LyricsListItemV2.Indicator) indicatorHeightPx else lineHeightPx)
                            val nextNoGap = (nextItem as? LyricsListItemV2.Line)?.entry?.isBackground == true || nextItem is LyricsListItemV2.Indicator
                            currentY += (height + if (nextNoGap) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() })
                            map[i + 1] = currentY
                        }
                        map
                    }

                    val minOffset = remember(itemHeights.toMap(), mergedLyricsList, activeListIndex, anchorY) {
                        if (mergedLyricsList.isEmpty() || activeListIndex == -1) return@remember 0f
                        val indicatorHeightPx = with(density) { 72.dp.toPx() }
                        val totalBelow = (activeListIndex until mergedLyricsList.size - 1).sumOf { i ->
                            val currentItem = mergedLyricsList[i]
                            val nextItem = mergedLyricsList[i + 1]
                            val height = itemHeights[i]?.toFloat() ?: (if (currentItem is LyricsListItemV2.Indicator) indicatorHeightPx else constraintLineHeightPx)
                            val nextNoGap = (nextItem as? LyricsListItemV2.Line)?.entry?.isBackground == true || nextItem is LyricsListItemV2.Indicator
                            (height + if (nextNoGap) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() }).toDouble()
                        }.toFloat()
                        val lastItem = mergedLyricsList.last()
                        val lastHeight = itemHeights[mergedLyricsList.size - 1]?.toFloat() ?: (if (lastItem is LyricsListItemV2.Indicator) indicatorHeightPx else constraintLineHeightPx)
                        with(density) { 100.dp.toPx() } - anchorY - totalBelow - lastHeight
                    }

                    val maxOffset = remember(itemHeights.toMap(), mergedLyricsList, activeListIndex, maxHeightPx, anchorY) {
                        if (mergedLyricsList.isEmpty() || activeListIndex == -1) return@remember 0f
                        val indicatorHeightPx = with(density) { 72.dp.toPx() }
                        val totalAbove = (0 until activeListIndex).sumOf { i ->
                            val item = mergedLyricsList[i]
                            val height = itemHeights[i]?.toFloat() ?: (if (item is LyricsListItemV2.Indicator) indicatorHeightPx else constraintLineHeightPx)
                            val noGap = (item as? LyricsListItemV2.Line)?.entry?.isBackground == true || item is LyricsListItemV2.Indicator
                            (height + if (noGap) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() }).toDouble()
                        }.toFloat()
                        maxHeightPx - with(density) { 150.dp.toPx() } - anchorY + totalAbove
                    }

                    val scrollClampMin = minOf(minOffset, maxOffset)
                    val scrollClampMax = maxOf(minOffset, maxOffset)

                    LaunchedEffect(scrollClampMin, scrollClampMax) {
                        if (userManualOffset < scrollClampMin || userManualOffset > scrollClampMax) {
                            userManualOffset = userManualOffset.coerceIn(scrollClampMin, scrollClampMax)
                        }
                    }

                    LaunchedEffect(isAutoScrollEnabled, mergedLyricsList.size) {
                        if (isAutoScrollEnabled) {
                            val start = userManualOffset
                            if (kotlin.math.abs(start) < 1f) {
                                userManualOffset = 0f
                                return@LaunchedEffect
                            }
                            val anim = androidx.compose.animation.core.Animatable(start)
                            var lastValue = start
                            anim.animateTo(0f, tween((kotlin.math.abs(start) / 4f).toInt().coerceIn(200, 600), easing = FastOutSlowInEasing)) {
                                userManualOffset += (value - lastValue)
                                lastValue = value
                            }
                            userManualOffset = 0f
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .fadingEdge(top = LYRICS_FADE_TOP_DP, bottom = LYRICS_FADE_BOTTOM_DP)
                            .clipToBounds()
                            .pointerInput(Unit) {
                                coroutineScope {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        flingJob?.cancel()
                                        velocityTracker.resetTracking()
                                        velocityTracker.addPosition(down.uptimeMillis, down.position)

                                        var dragConsumed = false
                                        verticalDrag(down.id) { change ->
                                            val dragAmount = change.positionChange().y
                                            velocityTracker.addPosition(change.uptimeMillis, change.position)

                                            val newOffset = userManualOffset + dragAmount
                                            if (newOffset in scrollClampMin..scrollClampMax) {
                                                userManualOffset = newOffset
                                                dragConsumed = true
                                                change.consume()
                                            } else {
                                                val overscroll = if (newOffset < scrollClampMin) newOffset - scrollClampMin else newOffset - scrollClampMax
                                                val friction = 0.35f / (1f + kotlin.math.abs(overscroll) / 300f)
                                                userManualOffset = (userManualOffset + dragAmount * friction).coerceIn(
                                                    scrollClampMin - 150f,
                                                    scrollClampMax + 150f
                                                )
                                                change.consume()
                                            }
                                            isAutoScrollEnabled = false
                                            lastPreviewTime = System.currentTimeMillis()
                                        }

                                        if (isAutoScrollEnabled) return@awaitEachGesture

                                        val velocity = velocityTracker.calculateVelocity().y
                                        if (kotlin.math.abs(velocity) > 100f) {
                                            flingJob = launch {
                                                val animState = AnimationState(
                                                    initialValue = userManualOffset,
                                                    initialVelocity = velocity
                                                )
                                                animState.animateDecay(decayAnimSpec) {
                                                    if (value in scrollClampMin..scrollClampMax) {
                                                        userManualOffset = value
                                                    } else {
                                                        val target = value.coerceIn(scrollClampMin, scrollClampMax)
                                                        cancelAnimation()
                                                        launch {
                                                            androidx.compose.animation.core.Animatable(userManualOffset).animateTo(
                                                                targetValue = target,
                                                                animationSpec = androidx.compose.animation.core.spring(
                                                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                                                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                                                )
                                                            ) {
                                                                userManualOffset = value
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            if (userManualOffset < scrollClampMin || userManualOffset > scrollClampMax) {
                                                val target = userManualOffset.coerceIn(scrollClampMin, scrollClampMax)
                                                flingJob = launch {
                                                    androidx.compose.animation.core.Animatable(userManualOffset).animateTo(
                                                        targetValue = target,
                                                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                                                    ) {
                                                        userManualOffset = value
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        if (isLyricsProviderShown) {
                            val targetProviderBase = anchorY + (positions[0] ?: 0f) - with(density) { 32.dp.toPx() }
                            val animatedProviderBase by animateFloatAsState(
                                targetValue = targetProviderBase,
                                animationSpec = if (isInitialLayout || !isAutoScrollEnabled) snap()
                                else {
                                    tween(750, 0, FastOutSlowInEasing)
                                },
                                label = "lyricsProviderOffset"
                            )
                            Text(
                                text = "Lyrics from ${currentLyrics?.provider}",
                                fontSize = 12.sp,
                                color = adaptivePrimary.copy(alpha = if (isInitialLayout) 0f else 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset { IntOffset(0, (animatedProviderBase + userManualOffset).roundToInt()) }
                                    .padding(horizontal = 24.dp, vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        mergedLyricsList.forEachIndexed { listIndex, listItem ->
                            key(listItem) {
                                val distance = kotlin.math.abs(listIndex - activeListIndex)
                                val targetOffset = anchorY + (positions[listIndex] ?: ((listIndex - activeListIndex) * lineHeightPx))

                                val frozenOffset = remember { androidx.compose.runtime.mutableFloatStateOf(targetOffset) }
                                LaunchedEffect(isAutoScrollEnabled, targetOffset, isInitialLayout) {
                                    if (isAutoScrollEnabled || isInitialLayout) {
                                        frozenOffset.floatValue = targetOffset
                                    }
                                }

                                val animatedOffset by animateFloatAsState(
                                    targetValue = if (isAutoScrollEnabled) targetOffset else frozenOffset.floatValue,
                                    animationSpec = if (isInitialLayout || !isAutoScrollEnabled) snap()
                                    else {
                                        tween(750, (distance * LYRICS_STAGGER_DELAY_PER_DISTANCE).coerceAtMost(LYRICS_STAGGER_DELAY_MAX_MS), FastOutSlowInEasing)
                                    },
                                    label = "lyricStaggeredOffset_$listIndex"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            alpha = if (isInitialLayout) 0f else 1f
                                        }
                                        .layout { measurable, constraints ->
                                            val placeable = measurable.measure(constraints.copy(maxHeight = Constraints.Infinity))
                                            itemHeights[listIndex] = placeable.height
                                            layout(placeable.width, 0) {
                                                placeable.place(0, 0)
                                            }
                                        }
                                        .offset { IntOffset(0, (animatedOffset + userManualOffset).roundToInt()) }
                                ) {
                                    when (listItem) {
                                        is LyricsListItemV2.Line -> {
                                            val index = listItem.index
                                            val line = listItem.entry
                                            val isSelected = selectedIndices.contains(index)
                                            val effectivePlaybackPosition = currentPlaybackPosition + lyricsOffset

                                            val pairedMainLineIndex = if (line.isBackground) {
                                                (index - 1 downTo 0).firstOrNull { lines.getOrNull(it)?.isBackground == false } ?: -1
                                            } else -1
                                            val isInGapWithMain = if (line.isBackground && pairedMainLineIndex != -1) {
                                                val pairedMainLine = lines[pairedMainLineIndex]
                                                effectivePlaybackPosition >= pairedMainLine.time && effectivePlaybackPosition <= line.time
                                            } else false
                                            val bgVisible = !line.isBackground || (
                                                activeLineIndices.contains(pairedMainLineIndex) ||
                                                activeLineIndices.contains(index) ||
                                                isInGapWithMain
                                            )

                                            MetroLyricsLine(
                                                entry = line,
                                                nextEntryTime = lines.getOrNull(index + 1)?.time,
                                                effectivePlaybackPosition = effectivePlaybackPosition,
                                                lyricsOffset = lyricsOffset,
                                                isSynced = true,
                                                isActive = activeLineIndices.contains(index),
                                                distanceFromCurrent = kotlin.math.abs(index - deferredCurrentLineIndex),
                                                lyricsTextPosition = LyricsPosition.LEFT,
                                                textColor = adaptivePrimary,
                                                showRomanized = showRomanized,
                                                showTranslated = hasActiveTranslations,
                                                isAutoScrollActive = isAutoScrollEnabled,
                                                isSelectionModeActive = isSelectionModeActive,
                                                isSelected = isSelected,
                                                expressiveAccent = adaptivePrimary,
                                                bgVisible = bgVisible,
                                                onClick = {
                                                    if (isSelectionModeActive) {
                                                        toggleSelection(index)
                                                    } else if (!isGuest) {
                                                        playerConnection.seekTo((line.time - lyricsOffset).coerceAtLeast(0))
                                                        scrollTargetIndex = index
                                                        isAutoScrollEnabled = true
                                                        lastPreviewTime = 0L
                                                    }
                                                },
                                                onLongClick = {
                                                    startSelection(index)
                                                }
                                            )
                                        }

                                        is LyricsListItemV2.Indicator -> {
                                            val indicatorVisible = isAutoScrollEnabled &&
                                                currentPlaybackPosition >= listItem.gapStartMs &&
                                                currentPlaybackPosition <= listItem.gapEndMs - 650L
                                            IntervalIndicator(
                                                gapStartMs = listItem.gapStartMs,
                                                gapEndMs = listItem.gapEndMs - 650L,
                                                currentPositionMs = currentPlaybackPosition,
                                                visible = indicatorVisible,
                                                color = adaptivePrimary,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 24.dp)
                                                    .wrapContentWidth(Alignment.CenterHorizontally)
                                            )
                                        }
                                    }
                                }
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
                                if (currentLineIndex != -1) {
                                    scrollTargetIndex = currentLineIndex
                                    deferredCurrentLineIndex = currentLineIndex
                                    lastMainMaxSeen = currentLineIndex
                                }
                                isAutoScrollEnabled = true
                                lastPreviewTime = 0L
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
