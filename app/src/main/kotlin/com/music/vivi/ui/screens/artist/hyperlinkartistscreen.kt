package com.music.vivi.ui.screens.artist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/**
 * Helper function to parse text and create an AnnotatedString with clickable URL links.
 */
 fun parseTextWithLinks(text: String, linkColor: Color): AnnotatedString {
    // Regex pattern to match URLs (http, https, www)
    val urlPattern = Regex(
        """(https?://[^\s]+)|(www\.[^\s]+)""",
        RegexOption.IGNORE_CASE
    )

    return buildAnnotatedString {
        var lastIndex = 0

        urlPattern.findAll(text).forEach { matchResult ->
            val startIndex = matchResult.range.first
            val endIndex = matchResult.range.last + 1
            val url = matchResult.value

            // Append text before the URL
            append(text.substring(lastIndex, startIndex))

            // Append the URL with link styling and annotation
            pushStringAnnotation(
                tag = "URL",
                annotation = if (url.startsWith("www.")) "https://$url" else url
            )
            withStyle(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(url)
            }
            pop()

            lastIndex = endIndex
        }

        // Append remaining text after the last URL
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}
