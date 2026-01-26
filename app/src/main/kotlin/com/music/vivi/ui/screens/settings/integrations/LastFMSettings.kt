package com.music.vivi.ui.screens.settings.integrations

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.lastfm.LastFM
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.EnableLastFMScrobblingKey
import com.music.vivi.constants.LastFMSessionKey
import com.music.vivi.constants.LastFMUseNowPlaying
import com.music.vivi.constants.LastFMUsernameKey
import com.music.vivi.constants.ScrobbleDelayPercentKey
import com.music.vivi.constants.ScrobbleDelaySecondsKey
import com.music.vivi.constants.ScrobbleMinSongDurationKey
import com.music.vivi.constants.SettingsShapeColorTertiaryKey
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.PreferenceGroupTitle
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.utils.makeTimeString
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.utils.reportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Screen for configuring Last.fm scrobbling integration.
 * Allows login, enabling scrobbling/now playing, and tweaking scrobble delay parameters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastFMSettings(
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

    val coroutineScope = rememberCoroutineScope()

    var lastfmUsername by rememberPreference(LastFMUsernameKey, "")
    var lastfmSession by rememberPreference(LastFMSessionKey, "")

    val integrationsViewModel: com.music.vivi.viewmodels.IntegrationsViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
    val lastFmState: com.music.vivi.viewmodels.LastFMState by integrationsViewModel.lastFmState.collectAsState()

    val isLoggedIn = lastFmState.isLoggedIn

    val (useNowPlaying, onUseNowPlayingChange) = rememberPreference(
        key = LastFMUseNowPlaying,
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
    var showScrobbleDelayPercentDialog by rememberSaveable { mutableStateOf(false) }
    var showScrobbleDelaySecondsDialog by rememberSaveable { mutableStateOf(false) }

    if (showLoginDialog) {
        var tempUsername by rememberSaveable { mutableStateOf("") }
        var tempPassword by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            title = { Text(stringResource(R.string.login)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tempUsername,
                        onValueChange = { tempUsername = it },
                        label = { Text(stringResource(R.string.username)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = { tempPassword = it },
                        label = { Text(stringResource(R.string.password)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            LastFM.getMobileSession(tempUsername, tempPassword)
                                .onSuccess {
                                    lastfmUsername = it.session.name
                                    lastfmSession = it.session.key
                                }
                                .onFailure {
                                    reportException(it)
                                }
                        }
                        showLoginDialog = false
                    }
                ) {
                    Text(stringResource(R.string.login))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLoginDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
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
                    icon = { Icon(painterResource(R.drawable.music_note), null, modifier = Modifier.size(22.dp)) },
                    title = if (isLoggedIn) lastfmUsername else stringResource(R.string.not_logged_in),
                    subtitle = if (isLoggedIn) {
                        stringResource(
                            R.string.lastfm_account
                        )
                    } else {
                        stringResource(R.string.not_logged_in_fallback)
                    },
                    onClick = {
                        if (isLoggedIn) {
                            lastfmSession = ""
                            lastfmUsername = ""
                        } else {
                            showLoginDialog = true
                        }
                    },
                    showArrow = true,
                    iconBackgroundColor = iconBgColor,
                    iconContentColor = iconStyleColor
                )
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
                        icon = { Icon(painterResource(R.drawable.music_note), null, modifier = Modifier.size(22.dp)) },
                        title = stringResource(R.string.enable_scrobbling),
                        subtitle = stringResource(R.string.submit_playback_history_lastfm),
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        modifier = Modifier.weight(1f)
                    )
                    ModernSwitch(
                        checked = lastfmScrobbling,
                        onCheckedChange = onlastfmScrobblingChange,
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
                        title = stringResource(R.string.lastfm_now_playing),
                        subtitle = stringResource(R.string.show_current_song_lastfm_profile),
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        modifier = Modifier.weight(1f)
                    )
                    ModernSwitch(
                        checked = useNowPlaying,
                        onCheckedChange = onUseNowPlayingChange,
                        enabled = isLoggedIn && lastfmScrobbling,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            }
        }

        PreferenceGroupTitle(
            title = stringResource(R.string.scrobbling_configuration)
        )

        var showMinTrackDurationDialog by rememberSaveable { mutableStateOf(false) }

        if (showMinTrackDurationDialog) {
            var tempMinTrackDuration by remember { mutableIntStateOf(minTrackDuration) }

            DefaultDialog(
                onDismiss = {
                    tempMinTrackDuration = minTrackDuration
                    showMinTrackDurationDialog = false
                },
                buttons = {
                    TextButton(
                        onClick = {
                            tempMinTrackDuration = LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION
                        }
                    ) {
                        Text(stringResource(R.string.reset))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = {
                            tempMinTrackDuration = minTrackDuration
                            showMinTrackDurationDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            onMinTrackDurationChange(tempMinTrackDuration)
                            showMinTrackDurationDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.scrobble_min_track_duration),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = makeTimeString((tempMinTrackDuration * 1000).toLong()),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Slider(
                        value = tempMinTrackDuration.toFloat(),
                        onValueChange = { tempMinTrackDuration = it.toInt() },
                        valueRange = 10f..60f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (showScrobbleDelayPercentDialog) {
            var tempScrobbleDelayPercent by remember { mutableFloatStateOf(scrobbleDelayPercent) }

            DefaultDialog(
                onDismiss = {
                    tempScrobbleDelayPercent = scrobbleDelayPercent
                    showScrobbleDelayPercentDialog = false
                },
                buttons = {
                    TextButton(
                        onClick = {
                            tempScrobbleDelayPercent = LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT
                        }
                    ) {
                        Text(stringResource(R.string.reset))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = {
                            tempScrobbleDelayPercent = scrobbleDelayPercent
                            showScrobbleDelayPercentDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            onScrobbleDelayPercentChange(tempScrobbleDelayPercent)
                            showScrobbleDelayPercentDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.scrobble_delay_percent),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = stringResource(
                            R.string.sensitivity_percentage,
                            (tempScrobbleDelayPercent * 100).roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Slider(
                        value = tempScrobbleDelayPercent,
                        onValueChange = { tempScrobbleDelayPercent = it },
                        valueRange = 0.5f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (showScrobbleDelaySecondsDialog) {
            var tempScrobbleDelaySeconds by remember { mutableIntStateOf(scrobbleDelaySeconds) }

            DefaultDialog(
                onDismiss = {
                    tempScrobbleDelaySeconds = scrobbleDelaySeconds
                    showScrobbleDelaySecondsDialog = false
                },
                buttons = {
                    TextButton(
                        onClick = {
                            tempScrobbleDelaySeconds = LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS
                        }
                    ) {
                        Text(stringResource(R.string.reset))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = {
                            tempScrobbleDelaySeconds = scrobbleDelaySeconds
                            showScrobbleDelaySecondsDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            onScrobbleDelaySecondsChange(tempScrobbleDelaySeconds)
                            showScrobbleDelaySecondsDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.scrobble_delay_minutes),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = makeTimeString((tempScrobbleDelaySeconds * 1000).toLong()),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Slider(
                        value = tempScrobbleDelaySeconds.toFloat(),
                        onValueChange = { tempScrobbleDelaySeconds = it.toInt() },
                        valueRange = 0f..600f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

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
                    icon = { Icon(painterResource(R.drawable.timer), null, modifier = Modifier.size(22.dp)) },
                    title = stringResource(R.string.scrobble_min_track_duration),
                    subtitle = makeTimeString((minTrackDuration * 1000).toLong()),
                    onClick = { showMinTrackDurationDialog = true },
                    showArrow = true,
                    iconBackgroundColor = iconBgColor,
                    iconContentColor = iconStyleColor
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                ModernInfoItem(
                    icon = { Icon(painterResource(R.drawable.percent), null, modifier = Modifier.size(22.dp)) },
                    title = stringResource(R.string.scrobble_delay_percent),
                    subtitle = stringResource(
                        R.string.sensitivity_percentage,
                        (scrobbleDelayPercent * 100).roundToInt()
                    ),
                    onClick = { showScrobbleDelayPercentDialog = true },
                    showArrow = true,
                    iconBackgroundColor = iconBgColor,
                    iconContentColor = iconStyleColor
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                ModernInfoItem(
                    icon = { Icon(painterResource(R.drawable.timer_off), null, modifier = Modifier.size(22.dp)) },
                    title = stringResource(R.string.scrobble_delay_minutes),
                    subtitle = makeTimeString((scrobbleDelaySeconds * 1000).toLong()),
                    onClick = { showScrobbleDelaySecondsDialog = true },
                    showArrow = true,
                    iconBackgroundColor = iconBgColor,
                    iconContentColor = iconStyleColor
                )
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.lastfm_integration)) },
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
