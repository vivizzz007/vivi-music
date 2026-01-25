package com.music.vivi.ui.player

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.constants.QueueEditLockKey
import com.music.vivi.constants.SmartSuggestionsKey
import com.music.vivi.constants.UseNewPlayerDesignKey
import com.music.vivi.extensions.metadata
import com.music.vivi.extensions.move
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.extensions.toggleRepeatMode
import com.music.vivi.models.MediaMetadata
import com.music.vivi.ui.component.ActionPromptDialog
import com.music.vivi.ui.component.BottomSheet
import com.music.vivi.ui.component.BottomSheetState
import com.music.vivi.ui.component.LocalBottomSheetPageState
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.RoundedCheckbox
import com.music.vivi.ui.component.media.common.MediaMetadataListItem
import com.music.vivi.ui.menu.PlayerMenu
import com.music.vivi.ui.menu.SelectionMediaMetadataMenu
import com.music.vivi.ui.utils.ShowMediaInfo
import com.music.vivi.utils.makeTimeString
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.SimpleDateFormat
import java.util.Locale

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Queue(
    state: BottomSheetState,
    playerBottomSheetState: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    background: Color,
    onBackgroundColor: Color,
    TextBackgroundColor: Color,
    textButtonColor: Color,
    iconButtonColor: Color,
    onShowLyrics: () -> Unit = {},
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboard.current
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    val sleepTimerState = rememberTimePickerState(
        initialHour = 0,
        initialMinute = 30,
        is24Hour = true
    )
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState() // ADD THIS LINE

    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)

    val selectedSongs = remember { mutableStateListOf<MediaMetadata>() }
    val selectedItems = remember { mutableStateListOf<Timeline.Window>() }
    var selection by remember { mutableStateOf(false) }

    if (selection) {
        BackHandler {
            selection = false
        }
    }

    var locked by rememberPreference(QueueEditLockKey, defaultValue = true)

    val (useNewPlayerDesign, onUseNewPlayerDesignChange) = rememberPreference(
        UseNewPlayerDesignKey,
        defaultValue = true
    )

    var smartSuggestions by rememberPreference(SmartSuggestionsKey, defaultValue = false)

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

    BottomSheet(
        state = state,
        background = {
            Box(Modifier.fillMaxSize().background(Color.Unspecified))
        },
        modifier = modifier,
        collapsedContent = {
            if (useNewPlayerDesign) {
                // New design
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp, vertical = 12.dp)
                        .windowInsetsPadding(
                            WindowInsets.systemBars.only(
                                WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                            )
                        )
                ) {
                    val buttonSize = 42.dp
                    val iconSize = 24.dp
                    val borderColor = TextBackgroundColor.copy(alpha = 0.35f)
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(buttonSize)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 50.dp,
                                    bottomStart = 50.dp,
                                    topEnd = 5.dp,
                                    bottomEnd = 5.dp
                                )
                            )
                            .border(
                                1.dp,
                                borderColor,
                                RoundedCornerShape(
                                    topStart = 50.dp,
                                    bottomStart = 50.dp,
                                    topEnd = 5.dp,
                                    bottomEnd = 5.dp
                                )
                            )
                            .clickable { state.expandSoft() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.queue_music),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                            tint = TextBackgroundColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(buttonSize)
                            .clip(RoundedCornerShape(5.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(5.dp))
                            .clickable {
                                if (sleepTimerEnabled) {
                                    playerConnection.service.sleepTimer.clear()
                                } else {
                                    showSleepTimerDialog = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            label = "sleepTimer",
                            targetState = sleepTimerEnabled
                        ) { enabled ->
                            if (enabled) {
                                Text(
                                    text = makeTimeString(sleepTimerTimeLeft),
                                    color = TextBackgroundColor,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .basicMarquee()
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.bedtime),
                                    contentDescription = null,
                                    modifier = Modifier.size(iconSize),
                                    tint = TextBackgroundColor
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(buttonSize)
                            .clip(RoundedCornerShape(5.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(5.dp))
                            .clickable {
                                onShowLyrics()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.lyrics),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                            tint = TextBackgroundColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(buttonSize)
                            .clip(RoundedCornerShape(5.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(5.dp))
                            .clickable {
                                playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.shuffle),
                            contentDescription = null,
                            modifier = Modifier
                                .size(iconSize)
                                .alpha(if (shuffleModeEnabled) 1f else 0.5f), // CHANGE THIS LINE - use shuffleModeEnabled instead
                            tint = TextBackgroundColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(buttonSize)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 5.dp,
                                    bottomStart = 5.dp,
                                    topEnd = 50.dp,
                                    bottomEnd = 50.dp
                                )
                            )
                            .border(
                                1.dp,
                                borderColor,
                                RoundedCornerShape(
                                    topStart = 5.dp,
                                    bottomStart = 5.dp,
                                    topEnd = 50.dp,
                                    bottomEnd = 50.dp
                                )
                            )
                            .clickable {
                                playerConnection.player.toggleRepeatMode()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                id = when (repeatMode) {
                                    Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                    else -> R.drawable.repeat
                                }
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .size(iconSize)
                                .alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f),
                            tint = TextBackgroundColor
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
//                    Spacer(modifier = Modifier.weight(1f))
//
//                    Box(
//                        modifier = Modifier
//                            .size(buttonSize)
//                            .clip(CircleShape)
//                            .background(textButtonColor)
//                            .clickable {
//                                menuState.show {
//                                    PlayerMenu(
//                                        mediaMetadata = mediaMetadata,
//                                        navController = navController,
//                                        playerBottomSheetState = playerBottomSheetState,
//                                        onShowDetailsDialog = {
//                                            mediaMetadata?.id?.let {
//                                                bottomSheetPageState.show {
//                                                    ShowMediaInfo(it)
//                                                }
//                                            }
//                                        },
//                                        onDismiss = menuState::dismiss
//                                    )
//                                }
//                            },
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.more_vert),
//                            contentDescription = null,
//                            modifier = Modifier.size(iconSize),
//                            tint = iconButtonColor
//                        )
//                    }
                }
            } else {
                // Old design
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp, vertical = 12.dp)
                        .windowInsetsPadding(
                            WindowInsets.systemBars
                                .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                        )
                ) {
                    TextButton(
                        onClick = { state.expandSoft() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.queue_music),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = TextBackgroundColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.queue),
                                color = TextBackgroundColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            if (sleepTimerEnabled) {
                                playerConnection.service.sleepTimer.clear()
                            } else {
                                showSleepTimerDialog = true
                            }
                        },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.bedtime),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = TextBackgroundColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            AnimatedContent(
                                label = "sleepTimer",
                                targetState = sleepTimerEnabled
                            ) { enabled ->
                                if (enabled) {
                                    Text(
                                        text = makeTimeString(sleepTimerTimeLeft),
                                        color = TextBackgroundColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.basicMarquee()
                                    )
                                } else {
                                    Text(
                                        text = stringResource(id = R.string.sleep_timer),
                                        color = TextBackgroundColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { onShowLyrics() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.lyrics),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = TextBackgroundColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.lyrics),
                                color = TextBackgroundColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.basicMarquee()
                            )
                        }
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
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    },
                    onDismiss = { showSleepTimerDialog = false },
                    onConfirm = {
                        // Treat the time picker as a duration (hours and minutes from now)
                        val durationInMillis = (sleepTimerState.hour * 3600 + sleepTimerState.minute * 60) * 1000L

                        if (durationInMillis > 0) {
                            playerConnection.service.sleepTimer.start(durationInMillis)
                        }
                        showSleepTimerDialog = false
                    },
                    onCancel = {
                        showSleepTimerDialog = false
                    },
                    onReset = {
                        // Reset to default time (0:30)
                        sleepTimerState.hour = 0
                        sleepTimerState.minute = 30
                    },
                    content = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TimeInput(state = sleepTimerState)

                            Spacer(Modifier.height(16.dp))

                            OutlinedButton(
                                onClick = {
                                    showSleepTimerDialog = false
                                    playerConnection.service.sleepTimer.start(-1L)
                                }
                            ) {
                                Text(stringResource(R.string.end_of_song))
                            }
                        }
                    }
                )
            }
        }
    ) {
        val queueTitle by playerConnection.queueTitle.collectAsState()
        val queueWindows by playerConnection.queueWindows.collectAsState()
        val automix by playerConnection.service.automixItems.collectAsState()
        val mutableQueueWindows = remember { mutableStateListOf<Timeline.Window>() }
        val queueLength =
            remember(queueWindows) {
                queueWindows.sumOf { it.mediaItem.metadata!!.duration }
            }

        val currentSong by playerConnection.currentSong.collectAsState(initial = null)

        val coroutineScope = rememberCoroutineScope()

        val headerItems = 1
        val lazyListState = rememberLazyListState()
        var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }

        val reorderableState = rememberReorderableLazyListState(
            lazyListState = lazyListState,
            scrollThresholdPadding = WindowInsets.systemBars.add(
                WindowInsets(
                    top = ListItemHeight,
                    bottom = ListItemHeight
                )
            ).asPaddingValues()
        ) { from, to ->
            val currentDragInfo = dragInfo
            dragInfo = if (currentDragInfo == null) {
                from.index to to.index
            } else {
                currentDragInfo.first to to.index
            }

            val safeFrom = (from.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)
            val safeTo = (to.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)

            mutableQueueWindows.move(safeFrom, safeTo)
        }

        LaunchedEffect(reorderableState.isAnyItemDragging) {
            if (!reorderableState.isAnyItemDragging) {
                dragInfo?.let { (from, to) ->
                    val safeFrom = (from - headerItems).coerceIn(0, queueWindows.lastIndex)
                    val safeTo = (to - headerItems).coerceIn(0, queueWindows.lastIndex)

                    if (!shuffleModeEnabled) {
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
            mutableQueueWindows.apply {
                clear()
                addAll(queueWindows)
            }
        }

        LaunchedEffect(mutableQueueWindows) {
            if (currentWindowIndex != -1) {
                lazyListState.scrollToItem(currentWindowIndex)
            }
        }

        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .background(background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Static Header Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.statusBars.only(WindowInsetsSides.Top)
                        )
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp, top = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(16.dp))
                    // Current Song Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(mediaMetadata?.thumbnailUrl)
                                .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                                .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                                .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mediaMetadata?.title.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            mediaMetadata?.album?.title?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val likeDescription = if (currentSong?.song?.liked ==
                                true
                            ) {
                                stringResource(R.string.action_remove_like)
                            } else {
                                stringResource(R.string.action_like)
                            }
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Above
                                ),
                                tooltip = { PlainTooltip { Text(likeDescription) } },
                                state = rememberTooltipState()
                            ) {
                                FilledTonalIconButton(
                                    onClick = playerConnection::toggleLike,
                                    shapes = IconButtonDefaults.shapes()
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
                                        contentDescription = likeDescription,
                                        tint = if (currentSong?.song?.liked ==
                                            true
                                        ) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }

                            val lockDescription = if (locked) {
                                stringResource(
                                    R.string.unlock_queue
                                )
                            } else {
                                stringResource(R.string.lock_queue)
                            }
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Above
                                ),
                                tooltip = { PlainTooltip { Text(lockDescription) } },
                                state = rememberTooltipState()
                            ) {
                                FilledTonalIconButton(
                                    onClick = { locked = !locked },
                                    shapes = IconButtonDefaults.shapes()
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (locked) R.drawable.lock else R.drawable.lock_open
                                        ),
                                        contentDescription = lockDescription,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            val moreDescription = stringResource(R.string.more_options)
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Above
                                ),
                                tooltip = { PlainTooltip { Text(moreDescription) } },
                                state = rememberTooltipState()
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        menuState.show {
                                            PlayerMenu(
                                                mediaMetadata = mediaMetadata,
                                                navController = navController,
                                                playerBottomSheetState = playerBottomSheetState,
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
                                    shapes = IconButtonDefaults.shapes()
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = moreDescription,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Control Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        ToggleButton(
                            checked = shuffleModeEnabled,
                            onCheckedChange = {
                                playerConnection.player.shuffleModeEnabled =
                                    !playerConnection.player.shuffleModeEnabled
                            },
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                checkedContainerColor = MaterialTheme.colorScheme.primary,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                            Text(
                                text = stringResource(R.string.shuffle),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        ToggleButton(
                            checked = repeatMode != Player.REPEAT_MODE_OFF,
                            onCheckedChange = {
                                playerConnection.player.toggleRepeatMode()
                            },
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                checkedContainerColor = MaterialTheme.colorScheme.primary,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                        ) {
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
                            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                            Text(
                                text = stringResource(R.string.repeat),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        ToggleButton(
                            checked = smartSuggestions,
                            onCheckedChange = {
                                smartSuggestions = !smartSuggestions
                            },
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                checkedContainerColor = MaterialTheme.colorScheme.primary,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.loop_queue),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                            Text(
                                text = stringResource(R.string.suggestions),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Stats Header
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.continue_playing),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (automix.isNotEmpty()) {
                                    stringResource(
                                        R.string.upcoming_recommendations
                                    )
                                } else {
                                    stringResource(R.string.next_in_queue)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.n_song,
                                    queueWindows.size,
                                    queueWindows.size
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = makeTimeString(queueLength * 1000L),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = selection,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (pureBlack) {
                                Color.Black
                            } else {
                                MaterialTheme.colorScheme
                                    .secondaryContainer
                                    .copy(alpha = 0.90f)
                            }
                        )
                ) {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight)
                                .padding(horizontal = 12.dp)
                        ) {
                            val count = selectedSongs.size
                            val closeDescription = stringResource(R.string.close)
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Above
                                ),
                                tooltip = { PlainTooltip { Text(closeDescription) } },
                                state = rememberTooltipState()
                            ) {
                                FilledTonalIconButton(
                                    onClick = { selection = false },
                                    shapes = IconButtonDefaults.shapes()
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.close),
                                        contentDescription = closeDescription
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.elements_selected, count),
                                modifier = Modifier.weight(1f)
                            )
                            RoundedCheckbox(
                                checked = count == mutableQueueWindows.size,
                                onCheckedChange = {
                                    if (count == mutableQueueWindows.size) {
                                        selectedSongs.clear()
                                        selectedItems.clear()
                                    } else {
                                        queueWindows
                                            .filter { it.mediaItem.metadata!! !in selectedSongs }
                                            .forEach {
                                                selectedSongs.add(it.mediaItem.metadata!!)
                                                selectedItems.add(it)
                                            }
                                    }
                                }
                            )

                            val selectionMoreDescription = stringResource(R.string.more_options)
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Above
                                ),
                                tooltip = { PlainTooltip { Text(selectionMoreDescription) } },
                                state = rememberTooltipState()
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        menuState.show {
                                            SelectionMediaMetadataMenu(
                                                songSelection = selectedSongs,
                                                onDismiss = menuState::dismiss,
                                                clearAction = {
                                                    selectedSongs.clear()
                                                    selectedItems.clear()
                                                },
                                                currentItems = selectedItems
                                            )
                                        }
                                    },
                                    shapes = IconButtonDefaults.shapes()
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = selectionMoreDescription,
                                        tint = LocalContentColor.current
                                    )
                                }
                            }
                        }
                        if (pureBlack) {
                            HorizontalDivider()
                        }
                    }
                }

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .nestedScroll(state.preUpPostDownNestedScrollConnection),
                    contentPadding = WindowInsets.systemBars
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                        .add(WindowInsets(bottom = ListItemHeight + 8.dp))
                        .asPaddingValues()
                ) {
                    item(key = "queue_top_spacer") {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    itemsIndexed(
                        items = mutableQueueWindows,
                        key = { _, item -> item.uid.hashCode() }
                    ) { index, window ->
                        ReorderableItem(
                            state = reorderableState,
                            key = window.uid.hashCode()
                        ) {
                            val currentItem by rememberUpdatedState(window)
                            val dismissBoxState =
                                rememberSwipeToDismissBoxState(
                                    positionalThreshold = { totalDistance -> totalDistance }
                                )

                            val dismissColor by animateColorAsState(
                                when (dismissBoxState.targetValue) {
                                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                                    SwipeToDismissBoxValue.StartToEnd -> Color.Green.copy(alpha = 0.6f)
                                    SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.6f)
                                },
                                label = "dismissColor"
                            )

                            val isFirst = index == 0
                            val isLast = index == mutableQueueWindows.size - 1
                            val isActive = index == currentWindowIndex

                            val cornerRadius = 24.dp
                            val topShape = remember(cornerRadius) {
                                AbsoluteSmoothCornerShape(
                                    cornerRadiusTR = cornerRadius,
                                    smoothnessAsPercentBR = 0,
                                    cornerRadiusBR = 0.dp,
                                    smoothnessAsPercentTL = 60,
                                    cornerRadiusTL = cornerRadius,
                                    smoothnessAsPercentBL = 0,
                                    cornerRadiusBL = 0.dp,
                                    smoothnessAsPercentTR = 60
                                )
                            }
                            val middleShape = RectangleShape
                            val bottomShape = remember(cornerRadius) {
                                AbsoluteSmoothCornerShape(
                                    cornerRadiusTR = 0.dp,
                                    smoothnessAsPercentBR = 60,
                                    cornerRadiusBR = cornerRadius,
                                    smoothnessAsPercentTL = 0,
                                    cornerRadiusTL = 0.dp,
                                    smoothnessAsPercentBL = 60,
                                    cornerRadiusBL = cornerRadius,
                                    smoothnessAsPercentTR = 0
                                )
                            }
                            val singleShape = remember(cornerRadius) {
                                AbsoluteSmoothCornerShape(
                                    cornerRadiusTR = cornerRadius,
                                    smoothnessAsPercentBR = 60,
                                    cornerRadiusBR = cornerRadius,
                                    smoothnessAsPercentTL = 60,
                                    cornerRadiusTL = cornerRadius,
                                    smoothnessAsPercentBL = 60,
                                    cornerRadiusBL = cornerRadius,
                                    smoothnessAsPercentTR = 60
                                )
                            }

                            val shape = remember(isFirst, isLast) {
                                when {
                                    isFirst && isLast -> singleShape
                                    isFirst -> topShape
                                    isLast -> bottomShape
                                    else -> middleShape
                                }
                            }

                            var processedDismiss by remember { mutableStateOf(false) }
                            LaunchedEffect(dismissBoxState.currentValue) {
                                val dv = dismissBoxState.currentValue
                                if (!processedDismiss &&
                                    (
                                        dv == SwipeToDismissBoxValue.StartToEnd ||
                                            dv == SwipeToDismissBoxValue.EndToStart
                                        )
                                ) {
                                    processedDismiss = true
                                    if (dv == SwipeToDismissBoxValue.EndToStart) {
                                        playerConnection.player.removeMediaItem(currentItem.firstPeriodIndex)
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
                                                playerConnection.player.moveMediaItem(
                                                    mutableQueueWindows.size,
                                                    currentItem.firstPeriodIndex
                                                )
                                            }
                                        }
                                    } else if (dv == SwipeToDismissBoxValue.StartToEnd) {
                                        val targetIndex = (currentWindowIndex + 1).coerceAtMost(
                                            mutableQueueWindows.lastIndex
                                        )
                                        if (currentItem.firstPeriodIndex != targetIndex) {
                                            playerConnection.player.moveMediaItem(
                                                currentItem.firstPeriodIndex,
                                                targetIndex
                                            )
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = context.getString(R.string.added_to_play_next),
                                                    duration = SnackbarDuration.Short
                                                )
                                                dismissBoxState.reset()
                                                processedDismiss = false
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                dismissBoxState.reset()
                                                processedDismiss = false
                                            }
                                        }
                                    }
                                }
                                if (dv == SwipeToDismissBoxValue.Settled) {
                                    processedDismiss = false
                                }
                            }

                            val content: @Composable () -> Unit = {
                                MediaMetadataListItem(
                                    mediaMetadata = window.mediaItem.metadata!!,
                                    isSelected = selection && window.mediaItem.metadata!! in selectedSongs,
                                    isActive = isActive,
                                    isPlaying = isPlaying,
                                    inSelectionMode = selection,
                                    drawHighlight = false,
                                    onSelectionChange = { isChecked ->
                                        if (isChecked) {
                                            selectedSongs.add(window.mediaItem.metadata!!)
                                            selectedItems.add(currentItem)
                                        } else {
                                            selectedSongs.remove(window.mediaItem.metadata!!)
                                            selectedItems.remove(currentItem)
                                        }
                                    },
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    PlayerMenu(
                                                        mediaMetadata = window.mediaItem.metadata!!,
                                                        navController = navController,
                                                        playerBottomSheetState = playerBottomSheetState,
                                                        isQueueTrigger = true,
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
                                                contentDescription = null
                                            )
                                        }
                                        if (!locked) {
                                            IconButton(
                                                onClick = { },
                                                modifier = Modifier.draggableHandle()
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.drag_handle),
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    },
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                if (selection) {
                                                    if (window.mediaItem.metadata!! in selectedSongs) {
                                                        selectedSongs.remove(window.mediaItem.metadata!!)
                                                        selectedItems.remove(currentItem)
                                                    } else {
                                                        selectedSongs.add(window.mediaItem.metadata!!)
                                                        selectedItems.add(currentItem)
                                                    }
                                                } else {
                                                    if (index == currentWindowIndex) {
                                                        playerConnection.player.togglePlayPause()
                                                    } else {
                                                        playerConnection.player.seekToDefaultPosition(
                                                            window.firstPeriodIndex
                                                        )
                                                        playerConnection.player.playWhenReady =
                                                            true
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(
                                                    HapticFeedbackType.LongPress
                                                )
                                                if (!selection) {
                                                    selection = true
                                                }
                                                selectedSongs.clear() // Clear all selections
                                                selectedSongs.add(window.mediaItem.metadata!!) // Select current item
                                            }
                                        )
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .animateItem()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(ListItemHeight)
                                        .clip(shape)
                                        .background(
                                            if (isActive) {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainer
                                            }
                                        )
                                ) {
                                    if (locked) {
                                        content()
                                    } else {
                                        SwipeToDismissBox(
                                            state = dismissBoxState,
                                            backgroundContent = {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(dismissColor)
                                                        .padding(horizontal = 24.dp),
                                                    contentAlignment = if (dismissBoxState.targetValue ==
                                                        SwipeToDismissBoxValue.StartToEnd
                                                    ) {
                                                        Alignment.CenterStart
                                                    } else {
                                                        Alignment.CenterEnd
                                                    }
                                                ) {
                                                    val icon = if (dismissBoxState.targetValue ==
                                                        SwipeToDismissBoxValue.StartToEnd
                                                    ) {
                                                        painterResource(R.drawable.playlist_play)
                                                    } else {
                                                        painterResource(R.drawable.delete)
                                                    }

                                                    if (dismissBoxState.targetValue != SwipeToDismissBoxValue.Settled) {
                                                        Icon(
                                                            painter = icon,
                                                            contentDescription = null,
                                                            tint = Color.White
                                                        )
                                                    }
                                                }
                                            }
                                        ) {
                                            content()
                                        }
                                    }
                                }
                                if (!isLast) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                }
                            }
                        }
                    }

                    if (automix.isNotEmpty() && !locked) {
                        item(key = "suggestions_top_spacer") {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        item(key = "automix_header") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 8.dp)
                                    .animateItem()
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.similar),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.similar_content),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = stringResource(R.string.smart_suggestions),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        itemsIndexed(
                            items = automix,
                            key = { _, it -> it.mediaId }
                        ) { index, item ->
                            val isFirst = index == 0
                            val isLast = index == automix.size - 1
                            val cornerRadius = 24.dp
                            val topShape = remember(cornerRadius) {
                                AbsoluteSmoothCornerShape(
                                    cornerRadiusTR = cornerRadius,
                                    smoothnessAsPercentBR = 0,
                                    cornerRadiusBR = 0.dp,
                                    smoothnessAsPercentTL = 60,
                                    cornerRadiusTL = cornerRadius,
                                    smoothnessAsPercentBL = 0,
                                    cornerRadiusBL = 0.dp,
                                    smoothnessAsPercentTR = 60
                                )
                            }
                            val middleShape = RectangleShape
                            val bottomShape = remember(cornerRadius) {
                                AbsoluteSmoothCornerShape(
                                    cornerRadiusTR = 0.dp,
                                    smoothnessAsPercentBR = 60,
                                    cornerRadiusBR = cornerRadius,
                                    smoothnessAsPercentTL = 0,
                                    cornerRadiusTL = 0.dp,
                                    smoothnessAsPercentBL = 60,
                                    cornerRadiusBL = cornerRadius,
                                    smoothnessAsPercentTR = 0
                                )
                            }
                            val singleShape = remember(cornerRadius) {
                                AbsoluteSmoothCornerShape(
                                    cornerRadiusTR = cornerRadius,
                                    smoothnessAsPercentBR = 60,
                                    cornerRadiusBR = cornerRadius,
                                    smoothnessAsPercentTL = 60,
                                    cornerRadiusTL = cornerRadius,
                                    smoothnessAsPercentBL = 60,
                                    cornerRadiusBL = cornerRadius,
                                    smoothnessAsPercentTR = 60
                                )
                            }

                            val shape = remember(isFirst, isLast) {
                                when {
                                    isFirst && isLast -> singleShape
                                    isFirst -> topShape
                                    isLast -> bottomShape
                                    else -> middleShape
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .animateItem()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(ListItemHeight)
                                        .clip(shape)
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    MediaMetadataListItem(
                                        mediaMetadata = item.metadata!!,
                                        drawHighlight = false,
                                        trailingContent = {
                                            val playNextDescription = stringResource(R.string.play_next)
                                            TooltipBox(
                                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                                    TooltipAnchorPosition.Above
                                                ),
                                                tooltip = { PlainTooltip { Text(playNextDescription) } },
                                                state = rememberTooltipState()
                                            ) {
                                                FilledTonalIconButton(
                                                    onClick = {
                                                        playerConnection.service.playNextAutomix(
                                                            item,
                                                            index
                                                        )
                                                    },
                                                    shapes = IconButtonDefaults.shapes()
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.playlist_play),
                                                        contentDescription = playNextDescription
                                                    )
                                                }
                                            }

                                            val addToQueueDescription = stringResource(R.string.add_to_queue)
                                            TooltipBox(
                                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                                    TooltipAnchorPosition.Above
                                                ),
                                                tooltip = { PlainTooltip { Text(addToQueueDescription) } },
                                                state = rememberTooltipState()
                                            ) {
                                                FilledTonalIconButton(
                                                    onClick = {
                                                        playerConnection.service.addToQueueAutomix(
                                                            item,
                                                            index
                                                        )
                                                    },
                                                    shapes = IconButtonDefaults.shapes()
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.queue_music),
                                                        contentDescription = addToQueueDescription
                                                    )
                                                }
                                            }
                                        },
                                        modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    playerConnection.service.playNextAutomix(
                                                        item,
                                                        index
                                                    )
                                                },
                                                onLongClick = {
                                                    menuState.show {
                                                        PlayerMenu(
                                                            mediaMetadata = item.metadata!!,
                                                            navController = navController,
                                                            playerBottomSheetState = playerBottomSheetState,
                                                            isQueueTrigger = true,
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
                                if (!isLast) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

//        Box(
//            modifier =
//            Modifier
//                .background(Color.Transparent)
//                .fillMaxWidth()
//                .height(
//                    ListItemHeight +
//                            WindowInsets.systemBars
//                                .asPaddingValues()
//                                .calculateBottomPadding(),
//                )
//                .align(Alignment.BottomCenter)
//                .clickable {
//                    state.collapseSoft()
//                }
//                .windowInsetsPadding(
//                    WindowInsets.systemBars
//                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
//                )
//                .padding(12.dp),
//        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
            Modifier
                .padding(
                    bottom =
                    ListItemHeight +
                        WindowInsets.systemBars
                            .asPaddingValues()
                            .calculateBottomPadding()
                )
                .align(Alignment.BottomCenter)
        )
    }
}
