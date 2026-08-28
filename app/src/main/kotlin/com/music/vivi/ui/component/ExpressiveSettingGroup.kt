/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.vivi.utils.listItemShape

/**
 * A group of setting items styled exactly like WeatherMaster's SettingSection:
 * - No group title header
 * - Top curve, middle flat, bottom curve per item position
 * - Uses ListItem so icons auto-align: centered on title-only, top-aligned when description present
 */
@Composable
fun ExpressiveSettingGroup(
    title: String? = null,
    items: List<Material3SettingsItem>,
    modifier: Modifier = Modifier,
    itemMinHeight: Dp? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 8.dp)
            )
        }
        items.forEachIndexed { index, item ->
            val shape = listItemShape(index, items.size)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = shape,
                color = if (androidx.compose.foundation.isSystemInDarkTheme()) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                }
            ) {
                ListItem(
                    modifier = Modifier
                        .clickable(
                            enabled = item.enabled && item.onClick != null,
                            onClick = { item.onClick?.invoke() }
                        ),
                    leadingContent = item.leadingContent
                        ?: item.icon?.let { icon ->
                            {
                                if (item.tintIcon) {
                                    Icon(
                                        painter = icon,
                                        contentDescription = null,
                                        tint = if (!item.enabled)
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Image(
                                        painter = icon,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(
                                                item.iconShape ?: RoundedCornerShape(6.dp)
                                            ),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        },
                    headlineContent = {
                        ProvideTextStyle(
                            MaterialTheme.typography.bodyLarge.copy(
                                color = if (!item.enabled)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            item.title()
                        }
                    },
                    supportingContent = item.description,
                    trailingContent = item.trailingContent,
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    }
}

