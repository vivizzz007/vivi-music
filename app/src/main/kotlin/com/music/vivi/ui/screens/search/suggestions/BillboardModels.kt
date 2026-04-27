/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.search.suggestions

data class BillboardTrack(
    val rank: Int,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val lastWeek: Int? = null,
    val peakPos: Int? = null,
    val weeksOnChart: Int? = null,
    val popularityTrend: List<Float> = emptyList()
)

data class BillboardArtist(
    val rank: Int,
    val name: String,
    val thumbnailUrl: String?
)
