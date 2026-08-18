package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/** A single commit, reduced to what the list needs to render. */
data class CommitData(
    val sha: String,
    val message: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val date: String,
    val htmlUrl: String,
)

/** Minimal subset of the GitHub commits API response. */
@Serializable
data class GitHubCommit(
    val sha: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val commit: GitHubCommitDetail? = null,
    val author: GitHubCommitUser? = null,
)

@Serializable
data class GitHubCommitDetail(
    val message: String = "",
    val author: GitHubCommitAuthor? = null,
)

@Serializable
data class GitHubCommitAuthor(
    val name: String = "",
    val date: String = "",
)

@Serializable
data class GitHubCommitUser(
    val login: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

/**
 * Fetches the most recent commits of the configured update source (fork or
 * original) from the GitHub API, mirroring the mobile Commit screen but using
 * `UpdateSource.repo()/branch()` so the DE shows its own branch history.
 */
object CommitFetcher {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun fetch(): List<CommitData> = try {
        val request = Request.Builder()
            .url("https://api.github.com/repos/${UpdateSource.repo()}/commits?branch=${UpdateSource.branch()}&per_page=50")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "VIVIMusic-Desktop-Updater")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val commits = json.decodeFromString<List<GitHubCommit>>(response.body.string())
            commits.map { c ->
                val detail = c.commit
                val fullMessage = detail?.message.orEmpty()
                val message = fullMessage.lines().firstOrNull { it.isNotBlank() } ?: fullMessage
                val authorName = detail?.author?.name?.takeIf { it.isNotBlank() }
                    ?: c.author?.login?.takeIf { it.isNotBlank() }
                    ?: "Unknown"
                CommitData(
                    sha = c.sha,
                    message = message,
                    authorName = authorName,
                    authorAvatarUrl = c.author?.avatarUrl,
                    date = formatDate(detail?.author?.date.orEmpty()),
                    htmlUrl = c.htmlUrl,
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun formatDate(raw: String): String = try {
        ZonedDateTime.parse(raw).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
    } catch (_: Exception) {
        raw
    }
}

/**
 * Settings → Updates → Commits: lists the most recent repository commits
 * (port of the mobile `settings/commits` screen). Clicking a commit opens it
 * in the browser.
 */
@Composable
fun CommitsScreen(language: String, onBack: () -> Unit) {
    var commits by remember { mutableStateOf<List<CommitData>?>(null) }

    LaunchedEffect(Unit) {
        commits = withContext(Dispatchers.IO) { CommitFetcher.fetch() }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Text(Localization.get(language, "commits"), style = MaterialTheme.typography.headlineMedium)
        Text(
            Localization.get(language, "commits_desc"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        val list = commits
        when {
            list == null -> {
                Text(
                    Localization.get(language, "loading"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            list.isEmpty() -> {
                Text(
                    Localization.get(language, "error_loading_commits"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            else -> {
                LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                    items(list) { commit ->
                        CommitItem(commit, onClick = { openUrl(commit.htmlUrl) })
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 68.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CommitItem(commit: CommitData, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Left: commit icon in a circle (the mobile's R.drawable.commit indicator).
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }

        // Center: commit message + author/date + short SHA.
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                commit.message,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    commit.authorName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "·",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    commit.date,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    commit.sha.take(7),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        // Right: author avatar (fallback to initials).
        val avatar = commit.authorAvatarUrl
        if (avatar != null) {
            AsyncImage(
                model = avatar,
                contentDescription = commit.authorName,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    commit.authorName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}
