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

// Animation
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateIntAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    mediaMetadata: MediaMetadata,
    onBackClick: () -> Unit,
    navController: NavController,
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

    // Add lyrics position preference using your existing preference system
    val lyricsTextPosition by rememberPreference(LyricsTextPositionKey, "CENTER")

    // slider style preference
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)

    // Add loading state for better UX
    var isLoadingLyrics by remember { mutableStateOf(false) }

    // Debug: Let's see what we're getting
    LaunchedEffect(currentLyrics) {
        Log.d("LyricsDebug", "currentLyrics changed: $currentLyrics")
        Log.d("LyricsDebug", "lyrics text: ${currentLyrics?.lyrics}")
    }

    val lyricsPosition = when (lyricsTextPosition) {
        "LEFT" -> LyricsPosition.LEFT
        "RIGHT" -> LyricsPosition.RIGHT
        else -> LyricsPosition.CENTER
    }

    // Optimized auto-fetch lyrics with better error handling and faster execution
    LaunchedEffect(mediaMetadata.id, currentLyrics) {
        if (currentLyrics == null && !isLoadingLyrics) {
            isLoadingLyrics = true

            // Start fetch immediately without delay for faster response
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    Log.d("LyricsDebug", "Starting lyrics fetch for: ${mediaMetadata.title}")

                    // Get LyricsHelper from Hilt
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.music.vivi.di.LyricsHelperEntryPoint::class.java
                    )
                    val lyricsHelper = entryPoint.lyricsHelper()

                    // Fetch lyrics with timeout to prevent hanging
                    val lyrics = withTimeoutOrNull(10000) { // 10 second timeout
                        lyricsHelper.getLyrics(mediaMetadata)
                    }

                    if (lyrics != null) {
                        Log.d("LyricsDebug", "Lyrics fetched successfully, saving to database")

                        // Save to database
                        database.query {
                            upsert(LyricsEntity(mediaMetadata.id, lyrics))
                        }

                        Log.d("LyricsDebug", "Lyrics saved to database")
                    } else {
                        Log.w("LyricsDebug", "Lyrics fetch timed out for: ${mediaMetadata.title}")
                    }

                } catch (e: Exception) {
                    // Log error for debugging but don't crash the app
                    Log.w("LyricsScreen", "Failed to auto-fetch lyrics for ${mediaMetadata.title}: ${e.message}")
                    // User can still manually refetch if needed
                } finally {
                    isLoadingLyrics = false
                }
            }
        }
    }

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(C.TIME_UNSET) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.GRADIENT)

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    val defaultGradientColors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    // Optimize gradient loading with better caching
    LaunchedEffect(mediaMetadata.id, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT) {
            if (mediaMetadata.thumbnailUrl != null) {
                val cachedColors = gradientColorsCache[mediaMetadata.id]
                if (cachedColors != null) {
                    gradientColors = cachedColors
                } else {
                    // Use a separate coroutine to avoid blocking lyrics loading
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
                // Landscape layout - split screen horizontally
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    // Unified header across full width
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .zIndex(1f),  // Ensure header is above content
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Down arrow button (left)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(
                                        bounded = true,
                                        radius = 16.dp
                                    )
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

                        // Now Playing info in center
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

                        // More button (right)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(
                                        bounded = true,
                                        radius = 16.dp
                                    )
                                ) {
                                    menuState.show {
                                        LyricsMenu(
                                            lyricsProvider = { currentLyrics },
                                            mediaMetadataProvider = { mediaMetadata },
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                },
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

                    // Main content row
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        // Right side - Lyrics only
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        ) {
                            // Apple-like Lyrics content - fill available space in landscape
                            AppleLikeLyrics(
                                lyrics = currentLyrics?.lyrics,
                                currentPosition = position,
                                lyricsPosition = lyricsPosition,
                                isPlaying = isPlaying,
                                textColor = textBackgroundColor,
                                isLoading = isLoadingLyrics, // Pass loading state
                                onSeek = { timestamp -> player.seekTo(timestamp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                        }

                        // Left side - Controls only (from slider to volume)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .padding(horizontal = 48.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Slider
                            when (sliderStyle) {
                                SliderStyle.DEFAULT -> {
                                    Slider(
                                        value = (sliderPosition ?: position).toFloat(),
                                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                        onValueChange = {
                                            sliderPosition = it.toLong()
                                        },
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
                                        onValueChange = {
                                            sliderPosition = it.toLong()
                                        },
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
                                        onValueChange = {
                                            sliderPosition = it.toLong()
                                        },
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

                            // Time display below slider
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
                                // Repeat button
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
                                        contentDescription = when (repeatMode) {
                                            Player.REPEAT_MODE_OFF -> "Repeat Off"
                                            Player.REPEAT_MODE_ALL -> "Repeat All"
                                            Player.REPEAT_MODE_ONE -> "Repeat One"
                                            else -> "Repeat"
                                        },
                                        tint = if (repeatMode == Player.REPEAT_MODE_OFF) {
                                            textBackgroundColor.copy(alpha = 0.4f)
                                        } else {
                                            textBackgroundColor
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Previous button
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

                                // Play/Pause button
                                IconButton(
                                    onClick = { player.togglePlayPause() },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (isPlaying) R.drawable.pause else R.drawable.play
                                        ),
                                        contentDescription = if (isPlaying) "Pause" else stringResource(R.string.play),
                                        tint = textBackgroundColor,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                // Next button
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

                                // Shuffle button
                                IconButton(
                                    onClick = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.shuffle),
                                        contentDescription = if (shuffleModeEnabled) stringResource(R.string.shuffle) else stringResource(R.string.shuffle),
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
                // Portrait layout - Apple-like lyrics only layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.systemBars.asPaddingValues())
                ) {
                    // Header with Down arrow and More button
                    // Replace your current header Row in both portrait and landscape modes with this:

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Center content with album artwork - like in the image
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Album artwork thumbnail
                            AsyncImage(
                                model = mediaMetadata.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Text content
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.now_playing),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = textBackgroundColor
                                )
                                Text(
                                    text = "${mediaMetadata.title}${if (mediaMetadata.artists.isNotEmpty()) " • ${mediaMetadata.artists.joinToString(", ")}" else ""}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp
                                    ),
                                    color = textBackgroundColor.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // More button (right)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(
                                        bounded = true,
                                        radius = 16.dp
                                    )
                                ) {
                                    menuState.show {
                                        LyricsMenu(
                                            lyricsProvider = { currentLyrics },
                                            mediaMetadataProvider = { mediaMetadata },
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                },
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

                    // Apple-like Lyrics - taking up the remaining space (FIXED - no more centering)
                    AppleLikeLyrics(
                        lyrics = currentLyrics?.lyrics,
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
    // Try to parse LRC first, fallback to simple lyrics
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

    // Check if we're in an instrumental section
    val isInInstrumental = remember(currentPosition, parsedLyrics) {
        isInstrumentalSection(parsedLyrics, currentPosition)
    }

    // Find current active line with smooth transition logic
    val currentLineData = remember(currentPosition, parsedLyrics) {
        if (parsedLyrics.isEmpty()) return@remember LyricsState(-1, 0f)

        var activeIndex = -1
        var progress = 0f

        for (i in parsedLyrics.indices) {
            if (currentPosition >= parsedLyrics[i].timestamp) {
                activeIndex = i

                // Calculate progress to next line for smooth transitions
                val nextIndex = i + 1
                if (nextIndex < parsedLyrics.size) {
                    val currentLineStart = parsedLyrics[i].timestamp
                    val nextLineStart = parsedLyrics[nextIndex].timestamp
                    val lineDuration = nextLineStart - currentLineStart

                    if (lineDuration > 0) {
                        progress = ((currentPosition - currentLineStart).toFloat() / lineDuration.toFloat()).coerceIn(0f, 1f)
                    }
                }
            } else {
                break
            }
        }

        LyricsState(activeIndex, progress)
    }

    // Smooth auto-scroll with better timing (starts moving closer to line completion)
    LaunchedEffect(currentLineData.activeIndex, currentLineData.progress) {
        if (currentLineData.activeIndex >= 0 && parsedLyrics.isNotEmpty()) {
            coroutineScope.launch {
                // Start scrolling when we're 85% through the current line (much later)
                if (currentLineData.progress > 0.85f) {
                    val targetIndex = (currentLineData.activeIndex + 1).coerceAtMost(parsedLyrics.size - 1)

                    // Smooth scroll with custom animation
                    lazyListState.animateScrollToItem(
                        index = targetIndex,
                        scrollOffset = 0
                    )
                } else if (currentLineData.progress == 0f) {
                    // Just became active, ensure it's visible
                    lazyListState.animateScrollToItem(
                        index = currentLineData.activeIndex,
                        scrollOffset = 0
                    )
                }
            }
        }
    }

    // Determine alignment based on lyrics position preference
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
    // Determine line state with smooth transitions
    val lineState = when {
        lineIndex == currentLineIndex -> LineState.ACTIVE
        lineIndex == currentLineIndex + 1 -> LineState.UPCOMING
        lineIndex < currentLineIndex -> LineState.PASSED
        else -> LineState.INACTIVE
    }

    // Smooth animations with custom easing
    val customEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    val scale by animateFloatAsState(
        targetValue = when (lineState) {
            LineState.ACTIVE -> 1.03f + (progress * 0.02f) // Subtle scale increase during progress
            LineState.UPCOMING -> 1.0f + (progress * 0.01f) // Anticipation scaling
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
            LineState.UPCOMING -> 0.75f + (progress * 0.15f) // Gradual brightening
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
            LineState.ACTIVE -> (30f + (progress * 1f)).dp // Subtle size increase during progress
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

    // Color transitions with smooth interpolation
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

                    // Add subtle translation for flowing effect
                    translationY = when (lineState) {
                        LineState.UPCOMING -> -progress * 8f // Subtle upward movement as it becomes active
                        else -> 0f
                    }
                }
        )
    }
}


data class LyricsState(
    val activeIndex: Int,
    val progress: Float // 0f to 1f indicating progress through current line
)

enum class LineState {
    ACTIVE,
    UPCOMING,
    PASSED,
    INACTIVE
}

// Keep existing helper functions
@Composable
fun AnimatedMusicBeatDots(
    isPlaying: Boolean,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "beat_dots")

    val dotCount = 3
    val animationDuration = 1200
    val delayBetweenDots = animationDuration / dotCount

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(dotCount) { index ->
            val animatedScale by infiniteTransition.animateFloat(
                initialValue = if (isPlaying) 0.4f else 0.6f,
                targetValue = if (isPlaying) 1.2f else 0.6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = animationDuration,
                        delayMillis = delayBetweenDots * index,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_scale_$index"
            )

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                    }
                    .background(
                        color = textColor.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
            )
        }
    }
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