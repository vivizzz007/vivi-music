/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import com.music.vivi.R
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.utils.backToMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class UpstreamRelease(
    val tagName: String,
    val name: String,
    val publishedAt: String,
    val body: String,
)

data class UpstreamNightly(
    val runNumber: Long,
    val title: String,
    val branch: String,
    val commitSha: String,
    val createdAt: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpstreamUpdatesScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var releases by remember { mutableStateOf<List<UpstreamRelease>>(emptyList()) }
    var nightlies by remember { mutableStateOf<List<UpstreamNightly>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    }

    fun fetchData() {
        coroutineScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isLoading = true
                errorMessage = null
            }
            try {
                // 1. Fetch releases
                val releaseList = mutableListOf<UpstreamRelease>()
                try {
                    val releasesUrl = URL("https://api.github.com/repos/vivizzz007/vivi-music/releases?per_page=50")
                    val conn = releasesUrl.openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", "ViviMusic-App")
                    conn.setRequestProperty("Accept", "application/vnd.github+json")
                    if (conn.responseCode == 200) {
                        val json = conn.inputStream.bufferedReader().use { it.readText() }
                        val arr = JSONArray(json)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val tagName = obj.optString("tag_name", "")
                            val name = obj.optString("name", tagName)
                            val publishedAtRaw = obj.optString("published_at", "")
                            val formattedDate = try {
                                ZonedDateTime.parse(publishedAtRaw).format(dateFormatter)
                            } catch (e: Exception) {
                                publishedAtRaw
                            }
                            val body = obj.optString("body", "")
                            releaseList.add(
                                UpstreamRelease(
                                    tagName = tagName,
                                    name = name,
                                    publishedAt = formattedDate,
                                    body = body
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag("UpstreamTracker").e(e, "Error fetching releases")
                }

                // 2. Fetch nightly builds
                val nightlyList = mutableListOf<UpstreamNightly>()
                try {
                    val nightlyUrl = URL("https://api.github.com/repos/vivizzz007/vivi-music/actions/workflows/nightly.yml/runs?status=success&per_page=30")
                    val conn = nightlyUrl.openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", "ViviMusic-App")
                    conn.setRequestProperty("Accept", "application/vnd.github+json")
                    if (conn.responseCode == 200) {
                        val json = conn.inputStream.bufferedReader().use { it.readText() }
                        val root = JSONObject(json)
                        val arr = root.optJSONArray("workflow_runs") ?: JSONArray()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val runNum = obj.optLong("run_number", 0L)
                            val title = obj.optString("display_title", "Nightly Run #$runNum")
                            val branch = obj.optString("head_branch", "main")
                            val sha = obj.optString("head_sha", "").take(7)
                            val createdAtRaw = obj.optString("created_at", "")
                            val formattedDate = try {
                                ZonedDateTime.parse(createdAtRaw).format(dateFormatter)
                            } catch (e: Exception) {
                                createdAtRaw
                            }
                            nightlyList.add(
                                UpstreamNightly(
                                    runNumber = runNum,
                                    title = title,
                                    branch = branch,
                                    commitSha = sha,
                                    createdAt = formattedDate
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag("UpstreamTracker").e(e, "Error fetching nightly runs")
                }

                withContext(Dispatchers.Main) {
                    releases = releaseList
                    nightlies = nightlyList
                    isLoading = false
                    isRefreshing = false
                    if (releaseList.isEmpty() && nightlyList.isEmpty()) {
                        errorMessage = "Unable to load upstream updates. Check your connection."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = e.message ?: "Failed to load upstream data"
                    isLoading = false
                    isRefreshing = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues())
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.upstream_tracker_title)) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null
                    )
                }
            }
        )

        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        if (releases.isNotEmpty()) "${stringResource(R.string.upstream_releases)} (${releases.size})"
                        else stringResource(R.string.upstream_releases)
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        if (nightlies.isNotEmpty()) "${stringResource(R.string.upstream_nightly)} (${nightlies.size})"
                        else stringResource(R.string.upstream_nightly)
                    )
                }
            )
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                fetchData()
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { fetchData() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                selectedTab == 0 -> {
                    if (releases.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.upstream_no_updates),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(releases, key = { it.tagName }) { rel ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = rel.tagName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = rel.publishedAt,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (rel.name.isNotBlank() && rel.name != rel.tagName) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = rel.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        if (rel.body.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = rel.body.trim(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 15,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    if (nightlies.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.upstream_no_updates),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(nightlies, key = { it.runNumber }) { run ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Nightly #${run.runNumber}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = run.createdAt,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = run.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = run.branch,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = run.commitSha,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
