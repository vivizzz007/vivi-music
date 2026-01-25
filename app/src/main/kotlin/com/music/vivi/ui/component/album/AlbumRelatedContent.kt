package com.music.vivi.ui.component.album

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.music.innertube.models.AlbumItem
import com.music.vivi.R
import com.music.vivi.models.MediaMetadata
import com.music.vivi.ui.component.MenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.media.youtube.YouTubeGridItem
import com.music.vivi.ui.menu.YouTubeAlbumMenu
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.albumRelatedContent(
    otherVersions: List<AlbumItem>,
    releasesForYou: List<AlbumItem>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    scope: CoroutineScope,
    navController: NavController,
    menuState: MenuState,
    haptic: HapticFeedback,
) {
    // Other Versions Section
    if (otherVersions.isNotEmpty()) {
        item(key = "other_versions_title") {
            NavigationTitle(
                title = stringResource(R.string.other_versions),
                modifier = Modifier.animateItem()
            )
        }
        item(key = "other_versions_list") {
            LazyRow(
                contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
            ) {
                items(
                    items = otherVersions.distinctBy { it.id },
                    key = { it.id }
                ) { item ->
                    YouTubeGridItem(
                        item = item,
                        isActive = mediaMetadata?.album?.id == item.id,
                        isPlaying = isPlaying,
                        coroutineScope = scope,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { navController.navigate("album/${item.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        YouTubeAlbumMenu(
                                            albumItem = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            )
                            .animateItem()
                    )
                }
            }
        }
    }

    // Releases For You Section
    if (releasesForYou.isNotEmpty()) {
        item(key = "releases_for_you_title") {
            NavigationTitle(
                title = stringResource(R.string.releases_for_you),
                modifier = Modifier.animateItem()
            )
        }
        item(key = "releases_for_you_list") {
            LazyRow(
                contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
            ) {
                items(
                    items = releasesForYou.distinctBy { it.id },
                    key = { it.id }
                ) { item ->
                    YouTubeGridItem(
                        item = item,
                        isActive = mediaMetadata?.album?.id == item.id,
                        isPlaying = isPlaying,
                        coroutineScope = scope,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { navController.navigate("album/${item.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        YouTubeAlbumMenu(
                                            albumItem = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            )
                            .animateItem()
                    )
                }
            }
        }
    }
}
