/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.github.news.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.vivi.R
import com.music.vivi.github.news.models.Announcement
import com.music.vivi.github.news.network.AnnouncementRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAnnouncementBottomSheet(
    announcement: Announcement?,
    repository: AnnouncementRepository,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
) {
    if (announcement == null) return

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    // Trigger a live refresh on open so latest edits on GitHub reflect immediately
    LaunchedEffect(Unit) {
        repository.refreshAnnouncement(context, force = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(2.dp),
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            // Header Row: Tag Badge + Date + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Text(
                            text = announcement.tag ?: stringResource(R.string.announcement_tag_news),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }

                    if (!announcement.date.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = announcement.date,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                            ),
                        )
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = stringResource(R.string.announcement_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            if (announcement.title.isNotBlank()) {
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Media Preview: Video Player or Image (176dp height, 8dp radius)
            if (!announcement.videoUrl.isNullOrBlank()) {
                AnnouncementVideoPlayer(
                    videoUrl = announcement.videoUrl,
                    height = 176.dp,
                    autoPlay = true,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
            } else if (!announcement.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(announcement.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .padding(bottom = 14.dp),
                )
            }

            // Rich Markdown Body (directly renders live announcement content)
            if (announcement.content.isNotBlank()) {
                RichMarkdownText(
                    markdown = announcement.content,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dynamic Action Buttons
            val hasPrimaryAction = !announcement.actionUrl.isNullOrBlank()
            val hasSecondaryAction = !announcement.secondaryActionUrl.isNullOrBlank()

            if (hasPrimaryAction && hasSecondaryAction) {
                // Dual Actions: Primary & Secondary in a row, Close button full-width below
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = {
                                try {
                                    uriHandler.openUri(announcement.actionUrl!!)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text(
                                text = announcement.actionLabel ?: stringResource(R.string.announcement_read_more),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(R.drawable.open_in_new_icon),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                try {
                                    uriHandler.openUri(announcement.secondaryActionUrl!!)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = announcement.secondaryActionLabel ?: stringResource(R.string.announcement_read_more),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(R.drawable.open_in_new_icon),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(text = stringResource(R.string.announcement_close))
                    }
                }
            } else if (hasPrimaryAction) {
                // Single Action: Action button + Close button side-by-side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri(announcement.actionUrl!!)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = announcement.actionLabel ?: stringResource(R.string.announcement_read_more),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(R.drawable.open_in_new_icon),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    OutlinedButton(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(text = stringResource(R.string.announcement_close))
                    }
                }
            } else {
                // No Action Link: Close button full-width
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(text = stringResource(R.string.announcement_close))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
