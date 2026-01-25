package com.music.innertube.models.response

import com.music.innertube.models.PlaylistPanelRenderer
import kotlinx.serialization.Serializable

@Serializable
data class GetQueueResponse(val queueDatas: List<QueueData>) {
    @Serializable
    data class QueueData(val content: PlaylistPanelRenderer.Content)
}
