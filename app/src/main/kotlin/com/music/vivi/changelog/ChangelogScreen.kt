package com.music.vivi.changelog



import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.music.vivi.BuildConfig
import com.music.vivi.R
import com.music.vivi.ui.screens.settings.ChangelogViewModel
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    versionTag: String = "v${BuildConfig.VERSION_NAME}"
) {
    val context = LocalContext.current
    val changelogViewModel: ChangelogViewModel = viewModel()
    val cachedChangelog = remember { mutableStateOf<String?>(null) }

    // Load cached changelog first
    LaunchedEffect(Unit) {
        cachedChangelog.value = loadCachedChangelog(context, versionTag)

        // Then try to fetch fresh data
        changelogViewModel.loadChangelog("vivizzz007", "vivi-music", versionTag)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vivi Music") },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val uiState by changelogViewModel.uiState.collectAsState()

            // Save changelog to cache when it's loaded
            LaunchedEffect(uiState.changes) {
                if (uiState.changes.isNotEmpty()) {
                    saveChangelogToCache(context, versionTag, uiState.changes)
                }
            }

            when {
                uiState.isLoading && cachedChangelog.value == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null && cachedChangelog.value == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "Error loading changelog",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                else -> {
                    val changelogContent = uiState.changes.ifEmpty { cachedChangelog.value ?: "" }

                    if (changelogContent.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No changelog available")
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Changelog heading below top bar
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Changelog",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = versionTag,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            // Changelog content
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                if (uiState.error != null && cachedChangelog.value != null) {
                                    Text(
                                        text = "Showing cached changelog (offline)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                FormattedChangelogText(
                                    markdown = changelogContent,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormattedChangelogText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val lines = markdown.split("\n")
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Column(modifier = modifier) {
        for (line in lines) {
            val trimmedLine = line.trim()
            when {
                line.startsWith("# ") -> {
                    // Main heading (H1)
                    Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                line.startsWith("## ") -> {
                    // Sub heading (H2)
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                line.startsWith("### ") -> {
                    // Sub sub heading (H3)
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                isMarkdownImage(trimmedLine) -> {
                    // Markdown images: ![alt text](url)
                    val imageData = parseMarkdownImage(trimmedLine)
                    if (imageData != null) {
                        ChangelogImage(
                            imageUrl = imageData.url,
                            altText = imageData.altText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }
                isImageUrl(trimmedLine) -> {
                    // Direct image URLs
                    ChangelogImage(
                        imageUrl = trimmedLine,
                        altText = "Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
                isEmojiHeading(trimmedLine) -> {
                    // Emoji headings (like "🎵 Vivi Music – Latest Release", "✨ What's New", etc.)
                    Text(
                        text = trimmedLine,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    // List item with bullet point
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        val text = line.removePrefix("- ").removePrefix("* ")

                        // Check if the list item contains an image
                        if (isMarkdownImage(text)) {
                            val imageData = parseMarkdownImage(text)
                            if (imageData != null) {
                                Column(modifier = Modifier.weight(1f)) {
                                    ChangelogImage(
                                        imageUrl = imageData.url,
                                        altText = imageData.altText,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        } else if (containsLink(text)) {
                            ClickableLinkText(
                                text = text,
                                modifier = Modifier.weight(1f),
                                onLinkClick = { url ->
                                    try {
                                        uriHandler.openUri(url)
                                    } catch (e: Exception) {
                                        openLinkInBrowser(context, url)
                                    }
                                }
                            )
                        } else {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                isDetailLine(trimmedLine) -> {
                    // Detail lines that should have bullet points
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp, horizontal = 16.dp)
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        // Check if the detail line contains an image
                        if (isMarkdownImage(trimmedLine)) {
                            val imageData = parseMarkdownImage(trimmedLine)
                            if (imageData != null) {
                                Column(modifier = Modifier.weight(1f)) {
                                    ChangelogImage(
                                        imageUrl = imageData.url,
                                        altText = imageData.altText,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        } else if (containsLink(trimmedLine)) {
                            ClickableLinkText(
                                text = trimmedLine,
                                modifier = Modifier.weight(1f),
                                onLinkClick = { url ->
                                    try {
                                        uriHandler.openUri(url)
                                    } catch (e: Exception) {
                                        openLinkInBrowser(context, url)
                                    }
                                }
                            )
                        } else {
                            Text(
                                text = trimmedLine,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                trimmedLine.isNotEmpty() -> {
                    // Regular paragraph text without bullet points
                    if (containsLink(trimmedLine)) {
                        ClickableLinkText(
                            text = trimmedLine,
                            modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp),
                            onLinkClick = { url ->
                                try {
                                    uriHandler.openUri(url)
                                } catch (e: Exception) {
                                    openLinkInBrowser(context, url)
                                }
                            }
                        )
                    } else {
                        Text(
                            text = trimmedLine,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp)
                        )
                    }
                }
                else -> {
                    // Empty line for spacing
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun ChangelogImage(
    imageUrl: String,
    altText: String,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 400.dp),
            contentAlignment = Alignment.Center
        ) {
            if (hasError) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_image_not_supported), // You'll need to add this icon
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = altText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = altText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit,
                    onLoading = { isLoading = true },
                    onSuccess = { isLoading = false },
                    onError = {
                        isLoading = false
                        hasError = true
                    }
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

data class ImageData(
    val url: String,
    val altText: String
)

@Composable
private fun ClickableLinkText(
    text: String,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit
) {
    val urlPattern = Regex("https?://[^\\s]+")
    val matches = urlPattern.findAll(text).toList()

    if (matches.isEmpty()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier
        )
        return
    }

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0

        for (match in matches) {
            // Add text before the link
            append(text.substring(lastIndex, match.range.first))

            // Add the clickable link
            pushStringAnnotation(
                tag = "URL",
                annotation = match.value
            )
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(match.value)
            }
            pop()

            lastIndex = match.range.last + 1
        }

        // Add remaining text after the last link
        append(text.substring(lastIndex))
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations(
                tag = "URL",
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                onLinkClick(annotation.item)
            }
        }
    )
}

private fun containsLink(text: String): Boolean {
    return text.contains("http://") || text.contains("https://")
}

private fun isMarkdownImage(text: String): Boolean {
    val markdownImagePattern = Regex("!\\[.*?\\]\\(.*?\\)")
    return markdownImagePattern.containsMatchIn(text)
}

private fun parseMarkdownImage(text: String): ImageData? {
    val markdownImagePattern = Regex("!\\[(.*?)\\]\\((.*?)\\)")
    val match = markdownImagePattern.find(text)
    return if (match != null) {
        ImageData(
            url = match.groupValues[2],
            altText = match.groupValues[1].ifEmpty { "Image" }
        )
    } else null
}

private fun isImageUrl(text: String): Boolean {
    val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg")
    val lowerText = text.lowercase()
    return (text.startsWith("http://") || text.startsWith("https://")) &&
            imageExtensions.any { lowerText.endsWith(it) }
}

private fun openLinkInBrowser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("LinkOpener", "Error opening link: $url", e)
    }
}

private fun isEmojiHeading(line: String): Boolean {
    val emojiPattern = Regex("^[\\p{So}\\p{Sk}]")
    return emojiPattern.containsMatchIn(line) &&
            (line.contains("What's New") ||
                    line.contains("Improvements") ||
                    line.contains("Fixes") ||
                    line.contains("Links") ||
                    line.contains("Latest Release") ||
                    line.contains("Important Warning") ||
                    line.contains("New Features") ||
                    line.contains("Updates") ||
                    line.contains("Changes"))
}

private fun isDetailLine(line: String): Boolean {
    val emojiPattern = Regex("^[\\p{So}\\p{Sk}]")
    return emojiPattern.containsMatchIn(line) && !isEmojiHeading(line)
}

// Cache functions
private fun saveChangelogToCache(context: Context, versionTag: String, changelog: String) {
    try {
        context.openFileOutput("changelog_${versionTag}.txt", Context.MODE_PRIVATE).use {
            it.write(changelog.toByteArray())
        }
    } catch (e: Exception) {
        Log.e("ChangelogCache", "Error saving changelog", e)
    }
}

private fun loadCachedChangelog(context: Context, versionTag: String): String? {
    return try {
        context.openFileInput("changelog_${versionTag}.txt").use {
            it.bufferedReader().readText()
        }
    } catch (e: Exception) {
        null
    }
}