package com.music.vivi.models

import com.music.innertube.models.YTItem
import com.music.vivi.db.entities.LocalItem

/**
 * Represents a recommendation section related to a local item (e.g., "Möglicherweise magst du auch").
 *
 * @property title The header/title item for the section.
 * @property items The list of recommended items.
 */
data class SimilarRecommendation(val title: LocalItem, val items: List<YTItem>)
