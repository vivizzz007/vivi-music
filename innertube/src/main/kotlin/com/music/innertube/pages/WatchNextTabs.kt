/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.innertube.pages

import com.music.innertube.models.BrowseEndpoint
import com.music.innertube.models.Tabs

// Tab positions shift when a song has no lyrics tab, so the browse id prefix is the reliable signal.
internal fun watchNextTabIndex(browseIds: List<String?>, prefix: String, fallbackIndex: Int): Int? =
    browseIds.indexOfFirst { it?.startsWith(prefix) == true }
        .takeIf { it != -1 }
        ?: fallbackIndex.takeIf { it in browseIds.indices }

internal fun List<Tabs.Tab>?.browseEndpointOf(prefix: String, fallbackIndex: Int): BrowseEndpoint? {
    if (this == null) return null
    val endpoints = map { it.tabRenderer.endpoint?.browseEndpoint }
    val index = watchNextTabIndex(endpoints.map { it?.browseId }, prefix, fallbackIndex) ?: return null
    return endpoints[index]
}
