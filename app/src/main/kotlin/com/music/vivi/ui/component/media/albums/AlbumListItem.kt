package com.music.vivi.ui.component.media.albums

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.R
import com.music.vivi.constants.ListThumbnailSize
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.db.entities.Album
import com.music.vivi.ui.component.core.ListItem
import com.music.vivi.ui.component.media.common.ItemThumbnail
import com.music.vivi.ui.component.media.common.MediaIcons
import com.music.vivi.utils.joinByBullet

@Composable
fun AlbumListItem(
    album: Album,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = {
        val downloadUtil = LocalDownloadUtil.current
        val database = LocalDatabase.current
        val downloadState by downloadUtil.getDownload(album.id)
            .collectAsState(initial = null)
        val albumState by database.album(album.id).collectAsState(initial = album)
        val isFavorite = albumState?.album?.bookmarkedAt != null

        if (isFavorite) {
            MediaIcons.Favorite()
        }
        if (album.album.explicit) {
            MediaIcons.Explicit()
        }
        MediaIcons.Download(downloadState?.state)
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
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
    isActive = isActive,
)
