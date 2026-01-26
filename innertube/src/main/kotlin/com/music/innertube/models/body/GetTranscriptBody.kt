package com.music.innertube.models.body

import com.music.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(val context: Context, val params: String)
