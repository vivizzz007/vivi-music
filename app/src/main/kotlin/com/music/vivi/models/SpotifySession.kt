/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.models

import kotlinx.serialization.Serializable

@Serializable
data class SpotifySession(
    val spDc: String,
    val spKey: String? = null,
    val accessToken: String? = null,
    val expiresAt: Long = 0,
    val accountName: String? = null,
    val accountAvatarUrl: String? = null,
)
