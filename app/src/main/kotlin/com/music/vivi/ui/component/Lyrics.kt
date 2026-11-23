/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.text.Layout
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.music.vivi.ui.player.lyricsanimation.rememberLyricClock
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.State
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import com.music.vivi.ui.player.lyricsanimation.CharGrowth
import com.music.vivi.ui.player.lyricsanimation.GLOW_RADIUS
import com.music.vivi.ui.player.lyricsanimation.GROW_HEADROOM
import com.music.vivi.ui.player.lyricsanimation.GrowingWord
import com.music.vivi.ui.player.lyricsanimation.DUET_LANE
import com.music.vivi.ui.player.lyricsanimation.LyricAlignment
import com.music.vivi.ui.player.lyricsanimation.computeLineAlignments
import com.music.vivi.ui.player.lyricsanimation.PHASE_AFTER
import com.music.vivi.ui.player.lyricsanimation.PHASE_BEFORE
import com.music.vivi.ui.player.lyricsanimation.PHASE_MOVING
import com.music.vivi.ui.player.lyricsanimation.InstrumentalCookieIndicator
import com.music.vivi.ui.player.lyricsanimation.UNSUNG_ALPHA
import com.music.vivi.ui.player.lyricsanimation.ViviLyricLine
import com.music.vivi.ui.player.lyricsanimation.WORD_RISE
import com.music.vivi.ui.player.lyricsanimation.WIPE_FEATHER
import com.music.vivi.ui.player.lyricsanimation.toViviLyricLine
import com.music.vivi.ui.player.lyricsanimation.withInstrumentalGaps
import com.music.vivi.ui.player.lyricsanimation.isInstrumental
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalListenTogetherManager
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.LyricsAnimationStyle
import com.music.vivi.constants.LyricsAnimationStyleKey
import com.music.vivi.constants.LyricsClickKey
import com.music.vivi.constants.LyricsRomanizeAllKey
import com.music.vivi.constants.LyricsRomanizeAsMainKey
import com.music.vivi.constants.LyricsRomanizeBelarusianKey
import com.music.vivi.constants.LyricsRomanizeBulgarianKey
import com.music.vivi.constants.LyricsRomanizeChineseKey
import com.music.vivi.constants.LyricsRomanizeCyrillicByLineKey
import com.music.vivi.constants.LyricsRomanizeHindiKey
import com.music.vivi.constants.LyricsRomanizePunjabiKey
import com.music.vivi.constants.LyricsRomanizeMalayalamKey
import com.music.vivi.constants.LyricsRomanizeJapaneseKey
import com.music.vivi.constants.LyricsRomanizeKoreanKey
import com.music.vivi.constants.LyricsRomanizeKyrgyzKey
import com.music.vivi.constants.LyricsRomanizeMacedonianKey
import com.music.vivi.constants.LyricsRomanizeRussianKey
import com.music.vivi.constants.LyricsRomanizeSerbianKey
import com.music.vivi.constants.LyricsRomanizeUkrainianKey
import com.music.vivi.constants.LyricsScrollKey
import com.music.vivi.constants.LyricsTextPositionKey
import com.music.vivi.constants.UseNewPlayerDesignKey
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.OpenRouterApiKey
import com.music.vivi.constants.DeeplApiKey
import com.music.vivi.constants.AiProviderKey
import com.music.vivi.constants.OpenRouterBaseUrlKey
import com.music.vivi.constants.OpenRouterModelKey
import com.music.vivi.constants.TranslateLanguageKey
import com.music.vivi.constants.TranslateModeKey
import com.music.vivi.constants.DeeplFormalityKey
import com.music.vivi.constants.PlayerBackgroundStyleKey
import com.music.vivi.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.music.vivi.lyrics.LyricsEntry
import com.music.vivi.lyrics.LyricsUtils.findCurrentLineIndex
import com.music.vivi.lyrics.LyricsUtils.findActiveLineIndices
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
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.CompositingStrategy
import com.music.vivi.lyrics.LyricsUtils.isBelarusian
import com.music.vivi.lyrics.LyricsUtils.isBulgarian
import com.music.vivi.lyrics.LyricsUtils.isChinese
import com.music.vivi.lyrics.LyricsUtils.isHindi
import com.music.vivi.lyrics.LyricsUtils.isPunjabi
import com.music.vivi.lyrics.LyricsUtils.isJapanese
import com.music.vivi.lyrics.LyricsUtils.isKorean
import com.music.vivi.lyrics.LyricsUtils.isKyrgyz
import com.music.vivi.lyrics.LyricsUtils.isMacedonian
import com.music.vivi.lyrics.LyricsUtils.isRussian
import com.music.vivi.lyrics.LyricsUtils.isSerbian
import com.music.vivi.lyrics.LyricsUtils.isUkrainian
import com.music.vivi.lyrics.LyricsUtils.parseLyrics
import com.music.vivi.lyrics.LyricsRomanizationHelper
import com.music.vivi.lyrics.LyricsTranslationHelper
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.component.shimmer.TextPlaceholder
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.ui.screens.settings.LyricsPosition
import com.music.vivi.ui.utils.fadingEdge
import com.music.vivi.utils.ComposeToImage
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

