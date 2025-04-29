package com.music.vivi.models

data class GithubRelease(
    val tag_name: String,
    val body: String,
    val assets: List<Asset>
)

data class Asset(
    val browser_download_url: String,
    val name: String,
    val size: Int
)