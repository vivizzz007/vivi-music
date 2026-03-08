/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.UriHandler
import com.music.vivi.R

/**
 * Safely opens a URI using the provided [UriHandler].
 * If the URI cannot be handled (e.g., no app installed for the scheme), 
 * it shows a toast message instead of crashing.
 */
fun UriHandler.safeOpenUri(context: Context, uri: String) {
    if (uri.isBlank()) return
    
    runCatching {
        openUri(uri)
    }.onFailure {
        Toast.makeText(
            context,
            context.getString(R.string.error_no_stream).replace("stream", "app"), // Fallback if specific string not found
            Toast.LENGTH_SHORT
        ).show()
    }
}
