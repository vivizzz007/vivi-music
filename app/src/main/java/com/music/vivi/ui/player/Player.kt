@file:Suppress("NAME_SHADOWING")

package com.music.vivi.ui.player

import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.media3.common.C
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
import com.music.vivi.constants.PlayerHorizontalPadding
import com.music.vivi.constants.PlayerStyle
import com.music.vivi.constants.PlayerStyleKey
import com.music.vivi.constants.PureBlackKey
import com.music.vivi.constants.QueuePeekHeight
import com.music.vivi.constants.ShowLyricsKey
import com.music.vivi.constants.SliderStyle
import com.music.vivi.constants.SliderStyleKey
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.extensions.toggleRepeatMode
import com.music.vivi.extensions.toggleShuffleMode
import com.music.vivi.models.MediaMetadata
import com.music.vivi.ui.component.AsyncLocalImage
import com.music.vivi.ui.component.BottomSheet
import com.music.vivi.ui.component.BottomSheetState
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.PlayerSliderTrack
import com.music.vivi.ui.component.ResizableIconButton
import com.music.vivi.ui.component.rememberBottomSheetState
import com.music.vivi.ui.menu.PlayerMenu
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.ui.theme.extractGradientColors
import com.music.vivi.ui.utils.imageCache
import com.music.vivi.utils.makeTimeString
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import me.saket.squiggles.SquigglySlider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return

    val menuState = LocalMenuState.current
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val haptic = LocalHapticFeedback.current
    val useBlackBackground = remember(isSystemInDarkTheme, darkTheme, pureBlack) {
        val useDarkTheme = if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        useDarkTheme && pureBlack
    }
    if (useBlackBackground && state.value > state.collapsedBound) {
        lerp(MaterialTheme.colorScheme.surfaceContainer, Color.Black, state.progress)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)

    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.SQUIGGLY)

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)

    val context = LocalContext.current
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val playerBackground by rememberEnumPreference(key = PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.BLUR)

    val (playerStyle) = rememberEnumPreference (PlayerStyleKey , defaultValue = PlayerStyle.NEW)

    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val onBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.BLUR -> MaterialTheme.colorScheme.secondary
        else ->
            if (useDarkTheme)
                MaterialTheme.colorScheme.onSurface
            else
                if (pureBlack && darkTheme == DarkMode.ON && isSystemInDarkTheme)
                    Color.White
                else
                    MaterialTheme.colorScheme.onPrimary
    }

    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }

    LaunchedEffect(mediaMetadata) {
        if (playerBackground != PlayerBackgroundStyle.GRADIENT) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            val result = (ImageLoader(context).execute(
                ImageRequest.Builder(context)
                    .data(mediaMetadata?.thumbnailUrl)
                    .allowHardware(false)
                    .build()
            ).drawable as? BitmapDrawable)?.bitmap?.extractGradientColors()

            result?.let {
                gradientColors = it
            }
        }
    }

    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    var duration by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.duration)
    }
    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }
    var showDetailsDialog by remember { mutableStateOf(false) }
    if (showDetailsDialog) {
        DetailsDialog(
            onDismiss = { showDetailsDialog = false }
        )
    }

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(100)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
            }
        }
    }

    val queueSheetState = rememberBottomSheetState(
        dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        expandedBound = state.expandedBound,
    )

    BottomSheet(
        state = state,
        modifier = modifier,
        backgroundColor = when {
            pureBlack  -> Color.Black
            useDarkTheme || playerBackground == PlayerBackgroundStyle.BLUR ->
                MaterialTheme.colorScheme.surfaceContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        collapsedBackgroundColor = MaterialTheme.colorScheme.surfaceContainer,
        onDismiss = {
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        collapsedContent = {
            MiniPlayer(
                position = position,
                duration = duration
            )
        }
    ) {
        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            val playPauseRoundness by animateDpAsState(
                targetValue = if (isPlaying) 24.dp else 36.dp,
                animationSpec = tween(durationMillis = 100, easing = LinearEasing),
                label = "playPauseRoundness",
            )

            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
            ) {
                Row {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        AnimatedContent(
                            targetState = mediaMetadata.title,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "",
                        ) { title ->
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = onBackgroundColor,
                                modifier =
                                    Modifier
                                        .basicMarquee()
                                        .clickable(enabled = mediaMetadata.album != null && !mediaMetadata.isLocal) {
                                            navController.navigate("album/${mediaMetadata.album!!.id}")
                                            state.collapseSoft()
                                        },
                            )
                        }

                        Row(
                            modifier = Modifier.offset(y = 25.dp)
                        ) {
                            mediaMetadata.artists.fastForEachIndexed { index, artist ->
                                AnimatedContent(
                                    targetState = artist.name,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "",
                                ) { name ->
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = onBackgroundColor,
                                        maxLines = 1,
                                        modifier =
                                            Modifier.clickable(enabled = artist.id != null && !mediaMetadata.isLocal) {
                                                navController.navigate("artist/${artist.id}")
                                                state.collapseSoft()
                                            },
                                    )
                                }

                                if (index != mediaMetadata.artists.lastIndex) {
                                    AnimatedContent(
                                        targetState = ", ",
                                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                                        label = "",
                                    ) { comma ->
                                        Text(
                                            text = comma,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = onBackgroundColor,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .offset(y = 5.dp)
                            .size(36.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        ResizableIconButton(
                            icon = if (currentSong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(24.dp),
                            onClick = playerConnection::toggleLike
                        )
                    }

                    Spacer(modifier = Modifier.width(7.dp))

                    Box(
                        modifier = Modifier
                            .offset(y = 5.dp)
                            .size(36.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        ResizableIconButton(
                            icon = R.drawable.more_vert,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.Center),
                            onClick = {
                                menuState.show {
                                    PlayerMenu(
                                        mediaMetadata = mediaMetadata,
                                        navController = navController,
                                        bottomSheetState = state,
                                        onShowDetailsDialog = { showDetailsDialog = true },
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when (sliderStyle) {

//                SliderStyle.DEFAULT -> {
//                    Slider(
//                        value = (sliderPosition ?: position).toFloat(),
//                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
//                        onValueChange = {
//                            sliderPosition = it.toLong()
//                        },
//                        onValueChangeFinished = {
//                            sliderPosition?.let {
//                                playerConnection.player.seekTo(it)
//                                position = it
//                            }
//                            sliderPosition = null
//                        },
//                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
//                        track = { sliderState ->
//                            PlayerSliderTrack(
//                                sliderState = sliderState,
//                                colors = SliderDefaults.colors()
//                            )
//                        },
//                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
//                    )
//                }

                SliderStyle.SQUIGGLY -> {
                    SquigglySlider(
                        value = (sliderPosition ?: position).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = {
                            sliderPosition = it.toLong()
                        },
                        onValueChangeFinished = {
                            sliderPosition?.let {
                                playerConnection.player.seekTo(it)
                                position = it
                            }
                            sliderPosition = null
                        },
                        squigglesSpec = SquigglySlider.SquigglesSpec(
                            amplitude = if (isPlaying) 2.dp else 0.dp,
                            strokeWidth = 4.dp,
                        ),
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                    )
                }

                SliderStyle.COMPOSE -> {
                    Slider(
                        value = (sliderPosition ?: position).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = {
                            sliderPosition = it.toLong()
                        },
                        onValueChangeFinished = {
                            sliderPosition?.let {
                                playerConnection.player.seekTo(it)
                                position = it
                            }
                            sliderPosition = null
                        },
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 4.dp)
            ) {
                Text(
                    text = makeTimeString(sliderPosition ?: position),
                    style = MaterialTheme.typography.labelMedium,
                    color = onBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = onBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
            ) {
                val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = R.drawable.shuffle,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                            .align(Alignment.Center)
                            .alpha(if (shuffleModeEnabled) 1f else 0.5f),
                        color = onBackgroundColor,
                        onClick = playerConnection.player::toggleShuffleMode
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (playerStyle == PlayerStyle.NEW) {
                        ResizableIconButton(
                            icon = R.drawable.skip_previous,
                            enabled = canSkipPrevious,
                            color = onBackgroundColor,
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.Center),
                            onClick = {
                                (playerConnection.player::seekToPrevious)()
                            }
                        )
                    } else {
                        ResizableIconButton(
                            icon = R.drawable.skip_previous,
                            enabled = canSkipPrevious,
                            color = onBackgroundColor,
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.Center)
                                .combinedClickable(
                                    onClick = {
                                        (playerConnection.player::seekToPrevious)()
                                    },
                                    onLongClick = {
                                        playerConnection.player.seekTo(playerConnection.player.currentPosition - 5000)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                )
                        )
                    }
                }


                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(playPauseRoundness))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable {
                            if (playbackState == STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else {
                                playerConnection.player.togglePlayPause()
                            }
                        }
                ) {
                    Image(
                        painter = painterResource(if (playbackState == STATE_ENDED) R.drawable.replay else if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (playerStyle == PlayerStyle.NEW) {
                        ResizableIconButton(
                            icon = R.drawable.skip_next,
                            enabled = canSkipNext,
                            color = onBackgroundColor,
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.Center),
                            onClick = {
                                (playerConnection.player::seekToNext)()
                            }
                        )
                    } else {
                        ResizableIconButton(
                            icon = R.drawable.skip_next,
                            enabled = canSkipNext,
                            color = onBackgroundColor,
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.Center)
                                .combinedClickable(
                                    onClick = {
                                        (playerConnection.player::seekToNext)()
                                    },
                                    onLongClick = {
                                        playerConnection.player.seekTo(playerConnection.player.currentPosition + 5000)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                )
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = when (repeatMode) {
                            REPEAT_MODE_OFF, REPEAT_MODE_ALL -> R.drawable.repeat
                            REPEAT_MODE_ONE -> R.drawable.repeat_one
                            else -> throw IllegalStateException()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                            .align(Alignment.Center)
                            .alpha(if (repeatMode == REPEAT_MODE_OFF) 0.5f else 1f),
                        color = onBackgroundColor,
                        onClick = playerConnection.player::toggleRepeatMode
                    )
                }
            }
        }
        if (playerBackground == PlayerBackgroundStyle.BLUR) {
            if (mediaMetadata?.isLocal == true) {
                mediaMetadata.let {
                    AsyncLocalImage(
                        image = { imageCache.getLocalThumbnail(it?.localPath) },
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(100.dp)
                    )
                }
            } else {
                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(100.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        } else if (playerBackground == PlayerBackgroundStyle.GRADIENT && gradientColors.size >= 2) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(gradientColors))
            )
        }
        if (playerBackground == PlayerBackgroundStyle.BLURMOV) {
            val infiniteTransition = rememberInfiniteTransition(label = "")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 100000,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ), label = ""
            )
            if (mediaMetadata?.isLocal == true) {
                mediaMetadata?.let {
                    AsyncLocalImage(
                        image = { imageCache.getLocalThumbnail(it.localPath) },
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(90.dp)
                            .alpha(0.8f)
                            .background(if (useBlackBackground) Color.Black.copy(alpha = 0.5f) else Color.Transparent)
                            .rotate(rotation)
                    )
                }
            } else {
                val infiniteTransition = rememberInfiniteTransition(label = "")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 100000,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Restart
                    ), label = ""
                )
                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(90.dp)
                        .alpha(0.8f)
                        .background(if (useBlackBackground) Color.Black.copy(alpha = 0.5f) else Color.Transparent)
                        .rotate(rotation)
                )
            }

            if (showLyrics) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }
        }

        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .padding(bottom = queueSheetState.collapsedBound)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f)
                    ) {
                        Thumbnail(
                            sliderPositionProvider = { sliderPosition },
                            modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection),
                            showLyricsOnClick = true
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    ) {
                        Spacer(Modifier.weight(1f))

                        mediaMetadata?.let {
                            controlsContent(it)
                        }

                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .padding(bottom = queueSheetState.collapsedBound)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f)
                    ) {
                        Thumbnail(
                            sliderPositionProvider = { sliderPosition },
                            modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection),
                            showLyricsOnClick = true
                        )
                    }

                    mediaMetadata?.let {
                        controlsContent(it)
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        Queue(
            state = queueSheetState,
            backgroundColor =
                if (useBlackBackground) {
                    Color.Black
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            navController = navController,
            onBackgroundColor = onBackgroundColor,
        )
    }
}