private const val LYRICS_ANCHOR_RATIO = 0.35f
private val LYRICS_ITEM_FALLBACK_HEIGHT_DP = 68.dp
private val LYRICS_ITEM_GAP_DP = 16.dp
private val LYRICS_FADE_TOP_DP = 130.dp
private val LYRICS_FADE_BOTTOM_DP = 160.dp
private const val LYRICS_STAGGER_DELAY_PER_DISTANCE = 20
private const val LYRICS_STAGGER_DELAY_MAX_MS = 200

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope", "StringFormatInvalid")
@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    showLyrics: Boolean,
    extraBottomPadding: Dp = 0.dp
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalWindowInfo.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost

    val lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.LEFT)
    val changeLyrics by rememberPreference(LyricsClickKey, true)
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
    val romanizeAllLyrics by rememberPreference(LyricsRomanizeAllKey, false)
    val romanizeAsMain by rememberPreference(LyricsRomanizeAsMainKey, false)
    val romanizeChineseLyrics by rememberPreference(LyricsRomanizeChineseKey, true)
    val romanizeHindiLyrics by rememberPreference(LyricsRomanizeHindiKey, true)
    val romanizePunjabiLyrics by rememberPreference(LyricsRomanizePunjabiKey, true)
    val romanizeMalayalamLyrics by rememberPreference(LyricsRomanizeMalayalamKey, true)
    val lyricsAnimationStyle by rememberEnumPreference(LyricsAnimationStyleKey, LyricsAnimationStyle.VIVIMUSIC_1)
    val lyricsTextSize = 24f
    val lyricsLineSpacing = 1.3f
    
    val openRouterApiKey by rememberPreference(OpenRouterApiKey, "")
    val deeplApiKey by rememberPreference(DeeplApiKey, "")
    val aiProvider by rememberPreference(AiProviderKey, "OpenRouter")
    val openRouterBaseUrl by rememberPreference(OpenRouterBaseUrlKey, "https://openrouter.ai/api/v1/chat/completions")
    val openRouterModel by rememberPreference(OpenRouterModelKey, "google/gemini-2.5-flash-lite")
    val translateLanguage by rememberPreference(TranslateLanguageKey, "en")
    val translateMode by rememberPreference(TranslateModeKey, "Literal")
    val deeplFormality by rememberPreference(DeeplFormalityKey, "default")
    
    val scope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val lyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.LIVE_MESH
    )
    val (useNewPlayerDesign) = rememberPreference(UseNewPlayerDesignKey, defaultValue = false)

    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val lines = remember(lyrics) {
        if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
            emptyList()
        } else if (lyrics.startsWith("[")) {
            val parsedLines = parseLyrics(lyrics)
            parsedLines.map { entry ->
                LyricsEntry(entry.time, entry.text, entry.words, agent = entry.agent, isBackground = entry.isBackground)
            }.let {
                listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + it
            }
        } else {
            lyrics.lines().mapIndexed { index, line ->
                LyricsEntry(index * 100L, line)
            }
        }
    }

    LaunchedEffect(
        lines, currentSong?.id, currentSong?.romanizeLyrics, romanizeAllLyrics,
        romanizeJapaneseLyrics, romanizeKoreanLyrics, romanizeRussianLyrics,
        romanizeUkrainianLyrics, romanizeSerbianLyrics, romanizeBulgarianLyrics,
        romanizeBelarusianLyrics, romanizeKyrgyzLyrics, romanizeMacedonianLyrics,
        romanizeChineseLyrics, romanizeHindiLyrics, romanizePunjabiLyrics, romanizeMalayalamLyrics
    ) {
        if (lines.isNotEmpty() && currentSong?.romanizeLyrics != false) {
            val enabledLanguages: Set<String>? = if (romanizeAllLyrics) {
                null
            } else {
                buildSet<String> {
                    if (romanizeJapaneseLyrics) add("ja")
                    if (romanizeKoreanLyrics) add("ko")
                    if (romanizeChineseLyrics) add("zh-CN")
                    if (romanizeHindiLyrics) add("hi")
                    if (romanizePunjabiLyrics) add("pa")
                    if (romanizeMalayalamLyrics) add("ml")
                    if (romanizeRussianLyrics) add("ru")
                    if (romanizeUkrainianLyrics) add("uk")
                    if (romanizeSerbianLyrics) add("sr")
                    if (romanizeBulgarianLyrics) add("bg")
                    if (romanizeBelarusianLyrics) add("be")
                    if (romanizeKyrgyzLyrics) add("ky")
                    if (romanizeMacedonianLyrics) add("mk")
                }
            }
            LyricsRomanizationHelper.romanizeLyrics(
                lyrics = lines,
                songId = currentSong?.id ?: lyricsEntity?.id ?: "",
                context = context,
                enabledLanguages = enabledLanguages
            )
        }
    }
    val isSynced =
        remember(lyrics) {
            !lyrics.isNullOrEmpty() && lyrics.startsWith("[")
        }

    // State for translation status
    val translationStatus by LyricsTranslationHelper.status.collectAsState()
    val hasActiveTranslations by LyricsTranslationHelper.hasActiveTranslations.collectAsState()
    
    // Track composition lifecycle
    DisposableEffect(Unit) {
        LyricsTranslationHelper.setCompositionActive(true)
        onDispose {
            LyricsTranslationHelper.setCompositionActive(false)
            LyricsTranslationHelper.cancelTranslation()
        }
    }
    
    // Load translations from database on initial display
    LaunchedEffect(lines, lyricsEntity, translateLanguage, translateMode) {
        if (lines.isNotEmpty() && lyricsEntity != null) {
            LyricsTranslationHelper.loadTranslationsFromDatabase(
                lyrics = lines,
                lyricsEntity = lyricsEntity,
                targetLanguage = translateLanguage,
                mode = translateMode
            )
        }
    }
    
    // Listen for manual trigger
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
                    scope = scope,
                    context = context,
                    provider = aiProvider,
                    deeplApiKey = deeplApiKey,
                    deeplFormality = deeplFormality,
                    useStreaming = true,
                    songId = currentSong?.id ?: "",
                    database = database
                )
            } else if (effectiveApiKey.isBlank()) {
                Toast.makeText(context, context.getString(R.string.ai_api_key_required), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Listen for clear translations trigger from helper (authoritative source)
    LaunchedEffect(Unit) {
        LyricsTranslationHelper.clearTranslationsTrigger.collect {
            lines.forEach { it.translatedTextFlow.value = null }
        }
    }

    // Use Material 3 expressive accents and keep glow/text colors unified
    val expressiveAccent = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.primary
        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.GLOW_ANIMATED, PlayerBackgroundStyle.APPLE_MUSIC, PlayerBackgroundStyle.LIVE_MESH -> {
            // For animated or processed backgrounds, always use light colors regardless of theme
            Color.White
        }
    }
    val textColor = expressiveAccent
    val providerTextColor = if (playerBackground == PlayerBackgroundStyle.DEFAULT) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color.White
    }

    var currentLineIndex by remember {
        mutableIntStateOf(-1)
    }
    var currentPlaybackPosition by remember {
        mutableLongStateOf(0L)
    }
    // Because LaunchedEffect has delay, which leads to inconsistent with current line color and scroll animation,
    // we use deferredCurrentLineIndex when user is scrolling
    var deferredCurrentLineIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    var previousLineIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    var lastPreviewTime by rememberSaveable {
        mutableLongStateOf(0L)
    }
    var isSeeking by remember {
        mutableStateOf(false)
    }

    var initialScrollDone by rememberSaveable {
        mutableStateOf(false)
    }

    var shouldScrollToFirstLine by rememberSaveable {
        mutableStateOf(true)
    }

    var isAppMinimized by rememberSaveable {
        mutableStateOf(false)
    }

    var showProgressDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    var showColorPickerDialog by remember { mutableStateOf(false) }
    var previewBackgroundColor by remember { mutableStateOf(Color(0xFF242424)) }
    var previewTextColor by remember { mutableStateOf(Color.White) }
    var previewSecondaryTextColor by remember { mutableStateOf(Color.White.copy(alpha = 0.7f)) }

    // State for multi-selection
    var isSelectionModeActive by rememberSaveable { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var showMaxSelectionToast by remember { mutableStateOf(false) } // State for showing max selection toast

    val isLyricsProviderShown = lyricsEntity?.provider != null && lyricsEntity?.provider != "Unknown" && !isSelectionModeActive

    val lazyListState = rememberLazyListState()
    
    // Professional animation states for smooth vivimusic-style transitions
    var isAnimating by remember { mutableStateOf(false) }
    var isAutoScrollEnabled by rememberSaveable { mutableStateOf(true) }

    var activeLineIndices by remember { mutableStateOf(emptySet<Int>()) }
    var scrollTargetIndex by rememberSaveable { mutableIntStateOf(-1) }
    var previousScrollActiveIndices by remember { mutableStateOf(emptySet<Int>()) }
    var lastMainMaxSeen by remember(lyrics, lines) { mutableIntStateOf(-1) }

    val mergedLyricsList = remember(lines) {
        val result = mutableListOf<LyricsListItem>()
        if (lines.isEmpty()) return@remember result
        
        lines.forEachIndexed { i, entry ->
            if (entry.text.isNotBlank()) {
                result.add(LyricsListItem.Line(i, entry))
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
                        result.add(LyricsListItem.Indicator(i, gap, currentEnd, nextStart, lines[i + 1].agent))
                    }
                }
            }
        }
        result
    }

    val activeListIndex by remember(mergedLyricsList, deferredCurrentLineIndex) {
        derivedStateOf {
            mergedLyricsList.indexOfFirst { 
                (it is LyricsListItem.Line && it.index == deferredCurrentLineIndex) ||
                (it is LyricsListItem.Indicator && it.afterLineIndex == deferredCurrentLineIndex)
            }.coerceAtLeast(0)
        }
    }

    var userManualOffset by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
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

    // Handle back button press - close selection mode instead of exiting screen
    BackHandler(enabled = isSelectionModeActive) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    // Define max selection limit
    val maxSelectionLimit = 5

    // Show toast when max selection is reached
    LaunchedEffect(showMaxSelectionToast) {
        if (showMaxSelectionToast) {
            Toast.makeText(
                context,
                context.getString(R.string.max_selection_limit, maxSelectionLimit),
                Toast.LENGTH_SHORT
            ).show()
            showMaxSelectionToast = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Keep screen on while lyrics are visible, and re-enable auto-scroll when lyrics are shown
    DisposableEffect(showLyrics) {
        val activity = context as? Activity
        if (showLyrics) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            isAutoScrollEnabled = true
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val visibleItemsInfo = lazyListState.layoutInfo.visibleItemsInfo
                val isCurrentLineVisible = visibleItemsInfo.any { it.index == currentLineIndex }
                if (isCurrentLineVisible) {
                    initialScrollDone = false
                }
                isAppMinimized = true
            } else if(event == Lifecycle.Event.ON_START) {
                isAppMinimized = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Reset selection mode if lyrics change
    LaunchedEffect(lyrics, lines) {
        isAutoScrollEnabled = true
        scrollTargetIndex = -1
        deferredCurrentLineIndex = 0
        isSelectionModeActive = false
        selectedIndices.clear()
        previousScrollActiveIndices = emptySet()
        lastMainMaxSeen = -1
        lazyListState.scrollToItem(0)
    }

    LaunchedEffect(lyrics, lines, lyricsAnimationStyle) {
        if (lyrics.isNullOrEmpty() || !lyrics.startsWith("[")) {
            currentLineIndex = -1
            activeLineIndices = emptySet()
            return@LaunchedEffect
        }
        if (lyricsAnimationStyle == LyricsAnimationStyle.METRO_LYRICS) {
            var lastPlayerPos = playerConnection.player.currentPosition
            var lastUpdateTime = System.currentTimeMillis()
            val hasWordTimings = lines.any { !it.words.isNullOrEmpty() }
            
            while (isActive) {
                withFrameNanos { _ -> }
                val now = System.currentTimeMillis()
                val sliderPosition = sliderPositionProvider()
                isSeeking = sliderPosition != null
                
                val position = if (isSeeking) {
                    sliderPosition!!
                } else {
                    val playerPos = playerConnection.player.currentPosition
                    val isPlaying = playerConnection.player.isPlaying
                    if (playerPos != lastPlayerPos || !isPlaying) {
                        lastPlayerPos = playerPos
                        lastUpdateTime = now
                    }
                    val elapsed = now - lastUpdateTime
                    lastPlayerPos + (if (isPlaying) elapsed else 0L)
                }
                
                currentPlaybackPosition = position
                val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
                val effectivePosition = position + lyricsOffset
                
                val initialActiveIndices = findActiveLineIndices(lines, effectivePosition)
                val scrollActiveIndicesRaw = findActiveLineIndices(lines, effectivePosition + (if (hasWordTimings) 0L else 250L))
                
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
                    if (targetToScroll != -1 && (isSeeking || targetToScroll > scrollTargetIndex)) {
                        scrollTargetIndex = targetToScroll
                    }
                }
                
                if (scrollMax > lastMainMaxSeen && scrollMax != -1) {
                    lastMainMaxSeen = scrollMax
                }
                
                previousScrollActiveIndices = scrollActiveIndices
                activeLineIndices = newActiveIndices
                currentLineIndex = scrollMax
            }
        } else {
            while (isActive) {
                delay(8)
                val sliderPosition = sliderPositionProvider()
                isSeeking = sliderPosition != null
                val position = sliderPosition ?: playerConnection.player.currentPosition
                currentPlaybackPosition = position
                val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
                currentLineIndex = findCurrentLineIndex(lines, position + lyricsOffset)
            }
        }
    }

    LaunchedEffect(isSeeking, lastPreviewTime) {
        if (isSeeking) {
            lastPreviewTime = 0L
        } else if (lastPreviewTime != 0L) {
            delay(LyricsPreviewTime)
            lastPreviewTime = 0L
            isAutoScrollEnabled = true
        }
    }

    suspend fun performSmoothPageScroll(targetIndex: Int, duration: Int = 700) {
        isAnimating = true
        try {
            val lookUpIndex = if (isLyricsProviderShown) targetIndex + 1 else targetIndex
            val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == lookUpIndex }
            if (itemInfo != null) {
                if (lyricsAnimationStyle == LyricsAnimationStyle.VIVIMUSIC_1) {
                    val offset = if (useNewPlayerDesign) {
                        val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                        val anchor = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight * LYRICS_ANCHOR_RATIO).toInt()
                        val itemCenter = itemInfo.offset + itemInfo.size / 2
                        itemCenter - anchor
                    } else {
                        itemInfo.offset
                    }
                    if (kotlin.math.abs(offset) > 4) {
                        lazyListState.animateScrollBy(
                            value = offset.toFloat(),
                            animationSpec = tween(durationMillis = duration, easing = LYRIC_EASING)
                        )
                    }
                } else {
                    // Item is visible — animate directly to center with smooth deceleration
                    val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                    val center = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                    val itemCenter = itemInfo.offset + itemInfo.size / 2
                    val offset = itemCenter - center
                    if (kotlin.math.abs(offset) > 10) {
                        lazyListState.animateScrollBy(
                            value = offset.toFloat(),
                            animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
                        )
                    }
                }
            } else {
                // Item not visible — jump to it so next cycle can animate from a visible position
                lazyListState.scrollToItem(targetIndex)
            }
        } finally {
            isAnimating = false
        }
    }
    LaunchedEffect(scrollTargetIndex, isAutoScrollEnabled, lyricsAnimationStyle) {
        if (lyricsAnimationStyle == LyricsAnimationStyle.METRO_LYRICS && scrollTargetIndex != -1 && isAutoScrollEnabled) {
            deferredCurrentLineIndex = scrollTargetIndex
        }
    }

    LaunchedEffect(currentLineIndex, lastPreviewTime, initialScrollDone, isAutoScrollEnabled, lyricsAnimationStyle) {
        if (lyricsAnimationStyle == LyricsAnimationStyle.METRO_LYRICS || lyricsAnimationStyle == LyricsAnimationStyle.VIVIMUSIC_1) return@LaunchedEffect
        if (!isSynced) return@LaunchedEffect
        if (isAutoScrollEnabled) {
        if((currentLineIndex == 0 && shouldScrollToFirstLine) || !initialScrollDone) {
            shouldScrollToFirstLine = false
            // Initial scroll to center the first line with medium animation (600ms)
            val initialCenterIndex = kotlin.math.max(0, currentLineIndex)
            performSmoothPageScroll(initialCenterIndex, 800) // Initial scroll duration
            if(!isAppMinimized) {
                initialScrollDone = true
            }
        } else if (currentLineIndex != -1) {
            deferredCurrentLineIndex = currentLineIndex
            if (isSeeking) {
                // Fast scroll for seeking to center the target line (300ms)
                val seekCenterIndex = kotlin.math.max(0, currentLineIndex)
                performSmoothPageScroll(seekCenterIndex, 500) // Fast seek duration
            } else if ((lastPreviewTime == 0L || currentLineIndex != previousLineIndex) && scrollLyrics) {
                // Auto-scroll when lyrics settings allow it
                if (currentLineIndex != previousLineIndex) {
                    // Calculate which line should be at the top to center the active group
                    val centerTargetIndex = currentLineIndex
                    performSmoothPageScroll(centerTargetIndex, 700) // Apple Music-like smooth flow
                }
            }
        }
        }
        if(currentLineIndex > 0) {
            shouldScrollToFirstLine = true
        }
        previousLineIndex = currentLineIndex
    }

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp)
    ) {
        val totalMaxHeight = maxHeight
        // Status UI for translation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .padding(top = 56.dp),
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

        val displayedCurrentLineIndex = if (!isAutoScrollEnabled) {
            currentLineIndex
        } else {
            if (isSeeking || isSelectionModeActive) deferredCurrentLineIndex else currentLineIndex
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (lyrics == LYRICS_NOT_FOUND) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(0.5f)
                )
            }
        } else if (lyricsAnimationStyle == LyricsAnimationStyle.METRO_LYRICS) {
            val metroLyricsLineWrapper = @Composable { index: Int, item: LyricsEntry ->
                val isSelected = selectedIndices.contains(index)

                val lyricsOffset = currentSong?.song?.lyricsOffset?.toLong() ?: 0L
                val effectivePlaybackPosition = currentPlaybackPosition + lyricsOffset

                val pairedMainLineIndex = if (item.isBackground) (index - 1 downTo 0).firstOrNull { lines.getOrNull(it)?.isBackground == false } ?: -1 else -1
                val isInGapWithMain = if (item.isBackground && pairedMainLineIndex != -1) {
                    val pairedMainLine = lines[pairedMainLineIndex]
                    effectivePlaybackPosition >= pairedMainLine.time && effectivePlaybackPosition <= item.time
                } else false
                val bgVisible = !item.isBackground || (activeLineIndices.contains(pairedMainLineIndex) || activeLineIndices.contains(index) || isInGapWithMain)

                MetroLyricsLine(
                    entry = item,
                    nextEntryTime = lines.getOrNull(index + 1)?.time,
                    effectivePlaybackPosition = effectivePlaybackPosition,
                    lyricsOffset = lyricsOffset,
                    isSynced = isSynced,
                    isActive = activeLineIndices.contains(index),
                    distanceFromCurrent = kotlin.math.abs(index - deferredCurrentLineIndex),
                    lyricsTextPosition = lyricsTextPosition,
                    textColor = textColor,
                    showRomanized = currentSong?.romanizeLyrics == true && (
                            romanizeAllLyrics ||
                                    romanizeJapaneseLyrics ||
                                    romanizeKoreanLyrics ||
                                    romanizeRussianLyrics ||
                                    romanizeUkrainianLyrics ||
                                    romanizeSerbianLyrics ||
                                    romanizeBulgarianLyrics ||
                                    romanizeBelarusianLyrics ||
                                    romanizeKyrgyzLyrics ||
                                    romanizeMacedonianLyrics ||
                                    romanizeChineseLyrics ||
                                    romanizeHindiLyrics ||
                                    romanizePunjabiLyrics ||
                                    romanizeMalayalamLyrics),
                    showTranslated = hasActiveTranslations,
                    isAutoScrollActive = isAutoScrollEnabled,
                    isSelectionModeActive = isSelectionModeActive,
                    isSelected = isSelected,
                    expressiveAccent = expressiveAccent,
                    bgVisible = bgVisible,
                    onClick = {
                        if (isSelectionModeActive) {
                            if (isSelected) {
                                selectedIndices.remove(index)
                                if (selectedIndices.isEmpty()) isSelectionModeActive = false
                            } else {
                                if (selectedIndices.size < maxSelectionLimit) selectedIndices.add(index)
                                else showMaxSelectionToast = true
                            }
                        } else if (isSynced && changeLyrics && !isGuest) {
                            val lyricsOffsetVal = currentSong?.song?.lyricsOffset ?: 0
                            playerConnection.seekTo((item.time - lyricsOffsetVal).coerceAtLeast(0))
                            scrollTargetIndex = index
                            isAutoScrollEnabled = true
                            lastPreviewTime = 0L
                        }
                    },
                    onLongClick = {
                        if (!isSelectionModeActive) {
                            isSelectionModeActive = true
                            selectedIndices.add(index)
                        } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                            selectedIndices.add(index)
                        } else if (!isSelected) {
                            showMaxSelectionToast = true
                        }
                    }
                )
            }

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
                        val height = itemHeights[i]?.toFloat() ?: (if (item is LyricsListItem.Indicator) indicatorHeightPx else lineHeightPx)
                        val noGap = (item as? LyricsListItem.Line)?.entry?.isBackground == true || item is LyricsListItem.Indicator
                        currentY -= (height + if (noGap) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() })
                        map[i] = currentY
                    }
                    currentY = 0f
                    for (i in activeListIndex until mergedLyricsList.size - 1) {
                        val currentItem = mergedLyricsList[i]
                        val nextItem = mergedLyricsList[i + 1]
                        val height = itemHeights[i]?.toFloat() ?: (if (currentItem is LyricsListItem.Indicator) indicatorHeightPx else lineHeightPx)
                        val nextNoGap = (nextItem as? LyricsListItem.Line)?.entry?.isBackground == true || nextItem is LyricsListItem.Indicator
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
                        val height = itemHeights[i]?.toFloat() ?: (if (currentItem is LyricsListItem.Indicator) indicatorHeightPx else constraintLineHeightPx)
                        val nextNoGap = (nextItem as? LyricsListItem.Line)?.entry?.isBackground == true || nextItem is LyricsListItem.Indicator
                        (height + if (nextNoGap) 0f else with(density) { LYRICS_ITEM_GAP_DP.toPx() }).toDouble()
                    }.toFloat()
                    val lastItem = mergedLyricsList.last()
                    val lastHeight = itemHeights[mergedLyricsList.size - 1]?.toFloat() ?: (if (lastItem is LyricsListItem.Indicator) indicatorHeightPx else constraintLineHeightPx)
                    with(density) { 100.dp.toPx() } - anchorY - totalBelow - lastHeight
                }

                val maxOffset = remember(itemHeights.toMap(), mergedLyricsList, activeListIndex, maxHeightPx, anchorY) {
                    if (mergedLyricsList.isEmpty() || activeListIndex == -1) return@remember 0f
                    val indicatorHeightPx = with(density) { 72.dp.toPx() }
                    val totalAbove = (0 until activeListIndex).sumOf { i ->
                        val item = mergedLyricsList[i]
                        val height = itemHeights[i]?.toFloat() ?: (if (item is LyricsListItem.Indicator) indicatorHeightPx else constraintLineHeightPx)
                        val noGap = (item as? LyricsListItem.Line)?.entry?.isBackground == true || item is LyricsListItem.Indicator
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
                            text = "Lyrics from ${lyricsEntity?.provider}",
                            fontSize = 12.sp,
                            color = providerTextColor.copy(alpha = if (isInitialLayout) 0f else 0.6f),
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
                                    is LyricsListItem.Line -> {
                                        metroLyricsLineWrapper(listItem.index, listItem.entry)
                                    }
                                    is LyricsListItem.Indicator -> {
                                        val indicatorVisible = isAutoScrollEnabled &&
                                            currentPlaybackPosition >= listItem.gapStartMs &&
                                            currentPlaybackPosition <= listItem.gapEndMs - 650L
                                        IntervalIndicator(
                                            gapStartMs = listItem.gapStartMs,
                                            gapEndMs = listItem.gapEndMs - 650L,
                                            currentPositionMs = currentPlaybackPosition,
                                            visible = indicatorVisible,
                                            color = expressiveAccent,
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
        } else {
            val lyricsOffset = currentSong?.song?.lyricsOffset?.toLong() ?: 0L
            val lyricClock = rememberLyricClock(
                trackKey = currentSong?.id,
                positionProvider = { (sliderPositionProvider() ?: playerConnection.player.currentPosition).coerceAtLeast(0L) + lyricsOffset },
                isPlaying = isPlaying,
            )

            val displayLines = remember(lines, lyricsAnimationStyle, isSynced) {
                if (lyricsAnimationStyle == LyricsAnimationStyle.VIVIMUSIC_1 && isSynced) {
                    lines.withInstrumentalGaps()
                } else {
                    lines
                }
            }

            val lineAlignments = remember(displayLines) {
                computeLineAlignments(displayLines)
            }
            val isDuet = remember(lineAlignments) {
                lineAlignments.any { it == LyricAlignment.End }
            }

            val activeRows by remember(displayLines, isSynced) {
                derivedStateOf {
                    if (!isSynced) emptyList() else activeLyricRows(displayLines, lyricClock.longValue)
                }
            }
            val scrollLine = activeRows.firstOrNull() ?: -1
            val leadLine by remember(displayLines, isSynced) {
                derivedStateOf {
                    if (!isSynced) -1
                    else {
                        val now = lyricClock.longValue
                        activeLyricRows(displayLines, now + scrollLead(displayLines, now)).firstOrNull() ?: -1
                    }
                }
            }
            val focusLine = if (leadLine >= 0) leadLine else scrollLine

            var run by remember(displayLines) { mutableStateOf(ScrollRun(0, 0f, LYRIC_SETTLE_MS)) }
            val since = remember(displayLines) { mutableFloatStateOf(0f) }
            LaunchedEffect(run.id) {
                if (run.id == 0) return@LaunchedEffect
                animate(
                    initialValue = 0f,
                    targetValue = run.spanMs,
                    animationSpec = tween(run.spanMs.toInt(), easing = LinearEasing),
                ) { value, _ -> since.floatValue = value }
            }

            LaunchedEffect(focusLine, showLyrics, isAutoScrollEnabled, isPlaying, displayLines, useNewPlayerDesign, isLyricsProviderShown) {
                if (lyricsAnimationStyle == LyricsAnimationStyle.VIVIMUSIC_1 &&
                    isSynced && isAutoScrollEnabled && showLyrics && focusLine >= 0 && focusLine in displayLines.indices
                ) {
                    val targetIndex = if (isLyricsProviderShown) focusLine + 1 else focusLine
                    val visible = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
                    if (visible != null) {
                        val span = scrollLead(displayLines, lyricClock.longValue).toInt()
                        val distance = if (useNewPlayerDesign) {
                            val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                            val anchor = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight * LYRICS_ANCHOR_RATIO).toInt()
                            val itemCenter = visible.offset + visible.size / 2
                            (itemCenter - anchor).toFloat()
                        } else {
                            visible.offset.toFloat()
                        }
                        if (kotlin.math.abs(distance) > 4) {
                            run = ScrollRun(run.id + 1, distance, span)
                            lazyListState.animateScrollBy(
                                value = distance,
                                animationSpec = tween(durationMillis = span, easing = LYRIC_EASING)
                            )
                        }
                    } else {
                        if (useNewPlayerDesign) {
                            lazyListState.animateScrollToItem(targetIndex, scrollOffset = 0)
                        } else {
                            lazyListState.animateScrollToItem(targetIndex, scrollOffset = 0)
                        }
                    }
                }
            }

            LazyColumn(
            state = lazyListState,
            contentPadding = if (lyricsAnimationStyle == LyricsAnimationStyle.VIVIMUSIC_1) {
                if (useNewPlayerDesign) {
                    PaddingValues(
                        top = totalMaxHeight * LYRICS_ANCHOR_RATIO,
                        bottom = totalMaxHeight * (1f - LYRICS_ANCHOR_RATIO),
                        start = 0.dp,
                        end = 0.dp,
                    )
                } else {
                    PaddingValues(
                        top = 40.dp - GLOW_ROOM,
                        bottom = totalMaxHeight * 0.8f,
                        start = 0.dp,
                        end = 0.dp,
                    )
                }
            } else {
                WindowInsets.systemBars
                    .only(WindowInsetsSides.Top)
                    .add(WindowInsets(top = totalMaxHeight / 3, bottom = totalMaxHeight / 2))
                    .asPaddingValues()
            },
            modifier = Modifier
                .fadingEdge(vertical = 64.dp)
                .nestedScroll(remember {
                    object : NestedScrollConnection {
                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            if (source == NestedScrollSource.UserInput) {
                                isAutoScrollEnabled = false
                            }
                            if (!isSelectionModeActive) { // Only update preview time if not selecting
                                lastPreviewTime = System.currentTimeMillis()
                            }
                            return super.onPostScroll(consumed, available, source)
                        }

                        override suspend fun onPostFling(
                            consumed: Velocity,
                            available: Velocity
                        ): Velocity {
                            isAutoScrollEnabled = false
                            if (!isSelectionModeActive) { // Only update preview time if not selecting
                                lastPreviewTime = System.currentTimeMillis()
                            }
                            return super.onPostFling(consumed, available)
                        }
                    }
                })
        ) {

            // Show lyrics provider at the top, scrolling with content
            if (isLyricsProviderShown) {
                item {
                    Text(
                        text = "Lyrics from ${lyricsEntity?.provider}",
                        fontSize = 12.sp,
                        color = providerTextColor.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            if (lyrics == null) {
                item {
                    ShimmerHost {
                        repeat(10) {
                            Box(
                                contentAlignment = when (lyricsTextPosition) {
                                    LyricsPosition.LEFT -> Alignment.CenterStart
                                    LyricsPosition.CENTER -> Alignment.Center
                                    LyricsPosition.RIGHT -> Alignment.CenterEnd
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 4.dp)
                            ) {
                                TextPlaceholder()
                            }
                        }
                    }
                }
            } else {
                val lyricsOffset = currentSong?.song?.lyricsOffset?.toLong() ?: 0L
                val effectivePlaybackPosition = currentPlaybackPosition + lyricsOffset

                itemsIndexed(
                    items = displayLines,
                    key = { index, item -> "$index-${item.time}" } // Add stable key
                ) { index, item ->
                    val isSelected = selectedIndices.contains(index)
                    if (lyricsAnimationStyle == LyricsAnimationStyle.VIVIMUSIC_1) {
                        val showRomanized = (currentSong?.romanizeLyrics != false) && (
                                romanizeAllLyrics ||
                                        romanizeJapaneseLyrics ||
                                        romanizeKoreanLyrics ||
                                        romanizeRussianLyrics ||
                                        romanizeUkrainianLyrics ||
                                        romanizeSerbianLyrics ||
                                        romanizeBulgarianLyrics ||
                                        romanizeBelarusianLyrics ||
                                        romanizeKyrgyzLyrics ||
                                        romanizeMacedonianLyrics ||
                                        romanizeChineseLyrics ||
                                        romanizeHindiLyrics ||
                                        romanizePunjabiLyrics ||
                                        romanizeMalayalamLyrics)

                        val romanizedText by item.romanizedTextFlow.collectAsState()
                        val isRomanizedAvailable = showRomanized && romanizedText != null
                        val mainText = if (romanizeAsMain && isRomanizedAvailable) romanizedText!! else item.text
                        val subText = if (romanizeAsMain && isRomanizedAvailable) item.text else if (showRomanized) romanizedText else null

                        val nextEntryTime = displayLines.getOrNull(index + 1)?.time
                        val lyricLine = remember(item, nextEntryTime, mainText) {
                            if (romanizeAsMain && isRomanizedAvailable) {
                                ViviLyricLine(
                                    timeMs = item.time,
                                    text = mainText,
                                    words = emptyList(),
                                    sungUntilMs = nextEntryTime
                                )
                            } else {
                                item.toViviLyricLine(nextEntryTime)
                            }
                        }

                        val isLineAnimating = lyricLine.isWordSynced && (lyricClock.longValue in lyricLine.animatesFromMs..lyricLine.animatesUntilMs)
                        val isActive = isSynced && (index in activeRows || isLineAnimating)
                        val offset = if (scrollLine < 0) 0 else index - scrollLine
                        val distanceFromCurrent = kotlin.math.abs(offset)
                        val sung = offset < 0
                        val step = distanceFromCurrent.coerceAtMost(LINE_FALLOFF_ALPHA_PHONE.lastIndex)

                        val tail by animateFloatAsState(
                            targetValue = if (sung) 1f else UNSUNG_ALPHA,
                            label = "lyricTail_$index",
                        )

                        val targetBlur = when {
                            !isSynced || !isAutoScrollEnabled || isActive || isSelectionModeActive -> 0.dp
                            else -> LINE_FALLOFF_BLUR[step.coerceAtMost(LINE_FALLOFF_BLUR.lastIndex)]
                        }

                        val animatedBlur by animateDpAsState(
                            targetValue = targetBlur,
                            animationSpec = tween(LYRIC_SETTLE_MS, easing = LYRIC_EASING),
                            label = "lyricBlur"
                        )

                        val targetAlpha = when {
                            !isSynced || (isSelectionModeActive && isSelected) -> 1f
                            isActive -> 1f
                            else -> LINE_FALLOFF_ALPHA_PHONE[step]
                        }

                        val animatedAlpha by animateFloatAsState(
                            targetValue = targetAlpha,
                            animationSpec = tween(LYRIC_SETTLE_MS, easing = LYRIC_EASING),
                            label = "lyricAlpha"
                        )

                        val behind = if (run.delta >= 0f) index - focusLine else focusLine - index
                        val staggerDelay = behind.coerceIn(0, STAGGER_STEPS) * STAGGER_FRACTION * run.durationMs
                        val rowTranslationY = if (staggerDelay <= 0f) {
                            0f
                        } else {
                            val elapsed = since.floatValue
                            run.delta * (
                                LYRIC_EASING.transform((elapsed / run.durationMs).coerceIn(0f, 1f)) -
                                LYRIC_EASING.transform(((elapsed - staggerDelay) / run.durationMs).coerceIn(0f, 1f))
                            )
                        }

                        val interaction = remember { MutableInteractionSource() }
                        val pressed by interaction.collectIsPressedAsState()

                        val scale by animateFloatAsState(
                            targetValue = when {
                                pressed -> PRESSED_SCALE
                                isActive -> 1f
                                else -> INACTIVE_SCALE
                            },
                            animationSpec = tween(
                                durationMillis = if (pressed) 120 else LYRIC_SETTLE_MS,
                                easing = LYRIC_EASING,
                            ),
                            label = "lyricScale"
                        )

                        val glow by animateFloatAsState(
                            targetValue = if (isActive) GLOW_ALPHA else 0f,
                            animationSpec = tween(durationMillis = 420),
                            label = "lyricGlow"
                        )

                        val alignEnd = isDuet && lineAlignments.getOrNull(index) == LyricAlignment.End
                        val isGroup = item.agent?.lowercase() in listOf("v1000", "group")

                        val agentTextAlign = when {
                            isGroup -> TextAlign.Center
                            isDuet -> if (alignEnd) TextAlign.End else TextAlign.Start
                            else -> when (lyricsTextPosition) {
                                LyricsPosition.LEFT -> TextAlign.Left
                                LyricsPosition.CENTER -> TextAlign.Center
                                LyricsPosition.RIGHT -> TextAlign.Right
                            }
                        }

                        val agentAlignment = when (agentTextAlign) {
                            TextAlign.Center -> Alignment.CenterHorizontally
                            TextAlign.End, TextAlign.Right -> Alignment.End
                            else -> Alignment.Start
                        }

                        if (item.isInstrumental) {
                            val until = displayLines.getOrNull(index + 1)?.time ?: (item.time + 4000L)
                            InstrumentalCookieIndicator(
                                timeMs = item.time,
                                untilMs = until,
                                isActive = isActive,
                                clock = lyricClock,
                                color = expressiveAccent,
                                lineAlpha = animatedAlpha,
                                blurRadius = animatedBlur,
                                glowRoom = GLOW_ROOM,
                                alignment = agentAlignment,
                                modifier = Modifier.graphicsLayer {
                                    this.translationY = rowTranslationY
                                },
                                onSeek = {
                                    if (isSynced && changeLyrics && !isGuest) {
                                        val lyricsOffsetVal = currentSong?.song?.lyricsOffset ?: 0
                                        playerConnection.seekTo((item.time - lyricsOffsetVal).coerceAtLeast(0))
                                        isAutoScrollEnabled = true
                                        scope.launch {
                                            val targetIndex = if (isLyricsProviderShown) index + 1 else index
                                            if (useNewPlayerDesign) {
                                                lazyListState.animateScrollToItem(targetIndex, scrollOffset = 0)
                                            } else {
                                                lazyListState.scrollToItem(index = targetIndex)
                                            }
                                        }
                                        lastPreviewTime = 0L
                                    }
                                }
                            )
                            return@itemsIndexed
                        }

                        val transformOrigin = when (agentTextAlign) {
                            TextAlign.End, TextAlign.Right -> TransformOrigin(1f, 0.5f)
                            TextAlign.Center -> TransformOrigin(0.5f, 0.5f)
                            else -> TransformOrigin(0f, 0.5f)
                        }

                        val itemModifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = if (isDuet && alignEnd && !isGroup) DUET_LANE else 0.dp,
                                end = if (isDuet && !alignEnd && !isGroup) DUET_LANE else 0.dp,
                            )
                            .graphicsLayer {
                                this.scaleX = scale
                                this.scaleY = scale
                                this.transformOrigin = transformOrigin
                                this.alpha = animatedAlpha
                                this.translationY = rowTranslationY
                            }
                            .blur(animatedBlur, BlurredEdgeTreatment.Unbounded)
                            .clip(RoundedCornerShape(10.dp))
                            .combinedClickable(
                                enabled = true,
                                interactionSource = interaction,
                                indication = LocalIndication.current,
                                onClick = {
                                    if (isSelectionModeActive) {
                                        if (isSelected) {
                                            selectedIndices.remove(index)
                                            if (selectedIndices.isEmpty()) isSelectionModeActive = false
                                        } else {
                                            if (selectedIndices.size < maxSelectionLimit) selectedIndices.add(index)
                                            else showMaxSelectionToast = true
                                        }
                                    } else if (isSynced && changeLyrics && !isGuest) {
                                        val lyricsOffsetVal = currentSong?.song?.lyricsOffset ?: 0
                                        playerConnection.seekTo((item.time - lyricsOffsetVal).coerceAtLeast(0))
                                        isAutoScrollEnabled = true
                                        scope.launch {
                                            val targetIndex = if (isLyricsProviderShown) index + 1 else index
                                            val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
                                            if (itemInfo != null) {
                                                val offset = if (useNewPlayerDesign) {
                                                    val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                                                    val anchor = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight * LYRICS_ANCHOR_RATIO).toInt()
                                                    val itemCenter = itemInfo.offset + itemInfo.size / 2
                                                    (itemCenter - anchor).toFloat()
                                                } else {
                                                    itemInfo.offset.toFloat()
                                                }
                                                if (kotlin.math.abs(offset) > 4) {
                                                    lazyListState.animateScrollBy(
                                                        value = offset,
                                                        animationSpec = tween(durationMillis = 600, easing = LYRIC_EASING)
                                                    )
                                                }
                                            } else {
                                                if (useNewPlayerDesign) {
                                                    lazyListState.animateScrollToItem(targetIndex, scrollOffset = 0)
                                                } else {
                                                    lazyListState.scrollToItem(index = targetIndex)
                                                }
                                            }
                                        }
                                        lastPreviewTime = 0L
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionModeActive) {
                                        isSelectionModeActive = true
                                        selectedIndices.add(index)
                                    } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                        selectedIndices.add(index)
                                    } else if (!isSelected) {
                                        showMaxSelectionToast = true
                                    }
                                }
                            )
                            .background(
                                if (isSelected && isSelectionModeActive)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .padding(vertical = 2.dp)

                        val boxAlignment = when (agentTextAlign) {
                            TextAlign.Center -> Alignment.Center
                            TextAlign.End, TextAlign.Right -> Alignment.CenterEnd
                            else -> Alignment.CenterStart
                        }

                        val lineTextColor = if (lyricsAnimationStyle == LyricsAnimationStyle.VIVIMUSIC_1) Color.White else textColor

                        val pairedMainIndex = remember(displayLines, index) {
                            (index - 1 downTo 0).firstOrNull { !displayLines[it].isBackground && !displayLines[it].isInstrumental } ?: -1
                        }
                        val bgVisible = !item.isBackground || isSelectionModeActive ||
                            (index in activeRows || index == focusLine || isLineAnimating) ||
                            (pairedMainIndex != -1 && (pairedMainIndex in activeRows || pairedMainIndex == focusLine))

                        AnimatedVisibility(
                            visible = bgVisible,
                            enter = fadeIn(tween(280)) + expandVertically(tween(320, easing = LYRIC_EASING)),
                            exit = fadeOut(tween(280)) + shrinkVertically(tween(320, easing = LYRIC_EASING))
                        ) {
                            Column(
                                modifier = itemModifier,
                                horizontalAlignment = when (agentTextAlign) {
                                    TextAlign.Center -> Alignment.CenterHorizontally
                                    TextAlign.End, TextAlign.Right -> Alignment.End
                                    else -> Alignment.Start
                                }
                            ) {
                            if (lyricLine.isWordSynced) {
                                var layout by remember(lyricLine.text) { mutableStateOf<TextLayoutResult?>(null) }
                                val growth = remember { CharGrowth() }

                                val phase = remember(lyricLine, lyricClock) {
                                    derivedStateOf(structuralEqualityPolicy()) {
                                        val now = lyricClock.value
                                        when {
                                            now < lyricLine.animatesFromMs -> PHASE_BEFORE
                                            now > lyricLine.animatesUntilMs -> PHASE_AFTER
                                            else -> PHASE_MOVING
                                        }
                                    }
                                }

                                val drawnAt: () -> Long = {
                                    when (phase.value) {
                                        PHASE_MOVING -> lyricClock.value
                                        PHASE_BEFORE -> lyricLine.animatesFromMs - 1
                                        else -> lyricLine.animatesUntilMs + 1
                                    }
                                }

                                val room = Modifier.padding(GLOW_ROOM)

                                val riseAgainst: (Modifier) -> Modifier = { inner ->
                                    Modifier
                                        .drawWithContent {
                                            val measured = layout
                                            if (measured == null || lyricLine.words.isEmpty()) {
                                                drawContent()
                                            } else {
                                                riseWith(
                                                    layout = measured,
                                                    line = lyricLine,
                                                    positionMs = drawnAt(),
                                                    inset = GLOW_ROOM.toPx(),
                                                    peak = WORD_RISE.toPx(),
                                                    growth = growth,
                                                )
                                            }
                                        }
                                        .then(inner)
                                }

                                val sweep = Modifier.drawWithContent {
                                    val position = drawnAt()
                                    when {
                                        position >= lyricLine.endMs -> drawContent()
                                        position <= lyricLine.timeMs -> Unit
                                        else -> layout?.let {
                                            sweepTo(it, lyricLine.revealedChars(position), lyricLine.sweepSpans, isActive)
                                        }
                                    }
                                }

                                val isPureBacking = item.isBackground || (lyricLine.text.trim().startsWith("(") && lyricLine.text.trim().endsWith(")"))

                                val lyricStyle = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = if (isPureBacking) BACKING_FONT_SIZE else if (isSynced) SYNCED_FONT_SIZE else UNSYNCED_FONT_SIZE,
                                    lineHeight = if (isPureBacking) BACKING_LINE_HEIGHT else if (isSynced) SYNCED_LINE_HEIGHT else UNSYNCED_LINE_HEIGHT,
                                    fontStyle = if (isPureBacking) FontStyle.Italic else FontStyle.Normal,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = agentTextAlign,
                                )

                                val effectiveColor = if (isPureBacking) lineTextColor.copy(alpha = BACKING_ALPHA) else lineTextColor

                                val styledAnnotatedText = remember(lyricLine.text, isPureBacking, effectiveColor) {
                                    if (isPureBacking) {
                                        AnnotatedString(lyricLine.text)
                                    } else {
                                        val parenRegex = Regex("\\([^)]+\\)")
                                        if (!parenRegex.containsMatchIn(lyricLine.text)) {
                                            AnnotatedString(lyricLine.text)
                                        } else {
                                            buildAnnotatedString {
                                                append(lyricLine.text)
                                                parenRegex.findAll(lyricLine.text).forEach { match ->
                                                    addStyle(
                                                        SpanStyle(
                                                            fontSize = BACKING_FONT_SIZE,
                                                            fontStyle = FontStyle.Italic,
                                                            color = effectiveColor.copy(alpha = BACKING_ALPHA)
                                                        ),
                                                        match.range.first,
                                                        match.range.last + 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                val styledUnlitAnnotatedText = remember(lyricLine.text, isPureBacking, effectiveColor, tail) {
                                    if (isPureBacking) {
                                        AnnotatedString(lyricLine.text)
                                    } else {
                                        val parenRegex = Regex("\\([^)]+\\)")
                                        if (!parenRegex.containsMatchIn(lyricLine.text)) {
                                            AnnotatedString(lyricLine.text)
                                        } else {
                                            buildAnnotatedString {
                                                append(lyricLine.text)
                                                parenRegex.findAll(lyricLine.text).forEach { match ->
                                                    addStyle(
                                                        SpanStyle(
                                                            fontSize = BACKING_FONT_SIZE,
                                                            fontStyle = FontStyle.Italic,
                                                            color = effectiveColor.copy(alpha = tail * 0.45f)
                                                        ),
                                                        match.range.first,
                                                        match.range.last + 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = boxAlignment,
                                ) {
                                    // 1. Dim background unlit layer
                                    Text(
                                        text = styledUnlitAnnotatedText,
                                        style = lyricStyle,
                                        color = effectiveColor.copy(alpha = tail),
                                        textAlign = agentTextAlign,
                                        onTextLayout = { layout = it },
                                        modifier = riseAgainst(room),
                                    )

                                    // 2. Blooming layer on held notes
                                    if (glow > 0.01f) {
                                        Text(
                                            text = styledAnnotatedText,
                                            style = lyricStyle,
                                            color = effectiveColor,
                                            textAlign = agentTextAlign,
                                            modifier = Modifier
                                                .graphicsLayer { alpha = glow }
                                                .blur(GLOW_RADIUS, BlurredEdgeTreatment.Unbounded)
                                                .then(room)
                                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                                .drawWithContent {
                                                    val measured = layout ?: return@drawWithContent
                                                    glowGrown(
                                                        layout = measured,
                                                        line = lyricLine,
                                                        positionMs = drawnAt(),
                                                        inset = GLOW_ROOM.toPx(),
                                                        peak = WORD_RISE.toPx(),
                                                        growth = growth,
                                                    )
                                                },
                                        )
                                    }

                                    // 3. Lit layer with progressive sweep
                                    Text(
                                        text = styledAnnotatedText,
                                        style = lyricStyle,
                                        color = effectiveColor,
                                        textAlign = agentTextAlign,
                                        modifier = riseAgainst(
                                            Modifier
                                                .graphicsLayer {
                                                    compositingStrategy = if (isActive) {
                                                        CompositingStrategy.Offscreen
                                                    } else {
                                                        CompositingStrategy.Auto
                                                    }
                                                }
                                                .then(room)
                                                .then(sweep)
                                        ),
                                    )
                                }
                            } else {
                                // ── Non-word-synced line (Apple Music whole sentence highlight) ──────
                                val sentenceAlpha by animateFloatAsState(
                                    targetValue = if (isActive) 1f else UNSUNG_ALPHA,
                                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                                    label = "sentenceAlpha"
                                )

                                val isPureBacking = item.isBackground || (mainText.trim().startsWith("(") && mainText.trim().endsWith(")"))

                                val lyricStyle = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = if (isPureBacking) BACKING_FONT_SIZE else if (isSynced) SYNCED_FONT_SIZE else UNSYNCED_FONT_SIZE,
                                    lineHeight = if (isPureBacking) BACKING_LINE_HEIGHT else if (isSynced) SYNCED_LINE_HEIGHT else UNSYNCED_LINE_HEIGHT,
                                    fontStyle = if (isPureBacking) FontStyle.Italic else FontStyle.Normal,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = agentTextAlign,
                                    shadow = if (sentenceAlpha > UNSUNG_ALPHA) {
                                        val factor = (sentenceAlpha - UNSUNG_ALPHA) / (1f - UNSUNG_ALPHA)
                                        Shadow(
                                            color = lineTextColor.copy(alpha = 0.5f * factor),
                                            offset = Offset.Zero,
                                            blurRadius = (10f * factor).coerceAtLeast(0.1f)
                                        )
                                    } else null
                                )

                                val styledAnnotatedMainText = remember(mainText, isPureBacking, lineTextColor, sentenceAlpha, isActive) {
                                    if (isPureBacking) {
                                        AnnotatedString(mainText)
                                    } else {
                                        val parenRegex = Regex("\\([^)]+\\)")
                                        if (!parenRegex.containsMatchIn(mainText)) {
                                            AnnotatedString(mainText)
                                        } else {
                                            val adLibAlpha = if (isActive) sentenceAlpha * BACKING_ALPHA else sentenceAlpha * 0.45f
                                            buildAnnotatedString {
                                                append(mainText)
                                                parenRegex.findAll(mainText).forEach { match ->
                                                    addStyle(
                                                        SpanStyle(
                                                            fontSize = BACKING_FONT_SIZE,
                                                            fontStyle = FontStyle.Italic,
                                                            color = lineTextColor.copy(alpha = adLibAlpha)
                                                        ),
                                                        match.range.first,
                                                        match.range.last + 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = styledAnnotatedMainText,
                                    style = lyricStyle,
                                    color = lineTextColor.copy(alpha = sentenceAlpha),
                                    textAlign = agentTextAlign,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(GLOW_ROOM)
                                )
                            }

                            // ── Sub-lyrics (Romanized / Translated) tucked into glow room ─────────
                            subText?.let { sub ->
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = SUB_LYRIC_FONT_SIZE,
                                        lineHeight = SUB_LYRIC_LINE_HEIGHT,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = agentTextAlign,
                                    ),
                                    color = lineTextColor.copy(alpha = SUB_LYRIC_ALPHA),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = GLOW_ROOM)
                                        .offset(y = -SUB_LYRIC_TUCK)
                                )
                            }

                            if (hasActiveTranslations) {
                                val translatedText by item.translatedTextFlow.collectAsState()
                                translatedText?.let { translated ->
                                    Text(
                                        text = translated,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = SUB_LYRIC_FONT_SIZE,
                                            lineHeight = SUB_LYRIC_LINE_HEIGHT,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = agentTextAlign,
                                        ),
                                        color = lineTextColor.copy(alpha = SUB_LYRIC_ALPHA),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = GLOW_ROOM)
                                            .offset(y = -SUB_LYRIC_TUCK)
                                    )
                                }
                            }
                        }
                    }
                    return@itemsIndexed
                    } else if (lyricsAnimationStyle == LyricsAnimationStyle.METRO_LYRICS) {
                        val currentLineTime = if (displayedCurrentLineIndex >= 0 && displayedCurrentLineIndex < lines.size) {
                            lines[displayedCurrentLineIndex].time
                        } else -1L
                        val isLineAtSameTime = item.time == currentLineTime
                        val isActiveByIndex = index == displayedCurrentLineIndex
                        val isActiveByTime = isLineAtSameTime && displayedCurrentLineIndex >= 0

                        val pairedMainLineIndex = if (item.isBackground) (index - 1 downTo 0).firstOrNull { lines.getOrNull(it)?.isBackground == false } ?: -1 else -1
                        val isInGapWithMain = if (item.isBackground && pairedMainLineIndex != -1) {
                            val pairedMainLine = lines[pairedMainLineIndex]
                            effectivePlaybackPosition >= pairedMainLine.time && effectivePlaybackPosition <= item.time
                        } else false
                        val bgVisible = !item.isBackground || (activeLineIndices.contains(pairedMainLineIndex) || activeLineIndices.contains(index) || isInGapWithMain)

                        MetroLyricsLine(
                            entry = item,
                            nextEntryTime = lines.getOrNull(index + 1)?.time,
                            effectivePlaybackPosition = effectivePlaybackPosition,
                            lyricsOffset = lyricsOffset,
                            isSynced = isSynced,
                            isActive = isActiveByIndex || isActiveByTime,
                            distanceFromCurrent = kotlin.math.abs(index - displayedCurrentLineIndex),
                            lyricsTextPosition = lyricsTextPosition,
                            textColor = textColor,
                            showRomanized = currentSong?.romanizeLyrics == true && (
                                    romanizeAllLyrics ||
                                            romanizeJapaneseLyrics ||
                                            romanizeKoreanLyrics ||
                                            romanizeRussianLyrics ||
                                            romanizeUkrainianLyrics ||
                                            romanizeSerbianLyrics ||
                                            romanizeBulgarianLyrics ||
                                            romanizeBelarusianLyrics ||
                                            romanizeKyrgyzLyrics ||
                                            romanizeMacedonianLyrics ||
                                            romanizeChineseLyrics ||
                                            romanizeHindiLyrics ||
                                            romanizePunjabiLyrics ||
                                            romanizeMalayalamLyrics),
                            showTranslated = hasActiveTranslations,
                            isAutoScrollActive = isAutoScrollEnabled,
                            isSelectionModeActive = isSelectionModeActive,
                            isSelected = isSelected,
                            expressiveAccent = expressiveAccent,
                            bgVisible = bgVisible,
                            onClick = {
                                if (isSelectionModeActive) {
                                    if (isSelected) {
                                        selectedIndices.remove(index)
                                        if (selectedIndices.isEmpty()) isSelectionModeActive = false
                                    } else {
                                        if (selectedIndices.size < maxSelectionLimit) selectedIndices.add(index)
                                        else showMaxSelectionToast = true
                                    }
                                } else if (isSynced && changeLyrics && !isGuest) {
                                    val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
                                    playerConnection.seekTo((item.time - lyricsOffset).coerceAtLeast(0))
                                    scope.launch {
                                        lazyListState.scrollToItem(index = index)
                                        val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                        if (itemInfo != null) {
                                            val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                                            val center = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                                            val itemCenter = itemInfo.offset + itemInfo.size / 2
                                            val offset = itemCenter - center
                                            if (kotlin.math.abs(offset) > 10) {
                                                lazyListState.animateScrollBy(
                                                    value = offset.toFloat(),
                                                    animationSpec = tween(durationMillis = 1500)
                                                )
                                            }
                                        }
                                    }
                                    lastPreviewTime = 0L
                                }
                            },
                            onLongClick = {
                                if (!isSelectionModeActive) {
                                    isSelectionModeActive = true
                                    selectedIndices.add(index)
                                } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                    selectedIndices.add(index)
                                } else if (!isSelected) {
                                    showMaxSelectionToast = true
                                }
                            }
                        )
                        return@itemsIndexed
                    }
                    val itemModifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)) // Clip for background
                        .combinedClickable(
                            enabled = true,
                            onClick = {
                                if (isSelectionModeActive) {
                                    // Toggle selection
                                    if (isSelected) {
                                        selectedIndices.remove(index)
                                        if (selectedIndices.isEmpty()) {
                                            isSelectionModeActive =
                                                false // Exit mode if last item deselected
                                        }
                                    } else {
                                        if (selectedIndices.size < maxSelectionLimit) {
                                            selectedIndices.add(index)
                                        } else {
                                            showMaxSelectionToast = true
                                        }
                                    }
                                } else if (isSynced && changeLyrics && !isGuest) {
                                    // Professional seek action with smooth animation
                                    val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
                                    playerConnection.seekTo((item.time - lyricsOffset).coerceAtLeast(0))
                                    // Smooth slow scroll when clicking on lyrics (3 seconds)
                                    scope.launch {
                                        // First scroll to the clicked item without animation
                                        lazyListState.scrollToItem(index = index)

                                        // Then animate it to center position slowly
                                        val itemInfo =
                                            lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                        if (itemInfo != null) {
                                            val viewportHeight =
                                                lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                                            val center =
                                                lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                                            val itemCenter = itemInfo.offset + itemInfo.size / 2
                                            val offset = itemCenter - center

                                            if (kotlin.math.abs(offset) > 10) { // Only animate if not already centered
                                                lazyListState.animateScrollBy(
                                                    value = offset.toFloat(),
                                                    animationSpec = tween(durationMillis = 1500) // Reduced to half speed
                                                )
                                            }
                                        }
                                    }
                                    lastPreviewTime = 0L
                                }
                            },
                            onLongClick = {
                                if (!isSelectionModeActive) {
                                    isSelectionModeActive = true
                                    selectedIndices.add(index)
                                } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                    // If already in selection mode and item not selected, add it if below limit
                                    selectedIndices.add(index)
                                } else if (!isSelected) {
                                    // If already at limit, show toast
                                    showMaxSelectionToast = true
                                }
                            }
                        )
                        .background(
                            if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.3f
                            )
                            else Color.Transparent
                        )
                        .padding(horizontal = 24.dp, vertical = (8 * lyricsLineSpacing).dp)
                    
                    // Check if this line shares the same time as the currently active line
                    // This enables synchronized word-by-word animation for both main and background vocals
                    val currentLineTime = if (displayedCurrentLineIndex >= 0 && displayedCurrentLineIndex < lines.size) {
                        lines[displayedCurrentLineIndex].time
                    } else -1L
                    val isLineAtSameTime = item.time == currentLineTime
                    val isActiveByIndex = index == displayedCurrentLineIndex
                    val isActiveByTime = isLineAtSameTime && displayedCurrentLineIndex >= 0
                    
                    val alpha by animateFloatAsState(
                        targetValue = when {
                            !isSynced || (isSelectionModeActive && isSelected) -> 1f
                            isActiveByIndex || isActiveByTime -> 1f
                            else -> 0.5f
                        },
                        animationSpec = tween(durationMillis = 400)
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isActiveByIndex || isActiveByTime) 1.05f else 1f,
                        animationSpec = tween(durationMillis = 400)
                    )

                    val blurRadius = 0f

                    // Determine alignment based on agent for multi-singer support
                    val alignEnd = isDuet && lineAlignments.getOrNull(index) == LyricAlignment.End
                    val isGroup = item.agent?.lowercase() in listOf("v1000", "group")

                    val agentAlignment = when {
                        item.isBackground || isGroup -> Alignment.CenterHorizontally
                        isDuet -> if (alignEnd) Alignment.End else Alignment.Start
                        else -> when (lyricsTextPosition) {
                            LyricsPosition.LEFT -> Alignment.Start
                            LyricsPosition.CENTER -> Alignment.CenterHorizontally
                            LyricsPosition.RIGHT -> Alignment.End
                        }
                    }
                    
                    val agentTextAlign = when {
                        item.isBackground || isGroup -> TextAlign.Center
                        isDuet -> if (alignEnd) TextAlign.End else TextAlign.Start
                        else -> when (lyricsTextPosition) {
                            LyricsPosition.LEFT -> TextAlign.Left
                            LyricsPosition.CENTER -> TextAlign.Center
                            LyricsPosition.RIGHT -> TextAlign.Right
                        }
                    }
                    
                    // Smaller scale for background vocals
                    val bgScale = if (item.isBackground) 0.85f else 1f

                    Column(
                        modifier = itemModifier
                            .padding(
                                start = if (isDuet && alignEnd && !isGroup) DUET_LANE else 0.dp,
                                end = if (isDuet && !alignEnd && !isGroup) DUET_LANE else 0.dp,
                            )
                            .graphicsLayer {
                                this.alpha = if (item.isBackground) alpha * 0.8f else alpha
                                this.scaleX = scale * bgScale
                                this.scaleY = scale * bgScale
                            }
                            .blur(blurRadius.dp),
                        horizontalAlignment = agentAlignment
                    ) {
                        // Use time-based active check to sync both main and background lines with same timestamp
                        val isActiveLine = (isActiveByIndex || isActiveByTime) && isSynced
                        val lineColor = if (isActiveLine) {
                            if (item.isBackground) expressiveAccent.copy(alpha = 0.85f) else expressiveAccent
                        } else {
                            expressiveAccent.copy(alpha = if (item.isBackground) 0.5f else 0.7f)
                        }
                        val alignment = agentTextAlign
                        
                        val romanizedTextState by item.romanizedTextFlow.collectAsState()
                        val romanizedText = romanizedTextState
                        val isRomanizedAvailable = romanizedText != null
                        
                        val mainText = if (romanizeAsMain && isRomanizedAvailable) romanizedText!! else item.text
                        val subText = if (romanizeAsMain && isRomanizedAvailable) item.text else romanizedText
                        
                        val hasWordTimings = if (romanizeAsMain && isRomanizedAvailable) false else item.words?.isNotEmpty() == true
                        
                        // Word-by-word animation styles
                        if (hasWordTimings && lyricsAnimationStyle == LyricsAnimationStyle.NONE) {
                            val styledText = buildAnnotatedString {
                                item.words?.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && effectivePlaybackPosition >= wordStartMs && effectivePlaybackPosition <= wordEndMs
                                    val hasWordPassed = isActiveLine && effectivePlaybackPosition > wordEndMs

                                    val transitionProgress = when {
                                        !isActiveLine -> 0f
                                        hasWordPassed -> 1f
                                        isWordActive && wordDuration > 0 -> {
                                            val elapsed = effectivePlaybackPosition - wordStartMs
                                            val linear = (elapsed.toFloat() / wordDuration).coerceIn(0f, 1f)
                                            linear * linear * (3f - 2f * linear)
                                        }
                                        else -> 0f
                                    }

                                    val wordAlpha = when {
                                        !isActiveLine -> 0.7f
                                        hasWordPassed -> 1f
                                        isWordActive -> 0.5f + (0.5f * transitionProgress)
                                        else -> 0.35f
                                    }

                                    val wordColor = expressiveAccent.copy(alpha = wordAlpha)
                                    val wordWeight = when {
                                        !isActiveLine -> FontWeight.Bold
                                        hasWordPassed -> FontWeight.Bold
                                        isWordActive -> FontWeight.ExtraBold
                                        else -> FontWeight.Medium
                                    }

                                    withStyle(style = SpanStyle(color = wordColor, fontWeight = wordWeight)) {
                                        append(word.text)
                                    }
                                    if (wordIndex < (item.words.size ?: 0) - 1) append(" ")
                                }
                            }
                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing.coerceAtMost(1.3f)).sp
                            )



                        } else if (isActiveLine) {
                            Text(
                                text = mainText,
                                fontSize = lyricsTextSize.sp,
                                color = expressiveAccent,
                                textAlign = alignment,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing.coerceAtMost(1.3f)).sp
                            )
                        } else {
                            // Inactive line
                            Text(
                                text = mainText,
                                fontSize = lyricsTextSize.sp,
                                color = lineColor,
                                textAlign = alignment,
                                fontWeight = FontWeight.Bold,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing.coerceAtMost(1.3f)).sp
                            )
                        }
                        if (currentSong?.romanizeLyrics == true
                            && (romanizeAllLyrics ||
                                    romanizeJapaneseLyrics ||
                                    romanizeKoreanLyrics ||
                                    romanizeRussianLyrics ||
                                    romanizeUkrainianLyrics ||
                                    romanizeSerbianLyrics ||
                                    romanizeBulgarianLyrics ||
                                    romanizeBelarusianLyrics ||
                                    romanizeKyrgyzLyrics ||
                                    romanizeMacedonianLyrics ||
                                    romanizeChineseLyrics ||
                                    romanizeHindiLyrics ||
                                    romanizePunjabiLyrics ||
                                    romanizeMalayalamLyrics)) {
                            // Show secondary text (romanized or original) if available
                            subText?.let { text ->
                                Text(
                                    text = text,
                                    fontSize = 18.sp,
                                    color = expressiveAccent.copy(alpha = 0.6f),
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
                        
                        // Show translated text if available
                        if (hasActiveTranslations) {
                            val translatedText by item.translatedTextFlow.collectAsState()
                            translatedText?.let { translated ->
                                Text(
                                    text = translated,
                                    fontSize = (lyricsTextSize * 0.7f).sp,
                                    color = expressiveAccent.copy(alpha = 0.8f),
                                    textAlign = when (lyricsTextPosition) {
                                        LyricsPosition.LEFT -> TextAlign.Left
                                        LyricsPosition.CENTER -> TextAlign.Center
                                        LyricsPosition.RIGHT -> TextAlign.Right
                                    },
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                                    lineHeight = (lyricsTextSize * 0.7f * lyricsLineSpacing.coerceAtMost(1.3f)).sp
                                )
                            }
                        }
                    }
                }
            }
        }
        // Action buttons are now in the bottom bar
        // Removed the more button from bottom - it's now in the top header
    }
    }

    val actionBottomPadding by animateDpAsState(
        targetValue = 16.dp + extraBottomPadding,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "lyricsActionsBottomPadding"
    )

    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = actionBottomPadding)
            .zIndex(2f)
    ) {
        AnimatedVisibility(
            visible = !isAutoScrollEnabled && isSynced && !isSelectionModeActive,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            FilledTonalButton(onClick = {
                if (lyricsAnimationStyle == LyricsAnimationStyle.METRO_LYRICS) {
                    scrollTargetIndex = currentLineIndex
                } else if (lyricsAnimationStyle != LyricsAnimationStyle.VIVIMUSIC_1) {
                    scope.launch {
                        performSmoothPageScroll(currentLineIndex, 1500)
                    }
                }
                isAutoScrollEnabled = true
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

        AnimatedVisibility(
            visible = isSelectionModeActive,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        isSelectionModeActive = false
                        selectedIndices.clear()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = stringResource(R.string.cancel),
                        modifier = Modifier.size(20.dp)
                    )
                }
                FilledTonalButton(
                    onClick = {
                        if (selectedIndices.isNotEmpty()) {
                            val sortedIndices = selectedIndices.sorted()
                            val selectedLyricsText = sortedIndices
                                .mapNotNull { lines.getOrNull(it)?.text }
                                .joinToString("\n")

                            if (selectedLyricsText.isNotBlank()) {
                                shareDialogData = Triple(
                                    selectedLyricsText,
                                    mediaMetadata?.title ?: "",
                                    mediaMetadata?.artists?.joinToString { it.name } ?: ""
                                )
                                showShareDialog = true
                            }
                            isSelectionModeActive = false
                            selectedIndices.clear()
                        }
                    },
                    enabled = selectedIndices.isNotEmpty()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.share),
                        contentDescription = stringResource(R.string.share_selected),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.share))
                }
            }
        }
    }

    if (showProgressDialog) {
        BasicAlertDialog(onDismissRequest = { /* Don't dismiss */ }) {
            Card( // Use Card for better styling
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.padding(32.dp)) {
                    Text(
                        text = stringResource(R.string.generating_image) + "\n" + stringResource(R.string.please_wait),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    if (showShareDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!! // Renamed 'lyrics' to 'lyricsText' for clarity
        BasicAlertDialog(onDismissRequest = { showShareDialog = false }) {
            Card(
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.85f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.share_lyrics),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Share as Text Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    val songLink =
                                        "https://music.youtube.com/watch?v=${mediaMetadata?.id}"
                                    // Use the potentially multi-line lyricsText here
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "\"$lyricsText\"\n\n$songTitle - $artists\n$songLink"
                                    )
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        context.getString(R.string.share_lyrics)
                                    )
                                )
                                showShareDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.share), // Use new share icon
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.share_as_text),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Share as Image Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Pass the potentially multi-line lyrics to the color picker
                                shareDialogData = Triple(lyricsText, songTitle, artists)
                                showColorPickerDialog = true
                                showShareDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.share), // Use new share icon
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.share_as_image),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Cancel Button Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { showShareDialog = false }
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showColorPickerDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!!
        val coverUrl = mediaMetadata?.thumbnailUrl
        val paletteColors = remember { mutableStateListOf<Color>() }
        
        var previewBackgroundStyle by remember { mutableStateOf(LyricsBackgroundStyle.SOLID) }

        val previewCardWidth = configuration.containerDpSize.width * 0.90f
        val previewPadding = 20.dp * 2
        val previewBoxPadding = 28.dp * 2
        val previewAvailableWidth = previewCardWidth - previewPadding - previewBoxPadding
        val previewBoxHeight = 340.dp
        val headerFooterEstimate = (48.dp + 14.dp + 16.dp + 20.dp + 8.dp + 28.dp * 2)
        val previewAvailableHeight = previewBoxHeight - headerFooterEstimate

        val lyricsTextAlign = when (lyricsTextPosition) {
            LyricsPosition.LEFT -> TextAlign.Left
            LyricsPosition.CENTER -> TextAlign.Center
            LyricsPosition.RIGHT -> TextAlign.Right
        }

        val textStyleForMeasurement = TextStyle(
            color = previewTextColor,
            fontWeight = FontWeight.Bold,
            textAlign = lyricsTextAlign
        )
        val textMeasurer = rememberTextMeasurer()

        rememberAdjustedFontSize(
            text = lyricsText,
            maxWidth = previewAvailableWidth,
            maxHeight = previewAvailableHeight,
            density = density,
            initialFontSize = 50.sp,
            minFontSize = 22.sp,
            style = textStyleForMeasurement,
            textMeasurer = textMeasurer
        )

        LaunchedEffect(coverUrl) {
            if (coverUrl != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val loader = ImageLoader(context)
                        val req = ImageRequest.Builder(context).data(coverUrl).allowHardware(false).build()
                        val result = loader.execute(req)
                        val bmp = result.image?.toBitmap()
                        if (bmp != null) {
                            val palette = Palette.from(bmp).generate()
                            val swatches = palette.swatches.sortedByDescending { it.population }
                            val colors = swatches.map { Color(it.rgb) }
                                .filter { color ->
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
                                    hsv[1] > 0.2f
                                }
                            paletteColors.clear()
                            paletteColors.addAll(colors.take(5))
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        BasicAlertDialog(onDismissRequest = { showColorPickerDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.customize_colors),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = stringResource(id = R.string.player_background_style), style = MaterialTheme.typography.titleMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        LyricsBackgroundStyle.entries.forEach { style ->
                            val label = when(style) {
                                LyricsBackgroundStyle.SOLID -> stringResource(R.string.player_background_solid)
                                LyricsBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                LyricsBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                            }
                            val selected = previewBackgroundStyle == style
                            
                            androidx.compose.material3.FilterChip(
                                selected = selected,
                                onClick = { previewBackgroundStyle = style },
                                label = { Text(label) }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        LyricsImageCard(
                            lyricText = lyricsText,
                            mediaMetadata = mediaMetadata ?: return@Box,
                            backgroundColor = previewBackgroundColor,
                            backgroundStyle = previewBackgroundStyle,
                            textColor = previewTextColor,
                                secondaryTextColor = previewSecondaryTextColor,
                                textAlign = lyricsTextAlign
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(text = stringResource(id = R.string.background_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        (paletteColors + listOf(Color(0xFF242424), Color(0xFF121212), Color.White, Color.Black, Color(0xFFF5F5F5))).distinct().take(8).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, shape = RoundedCornerShape(8.dp))
                                    .clickable { previewBackgroundColor = color }
                                    .border(
                                        2.dp,
                                        if (previewBackgroundColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                            )
                        }
                    }

                    Text(text = stringResource(id = R.string.text_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        (paletteColors + listOf(Color.White, Color.Black, Color(0xFF1DB954))).distinct().take(8).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, shape = RoundedCornerShape(8.dp))
                                    .clickable { previewTextColor = color }
                                    .border(
                                        2.dp,
                                        if (previewTextColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                            )
                        }
                    }

                    Text(text = stringResource(id = R.string.secondary_text_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        (paletteColors.map { it.copy(alpha = 0.7f) } + listOf(Color.White.copy(alpha = 0.7f), Color.Black.copy(alpha = 0.7f), Color(0xFF1DB954))).distinct().take(8).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, shape = RoundedCornerShape(8.dp))
                                    .clickable { previewSecondaryTextColor = color }
                                    .border(
                                        2.dp,
                                        if (previewSecondaryTextColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            showColorPickerDialog = false
                            showProgressDialog = true
                            scope.launch {
                                try {
                                    val screenWidth = configuration.containerSize.width
                                    val screenHeight = configuration.containerSize.height

                                    val image = ComposeToImage.createLyricsImage(
                                        context = context,
                                        coverArtUrl = coverUrl,
                                        songTitle = songTitle,
                                        artistName = artists,
                                        lyrics = lyricsText,
                                        width = (screenWidth * density.density).toInt(),
                                        height = (screenHeight * density.density).toInt(),
                                        backgroundColor = previewBackgroundColor.toArgb(),
                                        backgroundStyle = previewBackgroundStyle,
                                        textColor = previewTextColor.toArgb(),
                                        secondaryTextColor = previewSecondaryTextColor.toArgb(),
                                        lyricsAlignment = when (lyricsTextPosition) {
                                            LyricsPosition.LEFT -> Layout.Alignment.ALIGN_NORMAL
                                            LyricsPosition.CENTER -> Layout.Alignment.ALIGN_CENTER
                                            LyricsPosition.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                                        }
                                    )
                                    val timestamp = System.currentTimeMillis()
                                    val filename = "lyrics_$timestamp"
                                    val uri = ComposeToImage.saveBitmapAsFile(context, image, filename)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_lyrics)))
                                } catch (e: Exception) {
                                    Toast.makeText(context, context.getString(R.string.failed_to_create_image, e.message), Toast.LENGTH_SHORT).show()
                                } finally {
                                    showProgressDialog = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.share))
                    }
                }
            }
        }
        } //else block
    }
}

