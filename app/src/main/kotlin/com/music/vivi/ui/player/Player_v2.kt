package com.music.vivi.ui.player

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import androidx.media3.common.C
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
import com.music.vivi.constants.ShowPlayerThumbnailShadowKey
import com.music.vivi.constants.PlayerThumbnailShadowElevationKey
import com.music.vivi.constants.EnableGoogleCastKey
import com.music.vivi.constants.HidePlayerThumbnailKey
import com.music.vivi.models.MediaMetadata
import com.music.vivi.ui.component.BottomSheetState
import com.music.vivi.ui.component.PlayerSliderTrack
import com.music.vivi.ui.theme.PlayerSliderColors
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
import androidx.compose.ui.unit.sp
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.vivi.extensions.SwipeGesture
import com.music.vivi.vivimusic.AudioDeviceBottomSheet
import com.music.vivi.vivimusic.getConnectedBluetoothDeviceName
import com.music.vivi.ui.component.CastButton
import com.music.vivi.BuildConfig
import com.music.vivi.R
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.launch

enum class PlayerInternalState { COVER, LYRICS, QUEUE }

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerV2(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // starting landscape not support
    DisposableEffect(Unit) {
        val activity = (context as? android.app.Activity) ?: run {
            var ctx = context
            while (ctx is android.content.ContextWrapper) {
                if (ctx is android.app.Activity) break
                ctx = ctx.baseContext
            }
            ctx as? android.app.Activity
        }
        val originalOrientation = activity?.requestedOrientation ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }
    // ended landscape not support
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    var playerState by remember { mutableStateOf(PlayerInternalState.COVER) }
    
    val listenTogetherManager = com.music.vivi.LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsState(initial = com.music.vivi.listentogether.RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == com.music.vivi.listentogether.RoomRole.GUEST
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
    
    androidx.activity.compose.BackHandler(enabled = playerState != PlayerInternalState.COVER) {
        playerState = PlayerInternalState.COVER
    }
    
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(playerState, lastInteractionTime) {
        if (playerState == PlayerInternalState.LYRICS) {
            delay(3000)
            controlsVisible = false
        } else {
            controlsVisible = true
        }
    }
    
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
    val maxSystemVolume = remember { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() }
    
    // Custom volume state implementation since produceState awaitDispose can be tricky with imports
    var systemVolume by remember { mutableFloatStateOf(audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() / maxSystemVolume) }
    val animatedVolume by androidx.compose.animation.core.animateFloatAsState(
        targetValue = systemVolume,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 150,
            easing = androidx.compose.animation.core.FastOutLinearInEasing
        ),
        label = "volumeAnimation"
    )
    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    systemVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() / maxSystemVolume
                }
            }
        }
        context.registerReceiver(receiver, android.content.IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
        onDispose { context.unregisterReceiver(receiver) }
    }

    val storedPlayerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.GRADIENT
    )
    val playerBackground = if (storedPlayerBackground == PlayerBackgroundStyle.APPLE_MUSIC) {
        PlayerBackgroundStyle.DEFAULT
    } else {
        storedPlayerBackground
    }
    
    val showPlayerThumbnailShadow by rememberPreference(ShowPlayerThumbnailShadowKey, defaultValue = false)
    val playerThumbnailShadowElevation by rememberPreference(PlayerThumbnailShadowElevationKey, defaultValue = 8f)
    val enableGoogleCast by rememberPreference(EnableGoogleCastKey, defaultValue = true)
    val hidePlayerThumbnail by rememberPreference(HidePlayerThumbnailKey, false)

    var showAudioDeviceBottomSheet by remember { mutableStateOf(false) }
    
    val bluetoothDeviceName by produceState<String?>(initialValue = getConnectedBluetoothDeviceName(context)) {
        val am = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
        val scope = this
        val updateDeviceState: () -> Unit = {
            scope.value = getConnectedBluetoothDeviceName(context)
            scope.launch {
                kotlinx.coroutines.delay(500)
                scope.value = getConnectedBluetoothDeviceName(context)
            }
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: android.content.Context, intent: Intent) {
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent) 
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        if (event.changes.any { it.pressed }) {
                            lastInteractionTime = System.currentTimeMillis()
                            controlsVisible = true
                        }
                    }
                }
            }
    ) {
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
            androidx.compose.animation.SharedTransitionLayout(
                    modifier = Modifier.weight(1f)
                ) {
                    AnimatedContent(
                        targetState = playerState,
                        transitionSpec = {
                            if (targetState == PlayerInternalState.LYRICS || targetState == PlayerInternalState.QUEUE) {
                                // Pure crossfade: lets Shared Elements smoothly morph top-left while lyrics fade in calmly
                                fadeIn(animationSpec = tween(600, easing = FastOutSlowInEasing)) togetherWith 
                                fadeOut(animationSpec = tween(600, easing = FastOutSlowInEasing))
                            } else {
                                fadeIn(animationSpec = tween(600, easing = FastOutSlowInEasing)) togetherWith 
                                fadeOut(animationSpec = tween(600, easing = FastOutSlowInEasing))
                            }.using(
                                // Ensure the Lyrics pane is drawn OVER the Cover pane during transition
                                androidx.compose.animation.SizeTransform(clip = false)
                            ).apply {
                                targetContentZIndex = if (targetState == PlayerInternalState.LYRICS || targetState == PlayerInternalState.QUEUE) 1f else 0f
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "InternalWindow"
                    ) { targetState ->
                        if (targetState == PlayerInternalState.COVER) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp)
                            ) {
                                Spacer(modifier = Modifier.weight(1f))

                                // Massive Apple Music Style Artwork
                                Box(
                                    modifier = Modifier
                                        .sharedElement(
                                            rememberSharedContentState(key = "coverArt"),
                                            animatedVisibilityScope = this@AnimatedContent
                                        )
                                        .fillMaxWidth()
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
                                            onSwipeLeft = { if (canSkipNext) playerConnection.player.seekToNext() },
                                            onSwipeRight = { if (canSkipPrevious) playerConnection.player.seekToPrevious() }
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
                                        AsyncImage(
                                            model = mediaMetadata?.thumbnailUrl?.resize(1200, 1200),
                                            contentDescription = "Cover Art",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        PlayerV2Canvas(
                                            mediaMetadata = mediaMetadata,
                                            isPlaying = isPlaying,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.weight(1f)) // Allows the content to lock down

                                // Track Meta Info (Left Aligned)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            mediaMetadata?.title ?: "Unknown",
                                            style = MaterialTheme.typography.headlineSmall, 
                                            color = adaptivePrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.sharedBounds(
                                                rememberSharedContentState(key = "title"),
                                                animatedVisibilityScope = this@AnimatedContent
                                            ).clickable {
                                                state.collapseSoft()
                                                mediaMetadata?.album?.id?.let { navController.navigate("album/$it") }
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            mediaMetadata?.artists?.firstOrNull()?.name ?: "Unknown",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = adaptiveSecondary,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.sharedBounds(
                                                rememberSharedContentState(key = "artist"),
                                                animatedVisibilityScope = this@AnimatedContent
                                            ).clickable {
                                                state.collapseSoft()
                                                mediaMetadata?.artists?.firstOrNull()?.id?.let { navController.navigate("artist/$it") }
                                            }
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.sharedBounds(
                                            rememberSharedContentState(key = "actionButtons"),
                                            animatedVisibilityScope = this@AnimatedContent
                                        )
                                    ) {
                                        val isLiked = currentSong?.song?.liked == true


                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(adaptivePrimary.copy(alpha = 0.1f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                        IconButton(
                                            onClick = playerConnection::toggleLike
                                        ) {
                                            Icon(
                                                painter = painterResource(if (isLiked) R.drawable.star_filled_liked else R.drawable.star_like),
                                                contentDescription = "Like",
                                                tint = adaptivePrimary,
                                            )
                                        }
                                        }
                                        
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(adaptivePrimary.copy(alpha = 0.1f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
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
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreHoriz,
                                                contentDescription = "Options",
                                                tint = adaptivePrimary,
                                            )
                                        }
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
                                                animatedVisibilityScope = this@AnimatedContent
                                            )
                                            .size(64.dp)
                                            .customSoftShadow(
                                                elevation = playerThumbnailShadowElevation.dp / 2f, 
                                                cornerRadius = 8.dp, 
                                                enabled = showPlayerThumbnailShadow
                                            )
                                            .background(adaptiveSurface, RoundedCornerShape(8.dp))
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { playerState = PlayerInternalState.COVER }
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
                                            AsyncImage(
                                                model = mediaMetadata?.thumbnailUrl?.resize(1200, 1200),
                                                contentDescription = "Cover Art",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            PlayerV2Canvas(
                                                mediaMetadata = mediaMetadata,
                                                isPlaying = isPlaying,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            mediaMetadata?.title ?: "Unknown",
                                            style = MaterialTheme.typography.titleMedium, 
                                            color = adaptivePrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.sharedBounds(
                                                rememberSharedContentState(key = "title"),
                                                animatedVisibilityScope = this@AnimatedContent
                                            )
                                        )
                                        Text(
                                            mediaMetadata?.artists?.firstOrNull()?.name ?: "Unknown",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = adaptiveSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.sharedBounds(
                                                rememberSharedContentState(key = "artist"),
                                                animatedVisibilityScope = this@AnimatedContent
                                            )
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.sharedBounds(
                                            rememberSharedContentState(key = "actionButtons"),
                                            animatedVisibilityScope = this@AnimatedContent
                                        )
                                    ) {
                                        val isLiked = currentSong?.song?.liked == true
                                        
                                        IconButton(
                                            onClick = playerConnection::toggleLike
                                        ) {
                                            Icon(
                                                painter = painterResource(if (isLiked) R.drawable.star_filled_liked else R.drawable.star_like),
                                                contentDescription = "Like",
                                                tint = adaptivePrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        
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
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Options",
                                                tint = adaptivePrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                                
                                // Dynamic Engine Block
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .animateEnterExit(
                                            enter = slideInVertically(animationSpec = tween(600, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(600)),
                                            exit = fadeOut(animationSpec = tween(400)) + slideOutVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) { it }
                                        )
                                ) {
                                    if (targetState == PlayerInternalState.LYRICS) {
                                        LyricsV2(
                                            mediaMetadata = mediaMetadata,
                                            showLyrics = true,
                                            positionProvider = { sliderPosition ?: position }
                                        )
                                    } else {
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
            
            // Persistent Controls Array (Always at the bottom)
            AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(animationSpec = tween(500, easing = LinearOutSlowInEasing)) +
                            slideInVertically(animationSpec = tween(500, easing = LinearOutSlowInEasing)) { it / 2 } +
                            androidx.compose.animation.expandVertically(animationSpec = tween(500, easing = LinearOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(500, easing = LinearOutSlowInEasing)) +
                           slideOutVertically(animationSpec = tween(500, easing = LinearOutSlowInEasing)) { it / 2 } +
                           androidx.compose.animation.shrinkVertically(animationSpec = tween(500, easing = LinearOutSlowInEasing))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            )
                            .padding(horizontal = 24.dp)
                    ) {
                        // Apple Music Timeline Slider
                    val currentPos = sliderPosition ?: position
                    
                    val trackInteractionSource = remember { MutableInteractionSource() }
                    val isTrackDragged by trackInteractionSource.collectIsDraggedAsState()
                    val isTrackPressed by trackInteractionSource.collectIsPressedAsState()
                    val isTrackActive = isTrackDragged || isTrackPressed
                    
                    val trackHeight by animateDpAsState(
                        targetValue = if (isTrackActive) 12.dp else 6.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "trackScale"
                    )
                    
                    Slider(
                        value = currentPos.toFloat(),
                        valueRange = 0f..(if (duration == androidx.media3.common.C.TIME_UNSET) 0f else duration.toFloat()),
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
                                colors = PlayerSliderColors.getSliderColors(
                                    activeColor = adaptivePrimary.copy(alpha = 0.8f),
                                    playerBackground = playerBackground,
                                    useDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp), // Align with internal slider padding
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(makeTimeString(currentPos), color = adaptiveSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                                    else if (canSkipPrevious) playerConnection.player.seekToPrevious()
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
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
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
                                    else if (canSkipNext) playerConnection.player.seekToNext()
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
                        Icon(Icons.Default.VolumeMute, contentDescription = "Volume Down", tint = adaptiveSecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        val volumeInteractionSource = remember { MutableInteractionSource() }
                        val isVolDragged by volumeInteractionSource.collectIsDraggedAsState()
                        val isVolPressed by volumeInteractionSource.collectIsPressedAsState()
                        val isVolActive = isVolDragged || isVolPressed
                        
                        val volHeight by animateDpAsState(
                            targetValue = if (isVolActive) 12.dp else 6.dp,
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
                                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetVolume, 0)
                                }
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
                                        useDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f).height(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                            Icon(Icons.Default.VolumeUp, contentDescription = "Volume Up", tint = adaptiveSecondary, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            // (Controls AnimatedVisibility closed above)

            // Bottom Utility Action Bar
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(animationSpec = tween(500, easing = LinearOutSlowInEasing)) +
                        slideInVertically(animationSpec = tween(500, easing = LinearOutSlowInEasing)) { it / 2 } +
                        androidx.compose.animation.expandVertically(animationSpec = tween(500, easing = LinearOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(500, easing = LinearOutSlowInEasing)) +
                       slideOutVertically(animationSpec = tween(500, easing = LinearOutSlowInEasing)) { it / 2 } +
                       androidx.compose.animation.shrinkVertically(animationSpec = tween(500, easing = LinearOutSlowInEasing))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                val isLyricsActive = playerState == PlayerInternalState.LYRICS
                IconButton(
                    onClick = { playerState = if (isLyricsActive) PlayerInternalState.COVER else PlayerInternalState.LYRICS }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lyrics_apple), 
                        contentDescription = "Lyrics", 
                        tint = if (isLyricsActive) adaptivePrimary else adaptiveSecondary, 
                        modifier = Modifier.size(28.dp)
                    )
                }

                Box(contentAlignment = Alignment.Center) {
                    if (bluetoothDeviceName != null) {
                        IconButton(
                            onClick = { showAudioDeviceBottomSheet = true },
                            modifier = Modifier.background(Color.Transparent, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                Icons.Default.Bluetooth, 
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
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .absoluteOffset(y = 30.dp)
                                .widthIn(max = 84.dp)
                        )
                    } else if (BuildConfig.CAST_AVAILABLE && enableGoogleCast) {
                        CastButton(
                            modifier = Modifier.size(48.dp),
                            tintColor = adaptiveSecondary,
                            activeTintColor = adaptivePrimary,
                            showBackground = false
                        )
                    } else {
                        IconButton(
                            onClick = { showAudioDeviceBottomSheet = true },
                            modifier = Modifier.background(Color.Transparent, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.speaker_apple),
                                contentDescription = "Speaker", 
                                tint = adaptiveSecondary, 
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                
                val isQueueActive = playerState == PlayerInternalState.QUEUE
                IconButton(
                    onClick = { playerState = if (isQueueActive) PlayerInternalState.COVER else PlayerInternalState.QUEUE }
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
        
        if (showAudioDeviceBottomSheet) {
            AudioDeviceBottomSheet(onDismiss = { showAudioDeviceBottomSheet = false })
        }
    }
}
