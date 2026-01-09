package com.music.vivi.livemedia

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object QSStateProvider {
    private val _isQsOpen = MutableStateFlow(false)
    val isQsOpen = _isQsOpen.asStateFlow()

    fun updateQsState(isOpen: Boolean) {
        _isQsOpen.value = isOpen
    }
}
