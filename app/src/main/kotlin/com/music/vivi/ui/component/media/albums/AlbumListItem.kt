package com.music.vivi.ui.component.media.albums

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import com.music.vivi.R
import com.music.vivi.constants.ListThumbnailSize
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.db.entities.Album
import com.music.vivi.ui.component.ListItem
import com.music.vivi.ui.component.media.common.ItemThumbnail
import com.music.vivi.ui.component.media.common.MediaIcons
import com.music.vivi.utils.joinByBullet

@Composable
fun AlbumListItem(
    album: Album,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isFavorite: Boolean = false,
    downloadState: Int? = null,
    trailingContent: @Composable RowScope.() -> Unit = {},
    badges: @Composable RowScope.() -> Unit = {
        if (isFavorite) {
            MediaIcons.Favorite()
        }
        if (album.album.explicit) {
            MediaIcons.Explicit()
        }
        MediaIcons.Download(downloadState)
    },
    drawHighlight: Boolean = true,
) = ListItem(
    title = album.album.title,
    subtitle = joinByBullet(
        album.artists.joinToString { it.name },
        pluralStringResource(R.plurals.n_song, album.album.songCount, album.album.songCount),
        album.album.year?.toString()
    ),
    badges = badges,
    thumbnailContent = {
        ItemThumbnail(
            thumbnailUrl = album.album.thumbnailUrl,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = RoundedCornerShape(ThumbnailCornerRadius),
            modifier = Modifier.size(ListThumbnailSize)
        )
    },
    trailingContent = trailingContent,
    modifier = modifier,
    drawHighlight = drawHighlight,
    isActive = isActive
)
