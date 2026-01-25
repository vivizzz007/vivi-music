package com.music.vivi.update.changelog

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.music.vivi.BuildConfig
import com.music.vivi.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import java.net.URL

@Composable
fun ChangelogBottomSheet(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val changelogUrl = "https://github.com/vivizzz007/vivi-music"
    val currentVersion = BuildConfig.VERSION_NAME // e.g., "4.0.1"

    var changelogState by remember { mutableStateOf<ChangelogState>(ChangelogState.Loading) }

    // Fetch changelog on first composition
    LaunchedEffect(Unit) {
        changelogState = fetchChangelogFromGitHub(currentVersion, context)
    }

    val fabCornerRadius = 16.dp

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.changelog_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            SineWaveLine(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .height(32.dp)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 4.dp),
                animate = true,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                alpha = 0.95f,
                strokeWidth = 4.dp,
                amplitude = 4.dp,
                waves = 7.6f,
                phase = 0f
            )

            // Content based on state
            when (val state = changelogState) {
                is ChangelogState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.loading_changelog),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        SineWaveLine(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(40.dp),
                            animate = true,
                            color = MaterialTheme.colorScheme.primary,
                            alpha = 0.85f,
                            strokeWidth = 3.dp,
                            amplitude = 6.dp,
                            waves = 3f,
                            animationDurationMillis = 1500
                        )
                    }
                }

                is ChangelogState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.error),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.failed_to_load_changelog),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is ChangelogState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(state.changelog) { version ->
                            ChangelogVersionItemvivi(version = version)
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { openUrl(context, changelogUrl) },
            shape = AbsoluteSmoothCornerShape(
                cornerRadiusBR = fabCornerRadius,
                smoothnessAsPercentBR = 60,
                cornerRadiusBL = fabCornerRadius,
                smoothnessAsPercentBL = 60,
                cornerRadiusTR = fabCornerRadius,
                smoothnessAsPercentTR = 60,
                cornerRadiusTL = fabCornerRadius,
                smoothnessAsPercentTL = 60
            ),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.github),
                    contentDescription = null
                )
            },
            text = { Text(text = stringResource(R.string.view_on_github)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(30.dp)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                )
        )
    }
}

@Composable
fun ChangelogCategory(section: ChangelogSection) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            section.items.forEachIndexed { index, item ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (index != section.items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }
}

@Composable
fun ChangelogVersionItemvivi(version: ChangelogVersion) {
    val currentVersion = BuildConfig.VERSION_NAME
    val isCurrentVersion = version.version.removePrefix("v") == currentVersion

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VersionBadgevivi(
                versionNumber = version.version,
                isCurrentVersion = isCurrentVersion
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = version.date,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            version.sections.forEach { section ->
                ChangelogCategory(section = section)
            }
        }
    }
}

// Modified VersionBadge with current version highlighting
@Composable
fun VersionBadgevivi(versionNumber: String, isCurrentVersion: Boolean = false) {
    Box(
        modifier = Modifier
            .background(
                color = if (isCurrentVersion) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
                shape = CircleShape
            )
    ) {
        Text(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
            text = versionNumber,
            color = if (isCurrentVersion) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun openUrl(context: Context, url: String) {
    val uri = try {
        url.toUri()
    } catch (_: Throwable) {
        url.toUri()
    }
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

data class ChangelogSection(val title: String, val items: List<String>)

// Data class for a single changelog version
data class ChangelogVersion(val version: String, val date: String, val sections: List<ChangelogSection>)

// State for changelog
sealed class ChangelogState {
    object Loading : ChangelogState()
    data class Success(val changelog: List<ChangelogVersion>) : ChangelogState()
    data class Error(val message: String) : ChangelogState()
}

// Function to fetch changelog from GitHub
suspend fun fetchChangelogFromGitHub(
    currentVersion: String,
    context: Context,
    defaultSectionTitle: String = context.getString(R.string.changes_default),
): ChangelogState = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://api.github.com/repos/vivizzz007/vivi-music/releases")
        val connection = url.openConnection()
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

        val response = connection.getInputStream().bufferedReader().use { it.readText() }
        val releases = JSONArray(response)

        val changelogList = mutableListOf<ChangelogVersion>()

        // Find the release matching current version
        for (i in 0 until releases.length()) {
            val release = releases.getJSONObject(i)
            val tagName = release.getString("tag_name") // Keep as is: v4.0.6
            val versionWithoutV = tagName.removePrefix("v") // For comparison: 4.0.6

            // Only include releases up to current version
            if (compareVersions(versionWithoutV, currentVersion) <= 0) {
                val body = release.getString("body")
                val publishedAt = release.getString("published_at").take(10) // Get date only

                val sections = parseChangelogBody(body, defaultSectionTitle)

                changelogList.add(
                    ChangelogVersion(
                        version = tagName,
                        date = publishedAt,
                        sections = sections
                    )
                )
            }
        }

        ChangelogState.Success(changelogList.sortedByDescending { it.version })
    } catch (e: Exception) {
        ChangelogState.Error(e.message ?: context.getString(R.string.failed_to_fetch_changelog))
    }
}

// Parse GitHub release body into sections
fun parseChangelogBody(body: String, defaultSectionTitle: String): List<ChangelogSection> {
    val sections = mutableListOf<ChangelogSection>()
    val lines = body.split("\n")

    var currentTitle = ""
    val currentItems = mutableListOf<String>()

    for (line in lines) {
        val trimmedLine = line.trim()

        when {
            // Section headers (e.g., "## What's New", "### Added")
            trimmedLine.startsWith("##") || trimmedLine.startsWith("###") -> {
                // Save previous section if exists
                if (currentTitle.isNotEmpty() && currentItems.isNotEmpty()) {
                    sections.add(ChangelogSection(currentTitle, currentItems.toList()))
                    currentItems.clear()
                }
                currentTitle = trimmedLine.removePrefix("###").removePrefix("##").trim()
            }
            // Bullet points (-, *, +)
            trimmedLine.startsWith("-") || trimmedLine.startsWith("*") || trimmedLine.startsWith("+") -> {
                val item = trimmedLine.removePrefix("-").removePrefix("*").removePrefix("+").trim()
                if (item.isNotEmpty()) {
                    currentItems.add(item)
                }
            }
        }
    }

    // Add last section
    if (currentTitle.isNotEmpty() && currentItems.isNotEmpty()) {
        sections.add(ChangelogSection(currentTitle, currentItems.toList()))
    }

    // If no sections found, create a single "Changes" section with the body
    if (sections.isEmpty() && body.isNotBlank()) {
        sections.add(
            ChangelogSection(
                defaultSectionTitle,
                body.split("\n").filter { it.trim().isNotEmpty() }
            )
        )
    }

    return sections
}

// Compare version numbers (e.g., "4.0.1" vs "4.0.0")
fun compareVersions(v1: String, v2: String): Int {
    val parts1 = v1.split(".").mapNotNull { it.toIntOrNull() }
    val parts2 = v2.split(".").mapNotNull { it.toIntOrNull() }

    val maxLength = maxOf(parts1.size, parts2.size)

    for (i in 0 until maxLength) {
        val part1 = parts1.getOrElse(i) { 0 }
        val part2 = parts2.getOrElse(i) { 0 }

        if (part1 != part2) {
            return part1.compareTo(part2)
        }
    }

    return 0
}
