package com.music.vivi.update.contribution

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.update.settingstyle.Materialcard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class Contributor(val name: String, val avatarUrl: String, val githubUsername: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContributionScreen(navController: NavController, scrollBehavior: TopAppBarScrollBehavior) {
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current

    var contributors by remember { mutableStateOf<List<Contributor>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }

    // Fetch function
    suspend fun fetchContributors() {
        withContext(Dispatchers.IO) {
            try {
                val apiUrl = "https://raw.githubusercontent.com/vivizzz007/vivi-music/main/NEW-UI/contribution/contribution.json"
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("Cache-Control", "no-cache")
                    useCaches = false
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)

                    val contributorsList = mutableListOf<Contributor>()
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        contributorsList.add(
                            Contributor(
                                name = jsonObject.getString("name"),
                                avatarUrl = jsonObject.getString("avatarUrl"),
                                githubUsername = jsonObject.getString("githubUsername")
                            )
                        )
                    }
                    contributors = contributorsList
                    error = null
                } else {
                    error = context.getString(R.string.error_failed_to_load_contributors, connection.responseCode)
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                error = context.getString(R.string.error_loading_contributors, e.localizedMessage)
            }
        }
    }

    // Initial fetch with smooth progress animation
    LaunchedEffect(Unit) {
        val animationJob = async {
            val animationDuration = 1500L
            val steps = 100
            val delayPerStep = animationDuration / steps

            repeat(steps) { step ->
                loadingProgress = (step + 1).toFloat() / steps
                kotlinx.coroutines.delay(delayPerStep)
            }
        }

        val fetchJob = async { fetchContributors() }

        animationJob.await()
        fetchJob.await()
        kotlinx.coroutines.delay(200)
        isLoading = false
    }

    // Pull to refresh handler
    val onRefresh: () -> Unit = {
        isRefreshing = true
        coroutineScope.launch {
            fetchContributors()
            kotlinx.coroutines.delay(500) // Brief delay for smooth animation
            isRefreshing = false
        }
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
                            IconButton(onClick = navController::navigateUp) {
                                Icon(
                                    painterResource(R.drawable.arrow_back),
                                    contentDescription = context.getString(R.string.back),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        actions = {
                            // Accessible refresh button
                            IconButton(
                                onClick = onRefresh,
                                enabled = !isRefreshing && !isLoading
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = context.getString(R.string.refresh_contributors),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .then(
                                            if (isRefreshing) {
                                                Modifier.rotate(
                                                    animateFloatAsState(
                                                        targetValue = 360f,
                                                        animationSpec = infiniteRepeatable(
                                                            animation = tween(1000, easing = LinearEasing),
                                                            repeatMode = RepeatMode.Restart
                                                        ),
                                                        label = ""
                                                    ).value
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
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
                        LinearWavyProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            PullToRefreshBox(
                modifier = Modifier.padding(paddingValues),
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.contributors),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.contributors_subtitle),
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
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = error ?: stringResource(R.string.error_unknown),
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = onRefresh,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text(stringResource(R.string.retry))
                                        }
                                    }
                                }
                            }
                        }

                        contributors.isNotEmpty() -> {
                            item {
                                Materialcard(
                                    contributors = contributors,
                                    onGitHubClick = { username ->
                                        uriHandler.openUri("https://github.com/$username")
                                    }
                                )
                            }
                        }

                        !isLoading -> {
                            item {
                                Text(
                                    text = stringResource(R.string.no_contributors_found),
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
}
