package com.music.vivi.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.vivi.constants.ListItemHeight

/**
 * A generic list item with leading icon, title, subtitle, and trailing content.
 * Inline version for performance.
 */
@Composable
inline fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    noinline subtitle: (@Composable RowScope.() -> Unit)? = null,
    noinline leadingContent: @Composable (() -> Unit)? = null,
    thumbnailContent: @Composable () -> Unit = {},
    trailingContent: @Composable RowScope.() -> Unit = {},
    isActive: Boolean = false,
    drawHighlight: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(ListItemHeight)
            .padding(horizontal = 8.dp)
            .then(
                if (isActive && drawHighlight) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                } else {
                    Modifier
                }
            )
    ) {
        if (leadingContent != null) {
            Box(Modifier.padding(start = 6.dp), contentAlignment = Alignment.Center) { leadingContent() }
        }
        Box(Modifier.padding(6.dp), contentAlignment = Alignment.Center) { thumbnailContent() }
        Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) Row(verticalAlignment = Alignment.CenterVertically) { subtitle() }
        }
        trailingContent()
    }
}

/**
 * A generic list item wrapper that handles badges and subtitle styling.
 */
@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
    badges: @Composable RowScope.() -> Unit = {},
    leadingContent: @Composable (() -> Unit)? = null,
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    isActive: Boolean = false,
    drawHighlight: Boolean = true,
) = ListItem(
    title = title,
    modifier = modifier,
    isActive = isActive,
    drawHighlight = drawHighlight,
    leadingContent = leadingContent,
    subtitle = {
        badges()
        if (!subtitle.isNullOrEmpty()) {
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    },
    thumbnailContent = thumbnailContent,
    trailingContent = trailingContent
)
