/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberHiddenBottomSheetState(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true },
) = rememberBottomSheetState(
    initialValue = SheetValue.Hidden,
    enabledValues = if (skipPartiallyExpanded) {
        setOf(SheetValue.Hidden, SheetValue.Expanded)
    } else {
        setOf(SheetValue.Hidden, SheetValue.PartiallyExpanded, SheetValue.Expanded)
    },
    confirmValueChange = confirmValueChange,
)
