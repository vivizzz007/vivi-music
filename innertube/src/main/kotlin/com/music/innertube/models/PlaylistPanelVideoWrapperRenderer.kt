package com.music.innertube.models

import kotlinx.serialization.Serializable

/**
 * YouTube Music wraps a queue entry in this renderer when the track exists both as an audio-only
 * "song" and as a music video. [primaryRenderer] is the version that was requested, [counterpart]
 * holds the other one (this is what powers the Song / Video switch on music.youtube.com).
 */
@Serializable
data class PlaylistPanelVideoWrapperRenderer(
    val primaryRenderer: PrimaryRenderer? = null,
    val counterpart: List<Counterpart>? = null,
) {
    @Serializable
    data class PrimaryRenderer(
        val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer? = null,
    )

    @Serializable
    data class Counterpart(
        val counterpartRenderer: CounterpartRenderer? = null,
    )

    @Serializable
    data class CounterpartRenderer(
        val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer? = null,
    )
}
