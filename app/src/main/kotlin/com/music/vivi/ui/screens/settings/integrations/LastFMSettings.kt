/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings.integrations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.music.vivi.ui.component.TitlePill
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.lastfm.LastFM
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.EnableLastFMScrobblingKey
import com.music.vivi.constants.LastFMSessionKey
import com.music.vivi.constants.LastFMUseNowPlaying
import com.music.vivi.constants.LastFMUseSendLikes
import com.music.vivi.constants.LastFMUsernameKey
import com.music.vivi.constants.LastFMAvatarUrlKey
import com.music.vivi.constants.LastFMScrobbleCountKey
import com.music.vivi.constants.ScrobbleDelayPercentKey
import com.music.vivi.constants.ScrobbleDelaySecondsKey
import com.music.vivi.constants.ScrobbleMinSongDurationKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.ExpressiveSettingGroup
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.safeOpenUri
import com.music.vivi.utils.makeTimeString
import com.music.vivi.utils.rememberPreference
import com.music.vivi.utils.reportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.text.NumberFormat
import java.util.Locale

private fun formatScrobbleCount(count: String): String {
    val number = count.toLongOrNull() ?: return count
    return NumberFormat.getNumberInstance(Locale.getDefault()).format(number)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LastFMSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) { 
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var lastfmUsername by rememberPreference(LastFMUsernameKey, "")
    var lastfmSession by rememberPreference(LastFMSessionKey, "")
    var lastfmAvatarUrl by rememberPreference(LastFMAvatarUrlKey, "")
    var lastfmScrobbleCount by rememberPreference(LastFMScrobbleCountKey, "")

    val isLoggedIn = remember(lastfmSession) { lastfmSession != "" }

    // Continuously refresh scrobble count while the screen is open
    LaunchedEffect(isLoggedIn, lastfmUsername) {
        if (!isLoggedIn || lastfmUsername.isEmpty()) return@LaunchedEffect
        while (true) {
            withContext(Dispatchers.IO) {
                LastFM.getUserInfo(lastfmUsername).onSuccess { info ->
                    lastfmAvatarUrl = info.user.image.find { it.size == "extralarge" }?.url
                        ?: info.user.image.lastOrNull()?.url ?: lastfmAvatarUrl
                    lastfmScrobbleCount = info.user.playcount
                }
            }
            delay(30_000L) // refresh every 30 seconds
        }
    }

    val (useNowPlaying, onUseNowPlayingChange) = rememberPreference(
        key = LastFMUseNowPlaying,
        defaultValue = false
    )

    val (useSendLikes, onUseSendLikes) = rememberPreference(
        key = LastFMUseSendLikes,
        defaultValue = false
    )

    val (lastfmScrobbling, onlastfmScrobblingChange) = rememberPreference(
        key = EnableLastFMScrobblingKey,
        defaultValue = false
    )

    val (scrobbleDelayPercent, onScrobbleDelayPercentChange) = rememberPreference(
        ScrobbleDelayPercentKey,
        defaultValue = LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT
    )

    val (minTrackDuration, onMinTrackDurationChange) = rememberPreference(
        ScrobbleMinSongDurationKey,
        defaultValue = LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION
    )

    val (scrobbleDelaySeconds, onScrobbleDelaySecondsChange) = rememberPreference(
        ScrobbleDelaySecondsKey,
        defaultValue = LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS
    )

    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    var isLoggingIn by rememberSaveable { mutableStateOf(false) }
    var loginError by rememberSaveable { mutableStateOf<String?>(null) }

    if (showLoginDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var username by rememberSaveable { mutableStateOf("") }
        var password by rememberSaveable { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = {
                if (!isLoggingIn) {
                    showLoginDialog = false
                    loginError = null
                }
            },
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
                    text = stringResource(R.string.login),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        loginError = null
                    },
                    label = { Text(stringResource(R.string.username)) },
                    singleLine = true,
                    enabled = !isLoggingIn,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        loginError = null
                    },
                    label = { Text(stringResource(R.string.password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = !isLoggingIn,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(16.dp),
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
                            text = stringResource(R.string.lastfm_login_info),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                loginError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (isLoggingIn) {
                    Text(
                        text = stringResource(R.string.logging_in),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Connected Cancel | Login row
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Surface(
                        onClick = {
                            if (!isLoggingIn) {
                                showLoginDialog = false
                                loginError = null
                            }
                        },
                        enabled = !isLoggingIn,
                        shape = RoundedCornerShape(
                            topStart = 22.dp, bottomStart = 22.dp,
                            topEnd = 5.dp, bottomEnd = 5.dp,
                        ),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.cancel),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                loginError = "Please enter both username and password"
                                return@Surface
                            }

                            isLoggingIn = true
                            loginError = null

                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    LastFM.getMobileSession(username, password)
                                        .onSuccess { auth ->
                                            lastfmUsername = auth.session.name
                                            lastfmSession = auth.session.key
                                            LastFM.sessionKey = auth.session.key

                                            LastFM.getUserInfo(auth.session.name).onSuccess { info ->
                                                val url = info.user.image.find { it.size == "extralarge" }?.url
                                                    ?: info.user.image.lastOrNull()?.url ?: ""
                                                lastfmAvatarUrl = url
                                                lastfmScrobbleCount = info.user.playcount
                                            }

                                            // Switch back to main thread to update UI
                                            coroutineScope.launch(Dispatchers.Main) {
                                                isLoggingIn = false
                                                showLoginDialog = false
                                                loginError = null
                                            }
                                        }
                                        .onFailure { exception ->
                                            coroutineScope.launch(Dispatchers.Main) {
                                                isLoggingIn = false
                                                loginError = when (exception) {
                                                    is com.music.lastfm.LastFM.LastFmException -> {
                                                        when (exception.code) {
                                                            4 -> "Invalid username or password"
                                                            6 -> "Invalid parameters"
                                                            9 -> "Invalid session key"
                                                            10 -> "Invalid API key"
                                                            13 -> "Invalid method signature"
                                                            14 -> "Unauthorized token"
                                                            15 -> "Service temporarily unavailable"
                                                            else -> "Login failed: ${exception.message}"
                                                        }
                                                    }
                                                    else -> "Network error. Please check your connection."
                                                }
                                            }
                                            reportException(exception)
                                        }
                                } catch (e: Exception) {
                                    coroutineScope.launch(Dispatchers.Main) {
                                        isLoggingIn = false
                                        loginError = "Unexpected error occurred"
                                    }
                                    reportException(e)
                                }
                            }
                        },
                        enabled = !isLoggingIn,
                        shape = RoundedCornerShape(
                            topStart = 5.dp, bottomStart = 5.dp,
                            topEnd = 22.dp, bottomEnd = 22.dp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.login),
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
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        // ----- Account -----
        Text(
            text = stringResource(R.string.account),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 6.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // Top card: cookie avatar + username + scrobble count
            Surface(
                color = if (isSystemInDarkTheme()) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 5.dp, bottomStart = 5.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(MaterialShapes.Cookie12Sided.toShape())
                            .background(
                                if (isLoggedIn) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            isLoggedIn && lastfmAvatarUrl.isNotEmpty() -> AsyncImage(
                                model = lastfmAvatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            isLoggedIn -> Text(
                                text = lastfmUsername.firstOrNull()?.uppercase() ?: "L",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            else -> Icon(
                                painter = painterResource(R.drawable.lastfm),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isLoggedIn) lastfmUsername else stringResource(R.string.not_logged_in),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isLoggedIn && lastfmScrobbleCount.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.lastfm_scrobbles_count, formatScrobbleCount(lastfmScrobbleCount)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!isLoggedIn) {
                            Text(
                                text = stringResource(R.string.lastfm_about_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Middle card: view listening history
            Surface(
                onClick = {
                    if (isLoggedIn && lastfmUsername.isNotEmpty()) {
                        uriHandler.safeOpenUri(context, "https://www.last.fm/user/$lastfmUsername")
                    }
                },
                enabled = isLoggedIn,
                color = if (isSystemInDarkTheme()) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isLoggedIn) 1f else 0.4f),
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .height(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.view_listening_history),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        painter = painterResource(R.drawable.hyper_link),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Bottom card: Login / Logout
            Surface(
                onClick = if (isLoggedIn) {
                    {
                        lastfmSession = ""
                        lastfmUsername = ""
                        lastfmAvatarUrl = ""
                        lastfmScrobbleCount = ""
                    }
                } else {
                    { showLoginDialog = true }
                },
                shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomEnd = 28.dp, bottomStart = 28.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(if (isLoggedIn) R.string.action_logout else R.string.action_login),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        ExpressiveSettingGroup(
            title = stringResource(R.string.options),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.check),
                    title = { Text(stringResource(R.string.enable_scrobbling)) },
                    description = { Text(stringResource(R.string.lastfm_enable_scrobbling_desc)) },
                    enabled = isLoggedIn,
                    trailingContent = {
                        androidx.compose.material3.Switch(
                            checked = lastfmScrobbling,
                            onCheckedChange = onlastfmScrobblingChange,
                            enabled = isLoggedIn,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lastfmScrobbling) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(androidx.compose.material3.SwitchDefaults.IconSize),
                                )
                            }
                        )
                    },
                    onClick = { onlastfmScrobblingChange(!lastfmScrobbling) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.play),
                    title = { Text(stringResource(R.string.lastfm_now_playing)) },
                    description = { Text(stringResource(R.string.lastfm_now_playing_desc)) },
                    enabled = isLoggedIn && lastfmScrobbling,
                    trailingContent = {
                        androidx.compose.material3.Switch(
                            checked = useNowPlaying,
                            onCheckedChange = onUseNowPlayingChange,
                            enabled = isLoggedIn && lastfmScrobbling,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (useNowPlaying) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(androidx.compose.material3.SwitchDefaults.IconSize),
                                )
                            }
                        )
                    },
                    onClick = { onUseNowPlayingChange(!useNowPlaying) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.thumb_up_like),
                    title = { Text(stringResource(R.string.last_fm_send_likes)) },
                    description = { Text(stringResource(R.string.last_fm_send_likes_description)) },
                    enabled = isLoggedIn,
                    trailingContent = {
                        androidx.compose.material3.Switch(
                            checked = useSendLikes,
                            onCheckedChange = onUseSendLikes,
                            enabled = isLoggedIn,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (useSendLikes) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(androidx.compose.material3.SwitchDefaults.IconSize),
                                )
                            }
                        )
                    },
                    onClick = { onUseSendLikes(!useSendLikes) }
                )
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // ----- Scrobbling configuration (steppers) -----
        Text(
            text = stringResource(R.string.scrobbling_configuration),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 6.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // Min track duration
            Surface(
                color = if (isSystemInDarkTheme()) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 5.dp, bottomStart = 5.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.scrobble_min_track_duration),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = stringResource(R.string.lastfm_min_duration_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Surface(
                            onClick = { onMinTrackDurationChange(LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION) },
                            enabled = minTrackDuration != LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION,
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.alpha(if (minTrackDuration == LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION) 0.4f else 1f),
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.replay),
                                    contentDescription = stringResource(R.string.reset),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Surface(
                            onClick = { onMinTrackDurationChange((minTrackDuration - 1).coerceAtLeast(10)) },
                            enabled = minTrackDuration > 10,
                            shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 5.dp, bottomEnd = 5.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .width(64.dp)
                                .height(52.dp)
                                .alpha(if (minTrackDuration > 10) 1f else 0.4f),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.remove),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = makeTimeString((minTrackDuration * 1000).toLong()),
                                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                            )
                        }
                        Surface(
                            onClick = { onMinTrackDurationChange((minTrackDuration + 1).coerceAtMost(60)) },
                            enabled = minTrackDuration < 60,
                            shape = RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp, topEnd = 22.dp, bottomEnd = 22.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .width(64.dp)
                                .height(52.dp)
                                .alpha(if (minTrackDuration < 60) 1f else 0.4f),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.add),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Scrobble delay percent
            Surface(
                color = if (isSystemInDarkTheme()) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.scrobble_delay_percent),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = stringResource(R.string.lastfm_delay_percent_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Surface(
                            onClick = { onScrobbleDelayPercentChange(LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT) },
                            enabled = scrobbleDelayPercent != LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT,
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.alpha(if (scrobbleDelayPercent == LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT) 0.4f else 1f),
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.replay),
                                    contentDescription = stringResource(R.string.reset),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Surface(
                            onClick = { onScrobbleDelayPercentChange((((scrobbleDelayPercent * 100f).roundToInt() - 5) / 100f).coerceIn(0.3f, 0.95f)) },
                            enabled = scrobbleDelayPercent > 0.301f,
                            shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 5.dp, bottomEnd = 5.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .width(64.dp)
                                .height(52.dp)
                                .alpha(if (scrobbleDelayPercent > 0.301f) 1f else 0.4f),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.remove),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.sensitivity_percentage, (scrobbleDelayPercent * 100).roundToInt()),
                                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                            )
                        }
                        Surface(
                            onClick = { onScrobbleDelayPercentChange((((scrobbleDelayPercent * 100f).roundToInt() + 5) / 100f).coerceIn(0.3f, 0.95f)) },
                            enabled = scrobbleDelayPercent < 0.949f,
                            shape = RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp, topEnd = 22.dp, bottomEnd = 22.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .width(64.dp)
                                .height(52.dp)
                                .alpha(if (scrobbleDelayPercent < 0.949f) 1f else 0.4f),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.add),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Scrobble delay minutes
            Surface(
                color = if (isSystemInDarkTheme()) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomEnd = 20.dp, bottomStart = 20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.scrobble_delay_minutes),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = stringResource(R.string.lastfm_delay_time_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Surface(
                            onClick = { onScrobbleDelaySecondsChange(LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS) },
                            enabled = scrobbleDelaySeconds != LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS,
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.alpha(if (scrobbleDelaySeconds == LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS) 0.4f else 1f),
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.replay),
                                    contentDescription = stringResource(R.string.reset),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Surface(
                            onClick = { onScrobbleDelaySecondsChange((scrobbleDelaySeconds - 10).coerceAtLeast(30)) },
                            enabled = scrobbleDelaySeconds > 30,
                            shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 5.dp, bottomEnd = 5.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .width(64.dp)
                                .height(52.dp)
                                .alpha(if (scrobbleDelaySeconds > 30) 1f else 0.4f),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.remove),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = makeTimeString((scrobbleDelaySeconds * 1000).toLong()),
                                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                            )
                        }
                        Surface(
                            onClick = { onScrobbleDelaySecondsChange((scrobbleDelaySeconds + 10).coerceAtMost(360)) },
                            enabled = scrobbleDelaySeconds < 360,
                            shape = RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp, topEnd = 22.dp, bottomEnd = 22.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .width(64.dp)
                                .height(52.dp)
                                .alpha(if (scrobbleDelaySeconds < 360) 1f else 0.4f),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.add),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(50.dp))
    }

    var showInfoSheet by rememberSaveable { mutableStateOf(false) }

    if (showInfoSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                // Header: cookie icon + title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialShapes.Cookie12Sided.toShape())
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lastfm),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.lastfm_about_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.lastfm_about_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Tip box
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
                            text = stringResource(R.string.lastfm_about_tip),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                // Connected info rows + "Got it" button
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = 5.dp, bottomStart = 5.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Text(stringResource(R.string.lastfm_about_history_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.lastfm_about_history_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Text(stringResource(R.string.lastfm_about_scrobbling_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.lastfm_about_scrobbling_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Text(stringResource(R.string.lastfm_about_now_playing_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.lastfm_about_now_playing_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Text(stringResource(R.string.lastfm_about_likes_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.lastfm_about_likes_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        onClick = {
                            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { showInfoSheet = false }
                        },
                        shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomEnd = 22.dp, bottomStart = 22.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.lastfm_got_it), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    CenterAlignedTopAppBar(
        title = { TitlePill(stringResource(R.string.lastfm_integration)) },
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
            MaterialIconButton(onClick = { showInfoSheet = true }) {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = stringResource(R.string.information),
                )
            }
        },
    )
}


