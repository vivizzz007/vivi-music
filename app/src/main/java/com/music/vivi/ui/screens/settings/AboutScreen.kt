package com.music.vivi.ui.screens.settings

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.music.vivi.BuildConfig
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.utils.backToMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun shimmerEffect(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "shimmerEffect")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "shimmerEffect"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
}

@Composable
fun UserCard(
    imageUrl: String,
    name: String,
    role: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .height(140.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = imageUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = role,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp)
                    .offset(x = 20.dp, y = (-20).dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        CircleShape
                    )
            )
        }
    }
}

@Composable
fun HyperOSListItem(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                MaterialTheme.colorScheme.surfaceColorAtElevation(NavigationBarDefaults.Elevation),
                shape = MaterialTheme.shapes.medium
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp
                )
            )
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = "Go to details",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val uriHandler = LocalUriHandler.current
    var latestRelease by remember { mutableStateOf<String?>(null) }
    val isUpdateAvailable = remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    // Changelog state
    var showChangelog by remember { mutableStateOf(false) }
    val changelogViewModel: ChangelogViewModel = viewModel()

    LaunchedEffect(true) {
        // Load latest release info
        withContext(Dispatchers.IO) {
            try {
                val apiUrl = "https://api.github.com/repos/vivizzz007/vivi-music/releases/latest"
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    val fetchedLatestRelease = jsonObject.getString("tag_name").removePrefix("v")
                    latestRelease = fetchedLatestRelease

                    if (fetchedLatestRelease.isNotEmpty() && BuildConfig.VERSION_NAME.isNotEmpty()) {
                        isUpdateAvailable.value = fetchedLatestRelease > BuildConfig.VERSION_NAME
                    }
                    inputStream.close()
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Load changelog
        changelogViewModel.loadChangelog("vivizzz007", "vivi-music")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp, bottom = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(170.dp))
                Text(
                    text = "VIVI MUSIC",
                    style = TextStyle(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(modifier = Modifier.height(25.dp))
                Text(
                    text = BuildConfig.VERSION_NAME,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                )
                Spacer(modifier = Modifier.height(100.dp))

                if (isUpdateAvailable.value) {
                    Button(
                        onClick = { navController.navigate("settings/update") },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(60.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text(
                            text = "Update Now",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))

            // List Items
            HyperOSListItem(
                title = "Developer",
                value = "VIVIDH P ASHOKAN",
                onClick = { uriHandler.openUri("https://github.com/vivizzz007") }
            )
            HyperOSListItem(
                title = "Build Version",
                value = "V-2.0.0",
                onClick = { uriHandler.openUri("https://github.com/vivizzz007/vivi-music/releases") }
            )
            HyperOSListItem(
                title = "Android",
                value = "${Build.VERSION.RELEASE}",
                onClick = {}
            )
            HyperOSListItem(
                title = "Updated",
                value = "2025-5-18",
                onClick = { showChangelog = true }
            )
            HyperOSListItem(
                title = "Website",
                value = "VIVI-MUSIC",
                onClick = { uriHandler.openUri("https://vivi-music-web-com.vercel.app/") }
            )
            HyperOSListItem(
                title = "Github",
                value = "VIVI-MUSIC",
                onClick = { uriHandler.openUri("https://github.com/vivizzz007/vivi-music") }
            )
        }

        // Changelog Bottom Sheet
        if (showChangelog) {
            ModalBottomSheet(
                onDismissRequest = { showChangelog = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Changelog",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )

                    AutoChangelogCard(
                        viewModel = changelogViewModel,
                        repoOwner = "vivizzz007",
                        repoName = "vivi-music"
                    )

                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        TopAppBar(
            title = { Text(stringResource(R.string.about)) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain
                ) {
                    Icon(
                        painterResource(R.drawable.back_icon),
                        contentDescription = null
                    )
                }
            },
            scrollBehavior = scrollBehavior,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

// Changelog ViewModel
class ChangelogViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChangelogState())
    val uiState: StateFlow<ChangelogState> = _uiState.asStateFlow()

    fun loadChangelog(repoOwner: String, repoName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val markdownContent = fetchReleaseMarkdown(repoOwner, repoName)
                _uiState.update {
                    it.copy(
                        changes = markdownContent,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error loading changelog: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun fetchReleaseMarkdown(owner: String, repo: String): String {
        return withContext(Dispatchers.IO) {
            val url = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP error: ${connection.responseCode}") as Throwable
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(response)
            jsonObject.optString("body", "")
        }
    }
}

// Changelog State
data class ChangelogState(
    val changes: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

// Changelog Card
@Composable
fun AutoChangelogCard(
    viewModel: ChangelogViewModel,
    repoOwner: String,
    repoName: String
) {
    val uiState by viewModel.uiState.collectAsState()

    ElevatedCard(
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                uiState.changes.isEmpty() -> {
                    Text("No changelog available")
                }

                else -> {
                    MarkdownText(
                        markdown = uiState.changes,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// Markdown Text Renderer
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val lines = markdown.lines()

    Column(modifier = modifier) {
        for (line in lines) {
            val trimmedLine = line.trim()

            when {
                trimmedLine.startsWith("# ") -> {
                    Text(
                        text = trimmedLine.substring(2),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                trimmedLine.startsWith("## ") -> {
                    Text(
                        text = trimmedLine.substring(3),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("•", modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = trimmedLine.substring(2),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                trimmedLine.isNotEmpty() -> {
                    Text(
                        text = trimmedLine,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}