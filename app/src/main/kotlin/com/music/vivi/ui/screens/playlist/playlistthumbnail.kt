package com.music.vivi.ui.screens.playlist

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.music.innertube.YouTube
import com.music.vivi.LocalDatabase
import com.music.vivi.R
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.db.entities.Playlist
import com.music.vivi.ui.component.ActionPromptDialog
import com.music.vivi.ui.component.OverlayEditButton
import com.music.vivi.ui.menu.CustomThumbnailMenu
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.reportException
import com.yalantis.ucrop.UCrop
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlaylistThumbnail(
    playlist: Playlist,
    editable: Boolean,
    menuState: com.music.vivi.ui.component.MenuState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()

    val overrideThumbnail = remember { mutableStateOf<String?>(null) }
    var isCustomThumbnail: Boolean = playlist.thumbnails.firstOrNull()?.let {
        it.contains("studio_square_thumbnail") || it.contains("content://com.music.vivi")
    } ?: false

    val result = remember { mutableStateOf<Uri?>(null) }
    var pendingCropDestUri by remember { mutableStateOf<Uri?>(null) }
    var showEditNoteDialog by remember { mutableStateOf(false) }

    val cropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == android.app.Activity.RESULT_OK) {
            val output = res.data?.let { UCrop.getOutput(it) } ?: pendingCropDestUri
            if (output != null) result.value = output
        }
    }

    val (darkMode, _) = rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val cropColor = MaterialTheme.colorScheme
    val darkTheme = darkMode == DarkMode.ON || (darkMode == DarkMode.AUTO && isSystemInDarkTheme())

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { sourceUri ->
            val destFile = java.io.File(context.cacheDir, "playlist_cover_crop_${System.currentTimeMillis()}.jpg")
            val destUri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", destFile)
            pendingCropDestUri = destUri

            val options = UCrop.Options().apply {
                setCompressionFormat(Bitmap.CompressFormat.JPEG)
                setCompressionQuality(90)
                setHideBottomControls(true)
                setToolbarTitle(context.getString(R.string.edit_playlist_cover))
                setStatusBarLight(!darkTheme)
                setToolbarColor(cropColor.surface.toArgb())
                setToolbarWidgetColor(cropColor.inverseSurface.toArgb())
                setRootViewBackgroundColor(cropColor.surface.toArgb())
                setLogoColor(cropColor.surface.toArgb())
            }

            val intent = UCrop.of(sourceUri, destUri)
                .withAspectRatio(1f, 1f)
                .withOptions(options)
                .getIntent(context)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            cropLauncher.launch(intent)
        }
    }

    LaunchedEffect(result.value) {
        val uri = result.value ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            when {
                playlist.playlist.browseId == null -> {
                    overrideThumbnail.value = uri.toString()
                    isCustomThumbnail = true
                    database.query {
                        update(playlist.playlist.copy(thumbnailUrl = uri.toString()))
                    }
                }
                else -> {
                    val bytes = uriToByteArray(context, uri)
                    YouTube.uploadCustomThumbnailLink(
                        playlist.playlist.browseId,
                        bytes!!
                    ).onSuccess { newThumbnailUrl ->
                        overrideThumbnail.value = newThumbnailUrl
                        isCustomThumbnail = true
                        database.query {
                            update(playlist.playlist.copy(thumbnailUrl = newThumbnailUrl))
                        }
                    }.onFailure {
                        if (it is ClientRequestException) {
                            snackbarHostState.showSnackbar(
                                "${it.response.status.value} ${it.response.status.description}"
                            )
                        }
                        reportException(it)
                    }
                }
            }
        }
    }

    if (showEditNoteDialog) {
        ActionPromptDialog(
            title = stringResource(R.string.edit_playlist_cover),
            onDismiss = { showEditNoteDialog = false },
            onConfirm = {
                showEditNoteDialog = false
                pickLauncher.launch(
                    PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onCancel = { showEditNoteDialog = false }
        ) {
            if (playlist.playlist.browseId != null) {
                Text(
                    text = stringResource(R.string.edit_playlist_cover_note),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = stringResource(R.string.edit_playlist_cover_note_wait),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }

    when (playlist.thumbnails.size) {
        0 -> Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Icon(
                painter = painterResource(R.drawable.queue_music),
                contentDescription = null,
                tint = LocalContentColor.current.copy(alpha = 0.8f),
                modifier = Modifier.size(96.dp)
            )
        }
        1 -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(overrideThumbnail.value ?: playlist.thumbnails[0])
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (editable) {
                    OverlayEditButton(
                        visible = true,
                        onClick = {
                            if (isCustomThumbnail) {
                                menuState.show {
                                    CustomThumbnailMenu(
                                        onEdit = {
                                            pickLauncher.launch(
                                                PickVisualMediaRequest(
                                                    mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        },
                                        onRemove = {
                                            when {
                                                playlist.playlist.browseId == null -> {
                                                    overrideThumbnail.value = null
                                                    database.query {
                                                        update(playlist.playlist.copy(thumbnailUrl = null))
                                                    }
                                                }
                                                else -> {
                                                    scope.launch(Dispatchers.IO) {
                                                        YouTube.removeThumbnailPlaylist(
                                                            playlist.playlist.browseId
                                                        ).onSuccess { newThumbnailUrl ->
                                                            overrideThumbnail.value = newThumbnailUrl
                                                            database.query {
                                                                update(
                                                                    playlist.playlist.copy(
                                                                        thumbnailUrl = newThumbnailUrl
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            isCustomThumbnail = false
                                        },
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            } else {
                                showEditNoteDialog = true
                            }
                        },
                        alignment = Alignment.BottomEnd
                    )
                }
            }
        }
        else -> {
            Box(modifier = modifier) {
                listOf(
                    Alignment.TopStart,
                    Alignment.TopEnd,
                    Alignment.BottomStart,
                    Alignment.BottomEnd
                ).fastForEachIndexed { index, alignment ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(overrideThumbnail.value ?: playlist.thumbnails.getOrNull(index))
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(alignment)
                            .fillMaxSize(0.5f)
                    )
                }
                if (editable) {
                    OverlayEditButton(
                        visible = true,
                        onClick = {
                            if (isCustomThumbnail) {
                                menuState.show {
                                    CustomThumbnailMenu(
                                        onEdit = {
                                            pickLauncher.launch(
                                                PickVisualMediaRequest(
                                                    mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        },
                                        onRemove = {
                                            when {
                                                playlist.playlist.browseId == null -> {
                                                    overrideThumbnail.value = null
                                                    database.query {
                                                        update(playlist.playlist.copy(thumbnailUrl = null))
                                                    }
                                                }
                                                else -> {
                                                    scope.launch(Dispatchers.IO) {
                                                        YouTube.removeThumbnailPlaylist(
                                                            playlist.playlist.browseId
                                                        ).onSuccess { newThumbnailUrl ->
                                                            overrideThumbnail.value = newThumbnailUrl
                                                            database.query {
                                                                update(
                                                                    playlist.playlist.copy(
                                                                        thumbnailUrl = newThumbnailUrl
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            isCustomThumbnail = false
                                        },
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            } else {
                                showEditNoteDialog = true
                            }
                        },
                        alignment = Alignment.BottomEnd
                    )
                }
            }
        }
    }
}

fun uriToByteArray(context: Context, uri: Uri): ByteArray? = try {
    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
} catch (_: SecurityException) {
    null
}
