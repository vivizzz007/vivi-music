package com.music.vivi.ui.component.media.songs

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.music.vivi.constants.ListThumbnailSize
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.ui.component.ListItem
import com.music.vivi.ui.component.RoundedCheckbox
import com.music.vivi.ui.component.media.common.ItemThumbnail
import com.music.vivi.ui.component.media.common.MediaIcons
import com.music.vivi.ui.component.media.common.SwipeToSongBox
import com.music.vivi.utils.joinByBullet
import com.music.vivi.utils.makeTimeString

@Composable
public fun SongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    showLikedIcon: Boolean = true,
    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true,
    downloadState: Int? = null,
    swipeEnabled: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {
        if (showLikedIcon && song.song.liked) {
            MediaIcons.Favorite()
        }
        if (song.song.explicit) {
            MediaIcons.Explicit()
        }
        if (showInLibraryIcon && song.song.inLibrary != null) {
            MediaIcons.Library()
        }
        if (showDownloadIcon) {
            MediaIcons.Download(downloadState)
        }
    },
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isSwipeable: Boolean = true,
    inSelectionMode: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {},
    trailingContent: @Composable RowScope.() -> Unit = {},
    drawHighlight: Boolean = true,
) {
    val content: @Composable () -> Unit = {
        ListItem(
            title = song.song.title,
            subtitle = joinByBullet(
                song.artists.joinToString { it.name },
                makeTimeString(song.song.duration * 1000L)
            ),
            badges = badges,
            leadingContent = null,
            thumbnailContent = {
                ItemThumbnail(
                    thumbnailUrl = song.song.thumbnailUrl,
                    albumIndex = albumIndex,
                    isSelected = isSelected && !inSelectionMode,
                    isActive = isActive,
                    isPlaying = isPlaying,
                    shape = RoundedCornerShape(ThumbnailCornerRadius),
                    modifier = Modifier.size(ListThumbnailSize)
                )
            },
            trailingContent = if (inSelectionMode) {
                {
                    RoundedCheckbox(
                        checked = isSelected,
                        onCheckedChange = onSelectionChange,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            } else {
                trailingContent
            },
            modifier = modifier,
            isActive = isActive,
            drawHighlight = drawHighlight
        )
    }

    if (isSwipeable && swipeEnabled) {
        SwipeToSongBox(
            mediaItem = song.toMediaItem(),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    } else {
        content()
    }
}
