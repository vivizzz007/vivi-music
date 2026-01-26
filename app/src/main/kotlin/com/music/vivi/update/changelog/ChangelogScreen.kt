@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.music.vivi.update.changelog

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.vivi.BuildConfig
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.ui.screens.extractUrls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// -----------------------------------------------------------------------------
// 2. Public Entry Point
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    versionTag: String = "v${BuildConfig.VERSION_NAME}",
) {
    val context = LocalContext.current
    var changelog by remember { mutableStateOf("") }
    var updateImage by remember { mutableStateOf<String?>(null) }
    var updateDescription by remember { mutableStateOf<String?>(null) }
    var updateWarning by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var showingCached by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var currentVersionTag by remember { mutableStateOf(versionTag) }
    var selectedIndex by remember { mutableStateOf(0) }
    var oldReleasesMetadata by remember { mutableStateOf<List<ReleaseMetadata>>(emptyList()) }
    var isFetchingOldReleases by remember { mutableStateOf(false) }

    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = isLoading || isFetchingOldReleases

    // --- Logic Logic (Kept internal for SFC Pilot) ---
    fun fetchChangelog(tag: String) {
        isLoading = true
        hasError = false
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val cachedData = loadChangelogFromCache(context, tag)
                if (cachedData != null) {
                    withContext(Dispatchers.Main) {
                        changelog = cachedData.changelog
                        updateImage = cachedData.image
                        updateDescription = cachedData.description
                        updateWarning = cachedData.warning
                        isLoading = false
                        showingCached = true
                    }
                } else {
                    val changelogUrl =
                        URL("https://github.com/vivizzz007/vivi-music/releases/download/$tag/changelog.json")
                    val changelogJson = changelogUrl.openStream().bufferedReader().use { it.readText() }
                    val changelogData = JSONObject(changelogJson)

                    val desc = changelogData.optString("description")
                    val imageUrl = changelogData.optString("image")
                    val changelogArray = changelogData.getJSONArray("changelog")
                    val changelogText = buildString {
                        for (i in 0 until changelogArray.length()) {
                            appendLine(changelogArray.getString(i))
                        }
                    }.trim()
                    val warning = changelogData.optString("warning")

                    saveChangelogToCache(context, tag, changelogText, imageUrl, desc, warning)

                    withContext(Dispatchers.Main) {
                        changelog = changelogText
                        updateImage = imageUrl.takeIf { !it.isNullOrBlank() }
                        updateDescription = desc.takeIf { !it.isNullOrBlank() }
                        updateWarning = warning.takeIf { !it.isNullOrBlank() }
                        isLoading = false
                        hasError = false
                        showingCached = false
                    }
                }
            } catch (e: Exception) {
                Log.e("ChangelogScreen", "Error fetching changelog: ${e.message}")
                withContext(Dispatchers.Main) {
                    hasError = true
                    isLoading = false
                }
            }
        }
    }

    fun fetchOldReleases() {
        if (oldReleasesMetadata.isNotEmpty() && !isRefreshing) return
        isFetchingOldReleases = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val releasesUrl = URL("https://api.github.com/repos/vivizzz007/vivi-music/releases")
                val json = releasesUrl.openStream().bufferedReader().use { it.readText() }
                val array = JSONArray(json)
                val list = mutableListOf<ReleaseMetadata>()
                val outputFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val tagName = obj.getString("tag_name")
                    if (!tagName.startsWith("v", ignoreCase = true)) continue

                    val name = obj.optString("name", tagName)
                    val publishedAt = obj.getString("published_at")
                    val formattedDate = try {
                        ZonedDateTime.parse(publishedAt).format(outputFormatter)
                    } catch (e: Exception) {
                        publishedAt
                    }

                    val assets = obj.getJSONArray("assets")
                    var changelogUrl: String? = null
                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        if (asset.getString("name") == "changelog.json") {
                            changelogUrl = asset.getString("browser_download_url")
                            break
                        }
                    }

                    if (changelogUrl != null) {
                        try {
                            val changelogJson = URL(changelogUrl).openStream().bufferedReader().use { it.readText() }
                            val changelogData = JSONObject(changelogJson)
                            list.add(ReleaseMetadata(tagName, name, formattedDate, changelogData.optString("image")))
                        } catch (e: Exception) {
                            list.add(ReleaseMetadata(tagName, name, formattedDate, null))
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    oldReleasesMetadata = list
                    isFetchingOldReleases = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isFetchingOldReleases = false }
            }
        }
    }

    LaunchedEffect(currentVersionTag) {
        cleanupOldChangelogCache(context, currentVersionTag)
        fetchChangelog(currentVersionTag)
    }

    // --- UI Structure ---
    Scaffold(
        modifier = Modifier.pullToRefresh(
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = {
                if (selectedIndex == 0) fetchChangelog(currentVersionTag) else fetchOldReleases()
            }
        ),
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(painterResource(R.drawable.arrow_back), null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ChangelogTypeSelector(
                    selectedIndex = selectedIndex,
                    onOptionSelected = { index ->
                        selectedIndex = index
                        if (index == 0) currentVersionTag = versionTag else fetchOldReleases()
                    }
                )

                if (selectedIndex == 0) {
                    CurrentChangelogView(
                        hasError = hasError,
                        isLoading = isLoading,
                        showingCached = showingCached,
                        versionTag = currentVersionTag,
                        updateImage = updateImage,
                        updateDescription = updateDescription,
                        updateWarning = updateWarning,
                        changelog = changelog,
                        onUrlClick = { url ->
                            ContextCompat.startActivity(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)), null)
                        }
                    )
                } else {
                    OldReleasesView(
                        oldReleases = oldReleasesMetadata,
                        isFetching = isFetchingOldReleases,
                        onReleaseClick = { tag ->
                            currentVersionTag = tag
                            selectedIndex = 0
                        }
                    )
                }
            }

            // Loading Indicator
            PullIndicator(
                modifier = Modifier.align(Alignment.TopCenter),
                state = pullToRefreshState,
                isRefreshing = isRefreshing
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 4. Private Sub-components
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChangelogTypeSelector(selectedIndex: Int, onOptionSelected: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val context = LocalContext.current
        val options = listOf(context.getString(R.string.current), context.getString(R.string.old_releases))
        options.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                onClick = { onOptionSelected(index) },
                selected = index == selectedIndex
            ) { Text(label) }
        }
    }
}

