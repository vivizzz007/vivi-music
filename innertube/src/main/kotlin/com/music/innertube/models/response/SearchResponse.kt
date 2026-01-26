package com.music.innertube.models.response

import com.music.innertube.models.Continuation
import com.music.innertube.models.MusicResponsiveListItemRenderer
import com.music.innertube.models.Tabs
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(val contents: Contents?, val continuationContents: ContinuationContents?) {
    @Serializable
    data class Contents(val tabbedSearchResultsRenderer: Tabs?)

    @Serializable
    data class ContinuationContents(val musicShelfContinuation: MusicShelfContinuation) {
        @Serializable
        data class MusicShelfContinuation(val contents: List<Content>, val continuations: List<Continuation>?) {
            @Serializable
            data class Content(val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer)
        }
    }
}
