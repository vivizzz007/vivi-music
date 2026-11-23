/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.github.news.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LINK_TAG = "URL_LINK"

@Composable
fun RichMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = FontFamily.Default,
) {
    val uriHandler = LocalUriHandler.current
    val lines = markdown.lines()

    Column(modifier = modifier.fillMaxWidth()) {
        lines.forEach { rawLine ->
            val line = rawLine.trimEnd()
            when {
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### ").trim(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamily,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## ").trim(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamily,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                    )
                }
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# ").trim(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamily,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    val bulletText = line.substring(2).trim()
                    val annotated = buildMarkdownAnnotatedString(
                        raw = bulletText,
                        linkColor = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                        )
                        ClickableText(
                            text = annotated,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = fontFamily,
                                lineHeight = 20.sp,
                            ),
                            onClick = { offset ->
                                annotated.getStringAnnotations(tag = LINK_TAG, start = offset, end = offset)
                                    .firstOrNull()?.let { annotation ->
                                        try {
                                            uriHandler.openUri(annotation.item)
                                        } catch (_: Exception) {
                                            // Handle invalid URL or missing browser
                                        }
                                    }
                            },
                        )
                    }
                }
                else -> {
                    val annotated = buildMarkdownAnnotatedString(
                        raw = line,
                        linkColor = MaterialTheme.colorScheme.primary,
                    )
                    ClickableText(
                        text = annotated,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = fontFamily,
                            lineHeight = 22.sp,
                        ),
                        modifier = Modifier.padding(vertical = 3.dp),
                        onClick = { offset ->
                            annotated.getStringAnnotations(tag = LINK_TAG, start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    try {
                                        uriHandler.openUri(annotation.item)
                                    } catch (_: Exception) {
                                        // Ignore
                                    }
                                }
                        },
                    )
                }
            }
        }
    }
}

/**
 * Parses markdown inline styles: [label](url), **bold**, *italic*, and raw https:// links.
 */
private fun buildMarkdownAnnotatedString(
    raw: String,
    linkColor: androidx.compose.ui.graphics.Color,
): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val linkRegex = Regex("""\[([^\]]+)\]\(([^)]+)\)|(https?://[^\s]+)""")

        val matches = linkRegex.findAll(raw).toList()

        for (match in matches) {
            // Append text before link
            if (match.range.first > cursor) {
                appendInlineFormattedText(raw.substring(cursor, match.range.first))
            }

            val linkLabel = match.groups[1]?.value
            val linkTarget = match.groups[2]?.value ?: match.groups[3]?.value ?: ""
            val displayText = linkLabel ?: linkTarget

            val start = length
            append(displayText)
            val end = length

            addStyle(
                style = SpanStyle(
                    color = linkColor,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                ),
                start = start,
                end = end,
            )
            addStringAnnotation(
                tag = LINK_TAG,
                annotation = linkTarget,
                start = start,
                end = end,
            )

            cursor = match.range.last + 1
        }

        if (cursor < raw.length) {
            appendInlineFormattedText(raw.substring(cursor))
        }
    }
}

private fun AnnotatedString.Builder.appendInlineFormattedText(text: String) {
    // Handle **bold**
    val boldRegex = Regex("""\*\*([^*]+)\*\*""")
    var cursor = 0
    val matches = boldRegex.findAll(text).toList()

    for (match in matches) {
        if (match.range.first > cursor) {
            appendItalicFormattedText(text.substring(cursor, match.range.first))
        }

        val boldContent = match.groups[1]?.value ?: ""
        val start = length
        append(boldContent)
        val end = length
        addStyle(
            style = SpanStyle(fontWeight = FontWeight.Bold),
            start = start,
            end = end,
        )

        cursor = match.range.last + 1
    }

    if (cursor < text.length) {
        appendItalicFormattedText(text.substring(cursor))
    }
}

private fun AnnotatedString.Builder.appendItalicFormattedText(text: String) {
    val italicRegex = Regex("""\*([^*]+)\*""")
    var cursor = 0
    val matches = italicRegex.findAll(text).toList()

    for (match in matches) {
        if (match.range.first > cursor) {
            append(text.substring(cursor, match.range.first))
        }

        val italicContent = match.groups[1]?.value ?: ""
        val start = length
        append(italicContent)
        val end = length
        addStyle(
            style = SpanStyle(fontStyle = FontStyle.Italic),
            start = start,
            end = end,
        )

        cursor = match.range.last + 1
    }

    if (cursor < text.length) {
        append(text.substring(cursor))
    }
}
