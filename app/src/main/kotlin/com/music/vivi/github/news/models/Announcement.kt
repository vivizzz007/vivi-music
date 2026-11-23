/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.github.news.models

import androidx.compose.runtime.Immutable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Immutable
data class Announcement(
    val id: String,
    val title: String,
    val summary: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val previewMedia: String? = null,
    val tag: String? = null,
    val date: String? = null,
    val timestamp: String? = null,
    val expires: String? = null,
    val actionLabel: String? = null,
    val actionUrl: String? = null,
    val secondaryActionLabel: String? = null,
    val secondaryActionUrl: String? = null,
    val isDismissible: Boolean = true,
    val isActive: Boolean = true,
) {
    val hasMedia: Boolean
        get() = !imageUrl.isNullOrBlank() || !videoUrl.isNullOrBlank()

    val shouldPlayVideoInCard: Boolean
        get() = !videoUrl.isNullOrBlank() && (previewMedia.equals("video", ignoreCase = true) || imageUrl.isNullOrBlank())

    val isExpired: Boolean
        get() {
            val exp = expires?.trim() ?: return false
            if (exp.isEmpty() || exp.equals("null", ignoreCase = true)) return false
            return try {
                val now = Instant.now()
                val expiryInstant = when {
                    exp.contains("T") -> Instant.parse(exp)
                    else -> LocalDate.parse(exp).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
                }
                now.isAfter(expiryInstant)
            } catch (_: Exception) {
                false
            }
        }
}
