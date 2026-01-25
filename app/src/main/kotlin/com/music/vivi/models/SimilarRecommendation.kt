package com.music.vivi.models

import com.music.innertube.models.YTItem
import com.music.vivi.db.entities.LocalItem

data class SimilarRecommendation(val title: LocalItem, val items: List<YTItem>)
