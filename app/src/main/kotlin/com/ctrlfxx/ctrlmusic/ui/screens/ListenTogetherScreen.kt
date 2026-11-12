/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.ctrlfxx.ctrlmusic.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ctrlfxx.ctrlmusic.LocalListenTogetherManager
import com.ctrlfxx.ctrlmusic.LocalPlayerAwareWindowInsets
import com.ctrlfxx.ctrlmusic.R
import com.ctrlfxx.ctrlmusic.constants.AppBarHeight
import com.ctrlfxx.ctrlmusic.constants.ListenTogetherAvatarIndexKey
import com.ctrlfxx.ctrlmusic.constants.ListenTogetherInTopBarKey
import com.ctrlfxx.ctrlmusic.constants.ListenTogetherUsernameKey
import com.ctrlfxx.ctrlmusic.listentogether.ConnectionState
import com.ctrlfxx.ctrlmusic.listentogether.JoinRequestPayload
import com.ctrlfxx.ctrlmusic.listentogether.ListenTogetherEvent
import com.ctrlfxx.ctrlmusic.listentogether.SuggestionReceivedPayload
import com.ctrlfxx.ctrlmusic.listentogether.UserInfo
import com.ctrlfxx.ctrlmusic.ui.component.DefaultDialog
import com.ctrlfxx.ctrlmusic.ui.component.ExpressiveSettingGroup
import com.ctrlfxx.ctrlmusic.ui.component.IconButton
import com.ctrlfxx.ctrlmusic.ui.component.Material3SettingsItem
import com.ctrlfxx.ctrlmusic.ui.utils.backToMain
import com.ctrlfxx.ctrlmusic.utils.rememberPreference
import com.ctrlfxx.ctrlmusic.viewmodels.ListenTogetherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Shared design tokens (Android 16 / M3 Expressive grouped shapes)
// ---------------------------------------------------------------------------

private val OuterRadius = 10.dp
private val InnerRadius = 4.dp
private val GroupGap = 4.dp
private val SectionGap = 10.dp
private val StatusSectionHeight = 190.dp
private const val ROOM_CODE_LENGTH = 8
private const val COLLAPSED_USER_COUNT = 3
private val SheetCorner = 40.dp
private val BigOuter = 28.dp
private val BigInner = 8.dp

val avatarOptions = listOf(
    R.drawable.person,
    R.drawable.man,
    R.drawable.woman,
    R.drawable.man_1,
    R.drawable.man_2,
    R.drawable.man_3,
    R.drawable.man_4,
    R.drawable.man_5,
    R.drawable.man_6,
    R.drawable.woman_1,
    R.drawable.woman_2,
    R.drawable.woman_3,
    R.drawable.woman_4,
    R.drawable.luxury_women
)

/** Big outer radius on the "outside" corners, small radius where tiles touch. */
private fun corners(
    topStart: Boolean,
    topEnd: Boolean,
    bottomStart: Boolean,
    bottomEnd: Boolean
) = RoundedCornerShape(
    topStart = if (topStart) OuterRadius else InnerRadius,
    topEnd = if (topEnd) OuterRadius else InnerRadius,
    bottomStart = if (bottomStart) OuterRadius else InnerRadius,
    bottomEnd = if (bottomEnd) OuterRadius else InnerRadius
)

