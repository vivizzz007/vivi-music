package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.AudioNormalizationKey
import com.music.vivi.constants.AudioQuality
import com.music.vivi.constants.AudioQualityKey
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
import kotlin.math.roundToInt
import com.music.vivi.ui.component.PreferenceEntry
import com.music.vivi.ui.component.PreferenceGroupTitle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.music.vivi.constants.EnableDoubleTapGesturesKey
import com.music.vivi.constants.EnableSwipeGesturesKey
import com.music.vivi.update.experiment.SheetDragHandle
import com.music.vivi.update.mordernswitch.ModernSwitch

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
    val (seekExtraSeconds, onSeekExtraSeconds) = rememberPreference(
        SeekExtraSeconds,
        defaultValue = false
    )
    //double tap and pause for the lyrics
    val (enableSwipeGestures, onEnableSwipeGesturesChange) = rememberPreference(
        EnableSwipeGesturesKey,
        defaultValue = true
    )
    val (enableDoubleTapGestures, onEnableDoubleTapGesturesChange) = rememberPreference(
        EnableDoubleTapGesturesKey,
        defaultValue = true
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

    val scrollState = rememberScrollState()

    // Calculate scroll-based animations
    val titleAlpha by remember {
        derivedStateOf {
            1f - (scrollState.value / 200f).coerceIn(0f, 1f)
        }
    }

    val titleScale by remember {
        derivedStateOf {
            1f - (scrollState.value / 400f).coerceIn(0f, 0.3f)
        }
    }

    // Bottom sheet states
    val audioQualitySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAudioQualitySheet by remember { mutableStateOf(false) }

    val historySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showHistorySheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                // Header Section with scroll animations
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .graphicsLayer {
                            alpha = titleAlpha
                            scaleX = titleScale
                            scaleY = titleScale
                        }
                ) {
                    Text(
                        text = stringResource(R.string.player_and_audio),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            item {
                // Lottie Animation Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.internalplayer))
                    LottieAnimation(
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                SettingsSection(stringResource(R.string.player)) {
                    SettingsListItem(
                        title = stringResource(R.string.audio_quality),
                        subtitle = when (audioQuality) {
                            AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                            AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                            AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                        },
                        onClick = { showAudioQualitySheet = true },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.graphic_eq),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.history_duration),
                        subtitle = "${historyDuration.toInt()} minutes",
                        onClick = { showHistorySheet = true },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.history),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.skip_silence),
                        subtitle = if (skipSilence) "Enabled" else "Disabled",
                        onClick = { onSkipSilenceChange(!skipSilence) },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.fast_forward),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = skipSilence,
                                onCheckedChange = onSkipSilenceChange
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.audio_normalization),
                        subtitle = if (audioNormalization) "Enabled" else "Disabled",
                        onClick = { onAudioNormalizationChange(!audioNormalization) },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.volume_up),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = audioNormalization,
                                onCheckedChange = onAudioNormalizationChange
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.seek_seconds_addup),
                        subtitle = stringResource(R.string.seek_seconds_addup_description),
                        onClick = { onSeekExtraSeconds(!seekExtraSeconds) },
                        isLast = true,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.arrow_forward),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = seekExtraSeconds,
                                onCheckedChange = onSeekExtraSeconds
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.gestures)) {
                    SettingsListItem(
                        title = "Swipe Gestures",
                        subtitle = "Swipe left/right to change songs",
                        onClick = { onEnableSwipeGesturesChange(!enableSwipeGestures) },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.swipe),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = enableSwipeGestures,
                                onCheckedChange = onEnableSwipeGesturesChange
                            )
                        }
                    )

                    SettingsListItem(
                        title = "Double Tap",
                        subtitle = "Double tap to pause/play",
                        onClick = { onEnableDoubleTapGesturesChange(!enableDoubleTapGestures) },
                        isLast = true,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.pause),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = enableDoubleTapGestures,
                                onCheckedChange = onEnableDoubleTapGesturesChange
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.queue)) {
                    SettingsListItem(
                        title = stringResource(R.string.persistent_queue),
                        subtitle = stringResource(R.string.persistent_queue_desc),
                        onClick = { onPersistentQueueChange(!persistentQueue) },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.queue_music),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = persistentQueue,
                                onCheckedChange = onPersistentQueueChange
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.auto_load_more),
                        subtitle = stringResource(R.string.auto_load_more_desc),
                        onClick = { onAutoLoadMoreChange(!autoLoadMore) },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.playlist_add),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = autoLoadMore,
                                onCheckedChange = onAutoLoadMoreChange
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.disable_load_more_when_repeat_all),
                        subtitle = stringResource(R.string.disable_load_more_when_repeat_all_desc),
                        onClick = { onDisableLoadMoreWhenRepeatAllChange(!disableLoadMoreWhenRepeatAll) },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.repeat),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = disableLoadMoreWhenRepeatAll,
                                onCheckedChange = onDisableLoadMoreWhenRepeatAllChange
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.auto_download_on_like),
                        subtitle = stringResource(R.string.auto_download_on_like_desc),
                        onClick = { onAutoDownloadOnLikeChange(!autoDownloadOnLike) },
                        isLast = true,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.download),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = autoDownloadOnLike,
                                onCheckedChange = onAutoDownloadOnLikeChange
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.content)) {
                    SettingsListItem(
                        title = stringResource(R.string.enable_similar_content),
                        subtitle = stringResource(R.string.similar_content_desc),
                        onClick = { similarContentEnabledChange(!similarContentEnabled) },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.similar),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = similarContentEnabled,
                                onCheckedChange = similarContentEnabledChange
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.auto_skip_next_on_error),
                        subtitle = stringResource(R.string.auto_skip_next_on_error_desc),
                        onClick = { onAutoSkipNextOnErrorChange(!autoSkipNextOnError) },
                        isLast = true,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.skip_next),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = autoSkipNextOnError,
                                onCheckedChange = onAutoSkipNextOnErrorChange
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.misc)) {
                    SettingsListItem(
                        title = stringResource(R.string.stop_music_on_task_clear),
                        subtitle = "Stop music when app is cleared from recents",
                        onClick = { onStopMusicOnTaskClearChange(!stopMusicOnTaskClear) },
                        isLast = true,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.clear_all),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = stopMusicOnTaskClear,
                                onCheckedChange = onStopMusicOnTaskClearChange
                            )
                        }
                    )
                }

            }

            item {
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }

    // History Duration Bottom Sheet
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = historySheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    stringResource(R.string.history_duration),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "How long to keep track of listening history",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "${historyDuration.toInt()} minutes",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Slider(
                            value = historyDuration,
                            onValueChange = onHistoryDurationChange,
                            valueRange = 1f..120f,
                            steps = 119,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showHistorySheet = false },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { showHistorySheet = false },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Audio Quality Bottom Sheet
    if (showAudioQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showAudioQualitySheet = false },
            sheetState = audioQualitySheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    stringResource(R.string.audio_quality),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose the audio quality for playback",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                AudioQuality.values().forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAudioQualityChange(quality)
                                showAudioQualitySheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = audioQuality == quality,
                            onClick = {
                                onAudioQualityChange(quality)
                                showAudioQualitySheet = false
                            }
                        )
                        Text(
                            text = when (quality) {
                                AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                                AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                                AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}




