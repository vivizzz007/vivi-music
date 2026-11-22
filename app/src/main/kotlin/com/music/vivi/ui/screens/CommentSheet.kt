/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.comment.CommentRenderer
import com.music.innertube.models.comment.CommentThreadRenderer
import com.music.innertube.utils.parseCookieString
import com.music.vivi.R
import com.music.vivi.constants.AccountChannelHandleKey
import com.music.vivi.constants.AccountNameKey
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.Material3MenuGroup
import com.music.vivi.ui.component.Material3MenuItemData
import com.music.vivi.utils.listItemShape
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.HomeViewModel
import com.music.vivi.viewmodels.YTMusicCommentViewModel
import kotlinx.coroutines.launch

enum class CommentSort {
    TOP,
    NEWEST
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CommentSheet(
    videoId: String,
    onDismiss: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
    commentViewModel: YTMusicCommentViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val (innerTubeCookie, _) = rememberPreference(InnerTubeCookieKey, "")
    val (accountChannelHandle, _) = rememberPreference(AccountChannelHandleKey, "")
    val (accountNamePref, _) = rememberPreference(AccountNameKey, "")
    val accountName by homeViewModel.accountName.collectAsState()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()

    val isLoggedIn = remember(innerTubeCookie) {
        innerTubeCookie.isNotEmpty() && "SAPISID" in parseCookieString(innerTubeCookie)
    }

    val comments by commentViewModel.comments.collectAsState()
    val isLoading by commentViewModel.isLoading.collectAsState()
    val isLoadingMore by commentViewModel.isLoadingMore.collectAsState()
    val isError by commentViewModel.isError.collectAsState()
    val totalComments by commentViewModel.totalComments.collectAsState()
    val selectedSort by commentViewModel.selectedSort.collectAsState()
    val isSubmitting by commentViewModel.isSubmitting.collectAsState()

    val replies by commentViewModel.replies.collectAsState()
    val isLoadingReplies by commentViewModel.isLoadingReplies.collectAsState()
    val hasNextPage by commentViewModel.hasNextPage.collectAsState()
    val hasNextRepliesPage by commentViewModel.hasNextRepliesPage.collectAsState()
    val likedCommentIds by commentViewModel.likedCommentIds.collectAsState()
    val context = LocalContext.current
    val guestLikePrompt = stringResource(R.string.sign_in_to_like)

    var selectedThread by remember { mutableStateOf<CommentThreadRenderer?>(null) }
    var textInput by remember { mutableStateOf("") }
    var replyingToAuthor by remember { mutableStateOf<String?>(null) }

    var menuComment by remember { mutableStateOf<CommentRenderer?>(null) }
    var deleteCommentId by remember { mutableStateOf<String?>(null) }

    val commentsListState = rememberLazyListState()
    val repliesListState = rememberLazyListState()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val clipboardManager = LocalClipboardManager.current
    val isDark = isSystemInDarkTheme()
    val cookie12 = MaterialShapes.Cookie12Sided.toShape()

    val currentUserName = remember(accountName, accountNamePref) {
        accountName.ifBlank { accountNamePref }
    }

    LaunchedEffect(Unit) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    LaunchedEffect(videoId, innerTubeCookie) {
        commentViewModel.loadComments(videoId, forceRefresh = false)
    }

    DisposableEffect(videoId) {
        commentViewModel.startLiveSync(videoId)
        onDispose {
            commentViewModel.stopLiveSync()
        }
    }

    LaunchedEffect(selectedThread) {
        commentViewModel.selectThread(selectedThread)
    }

    BackHandler(enabled = selectedThread != null) {
        focusManager.clearFocus()
        keyboardController?.hide()
        selectedThread = null
        textInput = ""
        replyingToAuthor = null
    }

    val onSend: () -> Unit = {
        val currentThread = selectedThread
        if (currentThread == null) {
            // Send top-level comment
            if (textInput.isNotBlank() && !isSubmitting) {
                val textToPost = textInput.trim()
                textInput = ""
                focusManager.clearFocus()
                commentViewModel.postComment(
                    videoId = videoId,
                    text = textToPost,
                    authorName = currentUserName,
                    authorAvatar = accountImageUrl,
                    channelHandle = accountChannelHandle,
                    onSuccess = {
                        scope.launch { commentsListState.animateScrollToItem(0) }
                    }
                )
            }
        } else {
            // Send reply to thread
            val mainComment = currentThread.comment?.commentRenderer
            val cid = mainComment?.commentId.orEmpty()
            val replyParams = mainComment?.actionButtons?.commentActionButtonsRenderer?.replyButton
                ?.buttonRenderer?.serviceEndpoint?.createCommentReplyEndpoint?.createReplyParams
                ?: mainComment?.actionButtons?.commentActionButtonsRenderer?.replyButton
                    ?.buttonRenderer?.command?.createCommentReplyEndpoint?.createReplyParams
                ?: if (videoId.isNotBlank() && cid.isNotBlank()) YouTube.buildReplyParams(videoId, cid) else null

            if (textInput.isNotBlank() && !isSubmitting && replyParams != null) {
                val textToPost = textInput.trim()
                textInput = ""
                replyingToAuthor = null
                focusManager.clearFocus()
                commentViewModel.postReply(
                    text = textToPost,
                    createReplyParams = replyParams,
                    videoId = videoId,
                    commentId = cid,
                    authorName = currentUserName,
                    authorAvatar = accountImageUrl,
                    channelHandle = accountChannelHandle,
                    onSuccess = {
                        scope.launch { repliesListState.animateScrollToItem(replies.size) }
                    }
                )
            }
        }
    }

    val onDeleteComment: (String) -> Unit = { id ->
        if (selectedThread?.comment?.commentRenderer?.commentId == id) {
            selectedThread = null
        }
        commentViewModel.deleteComment(id)
        commentViewModel.deleteReply(id)
    }

    val onReplyToAuthor: (String) -> Unit = { author ->
        val clean = author.trim().removePrefix("@")
        replyingToAuthor = clean
        val handle = "@$clean"
        if (!textInput.contains(handle)) {
            textInput = if (textInput.isBlank()) "$handle " else "$textInput $handle "
        }
        scope.launch {
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {}
        }
    }

    val onLikeClick: (CommentRenderer) -> Unit = { renderer ->
        val cid = renderer.commentId.orEmpty()
        if (cid.isNotBlank()) {
            if (isLoggedIn) {
                val initialLiked = renderer.voteStatus == "UPVOTE"
                commentViewModel.toggleLikeComment(cid, initialLiked)
            } else {
                Toast.makeText(context, guestLikePrompt, Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .navigationBarsPadding()
        ) {
            val isDetail = selectedThread != null

            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isDetail) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                selectedThread = null
                                textInput = ""
                                replyingToAuthor = null
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.arrow_back),
                                    contentDescription = "Back",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.replies),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.comments_header),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val countDisplay = if (!totalComments.isNullOrBlank()) {
                            totalComments!!.replace("Comments", "", ignoreCase = true)
                                .replace("Comment", "", ignoreCase = true).trim()
                        } else if (comments.isNotEmpty()) {
                            "${comments.size}"
                        } else null

                        if (countDisplay != null) {
                            Text(
                                text = countDisplay,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.close),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Sort Filter Boxes (Top / Newest)
            if (!isDetail) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = {
                            commentViewModel.switchSort(CommentSort.TOP)
                            scope.launch { commentsListState.scrollToItem(0) }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedSort == CommentSort.TOP) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        contentColor = if (selectedSort == CommentSort.TOP) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.top_comments),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedSort == CommentSort.TOP) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        onClick = {
                            commentViewModel.switchSort(CommentSort.NEWEST)
                            scope.launch { commentsListState.scrollToItem(0) }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedSort == CommentSort.NEWEST) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        contentColor = if (selectedSort == CommentSort.NEWEST) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.newest_comments),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedSort == CommentSort.NEWEST) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // Body Area with animated transition between List & Thread
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = selectedThread,
                    transitionSpec = {
                        if (targetState != null) {
                            (slideInHorizontally { it / 3 } + fadeIn()).togetherWith(
                                slideOutHorizontally { -it / 3 } + fadeOut())
                        } else {
                            (slideInHorizontally { -it / 3 } + fadeIn()).togetherWith(
                                slideOutHorizontally { it / 3 } + fadeOut())
                        }
                    },
                    label = "CommentViewTransition"
                ) { thread ->
                    if (thread == null) {
                        // Main Comments List View
                        when {
                            isLoading && comments.isEmpty() -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LoadingIndicator(
                                        modifier = Modifier.size(48.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            isError -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            painter = painterResource(R.drawable.error),
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(text = stringResource(R.string.no_comments))
                                    }
                                }
                            }
                            comments.isEmpty() -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = cookie12,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(76.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    painter = painterResource(R.drawable.person),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(36.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            text = stringResource(R.string.no_comments),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            else -> {
                                LazyColumn(
                                    state = commentsListState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    itemsIndexed(
                                        comments,
                                        key = { _, item -> item.comment?.commentRenderer?.commentId ?: "comment-${item.hashCode()}" }
                                    ) { index, itemThread ->
                                        val renderer = itemThread.comment?.commentRenderer ?: return@itemsIndexed
                                        val isOwn = commentViewModel.checkIsOwnComment(renderer, currentUserName, accountChannelHandle, isLoggedIn)
                                        val shape = listItemShape(index, comments.size, radius = 20.dp)
                                        val liveReplyCount = commentViewModel.getLiveReplyCount(itemThread)
                                        val cid = renderer.commentId.orEmpty()
                                        val isLiked = likedCommentIds[cid] ?: (renderer.voteStatus == "UPVOTE")

                                        CommentCard(
                                            renderer = renderer,
                                            shape = shape,
                                            isOwn = isOwn,
                                            isDark = isDark,
                                            replyCount = liveReplyCount,
                                            isLoggedIn = isLoggedIn,
                                            isLiked = isLiked,
                                            onLikeClick = { onLikeClick(renderer) },
                                            onCardClick = {
                                                selectedThread = itemThread
                                                textInput = ""
                                                replyingToAuthor = null
                                            },
                                            onReplyClick = if (isLoggedIn) {
                                                {
                                                    selectedThread = itemThread
                                                    textInput = ""
                                                    val author = renderer.authorText?.runs?.firstOrNull()?.text ?: ""
                                                    onReplyToAuthor(author)
                                                }
                                            } else null,
                                            onMoreClick = { menuComment = renderer }
                                        )
                                    }

                                    if (!isLoading && comments.isNotEmpty() && hasNextPage) {
                                        item(key = "pagination_loader") {
                                            LaunchedEffect(comments.size) {
                                                commentViewModel.loadMoreComments()
                                            }
                                            if (isLoadingMore) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 32.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    LoadingIndicator(
                                                        modifier = Modifier.size(36.dp),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Replies Detail View
                        val mainComment = thread.comment?.commentRenderer
                        if (mainComment != null) {
                            val isMainOwn = commentViewModel.checkIsOwnComment(mainComment, currentUserName, accountChannelHandle, isLoggedIn)
                            val parentAuthor = mainComment.authorText?.runs?.firstOrNull()?.text ?: ""

                            LazyColumn(
                                state = repliesListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                val mainCid = mainComment.commentId.orEmpty()
                                val isMainLiked = likedCommentIds[mainCid] ?: (mainComment.voteStatus == "UPVOTE")

                                // Hero parent comment
                                item {
                                    CommentCard(
                                        renderer = mainComment,
                                        shape = RoundedCornerShape(24.dp),
                                        isOwn = isMainOwn,
                                        isHero = true,
                                        isDark = isDark,
                                        replyCount = 0,
                                        isLoggedIn = isLoggedIn,
                                        isLiked = isMainLiked,
                                        onLikeClick = { onLikeClick(mainComment) },
                                        onReplyClick = if (isLoggedIn) {
                                            { onReplyToAuthor(parentAuthor) }
                                        } else null,
                                        onMoreClick = { menuComment = mainComment }
                                    )
                                }

                                // Replies Section Header
                                item {
                                    val liveTotalReplies = maxOf(commentViewModel.getLiveReplyCount(thread), replies.size)
                                    Text(
                                        text = if (liveTotalReplies > 0) {
                                            "$liveTotalReplies " + (if (liveTotalReplies == 1) stringResource(R.string.reply) else stringResource(R.string.replies))
                                        } else {
                                            stringResource(R.string.replies)
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                                    )
                                }

                                // Replies Items
                                itemsIndexed(
                                    replies,
                                    key = { _, item -> item.commentId ?: "reply-${item.hashCode()}" }
                                ) { index, replyRenderer ->
                                    val isReplyOwn = commentViewModel.checkIsOwnComment(replyRenderer, currentUserName, accountChannelHandle, isLoggedIn)
                                    val replyShape = listItemShape(index, replies.size, radius = 20.dp)
                                    val replyAuthor = replyRenderer.authorText?.runs?.firstOrNull()?.text ?: ""
                                    val replyCid = replyRenderer.commentId.orEmpty()
                                    val isReplyLiked = likedCommentIds[replyCid] ?: (replyRenderer.voteStatus == "UPVOTE")

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp)
                                    ) {
                                        CommentCard(
                                            renderer = replyRenderer,
                                            shape = replyShape,
                                            isOwn = isReplyOwn,
                                            isReply = true,
                                            isDark = isDark,
                                            replyCount = 0,
                                            isLoggedIn = isLoggedIn,
                                            isLiked = isReplyLiked,
                                            onLikeClick = { onLikeClick(replyRenderer) },
                                            onReplyClick = if (isLoggedIn) {
                                                { onReplyToAuthor(replyAuthor) }
                                            } else null,
                                            onMoreClick = { menuComment = replyRenderer }
                                        )
                                    }
                                }

                                if (replies.isEmpty() && !isLoadingReplies) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.chat_msg),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = stringResource(R.string.no_comments),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                if (hasNextRepliesPage) {
                                    item(key = "replies_pagination") {
                                        LaunchedEffect(replies.size) {
                                            commentViewModel.loadMoreReplies()
                                        }
                                        if (isLoadingReplies) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                LoadingIndicator(
                                                    modifier = Modifier.size(32.dp),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Area: Replying Chip + Input Bar / Logged Out Banner
            if (isDetail && replyingToAuthor != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.chat_msg),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.replying_to, replyingToAuthor!!),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { replyingToAuthor = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = stringResource(R.string.close),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            if (isLoggedIn) {
                val placeholder = if (isDetail) {
                    val target = replyingToAuthor ?: selectedThread?.comment?.commentRenderer?.authorText?.runs?.firstOrNull()?.text
                    if (!target.isNullOrBlank()) stringResource(R.string.reply_to_comment, target)
                    else stringResource(R.string.add_a_comment)
                } else {
                    stringResource(R.string.add_a_comment)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!accountImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = accountImageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(cookie12),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(cookie12)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.person),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    try { focusRequester.requestFocus() } catch (_: Exception) {}
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (textInput.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                maxLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        if (isSubmitting) {
                            Box(
                                modifier = Modifier.size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Surface(
                                onClick = onSend,
                                enabled = textInput.isNotBlank(),
                                shape = CircleShape,
                                color = if (textInput.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (textInput.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.send_chat),
                                        contentDescription = stringResource(R.string.send),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Logged-out Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.person),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.sign_in_to_comment),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    if (!sheetState.isVisible) onDismiss()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.login),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // Single Centralized Action Bottom Sheet for More Options
    menuComment?.let { comment ->
        val actionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val isOwn = commentViewModel.checkIsOwnComment(comment, currentUserName, accountChannelHandle, isLoggedIn)
        val commentText = comment.contentText?.runs?.joinToString("") { it.text }.orEmpty()

        ModalBottomSheet(
            onDismissRequest = { menuComment = null },
            sheetState = actionSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Material3MenuGroup(
                    expressive = true,
                    items = listOfNotNull(
                        if (commentText.isNotBlank()) {
                            Material3MenuItemData(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.content_copy),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                title = { Text(stringResource(R.string.copy)) },
                                onClick = {
                                    menuComment = null
                                    clipboardManager.setText(AnnotatedString(commentText))
                                }
                            )
                        } else null,
                        if (isOwn) {
                            Material3MenuItemData(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.delete),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                title = {
                                    Text(
                                        stringResource(R.string.delete),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    val cid = comment.commentId
                                    menuComment = null
                                    deleteCommentId = cid
                                }
                            )
                        } else null
                    )
                )
            }
        }
    }

    // Single Centralized Delete Confirmation Dialog
    deleteCommentId?.let { cid ->
        DefaultDialog(
            onDismiss = { deleteCommentId = null },
            title = { Text(stringResource(R.string.delete_comment)) },
            buttons = {
                TextButton(onClick = { deleteCommentId = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        deleteCommentId = null
                        onDeleteComment(cid)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        ) {
            Text(
                text = stringResource(R.string.delete_comment_confirm),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Lightweight, stateless comment card row
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CommentCard(
    renderer: CommentRenderer,
    shape: Shape,
    isOwn: Boolean,
    isDark: Boolean,
    isHero: Boolean = false,
    isReply: Boolean = false,
    replyCount: Int = 0,
    isLoggedIn: Boolean = false,
    isLiked: Boolean = false,
    onLikeClick: () -> Unit,
    onCardClick: (() -> Unit)? = null,
    onReplyClick: (() -> Unit)? = null,
    onMoreClick: () -> Unit
) {
    Surface(
        shape = shape,
        color = if (isOwn) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onCardClick != null) {
                    Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onCardClick() }
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isHero) 16.dp else if (isReply) 12.dp else 14.dp)
        ) {
            val authorThumbnailUrl = renderer.authorThumbnail?.thumbnails?.lastOrNull()?.url
            val avatarSize = if (isHero) 42.dp else if (isReply) 28.dp else 36.dp
            val avatarIconSize = if (isHero) 22.dp else if (isReply) 16.dp else 20.dp
            val authorName = renderer.authorText?.runs?.firstOrNull()?.text.orEmpty()
            val cookie12 = MaterialShapes.Cookie12Sided.toShape()

            if (!authorThumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = authorThumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(cookie12),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(cookie12)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(avatarIconSize)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = authorName,
                        style = if (isHero) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isOwn) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = stringResource(R.string.you),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = renderer.publishedTimeText?.runs?.firstOrNull()?.text ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                val rawText = renderer.contentText?.runs?.joinToString("") { it.text } ?: ""
                val primaryColor = MaterialTheme.colorScheme.primary
                val styledText = remember(rawText, primaryColor) {
                    buildAnnotatedString {
                        val words = rawText.split(" ")
                        words.forEachIndexed { i, word ->
                            if (word.startsWith("@") && word.length > 1) {
                                withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.SemiBold)) {
                                    append(word)
                                }
                            } else {
                                append(word)
                            }
                            if (i < words.size - 1) append(" ")
                        }
                    }
                }

                Text(
                    text = styledText,
                    style = if (isHero) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                    lineHeight = if (isHero) 22.sp else 20.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Expressive Action Pills Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Like pill (Material 3 Expressive)
                    val rawVoteCount = renderer.voteCount?.runs?.firstOrNull()?.text
                    val initiallyLiked = renderer.voteStatus == "UPVOTE"
                    val displayVoteCount = remember(rawVoteCount, isLiked, initiallyLiked) {
                        if (isLiked == initiallyLiked) {
                            rawVoteCount ?: ""
                        } else if (isLiked && !initiallyLiked) {
                            if (rawVoteCount.isNullOrBlank() || rawVoteCount == "0") {
                                "1"
                            } else {
                                val num = rawVoteCount.toIntOrNull()
                                if (num != null) "${num + 1}" else rawVoteCount
                            }
                        } else {
                            val num = rawVoteCount?.toIntOrNull()
                            if (num != null && num > 1) {
                                "${num - 1}"
                            } else if (num == 1) {
                                ""
                            } else {
                                rawVoteCount ?: ""
                            }
                        }
                    }

                    Surface(
                        onClick = onLikeClick,
                        shape = RoundedCornerShape(16.dp),
                        color = if (isLiked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                        contentColor = if (isLiked) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.thumb_up_like),
                                contentDescription = stringResource(R.string.like),
                                modifier = Modifier.size(15.dp)
                            )
                            if (displayVoteCount.isNotBlank() && displayVoteCount != "0") {
                                Text(
                                    text = displayVoteCount,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Reply / Chat pill
                    if (replyCount > 0) {
                        Surface(
                            onClick = { onCardClick?.invoke() ?: onReplyClick?.invoke() },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.chat_msg),
                                    contentDescription = stringResource(R.string.replies),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = replyCount.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else if (isLoggedIn && onReplyClick != null) {
                        Surface(
                            onClick = onReplyClick,
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.chat_msg),
                                    contentDescription = stringResource(R.string.reply),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = stringResource(R.string.reply),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // More button (3 dots)
                    Surface(
                        onClick = onMoreClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = stringResource(R.string.more_options),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
