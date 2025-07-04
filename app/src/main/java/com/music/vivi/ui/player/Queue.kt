package com.music.vivi.ui.player

import android.annotation.SuppressLint
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.constants.LockQueueKey
import com.music.vivi.constants.PlayerStyle
import com.music.vivi.constants.PlayerStyleKey
import com.music.vivi.constants.ShowLyricsKey
import com.music.vivi.constants.SliderStyle
import com.music.vivi.constants.SliderStyleKey
import com.music.vivi.extensions.metadata
import com.music.vivi.extensions.move
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.ui.component.BottomSheet
import com.music.vivi.ui.component.BottomSheetState
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.MediaMetadataListItem
import com.music.vivi.ui.component.PlayerSliderTrack
import com.music.vivi.ui.menu.MediaMetadataMenu
import com.music.vivi.ui.menu.QueueSelectionMenu
import com.music.vivi.utils.joinByBullet
import com.music.vivi.utils.makeTimeString
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.saket.squiggles.SquigglySlider
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt

//new import
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.Button
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.Divider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp


@SuppressLint("AutoboxingStateCreation")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class, ExperimentalAnimationApi::class
)
@Composable
fun Queue(
    state: BottomSheetState,
    backgroundColor: Color,
    navController: NavController,
    modifier: Modifier = Modifier,
    onBackgroundColor: Color,
) {
    val haptic = LocalHapticFeedback.current
    val menuState = LocalMenuState.current

    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()

    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    var lockQueue by rememberPreference(LockQueueKey, defaultValue = false)

    var inSelectMode by remember {
        mutableStateOf(false)
    }

    //sleep timer
    val coroutineScope = rememberCoroutineScope()
    val sleepTimerSheetState = rememberModalBottomSheetState()
    val swipeableState = rememberSwipeableState(initialValue = 0f)


    val selection = remember { mutableStateListOf<Int>() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    var showDetailsDialog by remember { mutableStateOf(false) }
    if (showDetailsDialog) {
        DetailsDialog(
            onDismiss = { showDetailsDialog = false }
        )
    }

    var showLyrics by rememberPreference(ShowLyricsKey, false)

    val (playerStyle) = rememberEnumPreference (PlayerStyleKey , defaultValue = PlayerStyle.NEW)

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerValue by remember { mutableStateOf(30f) }
    val sleepTimerEnabled = remember(playerConnection.service.sleepTimer.triggerTime, playerConnection.service.sleepTimer.pauseWhenSongEnd) {
        playerConnection.service.sleepTimer.isActive
    }
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.SQUIGGLY)

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

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

    //new code
    if (showSleepTimerDialog) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    sleepTimerSheetState.hide()
                    delay(100)
                    showSleepTimerDialog = false
                }
            },
            sheetState = sleepTimerSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
                    .imePadding()
                    .animateContentSize()
                    .swipeable(
                        state = swipeableState,
                        anchors = mapOf(
                            0f to 0f,
                            1f to 1f
                        ),
                        thresholds = { _, _ -> FractionalThreshold(0.5f) },
                        orientation = Orientation.Vertical
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sleep_timer),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                sleepTimerSheetState.hide()
                                delay(100)
                                showSleepTimerDialog = false
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Animated time display
                AnimatedContent(
                    targetState = sleepTimerValue.roundToInt(),
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInVertically { height -> height } + fadeIn() with
                                    slideOutVertically { height -> -height } + fadeOut()
                        } else {
                            slideInVertically { height -> -height } + fadeIn() with
                                    slideOutVertically { height -> height } + fadeOut()
                        }.using(SizeTransform(clip = false))
                    }
                ) { targetCount ->
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold)) {
                                append(targetCount.toString())
                            }
                            append(" ")
                            withStyle(style = SpanStyle(fontSize = 16.sp)) {
                                append(pluralStringResource(
                                    R.plurals.minute,
                                    targetCount,
                                    targetCount
                                ).replace(targetCount.toString(), "").trim())
                            }
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                when (sliderStyle) {
                    SliderStyle.SQUIGGLY -> {
                        SquigglySlider(
                            value = sleepTimerValue,
                            onValueChange = { sleepTimerValue = it },
                            valueRange = 5f..120f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    SliderStyle.COMPOSE -> {
                        // Enhanced slider with animation
                        var sliderPosition by remember { mutableFloatStateOf(sleepTimerValue) }
                        val animatedSliderValue by animateFloatAsState(
                            targetValue = sliderPosition,
                            animationSpec = spring(
                                dampingRatio = 0.5f,
                                stiffness = 100f
                            ),
                            label = "sliderAnimation"
                        )

                        LaunchedEffect(sleepTimerValue) {
                            sliderPosition = sleepTimerValue
                        }

                        Slider(
                            value = animatedSliderValue,
                            onValueChange = {
                                sleepTimerValue = it
                                sliderPosition = it
                            },
                            valueRange = 5f..120f,
                            steps = (120 - 5) / 5 - 1,
                            modifier = Modifier.fillMaxWidth(),
                            interactionSource = remember { MutableInteractionSource() }.also { interactionSource ->
                                LaunchedEffect(interactionSource) {
                                    interactionSource.interactions.collect {
                                        if (it is PressInteraction.Release) {
                                            // Snap to nearest 5-minute interval
                                            sleepTimerValue = (sleepTimerValue.roundToInt() / 5 * 5).toFloat()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                // Time markers under the slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("5m", style = MaterialTheme.typography.labelSmall)
                    Text("30m", style = MaterialTheme.typography.labelSmall)
                    Text("60m", style = MaterialTheme.typography.labelSmall)
                    Text("90m", style = MaterialTheme.typography.labelSmall)
                    Text("120m", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
                            sleepTimerSheetState.hide()
                            delay(100)
                            showSleepTimerDialog = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = sleepTimerValue.roundToInt() > 0
                ) {
                    Text("Set timer for ${sleepTimerValue.roundToInt()} min")
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            playerConnection.service.sleepTimer.start(-1)
                            sleepTimerSheetState.hide()
                            delay(100)
                            showSleepTimerDialog = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.end_of_song))
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }


    //end of new code

    BottomSheet(
        state = state,
        backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(NavigationBarDefaults.Elevation),
        modifier = modifier,
        collapsedContent = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.systemBars
                            .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                        )
            ) {
                if (playerStyle == PlayerStyle.NEW) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 30.dp)
                            .windowInsetsPadding(
                                WindowInsets.systemBars
                                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                            ),
                    ) {
                        TextButton(onClick = { state.expandSoft() }) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.queue_icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = onBackgroundColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.queue),
                                    color = onBackgroundColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .sizeIn(maxWidth = 80.dp)
                                        .basicMarquee()
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
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.bedtime),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = onBackgroundColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                if (sleepTimerEnabled) {
                                    Text(
                                        text = makeTimeString(sleepTimerTimeLeft),
                                        color = onBackgroundColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .sizeIn(maxWidth = 80.dp)
                                            .basicMarquee()
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.sleep_timer),
                                        color = onBackgroundColor,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .sizeIn(maxWidth = 80.dp)
                                            .basicMarquee()
                                    )
                                }
                            }
                        }

                        TextButton(onClick = { showLyrics = !showLyrics }) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.lyrics),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = onBackgroundColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.lyrics),
                                    color = onBackgroundColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .sizeIn(maxWidth = 80.dp)
                                        .basicMarquee()
                                )
                            }
                        }
                    }
                } else {
                    IconButton(onClick = { state.expandSoft() }) {
                        Icon(
                            painter = painterResource(R.drawable.expand_less),
                            tint = onBackgroundColor,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    ) {
        val queueTitle by playerConnection.queueTitle.collectAsState()
        val queueWindows by playerConnection.queueWindows.collectAsState()
        val mutableQueueWindows = remember { mutableStateListOf<Timeline.Window>() }
        val queueLength = remember(queueWindows) {
            queueWindows.sumOf { it.mediaItem.metadata!!.duration }
        }

        val coroutineScope = rememberCoroutineScope()
        val lazyListState = rememberLazyListState()
        var dragInfo by remember {
            mutableStateOf<Pair<Int, Int>?>(null)
        }
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

            mutableQueueWindows.move(from.index, to.index)
        }

        LaunchedEffect(reorderableState.isAnyItemDragging) {
            if (!reorderableState.isAnyItemDragging) {
                dragInfo?.let { (from, to) ->
                    if (!playerConnection.player.shuffleModeEnabled) {
                        playerConnection.player.moveMediaItem(from, to)
                    } else {
                        playerConnection.player.setShuffleOrder(
                            DefaultShuffleOrder(
                                queueWindows.map { it.firstPeriodIndex }.toMutableList().move(from, to).toIntArray(),
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
            selection.fastForEachReversed { uidHash ->
                if (queueWindows.find { it.uid.hashCode() == uidHash } == null) {
                    selection.remove(uidHash)
                }
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
                .background(backgroundColor),
        ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = WindowInsets.systemBars
                .add(
                    WindowInsets(
                        top = ListItemHeight,
                        bottom = ListItemHeight
                    )
                )
                .asPaddingValues(),
            modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection)
        ) {
            itemsIndexed(
                items = mutableQueueWindows,
                key = { _, item -> item.uid.hashCode() }
            ) { index, window ->
                ReorderableItem(
                    state = reorderableState,
                    key = window.uid.hashCode()
                ) {
                    val currentItem by rememberUpdatedState(window)
                    val dismissState = rememberSwipeToDismissBoxState(
                        positionalThreshold = { totalDistance -> totalDistance },
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.StartToEnd || dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                playerConnection.player.removeMediaItem(currentItem.firstPeriodIndex)
                            }
                            true
                        }
                    )

                    val onCheckedChange: (Boolean) -> Unit = {
                        if (it) {
                            selection.add(window.uid.hashCode())
                        } else {
                            selection.remove(window.uid.hashCode())
                        }
                    }

                    val content = @Composable {
                        MediaMetadataListItem(
                            mediaMetadata = window.mediaItem.metadata!!,
                            isActive = index == currentWindowIndex,
                            isPlaying = isPlaying,
                            trailingContent = {
                                if (inSelectMode) {
                                    if (!window.mediaItem.metadata!!.isLocal) {
                                        Checkbox(
                                            checked = window.uid.hashCode() in selection,
                                            onCheckedChange = onCheckedChange
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                MediaMetadataMenu(
                                                    mediaMetadata = window.mediaItem.metadata!!,
                                                    navController = navController,
                                                    bottomSheetState = state,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null
                                        )
                                    }

                                    if (!lockQueue) {
                                        IconButton(
                                            onClick = { },
                                            modifier = Modifier.draggableHandle()
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.twolines_icon),
                                                contentDescription = null
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
                                            if (!window.mediaItem.metadata!!.isLocal) {
                                                onCheckedChange(window.uid.hashCode() !in selection)
                                            }
                                        } else {
                                            coroutineScope.launch(Dispatchers.Main) {
                                                if (index == currentWindowIndex) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.player.seekToDefaultPosition(
                                                        window.firstPeriodIndex
                                                    )
                                                    playerConnection.player.playWhenReady = true
                                                }
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        if (!inSelectMode) {
                                            if (!window.mediaItem.metadata!!.isLocal) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                inSelectMode = true
                                                onCheckedChange(true)
                                            }
                                        }
                                        else {
                                            if (!window.mediaItem.metadata!!.isLocal) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    MediaMetadataMenu(
                                                        mediaMetadata = window.mediaItem.metadata!!,
                                                        navController = navController,
                                                        bottomSheetState = state,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                        )
                    }

                    if (!lockQueue && !inSelectMode) {
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {},
                            content = { content() }
                        )
                    } else {
                        content()
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f)
                )
                .windowInsetsPadding(
                    WindowInsets.systemBars
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(ListItemHeight)
                    .padding(horizontal = 6.dp)
            ) {
                if (inSelectMode) {
                    IconButton(onClick = onExitSelectionMode) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                        )
                    }
                    Text(
                        text = pluralStringResource(R.plurals.n_selected, selection.size, selection.size),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = queueWindows.size == selection.size,
                        onCheckedChange = {
                            if (queueWindows.size == selection.size) {
                                selection.clear()
                            } else {
                                selection.clear()
                                selection.addAll(
                                    queueWindows
                                        .filter { !it.mediaItem.metadata!!.isLocal }
                                        .map { it.uid.hashCode() }
                                )
                            }
                        }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .weight(1f)
                    ) {
                        if (!queueTitle.isNullOrEmpty()) {
                            Text(
                                text = queueTitle.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                        Text(
                            text = joinByBullet(pluralStringResource(R.plurals.n_song, queueWindows.size, queueWindows.size), makeTimeString(queueLength * 1000L)),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()

        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f))
                .fillMaxWidth()
                .height(
                    ListItemHeight +
                            WindowInsets.systemBars
                                .asPaddingValues()
                                .calculateBottomPadding()
                )
                .align(Alignment.BottomCenter)
                .clickable {
                    onExitSelectionMode()
                    state.collapseSoft()
                }
                .windowInsetsPadding(
                    WindowInsets.systemBars
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
                .padding(12.dp)
        ) {
            IconButton(
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = {
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(
                            if (playerConnection.player.shuffleModeEnabled) playerConnection.player.currentMediaItemIndex else 0
                        )
                    }.invokeOnCompletion {
                        playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = null,
                    modifier = Modifier.alpha(if (shuffleModeEnabled) 1f else 0.5f)
                )
            }

            Icon(
                painter = painterResource(R.drawable.expand_more),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center)
            )

            if (inSelectMode) {
                IconButton(
                    enabled = selection.size > 0,
                    onClick = {
                        menuState.show {
                            QueueSelectionMenu(
                                selection = selection.mapNotNull { uidHash ->
                                    mutableQueueWindows.find { it.uid.hashCode() == uidHash }
                                },
                                onExitSelectionMode = onExitSelectionMode,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null,
                    )
                }
            } else {
                IconButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = {
                        lockQueue = !lockQueue
                    }
                ) {
                    Icon(
                        painter = if (lockQueue) painterResource(R.drawable.lockon_icon) else painterResource(R.drawable.lockoff_icon),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.info_icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.details),
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.ok))
                }
            }

            Spacer(Modifier.height(16.dp))

            listOf(
                stringResource(R.string.song_title) to mediaMetadata?.title,
                stringResource(R.string.song_artists) to mediaMetadata?.artists?.joinToString { it.name },
                stringResource(R.string.media_id) to mediaMetadata?.id,
                "I tag" to currentFormat?.itag?.toString(),
                stringResource(R.string.mime_type) to currentFormat?.mimeType,
                stringResource(R.string.codecs) to currentFormat?.codecs,
                stringResource(R.string.bitrate) to currentFormat?.bitrate?.let { "${it / 1000} Kbps" },
                stringResource(R.string.sample_rate) to currentFormat?.sampleRate?.let { "$it Hz" },
                stringResource(R.string.loudness) to currentFormat?.loudnessDb?.let { "$it dB" },
                stringResource(R.string.volume) to "${(playerConnection.player.volume * 100).toInt()}%",
                stringResource(R.string.file_size) to currentFormat?.contentLength?.let { Formatter.formatShortFileSize(context, it) }
            ).forEach { (label, text) ->
                val displayText = text ?: stringResource(R.string.unknown)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current,
                            onClick = {
                                clipboardManager.setText(AnnotatedString(displayText))
                                Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                            }
                        )
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}