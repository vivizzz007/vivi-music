package com.music.vivi.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

// Enhanced Action Button - Material 3 Expressive Design (Box-based)
/**
 * An enhanced action button used in the new grid-style menus.
 * Features a large icon, marquee text, and smooth background animation.
 */
@Composable
public fun NewActionButton(
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    val animatedBackground by animateColorAsState(
        targetValue = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "background"
    )

    val animatedContent by animateColorAsState(
        targetValue = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "content"
    )

    val shape = remember { AbsoluteSmoothCornerShape(28.dp, 60) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(animatedBackground)
            .clickable(enabled = enabled) { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides animatedContent) {
                    icon()
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = animatedContent,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}

public enum class MenuGroupPosition {
    Top,
    Middle,
    Bottom,
    Single,
}

@Composable
public fun getGroupShape(
    position: MenuGroupPosition,
    cornerRadius: androidx.compose.ui.unit.Dp = 28.dp,
): AbsoluteSmoothCornerShape = remember(position, cornerRadius) {
    when (position) {
        MenuGroupPosition.Top -> AbsoluteSmoothCornerShape(
            cornerRadiusTL = cornerRadius,
            cornerRadiusTR = cornerRadius,
            smoothnessAsPercentTL = 60,
            smoothnessAsPercentTR = 60
        )
        MenuGroupPosition.Middle -> AbsoluteSmoothCornerShape(0.dp, 0)
        MenuGroupPosition.Bottom -> AbsoluteSmoothCornerShape(
            cornerRadiusBL = cornerRadius,
            cornerRadiusBR = cornerRadius,
            smoothnessAsPercentBL = 60,
            smoothnessAsPercentBR = 60
        )
        MenuGroupPosition.Single -> AbsoluteSmoothCornerShape(cornerRadius, 60)
    }
}

/**
 * An enhanced list item for menus, supporting grouping (top, middle, bottom, single) with rounded corners.
 */
@Composable
public fun NewMenuItem(
    headlineContent: @Composable () -> Unit,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    position: MenuGroupPosition = MenuGroupPosition.Single,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.ListItem(
        headlineContent = headlineContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        supportingContent = supportingContent,
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            headlineColor = MaterialTheme.colorScheme.onSecondaryContainer,
            supportingColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            leadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            trailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = modifier
            .clip(getGroupShape(position))
            .clickable(enabled = enabled) { onClick?.invoke() }
            .padding(horizontal = 4.dp),
        tonalElevation = 0.dp
    )
}

// Enhanced Menu Section Header - Material 3 Expressive Design
/**
 * header for sections within the new menu design.
 */
@Composable
public fun NewMenuSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

// Enhanced Action Grid - Material 3 Expressive Design
/**
 * A grid of action buttons for the top of menus.
 */
@Composable
public fun NewActionGrid(actions: List<NewAction>, modifier: Modifier = Modifier, columns: Int = 3) {
    val rows = actions.chunked(columns)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { action ->
                    NewActionButton(
                        icon = action.icon,
                        text = action.text,
                        onClick = action.onClick,
                        modifier = Modifier.weight(1f),
                        enabled = action.enabled,
                        backgroundColor = if (action.backgroundColor !=
                            Color.Unspecified
                        ) {
                            action.backgroundColor
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (action.contentColor !=
                            Color.Unspecified
                        ) {
                            action.contentColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                // Fill remaining space if row is not full
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Enhanced Action Data Class
public data class NewAction(
    val icon: @Composable () -> Unit,
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val backgroundColor: Color = Color.Unspecified,
    val contentColor: Color = Color.Unspecified,
)

// Enhanced Menu Content - Material 3 Expressive Design
/**
 * A container for menu content that organizes headers, action grids, and list items.
 */
@Composable
public fun NewMenuContent(
    headerContent: @Composable (() -> Unit)? = null,
    actionGrid: @Composable (() -> Unit)? = null,
    menuItems: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        headerContent?.invoke()

        // Action Grid
        actionGrid?.invoke()

        // Divider if both header and actions exist
        if (headerContent != null && actionGrid != null) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        // Menu Items
        menuItems?.invoke()
    }
}

// Enhanced Icon Button - Material 3 Expressive Design
/**
 * A square icon button with a background card, used in menus.
 */
@Composable
public fun NewIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val animatedBackground by animateColorAsState(
        targetValue = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "background"
    )

    val animatedContent by animateColorAsState(
        targetValue = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "content"
    )

    val shape = remember { AbsoluteSmoothCornerShape(28.dp, 60) }

    Card(
        modifier = modifier
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = animatedBackground,
            contentColor = animatedContent
        ),
        shape = shape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}

// Enhanced Menu Container - Material 3 Expressive Design
/**
 * A standard container for the new menu design, ensuring consistent padding and layout.
 */
@Composable
public fun NewMenuContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        content()
    }
}
