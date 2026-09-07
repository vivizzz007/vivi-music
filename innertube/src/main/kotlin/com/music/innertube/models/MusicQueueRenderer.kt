package com.music.innertube.models

import com.music.innertube.models.NavigationEndpoint
import kotlinx.serialization.Serializable

@Serializable
data class MusicQueueRenderer(
    val content: Content?,
    val header: Header?,
    val subHeaderChipCloud: Header.MusicQueueHeaderRenderer.ChipCloud? = null,
) {
    @Serializable
    data class Content(
        val playlistPanelRenderer: PlaylistPanelRenderer,
    )

    @Serializable
    data class Header(
        val musicQueueHeaderRenderer: MusicQueueHeaderRenderer?,
    ) {
        @Serializable
        data class MusicQueueHeaderRenderer(
            val title: Runs?,
            val subtitle: Runs?,
            val chips: ChipCloud?,
        ) {
            @Serializable
            data class ChipCloud(
                val chipCloudRenderer: ChipCloudRenderer?
            ) {
                @Serializable
                data class ChipCloudRenderer(
                    val chips: List<Chip>?
                ) {
                    @Serializable
                    data class Chip(
                        val chipCloudChipRenderer: ChipCloudChipRenderer?
                    ) {
                        @Serializable
                        data class ChipCloudChipRenderer(
                            val text: Runs?,
                            val navigationEndpoint: NavigationEndpoint?,
                            val isSelected: Boolean = false,
                        )
                    }
                }
            }
        }
    }
}