private fun verticalShape(isTop: Boolean, isBottom: Boolean) =
    corners(isTop, isTop, isBottom, isBottom)

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ListenTogetherScreen(
    navController: NavController,
    showTopBar: Boolean = false,
    viewModel: ListenTogetherViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val windowInsets = LocalPlayerAwareWindowInsets.current

    if (listenTogetherManager == null) {
        NotConfiguredContent()
        return
    }

    val connectionState by listenTogetherManager.connectionState.collectAsState()
    val roomState by listenTogetherManager.roomState.collectAsState()
    val userId by listenTogetherManager.userId.collectAsState()
    val pendingJoinRequests by listenTogetherManager.pendingJoinRequests.collectAsState()
    val pendingSuggestions by listenTogetherManager.pendingSuggestions.collectAsState()
    val unreadMessageCount by listenTogetherManager.unreadMessageCount.collectAsState()
    val pingMs by listenTogetherManager.pingMs.collectAsState()

    val serverStrength = when {
        connectionState != ConnectionState.CONNECTED -> 0
        pingMs == null -> 3 // Full strength on initial connect before first pong
        pingMs!! < 300L -> 3 // Excellent (< 300ms for cloud server)
        pingMs!! <= 600L -> 2 // Good (300 - 600ms)
        else -> 1 // Weak (> 600ms)
    }

    val (listenTogetherInTopBar) = rememberPreference(ListenTogetherInTopBarKey, defaultValue = true)
    val shouldShowTopBar = showTopBar || listenTogetherInTopBar

    var savedUsername by rememberPreference(ListenTogetherUsernameKey, "")
    var avatarIndex by rememberPreference(ListenTogetherAvatarIndexKey, 0)
    val roomCodeInput by viewModel.roomCodeInput.collectAsState()
    val usernameInput by viewModel.usernameInput.collectAsState()

    var showUsernameDialog by rememberSaveable { mutableStateOf(false) }
    var tempUsername by rememberSaveable { mutableStateOf("") }

    val handleConnect = {
        val effectiveUsername = usernameInput.ifBlank { savedUsername }.trim()
        if (effectiveUsername.isBlank()) {
            tempUsername = ""
            showUsernameDialog = true
        } else {
            listenTogetherManager.connect()
        }
    }

    val isJoiningRoom by viewModel.isJoiningRoom.collectAsState()
    val joinErrorMessage by viewModel.joinErrorMessage.collectAsState()

    val selectedUserForMenu by viewModel.selectedUserForMenu.collectAsState()
    val selectedUsername by viewModel.selectedUsername.collectAsState()

    val waitingForApprovalText = stringResource(R.string.waiting_for_approval)

    LaunchedEffect(savedUsername) {
        if (usernameInput.isBlank() && savedUsername.isNotBlank()) {
            viewModel.usernameInput.value = savedUsername
        }
    }

    LaunchedEffect(listenTogetherManager) {
        listenTogetherManager.events.collect { event ->
            if (event is ListenTogetherEvent.RoomCreated) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("ListenTogetherRoom", event.roomCode))
            }
        }
    }

    val isInRoom = listenTogetherManager.isInRoom
    val isHost = roomState?.hostId == userId

    val userJoinedTimes = remember { mutableMapOf<String, Long>() }
    LaunchedEffect(roomState?.users) {
        val now = System.currentTimeMillis()
        roomState?.users?.forEach { user ->
            userJoinedTimes.putIfAbsent(user.userId, now)
        }
    }

    // User action bottom sheet (host only)
    if (selectedUserForMenu != null && selectedUsername != null) {
        val targetUser = remember(selectedUserForMenu, roomState) {
            roomState?.users?.find { it.userId == selectedUserForMenu }
        }
        val joinTime = userJoinedTimes[selectedUserForMenu]
        val joinedText by produceState<String?>(initialValue = null, joinTime) {
            if (joinTime == null) {
                value = null
                return@produceState
            }
            while (true) {
                val minutes = ((System.currentTimeMillis() - joinTime) / 60_000).toInt()
                value = if (minutes <= 0) {
                    context.getString(R.string.joined_just_now)
                } else {
                    context.getString(R.string.joined_minutes_ago, minutes)
                }
                delay(30_000)
            }
        }

        UserActionBottomSheet(
            username = selectedUsername ?: targetUser?.cleanUsername ?: "",
            avatarIndex = targetUser?.avatarIndex ?: 0,
            joinedText = joinedText,
            onKick = {
                selectedUserForMenu?.let {
                    listenTogetherManager.kickUser(it, "Removed by host")
                }
                viewModel.selectedUserForMenu.value = null
                viewModel.selectedUsername.value = null
            },
            onPermanentKick = {
                selectedUserForMenu?.let { targetId ->
                    selectedUsername?.let { username ->
                        listenTogetherManager.blockUser(username)
                        listenTogetherManager.kickUser(
                            targetId,
                            context.getString(R.string.user_blocked_by_host)
                        )
                    }
                }
                viewModel.selectedUserForMenu.value = null
                viewModel.selectedUsername.value = null
            },
            onTransferOwnership = {
                selectedUserForMenu?.let {
                    listenTogetherManager.transferHost(it)
                }
                viewModel.selectedUserForMenu.value = null
                viewModel.selectedUsername.value = null
            },
            onDismiss = {
                viewModel.selectedUserForMenu.value = null
                viewModel.selectedUsername.value = null
            }
        )
    }

    if (showUsernameDialog) {
        val isOffline = connectionState == ConnectionState.DISCONNECTED ||
                connectionState == ConnectionState.ERROR
        DefaultDialog(
            onDismiss = { showUsernameDialog = false },
            icon = { Icon(painterResource(R.drawable.person), contentDescription = null) },
            title = { Text(stringResource(R.string.listen_together_username)) },
            buttons = {
                TextButton(onClick = { showUsernameDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val trimmed = tempUsername.trim()
                        if (trimmed.isNotBlank()) {
                            savedUsername = trimmed
                            viewModel.usernameInput.value = trimmed
                            showUsernameDialog = false
                            if (isOffline) {
                                listenTogetherManager.connect()
                            }
                        }
                    },
                    enabled = tempUsername.isNotBlank()
                ) {
                    Text(
                        stringResource(
                            if (isOffline) R.string.connect else android.R.string.ok
                        )
                    )
                }
            }
        ) {
            OutlinedTextField(
                value = tempUsername,
                onValueChange = { tempUsername = it },
                label = { Text(stringResource(R.string.enter_username)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val room = roomState
    val showRoomUi = isInRoom && room != null
    var isDisconnectingFromRoom by remember { mutableStateOf(false) }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = windowInsets.asPaddingValues().calculateTopPadding() + 16.dp,
            bottom = windowInsets.asPaddingValues().calculateBottomPadding() + 16.dp + AppBarHeight
        ),
        verticalArrangement = Arrangement.spacedBy(SectionGap)
    ) {
        if (!shouldShowTopBar) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TitlePill()
                }
            }
        }

        if (showRoomUi && room != null) {
            val connectedUsers = room.users.filter { it.isConnected }.ifEmpty { room.users }
            val connectedGuestsCount = room.users.count { it.isConnected && it.userId != room.hostId }
            val currentUserIdValue = userId ?: ""

            // Ring + Room code / Chat / Invite
            item {
                RoomStatusSection(
                    connectionState = connectionState,
                    listenerCount = connectedGuestsCount,
                    roomCode = room.roomCode,
                    isHost = isHost,
                    serverStrength = serverStrength,
                    unreadMessageCount = unreadMessageCount,
                    isDisconnecting = isDisconnectingFromRoom,
                    onCopyCode = { copyToClipboard(context, "Room Code", room.roomCode) },
                    onCopyLink = {
                        copyToClipboard(
                            context,
                            "Listen Together Link",
                            "https://vivimusic-listen-together.onrender.com/listen?code=${room.roomCode}"
                        )
                    },
                    onOpenChat = { navController.navigate("listen_together/chat") }
                )
            }

            item {
                Text(
                    text = if (isHost) {
                        stringResource(R.string.listen_together_host_description)
                    } else {
                        stringResource(R.string.listen_together_guest_description)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Reconnect | Disconnect + Leave room
            item {
                ConnectionActionGroup(
                    onReconnect = { listenTogetherManager.forceReconnect() },
                    onDisconnect = {
                        if (!isDisconnectingFromRoom) {
                            coroutineScope.launch {
                                isDisconnectingFromRoom = true
                                delay(420)
                                listenTogetherManager.disconnect()
                                isDisconnectingFromRoom = false
                            }
                        }
                    },
                    onLeaveRoom = {
                        if (!isDisconnectingFromRoom) {
                            coroutineScope.launch {
                                isDisconnectingFromRoom = true
                                delay(420)
                                listenTogetherManager.leaveRoom()
                                isDisconnectingFromRoom = false
                            }
                        }
                    }
                )
            }

            if (isHost && (pendingJoinRequests.isNotEmpty() || pendingSuggestions.isNotEmpty())) {
                item {
                    WaitingForYouSection(
                        requests = pendingJoinRequests,
                        suggestions = pendingSuggestions,
                        onApproveJoin = { listenTogetherManager.approveJoin(it) },
                        onRejectJoin = { listenTogetherManager.rejectJoin(it, "Rejected by host") },
                        onApproveSuggestion = { listenTogetherManager.approveSuggestion(it) },
                        onRejectSuggestion = { listenTogetherManager.rejectSuggestion(it, "Rejected by host") }
                    )
                }
            }

            item {
                ConnectedUsersSection(
                    users = connectedUsers,
                    isHost = isHost,
                    currentUserId = currentUserIdValue,
                    onUserClick = { clickedUserId, username ->
                        if (isHost && clickedUserId != currentUserIdValue) {
                            viewModel.selectedUserForMenu.value = clickedUserId
                            viewModel.selectedUsername.value = username
                        }
                    }
                )
            }
        } else {
            // Connection ring + status / actions
            item {
                ConnectionStatusSection(
                    connectionState = connectionState,
                    serverStrength = serverStrength,
                    username = usernameInput.ifBlank { savedUsername },
                    onConnect = handleConnect,
                    onDisconnect = { listenTogetherManager.disconnect() },
                    onReconnect = { listenTogetherManager.forceReconnect() },
                    onUsernameClick = {
                        tempUsername = usernameInput.ifBlank { savedUsername }
                        showUsernameDialog = true
                    }
                )
            }

            if (connectionState == ConnectionState.CONNECTED) {
                item {
                    Text(
                        text = stringResource(R.string.listen_together_background_disconnect_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Create / Join only once connected
                item {
                    JoinCreateRoomSection(
                        usernameInput = usernameInput,
                        onUsernameChange = { viewModel.usernameInput.value = it },
                        roomCodeInput = roomCodeInput,
                        onRoomCodeChange = { viewModel.roomCodeInput.value = it },
                        savedUsername = savedUsername,
                        avatarIndex = avatarIndex,
                        isJoiningRoom = isJoiningRoom,
                        joinErrorMessage = joinErrorMessage,
                        waitingForApprovalText = waitingForApprovalText,
                        bringIntoViewRequester = bringIntoViewRequester,
                        onCreateRoom = {
                            val username = usernameInput.takeIf { it.isNotBlank() } ?: savedUsername
                            val finalUsername = username.trim()
                            if (finalUsername.isNotBlank()) {
                                savedUsername = finalUsername
                                Toast.makeText(context, R.string.creating_room, Toast.LENGTH_SHORT).show()
                                viewModel.isCreatingRoom.value = true
                                viewModel.isJoiningRoom.value = false
                                viewModel.joinErrorMessage.value = null
                                listenTogetherManager.connect()
                                listenTogetherManager.createRoom(finalUsername)
                            } else {
                                Toast.makeText(context, R.string.error_username_empty, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onJoinRoom = {
                            val username = usernameInput.takeIf { it.isNotBlank() } ?: savedUsername
                            val finalUsername = username.trim()
                            if (finalUsername.isNotBlank()) {
                                savedUsername = finalUsername
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.joining_room, roomCodeInput),
                                    Toast.LENGTH_SHORT
                                ).show()
                                viewModel.isJoiningRoom.value = true
                                viewModel.isCreatingRoom.value = false
                                viewModel.joinErrorMessage.value = null
                                listenTogetherManager.connect()
                                listenTogetherManager.joinRoom(roomCodeInput, finalUsername)
                            } else {
                                Toast.makeText(context, R.string.error_username_empty, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCancelJoin = {
                            if (isJoiningRoom) {
                                viewModel.isJoiningRoom.value = false
                                viewModel.joinErrorMessage.value = null
                                listenTogetherManager.leaveRoom()
                            }
                        },
                        onFieldFocused = {
                            coroutineScope.launch {
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    )
                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.listen_together_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Settings link
        if (!isInRoom) {
            item {
                ExpressiveSettingGroup(
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.settings),
                            title = { Text(stringResource(R.string.settings)) },
                            description = { Text(stringResource(R.string.listen_together_settings_desc)) },
                            onClick = { navController.navigate("settings/integrations/listen_together") }
                        )
                    )
                )
            }
        }
    }

    if (shouldShowTopBar) {
        CenterAlignedTopAppBar(
            title = { TitlePill() },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null
                    )
                }
            },
            actions = {
                MaterialIconButton(
                    onClick = { navController.navigate("settings/integrations/listen_together") }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.settings),
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )
    }
}

// ---------------------------------------------------------------------------
// Building blocks
// ---------------------------------------------------------------------------

@Composable
private fun NotConfiguredContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.group),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.listen_together),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.listen_together_not_configured),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TitlePill() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = stringResource(R.string.listen_together),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

/**
 * Base tile. Optional [fillFraction] paints a darker "slider" fill behind the content,
 * like the Google Health tiles.
 */
@Composable
private fun StatusTile(
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    fillFraction: Float = 0f,
    content: @Composable BoxScope.() -> Unit
) {
    val inner: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize()) {
            if (fillFraction > 0f) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fillFraction.coerceIn(0f, 1f))
                        .background(contentColor.copy(alpha = 0.12f))
                )
            }
            content()
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            content = inner
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            content = inner
        )
    }
}

@Composable
private fun BoxScope.TileText(
    value: String,
    label: String? = null,
    letterSpacing: Float = 0f
) {
    Column(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = letterSpacing.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Text-only action tile. */
@Composable
private fun ActionTile(
    text: String,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    StatusTile(
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = modifier,
        onClick = onClick
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 8.dp)
        )
    }
}

private fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + f * (stop.red - start.red),
        green = start.green + f * (stop.green - start.green),
        blue = start.blue + f * (stop.blue - start.blue),
        alpha = start.alpha + f * (stop.alpha - start.alpha)
    )
}

