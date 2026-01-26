package com.music.vivi.models

import com.music.innertube.models.YTItem

/**
 * Represents a page of items fetched from YouTube, along with a continuation token for the next page.
 */
data class ItemsPage(val items: List<YTItem>, val continuation: String?)
