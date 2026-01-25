package com.music.vivi.utils

import androidx.compose.runtime.Immutable

@Immutable
data class ImmutableWrapper<T>(val item: T)

@Immutable
data class ImmutableList<T>(val items: List<T>) : List<T> by items
