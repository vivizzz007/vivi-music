package com.music.innertube.models.comment

import kotlinx.serialization.Serializable

@Serializable
data class CommentResponse(
    val onResponseReceivedEndpoints: List<OnResponseReceivedEndpoint>? = null,
    val frameworkUpdates: FrameworkUpdates? = null,
) {
    @Serializable
    data class OnResponseReceivedEndpoint(
        val reloadContinuationItemsCommand: ReloadContinuationItemsCommand? = null,
        val appendContinuationItemsAction: AppendContinuationItemsAction? = null,
    ) {
        @Serializable
        data class ReloadContinuationItemsCommand(
            val continuationItems: List<ContinuationItem>? = null,
        )
        @Serializable
        data class AppendContinuationItemsAction(
            val continuationItems: List<ContinuationItem>? = null,
        )
        @Serializable
        data class ContinuationItem(
            val commentThreadRenderer: CommentThreadRenderer? = null,
            val continuationItemRenderer: ContinuationItemRenderer? = null,
            val commentRenderer: CommentRenderer? = null,
        )
    }

    @Serializable
    data class FrameworkUpdates(
        val entityBatchUpdate: EntityBatchUpdate? = null,
    ) {
        @Serializable
        data class EntityBatchUpdate(
            val mutations: List<Mutation>? = null,
        ) {
            @Serializable
            data class Mutation(
                val payload: Payload? = null,
            ) {
                @Serializable
                data class Payload(
                    val commentEntityPayload: CommentEntityPayload? = null,
                    val engagementToolbarStateEntityPayload: EngagementToolbarStateEntityPayload? = null,
                    val engagementToolbarSurfaceEntityPayload: EngagementToolbarSurfaceEntityPayload? = null,
                ) {
                    @Serializable
                    data class CommentEntityPayload(
                        val key: String? = null,
                        val properties: Properties? = null,
                        val author: Author? = null,
                        val toolbar: EngagementToolbarSurfaceEntityPayload.Toolbar? = null,
                    ) {
                        @Serializable
                        data class Properties(
                            val commentId: String? = null,
                            val content: Content? = null,
                            val publishedTime: String? = null,
                            val toolbarStateKey: String? = null,
                            val toolbarSurfaceKey: String? = null,
                        ) {
                            @Serializable
                            data class Content(
                                val content: String? = null,
                            )
                        }

                        @Serializable
                        data class Author(
                            val displayName: String? = null,
                            val avatarThumbnailUrl: String? = null,
                        )
                    }

                    @Serializable
                    data class EngagementToolbarStateEntityPayload(
                        val key: String? = null,
                        val likeState: String? = null,
                        val heartState: String? = null,
                    )

                    @Serializable
                    data class EngagementToolbarSurfaceEntityPayload(
                        val key: String? = null,
                        val toolbar: Toolbar? = null,
                    ) {
                        @Serializable
                        data class Toolbar(
                            val likeCountNotliked: String? = null,
                            val likeCountLiked: String? = null,
                            val replyCount: String? = null,
                        )
                    }
                }
            }
        }
    }
}
