/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.eq.data

import kotlinx.serialization.Serializable

@Serializable
enum class FilterType {
    /** Peaking filter - boosts or cuts around a center frequency */
    PK,
    /** Low-shelf filter - affects frequencies below the cutoff */
    LSC,
    /** High-shelf filter - affects frequencies above the cutoff */
    HSC,
    /** Low-pass filter - attenuates frequencies above the cutoff */
    LPQ,
    /** High-pass filter - attenuates frequencies below the cutoff */
    HPQ
}