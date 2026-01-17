package com.music.vivi.ui.component

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.LyricsClickKey
import com.music.vivi.constants.LyricsRomanizeBelarusianKey
import com.music.vivi.constants.LyricsRomanizeBulgarianKey
import com.music.vivi.constants.LyricsRomanizeCyrillicByLineKey
import com.music.vivi.constants.LyricsRomanizeDevanagariKey
import com.music.vivi.constants.LyricsRomanizeJapaneseKey
import com.music.vivi.constants.LyricsRomanizeKoreanKey
import com.music.vivi.constants.LyricsRomanizeKyrgyzKey
import com.music.vivi.constants.LyricsRomanizeRussianKey
import com.music.vivi.constants.LyricsRomanizeSerbianKey
import com.music.vivi.constants.LyricsRomanizeUkrainianKey
import com.music.vivi.constants.LyricsRomanizeMacedonianKey
import com.music.vivi.constants.LyricsScrollKey
import com.music.vivi.constants.LyricsTextPositionKey
import com.music.vivi.constants.LyricsTextSizeKey
import com.music.vivi.constants.LyricsLineSpacingKey
import com.music.vivi.constants.LyricsWordForWordKey
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
import com.music.vivi.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.music.vivi.lyrics.LyricsEntry
import com.music.vivi.lyrics.LyricsUtils.findCurrentLineIndex
import com.music.vivi.lyrics.LyricsUtils.isBelarusian
import com.music.vivi.lyrics.LyricsUtils.isChinese
import com.music.vivi.lyrics.LyricsUtils.isJapanese
import com.music.vivi.lyrics.LyricsUtils.isKorean
import com.music.vivi.lyrics.LyricsUtils.isKyrgyz
import com.music.vivi.lyrics.LyricsUtils.isRussian
import com.music.vivi.lyrics.LyricsUtils.isSerbian
import com.music.vivi.lyrics.LyricsUtils.isBulgarian
import com.music.vivi.lyrics.LyricsUtils.isDevanagari
import com.music.vivi.lyrics.LyricsUtils.isUkrainian
import com.music.vivi.lyrics.LyricsUtils.isMacedonian
import com.music.vivi.lyrics.LyricsUtils.parseLyrics
import com.music.vivi.lyrics.LyricsUtils.romanizeCyrillic
import com.music.vivi.lyrics.LyricsUtils.romanizeDevanagari
import com.music.vivi.lyrics.LyricsUtils.romanizeJapanese
import com.music.vivi.lyrics.LyricsUtils.romanizeKorean

