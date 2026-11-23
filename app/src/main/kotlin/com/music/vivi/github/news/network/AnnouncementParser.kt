/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.github.news.network

import com.music.vivi.github.news.models.Announcement

object AnnouncementParser {

    fun parse(rawMarkdown: String): Announcement? {
        val trimmed = rawMarkdown.trim()
        if (trimmed.isEmpty()) return null

        val frontmatterMap = mutableMapOf<String, String>()
        var bodyContent = ""

        if (trimmed.startsWith("---")) {
            val secondDivider = trimmed.indexOf("---", startIndex = 3)
            if (secondDivider != -1) {
                val frontmatterBlock = trimmed.substring(3, secondDivider).trim()
                bodyContent = trimmed.substring(secondDivider + 3).trim()

                frontmatterBlock.lines().forEach { line ->
                    val colonIdx = line.indexOf(':')
                    if (colonIdx != -1) {
                        val key = line.substring(0, colonIdx).trim()
                        val value = line.substring(colonIdx + 1).trim()
                            .removeSurrounding("\"")
                            .removeSurrounding("'")
                        if (key.isNotEmpty()) {
                            frontmatterMap[key] = value
                        }
                    }
                }
            } else {
                bodyContent = trimmed
            }
        } else {
            bodyContent = trimmed
        }

        val id = frontmatterMap["id"] ?: "0"
        val title = frontmatterMap["title"] ?: ""
        val summary = frontmatterMap["summary"] ?: ""
        val tag = frontmatterMap["tag"]?.takeIf { it != "null" && it.isNotBlank() }
        val date = frontmatterMap["date"]?.takeIf { it != "null" && it.isNotBlank() }
        val timestamp = frontmatterMap["timestamp"]?.takeIf { it != "null" && it.isNotBlank() }
        val expires = frontmatterMap["expires"]?.takeIf { it != "null" && it.isNotBlank() }
        val imageUrl = (frontmatterMap["image"] ?: frontmatterMap["imageUrl"])?.takeIf { it != "null" && it.isNotBlank() }
        val videoUrl = (frontmatterMap["video"] ?: frontmatterMap["videoUrl"])?.takeIf { it != "null" && it.isNotBlank() }
        val previewMedia = (frontmatterMap["previewMedia"] ?: frontmatterMap["cardMedia"])?.takeIf { it != "null" && it.isNotBlank() }
        val actionLabel = frontmatterMap["actionLabel"]?.takeIf { it != "null" && it.isNotBlank() }
        val actionUrl = frontmatterMap["actionUrl"]?.takeIf { it != "null" && it.isNotBlank() }
        val secondaryActionLabel = frontmatterMap["secondaryActionLabel"]?.takeIf { it != "null" && it.isNotBlank() }
        val secondaryActionUrl = frontmatterMap["secondaryActionUrl"]?.takeIf { it != "null" && it.isNotBlank() }
        val isDismissible = frontmatterMap["dismissible"]?.toBooleanStrictOrNull() ?: true
        val isActive = frontmatterMap["active"]?.toBooleanStrictOrNull() ?: true

        if (title.isBlank() && summary.isBlank() && bodyContent.isBlank()) {
            return null
        }

        return Announcement(
            id = id,
            title = title,
            summary = summary,
            content = bodyContent,
            imageUrl = imageUrl,
            videoUrl = videoUrl,
            previewMedia = previewMedia,
            tag = tag,
            date = date,
            timestamp = timestamp,
            expires = expires,
            actionLabel = actionLabel,
            actionUrl = actionUrl,
            secondaryActionLabel = secondaryActionLabel,
            secondaryActionUrl = secondaryActionUrl,
            isDismissible = isDismissible,
            isActive = isActive,
        )
    }
}