// Professional page animation constants inspired by vivimusic design - slower for smoothness
private const val vivimusic_AUTO_SCROLL_DURATION = 1500L // Much slower auto-scroll for smooth transitions
private const val vivimusic_INITIAL_SCROLL_DURATION = 1000L // Slower initial positioning
private const val vivimusic_SEEK_DURATION = 800L // Slower user interaction
private const val vivimusic_FAST_SEEK_DURATION = 600L // Less aggressive seeking

// Lyrics constants
val LyricsPreviewTime = 2.seconds

sealed class LyricsListItem {
    data class Line(val index: Int, val entry: LyricsEntry) : LyricsListItem()
    data class Indicator(
        val afterLineIndex: Int,
        val gapMs: Long,
        val gapStartMs: Long,
        val gapEndMs: Long,
        val nextAgent: String?
    ) : LyricsListItem()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun IntervalIndicator(
    gapStartMs: Long,
    gapEndMs: Long,
    currentPositionMs: Long,
    visible: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val alpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val rowHeightPx = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            rowHeightPx.animateTo(1f, tween(200))
            alpha.animateTo(1f, tween(200))
        } else {
            alpha.animateTo(0f, tween(200))
            rowHeightPx.animateTo(0f, tween(200))
        }
    }

    val targetHeightDp = 72.dp

    val progress = if (gapEndMs > gapStartMs) {
        ((currentPositionMs - gapStartMs).toFloat() / (gapEndMs - gapStartMs).toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "intervalProgress"
    )

    Box(
        modifier = modifier
            .height(targetHeightDp * rowHeightPx.value)
            .padding(top = 16.dp * rowHeightPx.value)
            .graphicsLayer {
                this.alpha = alpha.value
                this.clip = true
            },
        contentAlignment = Alignment.Center
    ) {
        CircularWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .size(36.dp)
                .alpha(alpha.value),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
    }
}

