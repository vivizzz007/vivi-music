package com.music.vivi.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads the CHANGELOG.md bundled as a classpath resource. */
object ChangelogLoader {
    fun fromResources(): String? = runCatching {
        ChangelogLoader::class.java.getResourceAsStream("/CHANGELOG.md")
            ?.bufferedReader()
            ?.use { it.readText() }
    }.getOrNull()
}

/**
 * About → Changelog: shows the latest GitHub release notes (when reachable)
 * followed by the bundled CHANGELOG.md, rendered with a lightweight markdown
 * subset (headings, bullets, paragraphs).
 */
@Composable
fun ChangelogScreen(language: String, onBack: () -> Unit) {
    var releaseNotes by remember { mutableStateOf<String?>(null) }
    val local = remember { ChangelogLoader.fromResources() }

    LaunchedEffect(Unit) {
        releaseNotes = withContext(Dispatchers.IO) { UpdateChecker.latestReleaseNotes() }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Text(Localization.get(language, "changelog"), style = MaterialTheme.typography.headlineMedium)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp),
        ) {
            if (!releaseNotes.isNullOrBlank()) {
                Text(
                    Localization.get(language, "latest_release"),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                MarkdownLite(releaseNotes!!)
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
            }
            if (local.isNullOrBlank()) {
                Text(
                    Localization.get(language, "changelog_unavailable"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                MarkdownLite(local)
            }
        }
    }
}

/** Minimal markdown renderer: `##`/`###` headings, `-` bullets, blank lines. */
@Composable
private fun MarkdownLite(markdown: String) {
    Column {
        markdown.lines().forEach { line ->
            val trimmed = line.trimEnd()
            when {
                trimmed.startsWith("## ") -> Text(
                    trimmed.removePrefix("## ").trim(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                trimmed.startsWith("### ") -> Text(
                    trimmed.removePrefix("### ").trim(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                )
                trimmed.startsWith("- ") -> Text(
                    "• ${trimmed.removePrefix("- ").trim()}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                )
                trimmed.isBlank() -> Spacer(Modifier.height(4.dp))
                else -> Text(trimmed, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
