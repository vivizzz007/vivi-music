package com.music.vivi.ui.component.media.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.music.vivi.constants.ListThumbnailSize
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.models.MediaMetadata
import com.music.vivi.ui.component.ListItem
import com.music.vivi.ui.component.RoundedCheckbox
import com.music.vivi.utils.joinByBullet
import com.music.vivi.utils.makeTimeString

@Composable
public fun MediaMetadataListItem(
    mediaMetadata: MediaMetadata,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isSelected: Boolean = false,
    inSelectionMode: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {},
    trailingContent: @Composable RowScope.() -> Unit = {},
    drawHighlight: Boolean = true,
) {
    ListItem(
        title = mediaMetadata.title,
        subtitle = joinByBullet(
            mediaMetadata.artists.joinToString { it.name },
            makeTimeString(mediaMetadata.duration * 1000L)
        ),
        leadingContent = null,
        thumbnailContent = {
            ItemThumbnail(
                thumbnailUrl = mediaMetadata.thumbnailUrl,
                albumIndex = null,
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
