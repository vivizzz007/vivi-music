package com.music.vivi.ui.screens.settings.integrations

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.DiscordInfoDismissedKey
import com.music.vivi.constants.DiscordNameKey
import com.music.vivi.constants.DiscordTokenKey
import com.music.vivi.constants.DiscordUseDetailsKey
import com.music.vivi.constants.DiscordUsernameKey
import com.music.vivi.constants.EnableDiscordRPCKey
import com.music.vivi.constants.SettingsShapeColorTertiaryKey
import com.music.vivi.db.entities.Song
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.InfoLabel
import com.music.vivi.ui.component.PreferenceGroupTitle
import com.music.vivi.ui.component.TextFieldDialog
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.utils.makeTimeString
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Screen for configuring Discord integration.
 * Allows logging in (via token or WebView) and toggling rich presence/detailed status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: (() -> Unit)? = null,
) {
    val (settingsShapeTertiary, _) = rememberPreference(SettingsShapeColorTertiaryKey, false)
    val (darkMode, _) = rememberEnumPreference(
        DarkModeKey,
        defaultValue = DarkMode.AUTO
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkMode, isSystemInDarkTheme) {
        if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
    }

    val (iconBgColor, iconStyleColor) = if (settingsShapeTertiary) {
        if (useDarkTheme) {
            Pair(
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.onTertiary
            )
        } else {
            Pair(
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    } else {
        Pair(
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.primary
        )
    }

    val playerConnection = LocalPlayerConnection.current ?: return
    val song: com.music.vivi.db.entities.Song? by playerConnection.currentSong.collectAsState(null)
    val playbackState: Int by playerConnection.playbackState.collectAsState()

    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }

    val coroutineScope = rememberCoroutineScope()

    var discordToken by rememberPreference(DiscordTokenKey, "")
    val integrationsViewModel: com.music.vivi.viewmodels.IntegrationsViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
    val discordState: com.music.vivi.viewmodels.DiscordState by integrationsViewModel.discordState.collectAsState()

    // Delegate actual display values to VM state for reliability, but use preferences for mutability
    var discordUsername by rememberPreference(DiscordUsernameKey, "")
    var discordName by rememberPreference(DiscordNameKey, "")
    val isLoggedIn = discordToken.isNotEmpty()

    var infoDismissed by rememberPreference(DiscordInfoDismissedKey, false)

    // Local manual update for token editing still writes to DataStore via rememberPreference
    // VM observes DataStore and updates state.

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(100)
                position = playerConnection.player.currentPosition
            }
        }
    }

    val (discordRPC, onDiscordRPCChange) = rememberPreference(
        key = EnableDiscordRPCKey,
        defaultValue = true
    )

    val (useDetails, onUseDetailsChange) = rememberPreference(
        key = DiscordUseDetailsKey,
        defaultValue = false
    )

    var showTokenDialog by rememberSaveable { mutableStateOf(false) }

    if (showTokenDialog) {
        TextFieldDialog(
            onDismiss = { showTokenDialog = false },
            icon = { Icon(painterResource(R.drawable.token), null) },
            onDone = {
                discordToken = it
                showTokenDialog = false
            },
            singleLine = true,
            isInputValid = { it.isNotEmpty() },
            extraContent = {
                InfoLabel(text = stringResource(R.string.token_adv_login_description))
            }
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        AnimatedVisibility(
            visible = !infoDismissed
        ) {
            Card(
                colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp)
                )

                Text(
                    text = stringResource(R.string.discord_information),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                TextButton(
                    onClick = {
                        infoDismissed = true
                    },
                    modifier =
                    Modifier
                        .align(Alignment.End)
                        .padding(16.dp)
                ) {
                    Text(stringResource(R.string.dismiss))
                }
            }
        }

        PreferenceGroupTitle(
            title = stringResource(R.string.account)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                ModernInfoItem(
                    icon = {
                        if (isLoggedIn && song?.artists?.firstOrNull()?.thumbnailUrl != null) {
                            AsyncImage(
                                model = song?.artists?.firstOrNull()?.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(painterResource(R.drawable.discord), null, modifier = Modifier.size(22.dp))
                        }
                    },
                    title = if (isLoggedIn) discordName else stringResource(R.string.not_logged_in),
                    subtitle = if (discordUsername.isNotEmpty()) {
                        "@$discordUsername"
                    } else {
                        stringResource(
                            R.string.discord_account
                        )
                    },
                    onClick = if (isLoggedIn) {
                        {
                            discordName = ""
                            discordToken = ""
                            discordUsername = ""
                        }
                    } else {
                        {
                            navController.navigate("settings/discord/login")
                        }
                    },
                    showArrow = true,
                    iconBackgroundColor = iconBgColor,
                    iconContentColor = iconStyleColor
                )

                if (!isLoggedIn) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    ModernInfoItem(
                        icon = { Icon(painterResource(R.drawable.token), null, modifier = Modifier.size(22.dp)) },
                        title = stringResource(R.string.advanced_login),
                        subtitle = stringResource(R.string.login_using_token),
                        onClick = { showTokenDialog = true },
                        showArrow = true,
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor
                    )
                }
            }
        }

        PreferenceGroupTitle(
            title = stringResource(R.string.options)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModernInfoItem(
                        icon = { Icon(painterResource(R.drawable.discord), null, modifier = Modifier.size(22.dp)) },
                        title = stringResource(R.string.enable_discord_rpc),
                        subtitle = stringResource(R.string.show_current_song_discord),
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        modifier = Modifier.weight(1f)
                    )
                    ModernSwitch(
                        checked = discordRPC,
                        onCheckedChange = onDiscordRPCChange,
                        enabled = isLoggedIn,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModernInfoItem(
                        icon = { Icon(painterResource(R.drawable.info), null, modifier = Modifier.size(22.dp)) },
                        title = stringResource(R.string.discord_use_details),
                        subtitle = stringResource(R.string.discord_use_details_description),
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        modifier = Modifier.weight(1f)
                    )
                    ModernSwitch(
                        checked = useDetails,
                        onCheckedChange = onUseDetailsChange,
                        enabled = isLoggedIn && discordRPC,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            }
        }

        PreferenceGroupTitle(
            title = stringResource(R.string.preview)
        )

        RichPresence(song, position)
    }

    TopAppBar(
        title = { Text(stringResource(R.string.discord_integration)) },
        navigationIcon = {
            IconButton(
                onClick = { onBack?.invoke() ?: navController.navigateUp() },
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
fun RichPresence(song: Song?, currentPlaybackTimeMillis: Long = 0L) {
    val context = LocalContext.current

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 6.dp,
        modifier =
        Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.listening_to_vivimusic),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    Modifier.size(108.dp)
                ) {
                    AsyncImage(
                        model = song?.song?.thumbnailUrl,
                        contentDescription = null,
                        modifier =
                        Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .align(Alignment.TopStart)
                            .run {
                                if (song == null) {
                                    border(
                                        2.dp,
                                        MaterialTheme.colorScheme.onSurface,
                                        RoundedCornerShape(12.dp)
                                    )
                                } else {
                                    this
                                }
                            }
                    )

                    song?.artists?.firstOrNull()?.thumbnailUrl?.let {
                        Box(
                            modifier =
                            Modifier
                                .border(
                                    2.dp,
                                    MaterialTheme.colorScheme.surfaceContainer,
                                    CircleShape
                                )
                                .padding(2.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            AsyncImage(
                                model = it,
                                contentDescription = null,
                                modifier =
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }

                Column(
                    modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = song?.song?.title ?: stringResource(R.string.song_title_fallback),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = song?.artists?.joinToString { it.name } ?: stringResource(R.string.artist_fallback),
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    song?.album?.title?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (song != null) {
                        SongProgressBar(
                            currentTimeMillis = currentPlaybackTimeMillis,
                            durationMillis = song.song.duration.times(1000L)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                enabled = song != null,
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://music.youtube.com/watch?v=${song?.id}".toUri()
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.listen_on_youtube_music))
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://github.com/vivizzz007/vivi-music".toUri()
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.visit_vivimusic))
            }
        }
    }
}

@Composable
fun SongProgressBar(currentTimeMillis: Long, durationMillis: Long) {
    val progress = if (durationMillis > 0) currentTimeMillis.toFloat() / durationMillis else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = makeTimeString(currentTimeMillis),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                fontSize = 12.sp
            )
            Text(
                text = makeTimeString(durationMillis),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                fontSize = 12.sp
            )
        }
    }
}
