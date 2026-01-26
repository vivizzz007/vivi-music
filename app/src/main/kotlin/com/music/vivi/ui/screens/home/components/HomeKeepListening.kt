package com.music.vivi.ui.component.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.GridThumbnailHeight
import com.music.vivi.db.entities.LocalItem
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.utils.ImmutableList
import kotlinx.coroutines.CoroutineScope

/**
 * "Keep Listening" section.
 * Displays recently played LOCAL items (Albums, Artists) to quickly resume playback.
 */
@Composable
internal fun HomeKeepListening(
    keepListening: ImmutableList<LocalItem>,
    isPlaying: Boolean,
    activeId: String?,
    activeAlbumId: String?,
    navController: NavController,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    if (keepListening.isEmpty()) return

    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current

    NavigationTitle(
        title = stringResource(R.string.keep_listening),
        modifier = Modifier
    )

    val rows = if (keepListening.size > 6) 2 else 1
    LazyHorizontalGrid(
        state = rememberLazyGridState(),
        rows = GridCells.Fixed(rows),
        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
            .asPaddingValues(),
        modifier = modifier
            .fillMaxWidth()
            .height(
                (
                    GridThumbnailHeight + with(LocalDensity.current) {
                        MaterialTheme.typography.bodyLarge.lineHeight.toDp() * 2 +
                            MaterialTheme.typography.bodyMedium.lineHeight.toDp() * 2
                    }
                    ) * rows
            )
    ) {
        items(keepListening) {
            HomeLocalGridItem(
                item = it,
                isPlaying = isPlaying,
                activeId = activeId,
                activeAlbumId = activeAlbumId,
                navController = navController,
                menuState = menuState,
                scope = scope,
                playerConnection = playerConnection
            )
        }
    }
}