/**
 * Single unified circular connection progress indicator.
 * - [ConnectionState.CONNECTING], [ConnectionState.RECONNECTING]: One clean forward spinning arc.
 * - [ConnectionState.CONNECTED]: Animates forward to full circle (360f) in primary color.
 * - [ConnectionState.ERROR]: Full circle in error color.
 * - [ConnectionState.DISCONNECTED]: Animates backward to 0f, leaving the empty track in outline.
 */
@Composable
private fun CircularProgressRing(
    connectionState: ConnectionState,
    serverStrength: Int = 0,
    isDisconnecting: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val isConnecting = connectionState == ConnectionState.CONNECTING ||
            connectionState == ConnectionState.RECONNECTING

    val targetColor = when (connectionState) {
        ConnectionState.ERROR -> scheme.error
        else -> scheme.primary
    }
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(400),
        label = "ringColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ringTransition")
    // Step-by-step progress fill animation when connecting (1 -> 2 -> 3)
    val connectingStep by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 3.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "connectingStep"
    )

    // Segment blink animation on strength level changes (2 blinks before vanishing or settling)
    val trackColor = scheme.onSurface.copy(alpha = 0.12f)
    val isConnected = connectionState == ConnectionState.CONNECTED

    val targetSeg1 = isConnected && serverStrength >= 1
    val targetSeg2 = isConnected && serverStrength >= 2
    val targetSeg3 = isConnected && serverStrength >= 3

    val seg1Anim = remember { Animatable(if (targetSeg1) 1f else 0f) }
    val seg2Anim = remember { Animatable(if (targetSeg2) 1f else 0f) }
    val seg3Anim = remember { Animatable(if (targetSeg3) 1f else 0f) }

    var prevSeg1 by remember { mutableStateOf<Boolean?>(null) }
    var prevSeg2 by remember { mutableStateOf<Boolean?>(null) }
    var prevSeg3 by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(targetSeg1) {
        val prev = prevSeg1
        prevSeg1 = targetSeg1
        if (prev == null) {
            seg1Anim.snapTo(if (targetSeg1) 1f else 0f)
            return@LaunchedEffect
        }
        if (prev == targetSeg1) return@LaunchedEffect

        if (targetSeg1) {
            seg1Anim.animateTo(1f, tween(75))
            seg1Anim.animateTo(0f, tween(75))
            seg1Anim.animateTo(1f, tween(75))
            seg1Anim.animateTo(0f, tween(75))
            seg1Anim.animateTo(1f, tween(90))
        } else {
            seg1Anim.animateTo(0f, tween(75))
            seg1Anim.animateTo(1f, tween(75))
            seg1Anim.animateTo(0f, tween(75))
            seg1Anim.animateTo(1f, tween(75))
            seg1Anim.animateTo(0f, tween(90))
        }
    }

    LaunchedEffect(targetSeg2) {
        val prev = prevSeg2
        prevSeg2 = targetSeg2
        if (prev == null) {
            seg2Anim.snapTo(if (targetSeg2) 1f else 0f)
            return@LaunchedEffect
        }
        if (prev == targetSeg2) return@LaunchedEffect

        if (targetSeg2) {
            seg2Anim.animateTo(1f, tween(75))
            seg2Anim.animateTo(0f, tween(75))
            seg2Anim.animateTo(1f, tween(75))
            seg2Anim.animateTo(0f, tween(75))
            seg2Anim.animateTo(1f, tween(90))
        } else {
            seg2Anim.animateTo(0f, tween(75))
            seg2Anim.animateTo(1f, tween(75))
            seg2Anim.animateTo(0f, tween(75))
            seg2Anim.animateTo(1f, tween(75))
            seg2Anim.animateTo(0f, tween(90))
        }
    }

    LaunchedEffect(targetSeg3) {
        val prev = prevSeg3
        prevSeg3 = targetSeg3
        if (prev == null) {
            seg3Anim.snapTo(if (targetSeg3) 1f else 0f)
            return@LaunchedEffect
        }
        if (prev == targetSeg3) return@LaunchedEffect

        if (targetSeg3) {
            seg3Anim.animateTo(1f, tween(75))
            seg3Anim.animateTo(0f, tween(75))
            seg3Anim.animateTo(1f, tween(75))
            seg3Anim.animateTo(0f, tween(75))
            seg3Anim.animateTo(1f, tween(90))
        } else {
            seg3Anim.animateTo(0f, tween(75))
            seg3Anim.animateTo(1f, tween(75))
            seg3Anim.animateTo(0f, tween(75))
            seg3Anim.animateTo(1f, tween(75))
            seg3Anim.animateTo(0f, tween(90))
        }
    }

    val seg1Color = if (isConnecting) {
        if (connectingStep >= 1f) scheme.primary else trackColor
    } else {
        lerpColor(trackColor, scheme.primary, seg1Anim.value)
    }

    val seg2Color = if (isConnecting) {
        if (connectingStep >= 2f) scheme.primary else trackColor
    } else {
        lerpColor(trackColor, scheme.primary, seg2Anim.value)
    }

    val seg3Color = if (isConnecting) {
        if (connectingStep >= 3f) scheme.primary else trackColor
    } else {
        lerpColor(trackColor, scheme.primary, seg3Anim.value)
    }

    // Sweep animation: starts from top point (-90f), sweeps out on connect/reconnect, and joins the point (360f) on connect
    val isInitiallyConnected = connectionState == ConnectionState.CONNECTED
    val sweepAnim = remember { Animatable(if (isInitiallyConnected) 360f else 0f) }
    var isFirstComposition by remember { mutableStateOf(true) }

    LaunchedEffect(connectionState, isDisconnecting) {
        if (isDisconnecting) {
            sweepAnim.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
            return@LaunchedEffect
        }

        if (isFirstComposition) {
            isFirstComposition = false
            sweepAnim.snapTo(if (connectionState == ConnectionState.CONNECTED) 360f else 0f)
            return@LaunchedEffect
        }

        when (connectionState) {
            ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> {
                // When connecting or reconnecting, reset to top point (0f) and sweep forward
                sweepAnim.snapTo(0f)
                sweepAnim.animateTo(255f, tween(700, easing = FastOutSlowInEasing))
            }
            ConnectionState.CONNECTED -> {
                // Smoothly sweep forward to 360f and join the point seamlessly
                sweepAnim.animateTo(360f, tween(800, easing = FastOutSlowInEasing))
            }
            ConnectionState.ERROR -> {
                sweepAnim.animateTo(360f, tween(400, easing = FastOutSlowInEasing))
            }
            ConnectionState.DISCONNECTED -> {
                // Smoothly retract back to top point
                sweepAnim.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
            }
        }
    }
    val animatedSweep = sweepAnim.value

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(10.dp)) {
            val diameter = size.minDimension
            val stroke = diameter * 0.12f
            val topLeft = Offset(
                (size.width - diameter) / 2f + stroke / 2f,
                (size.height - diameter) / 2f + stroke / 2f
            )
            val arcSize = Size(diameter - stroke, diameter - stroke)

            // Stationary background ring track
            drawArc(
                color = scheme.surfaceContainerHighest,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )

            // Progress bar: anchored at top (-90f), sweeps clockwise and joins point perfectly at 360f
            if (animatedSweep > 1f) {
                val sweep = animatedSweep.coerceIn(0f, 360f)
                val arcRadius = (diameter - stroke) / 2f
                val isFullCircle = sweep >= 359.5f

                // Draw the arc body with flat Butt ends flush with circle centers
                drawArc(
                    color = animatedColor,
                    startAngle = -90f,
                    sweepAngle = if (isFullCircle) 360f else sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )

                // Start point circle at 12 o'clock (provides smooth round start cap)
                val startCenter = Offset(center.x, center.y - arcRadius)
                drawCircle(
                    color = animatedColor,
                    radius = stroke / 2f,
                    center = startCenter
                )

                // Leading head circle: glides along perimeter and lands right into startCenter at 360f
                if (!isFullCircle) {
                    val headAngleRad = Math.toRadians((-90.0 + sweep.toDouble()))
                    val headCenter = Offset(
                        center.x + arcRadius * kotlin.math.cos(headAngleRad).toFloat(),
                        center.y + arcRadius * kotlin.math.sin(headAngleRad).toFloat()
                    )
                    drawCircle(
                        color = animatedColor,
                        radius = stroke / 2f,
                        center = headCenter
                    )
                }
            }

            // 3-segment server strength curved gauge at the top (styled like AudioDevice slider segments)
            val gaugeRadius = (diameter - stroke) / 2f - stroke / 2f - 7.dp.toPx()
            val gaugeThickness = 5.dp.toPx()
            val sweep = 26f
            val gap = 8f

            val startAngle2 = -90f - (sweep / 2f)
            val startAngle1 = startAngle2 - gap - sweep
            val startAngle3 = startAngle2 + sweep + gap

            val gaugeTopLeft = Offset(
                center.x - gaugeRadius,
                center.y - gaugeRadius
            )
            val gaugeSize = Size(gaugeRadius * 2f, gaugeRadius * 2f)

            // Segment 1 (Left): Rounded start cap, flat right end
            drawArc(
                color = seg1Color,
                startAngle = startAngle1,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = gaugeTopLeft,
                size = gaugeSize,
                style = Stroke(width = gaugeThickness, cap = StrokeCap.Butt)
            )
            val rad1 = Math.toRadians(startAngle1.toDouble())
            drawCircle(
                color = seg1Color,
                radius = gaugeThickness / 2f,
                center = Offset(
                    center.x + gaugeRadius * kotlin.math.cos(rad1).toFloat(),
                    center.y + gaugeRadius * kotlin.math.sin(rad1).toFloat()
                )
            )

            // Segment 2 (Center): Flat left end, flat right end (curved rectangle block)
            drawArc(
                color = seg2Color,
                startAngle = startAngle2,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = gaugeTopLeft,
                size = gaugeSize,
                style = Stroke(width = gaugeThickness, cap = StrokeCap.Butt)
            )

            // Segment 3 (Right): Flat left end, rounded right end cap
            drawArc(
                color = seg3Color,
                startAngle = startAngle3,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = gaugeTopLeft,
                size = gaugeSize,
                style = Stroke(width = gaugeThickness, cap = StrokeCap.Butt)
            )
            val rad3End = Math.toRadians((startAngle3 + sweep).toDouble())
            drawCircle(
                color = seg3Color,
                radius = gaugeThickness / 2f,
                center = Offset(
                    center.x + gaugeRadius * kotlin.math.cos(rad3End).toFloat(),
                    center.y + gaugeRadius * kotlin.math.sin(rad3End).toFloat()
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .offset(y = (-6).dp),
            content = content
        )
    }
}

