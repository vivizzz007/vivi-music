package com.music.vivi.utils

import androidx.compose.runtime.Immutable

/**
 * A wrapper for any class to ensure it is treated as Stable by the Compose Compiler.
 * Use this to wrap unstable classes (like those from external libraries) to prevent unnecessary recompositions.
 */
@Immutable
data class ImmutableWrapper<T>(val item: T)

/**
 * A wrapper for [List] that guarantees immutability to the Compose Compiler.
 * Standard [List] is considered unstable because it is an interface (could be ArrayList, LinkedList, etc.).
 * Wrapping it in an @Immutable data class forces it to be stable.
 */
@Immutable
data class ImmutableList<T>(val items: List<T>) : List<T> by items
