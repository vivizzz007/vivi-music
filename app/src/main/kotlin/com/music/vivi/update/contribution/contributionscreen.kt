package com.music.vivi.update.contribution

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.vivi.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class Contributor(
    val name: String,
    val avatarUrl: String,
    val githubUsername: String
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberLazyListState()

    var contributors by remember { mutableStateOf<List<Contributor>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }

    // Fetch contributors from JSON with smooth progress animation
    LaunchedEffect(Unit) {
        // Start progress animation
        val animationJob = async {
            val animationDuration = 2000L // 2 seconds smooth animation
            val steps = 100
            val delayPerStep = animationDuration / steps

            repeat(steps) { step ->
                loadingProgress = (step + 1).toFloat() / steps
                kotlinx.coroutines.delay(delayPerStep)
            }
        }

        // Fetch data
        val fetchJob = async(Dispatchers.IO) {
            try {
                val apiUrl = "https://raw.githubusercontent.com/vivizzz007/vivi-music/main/NEW-UI/contribution/contribution.json"
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)

                    val contributorsList = mutableListOf<Contributor>()
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val contributor = Contributor(
                            name = jsonObject.getString("name"),
                            avatarUrl = jsonObject.getString("avatarUrl"),
                            githubUsername = jsonObject.getString("githubUsername")
                        )
                        contributorsList.add(contributor)
                    }
                    contributors = contributorsList
                    inputStream.close()
                } else {
                    error = "Failed to load contributors (${connection.responseCode})"
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                error = "Error loading contributors: ${e.message}"
            }
        }

        // Wait for both animation and fetch to complete
        animationJob.await()
        fetchJob.await()

        // Brief pause at 100%
        kotlinx.coroutines.delay(300)
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { },
                        navigationIcon = {
                            IconButton(
                                onClick = navController::navigateUp
                            ) {
                                Icon(
                                    painterResource(R.drawable.arrow_back),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )
                    if (isLoading) {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { loadingProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 32.dp
                )
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                    ) {
                        Text(
                            text = "Contributors",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Amazing people who made this project better",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                when {
                    error != null -> {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = error ?: "Unknown error",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    contributors.isNotEmpty() -> {
                        item {
                            ContributorGroupSection(
                                contributors = contributors,
                                onGitHubClick = { username ->
                                    uriHandler.openUri("https://github.com/$username")
                                }
                            )
                        }
                    }

                    else -> {
                        item {
                            Text(
                                text = "No contributors found",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun ContributorGroupSection(
    contributors: List<Contributor>,
    onGitHubClick: (String) -> Unit
) {
    val cornerRadius = 16.dp
    val connectionRadius = 5.dp

    val topShape = RoundedCornerShape(
        topStart = cornerRadius,
        topEnd = cornerRadius,
        bottomStart = connectionRadius,
        bottomEnd = connectionRadius
    )
    val middleShape = RoundedCornerShape(connectionRadius)
    val bottomShape = RoundedCornerShape(
        topStart = connectionRadius,
        topEnd = connectionRadius,
        bottomStart = cornerRadius,
        bottomEnd = cornerRadius
    )
    val singleShape = RoundedCornerShape(cornerRadius)

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(
            modifier = Modifier.clip(
                if (contributors.size == 1) singleShape else RoundedCornerShape(cornerRadius)
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            contributors.forEachIndexed { index, contributor ->
                val shape = when {
                    contributors.size == 1 -> singleShape
                    index == 0 -> topShape
                    index == contributors.size - 1 -> bottomShape
                    else -> middleShape
                }

                Column(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer, shape)
                ) {
                    ContributorCard(
                        contributor = contributor,
                        onGitHubClick = { onGitHubClick(contributor.githubUsername) }
                    )
                }
            }
        }
    }
}

@Composable
fun ContributorCard(
    contributor: Contributor,
    onGitHubClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar
        AsyncImage(
            model = contributor.avatarUrl,
            contentDescription = contributor.name,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentScale = ContentScale.Crop
        )

        // Name Column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = contributor.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Add GitHub username here
            Text(
                text = "@${contributor.githubUsername}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        // GitHub Button
        IconButton(
            onClick = onGitHubClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.github),
                contentDescription = "GitHub Profile",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}