package com.music.innertube.models.body

import com.music.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SubscribeBody(val channelIds: List<String>, val context: Context)
