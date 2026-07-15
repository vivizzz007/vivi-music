/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.AlbumCanvasEnabledKey
import com.music.vivi.constants.CanvasThumbnailAnimationKey
import com.music.vivi.constants.DataSaverBackupAlbumCanvasKey
import com.music.vivi.constants.DataSaverBackupArtistBgVideoKey
import com.music.vivi.constants.DataSaverBackupArtistVideoKey
import com.music.vivi.constants.DataSaverBackupCanvasKey
import com.music.vivi.constants.DataSaverKey
import com.music.vivi.constants.ShowArtistBackgroundVideoKey
import com.music.vivi.constants.ShowArtistVideoKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.ModernSwitch
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.rememberPreference
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSaverSetting(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val dataSaver by rememberPreference(DataSaverKey, defaultValue = false)

    fun toggleDataSaver(enable: Boolean) {
        scope.launch {
            context.dataStore.edit { prefs ->
                if (enable) {
                    // Step 1: Backup current values before overriding
                    prefs[DataSaverBackupCanvasKey]        = prefs[CanvasThumbnailAnimationKey] ?: true
                    prefs[DataSaverBackupArtistVideoKey]   = prefs[ShowArtistVideoKey] ?: true
                    prefs[DataSaverBackupArtistBgVideoKey] = prefs[ShowArtistBackgroundVideoKey] ?: true
                    prefs[DataSaverBackupAlbumCanvasKey]   = prefs[AlbumCanvasEnabledKey] ?: false
                    // Step 2: Force everything off
                    prefs[CanvasThumbnailAnimationKey]  = false
                    prefs[ShowArtistVideoKey]           = false
                    prefs[ShowArtistBackgroundVideoKey] = false
                    prefs[AlbumCanvasEnabledKey]        = false
                    prefs[DataSaverKey]                 = true
                } else {
                    // Restore user's original values from backup
                    prefs[CanvasThumbnailAnimationKey]  = prefs[DataSaverBackupCanvasKey] ?: true
                    prefs[ShowArtistVideoKey]           = prefs[DataSaverBackupArtistVideoKey] ?: true
                    prefs[ShowArtistBackgroundVideoKey] = prefs[DataSaverBackupArtistBgVideoKey] ?: true
                    prefs[AlbumCanvasEnabledKey]        = prefs[DataSaverBackupAlbumCanvasKey] ?: false
                    prefs[DataSaverKey]                 = false
                }
            }
        }
    }

    // Animated card color based on Data Saver state
    val containerColor by animateColorAsState(
        targetValue = if (dataSaver) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        label = "dataSaverContainerColor"
    )

    val contentColor = if (dataSaver) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // Description
        Text(
            text = stringResource(R.string.data_saver_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
        )

        // Main toggle capsule card
        Card(
            onClick = { toggleDataSaver(!dataSaver) },
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
                    text = stringResource(R.string.data_saver),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                ModernSwitch(
                    checked = dataSaver,
                    onCheckedChange = { toggleDataSaver(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info card about what gets turned off
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.data_saver_turns_off_header),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                DataSaverInfoRow(
                    icon = R.drawable.canvas_art,
                    text = stringResource(R.string.data_saver_player_canvas)
                )
                DataSaverInfoRow(
                    icon = R.drawable.slow_motion_video,
                    text = stringResource(R.string.data_saver_artist_video)
                )
                DataSaverInfoRow(
                    icon = R.drawable.slow_motion_video,
                    text = stringResource(R.string.data_saver_artist_bg_video)
                )
                DataSaverInfoRow(
                    icon = R.drawable.image,
                    text = stringResource(R.string.data_saver_album_canvas)
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.data_saver)) },
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
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun DataSaverInfoRow(icon: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
