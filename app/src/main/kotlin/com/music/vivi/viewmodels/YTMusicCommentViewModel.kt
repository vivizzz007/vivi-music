/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.Run
import com.music.innertube.models.Runs
import com.music.innertube.models.Thumbnail
import com.music.innertube.models.Thumbnails
import com.music.innertube.models.comment.CommentRenderer
import com.music.innertube.models.comment.CommentThreadRenderer
import com.music.vivi.ui.screens.CommentSort
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "YTMusicCommentVM"

@HiltViewModel
class YTMusicCommentViewModel @Inject constructor() : ViewModel() {

    // --- State: Main Comments ---
    private val _comments = MutableStateFlow<List<CommentThreadRenderer>>(emptyList())
    val comments: StateFlow<List<CommentThreadRenderer>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    private val _totalComments = MutableStateFlow<String?>(null)
    val totalComments: StateFlow<String?> = _totalComments.asStateFlow()

    private val _selectedSort = MutableStateFlow(CommentSort.TOP)
    val selectedSort: StateFlow<CommentSort> = _selectedSort.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    // Continuation Tokens
    private var currentVideoId: String? = null
    private var nextToken: String? = null
    private var topToken: String? = null
    private var newestToken: String? = null
    private val deletedCommentIds = mutableSetOf<String>()

    // --- State: Replies & Detail View ---
    private val _selectedThread = MutableStateFlow<CommentThreadRenderer?>(null)
    val selectedThread: StateFlow<CommentThreadRenderer?> = _selectedThread.asStateFlow()

    private val _replies = MutableStateFlow<List<CommentRenderer>>(emptyList())
    val replies: StateFlow<List<CommentRenderer>> = _replies.asStateFlow()

    private val _isLoadingReplies = MutableStateFlow(false)
    val isLoadingReplies: StateFlow<Boolean> = _isLoadingReplies.asStateFlow()

    private var nextRepliesToken: String? = null

    /**
     * Load initial comments for a video.
     * Always fetches fresh, dynamic comments directly from YouTube.
     * Set [forceRefresh] to true to reload when user logs in or switches accounts.
     */
    fun loadComments(videoId: String, forceRefresh: Boolean = false) {
        if (!forceRefresh && currentVideoId == videoId && _comments.value.isNotEmpty()) return
        currentVideoId = videoId
        _isLoading.value = true
        _isError.value = false
        _comments.value = emptyList()

        viewModelScope.launch {
            YouTube.comments(videoId).onSuccess { result ->
                _comments.value = result.comments.filterNot { it.comment?.commentRenderer?.commentId in deletedCommentIds }
                nextToken = result.nextToken
                _totalComments.value = result.totalComments
                topToken = result.topToken
                newestToken = result.newestToken
                _isLoading.value = false
                Log.d(TAG, "Loaded ${result.comments.size} comments for video $videoId (filtered ${_comments.value.size})")
            }.onFailure { ex ->
                Log.e(TAG, "Failed to load comments for video $videoId", ex)
                _isLoading.value = false
                _isError.value = true
            }
        }
    }

    /**
     * Load more comments when scrolling near bottom.
     */
    fun loadMoreComments() {
        val token = nextToken ?: return
        if (_isLoadingMore.value) return

        _isLoadingMore.value = true
        viewModelScope.launch {
            YouTube.commentContinuation(token).onSuccess { result ->
                _comments.value = (_comments.value + result.comments).distinctBy {
                    it.comment?.commentRenderer?.commentId ?: it.hashCode()
                }.filterNot { it.comment?.commentRenderer?.commentId in deletedCommentIds }
                nextToken = result.nextToken
                if (result.totalComments != null) _totalComments.value = result.totalComments
                _isLoadingMore.value = false
            }.onFailure { ex ->
                Log.e(TAG, "Failed to load more comments", ex)
                _isLoadingMore.value = false
            }
        }
    }

    /**
     * Switch sort order between Top and Newest comments.
     */
    fun switchSort(sort: CommentSort) {
        if (_selectedSort.value == sort) return
        _selectedSort.value = sort

        val videoId = currentVideoId
        val sortIndex = if (sort == CommentSort.NEWEST) 1 else 0
        val targetToken = (if (sort == CommentSort.NEWEST) newestToken else topToken)
            ?: if (videoId != null) YouTube.createCommentsContinuationToken(videoId, sortIndex) else null

        if (targetToken != null) {
            _isLoading.value = true
            _comments.value = emptyList() // Clear old comments immediately so user sees progress feedback
            viewModelScope.launch {
                YouTube.commentContinuation(targetToken).onSuccess { result ->
                    _comments.value = result.comments.filterNot { it.comment?.commentRenderer?.commentId in deletedCommentIds }
                    nextToken = result.nextToken
                    if (result.totalComments != null) _totalComments.value = result.totalComments
                    if (result.topToken != null) topToken = result.topToken
                    if (result.newestToken != null) newestToken = result.newestToken
                    _isLoading.value = false
                }.onFailure { ex ->
                    Log.e(TAG, "Failed to load sorted comments", ex)
                    _isLoading.value = false
                    _isError.value = true
                }
            }
        }
    }

