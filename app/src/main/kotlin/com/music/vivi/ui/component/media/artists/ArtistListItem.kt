package com.music.vivi.ui.component.media.artists

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import com.music.vivi.R
import com.music.vivi.constants.ListThumbnailSize
import com.music.vivi.db.entities.Artist
import com.music.vivi.ui.component.ListItem
import com.music.vivi.ui.component.media.common.ItemThumbnail
import com.music.vivi.ui.component.media.common.MediaIcons

@Composable
fun ArtistListItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = {
        if (artist.artist.bookmarkedAt != null) {
            MediaIcons.Favorite()
        }
    },
    trailingContent: @Composable RowScope.() -> Unit = {},
    isActive: Boolean = false,
    drawHighlight: Boolean = true,
) = ListItem(
    title = artist.artist.name,
    subtitle = pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount),
    badges = badges,
    thumbnailContent = {
        ItemThumbnail(
            thumbnailUrl = artist.artist.thumbnailUrl,
            isActive = isActive,
            isPlaying = false,
            shape = CircleShape,
            modifier = Modifier.size(ListThumbnailSize)
        )
    },
    trailingContent = trailingContent,
    modifier = modifier,
    isActive = isActive,
    drawHighlight = drawHighlight
)
