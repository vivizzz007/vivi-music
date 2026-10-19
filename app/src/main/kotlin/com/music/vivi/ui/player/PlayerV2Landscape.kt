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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.music.vivi.BuildConfig
import com.music.vivi.LocalListenTogetherManager
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import androidx.compose.ui.graphics.graphicsLayer
import com.music.vivi.constants.CropAlbumArtKey
import com.music.vivi.constants.EnableGoogleCastKey
import com.music.vivi.constants.HidePlayerThumbnailKey
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
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

@Composable
fun PlayerV2Landscape(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
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

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
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

    val (storedPlayerBackground, onPlayerBackgroundChange) = rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.GRADIENT
    )
    LaunchedEffect(storedPlayerBackground) {
        if (storedPlayerBackground == PlayerBackgroundStyle.APPLE_MUSIC) {
            onPlayerBackgroundChange(PlayerBackgroundStyle.DEFAULT)
        }
    }
    val playerBackground = if (storedPlayerBackground == PlayerBackgroundStyle.APPLE_MUSIC) {
        PlayerBackgroundStyle.DEFAULT
    } else {
        storedPlayerBackground
    }

    val showPlayerThumbnailShadow by rememberPreference(ShowPlayerThumbnailShadowKey, defaultValue = false)
    val playerThumbnailShadowElevation by rememberPreference(PlayerThumbnailShadowElevationKey, defaultValue = 8f)
    val enableGoogleCast by rememberPreference(EnableGoogleCastKey, defaultValue = true)
    val hidePlayerThumbnail by rememberPreference(HidePlayerThumbnailKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)

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

    // Also update once on song change
    LaunchedEffect(mediaMetadata?.id) {
        if (!isCasting) {
            val rawDuration = playerConnection.player.duration
            position = playerConnection.player.currentPosition.coerceAtLeast(0L)
            duration = if (rawDuration == C.TIME_UNSET || rawDuration < 0) 0L else rawDuration
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

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = WindowInsets.systemBars.asPaddingValues().calculateStartPadding(LayoutDirection.Ltr) + 16.dp,
                end = WindowInsets.systemBars.asPaddingValues().calculateEndPadding(LayoutDirection.Ltr) + 16.dp,
                top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 8.dp,
                bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT SIDE: Centered Thumbnail + Bottom Row of Lyrics, Queue, Cast/Headphones
        Column(
            modifier = Modifier
                .weight(0.48f)
                .fillMaxHeight()
                .padding(top = 16.dp, bottom = 16.dp, start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Thumbnail Box (upsized to ~215dp, centered)
            Box(
                modifier = Modifier
                    .size(215.dp)
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
                    PlayerV2Motionartowek(
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Row of Lyrics, Queue, Cast/Headphones (aligned with volume slider row on the right)
            Row(
                modifier = Modifier
                    .width(215.dp)
                    .height(40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Lyrics Button (Left)
                val isLyricsActive = playerState == PlayerInternalState.LYRICS
                IconButton(
                    onClick = {
                        playerState = if (isLyricsActive) PlayerInternalState.COVER else PlayerInternalState.LYRICS
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLyricsActive) adaptivePrimary.copy(alpha = 0.2f) else Color.Transparent
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lyrics_apple),
                        contentDescription = "Lyrics",
                        tint = if (isLyricsActive) adaptivePrimary else adaptiveSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // 2. Cast / Headphone / Audio Device Button (Center)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (bluetoothDeviceName != null) {
                        IconButton(
                            onClick = { showAudioDeviceBottomSheet = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.headphones_apple),
                                contentDescription = "Audio Device",
                                tint = adaptiveSecondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    } else if (BuildConfig.CAST_AVAILABLE && enableGoogleCast) {
                        CastButton(
                            modifier = Modifier.size(44.dp),
                            tintColor = adaptiveSecondary,
                            activeTintColor = adaptivePrimary,
                            showBackground = false
                        )
                    } else {
                        IconButton(
                            onClick = { showAudioDeviceBottomSheet = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.home_speaker_devices),
                                contentDescription = "Speaker",
                                tint = adaptiveSecondary,
                                modifier = Modifier.size(26.dp)
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
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isQueueActive) adaptivePrimary.copy(alpha = 0.2f) else Color.Transparent
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.apple_queue),
                        contentDescription = "Queue",
                        tint = if (isQueueActive) adaptivePrimary else adaptiveSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // RIGHT SIDE: Raw controls (Cover) OR Pure Lyrics (Lyrics) OR Pure Queue (Queue)
        // No extra box or card behind anything, completely transparent on player background.
        Box(
            modifier = Modifier
                .weight(0.52f)
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
                                .padding(top = 16.dp, bottom = 16.dp, start = 8.dp, end = 16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Song Title, Artist & Action Buttons (Star, More)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mediaMetadata?.title ?: "Unknown",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = adaptivePrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable {
                                            state.collapseSoft()
                                            mediaMetadata?.album?.id?.let { navController.navigate("album/$it") }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = mediaMetadata?.artists?.firstOrNull()?.name ?: "Unknown",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = adaptiveSecondary,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable {
                                            state.collapseSoft()
                                            mediaMetadata?.artists?.firstOrNull()?.id?.let { navController.navigate("artist/$it") }
                                        }
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isLiked = currentSong?.song?.liked == true
                                    IconButton(
                                        onClick = playerConnection::toggleLike,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (isLiked) R.drawable.star_filled_liked else R.drawable.star_like
                                            ),
                                            contentDescription = "Like",
                                            tint = adaptivePrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

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
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_horiz),
                                            contentDescription = "More",
                                            tint = adaptivePrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            // Progress Slider & Timestamps
                            Column(modifier = Modifier.fillMaxWidth()) {
                                val currentPos = sliderPosition ?: position
                                val trackInteractionSource = remember { MutableInteractionSource() }
                                val isTrackDragged by trackInteractionSource.collectIsDraggedAsState()
                                val isTrackPressed by trackInteractionSource.collectIsPressedAsState()
                                val isTrackActive = isTrackDragged || isTrackPressed

                                val trackHeight by animateDpAsState(
                                    targetValue = if (isTrackActive) 14.dp else 8.dp,
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
                                                activeColor = adaptivePrimary,
                                                playerBackground = playerBackground,
                                                useDarkTheme = isSystemInDarkTheme()
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
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = makeTimeString(currentPos),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = adaptiveSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
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

                            // Playback Controls Row
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
                                        modifier = Modifier.size(42.dp)
                                    )
                                }

                                // Play / Pause (matching portrait PlayerV2)
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
                                    modifier = Modifier.size(68.dp)
                                ) {
                                    if (isListenTogetherGuest) {
                                        Icon(
                                            painter = painterResource(if (isMuted) R.drawable.volume_off else R.drawable.volume_up),
                                            contentDescription = if (isMuted) "Unmute" else "Mute",
                                            modifier = Modifier.size(52.dp),
                                            tint = adaptivePrimary
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(
                                                if (effectiveIsPlaying) R.drawable.pause_applemusic else R.drawable.play_applemusic
                                            ),
                                            contentDescription = if (effectiveIsPlaying) "Pause" else "Play",
                                            modifier = Modifier.size(64.dp),
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
                                        modifier = Modifier.size(42.dp)
                                    )
                                }
                            }

                            // Volume Slider Row (height 40.dp matching the left button row)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painterResource(R.drawable.volume_mute),
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
                                    targetValue = if (isVolActive) 10.dp else 5.dp,
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
                                                activeColor = adaptivePrimary.copy(alpha = 0.8f),
                                                playerBackground = playerBackground,
                                                useDarkTheme = isSystemInDarkTheme()
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    painterResource(R.drawable.volume_up),
                                    contentDescription = "Volume Up",
                                    tint = adaptiveSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    PlayerInternalState.LYRICS -> {
                        // In landscape, when lyrics pressed: pure LyricsV2 with floating circular action buttons (Star on left, Options on right)
                        Box(modifier = Modifier.fillMaxSize()) {
                            LyricsV2(
                                mediaMetadata = mediaMetadata,
                                showLyrics = true,
                                sliderPositionProvider = { sliderPosition },
                                positionProvider = { sliderPosition ?: position },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Small compact circular star/like button at bottom left of lyrics
                            val isLiked = currentSong?.song?.liked == true
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 16.dp, bottom = 16.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(adaptiveSurface)
                                    .clickable {
                                        playerConnection.toggleLike()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isLiked) R.drawable.star_filled_liked else R.drawable.star_like
                                    ),
                                    contentDescription = "Like",
                                    tint = adaptivePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Small compact circular more_vert button at bottom right to open LyricsMenu
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 16.dp, bottom = 16.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(adaptiveSurface)
                                    .clickable {
                                        if (mediaMetadata != null) {
                                            menuState.show {
                                                LyricsMenu(
                                                    lyricsProvider = { currentLyrics },
                                                    songProvider = { currentSong?.song },
                                                    mediaMetadataProvider = { mediaMetadata!! },
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
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = "Lyrics Options",
                                    tint = adaptivePrimary,
                                    modifier = Modifier.size(18.dp)
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

    if (showAudioDeviceBottomSheet) {
        AudioDeviceBottomSheet(
            onDismiss = { showAudioDeviceBottomSheet = false }
        )
    }
}