    /**
     * Select a comment thread to view its replies in detail view.
     */
    fun selectThread(thread: CommentThreadRenderer?) {
        _selectedThread.value = thread
        if (thread != null) {
            loadReplies(thread)
        } else {
            _replies.value = emptyList()
            nextRepliesToken = null
        }
    }

    /**
     * Fetch replies for a given comment thread.
     */
    fun loadReplies(thread: CommentThreadRenderer) {
        val token = thread.replies?.commentRepliesRenderer?.contents
            ?.firstOrNull()?.continuationItemRenderer?.continuationEndpoint
            ?.continuationCommand?.token
            ?: thread.replies?.commentRepliesRenderer?.viewReplies?.buttonRenderer
                ?.command?.continuationCommand?.token

        if (token == null) {
            _replies.value = emptyList()
            return
        }

        _isLoadingReplies.value = true
        _replies.value = emptyList()
        nextRepliesToken = token

        viewModelScope.launch {
            YouTube.commentReplies(token).onSuccess { (repliesList, next) ->
                _replies.value = repliesList
                nextRepliesToken = next
                _isLoadingReplies.value = false
            }.onFailure { ex ->
                Log.e(TAG, "Failed to load replies", ex)
                _isLoadingReplies.value = false
            }
        }
    }

    /**
     * Load more replies in the active thread.
     */
    fun loadMoreReplies() {
        val token = nextRepliesToken ?: return
        if (_isLoadingReplies.value) return

        _isLoadingReplies.value = true
        viewModelScope.launch {
            YouTube.commentReplies(token).onSuccess { (repliesList, next) ->
                _replies.value = (_replies.value + repliesList).distinctBy {
                    it.commentId ?: it.hashCode()
                }
                nextRepliesToken = next
                _isLoadingReplies.value = false
            }.onFailure { ex ->
                Log.e(TAG, "Failed to load more replies", ex)
                _isLoadingReplies.value = false
            }
        }
    }

    /**
     * Post a new top-level comment.
     * Inserts optimistically into the UI immediately.
     */
    fun postComment(
        videoId: String,
        text: String,
        authorName: String,
        authorAvatar: String?,
        channelHandle: String?,
        createParams: String? = null,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        if (text.isBlank() || _isSubmitting.value) return
        _isSubmitting.value = true

        val optimisticThread = CommentThreadRenderer(
            comment = CommentThreadRenderer.Comment(
                commentRenderer = CommentRenderer(
                    authorText = Runs(runs = listOf(Run(text = authorName.ifBlank { "You" }))),
                    authorThumbnail = Thumbnails(
                        thumbnails = listOf(Thumbnail(url = authorAvatar.orEmpty(), width = 0, height = 0))
                    ),
                    contentText = Runs(runs = listOf(Run(text = text))),
                    publishedTimeText = Runs(runs = listOf(Run(text = "Just now"))),
                    commentId = "temp-${System.currentTimeMillis()}",
                    voteCount = Runs(runs = listOf(Run(text = "0"))),
                    replyCount = 0,
                    isAuthor = true
                )
            )
        )

        // Optimistically prepend to the top of list
        _comments.value = listOf(optimisticThread) + _comments.value

        viewModelScope.launch {
            YouTube.createComment(videoId, text, createParams)
                .onSuccess {
                    _isSubmitting.value = false
                    onSuccess()
                    Log.i(TAG, "Comment successfully posted, reloading comments to replace temp placeholder...")
                    loadComments(videoId, forceRefresh = true)
                }
                .onFailure { ex ->
                    _isSubmitting.value = false
                    Log.e(TAG, "Failed to post comment", ex)
                    onError(ex)
                }
        }
    }

