/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.music.vivi.ui.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.music.vivi.ui.player.FloatingMiniPlayer
import com.music.vivi.ui.screens.Screens
import com.music.vivi.ui.component.floatingtabbar.FloatingTabBar
import com.music.vivi.ui.component.floatingtabbar.FloatingTabBarDefaults
import com.music.vivi.ui.component.floatingtabbar.FloatingTabBarScrollConnection

/**
 * The iOS 26 style floating navigation bar, an alternative to [AppNavigationBar].
 *
 * Collapses to an inline pill while scrolling down (driven by [scrollConnection]) and
 * expands back on scroll up. The search destination is rendered as the standalone
 * circular tab. When the liquid glass effect is enabled for the navigation bar, the tab
 * bar surfaces sample the app backdrop through [Modifier.liquidGlass].
 *
 * When [showPlayerAccessory] is true the now playing controls dock into the bar as an
 * accessory (a pill above the tabs when expanded, inline between the tab pill and the
 * search tab when collapsed) and [onAccessoryClick] opens the full player.
 */
@Composable
fun AppFloatingNavBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    scrollConnection: FloatingTabBarScrollConnection,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    showPlayerAccessory: Boolean = false,
    onAccessoryClick: () -> Unit = {},
) {
    val glassConfig = LocalGlassEffectConfig.current
    val useGlass = glassConfig.isEnabledFor(GlassComponent.NAV_BAR) && isGlassSupported()

    val backgroundColor = when {
        useGlass -> Color.Transparent
        pureBlack -> Color.Black
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val selectedContentColor = when {
        useGlass -> glassConfig.textColor
        pureBlack -> Color.White
        else -> MaterialTheme.colorScheme.primary
    }
    val unselectedContentColor = when {
        useGlass -> glassConfig.textColor.copy(alpha = 0.65f)
        pureBlack -> Color.White.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val tabBarContentModifier = if (useGlass) {
        Modifier.liquidGlass(
            config = glassConfig,
            shape = RoundedCornerShape(percent = 50),
        )
    } else {
        Modifier
    }

    val selectedTabKey = navigationItems.firstOrNull { screen ->
        isRouteSelected(currentRoute, screen.route, navigationItems)
    }?.route

    val searchScreen = navigationItems.firstOrNull { it == Screens.Search }
    val tabScreens = remember(navigationItems) { navigationItems.filter { it != Screens.Search } }

    val accessoryContentColor = when {
        useGlass -> glassConfig.textColor
        pureBlack -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    val inlineAccessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? =
        if (showPlayerAccessory) {
            { accessoryModifier, _ ->
                FloatingMiniPlayer(
                    isInline = true,
                    contentColor = accessoryContentColor,
                    onClick = onAccessoryClick,
                    modifier = accessoryModifier.then(tabBarContentModifier),
                )
            }
        } else {
            null
        }
    val expandedAccessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? =
        if (showPlayerAccessory) {
            { accessoryModifier, _ ->
                FloatingMiniPlayer(
                    isInline = false,
                    contentColor = accessoryContentColor,
                    onClick = onAccessoryClick,
                    modifier = accessoryModifier.fillMaxWidth().then(tabBarContentModifier),
                )
            }
        } else {
            null
        }

    FloatingTabBar(
        selectedTabKey = selectedTabKey,
        scrollConnection = scrollConnection,
        modifier = modifier,
        tabBarContentModifier = tabBarContentModifier,
        inlineAccessory = inlineAccessory,
        expandedAccessory = expandedAccessory,
        colors = FloatingTabBarDefaults.colors(
            backgroundColor = backgroundColor,
            accessoryBackgroundColor = backgroundColor,
        ),
        // The tab content lambdas are captured once per contentKey, so anything they
        // close over (selection, colors) must be part of the key to avoid stale UI.
        contentKey = listOf(selectedTabKey, navigationItems, selectedContentColor, unselectedContentColor),
    ) {
        tabScreens.forEach { screen ->
            val isSelected = screen.route == selectedTabKey
            tab(
                key = screen.route,
                title = {
                    Text(
                        text = stringResource(screen.titleId),
                        color = if (isSelected) selectedContentColor else unselectedContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                icon = {
                    Icon(
                        painter = painterResource(
                            if (isSelected) screen.iconIdActive else screen.iconIdInactive
                        ),
                        contentDescription = stringResource(screen.titleId),
                        tint = if (isSelected) selectedContentColor else unselectedContentColor,
                    )
                },
                onClick = { onItemClick(screen, isSelected) },
            )
        }

        searchScreen?.let { screen ->
            val isSelected = screen.route == selectedTabKey
            standaloneTab(
                key = screen.route,
                icon = {
                    Icon(
                        painter = painterResource(
                            if (isSelected) screen.iconIdActive else screen.iconIdInactive
                        ),
                        contentDescription = stringResource(screen.titleId),
                        tint = if (isSelected) selectedContentColor else unselectedContentColor,
                    )
                },
                onClick = { onItemClick(screen, isSelected) },
            )
        }
    }
}
