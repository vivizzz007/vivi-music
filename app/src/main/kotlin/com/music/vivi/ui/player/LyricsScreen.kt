package com.music.vivi.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_READY
import androidx.palette.graphics.Palette
import androidx.core.graphics.drawable.toBitmap
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
import com.music.vivi.constants.SliderStyle
import com.music.vivi.constants.SliderStyleKey
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.db.entities.LyricsEntity
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.extensions.toggleRepeatMode
import com.music.vivi.lyrics.LyricsHelper
import com.music.vivi.models.MediaMetadata
import com.music.vivi.ui.component.Lyrics
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.PlayerSliderTrack
import com.music.vivi.ui.component.BigSeekBar
import androidx.navigation.NavController
import me.saket.squiggles.SquigglySlider
import com.music.vivi.ui.menu.LyricsMenu
import com.music.vivi.ui.theme.PlayerColorExtractor
import com.music.vivi.ui.theme.PlayerSliderColors
import com.music.vivi.utils.rememberEnumPreference
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.runCatching
import com.music.vivi.utils.makeTimeString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.music.vivi.constants.LyricsTextPositionKey
import com.music.vivi.ui.screens.settings.LyricsPosition
import com.music.vivi.utils.rememberPreference
import androidx.compose.ui.graphics.CompositingStrategy
import com.music.vivi.Dotlyrics.AnimatedMusicBeatDots
// Coroutines
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs
// Animation
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.gestures.detectTapGestures
import com.music.vivi.constants.EnableDoubleTapGesturesKey
import com.music.vivi.constants.EnableSwipeGesturesKey
import com.music.vivi.db.entities.Song
import com.music.vivi.playback.PlayerConnection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    mediaMetadata: MediaMetadata,
    onBackClick: () -> Unit,
    navController: NavController,
    preloadedLyrics: String? = null,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val playerVolume = playerConnection.service.playerVolume.collectAsState()
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)

    val lyricsTextPosition by rememberPreference(LyricsTextPositionKey, "CENTER")
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)

    val (enableSwipeGestures, onEnableSwipeGesturesChange) = rememberPreference(
        EnableSwipeGesturesKey,
        defaultValue = true
    )
    val (enableDoubleTapGestures, onEnableDoubleTapGesturesChange) = rememberPreference(
        EnableDoubleTapGesturesKey,
        defaultValue = true
    )

    var isLoadingLyrics by remember { mutableStateOf(false) }

    val lyricsPosition = when (lyricsTextPosition) {
        "LEFT" -> LyricsPosition.LEFT
        "RIGHT" -> LyricsPosition.RIGHT
        else -> LyricsPosition.CENTER
    }

    var displayLyrics by remember { mutableStateOf<String?>(null) }

    // Initialize with preloaded lyrics if available
    LaunchedEffect(preloadedLyrics, mediaMetadata.id) {
        if (preloadedLyrics != null) {
            displayLyrics = preloadedLyrics
            isLoadingLyrics = false
        } else {
            // Only set loading if we don't have preloaded lyrics
            isLoadingLyrics = true
            displayLyrics = null
        }
    }

    // Update display lyrics when currentLyrics changes
    LaunchedEffect(currentLyrics) {
        val lyrics = currentLyrics
        if (lyrics?.lyrics != null) {
            displayLyrics = lyrics.lyrics
            isLoadingLyrics = false
        }
    }

    // Modified lyrics fetching - only fetch if we don't have preloaded lyrics
    LaunchedEffect(mediaMetadata.id) {
        // Skip fetching if we already have preloaded lyrics
        if (preloadedLyrics != null) {
            return@LaunchedEffect
        }

        isLoadingLyrics = true
        displayLyrics = null

        delay(10)

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    com.music.vivi.di.LyricsHelperEntryPoint::class.java
                )
                val lyricsHelper = entryPoint.lyricsHelper()

                val lyrics = lyricsHelper.getLyrics(mediaMetadata)

                database.query {
                    upsert(LyricsEntity(mediaMetadata.id, lyrics))
                }

                withContext(Dispatchers.Main) {
                    displayLyrics = lyrics
                }

            } catch (e: Exception) {
                Log.w("LyricsScreen", "Failed to auto-fetch lyrics for ${mediaMetadata.title}: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    isLoadingLyrics = false
                }
            }
        }
    }

    // Rest of your existing LyricsScreen implementation...
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(C.TIME_UNSET) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.GRADIENT)

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    val defaultGradientColors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata.id, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT) {
            if (mediaMetadata.thumbnailUrl != null) {
                val cachedColors = gradientColorsCache[mediaMetadata.id]
                if (cachedColors != null) {
                    gradientColors = cachedColors
                } else {
                    launch(Dispatchers.IO) {
                        val request = ImageRequest.Builder(context)
                            .data(mediaMetadata.thumbnailUrl)
                            .size(Size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE))
                            .allowHardware(false)
                            .memoryCacheKey("gradient_${mediaMetadata.id}")
                            .build()

                        val result = runCatching {
                            context.imageLoader.execute(request).image
                        }.getOrNull()

                        if (result != null) {
                            val bitmap = result.toBitmap()
                            val palette = withContext(Dispatchers.Default) {
                                Palette.from(bitmap)
                                    .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                                    .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                                    .generate()
                            }

                            val extractedColors = PlayerColorExtractor.extractGradientColors(
                                palette = palette,
                                fallbackColor = fallbackColor
                            )

                            gradientColorsCache[mediaMetadata.id] = extractedColors
                            gradientColors = extractedColors
                        } else {
                            gradientColors = defaultGradientColors
                        }
                    }
                }
            } else {
                gradientColors = emptyList()
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val textBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.BLUR -> Color.White
        PlayerBackgroundStyle.GRADIENT -> Color.White
    }

    val icBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.BLUR -> Color.Black
        PlayerBackgroundStyle.GRADIENT -> Color.Black
    }

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(100)
                position = player.currentPosition
                duration = player.duration
            }
        }
    }

    BackHandler(onBack = onBackClick)

    Box(modifier = modifier.fillMaxSize()) {
        // Background layer
        Box(modifier = Modifier.fillMaxSize()) {
            when (playerBackground) {
                PlayerBackgroundStyle.BLUR -> {
                    AnimatedContent(
                        targetState = mediaMetadata.thumbnailUrl,
                        transitionSpec = {
                            fadeIn(tween(1000)) togetherWith fadeOut(tween(1000))
                        }
                    ) { thumbnailUrl ->
                        if (thumbnailUrl != null) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = "Blurred background",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .blur(50.dp)
                                )
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
                        }
                    }
                }
                PlayerBackgroundStyle.GRADIENT -> {
                    AnimatedContent(
                        targetState = gradientColors,
                        transitionSpec = {
                            fadeIn(tween(1000)) togetherWith fadeOut(tween(1000))
                        }
                    ) { colors ->
                        if (colors.isNotEmpty()) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                val gradientColorStops = if (colors.size >= 3) {
                                    arrayOf(
                                        0.0f to colors[0],
                                        0.5f to colors[1],
                                        1.0f to colors[2]
                                    )
                                } else {
                                    arrayOf(
                                        0.0f to colors[0],
                                        0.6f to colors[0].copy(alpha = 0.7f),
                                        1.0f to Color.Black
                                    )
                                }
                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops = gradientColorStops)))
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                            }
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
                }
            }
        }

        // Check orientation and layout accordingly
        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                // Landscape layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    SwipeableHeader(
                        mediaMetadata = mediaMetadata,
                        textBackgroundColor = textBackgroundColor,
                        onBackClick = onBackClick,
                        onPrevious = { player.seekToPrevious() },
                        onNext = { player.seekToNext() },
                        onMenuClick = {
                            menuState.show {
                                LyricsMenu(
                                    lyricsProvider = { currentLyrics },
                                    mediaMetadataProvider = { mediaMetadata },
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )

                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        ) {
                            AppleLikeLyrics(
                                lyrics = displayLyrics, // Use displayLyrics instead of currentLyrics?.lyrics
                                currentPosition = position,
                                lyricsPosition = lyricsPosition,
                                isPlaying = isPlaying,
                                textColor = textBackgroundColor,
                                isLoading = isLoadingLyrics,
                                onSeek = { timestamp -> player.seekTo(timestamp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .padding(horizontal = 48.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Complete slider implementation with all styles
                            when (sliderStyle) {
                                SliderStyle.DEFAULT -> {
                                    Slider(
                                        value = (sliderPosition ?: position).toFloat(),
                                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                        onValueChange = { sliderPosition = it.toLong() },
                                        onValueChangeFinished = {
                                            sliderPosition?.let {
                                                player.seekTo(it)
                                                position = it
                                            }
                                            sliderPosition = null
                                        },
                                        colors = PlayerSliderColors.defaultSliderColors(textBackgroundColor),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                SliderStyle.SQUIGGLY -> {
                                    SquigglySlider(
                                        value = (sliderPosition ?: position).toFloat(),
                                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                        onValueChange = { sliderPosition = it.toLong() },
                                        onValueChangeFinished = {
                                            sliderPosition?.let {
                                                player.seekTo(it)
                                                position = it
                                            }
                                            sliderPosition = null
                                        },
                                        colors = PlayerSliderColors.squigglySliderColors(textBackgroundColor),
                                        modifier = Modifier.fillMaxWidth(),
                                        squigglesSpec = SquigglySlider.SquigglesSpec(
                                            amplitude = if (isPlaying) (2.dp).coerceAtLeast(2.dp) else 0.dp,
                                            strokeWidth = 3.dp,
                                        )
                                    )
                                }
                                SliderStyle.SLIM -> {
                                    Slider(
                                        value = (sliderPosition ?: position).toFloat(),
                                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                        onValueChange = { sliderPosition = it.toLong() },
                                        onValueChangeFinished = {
                                            sliderPosition?.let {
                                                player.seekTo(it)
                                                position = it
                                            }
                                            sliderPosition = null
                                        },
                                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                                        track = { sliderState ->
                                            PlayerSliderTrack(
                                                sliderState = sliderState,
                                                colors = PlayerSliderColors.slimSliderColors(textBackgroundColor)
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = makeTimeString(sliderPosition ?: position),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textBackgroundColor.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textBackgroundColor.copy(alpha = 0.7f)
                                )
                            }

                            Spacer(modifier = Modifier.height(45.dp))

                            // Control buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { playerConnection.player.toggleRepeatMode() },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            when (repeatMode) {
                                                Player.REPEAT_MODE_OFF,
                                                Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                                else -> R.drawable.repeat
                                            }
                                        ),
                                        contentDescription = "Repeat",
                                        tint = if (repeatMode == Player.REPEAT_MODE_OFF) {
                                            textBackgroundColor.copy(alpha = 0.4f)
                                        } else {
                                            textBackgroundColor
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { player.seekToPrevious() },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.skip_previous),
                                        contentDescription = null,
                                        tint = textBackgroundColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { player.togglePlayPause() },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (isPlaying) R.drawable.pause else R.drawable.play
                                        ),
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = textBackgroundColor,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { player.seekToNext() },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.skip_next),
                                        contentDescription = null,
                                        tint = textBackgroundColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.shuffle),
                                        contentDescription = "Shuffle",
                                        tint = if (shuffleModeEnabled) {
                                            textBackgroundColor
                                        } else {
                                            textBackgroundColor.copy(alpha = 0.4f)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                // Portrait layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.systemBars.asPaddingValues())
                ) {
                    SwipeableHeader(
                        mediaMetadata = mediaMetadata,
                        currentSong = currentSong,
                        textBackgroundColor = textBackgroundColor,
                        playerConnection = playerConnection,
                        onBackClick = onBackClick,
                        onPrevious = { player.seekToPrevious() },
                        onNext = { player.seekToNext() },
                        enableSwipeGestures = enableSwipeGestures,
                        enableDoubleTapGestures = enableDoubleTapGestures,
                        onMenuClick = {
                            menuState.show {
                                LyricsMenu(
                                    lyricsProvider = { currentLyrics },
                                    mediaMetadataProvider = { mediaMetadata },
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )

                    AppleLikeLyrics(
                        lyrics = displayLyrics, // Use displayLyrics instead of currentLyrics?.lyrics
                        currentPosition = position,
                        lyricsPosition = lyricsPosition,
                        isPlaying = isPlaying,
                        textColor = textBackgroundColor,
                        isLoading = isLoadingLyrics,
                        onSeek = { timestamp -> player.seekTo(timestamp) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}
@Composable
fun SwipeableHeader(
    mediaMetadata: MediaMetadata,
    currentSong: Song? = null,
    textBackgroundColor: Color,
    playerConnection: PlayerConnection? = null,
    enableSwipeGestures: Boolean = true,
    enableDoubleTapGestures: Boolean = true,
    onBackClick: () -> Unit = {},
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMenuClick: () -> Unit,
    isLandscape: Boolean = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isDragging) offsetX else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val swipeThreshold = 100f
    val haptic = LocalHapticFeedback.current

    if (isLandscape) {
        // Landscape header (simple header without swipe for now, can be added if needed)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .zIndex(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 16.dp)
                    ) { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.expand_more),
                    contentDescription = stringResource(R.string.close),
                    tint = textBackgroundColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.now_playing),
                    style = MaterialTheme.typography.titleMedium,
                    color = textBackgroundColor
                )
                Text(
                    text = mediaMetadata.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textBackgroundColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 16.dp)
                    ) { onMenuClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_horiz),
                    contentDescription = stringResource(R.string.more_options),
                    tint = textBackgroundColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    } else {
        // Portrait header with conditional swipe and double-tap functionality
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (enableDoubleTapGestures) {
                        Modifier.pointerInput(Unit) {
                            var lastTapTime = 0L
                            val doubleTapTimeWindow = 300L

                            detectTapGestures(
                                onTap = { offset ->
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastTapTime < doubleTapTimeWindow) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        playerConnection?.player?.togglePlayPause()
                                        lastTapTime = 0L
                                    } else {
                                        lastTapTime = currentTime
                                    }
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .then(
                    if (enableSwipeGestures) {
                        Modifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = {
                                    if (abs(offsetX) > swipeThreshold) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (offsetX > 0) {
                                            onPrevious()
                                        } else {
                                            onNext()
                                        }
                                    }
                                    isDragging = false
                                    offsetX = 0f
                                }
                            ) { _, dragAmount ->
                                offsetX = (offsetX + dragAmount).coerceIn(-200f, 200f)

                                if (abs(offsetX) > swipeThreshold && abs(offsetX - dragAmount) <= swipeThreshold) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .graphicsLayer {
                        translationX = animatedOffsetX
                        alpha = 1f - (abs(animatedOffsetX) / 400f).coerceIn(0f, 0.3f)
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side - Album artwork and text info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AsyncImage(
                        model = mediaMetadata.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = mediaMetadata.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = textBackgroundColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (mediaMetadata.artists.isNotEmpty()) {
                                mediaMetadata.artists.joinToString(", ") { it.name }
                            } else {
                                "Unknown Artist"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp
                            ),
                            color = textBackgroundColor.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right side - Action buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Favorite button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, radius = 16.dp)
                            ) {
                                playerConnection?.toggleLike()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                if (currentSong?.song?.liked == true)
                                    R.drawable.favorite
                                else R.drawable.favorite_border
                            ),
                            contentDescription = if (currentSong?.song?.liked == true) "Remove from favorites" else "Add to favorites",
                            tint = if (currentSong?.song?.liked == true)
                                MaterialTheme.colorScheme.error
                            else
                                textBackgroundColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // More button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, radius = 16.dp)
                            ) { onMenuClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_horiz),
                            contentDescription = stringResource(R.string.more_options),
                            tint = textBackgroundColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Visual feedback for swipe direction
            if (isDragging && abs(offsetX) > 50f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (offsetX > 0) Color.Green.copy(alpha = 0.1f)
                            else Color.Blue.copy(alpha = 0.1f)
                        ),
                    contentAlignment = if (offsetX > 0) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Icon(
                        painter = painterResource(
                            if (offsetX > 0) R.drawable.skip_previous else R.drawable.skip_next
                        ),
                        contentDescription = null,
                        tint = textBackgroundColor.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(32.dp)
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AppleLikeLyrics(
    lyrics: String?,
    currentPosition: Long,
    lyricsPosition: LyricsPosition,
    isPlaying: Boolean,
    textColor: Color,
    isLoading: Boolean = false,
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val parsedLyrics = remember(lyrics) {
        when {
            lyrics.isNullOrBlank() -> emptyList()
            lyrics.contains("[") && lyrics.contains("]") -> {
                parseLrcLyrics(lyrics)
            }
            else -> {
                parseSimpleLyrics(lyrics)
            }
        }
    }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val isInInstrumental = remember(currentPosition, parsedLyrics) {
        isInstrumentalSection(parsedLyrics, currentPosition)
    }

    // Fixed lyrics line calculation with proper reset logic
    val currentLineData = remember(currentPosition, parsedLyrics) {
        if (parsedLyrics.isEmpty()) return@remember LyricsState(-1, 0f)

        var activeIndex = -1
        var progress = 0f

        // Check if position is before first line (song start/restart)
        if (currentPosition < parsedLyrics.first().timestamp) {
            return@remember LyricsState(-1, 0f)
        }

        // Find the current active line
        for (i in parsedLyrics.indices) {
            val currentLine = parsedLyrics[i]
            val nextLine = parsedLyrics.getOrNull(i + 1)

            if (currentPosition >= currentLine.timestamp) {
                // Check if we're still within this line's duration
                if (nextLine == null || currentPosition < nextLine.timestamp) {
                    activeIndex = i

                    // Calculate progress to next line
                    if (nextLine != null) {
                        val lineDuration = nextLine.timestamp - currentLine.timestamp
                        if (lineDuration > 0) {
                            progress = ((currentPosition - currentLine.timestamp).toFloat() / lineDuration.toFloat()).coerceIn(0f, 1f)
                        }
                    } else {
                        // Last line - calculate progress based on time since line started
                        val timeSinceLineStart = currentPosition - currentLine.timestamp
                        // Assume each line lasts 4 seconds if it's the last line
                        val estimatedLineDuration = 4000L
                        progress = (timeSinceLineStart.toFloat() / estimatedLineDuration.toFloat()).coerceIn(0f, 1f)
                    }
                    break
                }
            }
        }

        // If we went past all lyrics, check if we should reset (song restarted)
        if (activeIndex == -1 && parsedLyrics.isNotEmpty()) {
            val lastLine = parsedLyrics.last()
            // If current position is way past the last line, song might have restarted
            if (currentPosition > lastLine.timestamp + 10000L) { // 10 second buffer
                // Check if position is now closer to start
                if (currentPosition < parsedLyrics.first().timestamp + 30000L) { // Within 30 seconds of first line
                    activeIndex = -1 // Reset to beginning state
                }
            }
        }

        LyricsState(activeIndex, progress)
    }

    // Smooth auto-scroll with better timing and reset handling
    LaunchedEffect(currentLineData.activeIndex, currentLineData.progress) {
        if (currentLineData.activeIndex >= 0 && parsedLyrics.isNotEmpty()) {
            coroutineScope.launch {
                if (currentLineData.progress > 0.85f) {
                    val targetIndex = (currentLineData.activeIndex + 1).coerceAtMost(parsedLyrics.size - 1)
                    lazyListState.animateScrollToItem(
                        index = targetIndex,
                        scrollOffset = 0
                    )
                } else if (currentLineData.progress == 0f) {
                    lazyListState.animateScrollToItem(
                        index = currentLineData.activeIndex,
                        scrollOffset = 0
                    )
                }
            }
        } else if (currentLineData.activeIndex == -1 && parsedLyrics.isNotEmpty()) {
            // Reset scroll to top when lyrics reset
            coroutineScope.launch {
                lazyListState.animateScrollToItem(0, scrollOffset = 0)
            }
        }
    }

    val horizontalAlignment = when (lyricsPosition) {
        LyricsPosition.LEFT -> Alignment.Start
        LyricsPosition.CENTER -> Alignment.CenterHorizontally
        LyricsPosition.RIGHT -> Alignment.End
    }

    val textAlign = when (lyricsPosition) {
        LyricsPosition.LEFT -> TextAlign.Start
        LyricsPosition.CENTER -> TextAlign.Center
        LyricsPosition.RIGHT -> TextAlign.End
    }

    val topPadding = if (isLandscape) 16.dp else 40.dp
    val bottomPadding = if (isLandscape) 100.dp else 120.dp

    when {
        isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading lyrics...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        parsedLyrics.isEmpty() -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (lyrics == null) {
                        Icon(
                            painter = painterResource(R.drawable.music_note),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = textColor.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No lyrics available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        AnimatedMusicBeatDots(
                            isPlaying = isPlaying,
                            textColor = textColor,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        else -> {
            if (isInInstrumental && currentLineData.activeIndex == -1) {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AnimatedMusicBeatDots(
                            isPlaying = isPlaying,
                            textColor = textColor,
                            modifier = Modifier.padding(16.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "Music playing...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = topPadding,
                        bottom = bottomPadding
                    ),
                    horizontalAlignment = horizontalAlignment
                ) {
                    itemsIndexed(parsedLyrics) { index, lyricLine ->
                        SmoothAppleLyricLine(
                            text = lyricLine.text,
                            currentLineIndex = currentLineData.activeIndex,
                            lineIndex = index,
                            progress = if (index == currentLineData.activeIndex) currentLineData.progress else 0f,
                            textAlign = textAlign,
                            textColor = textColor,
                            onClick = { onSeek(lyricLine.timestamp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 24.dp,
                                    vertical = if (isLandscape) 8.dp else 12.dp
                                )
                        )

                        if (isInInstrumental && index == currentLineData.activeIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = if (isLandscape) 24.dp else 32.dp),
                                contentAlignment = when (lyricsPosition) {
                                    LyricsPosition.LEFT -> Alignment.CenterStart
                                    LyricsPosition.CENTER -> Alignment.Center
                                    LyricsPosition.RIGHT -> Alignment.CenterEnd
                                }
                            ) {
                                AnimatedMusicBeatDots(
                                    isPlaying = isPlaying,
                                    textColor = textColor,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmoothAppleLyricLine(
    text: String,
    currentLineIndex: Int,
    lineIndex: Int,
    progress: Float,
    textAlign: TextAlign,
    textColor: Color,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lineState = when {
        lineIndex == currentLineIndex -> LineState.ACTIVE
        lineIndex == currentLineIndex + 1 -> LineState.UPCOMING
        lineIndex < currentLineIndex -> LineState.PASSED
        else -> LineState.INACTIVE
    }

    val customEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    val scale by animateFloatAsState(
        targetValue = when (lineState) {
            LineState.ACTIVE -> 1.03f + (progress * 0.02f)
            LineState.UPCOMING -> 1.0f + (progress * 0.01f)
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = when (lineState) {
            LineState.ACTIVE -> 1.0f
            LineState.UPCOMING -> 0.75f + (progress * 0.15f)
            LineState.PASSED -> 0.45f
            LineState.INACTIVE -> 0.25f
        },
        animationSpec = tween(
            durationMillis = 500,
            easing = customEasing
        ),
        label = "alpha"
    )

    val fontSize by animateDpAsState(
        targetValue = when (lineState) {
            LineState.ACTIVE -> (30f + (progress * 1f)).dp
            LineState.UPCOMING -> (26f + (progress * 1f)).dp
            else -> 26.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fontSize"
    )

    val fontWeight by animateIntAsState(
        targetValue = when (lineState) {
            LineState.ACTIVE -> FontWeight.Bold.weight
            LineState.UPCOMING -> FontWeight.Medium.weight + ((progress * (FontWeight.Bold.weight - FontWeight.Medium.weight)).toInt())
            else -> FontWeight.Normal.weight
        },
        animationSpec = tween(
            durationMillis = 300,
            easing = customEasing
        ),
        label = "fontWeight"
    )

    val color by animateColorAsState(
        targetValue = when (lineState) {
            LineState.ACTIVE -> textColor
            LineState.UPCOMING -> textColor.copy(alpha = 0.8f + (progress * 0.2f))
            LineState.PASSED -> textColor.copy(alpha = 0.5f)
            LineState.INACTIVE -> textColor.copy(alpha = 0.3f)
        },
        animationSpec = tween(
            durationMillis = 400,
            easing = customEasing
        ),
        label = "color"
    )

    val boxAlignment = when (textAlign) {
        TextAlign.Start -> Alignment.CenterStart
        TextAlign.Center -> Alignment.Center
        TextAlign.End -> Alignment.CenterEnd
        else -> Alignment.Center
    }

    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    bounded = false,
                    radius = 100.dp,
                    color = textColor.copy(alpha = 0.3f)
                )
            ) { onClick() },
        contentAlignment = boxAlignment
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = fontSize.value.sp,
                fontWeight = FontWeight(fontWeight),
                letterSpacing = if (lineState == LineState.ACTIVE) (0.2f + progress * 0.1f).sp else 0.sp,
                lineHeight = (fontSize.value * 1.3f).sp
            ),
            color = color,
            textAlign = textAlign,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha

                    translationY = when (lineState) {
                        LineState.UPCOMING -> -progress * 8f
                        else -> 0f
                    }
                }
        )
    }
}

data class LyricsState(
    val activeIndex: Int,
    val progress: Float
)

enum class LineState {
    ACTIVE,
    UPCOMING,
    PASSED,
    INACTIVE
}

data class LyricLine(
    val timestamp: Long,
    val text: String
)


fun parseLrcLyrics(lyricsText: String): List<LyricLine> {
    val lines = lyricsText.split("\n")
    val lyricLines = mutableListOf<LyricLine>()

    val timeRegex = "\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.*)".toRegex()

    for (line in lines) {
        if (line.isBlank()) continue

        val matchResult = timeRegex.find(line.trim())
        if (matchResult != null) {
            val (minutes, seconds, centiseconds, text) = matchResult.destructured
            val timestamp = (minutes.toLong() * 60 + seconds.toLong()) * 1000 + (centiseconds.toLong() * 10)

            val cleanText = text.trim()
            if (cleanText.isNotEmpty()) {
                lyricLines.add(LyricLine(timestamp, cleanText))
            }
        }
    }

    return lyricLines.sortedBy { it.timestamp }
}

fun parseSimpleLyrics(lyricsText: String): List<LyricLine> {
    val lines = lyricsText.split("\n").filter { it.trim().isNotEmpty() }
    return lines.mapIndexed { index, line ->
        LyricLine(
            timestamp = (index * 4000).toLong(),
            text = line.trim()
        )
    }
}

fun isInstrumentalSection(
    parsedLyrics: List<LyricLine>,
    currentPosition: Long,
    instrumentalGapThreshold: Long = 6000L
): Boolean {
    if (parsedLyrics.isEmpty()) return true

    var currentLineIndex = -1
    var nextLineIndex = -1

    for (i in parsedLyrics.indices) {
        if (currentPosition >= parsedLyrics[i].timestamp) {
            currentLineIndex = i
        } else {
            nextLineIndex = i
            break
        }
    }

    if (currentLineIndex == -1 && nextLineIndex != -1) {
        return (parsedLyrics[nextLineIndex].timestamp - currentPosition) > 2000L
    }

    if (currentLineIndex != -1 && nextLineIndex == -1) {
        return (currentPosition - parsedLyrics[currentLineIndex].timestamp) > instrumentalGapThreshold
    }

    if (currentLineIndex != -1 && nextLineIndex != -1) {
        val gapDuration = parsedLyrics[nextLineIndex].timestamp - parsedLyrics[currentLineIndex].timestamp
        return gapDuration > instrumentalGapThreshold
    }

    return false
}

enum class LyricsPosition {
    LEFT, CENTER, RIGHT
}