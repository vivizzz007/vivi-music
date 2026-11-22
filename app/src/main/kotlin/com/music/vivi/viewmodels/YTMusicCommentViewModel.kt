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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
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

    private val _likedCommentIds = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val likedCommentIds: StateFlow<Map<String, Boolean>> = _likedCommentIds.asStateFlow()

    fun toggleLikeComment(commentId: String, initialLiked: Boolean = false) {
        val current = _likedCommentIds.value[commentId] ?: initialLiked
        _likedCommentIds.value = _likedCommentIds.value + (commentId to !current)
    }

    /**
     * Calculates the accurate live reply count for a comment thread.
     * Checks renderer replyCount, parsed viewReplies button text, and replies contents.
     */
    fun getLiveReplyCount(thread: CommentThreadRenderer): Int {
        val rendererCount = thread.comment?.commentRenderer?.replyCount ?: 0
        if (rendererCount > 0) return rendererCount

        val viewRepliesText = thread.replies?.commentRepliesRenderer?.viewReplies
            ?.buttonRenderer?.text?.runs?.firstOrNull()?.text
        if (!viewRepliesText.isNullOrBlank()) {
            val parsed = viewRepliesText.filter { it.isDigit() }.toIntOrNull()
            if (parsed != null && parsed > 0) return parsed
        }

        return thread.replies?.commentRepliesRenderer?.contents?.size ?: 0
    }

    /**
     * Determines whether a comment was authored by the current logged-in user.
     */
    fun checkIsOwnComment(
        renderer: CommentRenderer,
        userName: String,
        userChannelHandle: String,
        isLoggedIn: Boolean
    ): Boolean {
        if (!isLoggedIn) return false
        if (renderer.commentId?.startsWith("local-") == true || renderer.commentId?.startsWith("temp-") == true) return true
        val author = renderer.authorText?.runs?.firstOrNull()?.text?.trim().orEmpty()
        val cleanUserHandle = userChannelHandle.trim().removePrefix("@")
        val cleanAuthor = author.removePrefix("@")
        val matchesUser = author.isNotBlank() && (
            (userName.isNotBlank() && author.equals(userName.trim(), ignoreCase = true)) ||
            (cleanUserHandle.isNotBlank() && cleanAuthor.equals(cleanUserHandle, ignoreCase = true))
        )
        if (matchesUser) return true
        if (renderer.isAuthor) return true
        if (author.isBlank() && renderer.getDeleteActionToken() != null) return true
        return false
    }

    // Continuation & Pagination Tokens
    private var currentVideoId: String? = null
    private var nextToken: String? = null
    private var topToken: String? = null
    private var newestToken: String? = null
    private val deletedCommentIds = mutableSetOf<String>()

    // In-Memory Dual-Sort Cache
    private var topComments = listOf<CommentThreadRenderer>()
    private var topNextToken: String? = null
    private var newestComments = listOf<CommentThreadRenderer>()
    private var newestNextToken: String? = null

    private val _hasNextPage = MutableStateFlow(false)
    val hasNextPage: StateFlow<Boolean> = _hasNextPage.asStateFlow()

    private val _hasNextRepliesPage = MutableStateFlow(false)
    val hasNextRepliesPage: StateFlow<Boolean> = _hasNextRepliesPage.asStateFlow()

    // --- State: Replies & Detail View ---
    private val _selectedThread = MutableStateFlow<CommentThreadRenderer?>(null)
    val selectedThread: StateFlow<CommentThreadRenderer?> = _selectedThread.asStateFlow()

    private val _replies = MutableStateFlow<List<CommentRenderer>>(emptyList())
    val replies: StateFlow<List<CommentRenderer>> = _replies.asStateFlow()

    private val _isLoadingReplies = MutableStateFlow(false)
    val isLoadingReplies: StateFlow<Boolean> = _isLoadingReplies.asStateFlow()

    private var nextRepliesToken: String? = null
    private var liveSyncJob: Job? = null

    /**
     * Load initial comments for a video.
     * Restores from in-memory cache if available, preventing unnecessary refetches.
     */
    fun loadComments(videoId: String, forceRefresh: Boolean = false) {
        val sameVideo = currentVideoId == videoId
        if (!forceRefresh && sameVideo && (topComments.isNotEmpty() || newestComments.isNotEmpty())) {
            val cached = if (_selectedSort.value == CommentSort.NEWEST) newestComments else topComments
            if (cached.isNotEmpty()) {
                _comments.value = cached
                nextToken = if (_selectedSort.value == CommentSort.NEWEST) newestNextToken else topNextToken
                _hasNextPage.value = nextToken != null
                _isLoading.value = false
                return
            }
        }

        if (!sameVideo) {
            _likedCommentIds.value = emptyMap()
            topComments = emptyList()
            topNextToken = null
            newestComments = emptyList()
            newestNextToken = null
            _selectedSort.value = CommentSort.TOP
            deletedCommentIds.clear()
        }

        currentVideoId = videoId
        _isLoading.value = true
        _isError.value = false
        if (!sameVideo || forceRefresh) {
            _comments.value = emptyList()
        }

        viewModelScope.launch {
            YouTube.comments(videoId).onSuccess { result ->
                val filtered = result.comments.filterNot { it.comment?.commentRenderer?.commentId in deletedCommentIds }
                topComments = filtered
                topNextToken = result.nextToken
                if (_selectedSort.value == CommentSort.TOP) {
                    _comments.value = filtered
                    nextToken = result.nextToken
                    _hasNextPage.value = nextToken != null
                }
                _totalComments.value = result.totalComments
                topToken = result.topToken
                newestToken = result.newestToken
                _isLoading.value = false
                Log.d(TAG, "Loaded ${result.comments.size} comments for video $videoId (filtered ${filtered.size})")
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
                val newItems = result.comments.filterNot { it.comment?.commentRenderer?.commentId in deletedCommentIds }
                val merged = (_comments.value + newItems).distinctBy {
                    it.comment?.commentRenderer?.commentId ?: it.hashCode()
                }
                _comments.value = merged
                nextToken = result.nextToken
                _hasNextPage.value = nextToken != null

                if (_selectedSort.value == CommentSort.TOP) {
                    topComments = merged
                    topNextToken = result.nextToken
                } else {
                    newestComments = merged
                    newestNextToken = result.nextToken
                }

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
     * Uses in-memory caching for instant switching without full network refetching or list wiping.
     */
    fun switchSort(sort: CommentSort) {
        if (_selectedSort.value == sort) return
        _selectedSort.value = sort

        val videoId = currentVideoId ?: return

        if (sort == CommentSort.TOP) {
            if (topComments.isNotEmpty()) {
                // Instant switch from memory
                _comments.value = topComments
                nextToken = topNextToken
                _hasNextPage.value = nextToken != null
                return
            }
            val targetToken = topToken ?: YouTube.createCommentsContinuationToken(videoId, 0)
            _isLoading.value = true
            viewModelScope.launch {
                YouTube.commentContinuation(targetToken).onSuccess { result ->
                    val filtered = result.comments.filterNot { it.comment?.commentRenderer?.commentId in deletedCommentIds }
                    topComments = filtered
                    topNextToken = result.nextToken
                    _comments.value = filtered
                    nextToken = result.nextToken
                    _hasNextPage.value = nextToken != null
                    if (result.totalComments != null) _totalComments.value = result.totalComments
                    _isLoading.value = false
                }.onFailure { ex ->
                    Log.e(TAG, "Failed to load top comments", ex)
                    _isLoading.value = false
                    _isError.value = true
                }
            }
        } else {
            // NEWEST
            if (newestComments.isNotEmpty()) {
                // Instant switch from memory
                _comments.value = newestComments
                nextToken = newestNextToken
                _hasNextPage.value = nextToken != null
                return
            }
            val targetToken = newestToken ?: YouTube.createCommentsContinuationToken(videoId, 1)
            _isLoading.value = true
            viewModelScope.launch {
                YouTube.commentContinuation(targetToken).onSuccess { result ->
                    val filtered = result.comments.filterNot { it.comment?.commentRenderer?.commentId in deletedCommentIds }
                    newestComments = filtered
                    newestNextToken = result.nextToken
                    _comments.value = filtered
                    nextToken = result.nextToken
                    _hasNextPage.value = nextToken != null
                    if (result.totalComments != null) _totalComments.value = result.totalComments
                    _isLoading.value = false
                }.onFailure { ex ->
                    Log.e(TAG, "Failed to load newest comments", ex)
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
            _hasNextRepliesPage.value = false
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
            nextRepliesToken = null
            _hasNextRepliesPage.value = false
            return
        }

        _isLoadingReplies.value = true
        _replies.value = emptyList()
        nextRepliesToken = token
        _hasNextRepliesPage.value = true

        viewModelScope.launch {
            YouTube.commentReplies(token).onSuccess { (repliesList, next) ->
                _replies.value = repliesList
                nextRepliesToken = next
                _hasNextRepliesPage.value = next != null
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
                _hasNextRepliesPage.value = next != null
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

        // Optimistically prepend to active comments and in-memory caches
        _comments.value = listOf(optimisticThread) + _comments.value
        if (topComments.isNotEmpty()) {
            topComments = listOf(optimisticThread) + topComments
        }
        if (newestComments.isNotEmpty()) {
            newestComments = listOf(optimisticThread) + newestComments
        }

        viewModelScope.launch {
            YouTube.createComment(videoId, text, createParams)
                .onSuccess {
                    _isSubmitting.value = false
                    onSuccess()
                    Log.i(TAG, "Comment successfully posted and retained in memory")
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

        // 1. Optimistic removal from active list and caches
        _comments.value = _comments.value.filterNot {
            it.comment?.commentRenderer?.commentId == commentId
        }
        topComments = topComments.filterNot {
            it.comment?.commentRenderer?.commentId == commentId
        }
        newestComments = newestComments.filterNot {
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

    /**
     * Start background live synchronization of comments while the comments sheet is open.
     * Polls periodically without triggering full-screen loading spinners or list wiping.
     */
    fun startLiveSync(videoId: String) {
        liveSyncJob?.cancel()
        liveSyncJob = viewModelScope.launch {
            while (isActive) {
                delay(15_000)
                val currentThread = _selectedThread.value
                if (currentThread != null) {
                    // In thread replies view: sync incoming replies
                    val replyToken = currentThread.replies?.commentRepliesRenderer?.contents
                        ?.firstOrNull()?.continuationItemRenderer?.continuationEndpoint
                        ?.continuationCommand?.token
                        ?: currentThread.replies?.commentRepliesRenderer?.viewReplies?.buttonRenderer
                            ?.command?.continuationCommand?.token
                    if (replyToken != null) {
                        YouTube.commentReplies(replyToken).onSuccess { (freshReplies, _) ->
                            val existingReplyIds = _replies.value.mapNotNull { it.commentId }.toSet()
                            val newReplies = freshReplies.filter {
                                val cid = it.commentId
                                cid != null && cid !in existingReplyIds && cid !in deletedCommentIds
                            }
                            if (newReplies.isNotEmpty()) {
                                _replies.value = (_replies.value + newReplies).distinctBy {
                                    it.commentId ?: it.hashCode()
                                }
                            }
                        }
                    }
                } else {
                    // In main comments list: sync incoming top-level comments
                    val targetToken = if (_selectedSort.value == CommentSort.NEWEST) {
                        newestToken ?: YouTube.createCommentsContinuationToken(videoId, 1)
                    } else {
                        topToken ?: YouTube.createCommentsContinuationToken(videoId, 0)
                    }
                    YouTube.commentContinuation(targetToken).onSuccess { result ->
                        val existingIds = _comments.value.mapNotNull { it.comment?.commentRenderer?.commentId }.toSet()
                        val newArrivals = result.comments.filter { thread ->
                            val id = thread.comment?.commentRenderer?.commentId
                            id != null && id !in existingIds && id !in deletedCommentIds
                        }
                        if (newArrivals.isNotEmpty()) {
                            _comments.value = newArrivals + _comments.value
                            if (_selectedSort.value == CommentSort.TOP) {
                                topComments = newArrivals + topComments
                            } else {
                                newestComments = newArrivals + newestComments
                            }
                        }
                        if (result.totalComments != null && result.totalComments != _totalComments.value) {
                            _totalComments.value = result.totalComments
                        }
                    }
                }
            }
        }
    }

    /**
     * Stop background live synchronization immediately when comments sheet closes.
     */
    fun stopLiveSync() {
        liveSyncJob?.cancel()
        liveSyncJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopLiveSync()
    }
}
