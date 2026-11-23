/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.jiosaavn.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RawArtistMapItem(
    val id: String = "",
    val name: String = "",
    val role: String = "",
    val type: String = ""
)

@Serializable
data class RawArtistMap(
    @SerialName("primary_artists") val primaryArtists: List<RawArtistMapItem> = emptyList(),
    @SerialName("featured_artists") val featuredArtists: List<RawArtistMapItem> = emptyList(),
    val artists: List<RawArtistMapItem> = emptyList()
)

/**
 * Rights information returned by JioSaavn for every song.
 * code == "1" means the track is Pro-Only (requires a paid subscription).
 */
@Serializable
data class RawRights(
    val code: String = "",
    val cacheable: String = "",
    @SerialName("delete_cached_object") val deleteCachedObject: String = "",
    val reason: String = ""
) {
    /** True when this track is gated behind JioSaavn Pro. */
    val isProOnly: Boolean
        get() = code == "1" || reason.contains("Pro Only", ignoreCase = true)
}

@Serializable
data class RawMoreInfo(
    val album_id: String = "",
    val album: String = "",
    @SerialName("encrypted_media_url") val encryptedMediaUrl: String = "",
    val duration: String = "",
    val artistMap: RawArtistMap = RawArtistMap(),
    /** Pro/rights gating metadata. code=="1" means Pro-Only. */
    val rights: RawRights = RawRights()
)

@Serializable
data class RawSongItem(
    val id: String = "",
    val title: String = "",
    val type: String = "",
    val year: String = "",
    val image: String = "",
    val language: String = "",
    @SerialName("play_count") val playCount: String = "",
    @SerialName("explicit_content") val explicitContent: String = "",
    @SerialName("more_info") val moreInfo: RawMoreInfo = RawMoreInfo()
)

@Serializable
data class RawSearchResponse(
    val total: Int = 0,
    val start: Int = 0,
    val results: List<RawSongItem> = emptyList()
)

