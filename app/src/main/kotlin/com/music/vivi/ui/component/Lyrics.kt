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
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import coil3.toBitmap
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.LyricsClickKey
import com.music.vivi.constants.LyricsLineSpacingKey
import com.music.vivi.constants.LyricsRomanizeBelarusianKey
import com.music.vivi.constants.LyricsRomanizeBulgarianKey
import com.music.vivi.constants.LyricsRomanizeCyrillicByLineKey
import com.music.vivi.constants.LyricsRomanizeDevanagariKey
import com.music.vivi.constants.LyricsRomanizeJapaneseKey
import com.music.vivi.constants.LyricsRomanizeKoreanKey
import com.music.vivi.constants.LyricsRomanizeKyrgyzKey
import com.music.vivi.constants.LyricsRomanizeMacedonianKey
import com.music.vivi.constants.LyricsRomanizeRussianKey
import com.music.vivi.constants.LyricsRomanizeSerbianKey
import com.music.vivi.constants.LyricsRomanizeUkrainianKey
import com.music.vivi.constants.LyricsScrollKey
import com.music.vivi.constants.LyricsTextPositionKey
import com.music.vivi.constants.LyricsTextSizeKey
import com.music.vivi.constants.LyricsWordForWordKey
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
import com.music.vivi.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.music.vivi.lyrics.LyricsParser
import com.music.vivi.lyrics.LyricsUtils.findCurrentLineIndex
import com.music.vivi.ui.component.lyrics.LyricsActionButtons
import com.music.vivi.ui.component.lyrics.LyricsColorPickerDialog
import com.music.vivi.ui.component.lyrics.LyricsShareDialog
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

/**
 * The main Lyrics screen component.
 * Displays synced or plain lyrics with auto-scrolling, translation/romanization support, and sharing features.
 */
