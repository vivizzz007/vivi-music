/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings.integrations

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.DiscordActivityNameKey
import com.music.vivi.constants.DiscordActivityTypeKey
import com.music.vivi.constants.DiscordAdvancedModeKey
import com.music.vivi.constants.DiscordAvatarKey
import com.music.vivi.constants.DiscordButton1TextKey
import com.music.vivi.constants.DiscordButton1VisibleKey
import com.music.vivi.constants.DiscordButton2TextKey
import com.music.vivi.constants.DiscordButton2VisibleKey
import com.music.vivi.constants.DiscordInfoDismissedKey
import com.music.vivi.constants.DiscordNameKey
import com.music.vivi.constants.DiscordStatusKey
import com.music.vivi.constants.DiscordTokenKey
import com.music.vivi.constants.DiscordUseDetailsKey
import com.music.vivi.constants.DiscordUsernameKey
import com.music.vivi.constants.EnableDiscordRPCKey
import com.music.vivi.db.entities.Song
import com.music.vivi.ui.component.EnumDialog
import com.music.vivi.ui.component.ExpressiveSettingGroup
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.InfoLabel
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.component.ModernSwitch
import com.music.vivi.ui.component.TextFieldDialog
import com.music.vivi.ui.component.TitlePill
import com.music.vivi.ui.component.SquigglySlider
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.SuperProperties
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import com.music.vivi.utils.makeTimeString
import com.music.vivi.utils.rememberPreference
import com.my.kizzy.rpc.KizzyRPC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private enum class DiscordStatus { ONLINE, IDLE, DND }
private enum class DiscordActivityType { LISTENING, PLAYING, WATCHING, COMPETING }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DiscordSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    snackbarHostState: SnackbarHostState,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val song by playerConnection.currentSong.collectAsState(null)
    val playbackState by playerConnection.playbackState.collectAsState()

    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Preferences
    val initialToken = remember { context.dataStore.get(DiscordTokenKey, "") }
    var discordToken by rememberPreference(DiscordTokenKey, initialToken)
    var discordUsername by rememberPreference(DiscordUsernameKey, "")
    var discordName by rememberPreference(DiscordNameKey, "")
    var discordAvatar by rememberPreference(DiscordAvatarKey, "")
    val initialDismissed = remember { context.dataStore.get(DiscordInfoDismissedKey, false) }
    var infoDismissed by rememberPreference(DiscordInfoDismissedKey, initialDismissed)

    val (discordRPC, onDiscordRPCChange) = rememberPreference(EnableDiscordRPCKey, true)
    val (useDetails, onUseDetailsChange) = rememberPreference(DiscordUseDetailsKey, false)
    val (advancedMode, onAdvancedModeChange) = rememberPreference(DiscordAdvancedModeKey, false)

    var discordStatus by rememberPreference(DiscordStatusKey, "online")
    var button1Text by rememberPreference(DiscordButton1TextKey, "")
    var button1Visible by rememberPreference(DiscordButton1VisibleKey, true)
    var button2Text by rememberPreference(DiscordButton2TextKey, "")
    var button2Visible by rememberPreference(DiscordButton2VisibleKey, true)
    var activityType by rememberPreference(DiscordActivityTypeKey, "listening")
    var activityName by rememberPreference(DiscordActivityNameKey, "")

    val isLoggedIn = remember(discordToken) { discordToken.isNotEmpty() }

    var showTokenDialog by rememberSaveable { mutableStateOf(false) }
    var showInfoSheet by rememberSaveable { mutableStateOf(false) }
    var showStatusDialog by rememberSaveable { mutableStateOf(false) }
    var showActivityTypeDialog by rememberSaveable { mutableStateOf(false) }
    var showButton1TextDialog by rememberSaveable { mutableStateOf(false) }
    var showButton2TextDialog by rememberSaveable { mutableStateOf(false) }
    var showActivityNameDialog by rememberSaveable { mutableStateOf(false) }

    // Map string prefs to enums for dialogs
    val currentStatus = when (discordStatus) {
        "idle" -> DiscordStatus.IDLE
        "dnd" -> DiscordStatus.DND
        else -> DiscordStatus.ONLINE
    }
    val currentActivityType = when (activityType) {
        "playing" -> DiscordActivityType.PLAYING
        "watching" -> DiscordActivityType.WATCHING
        "competing" -> DiscordActivityType.COMPETING
        else -> DiscordActivityType.LISTENING
    }

    // Fetch user info when token changes
    LaunchedEffect(discordToken) {
        val token = discordToken
        if (token.isEmpty()) {
            discordUsername = ""
            discordName = ""
            discordAvatar = ""
            return@LaunchedEffect
        }
        launch(Dispatchers.IO) {
            KizzyRPC.getUserInfo(
                token,
                SuperProperties.userAgent,
                SuperProperties.superPropertiesBase64
            ).onSuccess {
                discordUsername = it.username
                discordName = it.name
                discordAvatar = it.avatar ?: ""
            }.onFailure {
                discordUsername = ""
                discordName = ""
                discordAvatar = ""
            }
        }
    }

    // Update playback position
    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(100)
                position = playerConnection.player.currentPosition
            }
        }
    }

    // Dialogs / Sheets
    if (showTokenDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var tokenInput by rememberSaveable { mutableStateOf("") }
        var isVerifying by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        ModalBottomSheet(
            onDismissRequest = { if (!isVerifying) showTokenDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.advanced_login),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    // Top Box: Token input field
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(
                            topStart = 22.dp, topEnd = 22.dp,
                            bottomEnd = 5.dp, bottomStart = 5.dp,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            OutlinedTextField(
                                value = tokenInput,
                                onValueChange = {
                                    tokenInput = it
                                    error = null
                                },
                                placeholder = { Text(stringResource(R.string.advanced_login)) },
                                singleLine = false,
                                minLines = 3,
                                maxLines = 6,
                                enabled = !isVerifying,
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = if (tokenInput.isNotEmpty()) {
                                    {
                                        MaterialIconButton(onClick = { tokenInput = ""; error = null }) {
                                            Icon(
                                                painter = painterResource(R.drawable.close),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                } else null,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (isVerifying) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                )
                            }
                            if (error != null) {
                                Text(
                                    text = error!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }

                    // Middle Box: Description info
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.info),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = stringResource(R.string.token_adv_login_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Bottom Box: Connect / Login button
                    Surface(
                        onClick = {
                            val token = tokenInput.trim()
                            if (token.isNotEmpty()) {
                                isVerifying = true
                                error = null
                                coroutineScope.launch(Dispatchers.IO) {
                                    KizzyRPC.getUserInfo(
                                        token,
                                        SuperProperties.userAgent,
                                        SuperProperties.superPropertiesBase64
                                    ).onSuccess {
                                        discordToken = token
                                        sheetState.hide()
                                        showTokenDialog = false
                                        snackbarHostState.showSnackbar(context.getString(R.string.login_successful))
                                    }.onFailure {
                                        error = "Invalid token"
                                        isVerifying = false
                                    }
                                }
                            }
                        },
                        enabled = tokenInput.trim().isNotEmpty() && !isVerifying,
                        shape = RoundedCornerShape(
                            topStart = 5.dp, topEnd = 5.dp,
                            bottomEnd = 22.dp, bottomStart = 22.dp,
                        ),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.login),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showInfoSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val items = listOf(
            stringResource(R.string.discord_about_what_title) to stringResource(R.string.discord_about_what_desc),
            stringResource(R.string.discord_about_login_title) to stringResource(R.string.discord_about_login_desc),
            stringResource(R.string.discord_about_custom_title) to stringResource(R.string.discord_about_custom_desc),
            stringResource(R.string.discord_about_risk_title) to stringResource(R.string.discord_about_risk_desc),
        )

        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.discord_about_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    items.forEachIndexed { index, (title, desc) ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(
                                topStart = if (index == 0) 22.dp else 5.dp,
                                topEnd = if (index == 0) 22.dp else 5.dp,
                                bottomEnd = 5.dp,
                                bottomStart = 5.dp,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { showInfoSheet = false }
                        },
                        shape = RoundedCornerShape(
                            topStart = 5.dp, topEnd = 5.dp,
                            bottomEnd = 22.dp, bottomStart = 22.dp,
                        ),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.discord_got_it),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStatusDialog) {
        EnumDialog(
            onDismiss = { showStatusDialog = false },
            onSelect = { selected ->
                discordStatus = when (selected) {
                    DiscordStatus.IDLE -> "idle"
                    DiscordStatus.DND -> "dnd"
                    DiscordStatus.ONLINE -> "online"
                }
                showStatusDialog = false
            },
            title = stringResource(R.string.discord_status),
            current = currentStatus,
            values = DiscordStatus.entries.toList(),
            valueText = {
                when (it) {
                    DiscordStatus.ONLINE -> stringResource(R.string.discord_status_online)
                    DiscordStatus.IDLE -> stringResource(R.string.discord_status_idle)
                    DiscordStatus.DND -> stringResource(R.string.discord_status_dnd)
                }
            }
        )
    }

    if (showActivityTypeDialog) {
        EnumDialog(
            onDismiss = { showActivityTypeDialog = false },
            onSelect = { selected ->
                activityType = when (selected) {
                    DiscordActivityType.PLAYING -> "playing"
                    DiscordActivityType.WATCHING -> "watching"
                    DiscordActivityType.COMPETING -> "competing"
                    DiscordActivityType.LISTENING -> "listening"
                }
                showActivityTypeDialog = false
            },
            title = stringResource(R.string.discord_activity_type),
            current = currentActivityType,
            values = DiscordActivityType.entries.toList(),
            valueText = {
                when (it) {
                    DiscordActivityType.LISTENING -> stringResource(R.string.discord_activity_listening)
                    DiscordActivityType.PLAYING -> stringResource(R.string.discord_activity_playing)
                    DiscordActivityType.WATCHING -> stringResource(R.string.discord_activity_watching)
                    DiscordActivityType.COMPETING -> stringResource(R.string.discord_activity_competing)
                }
            }
        )
    }

    if (showButton1TextDialog) {
        TextFieldDialog(
            onDismiss = { showButton1TextDialog = false },
            onDone = {
                button1Text = it
                showButton1TextDialog = false
            },
            singleLine = true,
            initialTextFieldValue = TextFieldValue(button1Text),
            extraContent = {
                InfoLabel(text = stringResource(R.string.discord_button_text_variables))
            }
        )
    }

    if (showButton2TextDialog) {
        TextFieldDialog(
            onDismiss = { showButton2TextDialog = false },
            onDone = {
                button2Text = it
                showButton2TextDialog = false
            },
            singleLine = true,
            initialTextFieldValue = TextFieldValue(button2Text),
            extraContent = {
                InfoLabel(text = stringResource(R.string.discord_button_text_variables))
            }
        )
    }

    if (showActivityNameDialog) {
        TextFieldDialog(
            onDismiss = { showActivityNameDialog = false },
            onDone = {
                activityName = it
                showActivityNameDialog = false
            },
            singleLine = true,
            initialTextFieldValue = TextFieldValue(activityName),
            extraContent = {
                InfoLabel(text = stringResource(R.string.discord_activity_name_description))
            }
        )
    }

    Column(
        modifier = Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        AnimatedVisibility(visible = !infoDismissed) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { showInfoSheet = true },
                    )
                    Text(
                        text = stringResource(R.string.discord_information_warning),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { infoDismissed = true }) {
                        Text(
                            text = stringResource(R.string.dismiss),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }


        // Account Section
        Text(
            text = stringResource(R.string.discord_account),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 6.dp),
        )

        val cookie = MaterialShapes.Cookie12Sided.toShape()

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // Top box: avatar + name
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(
                    topStart = 28.dp, topEnd = 28.dp,
                    bottomEnd = 5.dp, bottomStart = 5.dp,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        shape = cookie,
                        color = if (isLoggedIn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(cookie),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                isLoggedIn && discordAvatar.isNotEmpty() -> AsyncImage(
                                    model = discordAvatar,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(cookie),
                                )
                                isLoggedIn -> Text(
                                    text = discordName.firstOrNull()?.uppercase() ?: "D",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                else -> Icon(
                                    painter = painterResource(R.drawable.discord),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isLoggedIn) discordName else stringResource(R.string.not_logged_in),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isLoggedIn && discordUsername.isNotEmpty()) {
                            Text(
                                text = "@$discordUsername",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!isLoggedIn) {
                            Text(
                                text = stringResource(R.string.discord_connect_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Bottom box(es)
            if (isLoggedIn) {
                Surface(
                    onClick = {
                        discordName = ""
                        discordToken = ""
                        discordUsername = ""
                        discordAvatar = ""
                    },
                    shape = RoundedCornerShape(
                        topStart = 5.dp, topEnd = 5.dp,
                        bottomEnd = 28.dp, bottomStart = 28.dp,
                    ),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.action_logout),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Surface(
                        onClick = { navController.navigate("settings/discord/login") },
                        shape = RoundedCornerShape(
                            topStart = 5.dp, topEnd = 5.dp,
                            bottomEnd = 5.dp, bottomStart = 28.dp,
                        ),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.action_login),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                        }
                    }
                    Surface(
                        onClick = { showTokenDialog = true },
                        shape = RoundedCornerShape(
                            topStart = 5.dp, topEnd = 5.dp,
                            bottomEnd = 28.dp, bottomStart = 5.dp,
                        ),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.advanced_login),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Options section
        ExpressiveSettingGroup(
            title = stringResource(R.string.options),
            items = listOf(
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.enable_discord_rpc)) },
                    trailingContent = {
                        ModernSwitch(
                            checked = discordRPC,
                            onCheckedChange = onDiscordRPCChange,
                            enabled = isLoggedIn,
                        )
                    },
                    enabled = isLoggedIn,
                    onClick = { if (isLoggedIn) onDiscordRPCChange(!discordRPC) },
                ),
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.discord_use_details)) },
                    description = {
                        Text(stringResource(R.string.discord_use_details_description))
                    },
                    trailingContent = {
                        ModernSwitch(
                            checked = useDetails,
                            onCheckedChange = onUseDetailsChange,
                            enabled = isLoggedIn && discordRPC,
                        )
                    },
                    enabled = isLoggedIn && discordRPC,
                    onClick = {
                        if (isLoggedIn && discordRPC) onUseDetailsChange(!useDetails)
                    },
                ),
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.discord_advanced_mode)) },
                    description = {
                        Text(stringResource(R.string.discord_advanced_mode_description))
                    },
                    trailingContent = {
                        ModernSwitch(
                            checked = advancedMode,
                            onCheckedChange = onAdvancedModeChange,
                            enabled = isLoggedIn && discordRPC,
                        )
                    },
                    enabled = isLoggedIn && discordRPC,
                    onClick = {
                        if (isLoggedIn && discordRPC) onAdvancedModeChange(!advancedMode)
                    },
                ),
            ),
        )

        Spacer(Modifier.height(8.dp))

        // Presence section
        AnimatedVisibility(visible = isLoggedIn && discordRPC && advancedMode) {
            Column {
                Text(
                    text = stringResource(R.string.discord_presence),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 6.dp),
                )

                val options = listOf(
                    "online" to stringResource(R.string.discord_status_online),
                    "idle" to stringResource(R.string.discord_status_idle),
                    "dnd" to stringResource(R.string.discord_status_dnd),
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.discord_status),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.discord_status_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))

                        // Connected button row. Selection only changes color – never size or shape.
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            options.forEachIndexed { index, (key, label) ->
                                val selected = discordStatus == key
                                val shape = RoundedCornerShape(
                                    topStart = if (index == 0) 18.dp else 6.dp,
                                    bottomStart = if (index == 0) 18.dp else 6.dp,
                                    topEnd = if (index == options.lastIndex) 18.dp else 6.dp,
                                    bottomEnd = if (index == options.lastIndex) 18.dp else 6.dp,
                                )
                                Surface(
                                    onClick = { discordStatus = key },
                                    shape = shape,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Info box under Presence
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.info),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.discord_presence_info),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                ExpressiveSettingGroup(
                    title = stringResource(R.string.discord_customization),
                    items = listOf(
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.discord_activity_type)) },
                            description = {
                                Text(
                                    when (currentActivityType) {
                                        DiscordActivityType.LISTENING -> stringResource(R.string.discord_activity_listening)
                                        DiscordActivityType.PLAYING -> stringResource(R.string.discord_activity_playing)
                                        DiscordActivityType.WATCHING -> stringResource(R.string.discord_activity_watching)
                                        DiscordActivityType.COMPETING -> stringResource(R.string.discord_activity_competing)
                                    }
                                )
                            },
                            onClick = { showActivityTypeDialog = true },
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.discord_activity_name)) },
                            description = {
                                Text(activityName.ifEmpty { stringResource(R.string.discord_activity_name_description) })
                            },
                            onClick = { showActivityNameDialog = true },
                        ),
                    ),
                )

                Spacer(Modifier.height(10.dp))

                ExpressiveSettingGroup(
                    title = stringResource(R.string.discord_buttons),
                    items = listOf(
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.discord_button_1)) },
                            description = {
                                Text(button1Text.ifEmpty { "Listen on YouTube Music" })
                            },
                            trailingContent = {
                                ModernSwitch(
                                    checked = button1Visible,
                                    onCheckedChange = { button1Visible = it },
                                )
                            },
                            onClick = { showButton1TextDialog = true },
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.discord_button_2)) },
                            description = {
                                Text(button2Text.ifEmpty { "GitHub Repository" })
                            },
                            trailingContent = {
                                ModernSwitch(
                                    checked = button2Visible,
                                    onCheckedChange = { button2Visible = it },
                                )
                            },
                            onClick = { showButton2TextDialog = true },
                        ),
                    ),
                )
            }
        }

        // Preview section
        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.discord_rpc_preview),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 6.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // Box 1: cookie art + title/artist + wavy slider
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(
                    topStart = 28.dp, topEnd = 28.dp,
                    bottomEnd = if (button1Visible || button2Visible) 5.dp else 28.dp,
                    bottomStart = if (button1Visible || button2Visible) 5.dp else 28.dp,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (activityName.isNotEmpty()) activityName else when (activityType) {
                            "playing" -> stringResource(R.string.discord_playing_vivimusic)
                            "watching" -> stringResource(R.string.discord_watching_vivimusic)
                            "competing" -> stringResource(R.string.discord_competing_vivimusic)
                            else -> stringResource(R.string.listening_to_vivimusic)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(Modifier.size(84.dp)) {
                            Surface(
                                shape = cookie,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(cookie),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val thumb = song?.song?.thumbnailUrl
                                    if (thumb != null) {
                                        AsyncImage(
                                            model = thumb,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(cookie),
                                        )
                                    } else {
                                        Text(
                                            text = "\u266A",
                                            fontSize = 32.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }

                            song?.artists?.firstOrNull()?.thumbnailUrl?.let {
                                Box(
                                    modifier = Modifier
                                        .border(
                                            2.dp,
                                            MaterialTheme.colorScheme.surfaceContainerHigh,
                                            CircleShape
                                        )
                                        .padding(2.dp)
                                        .align(Alignment.BottomEnd),
                                ) {
                                    AsyncImage(
                                        model = it,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape),
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song?.song?.title ?: "Song Title",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = song?.artists?.joinToString { it.name } ?: "Artist",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (useDetails) {
                                song?.album?.title?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            SquigglySlider(
                                value = song?.song?.duration?.takeIf { it > 0 }?.let { position.toFloat() / (it * 1000L) } ?: 0f,
                                onValueChange = {},
                                enabled = false,
                                isPlaying = playbackState == STATE_READY,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            // Box 2: Button 1
            if (button1Visible) {
                Surface(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "https://music.youtube.com/watch?v=${song?.id}".toUri())
                        )
                    },
                    enabled = song != null,
                    shape = RoundedCornerShape(
                        topStart = 5.dp, topEnd = 5.dp,
                        bottomEnd = if (button2Visible) 5.dp else 28.dp,
                        bottomStart = if (button2Visible) 5.dp else 28.dp,
                    ),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = button1Text.ifEmpty { stringResource(R.string.listen_on_youtube_music) },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
            }

            // Box 3: Button 2
            if (button2Visible) {
                Surface(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "https://github.com/vivizzz007/vivi-music".toUri())
                        )
                    },
                    shape = RoundedCornerShape(
                        topStart = 5.dp, topEnd = 5.dp,
                        bottomEnd = 28.dp, bottomStart = 28.dp,
                    ),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = button2Text.ifEmpty { stringResource(R.string.visit_vivimusic) },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
            }
        }

        // Bottom padding for mini player
        Spacer(Modifier.height(24.dp))
    }

    CenterAlignedTopAppBar(
        title = { TitlePill(stringResource(R.string.discord_integration)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        actions = {
            MaterialIconButton(
                onClick = { showInfoSheet = true },
            ) {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = stringResource(R.string.information),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}
