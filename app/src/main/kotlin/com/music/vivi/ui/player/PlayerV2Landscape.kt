/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.ui.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.music.vivi.ui.menu.ListenTogetherDialog
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.music.vivi.BuildConfig
import com.music.vivi.LocalListenTogetherManager
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import coil3.request.crossfade
import com.music.vivi.constants.CrossfadeDurationKey
import com.music.vivi.constants.CrossfadeEnabledKey
import com.music.vivi.constants.CropAlbumArtKey
import com.music.vivi.constants.EnableGoogleCastKey
import com.music.vivi.constants.HidePlayerThumbnailKey
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp
import com.music.vivi.ui.component.Icon as MIcon
import com.music.vivi.constants.PlayerButtonsStyle
import com.music.vivi.constants.PlayerButtonsStyleKey
import com.music.vivi.constants.UseNewPlayerDesignKey
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.constants.PlayerThumbnailShadowElevationKey
import com.music.vivi.constants.ShowPlayerThumbnailShadowKey
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.SwipeGesture
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.listentogether.RoomRole
import com.music.vivi.models.MediaMetadata
import com.music.vivi.playback.CastConnectionHandler
import com.music.vivi.ui.component.BottomSheetState
import com.music.vivi.ui.component.CastButton
import com.music.vivi.ui.component.LocalBottomSheetPageState
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.LyricsV2
import com.music.vivi.ui.component.PlayerSliderTrack
import com.music.vivi.ui.menu.LyricsMenu
import com.music.vivi.ui.menu.OldPlayerMenu
import com.music.vivi.ui.theme.PlayerSliderColors
import com.music.vivi.ui.theme.SfProDisplayFontFamily
import com.music.vivi.ui.utils.ShowMediaInfo
import com.music.vivi.ui.utils.ShowOffsetDialog
import com.music.vivi.ui.utils.resize
import com.music.vivi.utils.makeTimeString
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.vivimusic.AudioDeviceBottomSheet
import com.music.vivi.vivimusic.getConnectedBluetoothDeviceName
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.music.vivi.ui.screens.settings.integrations.ListenTogetherAvatarList

