package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.*
import com.music.vivi.ui.component.*
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference

/**
 * Screen for configuring lyrics romanization settings.
 * Allows users to enable romanization for various languages (Japanese, Korean, Cyrillic, etc.).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RomanizationSettings(
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

    val (lyricsRomanizeJapanese, onLyricsRomanizeJapaneseChange) = rememberPreference(
        LyricsRomanizeJapaneseKey,
        defaultValue = true
    )
    val (lyricsRomanizeKorean, onLyricsRomanizeKoreanChange) = rememberPreference(
        LyricsRomanizeKoreanKey,
        defaultValue = true
    )
    val (lyricsRomanizeDevanagari, onLyricsRomanizeDevangariChange) = rememberPreference(
        LyricsRomanizeDevanagariKey,
        defaultValue = true
    )
    val (lyricsRomanizeRussian, onLyricsRomanizeRussianChange) = rememberPreference(
        LyricsRomanizeRussianKey,
        defaultValue = true
    )
    val (lyricsRomanizeUkrainian, onLyricsRomanizeUkrainianChange) = rememberPreference(
        LyricsRomanizeUkrainianKey,
        defaultValue = true
    )
    val (lyricsRomanizeSerbian, onLyricsRomanizeSerbianChange) = rememberPreference(
        LyricsRomanizeSerbianKey,
        defaultValue = true
    )
    val (lyricsRomanizeBulgarian, onLyricsRomanizeBulgarianChange) = rememberPreference(
        LyricsRomanizeBulgarianKey,
        defaultValue = true
    )
    val (lyricsRomanizeBelarusian, onLyricsRomanizeBelarusianChange) = rememberPreference(
        LyricsRomanizeBelarusianKey,
        defaultValue = true
    )
    val (lyricsRomanizeKyrgyz, onLyricsRomanizeKyrgyzChange) = rememberPreference(
        LyricsRomanizeKyrgyzKey,
        defaultValue = true
    )
    val (lyricsRomanizeMacedonian, onLyricsRomanizeMacedonianChange) = rememberPreference(
        LyricsRomanizeMacedonianKey,
        defaultValue = true
    )
    val (lyricsRomanizeCyrillicByLine, onLyricsRomanizeCyrillicByLineChange) = rememberPreference(
        LyricsRomanizeCyrillicByLineKey,
        defaultValue = false
    )
    val (showDialog, setShowDialog) = remember { mutableStateOf(false) }

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

        PreferenceGroupTitle(title = stringResource(R.string.general))

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
                        icon = {
                            Icon(
                                painterResource(R.drawable.language_japanese_latin),
                                null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        title = stringResource(R.string.lyrics_romanize_japanese),
                        subtitle = stringResource(R.string.romanize_japanese_lyrics_subtitle),
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        modifier = Modifier.weight(1f)
                    )
                    ModernSwitch(
                        checked = lyricsRomanizeJapanese,
                        onCheckedChange = onLyricsRomanizeJapaneseChange,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModernInfoItem(
                        icon = {
                            Icon(
                                painterResource(R.drawable.language_korean_latin),
                                null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        title = stringResource(R.string.lyrics_romanize_korean),
                        subtitle = stringResource(R.string.romanize_korean_lyrics_subtitle),
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        modifier = Modifier.weight(1f)
                    )
                    ModernSwitch(
                        checked = lyricsRomanizeKorean,
                        onCheckedChange = onLyricsRomanizeKoreanChange,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModernInfoItem(
                        icon = {
                            Icon(
                                painterResource(R.drawable.language_korean_latin),
                                null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        title = stringResource(R.string.lyrics_romanize_devanagari),
                        subtitle = "Romanize devanagari lyrics",
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        modifier = Modifier.weight(1f)
                    )
                    ModernSwitch(
                        checked = lyricsRomanizeDevanagari,
                        onCheckedChange = onLyricsRomanizeDevangariChange,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            }
        }

        PreferenceGroupTitle(title = stringResource(R.string.lyrics_romanization_cyrillic))

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
                val cyrillicItems = listOf(
                    Triple(R.string.lyrics_romanize_russian, lyricsRomanizeRussian, onLyricsRomanizeRussianChange),
                    Triple(
                        R.string.lyrics_romanize_ukrainian,
                        lyricsRomanizeUkrainian,
                        onLyricsRomanizeUkrainianChange
                    ),
                    Triple(R.string.lyrics_romanize_serbian, lyricsRomanizeSerbian, onLyricsRomanizeSerbianChange),
                    Triple(
                        R.string.lyrics_romanize_bulgarian,
                        lyricsRomanizeBulgarian,
                        onLyricsRomanizeBulgarianChange
                    ),
                    Triple(
                        R.string.lyrics_romanize_belarusian,
                        lyricsRomanizeBelarusian,
                        onLyricsRomanizeBelarusianChange
                    ),
                    Triple(R.string.lyrics_romanize_kyrgyz, lyricsRomanizeKyrgyz, onLyricsRomanizeKyrgyzChange),
                    Triple(
                        R.string.lyrics_romanize_macedonian,
                        lyricsRomanizeMacedonian,
                        onLyricsRomanizeMacedonianChange
                    )
                )

                cyrillicItems.forEachIndexed { index, (titleRes, checked, onCheckedChange) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ModernInfoItem(
                            icon = {
                                Icon(
                                    painterResource(R.drawable.alphabet_cyrillic),
                                    null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            title = stringResource(titleRes),
                            subtitle = stringResource(R.string.romanize_cyrillic_lyrics_subtitle),
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor,
                            modifier = Modifier.weight(1f)
                        )
                        ModernSwitch(
                            checked = checked,
                            onCheckedChange = onCheckedChange,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }

                    if (index < cyrillicItems.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        PreferenceGroupTitle(title = stringResource(R.string.options))

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
                        icon = { Icon(painterResource(R.drawable.warning), null, modifier = Modifier.size(22.dp)) },
                        title = stringResource(R.string.line_by_line_option_title),
                        subtitle = stringResource(R.string.line_by_line_option_desc),
                        iconBackgroundColor = iconBgColor,
                        iconContentColor = iconStyleColor,
                        modifier = Modifier.weight(1f)
                    )
                    ModernSwitch(
                        checked = lyricsRomanizeCyrillicByLine,
                        onCheckedChange = {
                            if (it) {
                                setShowDialog(true)
                            } else {
                                onLyricsRomanizeCyrillicByLineChange(false)
                            }
                        },
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            }
        }

        if (showDialog) {
            ActionPromptDialog(
                title = stringResource(R.string.line_by_line_dialog_title),
                onDismiss = { setShowDialog(false) },
                onConfirm = {
                    onLyricsRomanizeCyrillicByLineChange(true)
                    setShowDialog(false)
                },
                onCancel = { setShowDialog(false) },
                content = {
                    Text(stringResource(R.string.line_by_line_dialog_desc))
                }
            )
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.lyrics_romanize_title)) },
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