// ---------------------------------------------------------------------------
// Connection status section (ring on the left, grouped tiles on the right)
// ---------------------------------------------------------------------------

@Composable
private fun ConnectionStatusSection(
    connectionState: ConnectionState,
    username: String,
    serverStrength: Int = 0,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit,
    onUsernameClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isOffline = connectionState == ConnectionState.DISCONNECTED ||
            connectionState == ConnectionState.ERROR
    val isConnecting = connectionState == ConnectionState.CONNECTING ||
            connectionState == ConnectionState.RECONNECTING

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(StatusSectionHeight),
        horizontalArrangement = Arrangement.spacedBy(SectionGap)
    ) {
        CircularProgressRing(
            connectionState = connectionState,
            serverStrength = serverStrength,
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .clickable(onClick = onUsernameClick)
        ) {
            if (isOffline) {
                Text(
                    text = stringResource(R.string.server),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Transparent,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.listen_together_disconnected),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.not_connected),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            } else if (isConnecting) {
                Text(
                    text = stringResource(R.string.server),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Transparent,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.wait),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.connecting),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            } else if (connectionState == ConnectionState.CONNECTED) {
                Text(
                    text = stringResource(R.string.server),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.online),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.listen_together_connected),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(GroupGap)
        ) {
            val targetStatusContainer = when (connectionState) {
                ConnectionState.CONNECTED -> scheme.primaryContainer
                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> scheme.tertiaryContainer
                ConnectionState.ERROR -> scheme.errorContainer
                ConnectionState.DISCONNECTED -> scheme.surfaceContainerHighest
            }
            val targetStatusContent = when (connectionState) {
                ConnectionState.CONNECTED -> scheme.onPrimaryContainer
                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> scheme.onTertiaryContainer
                ConnectionState.ERROR -> scheme.onErrorContainer
                ConnectionState.DISCONNECTED -> scheme.onSurface
            }

            val targetServerContainer = when (connectionState) {
                ConnectionState.CONNECTED -> scheme.secondaryContainer
                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> scheme.secondaryContainer
                ConnectionState.ERROR -> scheme.errorContainer
                ConnectionState.DISCONNECTED -> scheme.surfaceContainerHighest
            }
            val targetServerContent = when (connectionState) {
                ConnectionState.CONNECTED -> scheme.onSecondaryContainer
                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> scheme.onSecondaryContainer
                ConnectionState.ERROR -> scheme.onErrorContainer
                ConnectionState.DISCONNECTED -> scheme.onSurface
            }

            val statusContainerColor by animateColorAsState(targetStatusContainer, label = "statusContainer")
            val statusContentColor by animateColorAsState(targetStatusContent, label = "statusContent")
            val serverContainerColor by animateColorAsState(targetServerContainer, label = "serverContainer")
            val serverContentColor by animateColorAsState(targetServerContent, label = "serverContent")

            // Tile 1: Status
            StatusTile(
                shape = verticalShape(isTop = true, isBottom = false),
                containerColor = statusContainerColor,
                contentColor = statusContentColor,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                TileText(
                    label = stringResource(R.string.status),
                    value = when (connectionState) {
                        ConnectionState.CONNECTED -> stringResource(R.string.listen_together_connected)
                        ConnectionState.CONNECTING -> stringResource(R.string.listen_together_connecting)
                        ConnectionState.RECONNECTING -> stringResource(R.string.listen_together_reconnecting)
                        ConnectionState.ERROR -> stringResource(R.string.listen_together_error)
                        ConnectionState.DISCONNECTED -> stringResource(R.string.disconnected)
                    }
                )
            }

            // Tile 2: Server
            StatusTile(
                shape = verticalShape(isTop = false, isBottom = false),
                containerColor = serverContainerColor,
                contentColor = serverContentColor,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                TileText(
                    label = stringResource(R.string.server),
                    value = when (connectionState) {
                        ConnectionState.CONNECTED -> stringResource(R.string.online)
                        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> stringResource(R.string.listen_together_connecting)
                        ConnectionState.ERROR -> stringResource(R.string.listen_together_error)
                        ConnectionState.DISCONNECTED -> stringResource(R.string.listen_together_disconnected)
                    }
                )
            }

            // Tile 3: Action button
            if (isOffline) {
                StatusTile(
                    shape = verticalShape(isTop = false, isBottom = true),
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                    onClick = onConnect,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    TileText(
                        label = stringResource(R.string.tap_to_start),
                        value = stringResource(R.string.connect)
                    )
                }
            } else {
                StatusTile(
                    shape = verticalShape(isTop = false, isBottom = true),
                    containerColor = scheme.errorContainer,
                    contentColor = scheme.onErrorContainer,
                    onClick = onDisconnect,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    TileText(
                        label = if (isConnecting) stringResource(R.string.listen_together_connecting) else stringResource(R.string.listen_together_connected),
                        value = stringResource(R.string.disconnect)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// In-room status section
// ---------------------------------------------------------------------------

@Composable
private fun RoomStatusSection(
    connectionState: ConnectionState = ConnectionState.CONNECTED,
    listenerCount: Int,
    roomCode: String,
    isHost: Boolean,
    serverStrength: Int = 0,
    unreadMessageCount: Int,
    isDisconnecting: Boolean = false,
    onCopyCode: () -> Unit,
    onCopyLink: () -> Unit,
    onOpenChat: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(StatusSectionHeight),
        horizontalArrangement = Arrangement.spacedBy(SectionGap)
    ) {
        CircularProgressRing(
            connectionState = connectionState,
            serverStrength = serverStrength,
            isDisconnecting = isDisconnecting,
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
        ) {
            val isOffline = connectionState == ConnectionState.DISCONNECTED ||
                    connectionState == ConnectionState.ERROR
            val isReconnecting = connectionState == ConnectionState.CONNECTING ||
                    connectionState == ConnectionState.RECONNECTING

            if (isOffline) {
                Text(
                    text = stringResource(R.string.server),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Transparent,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.connection_lost),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.Bold,
                    color = scheme.error,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.waiting_for_connection),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            } else if (isReconnecting) {
                Text(
                    text = stringResource(R.string.server),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Transparent,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.listen_together_reconnecting),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.waiting_for_connection),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            } else {
                val displayCount = if (isHost) listenerCount else maxOf(listenerCount, 1)
                Text(
                    text = displayCount.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                )
                Text(
                    text = stringResource(R.string.listen_together_connected),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(GroupGap)
        ) {
            StatusTile(
                shape = verticalShape(isTop = true, isBottom = false),
                containerColor = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer,
                onClick = onCopyCode,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                TileText(
                    label = stringResource(R.string.room_code),
                    value = roomCode,
                    letterSpacing = 1f
                )
            }
            StatusTile(
                shape = verticalShape(isTop = false, isBottom = false),
                containerColor = scheme.tertiaryContainer,
                contentColor = scheme.onTertiaryContainer,
                onClick = onOpenChat,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                TileText(
                    label = stringResource(R.string.comments),
                    value = unreadMessageCount.toString()
                )
            }
            StatusTile(
                shape = verticalShape(isTop = false, isBottom = true),
                containerColor = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer,
                onClick = onCopyLink,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                TileText(value = stringResource(R.string.copy_link))
            }
        }
    }
}

/** Reconnect | Disconnect side by side, Leave room full width underneath. */
@Composable
private fun ConnectionActionGroup(
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
    onLeaveRoom: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GroupGap)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(GroupGap)
        ) {
            ActionTile(
                text = stringResource(R.string.reconnect),
                shape = corners(topStart = true, topEnd = false, bottomStart = false, bottomEnd = false),
                containerColor = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer,
                onClick = onReconnect,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ActionTile(
                text = stringResource(R.string.disconnect),
                shape = corners(topStart = false, topEnd = true, bottomStart = false, bottomEnd = false),
                containerColor = scheme.surfaceContainerHighest,
                contentColor = scheme.onSurface,
                onClick = onDisconnect,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
        ActionTile(
            text = stringResource(R.string.leave_room),
            shape = verticalShape(isTop = false, isBottom = true),
            containerColor = scheme.errorContainer,
            contentColor = scheme.onErrorContainer,
            onClick = onLeaveRoom,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Users / pending sections
// ---------------------------------------------------------------------------

@Composable
private fun WaitingForYouSection(
    requests: List<JoinRequestPayload>,
    suggestions: List<SuggestionReceivedPayload>,
    onApproveJoin: (String) -> Unit,
    onRejectJoin: (String) -> Unit,
    onApproveSuggestion: (String) -> Unit,
    onRejectSuggestion: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val total = requests.size + suggestions.size
    if (total == 0) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GroupGap)
    ) {
        // Header tile
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = verticalShape(isTop = true, isBottom = false),
            color = scheme.tertiaryContainer,
            contentColor = scheme.onTertiaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.waiting_for_you),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(shape = CircleShape, color = scheme.surface) {
                    Text(
                        text = total.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // People asking to join
        requests.forEachIndexed { index, request ->
            val isLast = index == requests.lastIndex && suggestions.isEmpty()
            WaitingRow(
                shape = verticalShape(isTop = false, isBottom = isLast),
                title = request.cleanUsername,
                subtitle = null,
                leading = {
                    AvatarBadge(
                        name = request.cleanUsername,
                        avatarIndex = request.avatarIndex,
                        container = scheme.secondaryContainer,
                        content = scheme.onSecondaryContainer
                    )
                },
                onReject = { onRejectJoin(request.userId) },
                onApprove = { onApproveJoin(request.userId) }
            )
        }

        // Song suggestions
        suggestions.forEachIndexed { index, suggestion ->
            WaitingRow(
                shape = verticalShape(isTop = false, isBottom = index == suggestions.lastIndex),
                title = suggestion.trackInfo.title,
                subtitle = suggestion.fromUsername,
                leading = {
                    AvatarBadge(
                        name = suggestion.fromUsername,
                        avatarIndex = 0,
                        container = scheme.tertiaryContainer,
                        content = scheme.onTertiaryContainer
                    )
                },
                onReject = { onRejectSuggestion(suggestion.suggestionId) },
                onApprove = { onApproveSuggestion(suggestion.suggestionId) }
            )
        }
    }
}

@Composable
private fun WaitingRow(
    shape: Shape,
    title: String,
    subtitle: String?,
    leading: @Composable () -> Unit,
    onReject: () -> Unit,
    onApprove: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = scheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            leading()

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            ActionPill(
                text = stringResource(R.string.decline),
                container = scheme.surfaceContainerHighest,
                content = scheme.onSurface,
                onClick = onReject
            )
            ActionPill(
                text = stringResource(R.string.accept),
                container = scheme.primary,
                content = scheme.onPrimary,
                onClick = onApprove
            )
        }
    }
}

@Composable
private fun ActionPill(
    text: String,
    container: Color,
    content: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = container,
        contentColor = content
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun ConnectedUsersSection(
    users: List<UserInfo>,
    isHost: Boolean,
    currentUserId: String,
    onUserClick: (String, String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var expanded by rememberSaveable { mutableStateOf(false) }

    val ordered = remember(users, currentUserId) {
        users.sortedWith(
            compareByDescending<UserInfo> { it.isHost }
                .thenByDescending { it.userId == currentUserId }
        )
    }
    val hasFooter = ordered.size > COLLAPSED_USER_COUNT
    val visible = if (expanded || !hasFooter) ordered else ordered.take(COLLAPSED_USER_COUNT)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
        verticalArrangement = Arrangement.spacedBy(GroupGap)
    ) {
        // Header tile (top segment)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = verticalShape(isTop = true, isBottom = visible.isEmpty() && !hasFooter),
            color = scheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.connected_users),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // One tile per user
        visible.forEachIndexed { index, user ->
            val isCurrentUser = user.userId == currentUserId
            ConnectedUserRow(
                user = user,
                isCurrentUser = isCurrentUser,
                isClickable = isHost && !isCurrentUser,
                shape = verticalShape(
                    isTop = false,
                    isBottom = index == visible.lastIndex && !hasFooter
                ),
                onClick = { onUserClick(user.userId, user.cleanUsername) }
            )
        }

        // Show all / Show less
        if (hasFooter) {
            Surface(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
                shape = verticalShape(isTop = false, isBottom = true),
                color = scheme.surfaceContainerHigh
            ) {
                Text(
                    text = if (expanded) stringResource(R.string.show_less_action) else stringResource(R.string.show_all_count, ordered.size),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ConnectedUserRow(
    user: UserInfo,
    isCurrentUser: Boolean,
    isClickable: Boolean,
    shape: Shape,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val container = when {
        user.isHost -> scheme.primary
        isCurrentUser -> scheme.secondary
        else -> scheme.tertiaryContainer
    }
    val content = when {
        user.isHost -> scheme.onPrimary
        isCurrentUser -> scheme.onSecondary
        else -> scheme.onTertiaryContainer
    }

    Surface(
        onClick = onClick,
        enabled = isClickable,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = scheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AvatarBadge(
                name = user.cleanUsername,
                avatarIndex = user.avatarIndex,
                container = container,
                content = content
            )

            Text(
                text = user.cleanUsername,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (user.isHost) {
                RoleChip(
                    text = stringResource(R.string.host_label),
                    containerColor = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer
                )
            }
            if (isCurrentUser) {
                RoleChip(
                    text = stringResource(R.string.you_label),
                    containerColor = scheme.secondaryContainer,
                    contentColor = scheme.onSecondaryContainer
                )
            }
            if (isClickable) {
                Text(
                    text = stringResource(R.string.manage_user),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RoleChip(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun AvatarBadge(
    name: String,
    avatarIndex: Int,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = container
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (avatarIndex == 0) {
                Text(
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = content
                )
            } else {
                Image(
                    painter = painterResource(
                        avatarOptions.getOrElse(avatarIndex) { R.drawable.person }
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}



// ---------------------------------------------------------------------------
// Create / Join (grouped: username, segmented room code, Create | Join)
// ---------------------------------------------------------------------------

@Composable
private fun JoinCreateRoomSection(
    usernameInput: String,
    onUsernameChange: (String) -> Unit,
    roomCodeInput: String,
    onRoomCodeChange: (String) -> Unit,
    savedUsername: String,
    avatarIndex: Int = 0,
    isJoiningRoom: Boolean,
    joinErrorMessage: String?,
    waitingForApprovalText: String,
    bringIntoViewRequester: BringIntoViewRequester,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    onCancelJoin: () -> Unit = {},
    onFieldFocused: () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val hasUsername = usernameInput.trim().isNotBlank() || savedUsername.isNotBlank()
    val hasRoomCode = roomCodeInput.length == ROOM_CODE_LENGTH

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        verticalArrangement = Arrangement.spacedBy(GroupGap)
    ) {
        // Username (top segment)
        TextField(
            value = usernameInput,
            onValueChange = onUsernameChange,
            label = { Text(stringResource(R.string.username)) },
            placeholder = { Text(stringResource(R.string.enter_username)) },
            leadingIcon = {
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    shape = CircleShape,
                    color = scheme.primaryContainer
                ) {
                    Image(
                        painter = painterResource(avatarOptions.getOrElse(avatarIndex) { R.drawable.person }),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            singleLine = true,
            shape = verticalShape(isTop = true, isBottom = false),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = scheme.surfaceContainerHigh,
                unfocusedContainerColor = scheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) onFieldFocused() }
        )

        // Room code (middle segment) with segmented boxes
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = verticalShape(isTop = false, isBottom = false),
            color = scheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.room_code),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    AnimatedVisibility(visible = roomCodeInput.isNotBlank()) {
                        MaterialIconButton(
                            onClick = {
                                onRoomCodeChange("")
                                onCancelJoin()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.close),
                                contentDescription = null,
                                tint = scheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                BasicTextField(
                    value = roomCodeInput,
                    onValueChange = { if (it.length <= ROOM_CODE_LENGTH) onRoomCodeChange(it.uppercase()) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .onFocusChanged { if (it.isFocused) onFieldFocused() },
                    decorationBox = { innerTextField ->
                        Box {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(GroupGap),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                repeat(ROOM_CODE_LENGTH) { index ->
                                    val char = roomCodeInput.getOrNull(index)?.toString() ?: ""
                                    val isFocusedBox = roomCodeInput.length == index
                                    val hasChar = char.isNotEmpty()

                                    val boxShape = corners(
                                        topStart = index == 0,
                                        topEnd = index == ROOM_CODE_LENGTH - 1,
                                        bottomStart = index == 0,
                                        bottomEnd = index == ROOM_CODE_LENGTH - 1
                                    )
                                    val boxColor by animateColorAsState(
                                        targetValue = if (hasChar) scheme.primaryContainer
                                        else scheme.surfaceContainerHighest,
                                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                        label = "inputBoxColor_$index"
                                    )
                                    val textColor by animateColorAsState(
                                        targetValue = if (hasChar) scheme.onPrimaryContainer
                                        else scheme.onSurface,
                                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                        label = "inputTextColor_$index"
                                    )
                                    val scale by animateFloatAsState(
                                        targetValue = if (hasChar) 1f else 0.92f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "inputCharScale_$index"
                                    )

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .graphicsLayer(scaleX = scale, scaleY = scale),
                                        shape = boxShape,
                                        color = boxColor,
                                        border = if (isFocusedBox) BorderStroke(2.dp, scheme.primary) else null
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = char,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            }
                            Box(modifier = Modifier.matchParentSize().alpha(0.01f)) {
                                innerTextField()
                            }
                        }
                    }
                )
            }
        }

        // Create | Join (bottom segment)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GroupGap)
        ) {
            Button(
                onClick = onCreateRoom,
                enabled = hasUsername,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = corners(topStart = false, topEnd = false, bottomStart = true, bottomEnd = false),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer
                )
            ) {
                Text(stringResource(R.string.create_room), fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onJoinRoom,
                enabled = hasUsername && hasRoomCode,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = corners(topStart = false, topEnd = false, bottomStart = false, bottomEnd = true),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.join_room), fontWeight = FontWeight.SemiBold)
            }
        }

        // Waiting for approval indicator
        AnimatedVisibility(
            visible = isJoiningRoom,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = scheme.primaryContainer
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = scheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = waitingForApprovalText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Error message
        AnimatedVisibility(
            visible = joinErrorMessage != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = scheme.errorContainer
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.error),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = scheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = joinErrorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onErrorContainer,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Host user menu bottom sheet
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserActionBottomSheet(
    username: String,
    avatarIndex: Int = 0,
    roleText: String? = null,
    joinedText: String? = null,
    onKick: () -> Unit,
    onPermanentKick: () -> Unit,
    onTransferOwnership: () -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun closeSheetThen(block: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { block() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner),
        containerColor = scheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Header: Avatar + User Info (No 'X' button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    color = scheme.tertiaryContainer,
                    contentColor = scheme.onTertiaryContainer
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarIndex == 0) {
                            Text(
                                text = username.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Image(
                                painter = painterResource(
                                    avatarOptions.getOrElse(avatarIndex) { R.drawable.person }
                                ),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = username,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = scheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = roleText ?: stringResource(R.string.listener),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                        if (joinedText != null) {
                            Text(
                                text = joinedText,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Transfer | Kick
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GroupGap)
            ) {
                BigActionButton(
                    icon = R.drawable.crown,
                    text = stringResource(R.string.transfer),
                    container = scheme.primaryContainer,
                    content = scheme.onPrimaryContainer,
                    shape = RoundedCornerShape(
                        topStart = BigOuter,
                        topEnd = BigInner,
                        bottomEnd = BigInner,
                        bottomStart = BigInner
                    ),
                    onClick = { closeSheetThen(onTransferOwnership) },
                    modifier = Modifier.weight(1f)
                )
                BigActionButton(
                    icon = R.drawable.logout,
                    text = stringResource(R.string.kick_user),
                    container = scheme.tertiaryContainer,
                    content = scheme.onTertiaryContainer,
                    shape = RoundedCornerShape(
                        topStart = BigInner,
                        topEnd = BigOuter,
                        bottomEnd = BigInner,
                        bottomStart = BigInner
                    ),
                    onClick = { closeSheetThen(onKick) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(GroupGap))

            // Block Button (instant click using Material Theme, no holding)
            Surface(
                onClick = { closeSheetThen(onPermanentKick) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                shape = RoundedCornerShape(
                    topStart = BigInner,
                    topEnd = BigInner,
                    bottomEnd = BigOuter,
                    bottomStart = BigOuter
                ),
                color = scheme.errorContainer,
                contentColor = scheme.onErrorContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.block),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.block),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun BigActionButton(
    icon: Int,
    text: String,
    container: Color,
    content: Color,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(68.dp),
        shape = shape,
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}