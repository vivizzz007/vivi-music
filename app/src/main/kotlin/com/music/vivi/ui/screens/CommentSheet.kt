/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.music.vivi.ui.component.Material3MenuGroup
import com.music.vivi.ui.component.Material3MenuItemData
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.Run
import com.music.innertube.models.Runs
import com.music.innertube.models.Thumbnail
import com.music.innertube.models.Thumbnails
import com.music.innertube.models.comment.CommentRenderer
import com.music.innertube.models.comment.CommentThreadRenderer
import com.music.innertube.utils.parseCookieString
import com.music.vivi.R
import com.music.vivi.constants.AccountChannelHandleKey
import com.music.vivi.constants.AccountNameKey
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.HomeViewModel
import com.music.vivi.viewmodels.YTMusicCommentViewModel
import kotlinx.coroutines.launch

enum class CommentSort {
    TOP,
    NEWEST
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
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
    val isSubmittingComment by commentViewModel.isSubmitting.collectAsState()

    var selectedThread by remember { mutableStateOf<CommentThreadRenderer?>(null) }
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val commentsListState = rememberLazyListState()

    var commentText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    val currentUserName = remember(accountName, accountNamePref) {
        accountName.ifBlank { accountNamePref }
    }

    BackHandler(enabled = navigator.canNavigateBack()) {
        focusManager.clearFocus()
        keyboardController?.hide()
        scope.launch {
            navigator.navigateBack(BackNavigationBehavior.PopUntilScaffoldValueChange)
        }
    }

    LaunchedEffect(videoId, innerTubeCookie) {
        commentViewModel.loadComments(videoId, forceRefresh = true)
    }

    val onSelectSort: (CommentSort) -> Unit = { sort ->
        commentViewModel.switchSort(sort)
        scope.launch {
            commentsListState.scrollToItem(0)
        }
    }

    val onSendComment: () -> Unit = {
        if (commentText.isNotBlank() && !isSubmittingComment) {
            val textToPost = commentText.trim()
            commentText = ""
            focusManager.clearFocus()
            commentViewModel.postComment(
                videoId = videoId,
                text = textToPost,
                authorName = currentUserName,
                authorAvatar = accountImageUrl,
                channelHandle = accountChannelHandle,
                onSuccess = {
                    scope.launch {
                        commentsListState.animateScrollToItem(0)
                    }
                }
            )
        }
    }

    val onDeleteTopLevelComment: (String) -> Unit = { commentId ->
        android.util.Log.d("CommentDelete", "onDeleteTopLevelComment called with commentId=$commentId")
        val wasSelected = selectedThread?.comment?.commentRenderer?.commentId == commentId
        if (wasSelected) {
            selectedThread = null
            scope.launch {
                navigator.navigateBack(BackNavigationBehavior.PopUntilScaffoldValueChange)
            }
        }
        commentViewModel.deleteComment(commentId)
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
            val isDetailView = navigator.canNavigateBack()

            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isDetailView) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                scope.launch {
                                    navigator.navigateBack(BackNavigationBehavior.PopUntilScaffoldValueChange)
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = "Back",
                                modifier = Modifier.size(20.dp)
                            )
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
                            totalComments!!.replace("Comments", "", ignoreCase = true).replace("Comment", "", ignoreCase = true).trim()
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

                IconButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = stringResource(R.string.close),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (!isDetailView) {
                Spacer(modifier = Modifier.height(4.dp))

                // Sort Filter Boxes (Top / Newest)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { onSelectSort(CommentSort.TOP) },
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
                        onClick = { onSelectSort(CommentSort.NEWEST) },
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

            HorizontalDivider()

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading && comments.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularWavyProgressIndicator()
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
                    else -> {
                        ListDetailPaneScaffold(
                            directive = navigator.scaffoldDirective,
                            scaffoldState = navigator.scaffoldState,
                            modifier = Modifier.fillMaxHeight(),
                            listPane = {
                                AnimatedPane {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        if (comments.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.person),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(48.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text(
                                                        text = stringResource(R.string.no_comments),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        } else {
                                             LazyColumn(
                                                 state = commentsListState,
                                                 modifier = Modifier
                                                     .fillMaxWidth()
                                                     .weight(1f),
                                                 contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
                                                 verticalArrangement = Arrangement.spacedBy(20.dp)
                                             ) {
                                                items(
                                                    comments,
                                                    key = { item -> item.comment?.commentRenderer?.commentId ?: "comment-${item.hashCode()}" }
                                                ) { thread ->
                                                    val renderer = thread.comment?.commentRenderer ?: return@items
                                                    val hasReplies = (renderer.replyCount ?: 0) > 0
                                                    val isOwn = checkIsOwnComment(
                                                        renderer = renderer,
                                                        userName = currentUserName,
                                                        userChannelHandle = accountChannelHandle,
                                                        isLoggedIn = isLoggedIn
                                                    )
                                                    CommentItem(
                                                        renderer = renderer,
                                                        onShowReplies = {
                                                            selectedThread = thread
                                                            scope.launch {
                                                                navigator.navigateTo(
                                                                    ListDetailPaneScaffoldRole.Detail,
                                                                    renderer.commentId ?: thread.hashCode().toString()
                                                                )
                                                            }
                                                        },
                                                        onDelete = if (isOwn) {
                                                            { onDeleteTopLevelComment(renderer.commentId ?: "") }
                                                        } else null,
                                                        isOwnComment = isOwn
                                                    )
                                                }

                                                if (!isLoading && comments.isNotEmpty()) {
                                                    item(key = "pagination_loader") {
                                                        LaunchedEffect(comments.size) {
                                                            commentViewModel.loadMoreComments()
                                                        }
                                                        if (isLoadingMore) {
                                                            Box(
                                                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                CircularWavyProgressIndicator(modifier = Modifier.size(32.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Top-level comment input bar inside listPane
                                        if (isLoggedIn) {
                                            CommentInputBar(
                                                avatarUrl = accountImageUrl,
                                                placeholderText = stringResource(R.string.add_a_comment),
                                                text = commentText,
                                                onTextChange = { commentText = it },
                                                onSend = onSendComment,
                                                isSubmitting = isSubmittingComment
                                            )
                                        } else {
                                            LoggedOutCommentBanner(
                                                onSignInClick = {
                                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                                        if (!sheetState.isVisible) onDismiss()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            detailPane = {
                                AnimatedPane {
                                    val selectedCommentId = navigator.currentDestination?.contentKey
                                    val threadToDisplay = selectedThread?.takeIf {
                                        (it.comment?.commentRenderer?.commentId ?: it.hashCode().toString()) == selectedCommentId
                                    } ?: comments.firstOrNull {
                                        (it.comment?.commentRenderer?.commentId ?: it.hashCode().toString()) == selectedCommentId
                                    }
                                    if (threadToDisplay != null) {
                                        CommentDetailPane(
                                            thread = threadToDisplay,
                                            videoId = videoId,
                                            onBack = {
                                                scope.launch {
                                                    navigator.navigateBack(BackNavigationBehavior.PopUntilScaffoldValueChange)
                                                }
                                            },
                                            userName = currentUserName,
                                            userChannelHandle = accountChannelHandle,
                                            avatarUrl = accountImageUrl,
                                            isLoggedIn = isLoggedIn,
                                            onDeleteMainComment = {
                                                threadToDisplay.comment?.commentRenderer?.commentId?.let { id ->
                                                    onDeleteTopLevelComment(id)
                                                }
                                            },
                                            commentViewModel = commentViewModel
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun checkIsOwnComment(
    renderer: CommentRenderer,
    userName: String,
    userChannelHandle: String,
    isLoggedIn: Boolean
): Boolean {
    if (!isLoggedIn) return false

    // 1. Optimistic/local post before server round-trip
    if (renderer.commentId?.startsWith("local-") == true || renderer.commentId?.startsWith("temp-") == true) return true

    // 2. Author match against logged-in user's name or handle
    val author = renderer.authorText?.runs?.firstOrNull()?.text?.trim().orEmpty()
    val cleanUserHandle = userChannelHandle.trim().removePrefix("@")
    val cleanAuthor = author.removePrefix("@")
    val matchesUser = author.isNotBlank() && (
        (userName.isNotBlank() && author.equals(userName.trim(), ignoreCase = true)) ||
        (cleanUserHandle.isNotBlank() && cleanAuthor.equals(cleanUserHandle, ignoreCase = true))
    )
    if (matchesUser) return true

    // 3. YouTube API explicitly flags current user's comment
    if (renderer.isAuthor) return true

    // 4. Genuine delete action token only if author is not explicitly someone else
    if (author.isBlank() && renderer.getDeleteActionToken() != null) return true

    return false
}

@Composable
fun CommentInputBar(
    avatarUrl: String?,
    placeholderText: String,
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val localFocusRequester = remember { FocusRequester() }
    val effectiveFocusRequester = focusRequester ?: localFocusRequester

    Surface(
        modifier = modifier
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
                .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
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
                        try { effectiveFocusRequester.requestFocus() } catch (_: Exception) {}
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = placeholderText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(effectiveFocusRequester)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            if (isSubmitting) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank(),
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                ) {
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

@Composable
fun LoggedOutCommentBanner(
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
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
                onClick = onSignInClick,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentItem(
    renderer: CommentRenderer,
    onShowReplies: (() -> Unit)? = null,
    onReply: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    isReply: Boolean = false,
    isOwnComment: Boolean = false
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    if (showDeleteDialog) {
        DefaultDialog(
            onDismiss = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_comment)) },
            buttons = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        android.util.Log.d("CommentDelete", "Delete confirmed in dialog for commentId=${renderer.commentId}")
                        showDeleteDialog = false
                        onDelete?.invoke()
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isReply) 4.dp else 0.dp)
    ) {
        val authorThumbnailUrl = renderer.authorThumbnail?.thumbnails?.lastOrNull()?.url
        if (!authorThumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = authorThumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(if (isReply) 28.dp else 36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(if (isReply) 28.dp else 36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(if (isReply) 16.dp else 20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onShowReplies != null) Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onShowReplies() } else Modifier
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = renderer.authorText?.runs?.firstOrNull()?.text ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = renderer.publishedTimeText?.runs?.firstOrNull()?.text ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = renderer.contentText?.runs?.joinToString("") { it.text } ?: "",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val voteCount = renderer.voteCount
                if (voteCount != null) {
                    Icon(
                        painter = painterResource(R.drawable.thumb_up_like),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = voteCount.runs?.firstOrNull()?.text ?: "0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val replyCount = renderer.replyCount ?: 0
                if (onShowReplies != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onShowReplies() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.chat_msg),
                            contentDescription = stringResource(R.string.reply),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (replyCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = replyCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (onReply != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onReply() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.chat_msg),
                            contentDescription = stringResource(R.string.reply),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.reply),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // YouTube-style 3-dots "More" button (at the far right)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = stringResource(R.string.more_options),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (showMenu) {
                    val actionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ModalBottomSheet(
                        onDismissRequest = { showMenu = false },
                        sheetState = actionSheetState,
                        dragHandle = { BottomSheetDefaults.DragHandle() },
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 32.dp)
                        ) {
                            val commentText = renderer.contentText?.runs?.joinToString("") { it.text }.orEmpty()
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
                                                showMenu = false
                                                clipboardManager.setText(AnnotatedString(commentText))
                                            }
                                        )
                                    } else null,
                                    if (isOwnComment && onDelete != null) {
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
                                                android.util.Log.d("CommentDelete", "Delete action sheet item tapped for commentId=${renderer.commentId}")
                                                showMenu = false
                                                showDeleteDialog = true
                                            }
                                        )
                                    } else null
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CommentDetailPane(
    thread: CommentThreadRenderer,
    videoId: String = "",
    onBack: () -> Unit = {},
    userName: String = "",
    userChannelHandle: String = "",
    avatarUrl: String? = null,
    isLoggedIn: Boolean = false,
    onDeleteMainComment: (() -> Unit)? = null,
    commentViewModel: YTMusicCommentViewModel = hiltViewModel()
) {
    val mainComment = thread.comment?.commentRenderer ?: return
    val scope = rememberCoroutineScope()
    val replies by commentViewModel.replies.collectAsState()
    val isLoadingReplies by commentViewModel.isLoadingReplies.collectAsState()
    val isSubmittingReply by commentViewModel.isSubmitting.collectAsState()

    var replyText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val repliesListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    val replyParams = remember(mainComment, videoId) {
        mainComment.actionButtons?.commentActionButtonsRenderer?.replyButton
            ?.buttonRenderer?.serviceEndpoint?.createCommentReplyEndpoint?.createReplyParams
            ?: mainComment.actionButtons?.commentActionButtonsRenderer?.replyButton
                ?.buttonRenderer?.command?.createCommentReplyEndpoint?.createReplyParams
            ?: mainComment.commentId?.takeIf { videoId.isNotBlank() }?.let { cid ->
                YouTube.buildReplyParams(videoId, cid)
            }
    }

    LaunchedEffect(thread) {
        commentViewModel.loadReplies(thread)
    }

    val onSendReply: () -> Unit = {
        val cid = mainComment.commentId.orEmpty()
        val effectiveParams = replyParams ?: if (videoId.isNotBlank() && cid.isNotBlank()) {
            YouTube.buildReplyParams(videoId, cid)
        } else null

        if (replyText.isNotBlank() && !isSubmittingReply && (effectiveParams != null || (videoId.isNotBlank() && cid.isNotBlank()))) {
            val textToPost = replyText.trim()
            replyText = ""
            focusManager.clearFocus()
            commentViewModel.postReply(
                text = textToPost,
                createReplyParams = effectiveParams,
                videoId = videoId,
                commentId = cid,
                authorName = userName,
                authorAvatar = avatarUrl,
                channelHandle = userChannelHandle,
                onSuccess = {
                    scope.launch {
                        repliesListState.animateScrollToItem(replies.size)
                    }
                }
            )
        }
    }

    val onDeleteReply: (String) -> Unit = { replyId ->
        android.util.Log.d("CommentDelete", "onDeleteReply called with replyId=$replyId")
        commentViewModel.deleteReply(replyId)
    }

    val isMainCommentOwn = checkIsOwnComment(
        renderer = mainComment,
        userName = userName,
        userChannelHandle = userChannelHandle,
        isLoggedIn = isLoggedIn
    )

    val onReplyToAuthor: (String) -> Unit = { author ->
        val trimmed = author.trim()
        val handle = if (trimmed.startsWith("@")) trimmed else "@$trimmed"
        if (trimmed.isNotBlank()) {
            if (!replyText.contains(handle)) {
                replyText = if (replyText.isBlank()) "$handle " else "$replyText $handle "
            }
        }
        scope.launch {
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {}
        }
    }

    Column(modifier = Modifier.fillMaxHeight()) {
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = repliesListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    CommentItem(
                        renderer = mainComment,
                        onShowReplies = null,
                        onReply = {
                            val author = mainComment.authorText?.runs?.firstOrNull()?.text ?: ""
                            onReplyToAuthor(author)
                        },
                        onDelete = if (isMainCommentOwn) onDeleteMainComment else null,
                        isOwnComment = isMainCommentOwn
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(
                    replies,
                    key = { item -> item.commentId ?: "reply-${item.hashCode()}" }
                ) { replyRenderer ->
                    val isOwnReply = checkIsOwnComment(
                        renderer = replyRenderer,
                        userName = userName,
                        userChannelHandle = userChannelHandle,
                        isLoggedIn = isLoggedIn
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.width(36.dp))
                        CommentItem(
                            renderer = replyRenderer,
                            isReply = true,
                            onReply = {
                                val author = replyRenderer.authorText?.runs?.firstOrNull()?.text ?: ""
                                onReplyToAuthor(author)
                            },
                            onDelete = if (isOwnReply) {
                                { onDeleteReply(replyRenderer.commentId ?: "") }
                            } else null,
                            isOwnComment = isOwnReply
                        )
                    }
                }

                item(key = "replies_pagination") {
                    LaunchedEffect(replies.size) {
                        commentViewModel.loadMoreReplies()
                    }
                    if (isLoadingReplies) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }

        // Reply Input Bar
        if (isLoggedIn) {
            val authorName = mainComment.authorText?.runs?.firstOrNull()?.text ?: ""
            CommentInputBar(
                avatarUrl = avatarUrl,
                placeholderText = if (authorName.isNotBlank()) {
                    stringResource(R.string.reply_to_comment, authorName)
                } else {
                    stringResource(R.string.add_a_comment)
                },
                text = replyText,
                onTextChange = { replyText = it },
                onSend = onSendReply,
                isSubmitting = isSubmittingReply,
                focusRequester = focusRequester
            )
        }
    }
}
