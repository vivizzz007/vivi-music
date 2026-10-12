/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.innertube.models.body

import com.music.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class CreateCommentBody(
    val context: Context,
    val commentText: String,
    val createCommentParams: String,
)

@Serializable
data class CreateCommentReplyBody(
    val context: Context,
    val commentText: String,
    val createReplyParams: String,
)

@Serializable
data class PerformCommentActionBody(
    val context: Context,
    val actions: List<String>,
)
