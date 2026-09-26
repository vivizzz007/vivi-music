package com.music.lastfm.models

import kotlinx.serialization.Serializable

@Serializable
data class UserInfoResponse(
    val user: UserInfo
)

@Serializable
data class UserInfo(
    val name: String,
    val playcount: String = "0",
    val image: List<LastFmImage> = emptyList()
)

@Serializable
data class LastFmImage(
    val size: String,
    @kotlinx.serialization.SerialName("#text") val url: String
)
