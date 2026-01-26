package com.music.vivi.ui.component.media.songs

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.music.vivi.constants.ListItemHeight
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

/**
 * A reusable LazyList extension that renders a list of items with the "Squircle" rounded corner style.
 *
 * @param items The list of items to render.
 * @param key Optional key selector for LazyList.
 * @param isActive Selector to determine if an item is the currently playing active item.
 * @param bottomSpacerHeight Height of the spacer at the bottom of the list.
 * @param itemContent The composable content for each item, typically a [SongListItem].
 */
internal fun <T> LazyListScope.roundedSongItems(
    items: List<T>,
    key: ((item: T) -> Any)? = null,
    isActive: (T) -> Boolean,
    bottomSpacerHeight: androidx.compose.ui.unit.Dp = 16.dp,
    onItemClick: (T) -> Unit,
    onItemLongClick: (T) -> Unit,
    itemContent: @Composable (item: T, shape: Shape, modifier: Modifier) -> Unit,
) {
    itemsIndexed(
        items = items,
        key = if (key != null) { index, item -> key(item) } else null
    ) { index, item ->
        val isFirst = index == 0
        val isLast = index == items.size - 1
        val itemIsActive = isActive(item)
        val haptic = LocalHapticFeedback.current

        val cornerRadius = remember { 24.dp }

        val topShape = remember(cornerRadius) {
            AbsoluteSmoothCornerShape(
                cornerRadiusTR = cornerRadius,
                smoothnessAsPercentBR = 0,
                cornerRadiusBR = 0.dp,
                smoothnessAsPercentTL = 60,
                cornerRadiusTL = cornerRadius,
                smoothnessAsPercentBL = 0,
                cornerRadiusBL = 0.dp,
                smoothnessAsPercentTR = 60
            )
        }
        val middleShape = remember { RectangleShape }
        val bottomShape = remember(cornerRadius) {
            AbsoluteSmoothCornerShape(
                cornerRadiusTR = 0.dp,
                smoothnessAsPercentBR = 60,
                cornerRadiusBR = cornerRadius,
                smoothnessAsPercentTL = 0,
                cornerRadiusTL = 0.dp,
                smoothnessAsPercentBL = 60,
                cornerRadiusBL = cornerRadius,
                smoothnessAsPercentTR = 0
            )
        }
        val singleShape = remember(cornerRadius) {
            AbsoluteSmoothCornerShape(
                cornerRadiusTR = cornerRadius,
                smoothnessAsPercentBR = 60,
                cornerRadiusBR = cornerRadius,
                smoothnessAsPercentTL = 60,
                cornerRadiusTL = cornerRadius,
                smoothnessAsPercentBL = 60,
                cornerRadiusBL = cornerRadius,
                smoothnessAsPercentTR = 60
            )
        }

        val shape = remember(isFirst, isLast, cornerRadius) {
            when {
                isFirst && isLast -> singleShape
                isFirst -> topShape
                isLast -> bottomShape
                else -> middleShape
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(ListItemHeight)
                .clip(shape)
                .background(
                    if (itemIsActive) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    }
                )
        ) {
            // We pass the modifier to the content so it can handle clicks safely if needed,
            // OR we wrap it here. Standard Vivi pattern seems to be wrapping CombinedClickable on the content modifier.
            // Let's pass a modifier that the content MUST apply.

            val contentModifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = { onItemClick(item) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onItemLongClick(item)
                    }
                )

            itemContent(item, shape, contentModifier)
        }

        if (!isLast) {
            Spacer(modifier = Modifier.height(3.dp))
        } else {
            Spacer(modifier = Modifier.height(bottomSpacerHeight))
        }
    }
}
