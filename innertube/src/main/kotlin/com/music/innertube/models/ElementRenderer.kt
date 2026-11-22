/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.innertube.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class ElementRenderer(
    val newElement: NewElement? = null,
) {
    @Serializable
    data class NewElement(
        val type: ElementType? = null,
    ) {
        @Serializable
        data class ElementType(
            val componentType: ComponentType? = null,
        ) {
            @Serializable
            data class ComponentType(
                val model: ElementModel? = null,
            ) {
                @Serializable
                data class ElementModel(
                    val timedLyricsModel: TimedLyricsModel? = null,
                    val musicMessageModel: MusicMessageModel? = null,
                ) {
                    @Serializable
                    data class TimedLyricsModel(
                        val lyricsData: LyricsData? = null,
                    ) {
                        @Serializable
                        data class LyricsData(
                            val timedLyricsData: List<TimedLyricItem>? = null,
                        ) {
                            @Serializable
                            data class TimedLyricItem(
                                val lyricLine: String? = null,
                                val cueRange: CueRange? = null,
                            ) {
                                @Serializable
                                data class CueRange(
                                    val startTimeMilliseconds: JsonPrimitive? = null,
                                    val endTimeMilliseconds: JsonPrimitive? = null,
                                )
                            }
                        }
                    }

                    @Serializable
                    data class MusicMessageModel(
                        val text: String? = null,
                    )
                }
            }
        }
    }
}

