/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.innertube.models.comment

import com.music.innertube.models.Runs
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
            val commentsHeaderRenderer: CommentsHeaderRenderer? = null,
            val commentThreadRenderer: CommentThreadRenderer? = null,
            val continuationItemRenderer: ContinuationItemRenderer? = null,
            val commentRenderer: CommentRenderer? = null,
            val commentViewModel: CommentThreadRenderer.CommentViewModelWrapper.CommentViewModel? = null,
        )
    }

    @Serializable
    data class CommentsHeaderRenderer(
        val countText: Runs? = null,
        val sortMenu: SortMenu? = null,
    ) {
        @Serializable
        data class SortMenu(
            val sortFilterSubMenuRenderer: SortFilterSubMenuRenderer? = null,
        ) {
            @Serializable
            data class SortFilterSubMenuRenderer(
                val subMenuItems: List<SubMenuItem>? = null,
            ) {
                @Serializable
                data class SubMenuItem(
                    val title: String? = null,
                    val selected: Boolean? = null,
                    val continuation: Continuation? = null,
                    val serviceEndpoint: ServiceEndpoint? = null,
                ) {
                    @Serializable
                    data class Continuation(
                        val reloadContinuationItemsCommand: ReloadContinuationItemsCommand? = null,
                    ) {
                        @Serializable
                        data class ReloadContinuationItemsCommand(
                            val token: String? = null,
                        )
                    }

                    @Serializable
                    data class ServiceEndpoint(
                        val continuationCommand: ContinuationCommand? = null,
                    ) {
                        @Serializable
                        data class ContinuationCommand(
                            val token: String? = null,
                        )
                    }
                }
            }
        }
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
                            val channelId: String? = null,
                            val isCurrentUser: Boolean? = null,
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
