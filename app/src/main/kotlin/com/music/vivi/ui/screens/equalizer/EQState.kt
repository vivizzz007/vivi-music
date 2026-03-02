package com.music.vivi.ui.screens.equalizer

import com.music.vivi.eq.data.SavedEQProfile

/**
 * UI State for EQ Screen
 */
data class EQState(
    val profiles: List<SavedEQProfile> = emptyList(),
    val activeProfileId: String? = null,
    val importStatus: String? = null,
    val error: String? = null
)