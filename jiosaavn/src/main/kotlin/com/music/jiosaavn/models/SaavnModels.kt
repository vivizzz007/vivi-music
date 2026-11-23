/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.jiosaavn.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SaavnDownloadUrl(
    @SerialName("quality") val quality: String = "",
    @SerialName("url")     val url: String     = ""
)

@Serializable
data class SaavnImage(
    @SerialName("quality") val quality: String = "",
    @SerialName("url")     val url: String     = ""
)

@Serializable
data class SaavnArtistItem(
    @SerialName("id")   val id: String   = "",
    @SerialName("name") val name: String = ""
)

@Serializable
data class SaavnArtists(
    @SerialName("primary")  val primary:  List<SaavnArtistItem> = emptyList(),
    @SerialName("featured") val featured: List<SaavnArtistItem> = emptyList(),
    @SerialName("all")      val all:      List<SaavnArtistItem> = emptyList()
)

@Serializable
data class SaavnAlbum(
    @SerialName("id")   val id:   String? = null,
    @SerialName("name") val name: String? = null
)

@Serializable
data class SaavnSong(
    @SerialName("id")              val id:              String                 = "",
    @SerialName("name")            val name:            String                 = "",
    @SerialName("duration")        val duration:        Int?                   = null,
    @SerialName("explicitContent") val explicitContent: Boolean                = false,
    @SerialName("artists")         val artists:         SaavnArtists           = SaavnArtists(),
    @SerialName("image")           val image:           List<SaavnImage>       = emptyList(),
    @SerialName("downloadUrl")     val downloadUrl:     List<SaavnDownloadUrl> = emptyList(),
    @SerialName("album")           val album:           SaavnAlbum?            = null,
    /** True when JioSaavn marks this track as Pro-Only (paid/premium content). */
    val isProOnly: Boolean = false
)