@Composable
fun PlayerV2Landscape(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    var playerState by remember { mutableStateOf(PlayerInternalState.COVER) }

    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsState(initial = RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST
    val listenTogetherRoomState by listenTogetherManager?.roomState?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }
    val hostUser = remember(listenTogetherRoomState) {
        listenTogetherRoomState?.users?.firstOrNull { it.isHost || it.userId == listenTogetherRoomState?.hostId }
    }
    val hostName = hostUser?.username
    val hostAvatarRes = remember(hostUser?.avatarIndex) {
        val index = hostUser?.avatarIndex ?: 0
        ListenTogetherAvatarList.getOrElse(index) { R.drawable.person }
    }
    var showListenTogetherDialog by remember { mutableStateOf(false) }
    val isMuted by playerConnection.isMuted.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    // Cast state
    val castHandler = remember(playerConnection) {
        try { playerConnection.service.castConnectionHandler } catch (e: Exception) { null }
    }
    val isCasting by castHandler?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) }
    val castPosition by castHandler?.castPosition?.collectAsState() ?: remember { mutableLongStateOf(0L) }
    val castDuration by castHandler?.castDuration?.collectAsState() ?: remember { mutableLongStateOf(0L) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val castIsBuffering by castHandler?.castIsBuffering?.collectAsState() ?: remember { mutableStateOf(false) }
    val castVolume by castHandler?.castVolume?.collectAsState() ?: remember { mutableFloatStateOf(1f) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying
    var lastManualSeekTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = playerState != PlayerInternalState.COVER) {
        playerState = PlayerInternalState.COVER
    }

    var position by remember {
        mutableLongStateOf(
            try { playerConnection.player.currentPosition.coerceAtLeast(0L) } catch (_: Exception) { 0L }
        )
    }
    var duration by remember {
        mutableLongStateOf(
            try {
                val raw = playerConnection.player.duration
                if (raw == C.TIME_UNSET || raw < 0) 0L else raw
            } catch (_: Exception) { 0L }
        )
    }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxSystemVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

    var systemVolume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxSystemVolume)
    }
    val animatedVolume by animateFloatAsState(
        targetValue = systemVolume,
        animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing),
        label = "landscapeVolumeAnimation"
    )
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxSystemVolume
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
        onDispose { context.unregisterReceiver(receiver) }
    }

    val storedPlayerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.LIVE_MESH
    )
    val playerBackground = if (storedPlayerBackground == PlayerBackgroundStyle.APPLE_MUSIC) {
        PlayerBackgroundStyle.BLUR
    } else {
        storedPlayerBackground
    }

    val showPlayerThumbnailShadow by rememberPreference(ShowPlayerThumbnailShadowKey, defaultValue = false)
    val playerThumbnailShadowElevation by rememberPreference(PlayerThumbnailShadowElevationKey, defaultValue = 8f)
    val enableGoogleCast by rememberPreference(EnableGoogleCastKey, defaultValue = true)
    val hidePlayerThumbnail by rememberPreference(HidePlayerThumbnailKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )
    val (useNewPlayerDesign) = rememberPreference(
        UseNewPlayerDesignKey,
        defaultValue = false
    )
    val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val (textButtonColor, _) = when {
        playerBackground == PlayerBackgroundStyle.BLUR || 
        playerBackground == PlayerBackgroundStyle.GRADIENT ||
        playerBackground == PlayerBackgroundStyle.GLOW_ANIMATED ||
        playerBackground == PlayerBackgroundStyle.APPLE_MUSIC ||
        playerBackground == PlayerBackgroundStyle.LIVE_MESH -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT -> Pair(Color.White, Color.Black)
                PlayerButtonsStyle.PRIMARY -> Pair(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onPrimary
                )
                PlayerButtonsStyle.TERTIARY -> Pair(
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.onTertiary
                )
            }
        }
        else -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT ->
                    if (useDarkTheme) Pair(Color.White, Color.Black)
                    else Pair(Color.Black, Color.White)
                PlayerButtonsStyle.PRIMARY -> Pair(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onPrimary
                )
                PlayerButtonsStyle.TERTIARY -> Pair(
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.onTertiary
                )
            }
        }
    }

    val sliderActiveColor = if (useNewPlayerDesign) textButtonColor else textButtonColor.copy(alpha = 0.7f)

    var showAudioDeviceBottomSheet by remember { mutableStateOf(false) }

    val bluetoothDeviceName by produceState<String?>(initialValue = getConnectedBluetoothDeviceName(context)) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val scope = this
        val updateDeviceState: () -> Unit = {
            scope.value = getConnectedBluetoothDeviceName(context)
            scope.launch {
                delay(500)
                scope.value = getConnectedBluetoothDeviceName(context)
            }
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                updateDeviceState()
            }
        }
        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            object : android.media.AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    updateDeviceState()
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    updateDeviceState()
                }
            }
        } else null

        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction("android.bluetooth.adapter.action.STATE_CHANGED")
            addAction("android.bluetooth.device.action.ACL_CONNECTED")
            addAction("android.bluetooth.device.action.ACL_DISCONNECTED")
            addAction("android.media.AUDIO_BECOMING_NOISY")
        }

        context.registerReceiver(receiver, filter)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && callback != null) {
            am.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        }

        awaitDispose {
            context.unregisterReceiver(receiver)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && callback != null) {
                am.unregisterAudioDeviceCallback(callback)
            }
        }
    }

    // Position update — only poll local player when not casting
    LaunchedEffect(isPlaying, isCasting) {
        if (!isCasting && isPlaying) {
            while (isActive) {
                delay(100)
                if (sliderPosition == null) {
                    val rawDuration = playerConnection.player.duration
                    position = playerConnection.player.currentPosition.coerceAtLeast(0L)
                    duration = if (rawDuration == C.TIME_UNSET || rawDuration < 0) 0L else rawDuration
                }
            }
        }
    }

    // Also update position when playback state changes (e.g., song change, seek, restoration)
    LaunchedEffect(playbackState, mediaMetadata?.id) {
        if (!isCasting) {
            val rawDuration = playerConnection.player.duration
            position = playerConnection.player.currentPosition.coerceAtLeast(0L)
            duration = if (rawDuration == C.TIME_UNSET || rawDuration < 0) 0L else rawDuration
        }
    }

    // Capture seek and position discontinuity events immediately, even while paused
    DisposableEffect(playerConnection.player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPositionDiscontinuity(
                oldPosition: androidx.media3.common.Player.PositionInfo,
                newPosition: androidx.media3.common.Player.PositionInfo,
                reason: Int
            ) {
                if (!isCasting && sliderPosition == null) {
                    val rawDuration = playerConnection.player.duration
                    position = playerConnection.player.currentPosition.coerceAtLeast(0L)
                    duration = if (rawDuration == C.TIME_UNSET || rawDuration < 0) 0L else rawDuration
                }
            }
        }
        playerConnection.player.addListener(listener)
        onDispose {
            playerConnection.player.removeListener(listener)
        }
    }

    // Sync cast position+duration when casting
    LaunchedEffect(isCasting, castPosition, castDuration) {
        if (isCasting && sliderPosition == null) {
            val timeSinceManualSeek = System.currentTimeMillis() - lastManualSeekTime
            if (timeSinceManualSeek > 1500) {
                position = castPosition
                if (castDuration > 0) duration = castDuration
            }
        }
    }

    val adaptivePrimary by animateColorAsState(
        targetValue = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onSurface
            PlayerBackgroundStyle.BLUR,
            PlayerBackgroundStyle.GRADIENT,
            PlayerBackgroundStyle.GLOW_ANIMATED,
            PlayerBackgroundStyle.APPLE_MUSIC,
            PlayerBackgroundStyle.LIVE_MESH -> Color.White
        },
        label = "adaptivePrimary"
    )
    val adaptiveSecondary by animateColorAsState(
        targetValue = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onSurfaceVariant
            PlayerBackgroundStyle.BLUR,
            PlayerBackgroundStyle.GRADIENT,
            PlayerBackgroundStyle.GLOW_ANIMATED,
            PlayerBackgroundStyle.APPLE_MUSIC,
            PlayerBackgroundStyle.LIVE_MESH -> Color.White.copy(alpha = 0.7f)
        },
        label = "adaptiveSecondary"
    )
    val adaptiveSurface by animateColorAsState(
        targetValue = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surfaceVariant
            PlayerBackgroundStyle.BLUR,
            PlayerBackgroundStyle.GRADIENT,
            PlayerBackgroundStyle.GLOW_ANIMATED,
            PlayerBackgroundStyle.APPLE_MUSIC,
            PlayerBackgroundStyle.LIVE_MESH -> Color.White.copy(alpha = 0.2f)
        },
        label = "adaptiveSurface"
    )

    val crossfadeEnabled by rememberPreference(CrossfadeEnabledKey, defaultValue = false)
    val crossfadeDuration by rememberPreference(CrossfadeDurationKey, defaultValue = 6f)
    val isCrossfadingService by playerConnection.isCrossfading.collectAsState()
    val crossfadeOutgoingItem by playerConnection.crossfadeOutgoingItem.collectAsState()
    val crossfadeIncomingItem by playerConnection.crossfadeIncomingItem.collectAsState()
    val crossfadeProgressService by playerConnection.crossfadeProgress.collectAsState()
    val nextMediaMetadata by playerConnection.nextMediaMetadata.collectAsState()

    val isTransitioning = isCrossfadingService
    val isMixing = isTransitioning && crossfadeEnabled
    val mixAlpha by animateFloatAsState(
        targetValue = if (isMixing) 1f else 0f,
        animationSpec = tween(300),
        label = "mixAlpha"
    )

    val coverProgress by animateFloatAsState(
        targetValue = if (isTransitioning) crossfadeProgressService else 0f,
        animationSpec = tween(durationMillis = 150, easing = LinearEasing),
        label = "coverProgress"
    )

    val displayedMetadata = if (isTransitioning && crossfadeOutgoingItem != null) {
        crossfadeOutgoingItem
    } else {
        mediaMetadata
    }

    val fromCoverUrl = if (isTransitioning && crossfadeOutgoingItem != null) {
        crossfadeOutgoingItem?.thumbnailUrl
    } else {
        mediaMetadata?.thumbnailUrl
    }

    val toCoverUrl = if (isTransitioning) {
        crossfadeIncomingItem?.thumbnailUrl ?: nextMediaMetadata?.thumbnailUrl
    } else {
        null
    }

    val mixingInfiniteTransition = rememberInfiniteTransition(label = "MixingShimmerLandscape")
    val mixingShimmerTranslate by mixingInfiniteTransition.animateFloat(
        initialValue = -80f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mixingShimmerTranslateLandscape"
    )
    val mixingShimmerBrush = remember(mixingShimmerTranslate, adaptiveSecondary) {
        Brush.linearGradient(
            colors = listOf(
                adaptiveSecondary.copy(alpha = 0.5f),
                Color.White,
                Color.White,
                adaptiveSecondary.copy(alpha = 0.5f)
            ),
            start = Offset(mixingShimmerTranslate, 0f),
            end = Offset(mixingShimmerTranslate + 60f, 0f)
        )
    }

    val onSeek: (Long) -> Unit = { pos ->
        if (duration == C.TIME_UNSET || duration <= 0) {
            sliderPosition = null
        } else {
            if (isCasting) {
                castHandler?.seekTo(pos)
                lastManualSeekTime = System.currentTimeMillis()
            } else {
                playerConnection.player.seekTo(pos)
            }
            position = pos
        }
    }

    val onVolumeChange: (Float) -> Unit = { newValue ->
        if (isCasting) {
            castHandler?.setVolume(newValue)
        } else {
            systemVolume = newValue
            val targetVolume = (newValue * maxSystemVolume).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (storedPlayerBackground == PlayerBackgroundStyle.APPLE_MUSIC) {
            if (toCoverUrl != null && fromCoverUrl != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(fromCoverUrl)
                            .size(100, 100)
                            .crossfade(500)
                            .allowHardware(false)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = 1f - coverProgress }
                            .blur(120.dp)
                    )
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(toCoverUrl)
                            .size(100, 100)
                            .crossfade(500)
                            .allowHardware(false)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = coverProgress }
                            .blur(120.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.30f))
                    )
                }
            } else {
                displayedMetadata?.thumbnailUrl?.let { thumbnailUrl ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbnailUrl)
                            .size(100, 100)
                            .crossfade(500)
                            .allowHardware(false)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(120.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.30f))
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // LEFT SIDE: Centered dynamic thumbnail + Bottom Row of Lyrics, Queue, Cast/Headphones
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dynamic Thumbnail container (scales to available space)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val maxDimension = minOf(maxWidth * 0.92f, maxHeight - 8.dp)
                val thumbnailSize = maxDimension.coerceAtLeast(140.dp)

                // Thumbnail Box
                Box(
                    modifier = Modifier
                        .size(thumbnailSize)
                    .aspectRatio(1f)
                    .customSoftShadow(
                        elevation = playerThumbnailShadowElevation.dp / 2f,
                        cornerRadius = 14.dp,
                        enabled = showPlayerThumbnailShadow
                    )
                    .background(adaptiveSurface, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        if (playerState != PlayerInternalState.COVER) {
                            playerState = PlayerInternalState.COVER
                        }
                    }
                    .SwipeGesture(
                        enabled = (playerState == PlayerInternalState.COVER && !isListenTogetherGuest),
                        onSwipeLeft = { if (canSkipNext) playerConnection.player.seekToNext() },
                        onSwipeRight = { if (canSkipPrevious) playerConnection.player.seekToPrevious() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (hidePlayerThumbnail) {
                    Icon(
                        painter = painterResource(R.drawable.vivi_music_small_icon),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = adaptivePrimary.copy(alpha = 0.5f)
                    )
                } else {
                    Blendingimage(
                        fromCoverUrl = fromCoverUrl,
                        toCoverUrl = toCoverUrl,
                        nextCoverUrl = nextMediaMetadata?.thumbnailUrl,
                        progress = coverProgress,
                        crossfadeDuration = crossfadeDuration,
                        crossfadeEnabled = crossfadeEnabled,
                        cropAlbumArt = cropAlbumArt,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (toCoverUrl == null) {
                        PlayerV2Motionartowek(
                            mediaMetadata = displayedMetadata,
                            isPlaying = isPlaying,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Row of Lyrics, Cast/Headphones, Queue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Lyrics Button (Left)
                val isLyricsActive = playerState == PlayerInternalState.LYRICS
                IconButton(
                    onClick = {
                        playerState = if (isLyricsActive) PlayerInternalState.COVER else PlayerInternalState.LYRICS
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLyricsActive) adaptivePrimary.copy(alpha = 0.2f) else Color.Transparent
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lyrics_apple),
                        contentDescription = "Lyrics",
                        tint = if (isLyricsActive) adaptivePrimary else adaptiveSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // 2. Cast / Headphone / Audio Device Button (Center)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (bluetoothDeviceName != null) {
                        IconButton(
                            onClick = { showAudioDeviceBottomSheet = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.headphones_apple),
                                contentDescription = "Audio Device",
                                tint = adaptiveSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else if (BuildConfig.CAST_AVAILABLE && enableGoogleCast) {
                        CastButton(
                            modifier = Modifier.size(48.dp),
                            tintColor = adaptiveSecondary,
                            activeTintColor = adaptivePrimary,
                            showBackground = false,
                            customIconRes = R.drawable.airplay_audio
                        )
                    } else {
                        IconButton(
                            onClick = { showAudioDeviceBottomSheet = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.airplay_audio),
                                contentDescription = "Audio Routing",
                                tint = adaptiveSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // 3. Queue Button (Right)
                val isQueueActive = playerState == PlayerInternalState.QUEUE
                IconButton(
                    onClick = {
                        playerState = if (isQueueActive) PlayerInternalState.COVER else PlayerInternalState.QUEUE
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isQueueActive) adaptivePrimary.copy(alpha = 0.2f) else Color.Transparent
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.apple_queue),
                        contentDescription = "Queue",
                        tint = if (isQueueActive) adaptivePrimary else adaptiveSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        // RIGHT SIDE: Raw controls (Cover) OR Pure Lyrics (Lyrics) OR Pure Queue (Queue)
        // No extra box or card behind anything, completely transparent on player background.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = playerState,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) togetherWith
                     fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)))
                        .apply { targetContentZIndex = 1f }
                },
                modifier = Modifier.fillMaxSize(),
                label = "LandscapeRightSideContent"
            ) { targetState ->
                when (targetState) {
                    PlayerInternalState.COVER -> {
                        // Song name, artist name, slider, volume, playback controls
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            // Song Title, Artist & Action Buttons (Star, More)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    if (isListenTogetherGuest && !hostName.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = adaptiveSecondary.copy(alpha = 0.16f),
                                            modifier = Modifier
                                                .padding(bottom = 6.dp)
                                                .clickable { showListenTogetherDialog = true }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 3.dp)
                                            ) {
                                                Image(
                                                    painter = painterResource(hostAvatarRes),
                                                    contentDescription = "Host Avatar",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                )
                                                Text(
                                                    text = hostName,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 12.sp
                                                    ),
                                                    color = adaptivePrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = displayedMetadata?.title ?: "Unknown",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontFamily = SfProDisplayFontFamily,
                                        color = adaptivePrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                            .clickable {
                                                displayedMetadata?.album?.id?.let {
                                                    navController.navigate("album/$it")
                                                    state.collapseSoft()
                                                }
                                            }
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val lArtists = displayedMetadata?.artists.orEmpty().filter { it.name.isNotBlank() }
                                    if (lArtists.isNotEmpty()) {
                                        val annotatedString = buildAnnotatedString {
                                            withStyle(
                                                SpanStyle(
                                                    color = adaptiveSecondary,
                                                    fontFamily = SfProDisplayFontFamily,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Normal
                                                )
                                            ) {
                                                lArtists.forEachIndexed { index, artist ->
                                                    val tag = "artist_${artist.id.orEmpty()}"
                                                    pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty())
                                                    append(artist.name)
                                                    pop()
                                                    if (index != lArtists.lastIndex) append(", ")
                                                }
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (displayedMetadata?.explicit == true) {
                                                MIcon.Explicit(tint = adaptiveSecondary)
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                            ) {
                                                var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                                var clickOffset by remember { mutableStateOf<Offset?>(null) }
                                                Text(
                                                    text = annotatedString,
                                                    color = adaptiveSecondary,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    onTextLayout = { layoutResult = it },
                                                    modifier = Modifier
                                                        .pointerInput(Unit) {
                                                            awaitPointerEventScope {
                                                                while (true) {
                                                                    val event = awaitPointerEvent()
                                                                    val tapPosition = event.changes.firstOrNull()?.position
                                                                    if (tapPosition != null) {
                                                                        clickOffset = tapPosition
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        .clickable(
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            indication = null
                                                        ) {
                                                            val tapPosition = clickOffset
                                                            val layout = layoutResult
                                                            if (tapPosition != null && layout != null) {
                                                                val offset = layout.getOffsetForPosition(tapPosition)
                                                                annotatedString
                                                                    .getStringAnnotations(offset, offset)
                                                                    .firstOrNull()
                                                                    ?.let { ann ->
                                                                        val artistId = ann.item
                                                                        if (artistId.isNotBlank()) {
                                                                            navController.navigate("artist/$artistId")
                                                                            state.collapseSoft()
                                                                        }
                                                                    }
                                                            }
                                                        }
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Unknown",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontFamily = SfProDisplayFontFamily,
                                            color = adaptiveSecondary,
                                            fontWeight = FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isLiked = currentSong?.song?.liked == true
                                    IconButton(
                                        onClick = playerConnection::toggleLike,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (isLiked) R.drawable.ic_nowplaying_favorited else R.drawable.ic_nowplaying_favorite
                                            ),
                                            contentDescription = "Like",
                                            tint = adaptivePrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                OldPlayerMenu(
                                                    mediaMetadata = mediaMetadata,
                                                    navController = navController,
                                                    playerBottomSheetState = state,
                                                    onShowDetailsDialog = {
                                                        mediaMetadata?.id?.let {
                                                            bottomSheetPageState.show {
                                                                ShowMediaInfo(it)
                                                            }
                                                        }
                                                    },
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_nowplaying_more),
                                            contentDescription = "More",
                                            tint = adaptivePrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Progress Slider & Timestamps
                            Column(modifier = Modifier.fillMaxWidth()) {
                                val currentPos = sliderPosition ?: position
                                val trackInteractionSource = remember { MutableInteractionSource() }
                                val isTrackDragged by trackInteractionSource.collectIsDraggedAsState()
                                val isTrackPressed by trackInteractionSource.collectIsPressedAsState()
                                val isTrackActive = isTrackDragged || isTrackPressed

                                val trackHeight by animateDpAsState(
                                    targetValue = if (isTrackActive) 10.dp else 7.25.dp,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "landscapeTrackScale"
                                )

                                Slider(
                                    value = currentPos.toFloat(),
                                    valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                    onValueChange = { value ->
                                        if (!isListenTogetherGuest) {
                                            sliderPosition = value.toLong()
                                        }
                                    },
                                    onValueChangeFinished = {
                                        if (!isListenTogetherGuest) {
                                            sliderPosition?.let { pos ->
                                                onSeek(pos)
                                            }
                                            sliderPosition = null
                                        }
                                    },
                                    interactionSource = trackInteractionSource,
                                    thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                                    track = { sliderState ->
                                        PlayerSliderTrack(
                                            sliderState = sliderState,
                                            trackHeight = trackHeight,
                                            colors = PlayerSliderColors.getSliderColors(
                                                activeColor = sliderActiveColor,
                                                playerBackground = playerBackground,
                                                useDarkTheme = useDarkTheme
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = makeTimeString(currentPos),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = adaptiveSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (crossfadeEnabled && (mixAlpha > 0.001f || isMixing)) {
                                        Text(
                                            text = "Mixing",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                brush = mixingShimmerBrush,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            modifier = Modifier.graphicsLayer { alpha = mixAlpha }
                                        )
                                    }
                                    Text(
                                        text = if (duration != C.TIME_UNSET && duration > 0) {
                                            val remaining = duration - currentPos
                                            "-${makeTimeString(remaining.coerceAtLeast(0L))}"
                                        } else {
                                            makeTimeString(duration)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = adaptiveSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Playback Controls Row (Naked Transparent Controls)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous
                                IconButton(
                                    onClick = {
                                        if (!isListenTogetherGuest) {
                                            if (isCasting) castHandler?.skipToPrevious()
                                            else if (canSkipPrevious) playerConnection.player.seekToPrevious()
                                        }
                                    },
                                    enabled = !isListenTogetherGuest && canSkipPrevious,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .alpha(if (isListenTogetherGuest || !canSkipPrevious) 0.4f else 1f)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.apple_skip_previous),
                                        contentDescription = "Previous",
                                        tint = adaptivePrimary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                // Play / Pause
                                IconButton(
                                    onClick = {
                                        if (isListenTogetherGuest) {
                                            playerConnection.toggleMute()
                                        } else if (isCasting) {
                                            if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                                        } else {
                                            playerConnection.player.togglePlayPause()
                                        }
                                    },
                                    modifier = Modifier.size(76.dp)
                                ) {
                                    if (isListenTogetherGuest) {
                                        Icon(
                                            painter = painterResource(if (isMuted) R.drawable.volume_off else R.drawable.volume_up),
                                            contentDescription = if (isMuted) "Unmute" else "Mute",
                                            modifier = Modifier.size(56.dp),
                                            tint = adaptivePrimary
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(
                                                if (effectiveIsPlaying) R.drawable.pause_applemusic else R.drawable.play_applemusic
                                            ),
                                            contentDescription = if (effectiveIsPlaying) "Pause" else "Play",
                                            modifier = Modifier.size(68.dp),
                                            tint = adaptivePrimary
                                        )
                                    }
                                }

                                // Next
                                IconButton(
                                    onClick = {
                                        if (!isListenTogetherGuest) {
                                            if (isCasting) castHandler?.skipToNext()
                                            else if (canSkipNext) playerConnection.player.seekToNext()
                                        }
                                    },
                                    enabled = !isListenTogetherGuest && canSkipNext,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .alpha(if (isListenTogetherGuest || !canSkipNext) 0.4f else 1f)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.apple_skip_next),
                                        contentDescription = "Next",
                                        tint = adaptivePrimary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Volume Slider Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                            Icon(
                                painterResource(R.drawable.ic_speaker_24dp),
                                contentDescription = "Volume Down",
                                tint = adaptiveSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                                val volumeInteractionSource = remember { MutableInteractionSource() }
                            val isVolDragged by volumeInteractionSource.collectIsDraggedAsState()
                            val isVolPressed by volumeInteractionSource.collectIsPressedAsState()
                            val isVolActive = isVolDragged || isVolPressed

                            val volHeight by animateDpAsState(
                                targetValue = if (isVolActive) 10.dp else 7.25.dp,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "landscapeVolHeight"
                            )

                            Slider(
                                value = if (isCasting) castVolume
                                else if (isVolActive) systemVolume
                                else animatedVolume,
                                onValueChange = { newValue ->
                                    onVolumeChange(newValue)
                                },
                                interactionSource = volumeInteractionSource,
                                thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                                track = { sliderState ->
                                    PlayerSliderTrack(
                                        sliderState = sliderState,
                                        trackHeight = volHeight,
                                        colors = PlayerSliderColors.getSliderColors(
                                            activeColor = sliderActiveColor,
                                            playerBackground = playerBackground,
                                            useDarkTheme = useDarkTheme
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                painterResource(R.drawable.ic_speaker_wave_3_24dp),
                                contentDescription = "Volume Up",
                                tint = adaptiveSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                    PlayerInternalState.LYRICS -> {
                        // In landscape, when lyrics pressed: pure LyricsV2 with floating circular action buttons (Star on left, Options on right)
                        Box(modifier = Modifier.fillMaxSize()) {
                            AnimatedContent(
                                targetState = currentLyrics?.id to currentLyrics?.lyrics,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) togetherWith
                                     fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)))
                                        .apply { targetContentZIndex = 1f }
                                },
                                modifier = Modifier.fillMaxSize(),
                                label = "LandscapeLyricsContentTransition"
                            ) { _ ->
                                LyricsV2(
                                    mediaMetadata = mediaMetadata,
                                    showLyrics = true,
                                    sliderPositionProvider = { sliderPosition },
                                    positionProvider = { sliderPosition ?: position },
                                    initialLyrics = currentLyrics,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Small compact circular star/like button at bottom left of lyrics
                            val isLiked = currentSong?.song?.liked == true
                            IconButton(
                                onClick = playerConnection::toggleLike,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 16.dp, bottom = 16.dp)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isLiked) R.drawable.ic_nowplaying_favorited else R.drawable.ic_nowplaying_favorite
                                    ),
                                    contentDescription = "Like",
                                    tint = adaptivePrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Small compact circular more_vert button at bottom right to open LyricsMenu
                            IconButton(
                                onClick = {
                                    mediaMetadata?.let { metadata ->
                                        menuState.show {
                                            LyricsMenu(
                                                lyricsProvider = { currentLyrics },
                                                songProvider = { currentSong?.song },
                                                mediaMetadataProvider = { metadata },
                                                onDismiss = menuState::dismiss,
                                                onShowOffsetDialog = {
                                                    bottomSheetPageState.show {
                                                        ShowOffsetDialog(
                                                            songProvider = { currentSong?.song }
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 16.dp, bottom = 16.dp)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_nowplaying_more),
                                    contentDescription = "Lyrics Options",
                                    tint = adaptivePrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    PlayerInternalState.QUEUE -> {
                        // In landscape, when queue pressed: pure QueueV2, NO top header with song name/star/more
                        QueueV2(
                            navController = navController,
                            playerBottomSheetState = state,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

    if (showAudioDeviceBottomSheet) {
        AudioDeviceBottomSheet(
            onDismiss = { showAudioDeviceBottomSheet = false }
        )
    }

    if (showListenTogetherDialog) {
        ListenTogetherDialog(
            visible = showListenTogetherDialog,
            mediaMetadata = mediaMetadata,
            onDismiss = { showListenTogetherDialog = false }
        )
    }
}
