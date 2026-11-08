/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.ui.player

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import androidx.media3.common.C
import androidx.media3.common.Player
import com.music.vivi.LocalPlayerConnection
import androidx.compose.ui.graphics.graphicsLayer
import com.music.vivi.constants.CropAlbumArtKey
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
import com.music.vivi.constants.PlayerButtonsStyle
import com.music.vivi.constants.PlayerButtonsStyleKey
import com.music.vivi.constants.UseNewPlayerDesignKey
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.constants.ShowPlayerThumbnailShadowKey
import com.music.vivi.constants.PlayerThumbnailShadowElevationKey
import com.music.vivi.constants.EnableGoogleCastKey
import com.music.vivi.constants.HidePlayerThumbnailKey
import com.music.vivi.constants.KeepScreenOn
import com.music.vivi.constants.CrossfadeEnabledKey
import com.music.vivi.constants.CrossfadeDurationKey
import kotlin.math.roundToInt
import com.music.vivi.models.MediaMetadata
import com.music.vivi.ui.component.BottomSheetState
import com.music.vivi.ui.component.PlayerSliderTrack
import com.music.vivi.ui.theme.PlayerSliderColors
import com.music.vivi.ui.theme.SfProDisplayFontFamily
import com.music.vivi.utils.makeTimeString
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.ui.component.LyricsV2
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import com.music.vivi.ui.component.LocalBottomSheetPageState
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.menu.LyricsMenu
import com.music.vivi.ui.menu.OldPlayerMenu
import com.music.vivi.ui.utils.ShowMediaInfo
import com.music.vivi.ui.utils.ShowOffsetDialog
import com.music.vivi.ui.utils.resize
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.music.vivi.extensions.SwipeGesture
import com.music.vivi.vivimusic.AudioDeviceBottomSheet
import com.music.vivi.vivimusic.getConnectedBluetoothDeviceName
import com.music.vivi.ui.component.CastButton
import com.music.vivi.ui.menu.ListenTogetherDialog
import com.music.vivi.BuildConfig
import com.music.vivi.R
import com.music.vivi.LocalListenTogetherManager
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import com.music.vivi.ui.component.Icon as MIcon
import com.music.vivi.listentogether.RoomRole
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import com.music.vivi.ui.screens.settings.integrations.ListenTogetherAvatarList
import android.util.Log
import coil3.request.crossfade
enum class PlayerInternalState { COVER, LYRICS, QUEUE }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerV2(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
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
    val listenTogetherUsersCount = listenTogetherRoomState?.users?.size ?: if (listenTogetherManager?.isInRoom == true) 1 else 0
    val isInListenTogetherRoom = listenTogetherManager?.isInRoom == true || listenTogetherUsersCount > 0
    var showListenTogetherDialog by remember { mutableStateOf(false) }
    val hostUser = remember(listenTogetherRoomState) {
        listenTogetherRoomState?.users?.firstOrNull { it.isHost || it.userId == listenTogetherRoomState?.hostId }
    }
    val hostName = hostUser?.username
    val hostAvatarRes = remember(hostUser?.avatarIndex) {
        val index = hostUser?.avatarIndex ?: 0
        ListenTogetherAvatarList.getOrElse(index) { R.drawable.person }
    }
    val isMuted by playerConnection.isMuted.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    // Cast state — mirrors Player.kt lines 366-378
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
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    BackHandler(enabled = playerState != PlayerInternalState.COVER) {
        controlsVisible = true
        playerState = PlayerInternalState.COVER
    }

    LaunchedEffect(playerState, lastInteractionTime, effectiveIsPlaying) {
        if (playerState == PlayerInternalState.LYRICS) {
            controlsVisible = true
            delay(5000L)
            if (playerState == PlayerInternalState.LYRICS) {
                controlsVisible = false
            }
        } else {
            controlsVisible = true
        }
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
    
    // Custom volume state implementation since produceState awaitDispose can be tricky with imports
    var systemVolume by remember { mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxSystemVolume) }
    val animatedVolume by animateFloatAsState(
        targetValue = systemVolume,
        animationSpec = tween(
            durationMillis = 150,
            easing = FastOutLinearInEasing
        ),
        label = "volumeAnimation"
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
    val playerBackground = storedPlayerBackground
    
    val showPlayerThumbnailShadow by rememberPreference(ShowPlayerThumbnailShadowKey, defaultValue = false)
    val playerThumbnailShadowElevation by rememberPreference(PlayerThumbnailShadowElevationKey, defaultValue = 8f)
    val enableGoogleCast by rememberPreference(EnableGoogleCastKey, defaultValue = true)
    val hidePlayerThumbnail by rememberPreference(HidePlayerThumbnailKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
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

    // Debug log: trace what the UI is showing after crossfade state changes
    LaunchedEffect(isCrossfadingService, mediaMetadata?.id, crossfadeOutgoingItem?.id, crossfadeIncomingItem?.id) {
        Log.d("CrossfadeUI", "isCrossfading=$isCrossfadingService | mediaMetadata=${mediaMetadata?.title} (${mediaMetadata?.id}) | outgoing=${crossfadeOutgoingItem?.title} | incoming=${crossfadeIncomingItem?.title} | displayed=${displayedMetadata?.title}")
    }
    val isKeepScreenOn by rememberPreference(KeepScreenOn, false)
    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )
    val (useNewPlayerDesign) = rememberPreference(
        UseNewPlayerDesignKey,
        defaultValue = false
    )
    val isSystemInDarkTheme = isSystemInDarkTheme()
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

    DisposableEffect(playerState, isPlaying, isKeepScreenOn) {
        val window = (context as? Activity)?.window
        val shouldKeepScreenOn = (playerState == PlayerInternalState.LYRICS) || (isPlaying && isKeepScreenOn)
        if (shouldKeepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

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
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    updateDeviceState()
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
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
        val listener = object : Player.Listener {
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
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

    // Sync cast position+duration when casting, with a debounce after manual seeks
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

    val blurOverlayAlpha by animateFloatAsState(
        targetValue = if (playerState == PlayerInternalState.LYRICS || playerState == PlayerInternalState.QUEUE) 1f else 0f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "blurOverlayAlpha"
    )

    val mixingInfiniteTransition = rememberInfiniteTransition(label = "MixingShimmer")
    val mixingShimmerTranslate by mixingInfiniteTransition.animateFloat(
        initialValue = -80f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mixingShimmerTranslate"
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent) 
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.changedToDown() }) {
                            if (playerState == PlayerInternalState.COVER) {
                                controlsVisible = true
                            }
                        }
                    }
                }
            }
    ) {
        if (isLandscape) {
            PlayerV2Landscape(
                state = state,
                navController = navController,
                modifier = modifier
            )
        } else {
            if (playerBackground == PlayerBackgroundStyle.APPLE_MUSIC && blurOverlayAlpha > 0.001f) {
                mediaMetadata?.thumbnailUrl?.let { thumbnailUrl ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(blurOverlayAlpha)
                    ) {
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
                                .background(Color.Black.copy(alpha = 0.35f))
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
                    .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
            ) {
            // Minimal drag handle header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(adaptivePrimary.copy(alpha = 0.3f))
                        .clickable { state.collapseSoft() }
                )
            }

            // Morphing Content Area (Cover vs Lyrics vs Queue)
            SharedTransitionLayout(
                    modifier = Modifier.weight(1f)
                ) {
                    AnimatedContent(
                        targetState = playerState,
                        transitionSpec = {
                            // Fix 3: Shorter crossfade reduces concurrent animation CPU load.
                            // Asymmetric timing (exit faster than enter) avoids visual overlap.
                            (fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) togetherWith
                             fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing)))
                                .apply { targetContentZIndex = 1f }
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "InternalWindow"
                    ) { targetState ->
                        if (targetState == PlayerInternalState.COVER) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (playerBackground != PlayerBackgroundStyle.APPLE_MUSIC) {
                                    // Dynamic Apple Music Style Artwork (scales to fit screen without overflowing into slider)
                                BoxWithConstraints(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val artworkSize = minOf(maxWidth, maxHeight - 16.dp).coerceAtLeast(100.dp)

                                    Box(
                                        modifier = Modifier
                                            .sharedElement(
                                                rememberSharedContentState(key = "coverArt"),
                                                animatedVisibilityScope = this@AnimatedContent,
                                                boundsTransform = BoundsTransform { _, _ ->
                                                    tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                                }
                                            )
                                            .size(artworkSize)
                                            .aspectRatio(1f)
                                            .customSoftShadow(
                                                elevation = playerThumbnailShadowElevation.dp, 
                                                cornerRadius = 12.dp, 
                                                enabled = showPlayerThumbnailShadow
                                            )
                                            .background(adaptiveSurface, RoundedCornerShape(12.dp))
                                            .clip(RoundedCornerShape(12.dp))
                                            .SwipeGesture(
                                                enabled = (playerState == PlayerInternalState.COVER && !isListenTogetherGuest),
                                                onSwipeLeft = { if (canSkipNext) playerConnection.seekToNext() },
                                                onSwipeRight = { if (canSkipPrevious) playerConnection.seekToPrevious() }
                                            )
                                    ) {
                                        if (hidePlayerThumbnail) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.vivi_music_small_icon),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(72.dp),
                                                    tint = adaptivePrimary.copy(alpha = 0.5f)
                                                )
                                            }
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
                            } else {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .SwipeGesture(
                                            enabled = (playerState == PlayerInternalState.COVER && !isListenTogetherGuest),
                                            onSwipeLeft = { if (canSkipNext) playerConnection.seekToNext() },
                                            onSwipeRight = { if (canSkipPrevious) playerConnection.seekToPrevious() }
                                        )
                                )
                            }

                            // Track Meta Info (Left Aligned)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 12.dp)
                                            .clipToBounds()
                                    ) {
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
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontFamily = SfProDisplayFontFamily,
                                            color = adaptivePrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clipToBounds()
                                                .sharedBounds(
                                                    rememberSharedContentState(key = "title"),
                                                    animatedVisibilityScope = this@AnimatedContent,
                                                    boundsTransform = BoundsTransform { _, _ ->
                                                        tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                                    }
                                                )
                                                .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                                .clickable {
                                                    displayedMetadata?.album?.id?.let {
                                                        navController.navigate("album/$it")
                                                        state.collapseSoft()
                                                    }
                                                }
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val artists = displayedMetadata?.artists.orEmpty().filter { it.name.isNotBlank() }
                                        if (artists.isNotEmpty()) {
                                            val annotatedString = buildAnnotatedString {
                                                withStyle(
                                                    SpanStyle(
                                                        color = adaptiveSecondary,
                                                        fontFamily = SfProDisplayFontFamily,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Normal
                                                    )
                                                ) {
                                                    artists.forEachIndexed { index, artist ->
                                                        val tag = "artist_${artist.id.orEmpty()}"
                                                        pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty())
                                                        append(artist.name)
                                                        pop()
                                                        if (index != artists.lastIndex) append(", ")
                                                    }
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .sharedBounds(
                                                        rememberSharedContentState(key = "artist"),
                                                        animatedVisibilityScope = this@AnimatedContent,
                                                        boundsTransform = BoundsTransform { _, _ ->
                                                            tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                                        }
                                                    )
                                            ) {
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
                                                        style = MaterialTheme.typography.titleMedium,
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
                                                "Unknown",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontFamily = SfProDisplayFontFamily,
                                                color = adaptiveSecondary,
                                                fontWeight = FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.sharedBounds(
                                            rememberSharedContentState(key = "actionButtons"),
                                            animatedVisibilityScope = this@AnimatedContent,
                                            boundsTransform = BoundsTransform { _, _ ->
                                                tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                            }
                                        )
                                    ) {
                                        val isLiked = currentSong?.song?.liked == true

                                        IconButton(
                                            onClick = playerConnection::toggleLike,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(if (isLiked) R.drawable.ic_nowplaying_favorited else R.drawable.ic_nowplaying_favorite),
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
                                                contentDescription = "Options",
                                                tint = adaptivePrimary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (targetState == PlayerInternalState.LYRICS || targetState == PlayerInternalState.QUEUE) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Apple Music Morphing Mini Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .sharedElement(
                                                rememberSharedContentState(key = "coverArt"),
                                                animatedVisibilityScope = this@AnimatedContent,
                                                boundsTransform = BoundsTransform { _, _ ->
                                                    tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                                }
                                            )
                                            .size(64.dp)
                                            .customSoftShadow(
                                                elevation = playerThumbnailShadowElevation.dp / 2f, 
                                                cornerRadius = 8.dp, 
                                                enabled = showPlayerThumbnailShadow
                                            )
                                            .background(adaptiveSurface, RoundedCornerShape(8.dp))
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                controlsVisible = true
                                                playerState = PlayerInternalState.COVER
                                            }
                                    ) {
                                        if (hidePlayerThumbnail) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.vivi_music_small_icon),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(32.dp),
                                                    tint = adaptivePrimary.copy(alpha = 0.5f)
                                                )
                                            }
                                        } else {
                                            val isYtThumbnail = remember(mediaMetadata?.thumbnailUrl) {
                                                mediaMetadata?.thumbnailUrl?.let { it.contains("ytimg") || it.contains("/vi/") || it.contains("youtube.com") } == true
                                            }
                                            AsyncImage(
                                                model = mediaMetadata?.thumbnailUrl?.resize(1200, 1200),
                                                contentDescription = "Cover Art",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer {
                                                        if (cropAlbumArt && isYtThumbnail) {
                                                            scaleX = 4f / 3f
                                                            scaleY = 4f / 3f
                                                        }
                                                    },
                                                contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 12.dp)
                                            .clipToBounds()
                                    ) {
                                        Text(
                                            text = displayedMetadata?.title ?: "Unknown",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = SfProDisplayFontFamily,
                                            color = adaptivePrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clipToBounds()
                                                .sharedBounds(
                                                    rememberSharedContentState(key = "title"),
                                                    animatedVisibilityScope = this@AnimatedContent,
                                                    boundsTransform = BoundsTransform { _, _ ->
                                                        tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                                    }
                                                )
                                                .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clipToBounds()
                                                .sharedBounds(
                                                    rememberSharedContentState(key = "artist"),
                                                    animatedVisibilityScope = this@AnimatedContent,
                                                    boundsTransform = BoundsTransform { _, _ ->
                                                        tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                                    }
                                                )
                                        ) {
                                            if (displayedMetadata?.explicit == true) {
                                                MIcon.Explicit(tint = adaptiveSecondary)
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            val allArtists = displayedMetadata?.artists?.filter { it.name.isNotBlank() }?.joinToString { it.name } ?: "Unknown"
                                            Text(
                                                text = allArtists,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontFamily = SfProDisplayFontFamily,
                                                color = adaptiveSecondary,
                                                fontWeight = FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clipToBounds()
                                                    .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.sharedBounds(
                                            rememberSharedContentState(key = "actionButtons"),
                                            animatedVisibilityScope = this@AnimatedContent,
                                            boundsTransform = BoundsTransform { _, _ ->
                                                tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                            }
                                        )
                                    ) {
                                        val isLiked = currentSong?.song?.liked == true

                                        IconButton(
                                            onClick = playerConnection::toggleLike,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(if (isLiked) R.drawable.ic_nowplaying_favorited else R.drawable.ic_nowplaying_favorite),
                                                contentDescription = "Like",
                                                tint = adaptivePrimary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        IconButton(
                                            onClick = {
                                                if (targetState == PlayerInternalState.QUEUE) {
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
                                                } else {
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
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_nowplaying_more),
                                                contentDescription = "Options",
                                                tint = adaptivePrimary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                                
                                // Dynamic Engine Block
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .animateEnterExit(
                                            // Fix 5: Remove slideInVertically — it competed with
                                            // the cover art morph during exit, causing extra load.
                                            enter = fadeIn(tween(350, easing = FastOutSlowInEasing)),
                                            exit = fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
                                        )
                                ) {
                                    if (targetState == PlayerInternalState.LYRICS) {
                                        AnimatedContent(
                                            targetState = currentLyrics?.id to currentLyrics?.lyrics,
                                            transitionSpec = {
                                                (fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) togetherWith
                                                 fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)))
                                                    .apply { targetContentZIndex = 1f }
                                            },
                                            modifier = Modifier.fillMaxSize(),
                                            label = "LyricsContentTransition"
                                        ) { _ ->
                                            LyricsV2(
                                                mediaMetadata = mediaMetadata,
                                                showLyrics = true,
                                                sliderPositionProvider = { sliderPosition },
                                                positionProvider = { sliderPosition ?: position },
                                                initialLyrics = currentLyrics,
                                                onControlsVisibilityChange = { isVisible ->
                                                    controlsVisible = isVisible
                                                    if (isVisible) {
                                                        lastInteractionTime = System.currentTimeMillis()
                                                    }
                                                }
                                            )
                                        }
                                    } else {
                                        QueueV2(
                                            navController = navController,
                                            playerBottomSheetState = state,
                                            modifier = Modifier.fillMaxSize(),
                                            onControlsVisibilityChange = { isVisible ->
                                                controlsVisible = isVisible
                                            }
                                        )
                                    }
                                }
                            }
                        }
                }
            }
            
            // Persistent Controls & Utility Action Bar Container (Always at the bottom)
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(450, easing = FastOutLinearInEasing))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.changes.any { it.changedToDown() }) {
                                        lastInteractionTime = System.currentTimeMillis()
                                    }
                                }
                            }
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        // Apple Music Timeline Slider
                        val currentPos = sliderPosition ?: position
                        
                        val trackInteractionSource = remember { MutableInteractionSource() }
                        val isTrackDragged by trackInteractionSource.collectIsDraggedAsState()
                        val isTrackPressed by trackInteractionSource.collectIsPressedAsState()
                        val isTrackActive = isTrackDragged || isTrackPressed
                        
                        val trackHeight by animateDpAsState(
                            targetValue = if (isTrackActive) 10.dp else 7.25.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                            label = "trackScale"
                        )
                        
                        Slider(
                            value = currentPos.toFloat().coerceIn(0f, maxOf(1f, duration.toFloat())),
                            valueRange = 0f..(if (duration == C.TIME_UNSET || duration <= 0L) 1f else duration.toFloat()),
                            onValueChange = { value ->
                                if (!isListenTogetherGuest) {
                                    sliderPosition = value.toLong()
                                }
                            },
                            onValueChangeFinished = {
                                if (!isListenTogetherGuest) {
                                    sliderPosition?.let { pos ->
                                        if (isCasting) {
                                            castHandler?.seekTo(pos)
                                            lastManualSeekTime = System.currentTimeMillis()
                                        } else {
                                            playerConnection.player.seekTo(pos)
                                        }
                                        position = pos
                                        sliderPosition = null
                                    }
                                }
                            },
                            enabled = !isListenTogetherGuest,
                            interactionSource = trackInteractionSource,
                            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                            track = { sliderState ->
                                PlayerSliderTrack(
                                    sliderState = sliderState,
                                    trackHeight = trackHeight,
                                    useOverlay = false,
                                    colors = PlayerSliderColors.getSliderColors(
                                        activeColor = sliderActiveColor,
                                        playerBackground = playerBackground,
                                        useDarkTheme = useDarkTheme
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(makeTimeString(currentPos), color = adaptiveSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            if (playerBackground != PlayerBackgroundStyle.APPLE_MUSIC && crossfadeEnabled && (mixAlpha > 0.001f || isMixing)) {
                                Text(
                                    text = "Mixing",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        brush = mixingShimmerBrush,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.graphicsLayer { alpha = mixAlpha }
                                )
                            }
                            Text("-" + makeTimeString(maxOf(0L, duration - currentPos)), color = adaptiveSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    
                        Spacer(modifier = Modifier.height(16.dp))
                    
                        // Naked Transparent Playback Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (!isListenTogetherGuest) {
                                        if (isCasting) castHandler?.skipToPrevious()
                                        else if (canSkipPrevious) playerConnection.seekToPrevious()
                                    }
                                },
                                enabled = !isListenTogetherGuest && canSkipPrevious,
                                modifier = Modifier
                                    .size(64.dp)
                                    .alpha(if (isListenTogetherGuest || !canSkipPrevious) 0.4f else 1f)
                            ) {
                                Icon(painter = painterResource(R.drawable.apple_skip_previous), contentDescription = "Previous", tint = adaptivePrimary, modifier = Modifier.size(48.dp))
                            }
                    
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
                                modifier = Modifier.size(88.dp)
                            ) {
                                if (isListenTogetherGuest) {
                                    Icon(
                                        painter = painterResource(if (isMuted) R.drawable.volume_off else R.drawable.volume_up),
                                        contentDescription = if (isMuted) "Unmute" else "Mute",
                                        modifier = Modifier.size(64.dp),
                                        tint = adaptivePrimary
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(if (effectiveIsPlaying) R.drawable.pause_applemusic else R.drawable.play_applemusic),
                                        contentDescription = if (effectiveIsPlaying) "Pause" else "Play",
                                        modifier = Modifier.size(80.dp),
                                        tint = adaptivePrimary
                                    )
                                }
                            }
                    
                            IconButton(
                                onClick = {
                                    if (!isListenTogetherGuest) {
                                        if (isCasting) castHandler?.skipToNext()
                                        else if (canSkipNext) playerConnection.seekToNext()
                                    }
                                },
                                enabled = !isListenTogetherGuest && canSkipNext,
                                modifier = Modifier
                                    .size(64.dp)
                                    .alpha(if (isListenTogetherGuest || !canSkipNext) 0.4f else 1f)
                            ) {
                                Icon(painter = painterResource(R.drawable.apple_skip_next), contentDescription = "Next", tint = adaptivePrimary, modifier = Modifier.size(48.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Audio Volume Component
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(painterResource(R.drawable.ic_speaker_24dp), contentDescription = "Volume Down", tint = adaptiveSecondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            val volumeInteractionSource = remember { MutableInteractionSource() }
                            val isVolDragged by volumeInteractionSource.collectIsDraggedAsState()
                            val isVolPressed by volumeInteractionSource.collectIsPressedAsState()
                            val isVolActive = isVolDragged || isVolPressed
                            
                            val volHeight by animateDpAsState(
                                targetValue = if (isVolActive) 10.dp else 7.25.dp,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                label = "volHeight"
                            )
                            
                            Slider(
                                value = if (isCasting) castVolume
                                        else if (isVolActive) systemVolume
                                        else animatedVolume,
                                onValueChange = { newValue ->
                                    if (isCasting) {
                                        castHandler?.setVolume(newValue)
                                    } else {
                                        systemVolume = newValue
                                        val targetVolume = (newValue * maxSystemVolume).toInt()
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                                    }
                                },
                                interactionSource = volumeInteractionSource,
                                thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                                track = { sliderState ->
                                    PlayerSliderTrack(
                                        sliderState = sliderState,
                                        trackHeight = volHeight,
                                        useOverlay = false,
                                        colors = PlayerSliderColors.getSliderColors(
                                            activeColor = sliderActiveColor,
                                            playerBackground = playerBackground,
                                            useDarkTheme = useDarkTheme
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f).height(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(painterResource(R.drawable.ic_speaker_wave_3_24dp), contentDescription = "Volume Up", tint = adaptiveSecondary, modifier = Modifier.size(24.dp))
                        }
                    }

                    // Bottom Utility Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isLyricsActive = playerState == PlayerInternalState.LYRICS
                        IconButton(
                            onClick = {
                                if (isLyricsActive) controlsVisible = true
                                playerState = if (isLyricsActive) PlayerInternalState.COVER else PlayerInternalState.LYRICS
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.lyrics_apple), 
                                contentDescription = "Lyrics", 
                                tint = if (isLyricsActive) adaptivePrimary else adaptiveSecondary, 
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        if (isInListenTogetherRoom) {
                            Box(
                                modifier = Modifier.height(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = adaptiveSecondary.copy(alpha = 0.16f),
                                    modifier = Modifier.height(42.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left Half: Audio Device / AirPlay / Cast
                                        val onLeftClick: (() -> Unit)? = if (bluetoothDeviceName != null || !(BuildConfig.CAST_AVAILABLE && enableGoogleCast)) {
                                            { showAudioDeviceBottomSheet = true }
                                        } else null

                                        Box(
                                            modifier = Modifier
                                                .then(if (onLeftClick != null) Modifier.clickable(onClick = onLeftClick) else Modifier)
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (bluetoothDeviceName != null) {
                                                Icon(
                                                    painter = painterResource(R.drawable.headphones_apple),
                                                    contentDescription = "Audio Device",
                                                    tint = adaptivePrimary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            } else if (BuildConfig.CAST_AVAILABLE && enableGoogleCast) {
                                                CastButton(
                                                    modifier = Modifier.size(24.dp),
                                                    tintColor = adaptiveSecondary,
                                                    activeTintColor = adaptivePrimary,
                                                    showBackground = false,
                                                    customIconRes = R.drawable.airplay_audio
                                                )
                                            } else {
                                                Icon(
                                                    painter = painterResource(R.drawable.airplay_audio),
                                                    contentDescription = "Audio Routing",
                                                    tint = adaptiveSecondary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        // Center dividing line
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(22.dp)
                                                .background(adaptiveSecondary.copy(alpha = 0.25f))
                                        )

                                        // Right Half: Listen Together / SharePlay
                                        Row(
                                            modifier = Modifier
                                                .clickable { showListenTogetherDialog = true }
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.group_filled),
                                                contentDescription = "Listen Together",
                                                tint = adaptivePrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            if (listenTogetherUsersCount > 0) {
                                                Text(
                                                    text = "$listenTogetherUsersCount",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    ),
                                                    color = adaptivePrimary
                                                )
                                            }
                                        }
                                    }
                                }

                                if (bluetoothDeviceName != null) {
                                    Text(
                                        text = bluetoothDeviceName!!,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = adaptiveSecondary.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .absoluteOffset(y = 34.dp)
                                            .widthIn(max = 120.dp)
                                    )
                                }
                            }
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                if (bluetoothDeviceName != null) {
                                    IconButton(
                                        onClick = { showAudioDeviceBottomSheet = true },
                                        modifier = Modifier.background(Color.Transparent, RoundedCornerShape(12.dp))
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.headphones_apple),
                                            contentDescription = "Audio Device",
                                            tint = adaptiveSecondary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Text(
                                        text = bluetoothDeviceName!!,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = adaptiveSecondary.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .absoluteOffset(y = 30.dp)
                                            .widthIn(max = 84.dp)
                                    )
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
                                        modifier = Modifier.background(Color.Transparent, RoundedCornerShape(12.dp))
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
                        }
                        
                        val isQueueActive = playerState == PlayerInternalState.QUEUE
                        IconButton(
                            onClick = {
                                if (isQueueActive) controlsVisible = true
                                playerState = if (isQueueActive) PlayerInternalState.COVER else PlayerInternalState.QUEUE
                            }
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
            }
        }
    }
        
        if (showAudioDeviceBottomSheet) {
            AudioDeviceBottomSheet(onDismiss = { showAudioDeviceBottomSheet = false })
        }
        if (showListenTogetherDialog) {
            ListenTogetherDialog(
                visible = showListenTogetherDialog,
                mediaMetadata = mediaMetadata,
                onDismiss = { showListenTogetherDialog = false }
            )
        }
    }
}
