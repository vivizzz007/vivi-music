package com.music.vivi.vivimusic.updater

data class SDUIModifier(
    val weight: Float? = null,
    val heightDp: Int? = null,
    val fillMaxWidth: Boolean = false,
    val paddingDp: Int? = null,
    val aspectRatio: Float? = null
)

class SDUIBlock(
    val type: String,
    val url: String? = null,
    val modifierParams: SDUIModifier? = null,
    val children: List<SDUIBlock>? = null
)

data class ChangelogSection(val title: String, val items: List<String>, val description: String? = null, val blocks: List<SDUIBlock>? = null)

sealed class ViviUpdateStatus {
    object Idle : ViviUpdateStatus()
    object Checking : ViviUpdateStatus()
    data class Available(
        val version: String,
        val changelog: List<ChangelogSection>,
        val size: String,
        val releaseDate: String,
        val description: String?,
        val imageUrl: String?,
        val apkUrl: String?
    ) : ViviUpdateStatus()

    data class NoUpdate(val version: String) : ViviUpdateStatus()
    data class Error(val message: String) : ViviUpdateStatus()
}
