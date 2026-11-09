/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicResponsiveHeaderRenderer(
    val thumbnail: ThumbnailRenderer?,
    val buttons: List<Button>,
    val title: Runs,
    val subtitle: Runs,
    val secondSubtitle: Runs?,
    val straplineTextOne: Runs?,
    val straplineThumbnail: ThumbnailRenderer? = null,
    val facepile: Facepile? = null,
    val description: Description? = null,
) {
    @Serializable
    data class Facepile(
        val avatarStackViewModel: AvatarStackViewModel? = null,
    ) {
        @Serializable
        data class AvatarStackViewModel(
            val avatars: List<AvatarWrapper>? = null,
            val text: TextContent? = null,
            val rendererContext: RendererContext? = null,
        ) {
            @Serializable
            data class AvatarWrapper(
                val avatarViewModel: AvatarViewModel? = null,
            ) {
                @Serializable
                data class AvatarViewModel(
                    val image: AvatarImage? = null,
                ) {
                    @Serializable
                    data class AvatarImage(
                        val sources: List<Source>? = null,
                    ) {
                        @Serializable
                        data class Source(
                            val url: String,
                            val width: Int? = null,
                            val height: Int? = null,
                        )
                    }
                }
            }

            @Serializable
            data class TextContent(
                val content: String? = null,
            )

            @Serializable
            data class RendererContext(
                val commandContext: CommandContext? = null,
            ) {
                @Serializable
                data class CommandContext(
                    val onTap: OnTap? = null,
                ) {
                    @Serializable
                    data class OnTap(
                        val innertubeCommand: NavigationEndpoint? = null,
                    )
                }
            }
        }

        fun getAuthorName(): String? = avatarStackViewModel?.text?.content

        fun getAuthorThumbnailUrl(): String? =
            avatarStackViewModel?.avatars?.firstOrNull()?.avatarViewModel?.image?.sources?.lastOrNull()?.url

        fun getAuthorBrowseId(): String? =
            avatarStackViewModel?.rendererContext?.commandContext?.onTap?.innertubeCommand?.browseEndpoint?.browseId
    }

    @Serializable
    data class Description(
        val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer?,
    )

    @Serializable
    data class Button(
        val musicPlayButtonRenderer: MusicPlayButtonRenderer?,
        val menuRenderer: Menu.MenuRenderer?
    ) {
        @Serializable
        data class MusicPlayButtonRenderer(
            val playNavigationEndpoint: NavigationEndpoint?,
        )
    }
}
