package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.*
import com.music.vivi.ui.component.*
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RomanizationSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current

    val (lyricsRomanizeJapanese, onLyricsRomanizeJapaneseChange) = rememberPreference(LyricsRomanizeJapaneseKey, defaultValue = true)
    val (lyricsRomanizeKorean, onLyricsRomanizeKoreanChange) = rememberPreference(LyricsRomanizeKoreanKey, defaultValue = true)
    val (lyricsRomanizeRussian, onLyricsRomanizeRussianChange) = rememberPreference(LyricsRomanizeRussianKey, defaultValue = true)
    val (lyricsRomanizeUkrainian, onLyricsRomanizeUkrainianChange) = rememberPreference(LyricsRomanizeUkrainianKey, defaultValue = true)
    val (lyricsRomanizeSerbian, onLyricsRomanizeSerbianChange) = rememberPreference(LyricsRomanizeSerbianKey, defaultValue = true)
    val (lyricsRomanizeBulgarian, onLyricsRomanizeBulgarianChange) = rememberPreference(LyricsRomanizeBulgarianKey, defaultValue = true)
    val (lyricsRomanizeBelarusian, onLyricsRomanizeBelarusianChange) = rememberPreference(LyricsRomanizeBelarusianKey, defaultValue = true)
    val (lyricsRomanizeKyrgyz, onLyricsRomanizeKyrgyzChange) = rememberPreference(LyricsRomanizeKyrgyzKey, defaultValue = true)
    val (lyricsRomanizeMacedonian, onLyricsRomanizeMacedonianChange) = rememberPreference(LyricsRomanizeMacedonianKey, defaultValue = true)
    val (lyricsRomanizeCyrillicByLine, onLyricsRomanizeCyrillicByLineChange) = rememberPreference(LyricsRomanizeCyrillicByLineKey, defaultValue = false)
    val (showDialog, setShowDialog) = remember { mutableStateOf(false) }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        PreferenceGroupTitle(title = stringResource(R.string.general))

        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_romanize_japanese)) },
            icon = { Icon(painterResource(R.drawable.language_japanese_latin), null) },
            checked = lyricsRomanizeJapanese,
            onCheckedChange = onLyricsRomanizeJapaneseChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_romanize_korean)) },
            icon = { Icon(painterResource(R.drawable.language_korean_latin), null) },
            checked = lyricsRomanizeKorean,
            onCheckedChange = onLyricsRomanizeKoreanChange,
        )

        PreferenceGroupTitle(title = stringResource(R.string.lyrics_romanization_cyrillic))
        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_romanize_russian)) },
            icon = { Icon(painterResource(R.drawable.alphabet_cyrillic), null) },
            checked = lyricsRomanizeRussian,
            onCheckedChange = onLyricsRomanizeRussianChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_romanize_ukrainian)) },
            icon = { Icon(painterResource(R.drawable.alphabet_cyrillic), null) },
            checked = lyricsRomanizeUkrainian,
            onCheckedChange = onLyricsRomanizeUkrainianChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_romanize_serbian)) },
            icon = { Icon(painterResource(R.drawable.alphabet_cyrillic), null) },
            checked = lyricsRomanizeSerbian,
            onCheckedChange = onLyricsRomanizeSerbianChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_romanize_bulgarian)) },
            icon = { Icon(painterResource(R.drawable.alphabet_cyrillic), null) },
            checked = lyricsRomanizeBulgarian,
            onCheckedChange = onLyricsRomanizeBulgarianChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_romanize_belarusian)) },
            icon = { Icon(painterResource(R.drawable.alphabet_cyrillic), null) },
            checked = lyricsRomanizeBelarusian,
            onCheckedChange = onLyricsRomanizeBelarusianChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_romanize_kyrgyz)) },
            icon = { Icon(painterResource(R.drawable.alphabet_cyrillic), null) },
            checked = lyricsRomanizeKyrgyz,
            onCheckedChange = onLyricsRomanizeKyrgyzChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_romanize_macedonian)) },
            icon = { Icon(painterResource(R.drawable.alphabet_cyrillic), null) },
            checked = lyricsRomanizeMacedonian,
            onCheckedChange = onLyricsRomanizeMacedonianChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.line_by_line_option_title)) },
            icon = { Icon(painterResource(R.drawable.warning), null) },
            description = stringResource(R.string.line_by_line_option_desc),
            checked = lyricsRomanizeCyrillicByLine,
            onCheckedChange = {
                if (it) {
                    setShowDialog(true)
                } else {
                    onLyricsRomanizeCyrillicByLineChange(false)
                }
            }
        )
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
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}