@Composable
private fun CurrentChangelogView(
    hasError: Boolean,
    isLoading: Boolean,
    showingCached: Boolean,
    versionTag: String,
    updateImage: String?,
    updateDescription: String?,
    updateWarning: String?,
    changelog: String,
    onUrlClick: (String) -> Unit,
) {
    if (hasError && !isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.error_loading_changelog), color = MaterialTheme.colorScheme.error)
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            stringResource(R.string.changelog_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            versionTag,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (showingCached) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                stringResource(R.string.cached),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                updateImage?.let { imageUrl ->
                    Spacer(modifier = Modifier.height(16.dp))
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                updateDescription?.let { desc ->
                    Text(desc, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                }
                if (changelog.isNotEmpty()) {
                    changelog.split("\n").filter { it.isNotBlank() }.forEach { item ->
                        ChangelogItem(item, onUrlClick)
                    }
                }
                updateWarning?.let { warning ->
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                Icons.Default.Error,
                                null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                warning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangelogItem(item: String, onUrlClick: (String) -> Unit) {
    val urls = item.extractUrls()
    val annotatedText = buildAnnotatedString {
        append(item.trim())
        urls.forEach { (range, url) ->
            addStringAnnotation("URL", url, range.first, range.last + 1)
            addStyle(
                SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline),
                range.first,
                range.last + 1
            )
        }
    }
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.padding(
                top = 8.dp
            ).size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        @Suppress("DEPRECATION")
        ClickableText(
            text = annotatedText,
            onClick = { offset ->
                annotatedText.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                    onUrlClick(it.item)
                }
            },
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
        )
    }
}

@Composable
private fun OldReleasesView(oldReleases: List<ReleaseMetadata>, isFetching: Boolean, onReleaseClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (oldReleases.isEmpty() && !isFetching) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.no_old_releases_with_changelog),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            oldReleases.forEach { release ->
                ReleaseCard(release, onReleaseClick)
            }
        }
    }
}

@Composable
private fun ReleaseCard(release: ReleaseMetadata, onClick: (String) -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick(release.tagName) },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            release.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(
                        180.dp
                    ).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = release.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = release.tagName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = release.date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PullIndicator(
    modifier: Modifier = Modifier,
    state: androidx.compose.material3.pulltorefresh.PullToRefreshState,
    isRefreshing: Boolean,
) {
    val scaleFraction = {
        if (isRefreshing) {
            1f
        } else {
            LinearOutSlowInEasing.transform(state.distanceFraction).coerceIn(0f, 1f)
        }
    }
    Box(
        modifier.graphicsLayer {
            scaleX = scaleFraction()
            scaleY = scaleFraction()
        }
    ) {
        PullToRefreshDefaults.LoadingIndicator(state = state, isRefreshing = isRefreshing)
    }
}

// -----------------------------------------------------------------------------
// 5. Logic Helpers (Data Classes, Cache)
// -----------------------------------------------------------------------------

data class ReleaseMetadata(val tagName: String, val name: String, val date: String, val imageUrl: String?)
data class CachedChangelogData(
    val changelog: String,
    val image: String?,
    val description: String?,
    val warning: String?,
)

private fun cleanupOldChangelogCache(context: Context, currentVersionTag: String) {
    try {
        context.filesDir.listFiles { file ->
            file.name.startsWith("changelog_cache_") && file.name.endsWith(".json")
        }?.forEach { file ->
            if (file.name != "changelog_cache_$currentVersionTag.json") file.delete()
        }
    } catch (e: Exception) {
        Log.e("ChangelogCache", "Error cleaning up cache", e)
    }
}

private fun saveChangelogToCache(
    context: Context,
    versionTag: String,
    changelog: String,
    image: String?,
    description: String?,
    warning: String?,
) {
    try {
        val cacheData = JSONObject().apply {
            put("changelog", changelog)
            put("image", image ?: "")
            put("description", description ?: "")
            put("warning", warning ?: "")
        }
        context.openFileOutput("changelog_cache_$versionTag.json", Context.MODE_PRIVATE).use {
            it.write(cacheData.toString().toByteArray())
        }
    } catch (e: Exception) {
        Log.e("ChangelogCache", "Error saving cache", e)
    }
}

private fun loadChangelogFromCache(context: Context, versionTag: String): CachedChangelogData? {
    return try {
        val cacheFile = File(context.filesDir, "changelog_cache_$versionTag.json")
        if (!cacheFile.exists()) return null
        val cacheData =
            JSONObject(context.openFileInput("changelog_cache_$versionTag.json").use { it.bufferedReader().readText() })
        CachedChangelogData(
            changelog = cacheData.getString("changelog"),
            image = cacheData.optString("image").takeIf { !it.isNullOrBlank() },
            description = cacheData.optString("description").takeIf { !it.isNullOrBlank() },
            warning = cacheData.optString("warning").takeIf { !it.isNullOrBlank() }
        )
    } catch (e: Exception) {
        null
    }
}