// ── Lyrics Animation Metrics & Easing ───────────────────────────────────────────
private val LYRIC_EASING = CubicBezierEasing(0.41f, 0f, 0.12f, 0.99f)
private const val LYRIC_SETTLE_MS = 400
private const val INACTIVE_SCALE = 0.98f
private const val PRESSED_SCALE = 0.96f
private const val GLOW_ALPHA = 0.62f
private val GLOW_ROOM = 10.dp

private const val STAGGER_STEPS = 4
private const val STAGGER_FRACTION = 0.08f

private class ScrollRun(val id: Int, val delta: Float, val durationMs: Int) {
    val spanMs: Float get() = durationMs * (1f + STAGGER_FRACTION * STAGGER_STEPS)
}

private val LINE_FALLOFF_ALPHA_PHONE = floatArrayOf(1f, 0.74f, 0.6f, 0.48f, 0.4f, 0.34f)
private val LINE_FALLOFF_BLUR = arrayOf(0.dp, 1.dp, 1.dp, 1.7.dp, 2.4.dp)

private val SYNCED_FONT_SIZE = 34.sp
private val SYNCED_LINE_HEIGHT = 41.sp
private val UNSYNCED_FONT_SIZE = 30.sp
private val UNSYNCED_LINE_HEIGHT = 38.sp

