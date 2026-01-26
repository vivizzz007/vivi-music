package com.music.innertube.models.body

import com.music.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(val context: Context, val browseId: String?, val params: String?, val continuation: String?)
