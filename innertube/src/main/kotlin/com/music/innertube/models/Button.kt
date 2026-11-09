/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Button(
    val buttonRenderer: ButtonRenderer = ButtonRenderer(),
) {
    @Serializable
    data class ButtonRenderer(
        val text: Runs? = null,
        val navigationEndpoint: NavigationEndpoint? = null,
        val command: NavigationEndpoint? = null,
        val icon: Icon? = null,
    )
}