import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.ui.screens.settings.LyricsPosition
import com.music.vivi.ui.screens.settings.LyricsVerticalPosition
import com.music.vivi.ui.utils.fadingEdge
import com.music.vivi.utils.ComposeToImage
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope", "StringFormatInvalid")
@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    playingPosition: Long? = null
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current // Get configuration

    val landscapeOffset =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val (lyricsTextPosition, onLyricsPositionChange) = rememberEnumPreference(
        LyricsTextPositionKey,
        defaultValue = LyricsPosition.LEFT
    )
    val (lyricsVerticalPosition) = rememberEnumPreference(
        com.music.vivi.constants.LyricsVerticalPositionKey,
        defaultValue = LyricsVerticalPosition.TOP
    )

    val (lyricsTextSize) = rememberPreference(
        LyricsTextSizeKey,
        defaultValue = 28f
    )
    val (lyricsLineSpacing) = rememberPreference(
        LyricsLineSpacingKey,
        defaultValue = 6f
    )
    val lyricsWordForWord by rememberPreference(LyricsWordForWordKey, true)
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
    val romanizeDevanagariLyrics by rememberPreference(LyricsRomanizeDevanagariKey, true)
    val scope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val lyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.GRADIENT
    )

    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val lines = remember(lyrics, scope) {
        if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
            emptyList()
        } else if (lyrics.startsWith("[")) {
            val parsedLines = parseLyrics(lyrics)

            val isRussianLyrics = romanizeRussianLyrics && !romanizeCyrillicByLine && isRussian(lyrics)
            val isUkrainianLyrics = romanizeUkrainianLyrics && !romanizeCyrillicByLine && isUkrainian(lyrics)
            val isSerbianLyrics = romanizeSerbianLyrics && !romanizeCyrillicByLine && isSerbian(lyrics)
            val isBulgarianLyrics = romanizeBulgarianLyrics && !romanizeCyrillicByLine && isBulgarian(lyrics)
            val isBelarusianLyrics = romanizeBelarusianLyrics && !romanizeCyrillicByLine && isBelarusian(lyrics)
            val isKyrgyzLyrics = romanizeKyrgyzLyrics && !romanizeCyrillicByLine && isKyrgyz(lyrics)
            val isMacedonianLyrics = romanizeMacedonianLyrics && !romanizeCyrillicByLine && isMacedonian(lyrics)

            parsedLines.map { entry ->
                val newEntry = entry.copy()

                if (romanizeJapaneseLyrics && isJapanese(entry.text) && !isChinese(entry.text)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeJapanese(entry.text)
                    }
                }

                if (romanizeKoreanLyrics && isKorean(entry.text)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeKorean(entry.text)
                    }
                }

                if (romanizeDevanagariLyrics && isDevanagari(entry.text)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeDevanagari(entry.text)
                    }
                }

                if (romanizeRussianLyrics && (if (romanizeCyrillicByLine) isRussian(entry.text) else isRussianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeUkrainianLyrics && (if (romanizeCyrillicByLine) isUkrainian(entry.text) else isUkrainianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeSerbianLyrics && (if (romanizeCyrillicByLine) isSerbian(entry.text) else isSerbianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeBulgarianLyrics && (if (romanizeCyrillicByLine) isBulgarian(entry.text) else isBulgarianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeBelarusianLyrics && (if (romanizeCyrillicByLine) isBelarusian(entry.text) else isBelarusianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeKyrgyzLyrics && (if (romanizeCyrillicByLine) isKyrgyz(entry.text) else isKyrgyzLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                else if (romanizeMacedonianLyrics && (if (romanizeCyrillicByLine) isMacedonian(entry.text) else isMacedonianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(entry.text)
                    }
                }

                newEntry
            }.let {
                listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + it
            }
        } else {
            val isRussianLyrics = romanizeRussianLyrics && !romanizeCyrillicByLine && isRussian(lyrics)
            val isUkrainianLyrics = romanizeUkrainianLyrics && !romanizeCyrillicByLine && isUkrainian(lyrics)
            val isSerbianLyrics = romanizeSerbianLyrics && !romanizeCyrillicByLine && isSerbian(lyrics)
            val isBulgarianLyrics = romanizeBulgarianLyrics && !romanizeCyrillicByLine && isBulgarian(lyrics)
            val isBelarusianLyrics = romanizeBelarusianLyrics && !romanizeCyrillicByLine && isBelarusian(lyrics)
            val isKyrgyzLyrics = romanizeKyrgyzLyrics && !romanizeCyrillicByLine && isKyrgyz(lyrics)
            val isMacedonianLyrics = romanizeMacedonianLyrics && !romanizeCyrillicByLine && isMacedonian(lyrics)

            lyrics.lines().mapIndexed { index, line ->
                val newEntry = LyricsEntry(index * 100L, line, null)

                if (romanizeJapaneseLyrics && isJapanese(line) && !isChinese(line)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeJapanese(line)
                    }
                }

                if (romanizeKoreanLyrics && isKorean(line)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeKorean(line)
                    }
                }

                if (romanizeRussianLyrics && (if (romanizeCyrillicByLine) isRussian(line) else isRussianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeUkrainianLyrics && (if (romanizeCyrillicByLine) isUkrainian(line) else isUkrainianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeSerbianLyrics && (if (romanizeCyrillicByLine) isSerbian(line) else isSerbianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeBulgarianLyrics && (if (romanizeCyrillicByLine) isBulgarian(line) else isBulgarianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeBelarusianLyrics && (if (romanizeCyrillicByLine) isBelarusian(line) else isBelarusianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeKyrgyzLyrics && (if (romanizeCyrillicByLine) isKyrgyz(line) else isKyrgyzLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                else if (romanizeMacedonianLyrics && (if (romanizeCyrillicByLine) isMacedonian(line) else isMacedonianLyrics)) {
                    scope.launch {
                        newEntry.romanizedTextFlow.value = romanizeCyrillic(line)
                    }
                }

                newEntry
            }
        }
    }
    val isSynced =
        remember(lyrics) {
            !lyrics.isNullOrEmpty() && lyrics.startsWith("[")
        }

    val textColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else ->
            if (useDarkTheme)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onPrimary
    }

    var currentLineIndex by remember {
        mutableIntStateOf(-1)
    }

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

    var currentTime by remember {
        mutableLongStateOf(0L)
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

    val lazyListState = rememberLazyListState()

    // Professional animation states for smooth -vivi-style transitions
    var isAnimating by remember { mutableStateOf(false) }
    var isAutoScrollActive by rememberSaveable { mutableStateOf(true) }

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
    LaunchedEffect(lines) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    // Use rememberUpdatedState to ensure the latest playingPosition is used inside the loop
    val currentPlayingPosition by androidx.compose.runtime.rememberUpdatedState(playingPosition)

    LaunchedEffect(lyrics) {
        if (lyrics.isNullOrEmpty() || !lyrics.startsWith("[")) {
            currentLineIndex = -1
            return@LaunchedEffect
        }
        while (isActive) {
            val sliderPosition = sliderPositionProvider()
            isSeeking = sliderPosition != null
            // Prioritize slider, then passed playing position, then poll player
            currentTime = sliderPosition ?: currentPlayingPosition ?: playerConnection.player.currentPosition
            currentLineIndex = findCurrentLineIndex(
                lines,
                currentTime
            )
            delay(16)
        }
    }

    LaunchedEffect(isSeeking, lastPreviewTime) {
        if (isSeeking) {
            lastPreviewTime = 0L
        } else if (lastPreviewTime != 0L) {
            delay(LyricsPreviewTime)
            lastPreviewTime = 0L
        }
    }

    // Apple Music style scrolling - keep active line at top of screen with smooth movement
    suspend fun performSmoothTopScroll(targetIndex: Int, duration: Int = 1000) {
        if (isAnimating || targetIndex < 0) return

        isAnimating = true

        try {
            val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
            val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset

            if (itemInfo != null) {
                val targetTopPosition = if (lyricsVerticalPosition == LyricsVerticalPosition.CENTER) {
                    lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2) - (itemInfo.size / 2)
                } else {
                    lazyListState.layoutInfo.viewportStartOffset + 100
                }

                val currentItemTop = itemInfo.offset
                val scrollOffset = currentItemTop - targetTopPosition

                if (kotlin.math.abs(scrollOffset) > 10) {
                    lazyListState.animateScrollBy(
                        value = scrollOffset.toFloat(),
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            } else {
                // Item not visible, scroll to it first then adjust position
                lazyListState.scrollToItem(index = targetIndex)
                
                // Now get the item info after scrolling
                val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
                if (itemInfo != null) {
                    val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                    val targetTopPosition = if (lyricsVerticalPosition == LyricsVerticalPosition.CENTER) {
                        lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2) - (itemInfo.size / 2)
                    } else {
                        lazyListState.layoutInfo.viewportStartOffset + 100
                    }
                    
                    val currentItemTop = itemInfo.offset
                    val scrollOffset = currentItemTop - targetTopPosition
                    
                    if (kotlin.math.abs(scrollOffset) > 10) {
                        lazyListState.animateScrollBy(
                            value = scrollOffset.toFloat(),
                            animationSpec = tween(
                                durationMillis = duration,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }
                }
            }
        } finally {
            isAnimating = false
        }
    }

    LaunchedEffect(currentLineIndex, lastPreviewTime, initialScrollDone) {
        if (!isSynced) return@LaunchedEffect

        if((currentLineIndex == 0 && shouldScrollToFirstLine) || !initialScrollDone) {
            shouldScrollToFirstLine = false
            performSmoothTopScroll(kotlin.math.max(0, currentLineIndex), 800)
            if(!isAppMinimized) {
                initialScrollDone = true
            }
        } else if (currentLineIndex != -1) {
            deferredCurrentLineIndex = currentLineIndex
            if (isSeeking) {
                performSmoothTopScroll(currentLineIndex, 400) // Fast for seeking
            } else if (isAutoScrollActive && scrollLyrics) {
                if (currentLineIndex != previousLineIndex) {
                    performSmoothTopScroll(currentLineIndex, 1200) // Smooth auto-scroll
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
        } else {
            LazyColumn(
                state = lazyListState,
                contentPadding = WindowInsets.systemBars
                    .only(WindowInsetsSides.Top)
                    .add(WindowInsets(top = maxHeight / 3, bottom = maxHeight / 2))
                    .asPaddingValues(),
                modifier = Modifier
                    .fadingEdge(vertical = 64.dp)
                    .nestedScroll(remember {
                        object : NestedScrollConnection {
                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                if (!isSelectionModeActive && (consumed.y != 0f || available.y != 0f)) {
                                    isAutoScrollActive = false
                                }
                                return super.onPostScroll(consumed, available, source)
                            }

                            override suspend fun onPostFling(
                                consumed: Velocity,
                                available: Velocity
                            ): Velocity {
                                if (!isSelectionModeActive) { // Only update preview time if not selecting
                                    isAutoScrollActive = false
                                }
                                return super.onPostFling(consumed, available)
                            }
                        }
                    })
            ) {
                val displayedCurrentLineIndex =
                    if (isSeeking || isSelectionModeActive) deferredCurrentLineIndex else currentLineIndex

                if (lyrics == null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            ContainedLoadingIndicator()
                        }
                    }
                } else {
                    itemsIndexed(
                        items = lines,
                        key = { index, item -> "$index-${item.time}" }
                    ) { index, item ->
                        val isSelected = selectedIndices.contains(index)
                        val nextEntryTime = lines.getOrNull(index + 1)?.time

                        LyricsLine(
                            entry = item,
                            nextEntryTime = nextEntryTime,
                            currentTime = currentTime,
                            isSynced = isSynced,
                            isActive = index == displayedCurrentLineIndex,
                            distanceFromCurrent = kotlin.math.abs(index - displayedCurrentLineIndex),
                            lyricsTextPosition = lyricsTextPosition,
                            textColor = textColor,
                            showRomanized = currentSong?.romanizeLyrics == true && (
                                    romanizeJapaneseLyrics ||
                                            romanizeKoreanLyrics ||
                                            romanizeRussianLyrics ||
                                            romanizeUkrainianLyrics ||
                                            romanizeSerbianLyrics ||
                                            romanizeBulgarianLyrics ||
                                            romanizeBelarusianLyrics ||
                                            romanizeKyrgyzLyrics ||
                                            romanizeMacedonianLyrics),
                            textSize = lyricsTextSize,
                            lineSpacing = lyricsLineSpacing,
                            isWordForWord = lyricsWordForWord,
                            isScrolling = lazyListState.isScrollInProgress,
                            onClick = {
                                if (isSelectionModeActive) {
                                    if (isSelected) {
                                        selectedIndices.remove(index)
                                        if (selectedIndices.isEmpty()) {
                                            isSelectionModeActive = false
                                        }
                                    } else {
                                        if (selectedIndices.size < maxSelectionLimit) {
                                            selectedIndices.add(index)
                                        } else {
                                            showMaxSelectionToast = true
                                        }
                                    }
                                } else if (isSynced && changeLyrics) {
                                    playerConnection.player.seekTo(item.time)
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
                            },
                            isSelected = isSelected,
                            isSelectionModeActive = isSelectionModeActive
                        )
                    }
                }
            }



            // Action buttons: Close and Share buttons grouped together
            if (isSelectionModeActive) {
                mediaMetadata?.let { metadata ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp), // Just above player slider
                        contentAlignment = Alignment.Center
                    ) {
                        // Row containing both close and share buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Close button (circular, right side of share)
                            Box(
                                modifier = Modifier
                                    .size(48.dp) // Larger for better touch target
                                    .background(
                                        color = Color.Black.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        isSelectionModeActive = false
                                        selectedIndices.clear()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.close),
                                    contentDescription = stringResource(R.string.cancel),
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Share button (rectangular with text)
                            Row(
                                modifier = Modifier
                                    .background(
                                        color = if (selectedIndices.isNotEmpty())
                                            Color.White.copy(alpha = 0.9f) // White background when active
                                        else
                                            Color.White.copy(alpha = 0.5f), // Lighter white when inactive
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .clickable(enabled = selectedIndices.isNotEmpty()) {
                                        if (selectedIndices.isNotEmpty()) {
                                            val sortedIndices = selectedIndices.sorted()
                                            val selectedLyricsText = sortedIndices
                                                .mapNotNull { lines.getOrNull(it)?.text }
                                                .joinToString("\n")

                                            if (selectedLyricsText.isNotBlank()) {
                                                shareDialogData = Triple(
                                                    selectedLyricsText,
                                                    metadata.title,
                                                    metadata.artists.joinToString { it.name }
                                                )
                                                showShareDialog = true
                                            }
                                            isSelectionModeActive = false
                                            selectedIndices.clear()
                                        }
                                    }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.share),
                                    contentDescription = stringResource(R.string.share_selected),
                                    tint = Color.Black, // Black icon on white background
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.share),
                                    color = Color.Black, // Black text on white background
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            // Removed the more button from bottom - it's now in the top header
        }
        
        androidx.compose.animation.AnimatedVisibility(
            visible = !isAutoScrollActive && isSynced,
            enter = fadeIn(animationSpec = tween(300)) + androidx.compose.animation.scaleIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + androidx.compose.animation.scaleOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        isAutoScrollActive = true
                        if (currentLineIndex != -1) {
                            scope.launch {
                                // smooth scroll to current line
                                performSmoothTopScroll(currentLineIndex, 800)
                            }
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.sync),
                        contentDescription = stringResource(R.string.action_resync),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_resync),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
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

            val previewCardWidth = configuration.screenWidthDp.dp * 0.90f
            val previewPadding = 20.dp * 2
            val previewBoxPadding = 28.dp * 2
            val previewAvailableWidth = previewCardWidth - previewPadding - previewBoxPadding
            val previewBoxHeight = 340.dp
            val headerFooterEstimate = (48.dp + 14.dp + 16.dp + 20.dp + 8.dp + 28.dp * 2)
            val previewAvailableHeight = previewBoxHeight - headerFooterEstimate

            val textStyleForMeasurement = TextStyle(
                color = previewTextColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
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
                            val req = ImageRequest.Builder(context).data(coverUrl).build()
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

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                                .padding(8.dp)
                        ) {
                            LyricsImageCard(
                                lyricText = lyricsText,
                                mediaMetadata = mediaMetadata ?: return@Box,
                                backgroundColor = previewBackgroundColor,
                                textColor = previewTextColor,
                                secondaryTextColor = previewSecondaryTextColor
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
                                        val screenWidth = configuration.screenWidthDp
                                        val screenHeight = configuration.screenHeightDp

                                        val image = ComposeToImage.createLyricsImage(
                                            context = context,
                                            coverArtUrl = coverUrl,
                                            songTitle = songTitle,
                                            artistName = artists,
                                            lyrics = lyricsText,
                                            width = (screenWidth * density.density).toInt(),
                                            height = (screenHeight * density.density).toInt(),
                                            backgroundColor = previewBackgroundColor.toArgb(),
                                            textColor = previewTextColor.toArgb(),
                                            secondaryTextColor = previewSecondaryTextColor.toArgb(),
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
                                        Toast.makeText(context, context.getString(R.string.failed_to_create_image, e.message ?: ""), Toast.LENGTH_SHORT).show()
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
        }
    }
}

// Lyrics constants
val LyricsPreviewTime = 2.seconds