private val BACKING_FONT_SIZE = 23.sp
private val BACKING_LINE_HEIGHT = 29.sp
private const val BACKING_ALPHA = 0.72f

private val SUB_LYRIC_FONT_SIZE = 20.sp
private val SUB_LYRIC_LINE_HEIGHT = 25.sp
private const val SUB_LYRIC_ALPHA = 0.85f
private val SUB_LYRIC_TUCK = 6.dp

private const val SCROLL_LEAD_MIN_MS = 350L
private const val SCROLL_LEAD_MAX_MS = 500L

private fun activeLyricRows(lines: List<LyricsEntry>, positionMs: Long): List<Int> {
    val latest = lines.indexOfLast { it.time <= positionMs }
    if (latest < 0) return emptyList()
    return (0..latest).filter { index ->
        val line = lines[index]
        val endMs = if (!line.words.isNullOrEmpty()) {
            (line.words.last().endTime * 1000).toLong()
        } else {
            lines.getOrNull(index + 1)?.time ?: (line.time + 4000L)
        }
        index == latest || (!line.isInstrumental && line.time <= positionMs && positionMs < endMs)
    }
}

private fun scrollLead(lines: List<LyricsEntry>, positionMs: Long): Long {
    val current = lines.indexOfLast { it.time <= positionMs }
    if (current < 0) return SCROLL_LEAD_MIN_MS
    val next = lines.getOrNull(current + 1) ?: return SCROLL_LEAD_MIN_MS
    val currentEnd = if (!lines[current].words.isNullOrEmpty()) {
        (lines[current].words!!.last().endTime * 1000).toLong()
    } else {
        lines[current].time + 3000L
    }
    val gap = next.time - currentEnd
    return gap.coerceIn(SCROLL_LEAD_MIN_MS, SCROLL_LEAD_MAX_MS)
}