@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope", "StringFormatInvalid")
@Composable
public fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    playingPosition: Long? = null,
    showResyncButton: Boolean = true,
    resyncTrigger: Int = 0,
    onAutoScrollChange: (Boolean) -> Unit = {},
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

    val options = remember(
        romanizeJapaneseLyrics, romanizeKoreanLyrics, romanizeRussianLyrics,
        romanizeUkrainianLyrics, romanizeSerbianLyrics, romanizeBulgarianLyrics,
        romanizeBelarusianLyrics, romanizeKyrgyzLyrics, romanizeMacedonianLyrics,
        romanizeDevanagariLyrics, romanizeCyrillicByLine
    ) {
        LyricsParser.RomanizationOptions(
            japanese = romanizeJapaneseLyrics,
            korean = romanizeKoreanLyrics,
            russian = romanizeRussianLyrics,
            ukrainian = romanizeUkrainianLyrics,
            serbian = romanizeSerbianLyrics,
            bulgarian = romanizeBulgarianLyrics,
            belarusian = romanizeBelarusianLyrics,
            kyrgyz = romanizeKyrgyzLyrics,
            macedonian = romanizeMacedonianLyrics,
            devanagari = romanizeDevanagariLyrics,
            cyrillicByLine = romanizeCyrillicByLine
        )
    }

    val lines = remember(lyrics, scope, options) {
        LyricsParser.parse(lyrics, scope, options)
    }
    val isSynced =
        remember(lyrics) {
            !lyrics.isNullOrEmpty() && lyrics.startsWith("[")
        }

    val textColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else ->
            if (useDarkTheme) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onPrimary
            }
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
    var isAutoScrollActive by remember { mutableStateOf(true) }

    LaunchedEffect(isAutoScrollActive) {
        onAutoScrollChange(isAutoScrollActive)
    }

    // Apple Music style scrolling - keep active line at preferred position with smooth movement
    suspend fun performSmoothTopScroll(targetIndex: Int, duration: Int = 1000) {
        if (isAnimating || targetIndex < 0) return
        isAnimating = true
        try {
            val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
            val viewportHeight =
                lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset

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
                        animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
                    )
                }
            } else {
                // Item not visible, scroll to it first then adjust position
                lazyListState.scrollToItem(index = targetIndex)

                // Now get the item info after scrolling
                val newItemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
                if (newItemInfo != null) {
                    val targetTopPosition = if (lyricsVerticalPosition == LyricsVerticalPosition.CENTER) {
                        lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2) - (newItemInfo.size / 2)
                    } else {
                        lazyListState.layoutInfo.viewportStartOffset + 100
                    }

                    val currentItemTop = newItemInfo.offset
                    val scrollOffset = currentItemTop - targetTopPosition

                    if (kotlin.math.abs(scrollOffset) > 10) {
                        lazyListState.animateScrollBy(
                            value = scrollOffset.toFloat(),
                            animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
                        )
                    }
                }
            }
        } finally {
            isAnimating = false
        }
    }

    // Handle external resync trigger
    LaunchedEffect(resyncTrigger) {
        if (resyncTrigger > 0) {
            isAutoScrollActive = true
            if (currentLineIndex != -1) {
                performSmoothTopScroll(currentLineIndex, 1000)
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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val visibleItemsInfo = lazyListState.layoutInfo.visibleItemsInfo
                val isCurrentLineVisible = visibleItemsInfo.any { it.index == currentLineIndex }
                if (isCurrentLineVisible) {
                    initialScrollDone = false
                }
                isAppMinimized = true
            } else if (event == Lifecycle.Event.ON_START) {
                isAppMinimized = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Reset selection mode and auto-scroll if lyrics change
    LaunchedEffect(lines) {
        isSelectionModeActive = false
        selectedIndices.clear()
        isAutoScrollActive = true
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

    LaunchedEffect(currentLineIndex, lastPreviewTime, initialScrollDone, isAutoScrollActive) {
        if (!isSynced) return@LaunchedEffect

        if (currentLineIndex != -1) {
            deferredCurrentLineIndex = currentLineIndex
        }

        if (isAutoScrollActive) {
            if ((currentLineIndex == 0 && shouldScrollToFirstLine) || !initialScrollDone) {
                shouldScrollToFirstLine = false
                performSmoothTopScroll(kotlin.math.max(0, currentLineIndex), 800)
                if (!isAppMinimized) {
                    initialScrollDone = true
                }
            } else if (currentLineIndex != -1) {
                if (isSeeking) {
                    performSmoothTopScroll(currentLineIndex, 400) // Fast for seeking
                } else if (scrollLyrics) {
                    if (currentLineIndex != previousLineIndex) {
                        performSmoothTopScroll(currentLineIndex, 1200) // Smooth auto-scroll
                    }
                }
            }
        }
        if (currentLineIndex > 0) {
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
                    .nestedScroll(
                        remember {
                            object : NestedScrollConnection {
                                override fun onPostScroll(
                                    consumed: Offset,
                                    available: Offset,
                                    source: NestedScrollSource,
                                ): Offset {
                                    if (source == NestedScrollSource.UserInput &&
                                        !isSelectionModeActive &&
                                        (consumed.y != 0f || available.y != 0f)
                                    ) {
                                        isAutoScrollActive = false
                                    }
                                    return super.onPostScroll(consumed, available, source)
                                }

                                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                                    // Fling is always user input initiated in this context
                                    if (!isSelectionModeActive) {
                                        isAutoScrollActive = false
                                    }
                                    return super.onPostFling(consumed, available)
                                }
                            }
                        }
                    )
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
                            showRomanized = currentSong?.romanizeLyrics == true &&
                                (
                                    romanizeJapaneseLyrics ||
                                        romanizeKoreanLyrics ||
                                        romanizeRussianLyrics ||
                                        romanizeUkrainianLyrics ||
                                        romanizeSerbianLyrics ||
                                        romanizeBulgarianLyrics ||
                                        romanizeBelarusianLyrics ||
                                        romanizeKyrgyzLyrics ||
                                        romanizeMacedonianLyrics
                                    ),
                            textSize = lyricsTextSize,
                            lineSpacing = lyricsLineSpacing,
                            isWordForWord = lyricsWordForWord,
                            isScrolling = lazyListState.isScrollInProgress,
                            isAutoScrollActive = isAutoScrollActive,
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
                                        val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                                            it.index ==
                                                index
                                        }
                                        if (itemInfo != null) {
                                            val viewportHeight =
                                                lazyListState.layoutInfo.viewportEndOffset -
                                                    lazyListState.layoutInfo.viewportStartOffset
                                            val center =
                                                lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
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
                    LyricsActionButtons(
                        isSelectionModeActive = isSelectionModeActive,
                        selectedCount = selectedIndices.size,
                        onClose = {
                            isSelectionModeActive = false
                            selectedIndices.clear()
                        },
                        onShare = {
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
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    )
                }
            }
            // Removed the more button from bottom - it's now in the top header
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
                            text =
                            stringResource(R.string.generating_image) + "\n" + stringResource(R.string.please_wait),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showResyncButton && !isAutoScrollActive && isSynced,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + scaleOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    isAutoScrollActive = true
                    if (currentLineIndex != -1) {
                        scope.launch {
                            performSmoothTopScroll(currentLineIndex, 800)
                        }
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.sync),
                        contentDescription = stringResource(R.string.action_resync),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_resync),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        if (showShareDialog && shareDialogData != null) {
            val (lyricsText, songTitle, artists) = shareDialogData!!
            LyricsShareDialog(
                showDialog = showShareDialog,
                onDismiss = { showShareDialog = false },
                onShareAsText = {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        val songLink = "https://music.youtube.com/watch?v=${mediaMetadata?.id}"
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
                },
                onShareAsImage = {
                    shareDialogData = Triple(lyricsText, songTitle, artists)
                    showColorPickerDialog = true
                    showShareDialog = false
                }
            )
        }

        if (showColorPickerDialog && shareDialogData != null) {
            val (lyricsText, songTitle, artists) = shareDialogData!!
            val coverUrl = mediaMetadata?.thumbnailUrl
            val paletteColors = remember { mutableStateListOf<Color>() }

            // Image loading for palette (kept here as it involves fetching)
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

            LyricsColorPickerDialog(
                showDialog = showColorPickerDialog,
                onDismiss = { showColorPickerDialog = false },
                lyricsText = lyricsText,
                mediaMetadata = mediaMetadata,
                paletteColors = paletteColors,
                previewBackgroundColor = previewBackgroundColor,
                onPreviewBackgroundColorChange = { previewBackgroundColor = it },
                previewTextColor = previewTextColor,
                onPreviewTextColorChange = { previewTextColor = it },
                previewSecondaryTextColor = previewSecondaryTextColor,
                onPreviewSecondaryTextColorChange = { previewSecondaryTextColor = it },
                onShare = {
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
                                secondaryTextColor = previewSecondaryTextColor.toArgb()
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
                                Intent.createChooser(shareIntent, context.getString(R.string.share_lyrics))
                            )
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.failed_to_create_image, e.message ?: ""),
                                Toast.LENGTH_SHORT
                            ).show()
                        } finally {
                            showProgressDialog = false
                        }
                    }
                }
            )
        }
    }
}

// Lyrics constants
val LyricsPreviewTime = 2.seconds