    /**
     * Post a reply to a thread.
     * Inserts optimistically into the replies list immediately.
     */
    fun postReply(
        text: String,
        createReplyParams: String? = null,
        videoId: String = currentVideoId.orEmpty(),
        commentId: String = "",
        authorName: String,
        authorAvatar: String?,
        channelHandle: String?,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        if (text.isBlank() || _isSubmitting.value) return
        _isSubmitting.value = true

        val optimisticReply = CommentRenderer(
            authorText = Runs(runs = listOf(Run(text = authorName.ifBlank { "You" }))),
            authorThumbnail = Thumbnails(
                thumbnails = listOf(Thumbnail(url = authorAvatar.orEmpty(), width = 0, height = 0))
            ),
            contentText = Runs(runs = listOf(Run(text = text))),
            publishedTimeText = Runs(runs = listOf(Run(text = "Just now"))),
            commentId = "temp-reply-${System.currentTimeMillis()}",
            voteCount = Runs(runs = listOf(Run(text = "0"))),
            replyCount = 0,
            isAuthor = true
        )

        // Optimistically append to replies
        _replies.value = _replies.value + optimisticReply

        viewModelScope.launch {
            YouTube.createCommentReply(
                commentText = text,
                createReplyParams = createReplyParams,
                videoId = videoId,
                commentId = commentId
            )
                .onSuccess {
                    _isSubmitting.value = false
                    onSuccess()
                    Log.i(TAG, "Reply successfully posted")
                }
                .onFailure { ex ->
                    _isSubmitting.value = false
                    Log.e(TAG, "Failed to post reply", ex)
                    onError(ex)
                }
        }
    }

    /**
     * Delete a top-level comment.
     * Performs optimistic local removal first, then dispatches delete request if token exists.
     */
    fun deleteComment(
        commentId: String,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        deletedCommentIds.add(commentId)
        val threadToDelete = _comments.value.firstOrNull {
            it.comment?.commentRenderer?.commentId == commentId
        }
        val actionToken = threadToDelete?.comment?.commentRenderer?.getDeleteActionToken()
            ?: currentVideoId?.let { YouTube.buildDeleteActionToken(commentId, it) }
        android.util.Log.d("CommentDelete", "deleteComment() called: commentId=$commentId, foundThread=${threadToDelete != null}, isTemp=${commentId.startsWith("temp-")}, hasActionToken=${actionToken != null}, tokenLen=${actionToken?.length}")

        // 1. Optimistic removal
        _comments.value = _comments.value.filterNot {
            it.comment?.commentRenderer?.commentId == commentId
        }
        if (_selectedThread.value?.comment?.commentRenderer?.commentId == commentId) {
            _selectedThread.value = null
        }

        // 2. Dispatch network deletion if token is present
        if (actionToken != null) {
            viewModelScope.launch {
                YouTube.deleteComment(actionToken)
                    .onSuccess {
                        Log.i("CommentDelete", "deleteComment succeeded on YouTube for $commentId")
                        onSuccess()
                    }
                    .onFailure { ex ->
                        Log.e("CommentDelete", "deleteComment failed on YouTube for $commentId", ex)
                        onError(ex)
                    }
            }
        } else {
            Log.d("CommentDelete", "Comment $commentId deleted locally (no action token present, isTemp=${commentId.startsWith("temp-")})")
            onSuccess()
        }
    }

    /**
     * Delete a reply.
     * Performs optimistic local removal first, then dispatches delete request if token exists.
     */
    fun deleteReply(
        commentId: String,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        deletedCommentIds.add(commentId)
        val replyToDelete = _replies.value.firstOrNull { it.commentId == commentId }
        val actionToken = replyToDelete?.getDeleteActionToken()
            ?: currentVideoId?.let { YouTube.buildDeleteActionToken(commentId, it) }
        android.util.Log.d("CommentDelete", "deleteReply() called: replyId=$commentId, foundReply=${replyToDelete != null}, isTemp=${commentId.startsWith("temp-")}, hasActionToken=${actionToken != null}, tokenLen=${actionToken?.length}")

        // 1. Optimistic removal
        _replies.value = _replies.value.filterNot { it.commentId == commentId }

        // 2. Dispatch network deletion if token is present
        if (actionToken != null) {
            viewModelScope.launch {
                YouTube.deleteComment(actionToken)
                    .onSuccess {
                        Log.i("CommentDelete", "deleteReply succeeded on YouTube for $commentId")
                        onSuccess()
                    }
                    .onFailure { ex ->
                        Log.e("CommentDelete", "deleteReply failed on YouTube for $commentId", ex)
                        onError(ex)
                    }
            }
        } else {
            Log.d("CommentDelete", "Reply $commentId deleted locally (no action token present, isTemp=${commentId.startsWith("temp-")})")
            onSuccess()
        }
    }
}
