package com.music.vivi.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.music.innertube.utils.parseCookieString
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.AddingPlayedSongsToYTMHistoryKey
import com.music.vivi.constants.AudioNormalizationKey
import com.music.vivi.constants.AudioOffload
import com.music.vivi.constants.AudioQuality
import com.music.vivi.constants.AudioQualityKey
import com.music.vivi.constants.AutoLoadMoreKey
import com.music.vivi.constants.AutoSkipNextOnErrorKey
import com.music.vivi.constants.CrossfadeDurationKey
import com.music.vivi.constants.CrossfadeEnabledKey
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.PersistentQueueKey
import com.music.vivi.constants.SkipSilenceKey
import com.music.vivi.constants.StopMusicOnTaskClearKey
import com.music.vivi.constants.minPlaybackDurKey
import com.music.vivi.ui.component.CounterDialog
import com.music.vivi.ui.component.EnumListPreference
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.PreferenceEntry
import com.music.vivi.ui.component.PreferenceGroupTitle
import com.music.vivi.ui.component.SwitchPreference
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference








@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    // Existing preferences
    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(AudioQualityKey, defaultValue = AudioQuality.AUTO)
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(PersistentQueueKey, defaultValue = true)
    val (skipSilence, onSkipSilenceChange) = rememberPreference(SkipSilenceKey, defaultValue = false)
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(AudioNormalizationKey, defaultValue = true)
    val (autoSkipNextOnError, onAutoSkipNextOnErrorChange) = rememberPreference(AutoSkipNextOnErrorKey, defaultValue = false)
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(StopMusicOnTaskClearKey, defaultValue = false)
    val (autoLoadMore, onAutoLoadMoreChange) = rememberPreference(AutoLoadMoreKey, defaultValue = true)
    val (minPlaybackDur, onMinPlaybackDurChange) = rememberPreference(minPlaybackDurKey, defaultValue = 30)
    val (audioOffload, onAudioOffloadChange) = rememberPreference(AudioOffload, defaultValue = false)
    val (addingPlayedSongsToYtmHistory, onAddingPlayedSongsToYtmHistoryChange) = rememberPreference(AddingPlayedSongsToYTMHistoryKey, defaultValue = true)
    val (crossfadeEnabled, onCrossfadeEnabledChange) = rememberPreference(CrossfadeEnabledKey, defaultValue = false)
    val (crossfadeDuration, onCrossfadeDurationChange) = rememberPreference(CrossfadeDurationKey, defaultValue = 5)

    // New preference for auto-pause feature
    val AutoPauseOnVolumeZeroKey = booleanPreferencesKey("auto_pause_on_volume_zero")

    val (autoPauseOnVolumeZero, onAutoPauseOnVolumeZeroChange) = rememberPreference(AutoPauseOnVolumeZeroKey, defaultValue = true)

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    var showMinPlaybackDur by remember { mutableStateOf(false) }
    var showCrossfadeDialog by remember { mutableStateOf(false) }

    if (showMinPlaybackDur) {
        CounterDialog(
            title = stringResource(R.string.minimum_playback_duration),
            description = stringResource(R.string.minimum_playback_duration_info),
            initialValue = minPlaybackDur,
            upperBound = 100,
            lowerBound = 0,
            resetValue = 30,
            unitDisplay = "%",
            onDismiss = { showMinPlaybackDur = false },
            onConfirm = {
                showMinPlaybackDur = false
                onMinPlaybackDurChange(it)
            },
            onCancel = { showMinPlaybackDur = false },
            onReset = { onMinPlaybackDurChange(30) },
        )
    }

    if (showCrossfadeDialog) {
        CounterDialog(
            title = stringResource(R.string.crossfade_duration),
            description = stringResource(R.string.crossfade_duration_description),
            initialValue = crossfadeDuration,
            upperBound = 15,
            lowerBound = 1,
            resetValue = 5,
            unitDisplay = stringResource(R.string.seconds),
            onDismiss = { showCrossfadeDialog = false },
            onConfirm = {
                showCrossfadeDialog = false
                onCrossfadeDurationChange(it)
            },
            onCancel = { showCrossfadeDialog = false },
            onReset = { onCrossfadeDurationChange(5) },
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))

        // Lottie Animation
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.party))
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        PreferenceGroupTitle(title = stringResource(R.string.player))

        // Audio Quality Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            EnumListPreference(
                title = { Text(stringResource(R.string.audio_quality)) },
                icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                selectedValue = audioQuality,
                onValueSelected = onAudioQualityChange,
                valueText = {
                    when (it) {
                        AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                        AudioQuality.MAX -> stringResource(R.string.audio_quality_max)
                        AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                        AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                    }
                },
                modifier = Modifier.padding(16.dp)
            )
        }

        // Lyrics Settings Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.lyrics_settings_title)) },
                icon = { Icon(Icons.Rounded.Lyrics, null) },
                onClick = { navController.navigate("settings/player/lyrics") },
                modifier = Modifier.padding(16.dp)
            )
        }

        // Local Player Settings Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.local_player_settings_title)) },
                icon = { Icon(painterResource(R.drawable.folder_icon), null) },
                onClick = { navController.navigate("player/local") },
                modifier = Modifier.padding(16.dp)
            )
        }

        // Minimum Playback Duration Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.minimum_playback_duration)) },
                description = "$minPlaybackDur %",
                icon = { Icon(Icons.Rounded.Sync, null) },
                onClick = { showMinPlaybackDur = true },
                modifier = Modifier.padding(16.dp)
            )
        }

        // Skip Silence Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            SwitchPreference(
                title = { Text(stringResource(R.string.skip_silence)) },
                icon = { Icon(painterResource(R.drawable.fast_forward), null) },
                checked = skipSilence,
                onCheckedChange = onSkipSilenceChange,
                modifier = Modifier.padding(16.dp)
            )
        }

        // YTM History Card (conditionally shown)
        if (isLoggedIn) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                SwitchPreference(
                    title = { Text(stringResource(R.string.adding_played_songs_to_ytm_history)) },
                    icon = { Icon(painterResource(R.drawable.history), null) },
                    checked = addingPlayedSongsToYtmHistory,
                    onCheckedChange = onAddingPlayedSongsToYtmHistoryChange,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Audio Normalization Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            SwitchPreference(
                title = { Text(stringResource(R.string.audio_normalization)) },
                icon = { Icon(Icons.AutoMirrored.Rounded.VolumeUp, null) },
                checked = audioNormalization,
                onCheckedChange = onAudioNormalizationChange,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Auto Pause on Volume Zero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            SwitchPreference(
                title = { Text(stringResource(R.string.auto_pause_on_volume_zero)) },
                description = stringResource(R.string.auto_pause_on_volume_zero_desc),
                icon = { Icon(painterResource(R.drawable.ic_headphone), null) },
                checked = autoPauseOnVolumeZero,
                onCheckedChange = onAutoPauseOnVolumeZeroChange,
                modifier = Modifier.padding(16.dp)
            )
        }

        PreferenceGroupTitle(title = stringResource(R.string.playback_effects))

        // Crossfade Enable Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            SwitchPreference(
                title = { Text(stringResource(R.string.enable_crossfade)) },
                icon = { Icon(painterResource(R.drawable.ic_crossfade), null) },
                checked = crossfadeEnabled,
                onCheckedChange = onCrossfadeEnabledChange,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Crossfade Duration Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            PreferenceEntry(
                title = {
                    Text(
                        stringResource(R.string.crossfade_duration),
                        color = if (crossfadeEnabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.5f)
                    )
                },
                description = if (crossfadeEnabled)
                    stringResource(R.string.seconds_format, crossfadeDuration)
                else
                    stringResource(R.string.disabled),
                icon = {
                    Icon(
                        painterResource(R.drawable.timer_crossfade),
                        null,
                        tint = if (crossfadeEnabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.5f)
                    )
                },
                onClick = { if (crossfadeEnabled) showCrossfadeDialog = true },
                modifier = Modifier.padding(16.dp)
            )
        }

        PreferenceGroupTitle(title = stringResource(R.string.queue))

        // Persistent Queue Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            SwitchPreference(
                title = { Text(stringResource(R.string.persistent_queue)) },
                description = stringResource(R.string.persistent_queue_desc),
                icon = { Icon(painterResource(R.drawable.queue_music), null) },
                checked = persistentQueue,
                onCheckedChange = onPersistentQueueChange,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Auto Load More Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            SwitchPreference(
                title = { Text(stringResource(R.string.auto_load_more)) },
                description = stringResource(R.string.auto_load_more_desc),
                icon = { Icon(painterResource(R.drawable.playlist_add), null) },
                checked = autoLoadMore,
                onCheckedChange = onAutoLoadMoreChange,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Auto Skip Next on Error Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            SwitchPreference(
                title = { Text(stringResource(R.string.auto_skip_next_on_error)) },
                description = stringResource(R.string.auto_skip_next_on_error_desc),
                icon = { Icon(painterResource(R.drawable.skip_next), null) },
                checked = autoSkipNextOnError,
                onCheckedChange = onAutoSkipNextOnErrorChange,
                modifier = Modifier.padding(16.dp)
            )
        }

        PreferenceGroupTitle(title = stringResource(R.string.misc))

        // Audio Offload Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            SwitchPreference(
                title = { Text(stringResource(R.string.audio_offload)) },
                description = stringResource(R.string.audio_offload_description),
                icon = { Icon(Icons.Rounded.Bolt, null) },
                checked = audioOffload,
                onCheckedChange = onAudioOffloadChange,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Stop Music on Task Clear Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            SwitchPreference(
                title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
                icon = { Icon(painterResource(R.drawable.clear_all), null) },
                checked = stopMusicOnTaskClear,
                onCheckedChange = onStopMusicOnTaskClearChange,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.player_and_audio)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(painterResource(R.drawable.back_icon), contentDescription = null)
            }
        },
        scrollBehavior = scrollBehavior
    )
}
