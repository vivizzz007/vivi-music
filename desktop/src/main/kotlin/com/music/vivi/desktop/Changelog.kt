package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One release in the changelog: a version heading plus its change sections. */
data class ChangelogRelease(
    val version: String,
    val date: String,
    val sections: List<ChangelogSection>,
)

/** A Keep-a-Changelog section (Added / Fixed / Changed / …) and its bullets. */
data class ChangelogSection(
    val title: String,
    val items: List<String>,
)

/** Reads the CHANGELOG.md bundled as a classpath resource and parses it. */
object ChangelogLoader {
    fun fromResources(): String? = runCatching {
        ChangelogLoader::class.java.getResourceAsStream("/CHANGELOG.md")
            ?.bufferedReader()
            ?.use { it.readText() }
    }.getOrNull()

    private val releaseRegex = Regex("""^##\s+\[([^\]]+)](?:\s*-\s*(.*))?$""")
    private val sectionRegex = Regex("""^###\s+(.+)$""")
    private val bulletRegex = Regex("""^-\s+(.+)$""")

    /**
     * Parses the Keep-a-Changelog markdown into structured releases, matching
     * the mobile app's changelog model (`ChangelogSection` + per-version list).
     * `[Unreleased]` is skipped; multi-line bullets (indented continuations)
     * are joined back together.
     */
    fun parse(markdown: String): List<ChangelogRelease> {
        val releases = mutableListOf<ChangelogRelease>()

        var version: String? = null
        var date: String = ""
        val sections = mutableListOf<ChangelogSection>()
        var sectionTitle: String? = null
        val sectionItems = mutableListOf<String>()
        var currentItem: StringBuilder? = null

        fun endItem() {
            currentItem?.let { sb ->
                val text = sb.toString().trim()
                if (text.isNotEmpty() && sectionTitle != null) sectionItems.add(text)
            }
            currentItem = null
        }

        fun endSection() {
            endItem()
            val title = sectionTitle ?: return
            sections.add(ChangelogSection(title, sectionItems.toList()))
            sectionItems.clear()
            sectionTitle = null
        }

        fun endRelease() {
            endSection()
            val v = version ?: return
            releases.add(ChangelogRelease(v, date, sections.toList()))
            sections.clear()
            version = null
            date = ""
        }

        for (rawLine in markdown.lines()) {
            val line = rawLine.trimEnd()

            releaseRegex.matchEntire(line)?.let { m ->
                endRelease()
                val v = m.groupValues[1].trim()
                if (v.equals("Unreleased", ignoreCase = true)) {
                    version = null
                    date = ""
                } else {
                    version = v
                    date = m.groupValues[2].trim()
                }
                continue
            }

            sectionRegex.matchEntire(line)?.let { m ->
                if (version != null) {
                    endSection()
                    sectionTitle = m.groupValues[1].trim()
                }
                continue
            }

            bulletRegex.matchEntire(line)?.let { m ->
                if (version != null && sectionTitle != null) {
                    endItem()
                    currentItem = StringBuilder(m.groupValues[1].trim())
                }
                continue
            }

            // Indented continuation of the previous bullet.
            if (currentItem != null && line.isNotBlank() && !line.startsWith("#")) {
                currentItem!!.append(' ').append(line.trim())
            }
        }
        endRelease()

        return releases
    }
}

/** Strips inline markdown (backticks / emphasis) so bullets read cleanly. */
private fun cleanInline(text: String): String =
    text.replace("`", "").replace("**", "")

/**
 * About → Changelog: renders the repository `CHANGELOG.md` like the mobile app
 * — a version selector plus the selected version's Added/Fixed/Changed sections
 * — but with the version buttons in a vertical list (mouse-friendly) instead of
 * the mobile's horizontally scrollable chips. Falls back to the bundled copy
 * when offline.
 */
@Composable
fun ChangelogScreen(language: String, onBack: () -> Unit) {
    var repoChangelog by remember { mutableStateOf<String?>(null) }
    val bundled = remember { ChangelogLoader.fromResources() }

    LaunchedEffect(Unit) {
        repoChangelog = withContext(Dispatchers.IO) { UpdateChecker.fetchChangelogFromRepo() }
    }

    val markdown = (repoChangelog ?: bundled).orEmpty()
    val releases = remember(markdown) { ChangelogLoader.parse(markdown) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Text(Localization.get(language, "changelog"), style = MaterialTheme.typography.headlineMedium)
        Text(
            "VIVI Music DE ${AppInfo.FULL_VERSION} ${AppInfo.CHANNEL.uppercase()}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )

        if (releases.isEmpty()) {
            Text(
                Localization.get(language, "changelog_unavailable"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            return@Column
        }

        var selectedVersion by remember(releases) { mutableStateOf(releases.first().version) }
        val selected = releases.firstOrNull { it.version == selectedVersion } ?: releases.first()

        Row(
            Modifier.weight(1f).padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Vertical version selector (the mobile chips, listed top-to-bottom).
            Column(
                Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
            ) {
                releases.forEach { release ->
                    val isSelected = release.version == selected.version
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .clickable { selectedVersion = release.version }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            release.version,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                        )
                        if (release.date.isNotBlank()) {
                            Text(
                                release.date,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Selected release details.
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
            ) {
                ReleaseSection(selected)
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ReleaseSection(release: ChangelogRelease) {
    Text(
        release.version,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    if (release.date.isNotBlank()) {
        Text(
            release.date,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    release.sections.forEach { section ->
        Spacer(Modifier.height(12.dp))
        if (section.title.isNotBlank()) {
            Text(
                section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
        }
        section.items.forEach { item ->
            Row(
                Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
                Text(
                    cleanInline(item),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    HorizontalDivider(Modifier.padding(top = 16.dp))
}
