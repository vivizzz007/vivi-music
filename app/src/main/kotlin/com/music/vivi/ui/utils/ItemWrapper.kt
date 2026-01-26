package com.music.vivi.ui.utils

import androidx.compose.runtime.mutableStateOf

/**
 * A wrapper for items in a list that tracks selection state.
 *
 * @param item The item data.
 */
class ItemWrapper<T>(val item: T) {
    private val _isSelected = mutableStateOf(true)

    var isSelected: Boolean
        get() = _isSelected.value
        set(value) {
            _isSelected.value = value
        }
}
