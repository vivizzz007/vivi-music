@file:Suppress("NAME_SHADOWING")

package com.music.vivi.ui.player

import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.res.stringResource
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
import com.music.vivi.constants.fullScreenLyricsKey
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



import android.graphics.Bitmap
import android.media.audiofx.AudioEffect
//import android.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.playback.ExoDownloadService
import kotlinx.coroutines.launch





@RequiresApi(Build.VERSION_CODES.S)
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

    var showMoreOptionsSheet by remember { mutableStateOf(false) }
    val showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)
    val fullScreenLyrics by rememberPreference(fullScreenLyricsKey, defaultValue = true)
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.SQUIGGLY)

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)

    val context = LocalContext.current
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val playerBackground by rememberEnumPreference(key = PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.BLUR)
    val (playerStyle) = rememberEnumPreference(PlayerStyleKey, defaultValue = PlayerStyle.NEW)

    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val onBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
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

    // States for share functionality
    var showShareOptionsSheet by remember { mutableStateOf(false) }
    var showQrCodeSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // QR Code generation
    val shareLink = remember(mediaMetadata?.id) {
        mediaMetadata?.id?.let { "https://music.youtube.com/watch?v=$it" } ?: ""
    }
    val qrCodeBitmap = remember(shareLink) {
        try {
            val bitMatrix = QRCodeWriter().encode(
                shareLink,
                BarcodeFormat.QR_CODE,
                512,
                512
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        setPixel(x, y, if (bitMatrix.get(x, y)) Color.Black.hashCode() else Color.White.hashCode())
                    }
                }
            }.asImageBitmap()
        } catch (e: Exception) {
            null
        }
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
            useDarkTheme || playerBackground == PlayerBackgroundStyle.DEFAULT ->
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

            if (fullScreenLyrics) {
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

                        // ADDED FAVORITE BUTTON HERE
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

                        Spacer(modifier = Modifier.width(5.dp))
                        val downloadUtil = LocalDownloadUtil.current
                        val download by downloadUtil.getDownload(mediaMetadata?.id ?: "").collectAsState(initial = null)

                        Box(
                            modifier = Modifier
                                .offset(y = 5.dp)
                                .size(36.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            ResizableIconButton(
                                icon = when (download?.state) {
                                    Download.STATE_DOWNLOADING -> R.drawable.downloading_icon
                                    Download.STATE_COMPLETED -> R.drawable.downloaded_icon
                                    else -> R.drawable.download
                                },
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(24.dp),
                                onClick = {
                                    when (download?.state) {
                                        Download.STATE_COMPLETED -> {
                                            DownloadService.sendRemoveDownload(
                                                context,
                                                ExoDownloadService::class.java,
                                                mediaMetadata?.id ?: return@ResizableIconButton,
                                                false
                                            )
                                        }
                                        else -> {
                                            mediaMetadata?.let { metadata ->
                                                val downloadRequest = DownloadRequest.Builder(metadata.id, metadata.id.toUri())
                                                    .setCustomCacheKey(metadata.id)
                                                    .setData(metadata.title.toByteArray())
                                                    .build()
                                                DownloadService.sendAddDownload(
                                                    context,
                                                    ExoDownloadService::class.java,
                                                    downloadRequest,
                                                    false
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        if (showMoreOptionsSheet) {
                            MoreOptionsSheet(
                                mediaMetadata = mediaMetadata,
                                navController = navController,
                                bottomSheetState = state,
                                onDismiss = { showMoreOptionsSheet = false },
                                onShowDetailsDialog = { showDetailsDialog = true }
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
        }

        // Rest of the code remains unchanged...
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
                            .blur(100.dp)
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
                        .blur(100.dp)
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

        if (!showLyrics) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding)
                        .padding(top = 35.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ResizableIconButton(
                        icon = R.drawable.arrows_down,
                        modifier = Modifier.size(25.dp),
                        color = onBackgroundColor,
                        onClick = {
                            state.collapseSoft()
                        }
                    )

                    if (mediaMetadata != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.now_playing),
                                style = MaterialTheme.typography.titleMedium,
                                overflow = TextOverflow.Ellipsis,
                                color = onBackgroundColor
                            )
                            Text(
                                text = mediaMetadata!!.title,
                                style = MaterialTheme.typography.bodyMedium,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = onBackgroundColor
                            )
                        }
                    }

                    ResizableIconButton(
                        icon = R.drawable.share,
                        modifier = Modifier.size(25.dp),
                        color = onBackgroundColor,
                        onClick = {
                            scope.launch {
                                showShareOptionsSheet = true
                            }
                        }
                    )
                }
            }
        }

        if (fullScreenLyrics) {
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

    // Share Options Bottom Sheet
    if (showShareOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showShareOptionsSheet = false
            }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.share_song),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                showShareOptionsSheet = false
                                val intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareLink)
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                            }
                        }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.link_icon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.share_link))
                }

                Divider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                showShareOptionsSheet = false
                                showQrCodeSheet = true
                            }
                        }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.qr_code_icon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.share_qr_code))
                }
            }
        }
    }

    // QR Code Bottom Sheet
    if (showQrCodeSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showQrCodeSheet = false
                showShareOptionsSheet = true
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.share_via_qr),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (qrCodeBitmap != null) {
                    Image(
                        bitmap = qrCodeBitmap,
                        contentDescription = stringResource(R.string.qr_code),
                        modifier = Modifier.size(200.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                mediaMetadata?.let { metadata ->
                    Text(
                        text = metadata.title,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = shareLink,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        showQrCodeSheet = false
                        showShareOptionsSheet = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Text(text = stringResource(R.string.cancels))
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsSheet(
    mediaMetadata: MediaMetadata?,
    navController: NavController,
    bottomSheetState: BottomSheetState,
    onDismiss: () -> Unit,
    onShowDetailsDialog: () -> Unit
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Song information header
            mediaMetadata?.let { metadata ->
                Row(
                    modifier = Modifier.padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = metadata.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = metadata.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = metadata.artists.joinToString { it.name },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Divider()

            // Options list
            Column {
                // Add to playlist
                MoreOptionItem(
                    icon = R.drawable.playlist_add,
                    text = stringResource(R.string.add_to_playlist),
                    onClick = {
                        mediaMetadata?.let {
                            navController.navigate("add_to_playlist/${it.id}")
                            bottomSheetState.collapseSoft()
                            onDismiss()
                        }
                    }
                )

                // View album
                MoreOptionItem(
                    icon = R.drawable.album,
                    text = stringResource(R.string.view_album),
                    enabled = mediaMetadata?.album != null && !mediaMetadata.isLocal,
                    onClick = {
                        mediaMetadata?.album?.let {
                            navController.navigate("album/${it.id}")
                            bottomSheetState.collapseSoft()
                            onDismiss()
                        }
                    }
                )

                // View artist
                MoreOptionItem(
                    icon = R.drawable.person,
                    text = stringResource(R.string.view_artist),
                    enabled = mediaMetadata?.artists?.firstOrNull()?.id != null && !mediaMetadata.isLocal,
                    onClick = {
                        mediaMetadata?.artists?.firstOrNull()?.id?.let {
                            navController.navigate("artist/$it")
                            bottomSheetState.collapseSoft()
                            onDismiss()
                        }
                    }
                )

                // Song details
                MoreOptionItem(
                    icon = R.drawable.info,
                    text = stringResource(R.string.song_details),
                    onClick = {
                        onShowDetailsDialog()
                        onDismiss()
                    }
                )

                // Sleep timer
                MoreOptionItem(
                    icon = R.drawable.icon,
                    text = stringResource(R.string.sleep_timer),
                    onClick = {
                        // Implement sleep timer functionality
                        onDismiss()
                    }
                )

                // Equalizer
                MoreOptionItem(
                    icon = R.drawable.equalizer,
                    text = stringResource(R.string.equalizer),
                    onClick = {
                        // Open equalizer
                        try {
                            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, playerConnection?.player?.audioSessionId)
                                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Handle error
                        }
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun MoreOptionItem(
    icon: Int,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .alpha(if (enabled) 1f else 0.5f),
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}