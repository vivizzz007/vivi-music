/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.LyricsRomanizeAsMainKey
import com.music.vivi.constants.LyricsRomanizeBelarusianKey
import com.music.vivi.constants.LyricsRomanizeBulgarianKey
import com.music.vivi.constants.LyricsRomanizeChineseKey
import com.music.vivi.constants.LyricsRomanizeHindiKey
import com.music.vivi.constants.LyricsRomanizePunjabiKey
import com.music.vivi.constants.LyricsRomanizeCyrillicByLineKey
import com.music.vivi.constants.LyricsRomanizeJapaneseKey
import com.music.vivi.constants.LyricsRomanizeKoreanKey
import com.music.vivi.constants.LyricsRomanizeKyrgyzKey
import com.music.vivi.constants.LyricsRomanizeMacedonianKey
import com.music.vivi.constants.LyricsRomanizeRussianKey
import com.music.vivi.constants.LyricsRomanizeSerbianKey
import com.music.vivi.constants.LyricsRomanizeUkrainianKey
import com.music.vivi.ui.component.ActionPromptDialog
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.Material3SettingsGroup
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RomanizationSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current

    val (lyricsRomanizeAsMain, onLyricsRomanizeAsMainChange) = rememberPreference(LyricsRomanizeAsMainKey, defaultValue = false)
    val (lyricsRomanizeJapanese, onLyricsRomanizeJapaneseChange) = rememberPreference(LyricsRomanizeJapaneseKey, defaultValue = true)
    val (lyricsRomanizeKorean, onLyricsRomanizeKoreanChange) = rememberPreference(LyricsRomanizeKoreanKey, defaultValue = true)
    val (lyricsRomanizeChinese, onLyricsRomanizeChineseChange) = rememberPreference(LyricsRomanizeChineseKey, defaultValue = true)
    val (lyricsRomanizeHindi, onLyricsRomanizeHindiChange) = rememberPreference(LyricsRomanizeHindiKey, defaultValue = true)
    val (lyricsRomanizePunjabi, onLyricsRomanizePunjabiChange) = rememberPreference(LyricsRomanizePunjabiKey, defaultValue = true)
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Material3SettingsGroup(
            title = stringResource(R.string.general),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_romanize_as_main)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsRomanizeAsMain,
                            onCheckedChange = onLyricsRomanizeAsMainChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsRomanizeAsMain) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsRomanizeAsMainChange(!lyricsRomanizeAsMain) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.language_japanese_latin),
                    title = { Text(stringResource(R.string.lyrics_romanize_japanese)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsRomanizeJapanese,
                            onCheckedChange = onLyricsRomanizeJapaneseChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsRomanizeJapanese) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsRomanizeJapaneseChange(!lyricsRomanizeJapanese) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.language_korean_latin),
                    title = { Text(stringResource(R.string.lyrics_romanize_korean)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsRomanizeKorean,
                            onCheckedChange = onLyricsRomanizeKoreanChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsRomanizeKorean) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsRomanizeKoreanChange(!lyricsRomanizeKorean) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.language),
                    title = { Text(stringResource(R.string.lyrics_romanize_chinese)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsRomanizeChinese,
                            onCheckedChange = onLyricsRomanizeChineseChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsRomanizeChinese) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsRomanizeChineseChange(!lyricsRomanizeChinese) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.language),
                    title = { Text(stringResource(R.string.lyrics_romanize_hindi)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsRomanizeHindi,
                            onCheckedChange = onLyricsRomanizeHindiChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsRomanizeHindi) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsRomanizeHindiChange(!lyricsRomanizeHindi) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.language),
                    title = { Text(stringResource(R.string.lyrics_romanize_punjabi)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsRomanizePunjabi,
                            onCheckedChange = onLyricsRomanizePunjabiChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsRomanizePunjabi) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsRomanizePunjabiChange(!lyricsRomanizePunjabi) }
                )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.lyrics_romanization_cyrillic),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.alphabet_cyrillic),
                    title = { Text(stringResource(R.string.lyrics_romanize_russian)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsRomanizeRussian,
                            onCheckedChange = onLyricsRomanizeRussianChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsRomanizeRussian) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsRomanizeRussianChange(!lyricsRomanizeRussian) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.alphabet_cyrillic),
                    title = { Text(stringResource(R.string.lyrics_romanize_ukrainian)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsRomanizeUkrainian,
                            onCheckedChange = onLyricsRomanizeUkrainianChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsRomanizeUkrainian) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsRomanizeUkrainianChange(!lyricsRomanizeUkrainian) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.alphabet_cyrillic),
                    title = { Text(stringResource(R.string.lyrics_romanize_serbian)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsRomanizeSerbian,
                            onCheckedChange = onLyricsRomanizeSerbianChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsRomanizeSerbian) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsRomanizeSerbianChange(!lyricsRomanizeSerbian) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.alphabet_cyrillic),
                    title = { Text(stringResource(R.string.lyrics_romanize_bulgarian)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsRomanizeBulgarian,
                            onCheckedChange = onLyricsRomanizeBulgarianChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsRomanizeBulgarian) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsRomanizeBulgarianChange(!lyricsRomanizeBulgarian) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.alphabet_cyrillic),
                    title = { Text(stringResource(R.string.lyrics_romanize_belarusian)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsRomanizeBelarusian,
                            onCheckedChange = onLyricsRomanizeBelarusianChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsRomanizeBelarusian) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsRomanizeBelarusianChange(!lyricsRomanizeBelarusian) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.alphabet_cyrillic),
                    title = { Text(stringResource(R.string.lyrics_romanize_kyrgyz)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsRomanizeKyrgyz,
                            onCheckedChange = onLyricsRomanizeKyrgyzChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsRomanizeKyrgyz) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsRomanizeKyrgyzChange(!lyricsRomanizeKyrgyz) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.alphabet_cyrillic),
                    title = { Text(stringResource(R.string.lyrics_romanize_macedonian)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsRomanizeMacedonian,
                            onCheckedChange = onLyricsRomanizeMacedonianChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsRomanizeMacedonian) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsRomanizeMacedonianChange(!lyricsRomanizeMacedonian) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.warning),
                    title = { Text(stringResource(R.string.line_by_line_option_title)) },
                    description = { Text(stringResource(R.string.line_by_line_option_desc)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsRomanizeCyrillicByLine,
                            onCheckedChange = {
                                if (it) {
                                    setShowDialog(true)
                                } else {
                                    onLyricsRomanizeCyrillicByLineChange(false)
                                }
                            },
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsRomanizeCyrillicByLine) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = {
                        if (!lyricsRomanizeCyrillicByLine) {
                            setShowDialog(true)
                        } else {
                            onLyricsRomanizeCyrillicByLineChange(false)
                        }
                    }
                )
            )
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