// ── Lyrics Canvas Animation Helpers ───────────────────────────────────────────

private fun ContentDrawScope.glowGrown(
    layout: TextLayoutResult,
    line: ViviLyricLine,
    positionMs: Long,
    inset: Float,
    peak: Float,
    growth: CharGrowth,
) {
    if (!line.isGrowing(positionMs)) return
    val em = layout.layoutInput.style.fontSize.toPx()
    val length = layout.layoutInput.text.length
    for (word in line.growingWords) {
        if (positionMs < word.startMs || positionMs > word.restsAtMs) continue
        val span = line.wordSpans.getOrNull(word.index) ?: continue
        val fall = line.wordFall(word.index, positionMs)
        for (char in span.first..minOf(span.last, length - 1)) {
            word.sampleInto(char - span.first, positionMs, growth)
            if (growth.bloom <= 0.01f) continue
            val visualLine = layout.getLineForOffset(char)
            val from = layout.xOn(char, visualLine, inset)
            val to = layout.xOn(char + 1, visualLine, inset)
            if (to <= from) continue
            val dx = growth.shift * em
            val dy = -growth.rise * peak * fall
            val rowTop = layout.getLineTop(visualLine) + inset
            val bottom = layout.getLineBottom(visualLine) + inset
            val overhang = (to - from) * (growth.scale - 1f) / 2f
            clipRect(
                left = from - overhang + dx,
                top = rowTop - peak * GROW_HEADROOM,
                right = to + overhang + dx,
                bottom = bottom,
            ) {
                translate(left = dx, top = dy) {
                    scale(
                        growth.scale,
                        growth.scale,
                        Offset((from + to) / 2f, (rowTop + bottom) / 2f),
                    ) {
                        this@glowGrown.drawContent()
                    }
                }
                drawRect(
                    color = Color.White.copy(alpha = growth.bloom),
                    blendMode = BlendMode.DstIn,
                )
            }
        }
    }
}

