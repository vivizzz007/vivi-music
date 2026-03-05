/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Helper composable that auto-scrolls to a highlighted setting when the position is reported.
 * Use [onHighlightPositionFound] callback in Material3SettingsGroup to feed the position.
 *
 * Usage:
 * ```
 * val scrollState = rememberScrollState()
 * val (highlightScrollHandler, onHighlightPosition) = rememberHighlightScrollHandler(scrollState, highlightKey)
 *
 * Material3SettingsGroup(
 *     items = items,
 *     highlightKey = highlightKey,
 *     onHighlightPositionFound = onHighlightPosition
 * )
 * ```
 */
@Composable
fun rememberHighlightScrollHandler(
    scrollState: ScrollState,
    highlightKey: String?
): Pair<Unit, (Float) -> Unit> {
    var highlightPosition by remember { mutableIntStateOf(-1) }
    var hasScrolled by remember { mutableStateOf(false) }

    val onHighlightPosition: (Float) -> Unit = { y ->
        if (!hasScrolled) {
            highlightPosition = y.toInt()
        }
    }

    LaunchedEffect(highlightKey, highlightPosition) {
        if (highlightKey != null && highlightPosition >= 0 && !hasScrolled) {
            // positionInRoot gives screen-relative Y.
            // Adding current scroll converts to absolute content position.
            // Subtract ~500 so item lands near screen center.
            val target = (highlightPosition + scrollState.value - 500).coerceAtLeast(0)
            scrollState.animateScrollTo(target)
            hasScrolled = true // Only scroll once — let the user scroll freely after
        }
    }

    return Pair(Unit, onHighlightPosition)
}
