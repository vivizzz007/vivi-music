/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.playback.download

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * A downloaded song's [localFileUri][com.music.vivi.db.entities.SongEntity.localFileUri] is either
 * a `file://` URI (default internal storage, backed by [DocumentFile.fromFile]) or a `content://`
 * URI (user-chosen SAF folder, backed by [DocumentFile.fromTreeUri]). [DocumentFile.fromSingleUri]
 * only understands `content://` — this dispatches to the right check/delete for either scheme.
 */
object LocalDownloadFile {
    fun exists(context: Context, uri: Uri): Boolean = runCatching {
        if (uri.scheme == "file") {
            uri.path?.let { File(it).exists() } == true
        } else {
            DocumentFile.fromSingleUri(context, uri)?.exists() == true
        }
    }.getOrDefault(false)

    fun delete(context: Context, uri: Uri): Boolean = runCatching {
        if (uri.scheme == "file") {
            uri.path?.let { File(it).delete() } == true
        } else {
            DocumentFile.fromSingleUri(context, uri)?.delete() == true
        }
    }.getOrDefault(false)
}