private fun ContentDrawScope.riseWith(
    layout: TextLayoutResult,
    line: ViviLyricLine,
    positionMs: Long,
    inset: Float,
    peak: Float,
    growth: CharGrowth,
) {
    if (!line.isLifted(positionMs)) {
        drawContent()
        return
    }
    val em = layout.layoutInput.style.fontSize.toPx()
    for (visualLine in 0 until layout.lineCount) {
        val lineStart = layout.getLineStart(visualLine)
        val lineEnd = layout.getLineEnd(visualLine, visibleEnd = true)
        val top = layout.getLineTop(visualLine) + inset
        val bottom = layout.getLineBottom(visualLine) + inset
        var at = lineStart
        var edge = layout.getLineLeft(visualLine) + inset
        for (index in line.words.indices) {
            val span = line.wordSpans.getOrNull(index) ?: continue
            val start = maxOf(span.first, lineStart)
            val end = minOf(span.last + 1, lineEnd)
            if (start >= end) continue
            val held = line.growingAt(index)?.takeIf { positionMs in it.startMs..it.restsAtMs }
            val lift = line.wordLift(index, positionMs)
            if (held == null && lift <= 0.01f) continue
            val from = layout.xOn(start, visualLine, inset)
            val to = layout.xOn(end, visualLine, inset)
            if (to <= from) continue
            if (start > at) sliceRisen(edge, top, from, bottom, 0f)
            if (held != null) {
                growEach(
                    layout, held, line, positionMs, visualLine,
                    start, end, top, bottom, inset, peak, em, growth,
                )
            } else {
                sliceRisen(from, top - peak, to, bottom, -lift * peak)
            }
            at = end
            edge = to
        }
        if (at < lineEnd) {
            sliceRisen(edge, top, layout.getLineRight(visualLine) + inset, bottom, 0f)
        }
    }
}

