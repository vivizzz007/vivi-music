package com.music.vivi.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import android.app.Activity
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_READY
import androidx.palette.graphics.Palette
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
import com.music.vivi.db.entities.LyricsEntity
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.extensions.toggleRepeatMode
import com.music.vivi.models.MediaMetadata
import com.music.vivi.ui.component.Lyrics
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.PlayerSliderTrack
import com.music.vivi.ui.component.BigSeekBar
import androidx.navigation.NavController
import com.music.vivi.constants.SwipeGestureEnabledKey
import me.saket.squiggles.SquigglySlider
import com.music.vivi.ui.menu.LyricsMenu
import com.music.vivi.ui.theme.PlayerColorExtractor
import com.music.vivi.ui.theme.PlayerSliderColors
import com.music.vivi.update.apple.SwipeGesture
import com.music.vivi.utils.rememberEnumPreference
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.runCatching
import com.music.vivi.utils.makeTimeString
import com.music.vivi.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    mediaMetadata: MediaMetadata,
    onBackClick: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Keep the screen on when entering the screen
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            // Remove the feature when exiting the screen
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val playerVolume = playerConnection.service.playerVolume.collectAsState()

    // slider style preference
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)

    // Auto-fetch lyrics when no lyrics found (same logic as refetch)
    LaunchedEffect(mediaMetadata.id, currentLyrics) {
        if (currentLyrics == null) {
            // Small delay to ensure database state is stable
            delay(500)

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    // Get LyricsHelper from Hilt
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.music.vivi.di.LyricsHelperEntryPoint::class.java
                    )
                    val lyricsHelper = entryPoint.lyricsHelper()

                    // Fetch lyrics automatically
                    val lyrics = lyricsHelper.getLyrics(mediaMetadata)

                    // Save to database
                    database.query {
                        upsert(LyricsEntity(mediaMetadata.id, lyrics))
                    }
                } catch (e: Exception) {
                    // Handle error silently - user can manually refetch if needed
                }
            }
        }
    }

    val swipeGestureEnabled by rememberPreference(SwipeGestureEnabledKey, defaultValue = true)

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(C.TIME_UNSET) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.GRADIENT)

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = isSystemInDarkTheme

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
            } else {
                gradientColors = emptyList()
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val textBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        PlayerBackgroundStyle.BLUR -> Color.White
        PlayerBackgroundStyle.GRADIENT -> Color.White
    }

    val icBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
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
        // Background Layer
        if (playerBackground == PlayerBackgroundStyle.BLUR) {
            Crossfade(
                targetState = mediaMetadata?.thumbnailUrl,
                animationSpec = tween(600)
            ) { thumbnailUrl ->
                if (thumbnailUrl != null) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(60.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )
                }
            }
        } else if (playerBackground == PlayerBackgroundStyle.GRADIENT) {
            Crossfade(
                targetState = gradientColors,
                animationSpec = tween(600)
            ) { colors ->
                if (colors.isNotEmpty()) {
                    val gradientColorStops = if (colors.size >= 3) {
                        arrayOf(
                            0.0f to colors[0], // Top
                            0.5f to colors[1], // Middle
                            1.0f to colors[2]  // Bottom
                        )
                    } else {
                        arrayOf(
                            0.0f to colors[0], // Top
                            0.6f to colors[0].copy(alpha = 0.7f), // Middle
                            1.0f to Color.Black // Bottom
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(colorStops = gradientColorStops))
                            .background(Color.Black.copy(alpha = 0.2f))
                    )
                }
            }
        }

        if (playerBackground != PlayerBackgroundStyle.DEFAULT) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
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
                    // Detailed header with album art, song info, and action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .SwipeGesture(
                                enabled = swipeGestureEnabled,
                                onSwipeLeft = { player.seekToPrevious() },
                                onSwipeRight = { player.seekToNext() }
                            ),
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
                                        playerConnection.toggleLike()
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
                                    ) {
                                        menuState.show {
                                            LyricsMenu(
                                                lyricsProvider = { currentLyrics },
                                                songProvider = { currentSong?.song },
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
                    }

                    // Main content row
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        // Left side - Lyrics only
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        ) {
                            // Lyrics content - centered in landscape
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center  // Center lyrics in landscape
                            ) {
                                Lyrics(
                                    sliderPositionProvider = { sliderPosition }
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                // Portrait layout - original layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.systemBars.asPaddingValues())
                ) {
                    // Detailed header with album art, song info, and action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .SwipeGesture(
                                enabled = swipeGestureEnabled,
                                onSwipeLeft = { player.seekToPrevious() },
                                onSwipeRight = { player.seekToNext() }
                            ),
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
                                        playerConnection.toggleLike()
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
                                    ) {
                                        menuState.show {
                                            LyricsMenu(
                                                lyricsProvider = { currentLyrics },
                                                songProvider = { currentSong?.song },
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
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Lyrics(
                            sliderPositionProvider = { sliderPosition },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}