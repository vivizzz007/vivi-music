package com.music.vivi.changelog



import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import org.json.JSONObject
import java.io.File
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    versionTag: String = "v${BuildConfig.VERSION_NAME}"
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

    LaunchedEffect(Unit) {
        // Clean up old changelog cache files
        cleanupOldChangelogCache(context, versionTag)

        // First, try to load from cache
        val cachedData = loadChangelogFromCache(context, versionTag)
        if (cachedData != null) {
            changelog = cachedData.changelog
            updateImage = cachedData.image
            updateDescription = cachedData.description
            updateWarning = cachedData.warning
            isLoading = false
            showingCached = true
            Log.d("ChangelogScreen", "Loaded changelog from cache for $versionTag")
        } else {
            // If not cached, fetch from network
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val changelogUrl = URL("https://github.com/vivizzz007/vivi-music/releases/download/$versionTag/changelog.json")
                    Log.d("ChangelogScreen", "Fetching changelog from: $changelogUrl")
                    val changelogJson = changelogUrl.openStream().bufferedReader().use { it.readText() }
                    val changelogData = JSONObject(changelogJson)

                    // Get description (optional)
                    val desc = changelogData.optString("description", null)

                    // Get image URL (optional)
                    val imageUrl = changelogData.optString("image", null)

                    // Get changelog items
                    val changelogArray = changelogData.getJSONArray("changelog")
                    val changelogText = buildString {
                        for (i in 0 until changelogArray.length()) {
                            appendLine(changelogArray.getString(i))
                        }
                    }.trim()

                    // Get warning (optional)
                    val warning = changelogData.optString("warning", null)

                    // Save to cache
                    saveChangelogToCache(
                        context = context,
                        versionTag = versionTag,
                        changelog = changelogText,
                        image = imageUrl,
                        description = desc,
                        warning = warning
                    )

                    withContext(Dispatchers.Main) {
                        changelog = changelogText
                        updateImage = if (!imageUrl.isNullOrBlank()) imageUrl else null
                        updateDescription = if (!desc.isNullOrBlank()) desc else null
                        updateWarning = if (!warning.isNullOrBlank()) warning else null
                        isLoading = false
                        hasError = false
                    }
                } catch (e: Exception) {
                    Log.e("ChangelogScreen", "Failed to fetch changelog.json: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        hasError = true
                        isLoading = false
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("")
                        },
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
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current
                        .only(WindowInsetsSides.Bottom)
                )
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

                hasError -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Error loading changelog",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Check your internet connection",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Changelog heading with image (NO BACKGROUND)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
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

                                // Show cached indicator
                                if (showingCached) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Cached",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Display image from changelog.json
                            updateImage?.let { imageUrl ->
                                Spacer(modifier = Modifier.height(16.dp))
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Update preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        }

                        // Changelog content
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp)
                        ) {
                            // Description (if available)
                            updateDescription?.let { desc ->
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 24.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Changelog items
                            if (changelog.isNotEmpty()) {
                                val changelogItems = changelog.split("\n").filter { it.isNotBlank() }
                                changelogItems.forEach { item ->
                                    val urls = item.extractUrls()
                                    val annotatedText = buildAnnotatedString {
                                        append(item.trim())

                                        // Add URL annotations
                                        urls.forEach { (range, url) ->
                                            addStringAnnotation(
                                                tag = "URL",
                                                annotation = url,
                                                start = range.first,
                                                end = range.last + 1
                                            )
                                            addStyle(
                                                style = SpanStyle(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textDecoration = TextDecoration.Underline
                                                ),
                                                start = range.first,
                                                end = range.last + 1
                                            )
                                        }
                                    }

                                    // Bullet point with clickable text
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 6.dp)
                                                .size(8.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary,
                                                    CircleShape
                                                )
                                        )
                                        ClickableText(
                                            text = annotatedText,
                                            onClick = { offset ->
                                                annotatedText.getStringAnnotations("URL", offset, offset)
                                                    .firstOrNull()?.let { annotation ->
                                                        val intent = Intent(
                                                            Intent.ACTION_VIEW,
                                                            Uri.parse(annotation.item)
                                                        )
                                                        ContextCompat.startActivity(context, intent, null)
                                                    }
                                            },
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 24.sp
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "No changelog available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Warning (if available)
                            updateWarning?.let { warning ->
                                Spacer(modifier = Modifier.height(16.dp))

                                val warningUrls = warning.extractUrls()
                                val warningAnnotatedText = buildAnnotatedString {
                                    append(warning)

                                    // Add URL annotations for warning
                                    warningUrls.forEach { (range, url) ->
                                        addStringAnnotation(
                                            tag = "URL",
                                            annotation = url,
                                            start = range.first,
                                            end = range.last + 1
                                        )
                                        addStyle(
                                            style = SpanStyle(
                                                color = MaterialTheme.colorScheme.primary,
                                                textDecoration = TextDecoration.Underline
                                            ),
                                            start = range.first,
                                            end = range.last + 1
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(20.dp)
                                        )
                                        ClickableText(
                                            text = warningAnnotatedText,
                                            onClick = { offset ->
                                                warningAnnotatedText.getStringAnnotations("URL", offset, offset)
                                                    .firstOrNull()?.let { annotation ->
                                                        val intent = Intent(
                                                            Intent.ACTION_VIEW,
                                                            Uri.parse(annotation.item)
                                                        )
                                                        ContextCompat.startActivity(context, intent, null)
                                                    }
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                lineHeight = 20.sp
                                            ),
                                            modifier = Modifier.weight(1f)
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

// Data class to hold cached changelog data
data class CachedChangelogData(
    val changelog: String,
    val image: String?,
    val description: String?,
    val warning: String?
)

// Clean up old changelog cache files (keep only current version)
private fun cleanupOldChangelogCache(context: Context, currentVersionTag: String) {
    try {
        val filesDir = context.filesDir
        val cacheFiles = filesDir.listFiles { file ->
            file.name.startsWith("changelog_cache_") && file.name.endsWith(".json")
        }

        cacheFiles?.forEach { file ->
            // Delete if it's not the current version's cache
            if (file.name != "changelog_cache_$currentVersionTag.json") {
                val deleted = file.delete()
                if (deleted) {
                    Log.d("ChangelogCache", "Deleted old cache file: ${file.name}")
                }
            }
        }
    } catch (e: Exception) {
        Log.e("ChangelogCache", "Error cleaning up old changelog cache", e)
    }
}

// Save changelog to cache
private fun saveChangelogToCache(
    context: Context,
    versionTag: String,
    changelog: String,
    image: String?,
    description: String?,
    warning: String?
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
        Log.d("ChangelogCache", "Saved changelog cache for $versionTag")
    } catch (e: Exception) {
        Log.e("ChangelogCache", "Error saving changelog cache", e)
    }
}

// Load changelog from cache
private fun loadChangelogFromCache(context: Context, versionTag: String): CachedChangelogData? {
    return try {
        val cacheFile = File(context.filesDir, "changelog_cache_$versionTag.json")
        if (!cacheFile.exists()) {
            Log.d("ChangelogCache", "No cache found for $versionTag")
            return null
        }

        val cacheContent = context.openFileInput("changelog_cache_$versionTag.json").use {
            it.bufferedReader().readText()
        }

        val cacheData = JSONObject(cacheContent)
        CachedChangelogData(
            changelog = cacheData.getString("changelog"),
            image = cacheData.optString("image", null).takeIf { !it.isNullOrBlank() },
            description = cacheData.optString("description", null).takeIf { !it.isNullOrBlank() },
            warning = cacheData.optString("warning", null).takeIf { !it.isNullOrBlank() }
        )
    } catch (e: Exception) {
        Log.e("ChangelogCache", "Error loading changelog cache", e)
        null
    }
}