private fun ContentDrawScope.growEach(
    layout: TextLayoutResult,
    word: GrowingWord,
    line: ViviLyricLine,
    positionMs: Long,
    visualLine: Int,
    start: Int,
    end: Int,
    top: Float,
    bottom: Float,
    inset: Float,
    peak: Float,
    em: Float,
    growth: CharGrowth,
) {
    val fall = line.wordFall(word.index, positionMs)
    val first = line.wordSpans.getOrNull(word.index)?.first ?: 0
    val ceiling = top - peak * GROW_HEADROOM
    val middle = (top + bottom) / 2f
    for (char in start until end) {
        word.sampleInto(char - first, positionMs, growth)
        val from = layout.xOn(char, visualLine, inset)
        val to = layout.xOn(char + 1, visualLine, inset)
        if (to <= from) continue
        val dx = growth.shift * em
        val dy = -growth.rise * peak * fall
        val overhang = (to - from) * (growth.scale - 1f) / 2f
        clipShiftedDown(
            left = from - overhang + dx,
            top = ceiling,
            right = to + overhang + dx,
            bottom = bottom,
            dy = dy,
        ) {
            translate(left = dx) {
                scale(growth.scale, growth.scale, Offset((from + to) / 2f, middle)) {
                    this@growEach.drawContent()
                }
            }
        }
    }
}

private fun TextLayoutResult.xOn(offset: Int, visualLine: Int, inset: Float): Float {
    val lineStart = getLineStart(visualLine)
    val lineEndVisible = getLineEnd(visualLine, visibleEnd = true)
    val lineLeft = getLineLeft(visualLine) + inset
    val lineRight = getLineRight(visualLine) + inset

    val rtl = getParagraphDirection(lineStart.coerceIn(0, (layoutInput.text.length - 1).coerceAtLeast(0))) ==
        ResolvedTextDirection.Rtl

    return when {
        offset <= lineStart -> if (rtl) lineRight else lineLeft
        offset >= lineEndVisible -> if (rtl) lineLeft else lineRight
        else -> (getHorizontalPosition(offset, usePrimaryDirection = true) + inset)
            .coerceIn(lineLeft, lineRight)
    }
}

private fun ContentDrawScope.sliceRisen(
    from: Float,
    top: Float,
    to: Float,
    bottom: Float,
    dy: Float,
) {
    if (to <= from) return
    clipShiftedDown(left = from, top = top, right = to, bottom = bottom, dy = dy) {
        this@sliceRisen.drawContent()
    }
}

private fun DrawScope.clipShiftedDown(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    dy: Float,
    block: DrawScope.() -> Unit,
) {
    clipRect(left = left, top = top, right = right, bottom = bottom) {
        translate(top = dy) { block() }
    }
}

private fun horizontalAt(
    layout: TextLayoutResult,
    chars: Float,
    visualLine: Int,
    spans: List<IntRange>,
): Float {
    val lineStart = layout.getLineStart(visualLine)
    val lineEndVisible = layout.getLineEnd(visualLine, visibleEnd = true)
    val lineEndAll = layout.getLineEnd(visualLine, visibleEnd = false)
    val word = spans.firstOrNull { chars >= it.first && chars < it.last + 1 }
    if (word != null && word.first >= lineStart && word.last + 1 <= lineEndVisible) {
        val from = layout.xOn(word.first, visualLine, 0f)
        val to = layout.xOn(word.last + 1, visualLine, 0f)
        val spanWidth = (word.last + 1 - word.first).coerceAtLeast(1)
        return from + (to - from) * (chars - word.first) / spanWidth
    }
    val index = chars.toInt().coerceIn(lineStart, lineEndAll)
    val here = layout.xOn(index, visualLine, 0f)
    val rawNext = index + 1
    val textLen = layout.layoutInput.text.length
    val spills: Boolean = layout.getLineForOffset(rawNext.coerceIn(0, textLen)) != visualLine
    val next: Float = if (spills) {
        if (rawNext > lineEndVisible) {
            val rtl = layout.getParagraphDirection(
                visualLine.coerceAtMost(lineEndAll).coerceIn(0, (textLen - 1).coerceAtLeast(0))
            ) == ResolvedTextDirection.Rtl
            val farLeft = layout.getLineLeft(visualLine)
            val farRight = layout.getLineRight(visualLine)
            if (rtl) farLeft else farRight
        } else {
            layout.xOn(rawNext.coerceAtMost(lineEndVisible), visualLine, 0f)
        }
    } else {
        layout.xOn(rawNext, visualLine, 0f)
    }
    return here + (next - here) * (chars - index)
}

private fun ContentDrawScope.sweepTo(
    layout: TextLayoutResult,
    revealedChars: Float,
    spans: List<IntRange>,
    feather: Boolean,
) {
    if (revealedChars <= 0f) return
    val textLen = layout.layoutInput.text.length
    if (revealedChars >= textLen) {
        drawContent()
        return
    }
    val offset = layout.getLineStart(0).coerceIn(0, (textLen - 1).coerceAtLeast(0))
    val rtl = layout.getParagraphDirection(offset) == ResolvedTextDirection.Rtl

    for (visualLine in 0 until layout.lineCount) {
        val lineStart = layout.getLineStart(visualLine)
        val lineEndAll = layout.getLineEnd(visualLine, visibleEnd = false)

        if (revealedChars <= lineStart) return

        val cut = revealedChars < lineEndAll
        val charPos = if (cut) {
            horizontalAt(layout, revealedChars, visualLine, spans)
        } else {
            if (rtl) layout.getLineLeft(visualLine) else layout.getLineRight(visualLine)
        }

        val lineLeft = layout.getLineLeft(visualLine)
        val lineRight = layout.getLineRight(visualLine)
        val safeCharPos = charPos.coerceIn(lineLeft, lineRight)

        val featherPx = if (feather && cut) {
            minOf(
                WIPE_FEATHER.toPx(),
                if (rtl) lineRight - safeCharPos else safeCharPos - lineLeft,
            )
        } else {
            0f
        }
        val lead = if (rtl) {
            (safeCharPos - featherPx).coerceAtLeast(lineLeft)
        } else {
            (safeCharPos + featherPx).coerceAtMost(lineRight)
        }

        val (clipLeft, clipRight) = if (rtl) {
            lead to lineRight
        } else {
            lineLeft to lead
        }

        if (clipLeft >= clipRight) continue

        val top = layout.getLineTop(visualLine)
        val bottom = layout.getLineBottom(visualLine)
        clipRect(
            left = clipLeft,
            top = top,
            right = clipRight,
            bottom = bottom,
        ) {
            this@sweepTo.drawContent()
        }

        if (featherPx < 0.5f) continue

        clipRect(top = top, bottom = bottom) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.White,
                    1f to Color.Transparent,
                    startX = safeCharPos,
                    endX = if (rtl) safeCharPos - featherPx else safeCharPos + featherPx,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
    }
}
