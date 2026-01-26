package com.music.innertube.pages

import com.music.innertube.models.YTItem

data class ArtistItemsContinuationPage(val items: List<YTItem>, val continuation: String?)
