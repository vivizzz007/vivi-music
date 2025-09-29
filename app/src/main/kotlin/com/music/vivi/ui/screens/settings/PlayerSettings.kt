package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.AudioNormalizationKey
import com.music.vivi.constants.AudioQuality
import com.music.vivi.constants.AudioQualityKey
import com.music.vivi.constants.AudioOffload
import com.music.vivi.constants.AutoDownloadOnLikeKey
import com.music.vivi.constants.AutoLoadMoreKey
import com.music.vivi.constants.DisableLoadMoreWhenRepeatAllKey
import com.music.vivi.constants.AutoSkipNextOnErrorKey
import com.music.vivi.constants.PersistentQueueKey
import com.music.vivi.constants.SimilarContent
import com.music.vivi.constants.SkipSilenceKey
import com.music.vivi.constants.StopMusicOnTaskClearKey
import com.music.vivi.constants.HistoryDuration
import com.music.vivi.constants.SeekExtraSeconds
import com.music.vivi.ui.component.EnumListPreference
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.PreferenceGroupTitle
import com.music.vivi.ui.component.SliderPreference
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
    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(
        AudioQualityKey,
        defaultValue = AudioQuality.AUTO
    )
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(
        PersistentQueueKey,
        defaultValue = true
    )
    val (skipSilence, onSkipSilenceChange) = rememberPreference(
        SkipSilenceKey,
        defaultValue = false
    )
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(
        AudioNormalizationKey,
        defaultValue = true
    )

    val (audioOffload, onAudioOffloadChange) = rememberPreference(
        key = AudioOffload,
        defaultValue = false
    )

    val (seekExtraSeconds, onSeekExtraSeconds) = rememberPreference(
        SeekExtraSeconds,
        defaultValue = false
    )

    val (autoLoadMore, onAutoLoadMoreChange) = rememberPreference(
        AutoLoadMoreKey,
        defaultValue = true
    )
    val (disableLoadMoreWhenRepeatAll, onDisableLoadMoreWhenRepeatAllChange) = rememberPreference(
        DisableLoadMoreWhenRepeatAllKey,
        defaultValue = false
    )
    val (autoDownloadOnLike, onAutoDownloadOnLikeChange) = rememberPreference(
        AutoDownloadOnLikeKey,
        defaultValue = false
    )
    val (similarContentEnabled, similarContentEnabledChange) = rememberPreference(
        key = SimilarContent,
        defaultValue = true
    )
    val (autoSkipNextOnError, onAutoSkipNextOnErrorChange) = rememberPreference(
        AutoSkipNextOnErrorKey,
        defaultValue = false
    )
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(
        StopMusicOnTaskClearKey,
        defaultValue = false
    )
    val (historyDuration, onHistoryDurationChange) = rememberPreference(
        HistoryDuration,
        defaultValue = 30f
    )

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
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
            title = stringResource(R.string.player)
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.audio_quality)) },
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            selectedValue = audioQuality,
            onValueSelected = onAudioQualityChange,
            valueText = {
                when (it) {
                    AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                    AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                    AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                }
            }
        )

        SliderPreference(
            title = { Text(stringResource(R.string.history_duration)) },
            icon = { Icon(painterResource(R.drawable.history), null) },
            value = historyDuration,
            onValueChange = onHistoryDurationChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.skip_silence)) },
            icon = { Icon(painterResource(R.drawable.fast_forward), null) },
            checked = skipSilence,
            onCheckedChange = onSkipSilenceChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.audio_normalization)) },
            icon = { Icon(painterResource(R.drawable.volume_up), null) },
            checked = audioNormalization,
            onCheckedChange = onAudioNormalizationChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.audio_offload)) },
            description = stringResource(R.string.audio_offload_description),
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            checked = audioOffload,
            onCheckedChange = onAudioOffloadChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.seek_seconds_addup)) },
            description = stringResource(R.string.seek_seconds_addup_description),
            icon = { Icon(painterResource(R.drawable.arrow_forward), null) },
            checked = seekExtraSeconds,
            onCheckedChange = onSeekExtraSeconds
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.queue)
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.persistent_queue)) },
            description = stringResource(R.string.persistent_queue_desc),
            icon = { Icon(painterResource(R.drawable.queue_music), null) },
            checked = persistentQueue,
            onCheckedChange = onPersistentQueueChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.auto_load_more)) },
            description = stringResource(R.string.auto_load_more_desc),
            icon = { Icon(painterResource(R.drawable.playlist_add), null) },
            checked = autoLoadMore,
            onCheckedChange = onAutoLoadMoreChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.disable_load_more_when_repeat_all)) },
            description = stringResource(R.string.disable_load_more_when_repeat_all_desc),
            icon = { Icon(painterResource(R.drawable.repeat), null) },
            checked = disableLoadMoreWhenRepeatAll,
            onCheckedChange = onDisableLoadMoreWhenRepeatAllChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.auto_download_on_like)) },
            description = stringResource(R.string.auto_download_on_like_desc),
            icon = { Icon(painterResource(R.drawable.download), null) },
            checked = autoDownloadOnLike,
            onCheckedChange = onAutoDownloadOnLikeChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_similar_content)) },
            description = stringResource(R.string.similar_content_desc),
            icon = { Icon(painterResource(R.drawable.similar), null) },
            checked = similarContentEnabled,
            onCheckedChange = similarContentEnabledChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.auto_skip_next_on_error)) },
            description = stringResource(R.string.auto_skip_next_on_error_desc),
            icon = { Icon(painterResource(R.drawable.skip_next), null) },
            checked = autoSkipNextOnError,
            onCheckedChange = onAutoSkipNextOnErrorChange
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.misc)
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
            icon = { Icon(painterResource(R.drawable.clear_all), null) },
            checked = stopMusicOnTaskClear,
            onCheckedChange = onStopMusicOnTaskClearChange
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.player_and_audio)) },
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
        }
    )
}
