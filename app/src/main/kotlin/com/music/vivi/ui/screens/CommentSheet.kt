package com.music.vivi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.comment.CommentRenderer
import com.music.innertube.models.comment.CommentThreadRenderer
import com.music.vivi.R
import kotlinx.coroutines.launch

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun CommentSheet(
    videoId: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var comments by remember { mutableStateOf<List<CommentThreadRenderer>>(emptyList()) }
    var nextToken by remember { mutableStateOf<String?>(null) }
    var totalComments by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    val navigator = rememberListDetailPaneScaffoldNavigator<CommentThreadRenderer>()

    LaunchedEffect(videoId) {
        YouTube.comments(videoId).onSuccess { (initialComments, token) ->
            comments = initialComments
            nextToken = token
            isLoading = false
        }.onFailure {
            it.printStackTrace()
            isLoading = false
            isError = true
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.comments),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (totalComments != null) {
                        Text(
                            text = "$totalComments",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) onDismiss()
                    }
                }) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }

            HorizontalDivider()

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
                (isError || comments.isEmpty()) && !isLoading -> {
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
                                LazyColumn(
                                    modifier = Modifier.fillMaxHeight(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    items(
                                        comments,
                                        key = { item -> item.comment?.commentRenderer?.commentId ?: "comment-${item.hashCode()}" }
                                    ) { thread ->
                                        val renderer = thread.comment?.commentRenderer ?: return@items
                                        CommentItem(
                                            renderer = renderer,
                                            onShowReplies = {
                                                scope.launch {
                                                    navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, thread)
                                                }
                                            }
                                        )
                                    }

                                    if (nextToken != null) {
                                        item(key = "pagination_loader") {
                                            LaunchedEffect(nextToken) {
                                                YouTube.commentContinuation(nextToken!!).onSuccess { (newComments, token) ->
                                                    comments = comments + newComments
                                                    nextToken = token
                                                }
                                            }
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
                        },
                        detailPane = {
                            AnimatedPane {
                                val selectedThread = navigator.currentDestination?.contentKey
                                if (selectedThread != null) {
                                    CommentDetailPane(
                                        thread = selectedThread,
                                        onBack = {
                                            scope.launch {
                                                navigator.navigateBack(BackNavigationBehavior.PopUntilScaffoldValueChange)
                                            }
                                        }
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

@Composable
fun CommentItem(
    renderer: CommentRenderer,
    onShowReplies: (() -> Unit)? = null,
    isReply: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onShowReplies != null) Modifier.clickable { onShowReplies() } else Modifier)
            .padding(vertical = if (isReply) 4.dp else 0.dp)
    ) {
        AsyncImage(
            model = renderer.authorThumbnail?.thumbnails?.lastOrNull()?.url,
            contentDescription = null,
            modifier = Modifier
                .size(if (isReply) 28.dp else 36.dp)
                .clip(MaterialTheme.shapes.extraLarge),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = renderer.authorText?.runs?.firstOrNull()?.text ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                if (replyCount > 0 && onShowReplies != null) {
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
                            contentDescription = "Replies",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = replyCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
    onBack: () -> Unit
) {
    val mainComment = thread.comment?.commentRenderer ?: return
    var replies by remember { mutableStateOf<List<CommentRenderer>>(emptyList()) }
    var repliesNextToken by remember { mutableStateOf<String?>(null) }
    var isLoadingReplies by remember { mutableStateOf(false) }

    LaunchedEffect(thread) {
        val initialToken = thread.replies?.commentRepliesRenderer
            ?.contents?.firstOrNull()?.continuationItemRenderer
            ?.continuationEndpoint?.continuationCommand?.token
            ?: thread.replies?.commentRepliesRenderer?.viewReplies?.buttonRenderer?.command?.continuationCommand?.token
            ?: thread.replies?.commentRepliesRenderer?.viewReplies?.buttonRenderer?.navigationEndpoint?.continuationCommand?.token
        

        if (initialToken != null) {
            isLoadingReplies = true
            YouTube.commentReplies(initialToken).onSuccess { (newReplies, token) ->
                replies = replies + newReplies
                repliesNextToken = token
                isLoadingReplies = false
            }.onFailure {
                it.printStackTrace()
                isLoadingReplies = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxHeight()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "${mainComment.replyCount ?: 0} Replies",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxHeight(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CommentItem(renderer = mainComment, onShowReplies = null)
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(
                replies,
                key = { item -> item.commentId ?: "reply-${item.hashCode()}" }
            ) { replyRenderer ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.width(36.dp))
                    CommentItem(renderer = replyRenderer, isReply = true)
                }
            }
            
            if (repliesNextToken != null) {
                item(key = "replies_pagination") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingReplies) {
                            CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }

                    LaunchedEffect(repliesNextToken) {
                        if (isLoadingReplies) return@LaunchedEffect
                        
                        // Debounce: let the UI settle before starting the network request
                        kotlinx.coroutines.delay(500L)
                        
                        isLoadingReplies = true
                        YouTube.commentReplies(repliesNextToken!!).onSuccess { (newReplies, token) ->
                            replies = (replies + newReplies).distinctBy { it.commentId ?: it.hashCode() }
                            repliesNextToken = token
                            isLoadingReplies = false
                        }.onFailure {
                            isLoadingReplies = false
                        }
                    }
                }
            }
        }
    }
}
