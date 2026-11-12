/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.ctrlfxx.ctrlmusic.ui.screens.equalizer

import com.ctrlfxx.ctrlmusic.eq.data.SavedEQProfile

/**
 * UI State for EQ Screen
 */
data class EQState(
    val profiles: List<SavedEQProfile> = emptyList(),
    val activeProfileId: String? = null,
    val importStatus: String? = null,
    val error: String? = null
)