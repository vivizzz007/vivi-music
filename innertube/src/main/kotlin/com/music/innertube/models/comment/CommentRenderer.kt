/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.innertube.models.comment

import com.music.innertube.models.Runs
import com.music.innertube.models.Thumbnails
import com.music.innertube.models.NavigationEndpoint
import kotlinx.serialization.Serializable

@Serializable
data class CommentThreadRenderer(
    val comment: Comment? = null,
    val commentViewModel: CommentViewModelWrapper? = null,
    val replies: Replies? = null,
    val menu: CommentRenderer.ActionMenu? = null,
    val actionMenu: CommentRenderer.ActionMenu? = null,
    val actionButtons: CommentRenderer.ActionButtons? = null,
) {
    @Serializable
    data class CommentViewModelWrapper(
        val commentViewModel: CommentViewModel? = null,
    ) {
        @Serializable
        data class CommentViewModel(
            val commentId: String? = null,
            val commentKey: String? = null,
            val menu: CommentRenderer.ActionMenu? = null,
            val actionMenu: CommentRenderer.ActionMenu? = null,
            val actionButtons: CommentRenderer.ActionButtons? = null,
        )
    }
    
    @Serializable
    data class Comment(
        val commentRenderer: CommentRenderer? = null,
    )

    @Serializable
    data class Replies(
        val commentRepliesRenderer: CommentRepliesRenderer? = null,
    ) {
        @Serializable
        data class CommentRepliesRenderer(
            val contents: List<Content>? = null,
            val viewReplies: com.music.innertube.models.Button? = null,
            val hideReplies: com.music.innertube.models.Button? = null,
        ) {
            @Serializable
            data class Content(
                val continuationItemRenderer: ContinuationItemRenderer? = null,
            )
        }
    }
}

@Serializable
data class CommentRenderer(
    val authorText: Runs? = null,
    val authorThumbnail: Thumbnails? = null,
    val contentText: Runs? = null,
    val publishedTimeText: Runs? = null,
    val authorEndpoint: NavigationEndpoint? = null,
    val commentId: String? = null,
    val voteCount: Runs? = null,
    val voteStatus: String? = null,
    val replyCount: Int? = null,
    val actionButtons: ActionButtons? = null,
    val isAuthor: Boolean = false,
    val actionMenu: ActionMenu? = null,
) {
    fun getDeleteActionToken(): String? {
        val menuAction = actionMenu?.getDeleteActionToken()
        if (menuAction != null) return menuAction

        return actionButtons?.commentActionButtonsRenderer?.deleteButton
            ?.buttonRenderer?.serviceEndpoint?.performCommentActionEndpoint?.action
            ?: actionButtons?.commentActionButtonsRenderer?.deleteButton
                ?.buttonRenderer?.command?.performCommentActionEndpoint?.action
    }

    @Serializable
    data class ActionMenu(
        val menuRenderer: MenuRenderer? = null,
    ) {
        fun getDeleteActionToken(): String? {
            val items = menuRenderer?.items.orEmpty()
            return items.mapNotNull { item ->
                val serviceItem = item.menuServiceItemRenderer
                val navItem = item.menuNavigationItemRenderer
                val text = (serviceItem?.text ?: navItem?.text)?.runs?.firstOrNull()?.text?.lowercase().orEmpty()
                val action = serviceItem?.serviceEndpoint?.performCommentActionEndpoint?.action
                    ?: serviceItem?.command?.performCommentActionEndpoint?.action
                    ?: navItem?.serviceEndpoint?.performCommentActionEndpoint?.action
                    ?: navItem?.command?.performCommentActionEndpoint?.action
                if (action != null && text.contains("delete")) {
                    action
                } else {
                    null
                }
            }.firstOrNull()
        }

        @Serializable
        data class MenuRenderer(
            val items: List<Item>? = null,
        ) {
            @Serializable
            data class Item(
                val menuServiceItemRenderer: MenuServiceItemRenderer? = null,
                val menuNavigationItemRenderer: MenuNavigationItemRenderer? = null,
            ) {
                @Serializable
                data class MenuServiceItemRenderer(
                    val text: Runs? = null,
                    val serviceEndpoint: ServiceEndpoint? = null,
                    val command: ServiceEndpoint? = null,
                ) {
                    @Serializable
                    data class ServiceEndpoint(
                        val performCommentActionEndpoint: PerformCommentActionEndpoint? = null,
                    ) {
                        @Serializable
                        data class PerformCommentActionEndpoint(
                            val action: String? = null,
                        )
                    }
                }

                @Serializable
                data class MenuNavigationItemRenderer(
                    val text: Runs? = null,
                    val serviceEndpoint: MenuServiceItemRenderer.ServiceEndpoint? = null,
                    val command: MenuServiceItemRenderer.ServiceEndpoint? = null,
                )
            }
        }
    }

    @Serializable
    data class ActionButtons(
        val commentActionButtonsRenderer: CommentActionButtonsRenderer? = null,
    ) {
        @Serializable
        data class CommentActionButtonsRenderer(
            val replyButton: ReplyButton? = null,
            val deleteButton: DeleteButton? = null,
        ) {
            @Serializable
            data class DeleteButton(
                val buttonRenderer: ButtonRenderer? = null,
            ) {
                @Serializable
                data class ButtonRenderer(
                    val serviceEndpoint: ServiceEndpoint? = null,
                    val command: ServiceEndpoint? = null,
                ) {
                    @Serializable
                    data class ServiceEndpoint(
                        val performCommentActionEndpoint: PerformCommentActionEndpoint? = null,
                    ) {
                        @Serializable
                        data class PerformCommentActionEndpoint(
                            val action: String? = null,
                        )
                    }
                }
            }

            @Serializable
            data class ReplyButton(
                val buttonRenderer: ButtonRenderer? = null,
            ) {
                @Serializable
                data class ButtonRenderer(
                    val serviceEndpoint: ServiceEndpoint? = null,
                    val command: ServiceEndpoint? = null,
                ) {
                    @Serializable
                    data class ServiceEndpoint(
                        val createCommentReplyEndpoint: CreateCommentReplyEndpoint? = null,
                        val performCommentActionEndpoint: PerformCommentActionEndpoint? = null,
                    ) {
                        @Serializable
                        data class CreateCommentReplyEndpoint(
                            val createReplyParams: String? = null,
                        )
                        @Serializable
                        data class PerformCommentActionEndpoint(
                            val action: String? = null,
                        )
                    }
                }
            }
        }
    }
}

@Serializable
data class ContinuationItemRenderer(
    val trigger: String? = null,
    val continuationEndpoint: ContinuationEndpoint? = null,
    val button: com.music.innertube.models.Button? = null,
) {
    @Serializable
    data class ContinuationEndpoint(
        val clickTrackingParams: String? = null,
        val continuationCommand: ContinuationCommand? = null,
    ) {
        @Serializable
        data class ContinuationCommand(
            val token: String? = null,
        )
    }
}
