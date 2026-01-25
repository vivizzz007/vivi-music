package com.music.innertube.pages

import com.music.innertube.models.YTItem

data class LibraryContinuationPage(val items: List<YTItem>, val continuation: String?)
