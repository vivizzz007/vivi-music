/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.WindowManager
import android.widget.Toast
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.res.pluralStringResource
import kotlin.math.roundToInt
import com.music.vivi.ui.component.ActionPromptDialog
import com.music.vivi.ui.component.MediaMetadataListItem
import com.music.vivi.ui.menu.QueueMenu
import com.music.vivi.ui.menu.SelectionMediaMetadataMenu
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.toMutableStateList
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Checkbox
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.Job
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.common.Timeline
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.updateTransition
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.unit.lerp as dpLerp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.produceState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import coil3.size.Size as CoilSize
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.media3.common.VideoSize
import com.music.vivi.di.LyricsHelperEntryPoint
import com.music.vivi.playback.PlayerConnection
import com.music.vivi.ui.menu.LyricsMenu
import kotlinx.coroutines.flow.MutableStateFlow
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalListenTogetherManager
import com.music.vivi.R
import com.music.vivi.constants.CropAlbumArtKey
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.HidePlayerThumbnailKey
import com.music.vivi.constants.EnableLyricsThumbnailPlayPauseKey
import com.music.vivi.constants.KeepScreenOn
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
import com.music.vivi.constants.PlayerButtonsStyle
import com.music.vivi.constants.PlayerButtonsStyleKey
import com.music.vivi.constants.PlayerHorizontalPadding
import com.music.vivi.constants.QueueEditLockKey
import com.music.vivi.constants.QueuePeekHeight
import com.music.vivi.constants.SliderStyle
import com.music.vivi.constants.SliderStyleKey
import com.music.vivi.constants.SquigglySliderKey
import com.music.vivi.constants.SwipeLyricsKey
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.constants.ThumbnailCornerRadiusKey
import com.music.vivi.constants.SwipeThumbnailKey
import com.music.vivi.constants.UseNewPlayerDesignKey
import com.music.vivi.db.entities.LyricsEntity
import com.music.vivi.extensions.SwipeGesture
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.extensions.toggleRepeatMode
import com.music.vivi.extensions.move
import com.music.vivi.listentogether.RoomRole
import com.music.vivi.models.MediaMetadata
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.vivimusic.getConnectedBluetoothDeviceName
import com.music.vivi.vivimusic.isBuds
import com.music.vivi.vivimusic.isSpeaker
import com.music.vivi.vivimusic.AudioDeviceBottomSheet
import com.music.vivi.ui.component.BottomSheet
import com.music.vivi.ui.component.BottomSheetState
import com.music.vivi.ui.player.backdrop.PlayerBlurBackdrop
import com.music.vivi.ui.player.backdrop.rememberArtworkBlurBitmap
import com.music.vivi.ui.component.LocalBottomSheetPageState
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.Lyrics
import com.music.vivi.ui.component.PlayerSliderTrack
import com.music.vivi.ui.component.ResizableIconButton
import com.music.vivi.ui.component.SquigglySlider
import com.music.vivi.ui.component.ThinSlider
import com.music.vivi.ui.component.WavySlider
import com.music.vivi.ui.component.rememberBottomSheetState
import com.music.vivi.ui.menu.OldPlayerMenu
import com.music.vivi.ui.menu.PlayerMenu
import com.music.vivi.ui.component.VolumeSlider
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.ui.theme.PlayerColorExtractor
import com.music.vivi.ui.theme.PlayerSliderColors
import com.music.vivi.ui.utils.ShowMediaInfo
import com.music.vivi.ui.utils.ShowOffsetDialog
import com.music.vivi.utils.makeTimeString
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.ermohdamaan.justforpixel.justforpixelexpressivelab.core.components.sliders.ExpressiveWavySlider
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import com.music.vivi.ui.component.Icon as MIcon
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.DefaultLoadControl
import android.view.TextureView
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.request.crossfade
import com.music.vivi.Motionartwork.apple.AppleMusicMotionartworkProvider
import com.music.vivi.Motionartwork.Motionartwork
import com.music.vivi.Motionartwork.tidal.TidalMotionartworkProvider
import com.music.vivi.Motionartwork.vivimusic.ViviMusicMotionartworkProvider
import com.music.vivi.constants.MotionartworkSource
import com.music.vivi.constants.MotionartworkSourceKey
import com.music.vivi.constants.CanvasThumbnailAnimationKey
import com.music.vivi.constants.SpotifyCanvasEnabledKey
import com.music.spotify.Spotify
import com.music.spotify.SpotifyCanvasProvider
import com.music.vivi.constants.CanvasLoadOnlyWifiKey
import com.music.vivi.extensions.metadata
import com.music.vivi.extensions.removeWindow
import timber.log.Timber
import com.music.vivi.ui.player.MotionartworkPlaybackCache
import com.music.vivi.utils.isWifiConnected
import com.music.vivi.utils.SpotifySessionManager
import java.util.Locale
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.isCollapsed, state.isDismissed) {
        if (!state.isCollapsed && !state.isDismissed) {
            keyboardController?.hide()
            focusManager.clearFocus()
            (context as? android.app.Activity)?.let { activity ->
                WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    .hide(WindowInsetsCompat.Type.ime())
            }
        }
    }


    val (useNewPlayerDesign, onUseNewPlayerDesignChange) = rememberPreference(
        UseNewPlayerDesignKey,
        defaultValue = false
    )
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(HidePlayerThumbnailKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)
    val thumbnailCornerRadius by rememberPreference(ThumbnailCornerRadiusKey, defaultValue = 3f)
    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.LIVE_MESH
    )
    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val enableCanvas by rememberPreference(CanvasThumbnailAnimationKey, true)
    val (motionartworkSource) = rememberEnumPreference(MotionartworkSourceKey, defaultValue = MotionartworkSource.AUTO)
    val canvasLoadOnlyWifi by rememberPreference(CanvasLoadOnlyWifiKey, defaultValue = false)
    val spotifyCanvasEnabled by rememberPreference(SpotifyCanvasEnabledKey, false)

    val shouldUseDarkButtonColors = remember(playerBackground, useDarkTheme) {
        when (playerBackground) {
            PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.GLOW_ANIMATED, PlayerBackgroundStyle.APPLE_MUSIC, PlayerBackgroundStyle.LIVE_MESH -> true
            PlayerBackgroundStyle.DEFAULT -> useDarkTheme
        }
    }


    val isPlaying by playerConnection.isPlaying.collectAsState()
    val swipeLyrics by rememberPreference(SwipeLyricsKey, false)
    val enableLyricsThumbnailPlayPause by rememberPreference(EnableLyricsThumbnailPlayPauseKey, false)
    val isKeepScreenOn by rememberPreference(KeepScreenOn, false)
    val keepScreenOn = isPlaying && isKeepScreenOn

    DisposableEffect(playerBackground, state.isExpanded, useDarkTheme, keepScreenOn) {
        val window = (context as? android.app.Activity)?.window
        if (window != null && state.isExpanded) {
            WindowCompat.getInsetsController(window, window.decorView).hide(WindowInsetsCompat.Type.ime())
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)

            
            when (playerBackground) {
                PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.GLOW_ANIMATED, PlayerBackgroundStyle.APPLE_MUSIC, PlayerBackgroundStyle.LIVE_MESH -> {
                    insetsController.isAppearanceLightStatusBars = false
                }
                PlayerBackgroundStyle.DEFAULT -> {
                    insetsController.isAppearanceLightStatusBars = !useDarkTheme
                }
            }

            if (keepScreenOn && state.isExpanded)
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)

                insetsController.isAppearanceLightStatusBars = !useDarkTheme
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    val onBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val useBlackBackground =
        remember(isSystemInDarkTheme, darkTheme, pureBlack) {
            val useDarkTheme =
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            useDarkTheme && pureBlack
        }

    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val nextMediaMetadata by playerConnection.nextMediaMetadata.collectAsState()
    val crossfadeIncomingItem by playerConnection.crossfadeIncomingItem.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val automix by playerConnection.service.automixItems.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val isMuted by playerConnection.isMuted.collectAsState()
    val playerVolume by playerConnection.service.playerVolume.collectAsState()

    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.SLIM)
    val squigglySlider by rememberPreference(SquigglySliderKey, defaultValue = false)
    
    // Listen Together state (reactive)
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsState(initial = RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST
    
    // Cast state - safely access castConnectionHandler to prevent crashes during service lifecycle changes
    val castHandler = remember(playerConnection) {
        try {
            playerConnection.service.castConnectionHandler
        } catch (e: Exception) {
            null
        }
    }

    val isCasting by castHandler?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) }
    val castPosition by castHandler?.castPosition?.collectAsState() ?: remember { mutableLongStateOf(0L) }
    val castDuration by castHandler?.castDuration?.collectAsState() ?: remember { mutableLongStateOf(0L) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val castVolume by castHandler?.castVolume?.collectAsState() ?: remember { mutableFloatStateOf(1f) }
    
    // Use Cast state when casting, otherwise local player
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying

    // Use State objects for position/duration to pass to MiniPlayer without causing recomposition
    // These states persist across playback state changes to ensure continuous progress updates
    val positionState = remember { mutableLongStateOf(0L) }
    val durationState = remember { mutableLongStateOf(0L) }
    
    // Convenience accessors for local use
    var position by positionState
    var duration by durationState
    
    val effectivePosition by remember {
        derivedStateOf {
            if (isCasting) {
                castPosition
            } else {
                position
            }
        }
    }

    
    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }
    // Track when we last manually set position to avoid Cast overwriting it
    var lastManualSeekTime by remember { mutableLongStateOf(0L) }
    
    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    val bluetoothDeviceName by produceState<String?>(initialValue = getConnectedBluetoothDeviceName(context)) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                value = getConnectedBluetoothDeviceName(context)
            }
        }

        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            object : android.media.AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    value = getConnectedBluetoothDeviceName(context)
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    value = getConnectedBluetoothDeviceName(context)
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
            audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        }
        
        awaitDispose {
            context.unregisterReceiver(receiver)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && callback != null) {
                audioManager.unregisterAudioDeviceCallback(callback)
            }
        }
    }

    var showAudioDeviceBottomSheet by remember { mutableStateOf(false) }
    var lastNonNullName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(bluetoothDeviceName) {
        if (bluetoothDeviceName != null) lastNonNullName = bluetoothDeviceName
    }
    val nameToShow = bluetoothDeviceName ?: lastNonNullName


    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxSystemVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    val systemVolume by produceState(initialValue = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxSystemVolume) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
                    value = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxSystemVolume
                }
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context.registerReceiver(receiver, filter)
        awaitDispose {
            context.unregisterReceiver(receiver)
        }
    }


    val defaultGradientColors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata?.id, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT || playerBackground == PlayerBackgroundStyle.GLOW_ANIMATED) {
            val currentMetadata = mediaMetadata
            if (currentMetadata != null && currentMetadata.thumbnailUrl != null) {
                val cachedColors = gradientColorsCache[currentMetadata.id]
                if (cachedColors != null) {
                    gradientColors = cachedColors
                    return@LaunchedEffect
                }
                withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context)
                        .data(currentMetadata.thumbnailUrl)
                        .size(100, 100)
                        .allowHardware(false)
                        .memoryCacheKey("gradient_${currentMetadata.id}")
                        .build()

                    val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
                    if (result != null) {
                        val bitmap = result.image?.toBitmap()
                        if (bitmap != null) {
                            val palette = withContext(Dispatchers.Default) {
                                Palette.from(bitmap)
                                    .maximumColorCount(8)
                                    .resizeBitmapArea(100 * 100)
                                    .generate()
                            }
                            val extractedColors = if (playerBackground == PlayerBackgroundStyle.GLOW_ANIMATED) {
                                listOfNotNull(
                                    palette.getVibrantColor(fallbackColor).let { Color(it) },
                                    palette.getLightVibrantColor(fallbackColor).let { Color(it) },
                                    palette.getDarkVibrantColor(fallbackColor).let { Color(it) },
                                    palette.getMutedColor(fallbackColor).let { Color(it) },
                                    palette.getLightMutedColor(fallbackColor).let { Color(it) },
                                    palette.getDarkMutedColor(fallbackColor).let { Color(it) }
                                ).distinct()
                            } else {
                                PlayerColorExtractor.extractGradientColors(
                                    palette = palette,
                                    fallbackColor = fallbackColor
                                )
                            }
                            gradientColorsCache[currentMetadata.id] = extractedColors
                            withContext(Dispatchers.Main) { gradientColors = extractedColors }
                        }
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }


    // Preload incoming background thumbnail and gradient colors so the transition into next song has ZERO splash
    val incomingBackgroundItem = crossfadeIncomingItem ?: nextMediaMetadata
    LaunchedEffect(incomingBackgroundItem?.id, playerBackground) {
        val nextThumbnail = incomingBackgroundItem?.thumbnailUrl ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val req100 = ImageRequest.Builder(context)
                .data(nextThumbnail)
                .size(100, 100)
                .allowHardware(false)
                .build()
            context.imageLoader.execute(req100)

            val req128 = ImageRequest.Builder(context)
                .data(nextThumbnail)
                .size(128, 128)
                .allowHardware(false)
                .build()
            context.imageLoader.execute(req128)

            if ((playerBackground == PlayerBackgroundStyle.GRADIENT || playerBackground == PlayerBackgroundStyle.GLOW_ANIMATED) &&
                !gradientColorsCache.containsKey(incomingBackgroundItem.id)
            ) {
                val reqGradient = ImageRequest.Builder(context)
                    .data(nextThumbnail)
                    .size(100, 100)
                    .allowHardware(false)
                    .memoryCacheKey("gradient_${incomingBackgroundItem.id}")
                    .build()
                val result = runCatching { context.imageLoader.execute(reqGradient) }.getOrNull()
                val bitmap = result?.image?.toBitmap()
                if (bitmap != null) {
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap)
                            .maximumColorCount(8)
                            .resizeBitmapArea(100 * 100)
                            .generate()
                    }
                    val extractedColors = if (playerBackground == PlayerBackgroundStyle.GLOW_ANIMATED) {
                        listOfNotNull(
                            palette.getVibrantColor(fallbackColor).let { Color(it) },
                            palette.getLightVibrantColor(fallbackColor).let { Color(it) },
                            palette.getDarkVibrantColor(fallbackColor).let { Color(it) },
                            palette.getMutedColor(fallbackColor).let { Color(it) },
                            palette.getLightMutedColor(fallbackColor).let { Color(it) },
                            palette.getDarkMutedColor(fallbackColor).let { Color(it) }
                        ).distinct()
                    } else {
                        PlayerColorExtractor.extractGradientColors(
                            palette = palette,
                            fallbackColor = fallbackColor
                        )
                    }
                    gradientColorsCache[incomingBackgroundItem.id] = extractedColors
                }
            }
        }
    }


    val TextBackgroundColor by animateColorAsState(
        targetValue = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
            PlayerBackgroundStyle.BLUR -> Color.White
            PlayerBackgroundStyle.GRADIENT -> Color.White
            PlayerBackgroundStyle.GLOW_ANIMATED -> Color.White
            PlayerBackgroundStyle.APPLE_MUSIC -> Color.White
            PlayerBackgroundStyle.LIVE_MESH -> Color.White
        },
        label = "TextBackgroundColor"
    )

    val icBackgroundColor by animateColorAsState(
        targetValue = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
            PlayerBackgroundStyle.BLUR -> Color.Black
            PlayerBackgroundStyle.GRADIENT -> Color.Black
            PlayerBackgroundStyle.GLOW_ANIMATED -> Color.Black
            PlayerBackgroundStyle.APPLE_MUSIC -> Color.Black
            PlayerBackgroundStyle.LIVE_MESH -> Color.Black
        },
        label = "icBackgroundColor"
    )

    val albumTitle = mediaMetadata?.album?.title
    var canvasArtwork by remember(mediaMetadata?.id, albumTitle) { mutableStateOf<Motionartwork?>(null) }
    var canvasFetchInFlight by remember(mediaMetadata?.id, albumTitle) { mutableStateOf(false) }
    var spotifyCanvasUrl by remember(mediaMetadata?.id, albumTitle) { mutableStateOf<String?>(null) }
    
    val isNetworkConnected by (LocalPlayerConnection.current?.service?.connectivityObserver?.networkStatus ?: MutableStateFlow(false)).collectAsState(initial = false)

    LaunchedEffect(mediaMetadata?.id, albumTitle, playerBackground, motionartworkSource, spotifyCanvasEnabled, Spotify.accessToken, isNetworkConnected, enableCanvas, useNewPlayerDesign) {
        val item = mediaMetadata ?: return@LaunchedEffect
        
        if (!spotifyCanvasEnabled || !useNewPlayerDesign) {
            spotifyCanvasUrl = null
        } else {
            val prefs = context.getSharedPreferences("spotify_canvas_url_map", Context.MODE_PRIVATE)
            val cachedUrl = prefs.getString(item.id, null)
            if (cachedUrl != null) {
                spotifyCanvasUrl = cachedUrl
                timber.log.Timber.d("SpotifyCanvas: Loaded url from local SharedPreferences cache for offline bypass - ID: ${item.id}")
            } else {
                val accessToken = SpotifySessionManager.getValidAccessToken(context)
                if (accessToken != null && (!canvasLoadOnlyWifi || isWifiConnected(context))) {
                    timber.log.Timber.d("SpotifyCanvas: Triggering Canvas fetch for track: ${item.title}")
                    withContext(Dispatchers.IO) {
                        // Try REST API first as it's more reliable
                        val trackId = Spotify.searchTrackRest("${item.title} ${item.artists.joinToString { it.name }}") 
                            ?: Spotify.searchTrack("${item.title} ${item.artists.joinToString { it.name }}")?.id

                        timber.log.Timber.d("SpotifyCanvas: Resolved Track ID from search: $trackId")

                        if (trackId != null) {
                            val canvasRes = SpotifyCanvasProvider.getCanvasUrl(trackId, accessToken)
                            timber.log.Timber.d("SpotifyCanvas: Canvas URL extraction result: $canvasRes")
                            val fetchedUrl = canvasRes.getOrNull()
                            if (fetchedUrl != null) {
                                prefs.edit().putString(item.id, fetchedUrl).apply()
                            }
                            withContext(Dispatchers.Main) {
                                spotifyCanvasUrl = fetchedUrl
                            }
                        }
                    }
                } else {
                    timber.log.Timber.d("SpotifyCanvas: Conditions not met to fetch. canvasEnabled=$spotifyCanvasEnabled, tokenReady=${Spotify.accessToken != null}, itemReady=${item != null}")
                }
            }
        }

        if (!enableCanvas) {
            canvasArtwork = null
            return@LaunchedEffect
        }
        if (canvasLoadOnlyWifi && !isWifiConnected(context)) {
            canvasArtwork = null
            return@LaunchedEffect
        }
        
        val cacheKey = "${item.id}:${motionartworkSource.name}"
        // Use cached artwork if available
        MotionartworkPlaybackCache.get(cacheKey)?.let { cached ->
            canvasArtwork = cached
            spotifyCanvasUrl = null // Prioritize Motionartwork, disable Spotify Canvas
            return@LaunchedEffect
        }

        if (canvasFetchInFlight) return@LaunchedEffect
        canvasFetchInFlight = true
        
        withContext(Dispatchers.IO) {
            val storefront = Locale.getDefault().country.lowercase(Locale.ROOT).takeIf { it.length == 2 } ?: "us"
            val requestedTitle = item.title
            val requestedArtist = item.artists.joinToString { it.name }
            val requestedAlbum = item.album?.title ?: ""

            val s = requestedTitle
            val a = requestedArtist

            android.util.Log.d("CanvasDebug", "=== Canvas Fetch START ===")
            android.util.Log.d("CanvasDebug", "Source     : $motionartworkSource")
            android.util.Log.d("CanvasDebug", "Storefront : $storefront")
            android.util.Log.d("CanvasDebug", "Title      : '$requestedTitle'")
            android.util.Log.d("CanvasDebug", "Artist     : '$requestedArtist'")
            android.util.Log.d("CanvasDebug", "Album      : '$requestedAlbum' (blank=${requestedAlbum.isBlank()})")

            val fetched = when (motionartworkSource) {
                MotionartworkSource.AUTO -> {
                    AppleMusicMotionartworkProvider.getBySongArtist(s, a, requestedAlbum, storefront)
                        ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() && validateMotionartworkMatch(it, s, a, requestedAlbum) }
                        ?: TidalMotionartworkProvider.getBySongArtist(s, a, requestedAlbum)
                        ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() && validateMotionartworkMatch(it, s, a, requestedAlbum) }
                        ?: ViviMusicMotionartworkProvider.getBySongArtist(s, a, requestedAlbum)
                        ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() && validateMotionartworkMatch(it, s, a, requestedAlbum) }
                }
                MotionartworkSource.APPLE_MUSIC -> {
                    AppleMusicMotionartworkProvider.getBySongArtist(s, a, requestedAlbum, storefront)
                        ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                }
                MotionartworkSource.VIVIMUSIC -> {
                    ViviMusicMotionartworkProvider.getBySongArtist(s, a, requestedAlbum)
                        ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                }
                MotionartworkSource.TIDAL -> {
                    TidalMotionartworkProvider.getBySongArtist(s, a, requestedAlbum)
                        ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                }
            }

            android.util.Log.d("CanvasDebug", "Fetched artwork: ${if (fetched == null) "NULL" else "non-null"}")
            if (fetched != null) {
                android.util.Log.d("CanvasDebug", "  artwork.name      : '${fetched.name}'")
                android.util.Log.d("CanvasDebug", "  artwork.artist    : '${fetched.artist}'")
                android.util.Log.d("CanvasDebug", "  artwork.albumName : '${fetched.albumName}'")
                android.util.Log.d("CanvasDebug", "  artwork.animUrl   : '${fetched.preferredAnimationUrl}'")
            }

            val validated = fetched?.let { artwork ->
                // For AUTO mode validation is already done per-provider inside takeIf.
                // For single-source modes this block acts as the safety net.
                val passes = validateMotionartworkMatch(artwork, requestedTitle, requestedArtist, requestedAlbum)

                android.util.Log.d("CanvasDebug", "Validation:")
                android.util.Log.d("CanvasDebug", "  artistMatches  : ${artwork.artist.orEmpty().trim().equals(requestedArtist.trim(), ignoreCase = true)}  ('${artwork.artist?.trim()}' vs '${requestedArtist.trim()}')")
                android.util.Log.d("CanvasDebug", "  songMatches    : ${artwork.name.orEmpty().trim().equals(requestedTitle.trim(), ignoreCase = true)}  ('${artwork.name?.trim()}' vs '${requestedTitle.trim()}')")
                android.util.Log.d("CanvasDebug", "  albumMatches   : ${artwork.albumName.orEmpty().trim().equals(requestedAlbum.trim(), ignoreCase = true)}  ('${artwork.albumName?.trim()}' vs '${requestedAlbum.trim()}')")
                android.util.Log.d("CanvasDebug", "  RESULT         : $passes")

                if (passes) artwork else null
            }

            android.util.Log.d("CanvasDebug", "Final validated: ${if (validated == null) "NULL (canvas will NOT show)" else "OK (canvas will show)"}")
            android.util.Log.d("CanvasDebug", "=== Canvas Fetch END ===")

            withContext(Dispatchers.Main) {
                canvasArtwork = validated
                if (validated != null) {
                    MotionartworkPlaybackCache.put(cacheKey, validated)
                    spotifyCanvasUrl = null // Prioritize Motionartwork, disable Spotify Canvas
                }
                canvasFetchInFlight = false
            }
        }
    }


    val (textButtonColor, iconButtonColor) = when {
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


    // Separate colors for Previous/Next buttons in PRIMARY/TERTIARY modes
    val (sideButtonContainerColor, sideButtonContentColor) = when {
        playerBackground == PlayerBackgroundStyle.BLUR || 
        playerBackground == PlayerBackgroundStyle.GRADIENT -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT -> Pair(
                    Color.White.copy(alpha = 0.2f), 
                    Color.White
                )
                PlayerButtonsStyle.PRIMARY -> Pair(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer
                )
                PlayerButtonsStyle.TERTIARY -> Pair(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
        playerBackground == PlayerBackgroundStyle.GLOW_ANIMATED -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT -> Pair(
                    Color.White.copy(alpha = 0.2f), 
                    Color.White
                )
                PlayerButtonsStyle.PRIMARY -> Pair(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer
                )
                PlayerButtonsStyle.TERTIARY -> Pair(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
        else -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT -> Pair(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    MaterialTheme.colorScheme.onSurface
                )
                PlayerButtonsStyle.PRIMARY -> Pair(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer
                )
                PlayerButtonsStyle.TERTIARY -> Pair(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }


    val download by LocalDownloadUtil.current.getDownload(mediaMetadata?.id ?: "")
        .collectAsState(initial = null)

    val sleepTimerEnabled =
        remember(
            playerConnection.service.sleepTimer.triggerTime,
            playerConnection.service.sleepTimer.pauseWhenSongEnd
        ) {
            playerConnection.service.sleepTimer.isActive
        }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }


    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showInlineLyrics by rememberSaveable {
        mutableStateOf(false)
    }

    var showInlineQueue by rememberSaveable {
        mutableStateOf(false)
    }

    BackHandler(enabled = (showInlineLyrics || showInlineQueue) && !useNewPlayerDesign) {
        showInlineLyrics = false
        showInlineQueue = false
    }

    var isFullScreen by rememberSaveable {
        mutableStateOf(false)
    }

    // Position update - only for local playback
    // When casting, we use castPosition directly to avoid sync issues
    // Use isPlaying instead of playbackState to ensure continuous updates during playback
    LaunchedEffect(isPlaying, isCasting) {
        if (!isCasting && isPlaying) {
            while (isActive) {
                delay(100) // Update more frequently for smoother progress bar
                if (sliderPosition == null) { // Only update if user isn't dragging
                    position = playerConnection.player.currentPosition
                    duration = playerConnection.player.duration
                }
            }
        }
    }

    
    // Also update position when playback state changes (e.g., song change, seek)
    LaunchedEffect(playbackState, mediaMetadata?.id) {
        if (!isCasting) {
            position = playerConnection.player.currentPosition
            duration = playerConnection.player.duration
        }
    }

    
    // When casting, use Cast position/duration directly
    // But wait a bit after manual seeks to let Cast catch up
    LaunchedEffect(isCasting, castPosition, castDuration) {
        if (isCasting && sliderPosition == null) {
            val timeSinceManualSeek = System.currentTimeMillis() - lastManualSeekTime
            if (timeSinceManualSeek > 1500) {
                // Only update from Cast if we haven't manually seeked recently
                position = castPosition
                if (castDuration > 0) duration = castDuration
            }
        }
    }


    val dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

    val queueSheetState = rememberBottomSheetState(
        dismissedBound = if (useNewPlayerDesign) dismissedBound else 0.dp,
        expandedBound = state.expandedBound,
        collapsedBound = if (useNewPlayerDesign) dismissedBound + 1.dp else 0.dp,
        initialAnchor = if (useNewPlayerDesign) 1 else 0
    )

    val isCanvasActive = useNewPlayerDesign && spotifyCanvasUrl != null
    var showCanvasControls by rememberSaveable(mediaMetadata?.id, isCanvasActive) { mutableStateOf(true) }

    LaunchedEffect(isCanvasActive, showCanvasControls, isPlaying, showInlineLyrics, sliderPosition) {
        if (isCanvasActive && showCanvasControls && isPlaying && !showInlineLyrics && sliderPosition == null) {
            delay(4000L)
            showCanvasControls = false
        }
    }

    val canvasControlsAlpha by animateFloatAsState(
        targetValue = if (isCanvasActive && !showCanvasControls && !showInlineLyrics && !queueSheetState.isExpanded) 0f else 1f,
        label = "canvasControlsAlpha"
    )

    val bottomSheetBackgroundColor = when {
        useNewPlayerDesign && spotifyCanvasUrl != null -> Color.Black
        playerBackground == PlayerBackgroundStyle.BLUR ||
        playerBackground == PlayerBackgroundStyle.GRADIENT ||
        playerBackground == PlayerBackgroundStyle.GLOW_ANIMATED ||
        playerBackground == PlayerBackgroundStyle.APPLE_MUSIC -> Color.Black
        playerBackground == PlayerBackgroundStyle.LIVE_MESH ->
            if (useDarkTheme) Color(0xFF191919) else Color(0xFFE6E6E6)
        else ->
            if (useBlackBackground) Color.Black
            else MaterialTheme.colorScheme.surfaceContainer
    }

    val backgroundAlpha = state.progress.coerceIn(0f, 1f)

    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bottomSheetBackgroundColor)
            ) {
                if (useNewPlayerDesign && spotifyCanvasUrl != null) {
                    val lyricsBlurAlpha by animateFloatAsState(
                        targetValue = if (showInlineLyrics || showInlineQueue) 1f else 0f,
                        animationSpec = tween(400),
                        label = "canvasLyricsBlur"
                    )
                    AnimatedContent(
                        targetState = spotifyCanvasUrl,
                        transitionSpec = { fadeIn(tween(800)) togetherWith fadeOut(tween(800)) },
                        label = "spotifyCanvasBackground"
                    ) { url ->
                        if (url != null) {
                            Box(modifier = Modifier.fillMaxSize().alpha(backgroundAlpha)) {
                                if (backgroundAlpha > 0.01f) {
                                    BackgroundVideoView(
                                        videoUrl = url,
                                        isPlaying = isPlaying,
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                if (lyricsBlurAlpha > 0.01f) {
                                    PlayerBlurBackdrop(
                                        artworkUri = mediaMetadata?.thumbnailUrl?.toUri(),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .alpha(lyricsBlurAlpha)
                                    )
                                }
                            }
                        }
                    }
                } else when (playerBackground) {
                    PlayerBackgroundStyle.BLUR -> {
                        PlayerBlurBackdrop(
                            artworkUri = mediaMetadata?.thumbnailUrl?.toUri(),
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(backgroundAlpha)
                        )
                    }
                    PlayerBackgroundStyle.GRADIENT -> {
                        AnimatedContent(
                            targetState = gradientColors,
                            transitionSpec = {
                                fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                            },
                            label = "gradientBackground"
                        ) { colors ->
                            if (colors.isNotEmpty()) {
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
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .alpha(backgroundAlpha)
                                        .background(Brush.verticalGradient(colorStops = gradientColorStops))
                                        .background(Color.Black.copy(alpha = 0.2f))
                                )
                            }
                        }
                    }
                    PlayerBackgroundStyle.GLOW_ANIMATED -> {
                        AnimatedContent(
                            targetState = gradientColors,
                            transitionSpec = {
                                fadeIn(tween(1200)) togetherWith fadeOut(tween(1200))
                            },
                            label = "GlowAnimatedContent"
                        ) { colors ->
                            if (colors.isNotEmpty()) {
                                val infiniteTransition =
                                    rememberInfiniteTransition(label = "GlowAnimation")

                                val progress by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(20000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "glowProgress"
                                )

                                fun rotatedColorAt(index: Int): Color {
                                    val size = colors.size
                                    val idx = index.toFloat() + progress * size
                                    val a = kotlin.math.floor(idx).toInt() % size
                                    val b = (a + 1) % size
                                    val frac = idx - kotlin.math.floor(idx)
                                    return colorLerp(
                                        colors.getOrElse(a) { Color.DarkGray },
                                        colors.getOrElse(b) { Color.DarkGray },
                                        frac
                                    )
                                }

                                fun oscillate(
                                    min: Float,
                                    max: Float,
                                    phase: Float,
                                    speed: Float = 1f
                                ): Float {
                                    val v = kotlin.math.sin(
                                        2f * kotlin.math.PI.toFloat() * (progress * speed + phase)
                                    )
                                    return min + (max - min) * ((v + 1f) * 0.5f)
                                }

                                val color1 = rotatedColorAt(0)
                                val color2 = rotatedColorAt(1)
                                val color3 = rotatedColorAt(2)
                                val color4 = rotatedColorAt(3)
                                val color5 = rotatedColorAt(4)
                                val color6 = rotatedColorAt(5)

                                val o1x = oscillate(0.0f, 1.0f, 0.00f, 1.0f)
                                val o1y = oscillate(0.0f, 0.5f, 0.07f, 1.0f)
                                val r1 = oscillate(0.8f, 1.6f, 0.12f, 1.0f)

                                val o2x = oscillate(1.0f, 0.0f, 0.2f, 1.0f)
                                val o2y = oscillate(0.5f, 1.0f, 0.25f, 1.0f)
                                val r2 = oscillate(0.7f, 1.5f, 0.18f, 1.0f)

                                val o3x = oscillate(0.2f, 0.8f, 0.33f, 1.0f)
                                val o3y = oscillate(0.8f, 0.2f, 0.36f, 1.0f)
                                val r3 = oscillate(0.6f, 1.4f, 0.29f, 1.0f)

                                val o4x = oscillate(0.3f, 0.7f, 0.44f, 1.0f)
                                val o4y = oscillate(0.2f, 0.8f, 0.41f, 1.0f)
                                val r4 = oscillate(0.9f, 1.7f, 0.47f, 1.0f)

                                val o5x = oscillate(0.4f, 0.6f, 0.55f, 1.0f)
                                val o5y = oscillate(0.0f, 1.0f, 0.51f, 1.0f)
                                val r5 = oscillate(0.7f, 1.5f, 0.58f, 1.0f)

                                val o6x = oscillate(0.0f, 1.0f, 0.66f, 1.0f)
                                val o6y = oscillate(0.5f, 0.7f, 0.62f, 1.0f)
                                val r6 = oscillate(0.8f, 1.8f, 0.69f, 1.0f)

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(backgroundAlpha)
                                        .drawWithCache {
                                            val width = size.width
                                            val height = size.height
                                            val baseColor = Color(0xFF050505)

                                            val brush1 = Brush.radialGradient(
                                                colors = listOf(
                                                    color1.copy(alpha = 0.85f),
                                                    color1.copy(alpha = 0.5f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(width * o1x, height * o1y),
                                                radius = width * r1
                                            )
                                            val brush2 = Brush.radialGradient(
                                                colors = listOf(
                                                    color2.copy(alpha = 0.8f),
                                                    color2.copy(alpha = 0.45f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(width * o2x, height * o2y),
                                                radius = width * r2
                                            )
                                            val brush3 = Brush.radialGradient(
                                                colors = listOf(
                                                    color3.copy(alpha = 0.75f),
                                                    color3.copy(alpha = 0.4f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(width * o3x, height * o3y),
                                                radius = width * r3
                                            )
                                            val brush4 = Brush.radialGradient(
                                                colors = listOf(
                                                    color4.copy(alpha = 0.7f),
                                                    color4.copy(alpha = 0.35f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(width * o4x, height * o4y),
                                                radius = width * r4
                                            )
                                            val brush5 = Brush.radialGradient(
                                                colors = listOf(
                                                    color5.copy(alpha = 0.65f),
                                                    color5.copy(alpha = 0.3f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(width * o5x, height * o5y),
                                                radius = width * r5
                                            )
                                            val brush6 = Brush.radialGradient(
                                                colors = listOf(
                                                    color6.copy(alpha = 0.6f),
                                                    color6.copy(alpha = 0.25f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(width * o6x, height * o6y),
                                                radius = width * r6
                                            )

                                            onDrawBehind {
                                                drawRect(color = baseColor)
                                                drawRect(brush = brush1)
                                                drawRect(brush = brush2)
                                                drawRect(brush = brush3)
                                                drawRect(brush = brush4)
                                                drawRect(brush = brush5)
                                                drawRect(brush = brush6)
                                            }
                                        }
                                )
                            }
                        }
                    }
                    PlayerBackgroundStyle.APPLE_MUSIC -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(backgroundAlpha)
                        ) {
                            // Layer 1: Full-Screen Blurred Backdrop (Cached & persistent across song changes - zero flicker)
                            val blurBitmap = rememberArtworkBlurBitmap(mediaMetadata?.thumbnailUrl?.toUri())
                            Crossfade(
                                targetState = blurBitmap,
                                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                                label = "appleMusicBlurCrossfade"
                            ) { bitmap ->
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            // Layer 1.5: Artwork Depth Scrim
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0.00f to Color.Black.copy(alpha = 0.34f),
                                            0.55f to Color.Black.copy(alpha = 0.48f),
                                            1.00f to Color.Black.copy(alpha = 0.64f)
                                        )
                                    )
                            )

                            // Layer 2: Clear Artwork (Dissolves along smootherStep into backdrop)
                            // Only drawn in background for new player; old player uses the foreground morphing sleeve.
                            if (useNewPlayerDesign) {
                                val clearArtworkAlpha by animateFloatAsState(
                                    targetValue = if (showInlineLyrics || showInlineQueue) 0f else 1f,
                                    animationSpec = tween(500),
                                    label = "clearArtworkAlpha"
                                )
                                Crossfade(
                                    targetState = mediaMetadata?.thumbnailUrl,
                                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                                    label = "appleMusicClearArtworkCrossfade"
                                ) { thumbnailUrl ->
                                    if (thumbnailUrl != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(0.65f) // Occupies top 65%
                                                .alpha(clearArtworkAlpha)
                                                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                                .drawWithContent {
                                                    drawContent()
                                                    // Along smootherstep, so neither end of the dissolve shows as a line
                                                    drawRect(
                                                        brush = Brush.verticalGradient(
                                                            colorStops = Array(17) { i ->
                                                                val t = i / 16f
                                                                t to Color.Black.copy(alpha = 1f - smootherStep(t))
                                                            },
                                                            startY = size.height * (1f - 0.42f),
                                                            endY = size.height,
                                                        ),
                                                        blendMode = BlendMode.DstIn
                                                    )
                                                }
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(thumbnailUrl)
                                                    .size(CoilSize.ORIGINAL)
                                                    .crossfade(600)
                                                    .build(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            if (enableCanvas && canvasArtwork != null && backgroundAlpha > 0.01f) {
                                                BackgroundVideoView(
                                                    videoUrl = canvasArtwork?.animatedTall ?: canvasArtwork?.animated ?: canvasArtwork?.videoUrl ?: "",
                                                    isPlaying = isPlaying,
                                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    PlayerBackgroundStyle.LIVE_MESH -> {
                        if (backgroundAlpha > 0.01f) {
                            val thumbnailUrl = mediaMetadata?.thumbnailUrl
                            AndroidView<BlendView>(factory = { ctx -> BlendView(ctx) }, update = { blendView -> if (isPlaying) blendView.startRotationAnimation() else blendView.stopRotationAnimation(); blendView.setImageUrl(thumbnailUrl) }, modifier = Modifier.fillMaxSize())
                        }
                    }
                    PlayerBackgroundStyle.DEFAULT -> {
                        // Nothing
                    }
                }
            }
        },
        onDismiss = {
            playerConnection.service.clearAutomix()
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        collapsedContent = {
            MiniPlayer(
                positionState = positionState,
                durationState = durationState
            )
        },
    ) {
        val density = LocalDensity.current
        var trackInfoHeightDp by remember { mutableStateOf(56.dp) }
        val trackInfoSpacerHeight = 20.dp

        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            val trackInfoContent: @Composable () -> Unit = {
                val controlsHeaderAlpha by animateFloatAsState(
                    targetValue = if (!useNewPlayerDesign && (showInlineLyrics || showInlineQueue)) 0f else 1f,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                    label = "controlsHeaderAlpha",
                )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
                    .onSizeChanged { size ->
                        if (size.height > 0) {
                            trackInfoHeightDp = with(density) { size.height.toDp() }
                        }
                    }
                    .graphicsLayer { alpha = controlsHeaderAlpha },
            ) {
                AnimatedContent(
                    targetState = useNewPlayerDesign && (showInlineLyrics || showInlineQueue || spotifyCanvasUrl != null),
                    label = "ThumbnailAnimation"
                ) { showSmallThumbnail ->
                    if (showSmallThumbnail) {
                        Row {
                            if (hidePlayerThumbnail) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.vivi_music_small_icon),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(32.dp),
                                        tint = textButtonColor.copy(alpha = 0.7f)
                                    )
                                }
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                        .clickable(enabled = isFullScreen && enableLyricsThumbnailPlayPause) {
                                            playerConnection.togglePlayPause()
                                        }
                                ) {
                                    val isYtThumbnail = remember(mediaMetadata.thumbnailUrl) {
                                        mediaMetadata.thumbnailUrl?.let { it.contains("ytimg") || it.contains("/vi/") || it.contains("youtube.com") } == true
                                    }
                                    AsyncImage(
                                        model = mediaMetadata.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                if (cropAlbumArt && isYtThumbnail) {
                                                    scaleX = 4f / 3f
                                                    scaleY = 4f / 3f
                                                }
                                            }
                                    )

                                    if (isFullScreen && enableLyricsThumbnailPlayPause) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = if (isPlaying) 0f else 0.4f))
                                        )

                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = !isPlaying,
                                            enter = fadeIn(),
                                            exit = fadeOut()
                                        ) {
                                            Icon(
                                                painter = painterResource(
                                                    if (playbackState == Player.STATE_ENDED) R.drawable.replay
                                                    else R.drawable.play
                                                ),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(0.dp))
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .SwipeGesture(
                            enabled = isFullScreen && swipeLyrics,
                            onSwipeRight = { playerConnection.seekToPrevious() },
                            onSwipeLeft = { playerConnection.seekToNext() }
                        )
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
                            color = TextBackgroundColor,
                            modifier =
                            Modifier
                                .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                .combinedClickable(
                                    enabled = controlsHeaderAlpha > 0.5f,
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {
                                        if (mediaMetadata.album != null) {
                                            navController.navigate("album/${mediaMetadata.album.id}")
                                            state.collapseSoft()
                                        }
                                    },
                                    onLongClick = {
                                        val clip = ClipData.newPlainText(context.getString(R.string.copied_title), title)
                                        clipboardManager.setPrimaryClip(clip)
                                        Toast
                                            .makeText(context, context.getString(R.string.copied_title), Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                )
                            ,
                        )
                    }

                    Spacer(Modifier.height(if (useNewPlayerDesign) 4.dp else 2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (mediaMetadata.explicit) {
                            MIcon.Explicit(
                                tint = if (useNewPlayerDesign) TextBackgroundColor else TextBackgroundColor.copy(alpha = 0.65f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
                            val artistColor = if (useNewPlayerDesign) TextBackgroundColor else TextBackgroundColor.copy(alpha = 0.65f)
                            val artistFontWeight = if (useNewPlayerDesign) FontWeight.Normal else FontWeight.SemiBold
                            val annotatedString = buildAnnotatedString {
                                withStyle(SpanStyle(color = artistColor, fontSize = 16.sp, fontWeight = artistFontWeight)) {
                                    mediaMetadata.artists.forEachIndexed { index, artist ->
                                        val tag = "artist_${artist.id.orEmpty()}"
                                        pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty())
                                        append(artist.name)
                                        pop()
                                        if (index != mediaMetadata.artists.lastIndex) append(", ")
                                    }
                                }
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
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = artistColor,
                                        fontSize = 16.sp,
                                        fontWeight = artistFontWeight
                                    ),
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
                                        .combinedClickable(
                                            enabled = controlsHeaderAlpha > 0.5f,
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                            onClick = {
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
                                            },
                                            onLongClick = {
                                                val clip =
                                                    ClipData.newPlainText(
                                                        context.getString(R.string.copied_artist),
                                                        annotatedString
                                                    )
                                                clipboardManager.setPrimaryClip(clip)
                                                Toast
                                                    .makeText(
                                                        context,
                                                        context.getString(R.string.copied_artist),
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                            }
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                if (useNewPlayerDesign) {
                    val shareShape = RoundedCornerShape(
                        topStart = 50.dp, bottomStart = 50.dp,
                        topEnd = 3.dp, bottomEnd = 3.dp
                    )

                    val favShape = RoundedCornerShape(
                        topStart = 3.dp, bottomStart = 3.dp,
                        topEnd = 50.dp, bottomEnd = 50.dp
                    )

                    val middleShape = RoundedCornerShape(3.dp)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(targetState = showInlineLyrics, label = "DownloadButton") { showLyrics ->
                            if (showLyrics) {
                                FilledIconButton(
                                    onClick = { isFullScreen = !isFullScreen },
                                    shape = shareShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = textButtonColor,
                                        contentColor = iconButtonColor,
                                    ),
                                    modifier = Modifier.size(42.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.fullscreen),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                FilledIconButton(
                                    onClick = {
                                        mediaMetadata?.let { meta ->
                                            when (download?.state) {
                                                Download.STATE_COMPLETED, Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                                                    DownloadService.sendRemoveDownload(
                                                        context,
                                                        ExoDownloadService::class.java,
                                                        meta.id,
                                                        false,
                                                    )
                                                }
                                                else -> {
                                                    database.transaction {
                                                        insert(meta)
                                                    }
                                                    val downloadRequest =
                                                        DownloadRequest
                                                            .Builder(meta.id, meta.id.toUri())
                                                            .setCustomCacheKey(meta.id)
                                                            .setData(meta.title.toByteArray())
                                                            .build()
                                                    DownloadService.sendAddDownload(
                                                        context,
                                                        ExoDownloadService::class.java,
                                                        downloadRequest,
                                                        false,
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    shape = shareShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = textButtonColor,
                                        contentColor = iconButtonColor,
                                    ),
                                    modifier = Modifier.size(42.dp),
                                ) {
                                    when (download?.state) {
                                        Download.STATE_COMPLETED -> {
                                            Icon(
                                                painter = painterResource(R.drawable.offline),
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                                            CircularWavyProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                        else -> {
                                            Icon(
                                                painter = painterResource(R.drawable.download),
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AnimatedContent(targetState = showInlineLyrics, label = "LikeButton") { showLyrics ->
                            if (showLyrics) {
                                val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
                                FilledIconButton(
                                    onClick = {
                                        menuState.show {
                                            LyricsMenu(
                                                lyricsProvider = { currentLyrics },
                                                songProvider = { currentSong?.song },
                                                mediaMetadataProvider = { mediaMetadata },
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
                                    },
                                    shape = favShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = textButtonColor,
                                        contentColor = iconButtonColor,
                                    ),
                                    modifier = Modifier.size(42.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_horiz),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                FilledIconButton(
                                    onClick = playerConnection::toggleLike,
                                    shape = favShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = textButtonColor,
                                        contentColor = iconButtonColor,
                                    ),
                                    modifier = Modifier.size(42.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (currentSong?.song?.liked == true)
                                                R.drawable.favorite
                                            else R.drawable.favorite_border
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val isLiked = currentSong?.song?.liked == true

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(TextBackgroundColor.copy(alpha = 0.14f))
                                .clickable(
                                    enabled = !showInlineLyrics && !showInlineQueue,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = playerConnection::toggleLike,
                                ),
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isLiked)
                                        R.drawable.star_apple
                                    else
                                        R.drawable.star_outline_apple
                                ),
                                contentDescription = if (isLiked) "Unlike" else "Like",
                                tint = TextBackgroundColor,
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(TextBackgroundColor.copy(alpha = 0.14f))
                                .clickable(
                                    enabled = !showInlineLyrics && !showInlineQueue,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        menuState.show {
                                            OldPlayerMenu(
                                                mediaMetadata = mediaMetadata,
                                                navController = navController,
                                                playerBottomSheetState = state,
                                                onShowDetailsDialog = {
                                                    mediaMetadata.id.let {
                                                        bottomSheetPageState.show {
                                                           ShowMediaInfo(it)
                                                        }
                                                    }
                                                },
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    },
                                ),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ellipsis_horizontal),
                                contentDescription = "Options",
                                tint = TextBackgroundColor,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        val sliderContent: @Composable () -> Unit = {
            val effectiveSliderStyle = if (!useNewPlayerDesign) SliderStyle.THIN else sliderStyle
            when (effectiveSliderStyle) {
                SliderStyle.DEFAULT -> {
                    Slider(
                        value = (sliderPosition ?: effectivePosition).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = {
                            if (!isListenTogetherGuest) {
                                sliderPosition = it.toLong()
                            }
                        },
                        onValueChangeFinished = {
                            if (!isListenTogetherGuest) {
                                sliderPosition?.let {
                                    // Ignore seeks while duration is unknown (track still
                                    // loading): the seek would be lost and the optimistic
                                    // position update would show a wrong slider position.
                                    if (duration != C.TIME_UNSET && duration > 0) {
                                        if (isCasting) {
                                            castHandler?.seekTo(it)
                                            lastManualSeekTime = System.currentTimeMillis()
                                        } else {
                                            playerConnection.player.seekTo(it)
                                        }
                                        position = it
                                    }
                                }
                                sliderPosition = null
                            }
                        },
                        enabled = !isListenTogetherGuest,
                        colors = PlayerSliderColors.getSliderColors(
                            activeColor = textButtonColor,
                            playerBackground = playerBackground,
                            useDarkTheme = useDarkTheme
                        ),
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                    )
                }

                SliderStyle.WAVY -> {
                    if (squigglySlider) {
                        SquigglySlider(
                            value = (sliderPosition ?: effectivePosition).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = {
                                sliderPosition = it.toLong()
                            },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    if (duration != C.TIME_UNSET && duration > 0) {
                                        if (isCasting) {
                                            castHandler?.seekTo(it)
                                            lastManualSeekTime = System.currentTimeMillis()
                                        } else {
                                            playerConnection.player.seekTo(it)
                                        }
                                        position = it
                                    }
                                }
                                sliderPosition = null
                            },
                            modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                            colors = PlayerSliderColors.getSliderColors(
                                activeColor = textButtonColor,
                                playerBackground = playerBackground,
                                useDarkTheme = useDarkTheme
                            ),
                            isPlaying = effectiveIsPlaying,
                        )
                    } else {
                        WavySlider(
                            value = (sliderPosition ?: effectivePosition).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = {
                                sliderPosition = it.toLong()
                            },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    if (duration != C.TIME_UNSET && duration > 0) {
                                        if (isCasting) {
                                            castHandler?.seekTo(it)
                                            lastManualSeekTime = System.currentTimeMillis()
                                        } else {
                                            playerConnection.player.seekTo(it)
                                        }
                                        position = it
                                    }
                                }
                                sliderPosition = null
                            },
                            colors = PlayerSliderColors.getSliderColors(
                                activeColor = textButtonColor,
                                playerBackground = playerBackground,
                                useDarkTheme = useDarkTheme
                            ),
                            modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                            isPlaying = effectiveIsPlaying
                        )
                    }
                }

                SliderStyle.SLIM -> {
                    val trackInteractionSource = remember { MutableInteractionSource() }
                    val isTrackDragged by trackInteractionSource.collectIsDraggedAsState()
                    val isTrackPressed by trackInteractionSource.collectIsPressedAsState()
                    val isTrackActive = (isTrackDragged || isTrackPressed) && !useNewPlayerDesign

                    val trackHeight by animateDpAsState(
                        targetValue = if (isTrackActive) 16.dp else 10.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "trackHeight"
                    )

                    Slider(
                        value = (sliderPosition ?: effectivePosition).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = {
                            if (!isListenTogetherGuest) {
                                sliderPosition = it.toLong()
                            }
                        },
                        onValueChangeFinished = {
                            if (!isListenTogetherGuest) {
                                sliderPosition?.let {
                                    if (duration != C.TIME_UNSET && duration > 0) {
                                        if (isCasting) {
                                            castHandler?.seekTo(it)
                                            lastManualSeekTime = System.currentTimeMillis()
                                        } else {
                                            playerConnection.player.seekTo(it)
                                        }
                                        position = it
                                    }
                                }
                                sliderPosition = null
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
                                    activeColor = textButtonColor,
                                    playerBackground = playerBackground,
                                    useDarkTheme = useDarkTheme
                                )
                            )
                        },
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
                    )
                }

                SliderStyle.EXPRESSIVE -> {
                    ExpressiveWavySlider(
                        value = { (sliderPosition ?: effectivePosition).toFloat() },
                        onValueChange = {
                            if (!isListenTogetherGuest) {
                                sliderPosition = it.toLong()
                            }
                        },
                        onValueChangeFinished = {
                            if (!isListenTogetherGuest) {
                                sliderPosition?.let {
                                    if (duration != C.TIME_UNSET && duration > 0) {
                                        if (isCasting) {
                                            castHandler?.seekTo(it)
                                            lastManualSeekTime = System.currentTimeMillis()
                                        } else {
                                            playerConnection.player.seekTo(it)
                                        }
                                        position = it
                                    }
                                }
                                sliderPosition = null
                            }
                        },
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        enabled = !isListenTogetherGuest,
                        isPlaying = effectiveIsPlaying,
                        activeTrackColor = textButtonColor,
                        thumbColor = textButtonColor,
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
                    )
                }

                SliderStyle.THIN -> {
                    ThinSlider(
                        value = (sliderPosition ?: effectivePosition).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = {
                            if (!isListenTogetherGuest) {
                                sliderPosition = it.toLong()
                            }
                        },
                        onValueChangeFinished = {
                            if (!isListenTogetherGuest) {
                                sliderPosition?.let {
                                    if (duration != C.TIME_UNSET && duration > 0) {
                                        if (isCasting) {
                                            castHandler?.seekTo(it)
                                            lastManualSeekTime = System.currentTimeMillis()
                                        } else {
                                            playerConnection.player.seekTo(it)
                                        }
                                        position = it
                                    }
                                }
                                sliderPosition = null
                            }
                        },
                        enabled = !isListenTogetherGuest,
                        colors = PlayerSliderColors.getSliderColors(
                            activeColor = textButtonColor,
                            playerBackground = playerBackground,
                            useDarkTheme = useDarkTheme
                        ),
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 4.dp),
            ) {
                Text(
                    text = makeTimeString(sliderPosition ?: effectivePosition),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        val newPlaybackControlsContent: @Composable () -> Unit = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
            ) {
                val backInteractionSource = remember { MutableInteractionSource() }
                val nextInteractionSource = remember { MutableInteractionSource() }
                val playPauseInteractionSource = remember { MutableInteractionSource() }

                val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()

                val playPauseWeight by animateFloatAsState(
                    targetValue = if (isPlayPausePressed) 2.0f else 1.7f,
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = 400f
                    ),
                    label = "playPauseWeight"
                )

                val backButtonWeight by animateFloatAsState(
                    targetValue = if (isPlayPausePressed) 0.45f else 0.6f,
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = 400f
                    ),
                    label = "backButtonWeight"
                )

                val nextButtonWeight by animateFloatAsState(
                    targetValue = if (isPlayPausePressed) 0.45f else 0.6f,
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = 400f
                    ),
                    label = "nextButtonWeight"
                )

                val controlHeight = 68.dp
                val sideShape = remember { RoundedCornerShape(controlHeight / 2) }
                val playCornerRadius by animateDpAsState(
                    targetValue = if (effectiveIsPlaying) 20.dp else controlHeight / 2,
                    animationSpec = spring(
                        dampingRatio = 0.55f,
                        stiffness = 380f
                    ),
                    label = "playCornerRadius"
                )
                val playShape = RoundedCornerShape(playCornerRadius)

                val sideColors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = sideButtonContainerColor,
                    contentColor = sideButtonContentColor,
                )
                val centerColors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = textButtonColor,
                    contentColor = iconButtonColor,
                )

                FilledTonalButton(
                    onClick = playerConnection::seekToPrevious,
                    enabled = canSkipPrevious && !isListenTogetherGuest,
                    shape = sideShape,
                    interactionSource = backInteractionSource,
                    colors = sideColors,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .height(controlHeight)
                        .weight(backButtonWeight)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = {
                        if (isListenTogetherGuest) {
                            playerConnection.toggleMute()
                            return@FilledTonalButton
                        }
                        if (isCasting) {
                            if (castIsPlaying) {
                                castHandler?.pause()
                            } else {
                                castHandler?.play()
                            }
                        } else if (playbackState == STATE_ENDED) {
                            playerConnection.player.seekTo(0, 0)
                            playerConnection.player.playWhenReady = true
                        } else {
                            playerConnection.togglePlayPause()
                        }
                    },
                    shape = playShape,
                    interactionSource = playPauseInteractionSource,
                    colors = centerColors,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .height(controlHeight)
                        .weight(playPauseWeight)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val playIconRes = if (isListenTogetherGuest) {
                            if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                        } else {
                            if (effectiveIsPlaying) R.drawable.pause else R.drawable.play
                        }
                        AnimatedContent(
                            targetState = playIconRes,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.72f, animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f)))
                                    .togetherWith(fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.72f, animationSpec = tween(120)))
                            },
                            contentAlignment = Alignment.Center,
                            label = "playIconAnimation"
                        ) { iconRes ->
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = if (isListenTogetherGuest) {
                                    if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute)
                                } else {
                                    if (effectiveIsPlaying) stringResource(R.string.pause) else stringResource(R.string.play)
                                },
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val playPauseText = if (isListenTogetherGuest) {
                            if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute)
                        } else {
                            if (effectiveIsPlaying) stringResource(R.string.pause) else stringResource(R.string.play)
                        }
                        AnimatedContent(
                            targetState = playPauseText,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)))
                                    .togetherWith(fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.85f, animationSpec = tween(120)))
                            },
                            contentAlignment = Alignment.Center,
                            label = "playPauseLabel"
                        ) { labelText ->
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = playerConnection::seekToNext,
                    enabled = canSkipNext && !isListenTogetherGuest,
                    shape = sideShape,
                    interactionSource = nextInteractionSource,
                    colors = sideColors,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .height(controlHeight)
                        .weight(nextButtonWeight)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        val oldPlaybackControlsContent: @Composable () -> Unit = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding),
            ) {
//                            Box(modifier = Modifier.weight(1f)) {
//                                ResizableIconButton(
//                                    icon = when (repeatMode) {
//                                        Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
//                                        Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
//                                        else -> throw IllegalStateException()
//                                    },
//                                    color = TextBackgroundColor,
//                                    modifier = Modifier
//                                        .size(32.dp)
//                                        .padding(4.dp)
//                                        .align(Alignment.Center)
//                                        .alpha(if (isListenTogetherGuest) 0.5f else 1f),
//                                    enabled = !isListenTogetherGuest,
//                                    onClick = {
//                                        playerConnection.player.toggleRepeatMode()
//                                    }
//                                )
//                            }

                            Box(modifier = Modifier.weight(1f)) {
                                ResizableIconButton(
                                    icon = R.drawable.ic_player_previous,
                                    enabled = canSkipPrevious && !isListenTogetherGuest,
                                    color = TextBackgroundColor,
                                    modifier =
                                Modifier
                                    .size(width = 42.dp, height = 30.dp)
                                    .align(Alignment.Center)
                                    .alpha(if (isListenTogetherGuest) 0.5f else 1f),
                                    onClick = playerConnection::seekToPrevious,
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            if (isListenTogetherGuest) {
                                                playerConnection.toggleMute()
                                                return@clickable
                                            }
                                            if (isCasting) {
                                                if (castIsPlaying) {
                                                    castHandler?.pause()
                                                } else {
                                                    castHandler?.play()
                                                }
                                            } else if (playbackState == STATE_ENDED) {
                                                playerConnection.player.seekTo(0, 0)
                                                playerConnection.player.playWhenReady = true
                                            } else {
                                                playerConnection.player.togglePlayPause()
                                            }
                                        }
                                    ),
                            ) {
                                Image(
                                    painter =
                                    painterResource(
                                        if (isListenTogetherGuest) {
                                            if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                        } else if (playbackState ==
                                            STATE_ENDED
                                        ) {
                                            R.drawable.replay
                                        } else if (effectiveIsPlaying) {
                                            R.drawable.ic_player_pause
                                        } else {
                                            R.drawable.ic_player_play
                                        },
                                    ),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(TextBackgroundColor),
                                    modifier =
                                    Modifier
                                        .align(Alignment.Center)
                                        .size(44.dp),
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                ResizableIconButton(
                                    icon = R.drawable.ic_player_next,
                                    enabled = canSkipNext && !isListenTogetherGuest,
                                    color = TextBackgroundColor,
                                    modifier =
                                Modifier
                                    .size(width = 42.dp, height = 30.dp)
                                    .align(Alignment.Center)
                                    .alpha(if (isListenTogetherGuest) 0.5f else 1f),
                                    onClick = playerConnection::seekToNext,
                                )
                            }

//                            Box(modifier = Modifier.weight(1f)) {
//                                ResizableIconButton(
//                                    icon = if (currentSong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border,
//                                    color = if (currentSong?.song?.liked == true) MaterialTheme.colorScheme.error else TextBackgroundColor,
//                                    modifier =
//                                    Modifier
//                                        .size(32.dp)
//                                        .padding(4.dp)
//                                        .align(Alignment.Center),
//                                    onClick = playerConnection::toggleLike,
//                                )
//                            }
                        }
        }

        val volumeBarContent: @Composable () -> Unit = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
            ) {
                            var isDragging by remember { mutableStateOf(false) }
                            var lastAppliedStep by remember { mutableStateOf((systemVolume * maxSystemVolume).roundToInt()) }
                            val animatedVolume = remember { Animatable(systemVolume) }
                            val coroutineScope = rememberCoroutineScope()

                            LaunchedEffect(systemVolume) {
                                if (!isDragging) {
                                    lastAppliedStep = (systemVolume * maxSystemVolume).roundToInt()
                                    animatedVolume.animateTo(
                                        targetValue = systemVolume,
                                        animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing)
                                    )
                                }
                            }

                            fun applyVolumeFraction(fraction: Float) {
                                val clamped = fraction.coerceIn(0f, 1f)
                                coroutineScope.launch { animatedVolume.snapTo(clamped) }
                                if (isCasting) {
                                    castHandler?.setVolume(clamped)
                                } else {
                                    val targetStep = (clamped * maxSystemVolume).roundToInt().coerceIn(0, maxSystemVolume.roundToInt())
                                    if (targetStep != lastAppliedStep) {
                                        lastAppliedStep = targetStep
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetStep, 0)
                                    }
                                }
                            }

                            val currentBarHeight by animateDpAsState(
                                targetValue = if (isDragging) 10.dp else 6.dp,
                                animationSpec = tween(durationMillis = 150),
                                label = "volumeBarHeight"
                            )

                            val iconColor = TextBackgroundColor.copy(alpha = 0.65f)

                            Image(
                                painter = painterResource(R.drawable.apple_volume_down),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(iconColor),
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Canvas(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .pointerInput(maxSystemVolume) {
                                        detectTapGestures { offset ->
                                            if (size.width <= 0) return@detectTapGestures
                                            applyVolumeFraction(offset.x / size.width.toFloat())
                                        }
                                    }
                                    .pointerInput(maxSystemVolume) {
                                        var dragFraction = animatedVolume.value
                                        detectHorizontalDragGestures(
                                            onDragStart = { offset ->
                                                if (size.width <= 0) return@detectHorizontalDragGestures
                                                isDragging = true
                                                dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                                applyVolumeFraction(dragFraction)
                                            },
                                            onHorizontalDrag = { _, delta ->
                                                if (size.width <= 0) return@detectHorizontalDragGestures
                                                dragFraction = (dragFraction + delta / size.width.toFloat()).coerceIn(0f, 1f)
                                                applyVolumeFraction(dragFraction)
                                            },
                                            onDragEnd = {
                                                isDragging = false
                                            },
                                            onDragCancel = {
                                                isDragging = false
                                            }
                                        )
                                    }
                            ) {
                                val trackHeightPx = currentBarHeight.toPx()
                                val topY = (size.height - trackHeightPx) / 2f
                                val cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
                                val fraction = (if (isCasting) castVolume else animatedVolume.value).coerceIn(0f, 1f)

                                drawRoundRect(
                                    color = TextBackgroundColor.copy(alpha = 0.22f),
                                    topLeft = Offset(0f, topY),
                                    size = Size(size.width, trackHeightPx),
                                    cornerRadius = cornerRadius
                                )

                                if (fraction > 0f) {
                                    drawRoundRect(
                                        color = TextBackgroundColor,
                                        topLeft = Offset(0f, topY),
                                        size = Size((size.width * fraction).coerceIn(0f, size.width), trackHeightPx),
                                        cornerRadius = cornerRadius
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Image(
                                painter = painterResource(R.drawable.apple_volume_up),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(iconColor),
                                modifier = Modifier.size(20.dp)
                            )
                        }
        }

        val oldBottomActionsContent: @Composable () -> Unit = {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
                    .padding(bottom = 12.dp)
            ) {
                                    // Left: Lyrics
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {
                                                    val next = !showInlineLyrics
                                                    showInlineLyrics = next
                                                    if (next) showInlineQueue = false
                                                }
                                            )
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.lyrics_apple),
                                            contentDescription = stringResource(R.string.lyrics),
                                            tint = if (showInlineLyrics) textButtonColor else textButtonColor.copy(alpha = 0.5f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    // Center: Audio Device / Routing
                                    val isConnected = bluetoothDeviceName != null
                                    val tintColor = if (isConnected) textButtonColor else textButtonColor.copy(alpha = 0.5f)
                                    val label = if (isConnected) {
                                        nameToShow ?: "Bluetooth"
                                    } else {
                                        stringResource(R.string.player_audio_this_phone)
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .widthIn(max = 160.dp)
                                            .padding(horizontal = 4.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = { showAudioDeviceBottomSheet = true }
                                            )
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(
                                                    if (isConnected) {
                                                        when {
                                                            isSpeaker(nameToShow) -> R.drawable.speaker_applemusic
                                                            isBuds(nameToShow) -> R.drawable.headphones_apple
                                                            else -> R.drawable.headphones_apple
                                                        }
                                                    } else {
                                                        R.drawable.airplay_audio
                                                    }
                                                ),
                                                contentDescription = label,
                                                tint = tintColor,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                            color = tintColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Right: Queue
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {
                                                    if (useNewPlayerDesign) {
                                                        queueSheetState.expandSoft()
                                                    } else {
                                                        val next = !showInlineQueue
                                                        showInlineQueue = next
                                                        if (next) showInlineLyrics = false
                                                    }
                                                }
                                            )
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.apple_queue),
                                            contentDescription = stringResource(R.string.queue),
                                            tint = if (if (useNewPlayerDesign) queueSheetState.isExpanded else showInlineQueue) textButtonColor else textButtonColor.copy(alpha = 0.5f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
        }

        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isOldPlayerPortrait = !useNewPlayerDesign && !isLandscape

        if (isOldPlayerPortrait) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 8.dp)
            ) {
                // Group 1: Track Info + Slider + Time
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val isCompact = !useNewPlayerDesign && (showInlineLyrics || showInlineQueue)
                    if (!isCompact) {
                        trackInfoContent()
                        Spacer(Modifier.height(trackInfoSpacerHeight))
                    }
                    sliderContent()
                }

                // Group 2: Playback Controls (Middle)
                AnimatedVisibility(
                    visible = !isFullScreen,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    oldPlaybackControlsContent()
                }

                // Group 3: Volume + Bottom Actions (Bottom)
                AnimatedVisibility(
                    visible = !isFullScreen,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        volumeBarContent()
                        Spacer(Modifier.height(42.dp))
                        oldBottomActionsContent()
                    }
                }
            }
        } else {
            trackInfoContent()

            Spacer(Modifier.height(if (useNewPlayerDesign) 24.dp else 20.dp))

            sliderContent()

            Spacer(Modifier.height(if (useNewPlayerDesign) 24.dp else 32.dp))

            AnimatedVisibility(
                visible = !isFullScreen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Column {
                    if (useNewPlayerDesign) {
                        newPlaybackControlsContent()
                    } else {
                        oldPlaybackControlsContent()
                        Spacer(modifier = Modifier.height(28.dp))
                        volumeBarContent()
                        if (!isLandscape) {
                            Spacer(Modifier.height(42.dp))
                            oldBottomActionsContent()
                        }
                    }
                }
            }
        }
    }

        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                // Calculate vertical padding like OuterTune
                val density = LocalDensity.current
                val verticalPadding = max(
                    WindowInsets.systemBars.getTop(density),
                    WindowInsets.systemBars.getBottom(density)
                )
                val verticalPaddingDp = with(density) { verticalPadding.toDp() }
                val verticalWindowInsets = WindowInsets(left = 0.dp, top = verticalPaddingDp, right = 0.dp, bottom = if (useNewPlayerDesign) 0.dp else verticalPaddingDp)

                val currentSliderPosition by rememberUpdatedState(sliderPosition)
                val sliderPositionProvider = remember { { currentSliderPosition } }
                val isExpandedProvider = remember(state) { { state.isExpanded } }

                val landscapeBottomPadding by animateDpAsState(
                    targetValue = if (isFullScreen) 0.dp else if (useNewPlayerDesign) queueSheetState.collapsedBound else 16.dp,
                    label = "landscapeBottomPadding"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .windowInsetsPadding(
                            WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).add(verticalWindowInsets)
                        )
                        .padding(bottom = landscapeBottomPadding, top = 8.dp)
                        .fillMaxSize()
                ) {
                    // Left Column: Artwork Box + Action Buttons below it
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .padding(bottom = 8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .nestedScroll(state.preUpPostDownNestedScrollConnection)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(thumbnailCornerRadius.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable(enabled = showInlineLyrics || showInlineQueue) {
                                        showInlineLyrics = false
                                        showInlineQueue = false
                                    }
                            ) {
                                Crossfade(
                                    targetState = mediaMetadata?.id to (mediaMetadata?.thumbnailUrl ?: currentSong?.song?.thumbnailUrl),
                                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                                    label = "landscapeArtworkCrossfade"
                                ) { (_, thumbnailUrl) ->
                                    var currentModel by remember(thumbnailUrl) {
                                        mutableStateOf<Any?>(thumbnailUrl)
                                    }
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(currentModel)
                                            .size(CoilSize.ORIGINAL)
                                            .crossfade(true)
                                            .build(),
                                        onError = {
                                            val url = currentModel?.toString()
                                            currentModel = when {
                                                url == null -> null
                                                url.contains("maxresdefault.jpg") -> url.replace("maxresdefault.jpg", "sddefault.jpg")
                                                url.contains("sddefault.jpg") -> url.replace("sddefault.jpg", "hqdefault.jpg")
                                                url.contains("hqdefault.jpg") -> url.replace("hqdefault.jpg", "mqdefault.jpg")
                                                thumbnailUrl != null && url != thumbnailUrl -> thumbnailUrl
                                                else -> null
                                            }
                                        },
                                        contentDescription = null,
                                        contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                val motionVideoUrl = canvasArtwork?.animated ?: canvasArtwork?.videoUrl
                                if (enableCanvas && motionVideoUrl != null) {
                                    MotionartworkPlayer(
                                        primaryUrl = motionVideoUrl,
                                        fallbackUrl = canvasArtwork?.videoUrl,
                                        isPlaying = isPlaying,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        if (!useNewPlayerDesign) {
                            Spacer(Modifier.height(12.dp))

                            // Action buttons below the image showing box
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                            ) {
                                // Left: Lyrics
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                val next = !showInlineLyrics
                                                showInlineLyrics = next
                                                if (next) showInlineQueue = false
                                            }
                                        )
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.lyrics_apple),
                                        contentDescription = stringResource(R.string.lyrics),
                                        tint = if (showInlineLyrics) textButtonColor else textButtonColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                // Center: Audio Device / Routing
                                val isConnected = bluetoothDeviceName != null
                                val tintColor = if (isConnected) textButtonColor else textButtonColor.copy(alpha = 0.5f)
                                val label = if (isConnected) {
                                    nameToShow ?: "Bluetooth"
                                } else {
                                    stringResource(R.string.player_audio_this_phone)
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { showAudioDeviceBottomSheet = true }
                                        )
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (isConnected) {
                                                when {
                                                    isSpeaker(nameToShow) -> R.drawable.speaker_applemusic
                                                    isBuds(nameToShow) -> R.drawable.headphones_apple
                                                    else -> R.drawable.headphones_apple
                                                }
                                            } else {
                                                R.drawable.airplay_audio
                                            }
                                        ),
                                        contentDescription = label,
                                        tint = tintColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                // Right: Queue
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                val next = !showInlineQueue
                                                showInlineQueue = next
                                                if (next) showInlineLyrics = false
                                            }
                                        )
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.apple_queue),
                                        contentDescription = stringResource(R.string.queue),
                                        tint = if (showInlineQueue) textButtonColor else textButtonColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Right Column: Dynamic Pane (Controls / Lyrics / Queue)
                    Crossfade(
                        targetState = showInlineLyrics to showInlineQueue,
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                        label = "landscapeRightPane",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    ) { (showLyrics, showQueue) ->
                        when {
                            showLyrics -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .nestedScroll(state.preUpPostDownNestedScrollConnection)
                                ) {
                                    InlineLyricsView(
                                        mediaMetadata = mediaMetadata,
                                        showLyrics = true,
                                        positionProvider = { sliderPosition ?: effectivePosition }
                                    )

                                    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
                                    val metaForLyrics = mediaMetadata
                                    if (metaForLyrics != null) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(bottom = 12.dp, end = 12.dp)
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(TextBackgroundColor.copy(alpha = 0.14f))
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null,
                                                    onClick = {
                                                        menuState.show {
                                                            LyricsMenu(
                                                                lyricsProvider = { currentLyrics },
                                                                songProvider = { currentSong?.song },
                                                                mediaMetadataProvider = { metaForLyrics },
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
                                                )
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = stringResource(R.string.lyrics),
                                                tint = TextBackgroundColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            showQueue -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .nestedScroll(state.preUpPostDownNestedScrollConnection)
                                ) {
                                    InlineQueueView(
                                        playerConnection = playerConnection,
                                        navController = navController,
                                        playerBottomSheetState = state,
                                        primaryColor = TextBackgroundColor,
                                        onDraggingChange = {},
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            else -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxSize()
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
                    }
                }
            }

            else -> {
                val bottomPadding by animateDpAsState(
                    targetValue = if (isFullScreen) 0.dp else if (useNewPlayerDesign) queueSheetState.collapsedBound else WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
                    label = "bottomPadding"
                )

                if (useNewPlayerDesign) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                        Modifier
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .padding(bottom = bottomPadding)
                            .animateContentSize(),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .then(
                                    if (isCanvasActive && !showInlineLyrics) {
                                        Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            showCanvasControls = !showCanvasControls
                                        }
                                    } else Modifier
                                ),
                        ) {
                            val currentSliderPosition by rememberUpdatedState(sliderPosition)
                            val sliderPositionProvider = remember { { currentSliderPosition } }
                            val isExpandedProvider = remember(state) { { state.isExpanded } }
                            AnimatedContent(
                                targetState = showInlineLyrics,
                                label = "Lyrics",
                                transitionSpec = { fadeIn() togetherWith fadeOut() }
                            ) { showLyrics ->
                                if (showLyrics) {
                                    InlineLyricsView(
                                        mediaMetadata = mediaMetadata,
                                        showLyrics = showLyrics,
                                        positionProvider = { sliderPosition ?: effectivePosition }
                                    )
                                } else if (spotifyCanvasUrl == null && playerBackground != PlayerBackgroundStyle.APPLE_MUSIC) {
                                    Thumbnail(
                                        sliderPositionProvider = sliderPositionProvider,
                                        modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection),
                                        isPlayerExpanded = isExpandedProvider,
                                        isListenTogetherGuest = isListenTogetherGuest
                                    )
                                }
                            }
                        }

                        mediaMetadata?.let {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer { alpha = canvasControlsAlpha }
                            ) {
                                controlsContent(it)
                            }
                        }

                        Spacer(Modifier.height(30.dp))
                    }
                } else {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .padding(bottom = bottomPadding)
                    ) {
                        val isCompactHeader = showInlineLyrics || showInlineQueue
                        val lyricsTransition = updateTransition(
                            targetState = isCompactHeader,
                            label = "playerCompactTransition"
                        )
                        val lyricsProgress by lyricsTransition.animateFloat(
                            transitionSpec = { tween(durationMillis = 380, easing = FastOutSlowInEasing) },
                            label = "lyricsProgress"
                        ) { showing -> if (showing) 1f else 0f }

                        val normalTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 24.dp
                        val topSectionHeight = (maxHeight - normalTopPadding) * (1.25f / 2.25f)

                        val expandedArtworkSize = minOf(maxWidth - (PlayerHorizontalPadding * 2), topSectionHeight - 16.dp).coerceAtLeast(120.dp)
                        val expandedX = (maxWidth - expandedArtworkSize) / 2f
                        val expandedY = normalTopPadding + (topSectionHeight - expandedArtworkSize) / 2f
                        val controlsTop = (expandedY + expandedArtworkSize + 34.dp).coerceAtMost(normalTopPadding + topSectionHeight)
                        val bottomControlsHeight = (maxHeight - controlsTop).coerceAtLeast(120.dp)

                        val collapsedArtworkSize = 56.dp
                        val collapsedX = PlayerHorizontalPadding
                        val collapsedY = normalTopPadding

                        val isAppleMusic = playerBackground == PlayerBackgroundStyle.APPLE_MUSIC

                        val bannerHeight = (maxHeight * 0.65f).coerceAtMost(controlsTop + 16.dp)
                        val targetExpandedWidth = if (isAppleMusic) maxWidth else expandedArtworkSize
                        val targetExpandedHeight = if (isAppleMusic) bannerHeight else expandedArtworkSize
                        val targetExpandedX = if (isAppleMusic) 0.dp else expandedX
                        val targetExpandedY = if (isAppleMusic) 0.dp else expandedY

                        val artworkWidth = dpLerp(targetExpandedWidth, collapsedArtworkSize, lyricsProgress)
                        val artworkHeight = dpLerp(targetExpandedHeight, collapsedArtworkSize, lyricsProgress)
                        val artworkX = dpLerp(targetExpandedX, collapsedX, lyricsProgress)
                        val artworkY = dpLerp(targetExpandedY, collapsedY, lyricsProgress)

                        var showCompactControls by remember(showInlineLyrics, showInlineQueue) {
                            mutableStateOf(true)
                        }

                        var lastCompactInteraction by remember(showInlineLyrics, showInlineQueue) {
                            mutableIntStateOf(0)
                        }

                        var isDraggingQueue by remember(showInlineQueue) {
                            mutableStateOf(false)
                        }

                        LaunchedEffect(showInlineLyrics, showInlineQueue, effectiveIsPlaying) {
                            if ((showInlineLyrics || showInlineQueue) && !effectiveIsPlaying) {
                                showCompactControls = true
                            }
                        }

                        LaunchedEffect(
                            showInlineLyrics,
                            showInlineQueue,
                            effectiveIsPlaying,
                            showCompactControls,
                            lastCompactInteraction
                        ) {
                            if ((showInlineLyrics || showInlineQueue) && effectiveIsPlaying && showCompactControls) {
                                delay(3500)
                                showCompactControls = false
                            }
                        }

                        val lyricsTop = collapsedY + collapsedArtworkSize + 16.dp
                        val lyricsHeight = (maxHeight - lyricsTop - 16.dp).coerceAtLeast(120.dp)
                        val compactControlsOffset = if (isCompactHeader) trackInfoHeightDp + trackInfoSpacerHeight else 0.dp
                        val effectiveControlsTop = controlsTop + compactControlsOffset
                        val effectiveControlsHeight = (maxHeight - effectiveControlsTop).coerceAtLeast(120.dp)
                        val controlsMaskThresholdY = effectiveControlsTop

                        val lyricsClipEnd =
                            ((controlsMaskThresholdY - lyricsTop) / lyricsHeight).coerceIn(0.35f, 0.85f)
                        val lyricsRevealFraction by animateFloatAsState(
                            targetValue = if (showCompactControls) lyricsClipEnd else 1.15f,
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                            label = "lyricsRevealFraction"
                        )

                        val queueTop = collapsedY + collapsedArtworkSize + 12.dp
                        val queueHeight = (maxHeight - queueTop - 8.dp).coerceAtLeast(120.dp)
                        val queueClipEnd =
                            ((controlsMaskThresholdY - queueTop) / queueHeight).coerceIn(0.35f, 0.85f)
                        val queueRevealFraction by animateFloatAsState(
                            targetValue = if (showCompactControls) queueClipEnd else 1.15f,
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                            label = "queueRevealFraction"
                        )

                        // Apple Music Top Drag Handle / Grabber Pill
                        AnimatedVisibility(
                            visible = !isFullScreen,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(200)),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 4.dp)
                                .zIndex(2f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(width = 64.dp, height = 20.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { state.collapseSoft() }
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 36.dp, height = 5.dp)
                                        .clip(CircleShape)
                                        .background(TextBackgroundColor.copy(alpha = 0.45f))
                                )
                            }
                        }

                        // 1a. Synchronized Lyrics View
                        AnimatedVisibility(
                            visible = showInlineLyrics,
                            enter = fadeIn(tween(durationMillis = 280, delayMillis = 100)),
                            exit = fadeOut(tween(durationMillis = 150)),
                            modifier = Modifier
                                .offset(y = lyricsTop)
                                .fillMaxWidth()
                                .height(lyricsHeight)
                                .bottomControlsMask { lyricsRevealFraction }
                                .compactPanelGestures(
                                    key1 = showInlineLyrics,
                                    key2 = effectiveIsPlaying,
                                    onScrollDown = {
                                        if (effectiveIsPlaying && showCompactControls) {
                                            showCompactControls = false
                                        }
                                    },
                                    onScrollUp = {
                                        if (effectiveIsPlaying && showCompactControls) {
                                            showCompactControls = false
                                        }
                                    },
                                    onTapInteraction = {
                                        if (!showCompactControls) {
                                            showCompactControls = true
                                        }
                                        lastCompactInteraction++
                                    }
                                )
                        ) {
                            val controlsOverlap = if (showCompactControls) {
                                (lyricsTop + lyricsHeight - effectiveControlsTop).coerceAtLeast(0.dp)
                            } else {
                                0.dp
                            }
                            InlineLyricsView(
                                mediaMetadata = mediaMetadata,
                                showLyrics = showInlineLyrics,
                                positionProvider = { sliderPosition ?: effectivePosition },
                                controlsOverlap = controlsOverlap
                            )
                        }

                        // 1b. In-Screen Queue View
                        AnimatedVisibility(
                            visible = showInlineQueue,
                            enter = fadeIn(tween(durationMillis = 280, delayMillis = 100)),
                            exit = fadeOut(tween(durationMillis = 150)),
                            modifier = Modifier
                                .offset(y = queueTop)
                                .fillMaxWidth()
                                .height(queueHeight)
                                .bottomControlsMask { queueRevealFraction }
                                .compactPanelGestures(
                                    key1 = showInlineQueue,
                                    key2 = effectiveIsPlaying,
                                    onScrollDown = {
                                        if (!isDraggingQueue && showCompactControls) {
                                            showCompactControls = false
                                        }
                                    },
                                    onScrollUp = {
                                        if (!isDraggingQueue && !showCompactControls) {
                                            showCompactControls = true
                                        }
                                        lastCompactInteraction++
                                    },
                                    onTapInteraction = {
                                        if (!showCompactControls) {
                                            showCompactControls = true
                                        }
                                        lastCompactInteraction++
                                    }
                                )
                        ) {
                            InlineQueueView(
                                playerConnection = playerConnection,
                                navController = navController,
                                playerBottomSheetState = state,
                                primaryColor = TextBackgroundColor,
                                onDraggingChange = { isDraggingQueue = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // 2. Playback Controls (auto-hides on delay during lyrics playback, reveals on tap)
                        AnimatedVisibility(
                            visible = !isCompactHeader || showCompactControls,
                            enter = slideInVertically(
                                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                                initialOffsetY = { it }
                            ) + fadeIn(tween(durationMillis = 220)),
                            exit = slideOutVertically(
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                                targetOffsetY = { it }
                            ) + fadeOut(tween(durationMillis = 220)),
                            modifier = Modifier
                                .offset(y = effectiveControlsTop)
                                .fillMaxWidth()
                                .height(effectiveControlsHeight)
                                .let {
                                    if (isCompactHeader) {
                                        it.pointerInput(Unit) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                                    if (event.changes.any { change -> change.changedToDown() || change.changedToUp() }) {
                                                        lastCompactInteraction++
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        it
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                mediaMetadata?.let {
                                    controlsContent(it)
                                }
                            }
                        }

                        // 3. Compact Header: Song Title, Artist, Favorite Star, 3-dot Lyrics Menu
                        AnimatedVisibility(
                            visible = isCompactHeader,
                            enter = fadeIn(tween(durationMillis = 280, delayMillis = 100)),
                            exit = fadeOut(tween(durationMillis = 150)),
                            modifier = Modifier
                                .offset(y = collapsedY)
                                .fillMaxWidth()
                                .height(collapsedArtworkSize)
                                .padding(
                                    start = collapsedX + collapsedArtworkSize + 12.dp,
                                    end = PlayerHorizontalPadding
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                AnimatedContent(
                                    targetState = Triple(
                                        mediaMetadata?.id,
                                        mediaMetadata?.title.orEmpty(),
                                        mediaMetadata?.artists?.joinToString(", ") { it.name }.orEmpty()
                                    ),
                                    transitionSpec = {
                                        fadeIn(tween(durationMillis = 300)) togetherWith fadeOut(tween(durationMillis = 220))
                                    },
                                    label = "playerCompactTrackInfoTransition",
                                    modifier = Modifier.weight(1f)
                                ) { (_, title, artists) ->
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = TextBackgroundColor,
                                            modifier = Modifier.basicMarquee()
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (mediaMetadata?.explicit == true) {
                                                MIcon.Explicit(tint = TextBackgroundColor.copy(alpha = 0.65f))
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }

                                            Text(
                                                text = artists,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextBackgroundColor.copy(alpha = 0.65f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.basicMarquee()
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isLiked = currentSong?.song?.liked == true
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(TextBackgroundColor.copy(alpha = 0.14f))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = playerConnection::toggleLike
                                            )
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (isLiked) R.drawable.star_apple else R.drawable.star_outline_apple
                                            ),
                                            contentDescription = if (isLiked) "Unlike" else "Like",
                                            tint = TextBackgroundColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
                                    val metaForLyrics = mediaMetadata
                                    if (metaForLyrics != null) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(TextBackgroundColor.copy(alpha = 0.14f))
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null,
                                                    onClick = {
                                                        if (showInlineQueue) {
                                                            menuState.show {
                                                                OldPlayerMenu(
                                                                    mediaMetadata = metaForLyrics,
                                                                    navController = navController,
                                                                    playerBottomSheetState = state,
                                                                    onShowDetailsDialog = {
                                                                        metaForLyrics.id.let {
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
                                                                    mediaMetadataProvider = { metaForLyrics },
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
                                                )
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ellipsis_horizontal),
                                                contentDescription = if (showInlineQueue) "Queue Options" else "Lyrics Options",
                                                tint = TextBackgroundColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Artwork: Unified single morphing component matching ViviLite architecture
                        val artworkShape = if (isAppleMusic) {
                            RoundedCornerShape(dpLerp(0.dp, 8.dp, lyricsProgress))
                        } else {
                            RoundedCornerShape(dpLerp(thumbnailCornerRadius.dp, 0.dp, lyricsProgress))
                        }
                        val maskAlpha = if (isAppleMusic) (1f - lyricsProgress).coerceIn(0f, 1f) else 0f
                        val showDissolveMask = isAppleMusic && maskAlpha > 0.01f

                        Box(
                            modifier = Modifier
                                .offset(x = artworkX, y = artworkY)
                                .width(artworkWidth)
                                .height(artworkHeight)
                                .zIndex(1f)
                                .nestedScroll(state.preUpPostDownNestedScrollConnection)
                                .graphicsLayer {
                                    shape = artworkShape
                                    clip = true
                                    compositingStrategy = if (showDissolveMask) {
                                        CompositingStrategy.Offscreen
                                    } else {
                                        CompositingStrategy.Auto
                                    }
                                }
                                .drawWithContent {
                                    drawContent()
                                    if (showDissolveMask) {
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colorStops = Array(17) { i ->
                                                    val t = i / 16f
                                                    t to Color.Black.copy(alpha = 1f - maskAlpha * smootherStep(t))
                                                },
                                                startY = size.height * (1f - 0.42f),
                                                endY = size.height,
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                                }
                                .background(if (isAppleMusic) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Crossfade(
                                targetState = mediaMetadata?.id to (mediaMetadata?.thumbnailUrl ?: currentSong?.song?.thumbnailUrl),
                                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                                label = "artworkCrossfade"
                            ) { (_, thumbnailUrl) ->
                                var currentModel by remember(thumbnailUrl) {
                                    mutableStateOf<Any?>(thumbnailUrl)
                                }
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(currentModel)
                                        .size(CoilSize.ORIGINAL)
                                        .crossfade(true)
                                        .build(),
                                    onError = {
                                        val url = currentModel?.toString()
                                        currentModel = when {
                                            url == null -> null
                                            url.contains("maxresdefault.jpg") -> url.replace("maxresdefault.jpg", "sddefault.jpg")
                                            url.contains("sddefault.jpg") -> url.replace("sddefault.jpg", "hqdefault.jpg")
                                            url.contains("hqdefault.jpg") -> url.replace("hqdefault.jpg", "mqdefault.jpg")
                                            thumbnailUrl != null && url != thumbnailUrl -> thumbnailUrl
                                            else -> null
                                        }
                                    },
                                    contentDescription = null,
                                    contentScale = if (isAppleMusic || cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            val motionVideoUrl = if (isAppleMusic) {
                                canvasArtwork?.animatedTall ?: canvasArtwork?.animated ?: canvasArtwork?.videoUrl
                            } else {
                                canvasArtwork?.animated ?: canvasArtwork?.videoUrl
                            }
                            if (enableCanvas && !isCompactHeader && motionVideoUrl != null) {
                                if (isAppleMusic) {
                                    BackgroundVideoView(
                                        videoUrl = motionVideoUrl,
                                        isPlaying = isPlaying,
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    MotionartworkPlayer(
                                        primaryUrl = motionVideoUrl,
                                        fallbackUrl = canvasArtwork?.videoUrl,
                                        isPlaying = isPlaying,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(isCompactHeader, swipeThumbnailPref, isListenTogetherGuest) {
                                        if (isCompactHeader) {
                                            detectTapGestures(
                                                onTap = {
                                                    showInlineLyrics = false
                                                    showInlineQueue = false
                                                }
                                            )
                                        } else {
                                            detectHorizontalDragGestures { _, dragAmount ->
                                                if (swipeThumbnailPref && !isListenTogetherGuest) {
                                                    if (dragAmount < -20f && canSkipNext) {
                                                        playerConnection.player.seekToNext()
                                                    } else if (dragAmount > 20f && canSkipPrevious) {
                                                        playerConnection.player.seekToPreviousMediaItem()
                                                    }
                                                }
                                            }
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !isFullScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Queue(
                state = queueSheetState,
                playerBottomSheetState = state,
                modifier = Modifier.graphicsLayer { alpha = canvasControlsAlpha },
            navController = navController,
            background =
            if (useBlackBackground) {
                Color.Black
            } else {
                MaterialTheme.colorScheme.surface //fixed the issue causing the queue ui not good surfacecontainer
            },
            onBackgroundColor = onBackgroundColor,
            TextBackgroundColor = TextBackgroundColor,
            textButtonColor = textButtonColor,
            iconButtonColor = iconButtonColor,
            pureBlack = pureBlack,
            showInlineLyrics = showInlineLyrics,
            playerBackground = playerBackground,
            onToggleLyrics = {
                showInlineLyrics = !showInlineLyrics
            },
            )
        }

        if (showAudioDeviceBottomSheet) {
            AudioDeviceBottomSheet(onDismiss = { showAudioDeviceBottomSheet = false })
        }

        if (isCanvasActive && !showCanvasControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showCanvasControls = true
                    }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InlineLyricsView(
    mediaMetadata: MediaMetadata?,
    showLyrics: Boolean,
    positionProvider: () -> Long,
    controlsOverlap: Dp = 0.dp
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    val showLyricsMenu: () -> Unit = {
        mediaMetadata?.let { meta ->
            menuState.show {
                LyricsMenu(
                    lyricsProvider = { currentLyrics },
                    songProvider = { currentSong?.song },
                    mediaMetadataProvider = { meta },
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

    LaunchedEffect(mediaMetadata?.id, currentLyrics) {
        if (mediaMetadata != null && currentLyrics == null) {
            delay(500)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        LyricsHelperEntryPoint::class.java
                    )
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val fetchedLyricsWithProvider = lyricsHelper.getLyrics(mediaMetadata)
                    database.query {
                        upsert(LyricsEntity(mediaMetadata.id, fetchedLyricsWithProvider.lyrics, fetchedLyricsWithProvider.provider))
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }


    Box (
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            lyrics == null -> {
                ContainedLoadingIndicator()
            }
            lyrics == LyricsEntity.LYRICS_NOT_FOUND -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.lyrics_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = showLyricsMenu,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (mediaMetadata != null) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            val entryPoint = EntryPointAccessors.fromApplication(
                                                context.applicationContext,
                                                LyricsHelperEntryPoint::class.java
                                            )
                                            val lyricsHelper = entryPoint.lyricsHelper()
                                            val fetchedLyricsWithProvider = lyricsHelper.getLyrics(mediaMetadata)
                                            database.query {
                                                upsert(
                                                    LyricsEntity(
                                                        mediaMetadata.id,
                                                        fetchedLyricsWithProvider.lyrics,
                                                        fetchedLyricsWithProvider.provider
                                                    )
                                                )
                                            }
                                        } catch (_: Exception) {
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.cached),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.refetch),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
            else -> {
                val lyricsContent: @Composable () -> Unit = {
                    Lyrics(
                        sliderPositionProvider = positionProvider,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        showLyrics = showLyrics,
                        extraBottomPadding = controlsOverlap
                    )
                }
                ProvideTextStyle(
                    value = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                ) {
                    lyricsContent()
                }
            }
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InlineQueueView(
    playerConnection: PlayerConnection,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    primaryColor: Color = Color.White,
    onDraggingChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    val queueWindows by playerConnection.queueWindows.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val automix by playerConnection.service.automixItems.collectAsState()

    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsState(initial = RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST

    val castHandler = remember(playerConnection) {
        try {
            playerConnection.service.castConnectionHandler
        } catch (e: Exception) {
            null
        }
    }

    val isCasting by castHandler?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

    val currentPlayingUid = remember(currentWindowIndex, queueWindows) {
        if (currentWindowIndex in queueWindows.indices) {
            queueWindows[currentWindowIndex].uid
        } else null
    }

    var isQueueLocked by rememberPreference(QueueEditLockKey, defaultValue = true)

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var dismissJob: Job? by remember { mutableStateOf(null) }

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerValue by remember { mutableFloatStateOf(30f) }
    val sleepTimerEnabled = remember(
        playerConnection.service.sleepTimer.triggerTime,
        playerConnection.service.sleepTimer.pauseWhenSongEnd
    ) {
        playerConnection.service.sleepTimer.isActive
    }
    var sleepTimerTimeLeft by remember { mutableLongStateOf(0L) }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft = if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                    playerConnection.player.duration - playerConnection.player.currentPosition
                } else {
                    playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                }
                delay(1000L)
            }
        }
    }


    val mutableQueueWindows = remember {
        mutableStateListOf<Timeline.Window>().apply { addAll(queueWindows) }
    }
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = currentWindowIndex.coerceAtLeast(0))

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        scrollThresholdPadding = WindowInsets.systemBars.asPaddingValues()
    ) { from, to ->
        val currentDragInfo = dragInfo
        dragInfo = if (currentDragInfo == null) {
            from.index to to.index
        } else {
            currentDragInfo.first to to.index
        }
        val safeFrom = from.index.coerceIn(0, mutableQueueWindows.lastIndex)
        val safeTo = to.index.coerceIn(0, mutableQueueWindows.lastIndex)
        mutableQueueWindows.move(safeFrom, safeTo)
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        onDraggingChange(reorderableState.isAnyItemDragging)
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                val safeFrom = from.coerceIn(0, queueWindows.lastIndex)
                val safeTo = to.coerceIn(0, queueWindows.lastIndex)
                if (!playerConnection.player.shuffleModeEnabled) {
                    playerConnection.player.moveMediaItem(safeFrom, safeTo)
                } else {
                    playerConnection.player.setShuffleOrder(
                        DefaultShuffleOrder(
                            queueWindows.map { it.firstPeriodIndex }
                                .toMutableList()
                                .move(safeFrom, safeTo)
                                .toIntArray(),
                            System.currentTimeMillis()
                        )
                    )
                }
                dragInfo = null
            }
        }
    }


    LaunchedEffect(queueWindows) {
        val sameItems = queueWindows.size == mutableQueueWindows.size &&
            queueWindows.indices.all { i -> queueWindows[i].mediaItem.mediaId == mutableQueueWindows[i].mediaItem.mediaId }
        if (!sameItems) {
            mutableQueueWindows.clear()
            mutableQueueWindows.addAll(queueWindows)
        }
    }

    var previousWindowIndex by remember { mutableIntStateOf(currentWindowIndex) }

    LaunchedEffect(currentWindowIndex) {
        if (currentWindowIndex != -1 && currentWindowIndex != previousWindowIndex) {
            previousWindowIndex = currentWindowIndex
            if (currentWindowIndex in mutableQueueWindows.indices && !reorderableState.isAnyItemDragging) {
                val targetUid = if (currentWindowIndex in queueWindows.indices) queueWindows[currentWindowIndex].uid else null
                lazyListState.smoothScrollToTrack(
                    index = currentWindowIndex,
                    key = targetUid?.hashCode()
                )
            }
        }
    }


    val activePrimaryColor = primaryColor
    val activeContainerColor = activePrimaryColor.copy(alpha = 0.82f)
    val inactiveContainerColor = activePrimaryColor.copy(alpha = 0.16f)
    val activeIconColor = Color(0xFF1A1A1A)
    val inactiveIconColor = activePrimaryColor.copy(alpha = 0.85f)
    val activeSecondaryColor = activePrimaryColor.copy(alpha = 0.65f)

    data class QueueBarAction(
        val iconRes: Int,
        val active: Boolean,
        val description: String,
        val enabled: Boolean = true,
        val text: String? = null,
        val onClick: () -> Unit
    )

    val actions = listOf(
        // 1. Shuffle
        QueueBarAction(
            iconRes = R.drawable.shuffle,
            active = shuffleModeEnabled,
            description = "Shuffle",
            enabled = !isListenTogetherGuest,
            onClick = {
                val next = !shuffleModeEnabled
                playerConnection.player.shuffleModeEnabled = next
                coroutineScope.launch {
                    lazyListState.animateScrollToItem(0)
                }
            }
        ),
        // 2. Repeat
        QueueBarAction(
            iconRes = if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat,
            active = repeatMode != Player.REPEAT_MODE_OFF,
            description = "Repeat",
            enabled = !isListenTogetherGuest,
            onClick = { playerConnection.player.toggleRepeatMode() }
        ),
        // 3. Sleep Timer
        QueueBarAction(
            iconRes = R.drawable.timer,
            active = sleepTimerEnabled,
            description = if (sleepTimerEnabled) "Timer active: ${makeTimeString(sleepTimerTimeLeft)}" else "Sleep Timer",
            enabled = !isListenTogetherGuest,
            text = if (sleepTimerEnabled) makeTimeString(sleepTimerTimeLeft) else null,
            onClick = {
                if (sleepTimerEnabled) {
                    playerConnection.service.sleepTimer.clear()
                } else {
                    showSleepTimerDialog = true
                }
            }
        ),
        // 4. Lock / Reorder
        QueueBarAction(
            iconRes = if (isQueueLocked) R.drawable.lock else R.drawable.lock_open,
            active = !isQueueLocked,
            description = stringResource(if (isQueueLocked) R.string.unlock_queue else R.string.lock_queue),
            enabled = !isListenTogetherGuest,
            onClick = { isQueueLocked = !isQueueLocked }
        )
    )

    CompositionLocalProvider(LocalContentColor provides activePrimaryColor) {
        Column(modifier = modifier.fillMaxSize()) {
            // Header: Multi-selection bar OR Top 4 Action pill buttons
            if (inSelectMode) {
                val selectedSongs = remember(selection.toList(), mutableQueueWindows) {
                    mutableQueueWindows.filter { it.mediaItem.mediaId in selection }
                        .mapNotNull { it.mediaItem.metadata }
                }
                val selectedItems = remember(selection.toList(), mutableQueueWindows) {
                    mutableQueueWindows.filter { it.mediaItem.mediaId in selection }
                }
                val count = selection.size
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = PlayerHorizontalPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onExitSelectionMode) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            tint = activePrimaryColor
                        )
                    }
                    Text(
                        text = pluralStringResource(R.plurals.n_selected, count, count),
                        color = activePrimaryColor,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = count == mutableQueueWindows.size && count > 0,
                        onCheckedChange = {
                            if (count == mutableQueueWindows.size) {
                                selection.clear()
                            } else {
                                selection.clear()
                                mutableQueueWindows.forEach {
                                    selection.add(it.mediaItem.mediaId)
                                }
                            }
                        }
                    )
                    IconButton(
                        enabled = count > 0,
                        onClick = {
                            menuState.show {
                                SelectionMediaMetadataMenu(
                                    songSelection = selectedSongs,
                                    onDismiss = menuState::dismiss,
                                    clearAction = onExitSelectionMode,
                                    currentItems = selectedItems
                                )
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                            tint = if (count > 0) activePrimaryColor else activePrimaryColor.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding, vertical = 4.dp)
                ) {
                    actions.forEach { action ->
                        val targetContainerColor = if (action.active) activeContainerColor else inactiveContainerColor
                        val targetIconColor = if (action.active) activeIconColor else inactiveIconColor

                        val containerColor by animateColorAsState(
                            targetValue = targetContainerColor,
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                            label = "queueActionContainer"
                        )
                        val iconColor by animateColorAsState(
                            targetValue = targetIconColor,
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                            label = "queueActionIcon"
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(CircleShape)
                                .background(containerColor)
                                .alpha(if (action.enabled) 1f else 0.4f)
                                .clickable(
                                    enabled = action.enabled,
                                    onClick = action.onClick
                                )
                        ) {
                            if (action.text != null) {
                                Text(
                                    text = action.text,
                                    color = iconColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Icon(
                                    painter = painterResource(action.iconRes),
                                    contentDescription = action.description,
                                    tint = iconColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Queued songs list & Undo Snackbar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = mutableQueueWindows,
                        key = { _, window -> window.uid.hashCode() }
                    ) { index, window ->
                        ReorderableItem(
                            state = reorderableState,
                            key = window.uid.hashCode()
                        ) {
                            val currentItem by rememberUpdatedState(window)
                            val isActive = window.uid == currentPlayingUid
                            val itemAlpha by animateFloatAsState(
                                targetValue = if (currentWindowIndex >= 0 && index < currentWindowIndex) 0.55f else 1f,
                                animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                                label = "queueItemAlpha"
                            )
                            val dismissBoxState = rememberSwipeToDismissBoxState(
                                positionalThreshold = { totalDistance -> totalDistance }
                            )

                            var processedDismiss by remember { mutableStateOf(false) }
                            LaunchedEffect(dismissBoxState.currentValue) {
                                val dv = dismissBoxState.currentValue
                                if (!processedDismiss && !isListenTogetherGuest && (
                                    dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart
                                )) {
                                    processedDismiss = true
                                    playerConnection.player.removeWindow(currentItem)
                                    dismissJob?.cancel()
                                    dismissJob = coroutineScope.launch {
                                        val snackbarResult = snackbarHostState.showSnackbar(
                                            message = context.getString(
                                                R.string.removed_song_from_playlist,
                                                currentItem.mediaItem.metadata?.title
                                            ),
                                            actionLabel = context.getString(R.string.undo),
                                            duration = SnackbarDuration.Short
                                        )
                                        if (snackbarResult == SnackbarResult.ActionPerformed) {
                                            playerConnection.player.addMediaItem(currentItem.mediaItem)
                                            val targetIndex = currentItem.firstPeriodIndex.coerceIn(0, playerConnection.player.mediaItemCount)
                                            Timber.tag("Queue").d("undo: restored '${currentItem.mediaItem.metadata?.title}' at index $targetIndex")
                                            playerConnection.player.moveMediaItem(
                                                mutableQueueWindows.size.coerceIn(0, playerConnection.player.mediaItemCount - 1),
                                                targetIndex
                                            )
                                        }
                                    }
                                }
                                if (dv == SwipeToDismissBoxValue.Settled) {
                                    processedDismiss = false
                                }
                            }

                            val onCheckedChange: (Boolean) -> Unit = { checked ->
                                if (checked) {
                                    selection.add(window.mediaItem.mediaId)
                                } else {
                                    selection.remove(window.mediaItem.mediaId)
                                }
                            }

                            val itemContent: @Composable () -> Unit = {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.graphicsLayer { alpha = itemAlpha }
                                ) {
                                    MediaMetadataListItem(
                                        mediaMetadata = window.mediaItem.metadata!!,
                                        isSelected = inSelectMode && window.mediaItem.mediaId in selection,
                                        isActive = isActive,
                                        isPlaying = isPlaying && isActive,
                                        backgroundColor = Color.Transparent,
                                        subtitleColor = activeSecondaryColor,
                                        trailingContent = {
                                            if (inSelectMode) {
                                                Checkbox(
                                                    checked = window.mediaItem.mediaId in selection,
                                                    onCheckedChange = onCheckedChange
                                                )
                                            } else {
                                                if (!isListenTogetherGuest) {
                                                    IconButton(
                                                        onClick = {
                                                            menuState.show {
                                                                QueueMenu(
                                                                    mediaMetadata = window.mediaItem.metadata!!,
                                                                    navController = navController,
                                                                    playerBottomSheetState = playerBottomSheetState,
                                                                    onShowDetailsDialog = {
                                                                        window.mediaItem.mediaId.let {
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
                                                            painter = painterResource(R.drawable.more_vert),
                                                            contentDescription = null,
                                                            tint = activeSecondaryColor
                                                        )
                                                    }
                                                }
                                                AnimatedVisibility(
                                                    visible = !isQueueLocked && !isListenTogetherGuest,
                                                    enter = fadeIn(tween(180)) + expandHorizontally(tween(180)),
                                                    exit = fadeOut(tween(180)) + shrinkHorizontally(tween(180))
                                                ) {
                                                    IconButton(
                                                        onClick = {},
                                                        modifier = Modifier.draggableHandle()
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.drag_handle),
                                                            contentDescription = null,
                                                            tint = activeSecondaryColor
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    if (inSelectMode) {
                                                        onCheckedChange(window.mediaItem.mediaId !in selection)
                                                    } else if (!isListenTogetherGuest) {
                                                        if (isActive) {
                                                            if (isCasting) {
                                                                if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                                                            } else {
                                                                if (isPlaying) {
                                                                    playerConnection.player.pause()
                                                                } else {
                                                                    playerConnection.player.play()
                                                                }
                                                            }
                                                        } else {
                                                            coroutineScope.launch {
                                                                lazyListState.smoothScrollToTrack(window.firstPeriodIndex, window.uid.hashCode())
                                                            }
                                                            if (isCasting) {
                                                                val mediaId = window.mediaItem.mediaId
                                                                val navigated = castHandler?.navigateToMediaIfInQueue(mediaId) ?: false
                                                                if (!navigated) {
                                                                    playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                                                                    playerConnection.player.playWhenReady = true
                                                                }
                                                            } else {
                                                                playerConnection.player.playWhenReady = true
                                                                playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                                                            }
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!inSelectMode && !isListenTogetherGuest) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        inSelectMode = true
                                                        onCheckedChange(true)
                                                    }
                                                }
                                            )
                                    )
                                }
                            }

                            if (inSelectMode || isQueueLocked) {
                                itemContent()
                            } else {
                                SwipeToDismissBox(
                                    state = dismissBoxState,
                                    backgroundContent = {}
                                ) {
                                    itemContent()
                                }
                            }
                        }
                    }

                    if (automix.isNotEmpty()) {
                        item(key = "automix_divider") {
                            Text(
                                text = stringResource(R.string.similar_content),
                                color = activeSecondaryColor,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }

                        itemsIndexed(
                            items = automix,
                            key = { _, it -> it.mediaId }
                        ) { index, item ->
                            Row(
                                horizontalArrangement = Arrangement.Center
                            ) {
                                MediaMetadataListItem(
                                    mediaMetadata = item.metadata!!,
                                    backgroundColor = Color.Transparent,
                                    subtitleColor = activeSecondaryColor,
                                    trailingContent = {
                                        if (!isListenTogetherGuest) {
                                            IconButton(
                                                onClick = {
                                                    playerConnection.service.playNextAutomix(item, index)
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.playlist_play),
                                                    contentDescription = null,
                                                    tint = activeSecondaryColor
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    playerConnection.service.addToQueueAutomix(item, index)
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.queue_music),
                                                    contentDescription = null,
                                                    tint = activeSecondaryColor
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = {
                                                menuState.show {
                                                    QueueMenu(
                                                        mediaMetadata = item.metadata!!,
                                                        navController = navController,
                                                        playerBottomSheetState = playerBottomSheetState,
                                                        onShowDetailsDialog = {
                                                            item.mediaId.let {
                                                                bottomSheetPageState.show {
                                                                    ShowMediaInfo(it)
                                                                }
                                                            }
                                                        },
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        )
                                )
                            }
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )
            }
        }
    }


    if (showSleepTimerDialog) {
        ActionPromptDialog(
            titleBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.sleep_timer),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            },
            onDismiss = { showSleepTimerDialog = false },
            onConfirm = {
                showSleepTimerDialog = false
                playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
            },
            onCancel = {
                showSleepTimerDialog = false
            },
            onReset = {
                sleepTimerValue = 30f
            },
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.minute,
                            sleepTimerValue.roundToInt(),
                            sleepTimerValue.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Spacer(Modifier.height(16.dp))

                    Slider(
                        value = sleepTimerValue,
                        onValueChange = { sleepTimerValue = it },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            showSleepTimerDialog = false
                            playerConnection.service.sleepTimer.start(-1)
                        }
                    ) {
                        Text(stringResource(R.string.end_of_song))
                    }
                }
            }
        )
    }
}


@Composable
fun MoreActionsButton(
    mediaMetadata: MediaMetadata,
    navController: NavController,
    state: BottomSheetState,
    textButtonColor: Color,
    iconButtonColor: Color
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(textButtonColor)
            .clickable {
                menuState.show {
                    PlayerMenu(
                        mediaMetadata = mediaMetadata,
                        navController = navController,
                        playerBottomSheetState = state,
                        onShowDetailsDialog = {
                            mediaMetadata.id.let {
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
        Image(
            painter = painterResource(R.drawable.more_vert),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconButtonColor)
        )
    }
}

@Composable
private fun PlayerMoreMenuButton(
    mediaMetadata: MediaMetadata,
    navController: NavController,
    state: BottomSheetState,
    textButtonColor: Color,
    iconButtonColor: Color,
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    Box(
        contentAlignment = Alignment.Center,
        modifier =
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(textButtonColor)
            .clickable {
                menuState.show {
                    PlayerMenu(
                        mediaMetadata = mediaMetadata,
                        navController = navController,
                        playerBottomSheetState = state,
                        onShowDetailsDialog = {
                            mediaMetadata.id.let {
                                bottomSheetPageState.show {
                                    ShowMediaInfo(it)
                                }
                            }
                        },
                        onDismiss = menuState::dismiss,
                    )
                }
            },
    ) {
        Image(
            painter = painterResource(R.drawable.more_horiz),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconButtonColor),
        )
    }
}

private fun smootherStep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * x * (x * (x * 6f - 15f) + 10f)
}

@Composable
private fun BackgroundVideoView(
    videoUrl: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
) {
    val context = LocalContext.current
    var isVideoReady by remember(videoUrl) { mutableStateOf(false) }
    
    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setMaxVideoSize(4096, 4096)
                .setForceHighestSupportedBitrate(true)
                .build()
        }
    }


    val spotifyCanvasCache = LocalPlayerConnection.current?.service?.spotifyCanvasCache

    val exoPlayer = remember {
        val defaultDataSourceFactory = DefaultDataSource.Factory(context)
        val mediaSourceFactory = if (spotifyCanvasCache != null) {
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(spotifyCanvasCache)
                .setUpstreamDataSourceFactory(defaultDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            DefaultMediaSourceFactory(context)
                .setDataSourceFactory(cacheDataSourceFactory)
        } else {
            DefaultMediaSourceFactory(context)
        }

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setTargetBufferBytes(20 * 1024 * 1024) // 20MB buffer for 4K
                    .build()
            )
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                playWhenReady = isPlaying
            }
    }


    val aspectRatioFrameLayout = remember(resizeMode) {
        AspectRatioFrameLayout(context).apply {
            this.resizeMode = resizeMode
            setBackgroundColor(AndroidColor.TRANSPARENT)
        }
    }


    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    aspectRatioFrameLayout.setAspectRatio(videoSize.width.toFloat() / videoSize.height)
                }
            }
            override fun onRenderedFirstFrame() {
                isVideoReady = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(videoUrl) {
        isVideoReady = false
        val mediaItem = MediaItem.Builder()
            .setUri(videoUrl)
            .setMimeType(if (videoUrl.contains("m3u8")) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4)
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    LaunchedEffect(resizeMode) {
        exoPlayer.videoScalingMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
            C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        } else {
            C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        }
    }


    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }


    val alpha by animateFloatAsState(
        targetValue = if (isVideoReady) 1f else 0f,
        animationSpec = tween(800),
        label = "videoAlpha"
    )

    AndroidView(
        factory = { _ ->
            aspectRatioFrameLayout.apply {
                // Ensure the view doesn't capture touches intended for other sections
                isEnabled = false
                isClickable = false
                isFocusable = false

                // Ensure TextureView is added only once
                if (childCount == 0) {
                    val textureView = TextureView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    }
                    addView(textureView)
                    exoPlayer.setVideoTextureView(textureView)
                }
            }
        },
        modifier = modifier.alpha(alpha)
    )
}

internal fun Modifier.bottomControlsMask(revealFraction: () -> Float): Modifier =
    graphicsLayer {
        val fraction = revealFraction()
        compositingStrategy = if (fraction < 1.1f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }.drawWithContent {
        drawContent()
        val fraction = revealFraction()
        if (fraction < 1.1f) {
            val fadeTop = (fraction - 0.12f).coerceIn(0f, 1f)
            val fadeBottom = fraction.coerceIn(fadeTop + 0.01f, 1f)
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Black,
                    fadeTop to Color.Black,
                    fadeBottom to Color.Transparent,
                    1f to Color.Transparent
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }

internal fun Modifier.bottomControlsMask(revealFraction: Float): Modifier =
    bottomControlsMask { revealFraction }


private fun Modifier.compactPanelGestures(
    key1: Boolean,
    key2: Boolean,
    onScrollDown: () -> Unit,
    onScrollUp: () -> Unit,
    onTapInteraction: () -> Unit
): Modifier = pointerInput(key1, key2) {
    var downPos = Offset.Zero
    var lastPos = Offset.Zero
    var dragged = false
    var verticalDragDelta = 0f
    val touchSlop = viewConfiguration.touchSlop
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull() ?: continue
            if (change.changedToDown()) {
                downPos = change.position
                lastPos = change.position
                dragged = false
                verticalDragDelta = 0f
            } else if (change.pressed) {
                val deltaY = change.position.y - lastPos.y
                lastPos = change.position
                if (!dragged && (change.position - downPos).getDistance() > touchSlop) {
                    dragged = true
                }
                if (deltaY < 0f) {
                    if (verticalDragDelta > 0f) verticalDragDelta = 0f
                    verticalDragDelta += deltaY
                    if (verticalDragDelta < -touchSlop) {
                        verticalDragDelta = 0f
                        onScrollDown()
                    }
                } else if (deltaY > 0f) {
                    if (verticalDragDelta < 0f) verticalDragDelta = 0f
                    verticalDragDelta += deltaY
                    if (verticalDragDelta > touchSlop) {
                        verticalDragDelta = 0f
                        onScrollUp()
                    }
                }
            } else if (change.changedToUp()) {
                if (!dragged) {
                    onTapInteraction()
                }
            }
        }
    }

}
