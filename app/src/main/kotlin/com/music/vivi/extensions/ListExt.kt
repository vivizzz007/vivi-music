package com.music.vivi.extensions

import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.Song

/**
 * Returns a reversed view of the list if [reversed] is true, otherwise returns the original list.
 *
 * @param reversed Whether to reverse the list.
 */
fun <T> List<T>.reversed(reversed: Boolean) = if (reversed) asReversed() else this

/**
 * Moves an element from [fromIndex] to [toIndex] in a mutable list.
 *
 * @return The modified list.
 */
fun <T> MutableList<T>.move(fromIndex: Int, toIndex: Int): MutableList<T> {
    add(toIndex, removeAt(fromIndex))
    return this
}

/**
 * Merges adjacent elements that satisfy a condition.
 *
 * Iterates through the list and merges consecutive elements if they share the same key.
 *
 * @param key Selector to determine uniqueness (e.g., Artist ID).
 * @param merge Function to merge two similar elements into one.
 */
fun <T : Any> List<T>.mergeNearbyElements(
    key: (T) -> Any = { it },
    merge: (first: T, second: T) -> T = { first, _ -> first },
): List<T> {
    if (isEmpty()) return emptyList()

    val mergedList = mutableListOf<T>()
    var currentItem = this[0]

    for (i in 1 until size) {
        val nextItem = this[i]
        if (key(currentItem) == key(nextItem)) {
            currentItem = merge(currentItem, nextItem)
        } else {
            mergedList.add(currentItem)
            currentItem = nextItem
        }
    }
    mergedList.add(currentItem)

    return mergedList
}

// Extension function to filter explicit content for local Song entities
fun List<Song>.filterExplicit(enabled: Boolean = true) = if (enabled) {
    filter { !it.song.explicit }
} else {
    this
}

// Extension function to filter video songs for local Song entities
fun List<Song>.filterVideoSongs(enabled: Boolean = true) = if (enabled) {
    filter { !it.song.isVideo }
} else {
    this
}

// Extension function to filter explicit content for local Album entities
fun List<Album>.filterExplicitAlbums(enabled: Boolean = true) = if (enabled) {
    filter { !it.album.explicit }
} else {
    this
}
