package com.music.innertube.models.body

import com.music.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetQueueBody(val context: Context, val videoIds: List<String>?, val playlistId: String?)
