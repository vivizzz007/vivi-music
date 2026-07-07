/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.AppFont
import com.music.vivi.constants.SelectedFontKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.Material3SettingsGroup
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.theme.GoogleSansFontFamily
import com.music.vivi.ui.theme.SansFlexFontFamily
import com.music.vivi.ui.theme.OutfitFontFamily
import com.music.vivi.ui.theme.PlusJakartaSansFontFamily
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSelectionScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (selectedFont, onSelectedFontChange) = rememberPreference(
        SelectedFontKey,
        defaultValue = AppFont.SYSTEM.value
    )

    val activeFontFamily = remember(selectedFont) {
        when (AppFont.fromValue(selectedFont)) {
            AppFont.SYSTEM -> FontFamily.Default
            AppFont.GOOGLE_SANS -> GoogleSansFontFamily
            AppFont.SANS_FLEX -> SansFlexFontFamily
            AppFont.OUTFIT -> OutfitFontFamily
            AppFont.PLUS_JAKARTA_SANS -> PlusJakartaSansFontFamily
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {

        // Typography Preview Card
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.typography_preview).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = stringResource(R.string.preview_text_quote),
                    fontFamily = activeFontFamily,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "Expressive typeface is applied to display, headlines, and titles. Body copy and labels remain in the system font for maximum readability.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Options settings group
        Material3SettingsGroup(
            title = stringResource(R.string.font_selection),
            items = listOf(
                Material3SettingsItem(
                    leadingContent = {
                        RadioButton(
                            selected = selectedFont == AppFont.SYSTEM.value,
                            onClick = null
                        )
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.font_system),
                            fontFamily = FontFamily.Default
                        )
                    },
                    description = {
                        Text(
                            text = stringResource(R.string.font_system_desc),
                            fontFamily = FontFamily.Default
                        )
                    },
                    onClick = { onSelectedFontChange(AppFont.SYSTEM.value) }
                ),
                Material3SettingsItem(
                    leadingContent = {
                        RadioButton(
                            selected = selectedFont == AppFont.GOOGLE_SANS.value,
                            onClick = null
                        )
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.font_google_sans),
                            fontFamily = GoogleSansFontFamily
                        )
                    },
                    description = {
                        Text(
                            text = stringResource(R.string.font_google_sans_desc),
                            fontFamily = GoogleSansFontFamily
                        )
                    },
                    onClick = { onSelectedFontChange(AppFont.GOOGLE_SANS.value) }
                ),
                Material3SettingsItem(
                    leadingContent = {
                        RadioButton(
                            selected = selectedFont == AppFont.SANS_FLEX.value,
                            onClick = null
                        )
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.font_sans_flex),
                            fontFamily = SansFlexFontFamily
                        )
                    },
                    description = {
                        Text(
                            text = stringResource(R.string.font_sans_flex_desc),
                            fontFamily = SansFlexFontFamily
                        )
                    },
                    onClick = { onSelectedFontChange(AppFont.SANS_FLEX.value) }
                ),
                Material3SettingsItem(
                    leadingContent = {
                        RadioButton(
                            selected = selectedFont == AppFont.OUTFIT.value,
                            onClick = null
                        )
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.font_outfit),
                            fontFamily = OutfitFontFamily
                        )
                    },
                    description = {
                        Text(
                            text = stringResource(R.string.font_outfit_desc),
                            fontFamily = OutfitFontFamily
                        )
                    },
                    onClick = { onSelectedFontChange(AppFont.OUTFIT.value) }
                ),
                Material3SettingsItem(
                    leadingContent = {
                        RadioButton(
                            selected = selectedFont == AppFont.PLUS_JAKARTA_SANS.value,
                            onClick = null
                        )
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.font_plus_jakarta_sans),
                            fontFamily = PlusJakartaSansFontFamily
                        )
                    },
                    description = {
                        Text(
                            text = stringResource(R.string.font_plus_jakarta_sans_desc),
                            fontFamily = PlusJakartaSansFontFamily
                        )
                    },
                    onClick = { onSelectedFontChange(AppFont.PLUS_JAKARTA_SANS.value) }
                )
            )
        )
        Spacer(modifier = Modifier.height(36.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.app_font)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}
