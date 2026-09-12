/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.EnableSponsorBlockKey
import com.music.vivi.constants.SponsorBlockShowToastKey
import com.music.vivi.constants.SponsorBlockSkipInteractionKey
import com.music.vivi.constants.SponsorBlockSkipIntroOutroKey
import com.music.vivi.constants.SponsorBlockSkipNonMusicKey
import com.music.vivi.constants.SponsorBlockSkipPreviewFillerKey
import com.music.vivi.constants.SponsorBlockSkipSelfPromoKey
import com.music.vivi.constants.SponsorBlockSkipSponsorKey
import com.music.vivi.ui.component.ExpressiveSettingGroup
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.component.ModernSwitch
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorBlockSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (sponsorBlockEnabled, onSponsorBlockEnabledChange) = rememberPreference(
        EnableSponsorBlockKey,
        defaultValue = true
    )
    val (skipNonMusic, onSkipNonMusicChange) = rememberPreference(
        SponsorBlockSkipNonMusicKey,
        defaultValue = true
    )
    val (skipSponsor, onSkipSponsorChange) = rememberPreference(
        SponsorBlockSkipSponsorKey,
        defaultValue = true
    )
    val (skipSelfPromo, onSkipSelfPromoChange) = rememberPreference(
        SponsorBlockSkipSelfPromoKey,
        defaultValue = true
    )
    val (skipInteraction, onSkipInteractionChange) = rememberPreference(
        SponsorBlockSkipInteractionKey,
        defaultValue = true
    )
    val (skipIntroOutro, onSkipIntroOutroChange) = rememberPreference(
        SponsorBlockSkipIntroOutroKey,
        defaultValue = true
    )
    val (skipPreviewFiller, onSkipPreviewFillerChange) = rememberPreference(
        SponsorBlockSkipPreviewFillerKey,
        defaultValue = false
    )
    val (showToast, onShowToastChange) = rememberPreference(
        SponsorBlockShowToastKey,
        defaultValue = true
    )

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.sponsorblock_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
        )

        // Master toggle card
        val containerColor by animateColorAsState(
            targetValue = if (sponsorBlockEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
            label = "containerColor"
        )

        val contentColor = if (sponsorBlockEnabled) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Card(
            onClick = { onSponsorBlockEnabledChange(!sponsorBlockEnabled) },
            shape = RoundedCornerShape(50),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.enable_sponsorblock),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                ModernSwitch(
                    checked = sponsorBlockEnabled,
                    onCheckedChange = onSponsorBlockEnabledChange
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Categories Group
        ExpressiveSettingGroup(
            title = stringResource(R.string.sponsorblock_categories),
            items = listOf(
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.sponsorblock_skip_non_music)) },
                    description = { Text(stringResource(R.string.sponsorblock_skip_non_music_desc)) },
                    trailingContent = {
                        ModernSwitch(
                            checked = skipNonMusic,
                            onCheckedChange = onSkipNonMusicChange,
                            enabled = sponsorBlockEnabled
                        )
                    },
                    enabled = sponsorBlockEnabled,
                    onClick = { onSkipNonMusicChange(!skipNonMusic) }
                ),
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.sponsorblock_skip_sponsor)) },
                    description = { Text(stringResource(R.string.sponsorblock_skip_sponsor_desc)) },
                    trailingContent = {
                        ModernSwitch(
                            checked = skipSponsor,
                            onCheckedChange = onSkipSponsorChange,
                            enabled = sponsorBlockEnabled
                        )
                    },
                    enabled = sponsorBlockEnabled,
                    onClick = { onSkipSponsorChange(!skipSponsor) }
                ),
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.sponsorblock_skip_selfpromo)) },
                    description = { Text(stringResource(R.string.sponsorblock_skip_selfpromo_desc)) },
                    trailingContent = {
                        ModernSwitch(
                            checked = skipSelfPromo,
                            onCheckedChange = onSkipSelfPromoChange,
                            enabled = sponsorBlockEnabled
                        )
                    },
                    enabled = sponsorBlockEnabled,
                    onClick = { onSkipSelfPromoChange(!skipSelfPromo) }
                ),
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.sponsorblock_skip_interaction)) },
                    description = { Text(stringResource(R.string.sponsorblock_skip_interaction_desc)) },
                    trailingContent = {
                        ModernSwitch(
                            checked = skipInteraction,
                            onCheckedChange = onSkipInteractionChange,
                            enabled = sponsorBlockEnabled
                        )
                    },
                    enabled = sponsorBlockEnabled,
                    onClick = { onSkipInteractionChange(!skipInteraction) }
                ),
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.sponsorblock_skip_intro_outro)) },
                    description = { Text(stringResource(R.string.sponsorblock_skip_intro_outro_desc)) },
                    trailingContent = {
                        ModernSwitch(
                            checked = skipIntroOutro,
                            onCheckedChange = onSkipIntroOutroChange,
                            enabled = sponsorBlockEnabled
                        )
                    },
                    enabled = sponsorBlockEnabled,
                    onClick = { onSkipIntroOutroChange(!skipIntroOutro) }
                ),
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.sponsorblock_skip_preview_filler)) },
                    description = { Text(stringResource(R.string.sponsorblock_skip_preview_filler_desc)) },
                    trailingContent = {
                        ModernSwitch(
                            checked = skipPreviewFiller,
                            onCheckedChange = onSkipPreviewFillerChange,
                            enabled = sponsorBlockEnabled
                        )
                    },
                    enabled = sponsorBlockEnabled,
                    onClick = { onSkipPreviewFillerChange(!skipPreviewFiller) }
                )
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Notifications Group
        ExpressiveSettingGroup(
            title = stringResource(R.string.general),
            items = listOf(
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.sponsorblock_show_toast)) },
                    description = { Text(stringResource(R.string.sponsorblock_show_toast_desc)) },
                    trailingContent = {
                        ModernSwitch(
                            checked = showToast,
                            onCheckedChange = onShowToastChange,
                            enabled = sponsorBlockEnabled
                        )
                    },
                    enabled = sponsorBlockEnabled,
                    onClick = { onShowToastChange(!showToast) }
                )
            )
        )

        Spacer(modifier = Modifier.height(36.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.sponsorblock)) },
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
