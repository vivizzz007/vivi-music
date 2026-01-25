package com.music.vivi.ui.player

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.AppleMusicLyricsBlurKey
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
import com.music.vivi.constants.SwipeGestureEnabledKey
import com.music.vivi.db.entities.LyricsEntity
import com.music.vivi.extensions.toggleRepeatMode
import com.music.vivi.lyrics.LyricsResult
import com.music.vivi.models.MediaMetadata
import com.music.vivi.ui.component.BottomSheet
import com.music.vivi.ui.component.BottomSheetState
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.ListDialog
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.Lyrics
import com.music.vivi.ui.menu.LyricsMenu
import com.music.vivi.ui.theme.PlayerColorExtractor
import com.music.vivi.update.apple.SwipeGesture
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.LyricsMenuViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LyricsBottomSheet(
    state: BottomSheetState,
    navController: NavController,
    mediaMetadata: MediaMetadata,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LyricsMenuViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    val repeatMode by playerConnection.repeatMode.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val swipeGestureEnabled by rememberPreference(SwipeGestureEnabledKey, defaultValue = true)

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(C.TIME_UNSET) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    var resyncTrigger by remember { mutableStateOf(0) }
    var isAutoScrollActive by remember { mutableStateOf(true) }

    // Search Dialog States
    var showSearchDialog by rememberSaveable { mutableStateOf(false) }
    var showSearchResultDialog by rememberSaveable { mutableStateOf(false) }

    val (titleField, onTitleFieldChange) = rememberSaveable(showSearchDialog, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = mediaMetadata.title))
    }
    val (artistField, onArtistFieldChange) = rememberSaveable(showSearchDialog, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = mediaMetadata.artists.joinToString { it.name }))
    }

    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState(initial = false)

    // Lyrics Fetching Logic
    var lyricsFetchJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(mediaMetadata.id, currentLyrics) {
        if (currentLyrics == null) {
            lyricsFetchJob?.cancel()
            lyricsFetchJob = coroutineScope.launch(Dispatchers.IO) {
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
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(16) // Update at ~60fps
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
            }
        }
    }

    // Preferences
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.GRADIENT)
    val appleMusicLyricsBlur by rememberPreference(AppleMusicLyricsBlurKey, defaultValue = true)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = isSystemInDarkTheme

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata.id, playerBackground) {
        if ((playerBackground == PlayerBackgroundStyle.GRADIENT || playerBackground == PlayerBackgroundStyle.APPLE_MUSIC) &&
            mediaMetadata.thumbnailUrl != null
        ) {
            val cachedColors = gradientColorsCache[mediaMetadata.id]
            if (cachedColors != null) {
                gradientColors = cachedColors
                return@LaunchedEffect
            }
            withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(mediaMetadata.thumbnailUrl)
                    .size(100, 100)
                    .allowHardware(false)
                    .memoryCacheKey("gradient_${mediaMetadata.id}")
                    .build()
                val result = runCatching { context.imageLoader.execute(request).image }.getOrNull()
                if (result != null) {
                    val bitmap = result.toBitmap()
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap)
                            .maximumColorCount(8)
                            .resizeBitmapArea(100 * 100)
                            .generate()
                    }
                    val extractedColors = PlayerColorExtractor.extractGradientColors(
                        palette = palette,
                        fallbackColor = fallbackColor
                    )
                    gradientColorsCache[mediaMetadata.id] = extractedColors
                    withContext(Dispatchers.Main) { gradientColors = extractedColors }
                }
            }
        } else if (playerBackground == PlayerBackgroundStyle.DEFAULT) {
            gradientColors = emptyList()
        }
    }

    val textBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.APPLE_MUSIC -> Color.White
    }

    // Colors for buttons (mirrors Queue.kt style)
    val toggleButtonColors = ToggleButtonDefaults.toggleButtonColors(
        containerColor = if (playerBackground == PlayerBackgroundStyle.DEFAULT) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.White.copy(alpha = 0.1f)
        },
        contentColor = if (playerBackground == PlayerBackgroundStyle.DEFAULT) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            Color.White
        },
        checkedContainerColor = if (playerBackground == PlayerBackgroundStyle.DEFAULT) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.White
        },
        checkedContentColor = if (playerBackground == PlayerBackgroundStyle.DEFAULT) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.primary
        }
    )

    BottomSheet(
        state = state,
        background = {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (playerBackground) {
                        PlayerBackgroundStyle.BLUR -> {
                            AnimatedContent(
                                targetState = mediaMetadata.thumbnailUrl,
                                transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) },
                                label = "blurBackground"
                            ) { thumbnailUrl ->
                                if (thumbnailUrl != null) {
                                    AsyncImage(
                                        model = thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.FillBounds,
                                        modifier = Modifier.fillMaxSize().blur(if (useDarkTheme) 150.dp else 100.dp)
                                    )
                                }
                            }
                        }
                        PlayerBackgroundStyle.GRADIENT -> {
                            AnimatedContent(
                                targetState = gradientColors,
                                transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) },
                                label = "gradientBackground"
                            ) { colors ->
                                if (colors.isNotEmpty()) {
                                    val gradientColorStops = if (colors.size >= 3) {
                                        arrayOf(0.0f to colors[0], 0.5f to colors[1], 1.0f to colors[2])
                                    } else {
                                        arrayOf(
                                            0.0f to colors[0],
                                            0.6f to colors[0].copy(alpha = 0.7f),
                                            1.0f to Color.Black
                                        )
                                    }
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(
                                            Brush.verticalGradient(colorStops = gradientColorStops)
                                        )
                                    )
                                }
                            }
                        }
                        PlayerBackgroundStyle.APPLE_MUSIC -> {
                            AnimatedContent(
                                targetState = gradientColors,
                                transitionSpec = { fadeIn(tween(1200)).togetherWith(fadeOut(tween(1200))) },
                                label = "appleMusicLyricsBackground"
                            ) { colors ->
                                if (colors.isNotEmpty()) {
                                    val color1 = colors[0]
                                    val color2 = colors.getOrElse(1) { colors[0].copy(alpha = 0.8f) }
                                    val color3 = colors.getOrElse(2) { colors[0].copy(alpha = 0.6f) }
                                    Canvas(
                                        modifier = Modifier.fillMaxSize().then(
                                            if (appleMusicLyricsBlur) Modifier.blur(100.dp) else Modifier
                                        )
                                    ) {
                                        drawRect(brush = Brush.verticalGradient(listOf(color1, color2, color3)))
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                listOf(color1, Color.Transparent),
                                                center = Offset(
                                                    size.width * 0.2f,
                                                    size.height * 0.2f
                                                ),
                                                radius = size.width * 0.8f
                                            ),
                                            center = Offset(
                                                size.width * 0.2f,
                                                size.height * 0.2f
                                            ),
                                            radius = size.width * 0.8f
                                        )
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                listOf(color2, Color.Transparent),
                                                center = Offset(
                                                    size.width * 0.8f,
                                                    size.height * 0.5f
                                                ),
                                                radius = size.width * 0.7f
                                            ),
                                            center = Offset(
                                                size.width * 0.8f,
                                                size.height * 0.5f
                                            ),
                                            radius = size.width * 0.7f
                                        )
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                listOf(color3, Color.Transparent),
                                                center = Offset(
                                                    size.width * 0.3f,
                                                    size.height * 0.8f
                                                ),
                                                radius = size.width * 0.9f
                                            ),
                                            center = Offset(
                                                size.width * 0.3f,
                                                size.height * 0.8f
                                            ),
                                            radius = size.width * 0.9f
                                        )
                                    }
                                }
                            }
                        }
                        PlayerBackgroundStyle.DEFAULT -> {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
                        }
                    }
                    if (playerBackground != PlayerBackgroundStyle.DEFAULT) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                    }
                }
            }
        },
        onDismiss = onDismiss,
        collapsedContent = { /* Fully hidden */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
                )
                .SwipeGesture(
                    enabled = swipeGestureEnabled,
                    onSwipeLeft = { playerConnection.player.seekToPrevious() },
                    onSwipeRight = { playerConnection.player.seekToNext() }
                )
        ) {
            // 1. Header (Queue-like)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp, 4.dp)
                        .clip(CircleShape)
                        .background(textBackgroundColor.copy(alpha = 0.4f))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(16.dp))

                val overlayAlpha by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isPlaying) 0.0f else 0.4f,
                    label = "overlay_alpha",
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = textBackgroundColor.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (playbackState == Player.STATE_ENDED) {
                                    playerConnection.player.seekTo(0, 0)
                                    playerConnection.player.playWhenReady = true
                                } else {
                                    if (isPlaying) playerConnection.player.pause() else playerConnection.player.play()
                                }
                            }
                    ) {
                        AsyncImage(
                            model = mediaMetadata.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Color.Black.copy(alpha = overlayAlpha),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )

                        androidx.compose.animation.AnimatedVisibility(
                            visible = playbackState == Player.STATE_ENDED || !isPlaying,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (playbackState == Player.STATE_ENDED) {
                                        R.drawable.replay
                                    } else {
                                        R.drawable.play
                                    }
                                ),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mediaMetadata.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = textBackgroundColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = mediaMetadata.artists.joinToString { it.name },
                            style = MaterialTheme.typography.bodyMedium,
                            color = textBackgroundColor.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        mediaMetadata.album?.title?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = textBackgroundColor.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = playerConnection::toggleLike,
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (playerBackground ==
                                    PlayerBackgroundStyle.DEFAULT
                                ) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    Color.White.copy(
                                        alpha = 0.1f
                                    )
                                },
                                contentColor = if (currentSong?.song?.liked ==
                                    true
                                ) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    textBackgroundColor
                                }
                            )
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (currentSong?.song?.liked ==
                                        true
                                    ) {
                                        R.drawable.favorite
                                    } else {
                                        R.drawable.favorite_border
                                    }
                                ),
                                contentDescription = null
                            )
                        }
                        FilledTonalIconButton(
                            onClick = {
                                menuState.show {
                                    LyricsMenu(
                                        lyricsProvider = { currentLyrics },
                                        songProvider = { currentSong?.song },
                                        mediaMetadataProvider = { mediaMetadata },
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            },
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (playerBackground ==
                                    PlayerBackgroundStyle.DEFAULT
                                ) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    Color.White.copy(
                                        alpha = 0.1f
                                    )
                                },
                                contentColor = textBackgroundColor
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null
                            )
                        }
                    }
                }
            }

            // 2. Control Buttons (Shuffle, Repeat, Timer) - Mirrored from Queue.kt Expanded Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                // Lyrics Search
                ToggleButton(
                    checked = false,
                    onCheckedChange = {
                        showSearchDialog = true
                    },
                    colors = toggleButtonColors,
                    modifier = Modifier.weight(1f).height(60.dp),
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.search_lyrics),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Repeat
                ToggleButton(
                    checked = repeatMode != Player.REPEAT_MODE_OFF,
                    onCheckedChange = { playerConnection.player.toggleRepeatMode() },
                    colors = toggleButtonColors,
                    modifier = Modifier.weight(1f).height(60.dp),
                    shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(
                                id = when (repeatMode) {
                                    Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                    else -> R.drawable.repeat
                                }
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.repeat),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Resync
                ToggleButton(
                    checked = !isAutoScrollActive,
                    onCheckedChange = {
                        sliderPosition = null
                        resyncTrigger++
                    },
                    colors = toggleButtonColors,
                    modifier = Modifier.weight(1f).height(60.dp),
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.sync),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.action_resync),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 3. Lyrics Content
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Lyrics(
                    sliderPositionProvider = { sliderPosition ?: position },
                    modifier = Modifier.padding(horizontal = 4.dp),
                    playingPosition = position,
                    showResyncButton = false,
                    resyncTrigger = resyncTrigger,
                    onAutoScrollChange = { isActive -> isAutoScrollActive = isActive }
                )
            }
        }
    }

    if (showSearchDialog) {
        DefaultDialog(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            onDismiss = { showSearchDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.search_lyrics)) },
            buttons = {
                TextButton(
                    onClick = { showSearchDialog = false }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }

                Spacer(Modifier.width(8.dp))

                TextButton(
                    onClick = {
                        showSearchDialog = false
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_WEB_SEARCH).apply {
                                    putExtra(
                                        SearchManager.QUERY,
                                        "${artistField.text} ${titleField.text} lyrics"
                                    )
                                }
                            )
                        } catch (_: Exception) {
                        }
                    }
                ) {
                    Text(stringResource(R.string.search_online))
                }

                Spacer(Modifier.width(8.dp))

                TextButton(
                    onClick = {
                        viewModel.search(
                            mediaMetadata.id,
                            titleField.text,
                            artistField.text,
                            mediaMetadata.duration
                        )
                        showSearchResultDialog = true

                        if (!isNetworkAvailable) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.error_no_internet),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        ) {
            OutlinedTextField(
                value = titleField,
                onValueChange = onTitleFieldChange,
                singleLine = true,
                label = { Text(stringResource(R.string.song_title)) }
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = artistField,
                onValueChange = onArtistFieldChange,
                singleLine = true,
                label = { Text(stringResource(R.string.song_artists)) }
            )
        }
    }

    if (showSearchResultDialog) {
        val results by viewModel.results.collectAsState(initial = emptyList<LyricsResult>())
        val isLoading by viewModel.isLoading.collectAsState(initial = false)

        var expandedItemIndex by remember {
            mutableStateOf(-1)
        }

        ListDialog(
            onDismiss = { showSearchResultDialog = false }
        ) {
            itemsIndexed(results) { index, result ->
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            showSearchResultDialog = false
                            viewModel.cancelSearch()
                            viewModel.updateLyrics(mediaMetadata.id, result.lyrics)
                        }
                        .padding(12.dp)
                        .animateContentSize()
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = result.lyrics,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (index == expandedItemIndex) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = result.providerName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1
                            )
                            if (result.lyrics.startsWith("[")) {
                                Icon(
                                    painter = painterResource(R.drawable.sync),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier =
                                    Modifier
                                        .padding(start = 4.dp)
                                        .size(18.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            expandedItemIndex = if (expandedItemIndex == index) -1 else index
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                if (index ==
                                    expandedItemIndex
                                ) {
                                    R.drawable.expand_less
                                } else {
                                    R.drawable.expand_more
                                }
                            ),
                            contentDescription = null
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (!isLoading && results.isEmpty()) {
                item {
                    Text(
                        text = context.getString(R.string.lyrics_not_found),
                        textAlign = TextAlign.Center,
                        modifier =
                        Modifier
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}
