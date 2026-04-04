package com.music.vivi.vivimusic.changelog



import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.vivi.BuildConfig
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.vivimusic.updater.extractUrls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChangelogScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    versionTag: String = "v${BuildConfig.VERSION_NAME}"
) {
    val context = LocalContext.current
    var changelogSections by remember { mutableStateOf<List<ChangelogSection>>(emptyList()) }
    var updateImage by remember { mutableStateOf<String?>(null) }
    var updateDescription by remember { mutableStateOf<String?>(null) }
    var updateWarning by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var showingCached by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var currentVersionTag by remember { mutableStateOf(versionTag) }
    var availableReleases by remember { mutableStateOf<List<ReleaseMetadata>>(
        listOf(ReleaseMetadata(versionTag, versionTag, context.getString(R.string.current), null))
    ) }
    var isFetchingOldReleases by remember { mutableStateOf(false) }

    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = isLoading || isFetchingOldReleases

    val scaleFraction = {
        if (isRefreshing) 1f
        else LinearOutSlowInEasing.transform(pullToRefreshState.distanceFraction).coerceIn(0f, 1f)
    }

    fun fetchChangelog(tag: String) {
        isLoading = true
        hasError = false
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val cachedData = loadChangelogFromCache(context, tag)
                if (cachedData != null) {
                    withContext(Dispatchers.Main) {
                        changelogSections = cachedData.sections
                        updateImage = cachedData.image
                        updateDescription = cachedData.description
                        updateWarning = cachedData.warning
                        isLoading = false
                        showingCached = true
                    }
                } else {
                    val changelogUrl = URL("https://github.com/vivizzz007/vivi-music/releases/download/$tag/changelog.json")
                    val connection = changelogUrl.openConnection() as HttpURLConnection
                    connection.setRequestProperty("User-Agent", "ViviMusic-Changelog-App")
                    connection.setRequestProperty("Accept", "application/json")
                    
                    if (connection.responseCode == 200) {
                        val changelogJson = connection.inputStream.bufferedReader().use { it.readText() }
                        val changelogData = JSONObject(changelogJson)
                        
                        val desc = changelogData.optString("description", null)
                        val imageUrl = changelogData.optString("image", null)
                        val warning = changelogData.optString("warning", null)
                        val changelogArray = changelogData.optJSONArray("changelog")
                        
                        val sections = mutableListOf<ChangelogSection>()
                        if (changelogArray != null) {
                            for (i in 0 until changelogArray.length()) {
                                val sectionObj = changelogArray.optJSONObject(i)
                                if (sectionObj != null) {
                                    val title = sectionObj.optString("title", "")
                                    val itemsArray = sectionObj.optJSONArray("items")
                                    val items = mutableListOf<String>()
                                    if (itemsArray != null) {
                                        for (j in 0 until itemsArray.length()) {
                                            items.add(itemsArray.getString(j))
                                        }
                                    }
                                    if (title.isNotBlank() || items.isNotEmpty()) {
                                        sections.add(ChangelogSection(title, items))
                                    }
                                } else {
                                    // Fallback: This is the old format (Array of Strings)
                                    val item = changelogArray.optString(i, "")
                                    if (item.isNotBlank()) {
                                        if (sections.isEmpty() || sections[0].title.isNotBlank()) {
                                            sections.add(0, ChangelogSection("", mutableListOf()))
                                        }
                                        (sections[0].items as MutableList<String>).add(item)
                                    }
                                }
                            }
                        }
                        
                        saveChangelogToCache(context, tag, sections, imageUrl, desc, warning)
                        withContext(Dispatchers.Main) {
                            changelogSections = sections
                            updateImage = imageUrl.takeIf { !it.isNullOrBlank() }
                            updateDescription = desc.takeIf { !it.isNullOrBlank() }
                            updateWarning = warning.takeIf { !it.isNullOrBlank() }
                            isLoading = false
                            hasError = false
                            showingCached = false
                        }
                    } else {
                        Log.e("ChangelogScreen", "HTTP Error ${connection.responseCode} for $tag")
                        withContext(Dispatchers.Main) { hasError = true; isLoading = false }
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
        if (isFetchingOldReleases) return
        isFetchingOldReleases = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val releasesUrl = URL("https://api.github.com/repos/vivizzz007/vivi-music/releases")
                val connection = releasesUrl.openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "ViviMusic-Changelog-App")
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                
                if (connection.responseCode == 200) {
                    val json = connection.inputStream.bufferedReader().use { it.readText() }
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
                    } catch (e: Exception) { publishedAt }

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
                        list.add(ReleaseMetadata(tagName, name, formattedDate, null))
                    }
                }
                    withContext(Dispatchers.Main) {
                        val currentVersion = ReleaseMetadata(versionTag, versionTag, context.getString(R.string.current), null)
                        availableReleases = (listOf(currentVersion) + list).distinctBy { it.tagName }
                        isFetchingOldReleases = false
                    }
                } else {
                    Log.e("ChangelogScreen", "GitHub API Error ${connection.responseCode}")
                    withContext(Dispatchers.Main) { isFetchingOldReleases = false }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isFetchingOldReleases = false }
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchOldReleases()
    }

    LaunchedEffect(currentVersionTag) {
        cleanupOldChangelogCache(context, currentVersionTag)
        fetchChangelog(currentVersionTag)
    }

    Scaffold(
        modifier = Modifier.pullToRefresh(
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = { fetchChangelog(currentVersionTag) }
        ),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.changelog_title)) },
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
                // Version Selection Chips
                if (availableReleases.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            availableReleases.forEachIndexed { index, release ->
                                ToggleButton(
                                    checked = currentVersionTag == release.tagName,
                                    onCheckedChange = {
                                        if (currentVersionTag != release.tagName) {
                                            currentVersionTag = release.tagName
                                        }
                                    },
                                    shapes = when {
                                        availableReleases.size == 1 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        index == availableReleases.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    },
                                    modifier = Modifier.semantics { role = Role.RadioButton }
                                ) {
                                    Text(
                                        text = release.tagName,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                        if (isFetchingOldReleases) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

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
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (isLoading && availableReleases.isEmpty()) {
                            // Show nothing or a small loader while initial releases are fetching
                        } else {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentVersionTag,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (showingCached) {
                                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                                            Text(stringResource(R.string.cached), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                }
                                
                                updateImage?.let { imageUrl ->
                                    Spacer(modifier = Modifier.height(16.dp))
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.FillWidth
                                    )
                                }

                                updateDescription?.let { desc ->
                                    Spacer(Modifier.height(16.dp))
                                    Text(desc, style = MaterialTheme.typography.bodyLarge)
                                }

                                if (changelogSections.isNotEmpty()) {
                                    changelogSections.forEach { section ->
                                        if (section.title.isNotBlank()) {
                                            Spacer(Modifier.height(16.dp))
                                            Text(
                                                text = section.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.height(8.dp))
                                        } else {
                                            Spacer(Modifier.height(16.dp))
                                        }
                                        
                                        section.items.forEach { item ->
                                            val urls = item.extractUrls()
                                            val annotatedText = buildAnnotatedString {
                                                append(item.trim())
                                                urls.forEach { (range, url) ->
                                                    addStringAnnotation("URL", url, range.first, range.last + 1)
                                                    addStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline), range.first, range.last + 1)
                                                }
                                            }
                                            Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Box(modifier = Modifier.padding(top = 8.dp).size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                                ClickableText(
                                                    text = annotatedText,
                                                    onClick = { offset ->
                                                        annotatedText.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                                                            ContextCompat.startActivity(context, Intent(Intent.ACTION_VIEW, Uri.parse(it.item)), null)
                                                        }
                                                    },
                                                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                                                )
                                            }
                                        }
                                    }
                                }

                                updateWarning?.let { warning ->
                                    Spacer(Modifier.height(24.dp))
                                    Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                            Text(warning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                        }
                                    }
                                }
                                
                                Spacer(Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }

            // The Loading Indicator at the top center
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        scaleX = scaleFraction()
                        scaleY = scaleFraction()
                    }
            ) {
                PullToRefreshDefaults.LoadingIndicator(state = pullToRefreshState, isRefreshing = isRefreshing)
            }
        }
    }
}

