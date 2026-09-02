package com.music.vivi.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
import com.music.vivi.constants.QueueEditLockKey
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.extensions.move
import com.music.vivi.extensions.toggleRepeatMode
import com.music.vivi.ui.component.MediaMetadataListItem
import com.music.vivi.utils.listItemShape
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import com.music.vivi.extensions.metadata
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.LocalBottomSheetPageState
import com.music.vivi.ui.menu.QueueMenu
import com.music.vivi.ui.utils.ShowMediaInfo
import com.music.vivi.ui.component.BottomSheetState
import androidx.navigation.NavController
import com.music.vivi.ui.component.ActionPromptDialog
import com.music.vivi.utils.makeTimeString
import kotlin.math.roundToInt
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.alpha
import com.music.vivi.LocalListenTogetherManager
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QueueV2(
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    modifier: Modifier = Modifier,
    onControlsVisibilityChange: (Boolean) -> Unit = {}
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    
    val queueWindows by playerConnection.queueWindows.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()
    val radioChips by playerConnection.radioChips.collectAsState()
    
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsState(initial = com.music.vivi.listentogether.RoomRole.NONE)
    val isGuest = listenTogetherRoleState?.value == com.music.vivi.listentogether.RoomRole.GUEST
    
    var locked by rememberPreference(QueueEditLockKey, true)
    
    // Automatically lock queue for guests
    val isQueueEffectivelyLocked = locked || isGuest

    // Sleep Timer
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

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.GRADIENT
    )
    val adaptivePrimary = if (playerBackground == PlayerBackgroundStyle.DEFAULT) MaterialTheme.colorScheme.onSurface else Color.White
    val adaptiveSecondary = if (playerBackground == PlayerBackgroundStyle.DEFAULT) MaterialTheme.colorScheme.onSurfaceVariant else Color.White.copy(alpha = 0.7f)
    val adaptiveSurface = if (playerBackground == PlayerBackgroundStyle.DEFAULT) MaterialTheme.colorScheme.surfaceVariant else Color.White.copy(alpha = 0.2f)

    val lazyListState = rememberLazyListState()
    
    // Scroll direction tracking
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(lazyListState) {
        snapshotFlow { 
            Triple(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset, lazyListState.isScrollInProgress)
        }.collect { (index, offset, isScrollInProgress) ->
            // If the user reaches the very top of the queue, always bring the controls back
            if (index == 0 && offset < 50) {
                onControlsVisibilityChange(true)
            } else if (isScrollInProgress) {
                if (index != previousIndex) {
                    if (index > previousIndex) {
                        onControlsVisibilityChange(false)
                    } else {
                        onControlsVisibilityChange(true)
                    }
                } else {
                    val delta = offset - previousScrollOffset
                    if (delta > 15) {
                        onControlsVisibilityChange(false)
                    } else if (delta < -15) {
                        onControlsVisibilityChange(true)
                    }
                }
            }
            previousIndex = index
            previousScrollOffset = offset
        }
    }

    val mutableQueueWindows = remember { mutableStateListOf<Timeline.Window>() }
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    
    val currentPlayingUid = remember(currentWindowIndex, queueWindows) {
        if (currentWindowIndex in queueWindows.indices) {
            queueWindows[currentWindowIndex].uid
        } else null
    }

    LaunchedEffect(queueWindows) {
        mutableQueueWindows.apply {
            clear()
            addAll(queueWindows)
        }
    }
    
    val headerItems = 0

    LaunchedEffect(mutableQueueWindows.size, currentWindowIndex) {
        if (currentWindowIndex in mutableQueueWindows.indices) {
            lazyListState.scrollToItem(currentWindowIndex)
        }
    }

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState
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

    CompositionLocalProvider(LocalContentColor provides adaptivePrimary) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Fixed Top Control Pills
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val pillShape = RoundedCornerShape(16.dp)
            val activeColor = adaptivePrimary.copy(alpha = 0.25f)
            val inactiveColor = adaptivePrimary.copy(alpha = 0.1f)
            
            // Shuffle
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(if (shuffleModeEnabled) activeColor else inactiveColor, pillShape)
                    .clip(pillShape)
                    .clickable(enabled = !isGuest) { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.shuffle), contentDescription = "Shuffle", tint = adaptivePrimary, modifier = Modifier.size(24.dp))
            }
            // Repeat
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(if (repeatMode != Player.REPEAT_MODE_OFF) activeColor else inactiveColor, pillShape)
                    .clip(pillShape)
                    .clickable(enabled = !isGuest) { playerConnection.player.toggleRepeatMode() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                            else -> R.drawable.repeat
                        }
                    ),
                    contentDescription = "Repeat",
                    tint = adaptivePrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            // Timer
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(if (sleepTimerEnabled) activeColor else inactiveColor, pillShape)
                    .clip(pillShape)
                    .clickable(enabled = !isGuest) {
                        if (sleepTimerEnabled) {
                            playerConnection.service.sleepTimer.clear()
                        } else {
                            showSleepTimerDialog = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.bedtime), contentDescription = "Sleep Timer", tint = adaptivePrimary, modifier = Modifier.size(24.dp))
                    if (sleepTimerEnabled) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = makeTimeString(sleepTimerTimeLeft.coerceAtLeast(0L)),
                            style = MaterialTheme.typography.labelSmall,
                            color = adaptivePrimary
                        )
                    }
                }
            }
        }

        // Queue Header Row
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (queueTitle != null) {
                        Text(
                            text = stringResource(R.string.playing_from),
                            style = MaterialTheme.typography.labelMedium,
                            color = adaptiveSecondary
                        )
                        Text(
                            text = queueTitle!!,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = adaptivePrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.queue),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = adaptivePrimary
                        )
                    }
                }
                if (!isGuest) {
                    IconButton(onClick = { locked = !locked }) {
                        Icon(
                            painter = painterResource(if (locked) R.drawable.lock else R.drawable.lock_open),
                            contentDescription = if (locked) "Unlock Queue" else "Lock Queue",
                            tint = adaptiveSecondary
                        )
                    }
                }
            }
            
            if (radioChips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(radioChips) { chip ->
                        val chipBg = if (chip.isSelected) adaptivePrimary else adaptivePrimary.copy(alpha = 0.1f)
                        val chipTextCol = if (chip.isSelected) {
                            if (playerBackground == PlayerBackgroundStyle.DEFAULT) MaterialTheme.colorScheme.surface else Color.Black
                        } else {
                            adaptivePrimary
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBg)
                                .clickable(enabled = !isGuest && !chip.isSelected) {
                                    playerConnection.selectRadioChip(chip)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = chip.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = chipTextCol,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Queue List
            itemsIndexed(
                items = mutableQueueWindows,
                key = { _, item -> item.uid.hashCode() }
            ) { index, window ->
                ReorderableItem(
                    state = reorderableState,
                    key = window.uid.hashCode()
                ) {
                    val isActive = window.uid == currentPlayingUid

                    @OptIn(ExperimentalMaterial3Api::class)
                    val dismissBoxState = rememberSwipeToDismissBoxState(
                        positionalThreshold = { totalDistance -> totalDistance }
                    )
                    var processedDismiss by remember { mutableStateOf(false) }

                    LaunchedEffect(dismissBoxState.currentValue) {
                        val dv = dismissBoxState.currentValue
                        if (!processedDismiss && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) {
                            processedDismiss = true
                            playerConnection.player.removeMediaItem(window.firstPeriodIndex)
                        }
                        if (dv == SwipeToDismissBoxValue.Settled) {
                            processedDismiss = false
                        }
                    }

                    val content: @Composable () -> Unit = {
                        MediaMetadataListItem(
                            mediaMetadata = window.mediaItem.metadata!!,
                            isSelected = false,
                            isActive = isActive,
                            isPlaying = isPlaying && isActive,
                            backgroundColor = Color.Transparent,
                            subtitleColor = adaptiveSecondary,
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                            contentDescription = "Options"
                                        )
                                    }
                                    
                                    if (!isQueueEffectivelyLocked) {
                                        IconButton(
                                            onClick = { },
                                            modifier = Modifier.draggableHandle()
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.drag_handle),
                                                contentDescription = "Drag to reorder"
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = !isGuest) {
                                                    playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                                                    playerConnection.player.playWhenReady = true
                                                }
                        )
                    }

                    if (isQueueEffectivelyLocked) {
                        content()
                    } else {
                        @OptIn(ExperimentalMaterial3Api::class)
                        SwipeToDismissBox(
                            state = dismissBoxState,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    targetValue = when (dismissBoxState.targetValue) {
                                        SwipeToDismissBoxValue.Settled -> Color.Transparent
                                        else -> MaterialTheme.colorScheme.error
                                    }, label = ""
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 4.dp, horizontal = 16.dp)
                                        .background(color),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    val iconAlpha by animateFloatAsState(
                                        targetValue = if (dismissBoxState.targetValue != SwipeToDismissBoxValue.Settled) 1f else 0f,
                                        label = "iconAlpha"
                                    )
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        modifier = Modifier
                                            .padding(end = 16.dp)
                                            .alpha(iconAlpha),
                                        tint = MaterialTheme.colorScheme.onError
                                    )
                                }
                            },
                            content = { content() },
                            enableDismissFromStartToEnd = false
                        )
                    }
                }
            }
            // end of queue
            } // end Column
    } // end CompositionLocalProvider
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
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
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
            onCancel = { showSleepTimerDialog = false },
            onReset = { sleepTimerValue = 30f },
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
