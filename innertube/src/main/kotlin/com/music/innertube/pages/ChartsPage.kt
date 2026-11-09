/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.innertube.pages

import com.music.innertube.models.*

data class ChartsPage(
    val sections: List<ChartSection>,
    val continuation: String?
) {
    data class ChartSection(
        val title: String,
        val items: List<YTItem>,
        val chartType: ChartType
    )

    enum class ChartType {
        TRENDING, TOP, GENRE, NEW_RELEASES
    }
}