data class ChangelogSection(val title: String, val items: List<String>)
data class ReleaseMetadata(val tagName: String, val name: String, val date: String, val imageUrl: String?)
data class CachedChangelogData(val sections: List<ChangelogSection>, val image: String?, val description: String?, val warning: String?)

private fun cleanupOldChangelogCache(context: Context, currentVersionTag: String) {
    try {
        context.filesDir.listFiles { file -> file.name.startsWith("changelog_cache_") && file.name.endsWith(".json") }?.forEach { file ->
            if (file.name != "changelog_cache_$currentVersionTag.json") file.delete()
        }
    } catch (e: Exception) { Log.e("ChangelogCache", "Error cleaning up cache", e) }
}

private fun saveChangelogToCache(context: Context, versionTag: String, sections: List<ChangelogSection>, image: String?, description: String?, warning: String?) {
    try {
        val cacheData = JSONObject().apply {
            val sectionsArray = JSONArray()
            sections.forEach { section ->
                val sectionObj = JSONObject().apply {
                    put("title", section.title)
                    val itemsArray = JSONArray()
                    section.items.forEach { itemsArray.put(it) }
                    put("items", itemsArray)
                }
                sectionsArray.put(sectionObj)
            }
            put("sections", sectionsArray)
            put("image", image ?: "")
            put("description", description ?: "")
            put("warning", warning ?: "")
        }
        context.openFileOutput("changelog_cache_$versionTag.json", Context.MODE_PRIVATE).use { it.write(cacheData.toString().toByteArray()) }
    } catch (e: Exception) { Log.e("ChangelogCache", "Error saving cache", e) }
}

private fun loadChangelogFromCache(context: Context, versionTag: String): CachedChangelogData? {
    return try {
        val cacheFile = File(context.filesDir, "changelog_cache_$versionTag.json")
        if (!cacheFile.exists()) return null
        val cacheData = JSONObject(context.openFileInput("changelog_cache_$versionTag.json").use { it.bufferedReader().readText() })
        
        val sectionsArray = cacheData.optJSONArray("sections")
        val sections = mutableListOf<ChangelogSection>()
        if (sectionsArray != null) {
            for (i in 0 until sectionsArray.length()) {
                val sectionObj = sectionsArray.getJSONObject(i)
                val title = sectionObj.getString("title")
                val itemsArray = sectionObj.getJSONArray("items")
                val items = mutableListOf<String>()
                for (j in 0 until itemsArray.length()) {
                    items.add(itemsArray.getString(j))
                }
                sections.add(ChangelogSection(title, items))
            }
        }
        
        CachedChangelogData(
            sections = sections,
            image = cacheData.optString("image", null).takeIf { !it.isNullOrBlank() },
            description = cacheData.optString("description", null).takeIf { !it.isNullOrBlank() },
            warning = cacheData.optString("warning", null).takeIf { !it.isNullOrBlank() }
        )
    } catch (e: Exception) { null }
}