package com.music.vivi.ui.screens.settings

import android.content.Intent
import android.os.Build
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.LottieConstants // For iterations, like IterateForever
import com.airbnb.lottie.compose.LottieClipSpec // If you need to specify a clip range

import androidx.compose.runtime.getValue

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
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Paid
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.content.ActivityNotFoundException

import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.ui.platform.LocalContext

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard

import androidx.compose.material3.ModalBottomSheet

import androidx.compose.material3.rememberModalBottomSheetState

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign


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
    var showErrorBottomSheet by remember { mutableStateOf(false) }
    var showErrorBottom1Sheet by remember { mutableStateOf(false) }
    // Changelog state
    var showChangelog by remember { mutableStateOf(false) }
    val changelogViewModel: ChangelogViewModel = viewModel()

    var showDonationCardDialog by remember { mutableStateOf(false) }
    // Developer Bottom Sheet state
    var showDeveloperSheet by remember { mutableStateOf(false) }

    // NEW: Website Bottom Sheet state
    var showWebsiteSheet by remember { mutableStateOf(false) }

    // NEW: Github Bottom Sheet state
    var showGithubSheet by remember { mutableStateOf(false) }

    // ---feedback for vivi---
    var showFeedbackSheet by remember { mutableStateOf(false) }

    // --- NEW STATE FOR BUILD VERSION CLICKS ---
    var buildVersionClickCount by remember { mutableStateOf(0) }

    // --- LaunchedEffect for Build Version Clicks ---
    LaunchedEffect(buildVersionClickCount) {
        if (buildVersionClickCount >= 5) {
            // Navigate to AppearanceSetting.kt (assuming its route is "settings/appearance")
            navController.navigate("settings/experimental")
            buildVersionClickCount = 0 // Reset the count after navigation
        }
    }

    LaunchedEffect(true) {
        // Load latest release info for update button
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

        // Load changelog for the CURRENT app version
        val currentAppVersionTag = "v${BuildConfig.VERSION_NAME}"
        changelogViewModel.loadChangelog("vivizzz007", "vivi-music", currentAppVersionTag)
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

//                if (isUpdateAvailable.value)
//                {
//                    Button(
//                        onClick = { navController.navigate("settings/update") },
//                        modifier = Modifier
//                            .fillMaxWidth(0.7f)
//                            .height(60.dp),
//                        contentPadding = PaddingValues(16.dp)
//                    ) {
////                        Text(
////                            text = "Update Now",
////                            fontSize = 18.sp,
////                            fontWeight = FontWeight.SemiBold
////                        )
//                    }
//                }
            }

            Spacer(modifier = Modifier.height(100.dp))

            // List Items
            HyperOSListItem(
                title = "Developer",
                value = "VIVIDH P ASHOKAN",
                onClick = { showDeveloperSheet = true }
            )

            // --- MODIFIED: Build Version HyperOSListItem ---
            HyperOSListItem(
                title = "Build Version",
                value = "v${BuildConfig.VERSION_NAME}",
                onClick = { buildVersionClickCount++ } // Increment the count
            )

            HyperOSListItem(
                title = "changelog",
//                value = "v${BuildConfig.VERSION_NAME}",
                value = "CURRENT APP",
                onClick = { showChangelog = true }
            )
            // Modified Donate HyperOSListItem
            HyperOSListItem(
                title = "Donate",
                value = "SUPPORT APP",
                onClick = { showDonationCardDialog = true }
            )
            HyperOSListItem(
                title = "Website",
                value = "VIVI-MUSIC",
                onClick = { showWebsiteSheet = true }
            )
            // MODIFIED: Github HyperOSListItem now triggers the bottom sheet
            HyperOSListItem(
                title = "Github",
                value = "VIVI-MUSIC",
                onClick = { showGithubSheet = true }
            )
            HyperOSListItem(
                title = "Feedback",
                value = "Report",
                onClick = { showFeedbackSheet = true }
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "CHANGELOG FOR  ${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Ensure AutoChangelogCard and ChangelogViewModel are correctly set up
                            AutoChangelogCard(
                                viewModel = changelogViewModel,
                                repoOwner = "vivizzz007",
                                repoName = "vivi-music"
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        //feedback
        if (showFeedbackSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFeedbackSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                FeedbackDetailsSheet(
                    onClose = { showFeedbackSheet = false }
                )
            }
        }


        // Donation Options Bottom Sheet
        if (showDonationCardDialog) {
            ModalBottomSheet(
                onDismissRequest = { showDonationCardDialog = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.love)) // Replace with your Lottie JSON file
                        LottieAnimation(
                            composition = composition,
                            iterations = LottieConstants.IterateForever, // Loop the animation
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SUPPORT VIVI MUSIC",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Help us keep the music playing and improve the app!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Choose Your Way to Support",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    DonationOptionItem(
                        icon = Icons.Filled.Paid,
                        title = "PayPal",
                        description = "Make a secure one-time donation.",
                        onClick = {
                            showDonationCardDialog = false
                            showErrorBottom1Sheet = true
                        }
                    )
                    DonationOptionItem(
                        icon = Icons.Filled.Coffee,
                        title = "Buy Me a Coffee",
                        description = "Support with a virtual coffee.",
                        onClick = {
                            showErrorBottomSheet = true
                            showDonationCardDialog = false
                        }
                    )
                    DonationOptionItem(
                        icon = Icons.Filled.Money,
                        title = "Google Pay (GPay)",
                        description = "Direct support via UPI (India).",
                        onClick = {
                            val upiUri = "upi://pay?pa=vividhpashokan@axl&pn=Vivi%20Music&cu=INR"
                            uriHandler.openUri(upiUri)
                            showDonationCardDialog = false
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // NEW: Github Details Bottom Sheet
        if (showGithubSheet) {
            ModalBottomSheet(
                onDismissRequest = { showGithubSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                GithubDetailsSheet(
                    githubRepoUrl = "https://github.com/vivizzz007/vivi-music",
                    onClose = { showGithubSheet = false }
                )
            }
        }
        // NEW: Website Details Bottom Sheet
        if (showWebsiteSheet) {
            ModalBottomSheet(
                onDismissRequest = { showWebsiteSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                WebsiteDetailsSheet(
                    websiteUrl = "https://vivi-music-web-com.vercel.app/",
                    onClose = { showWebsiteSheet = false }
                )
            }
        }

        if (showDeveloperSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDeveloperSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                DeveloperDetailsSheet(
                    developerName = "VIVIDH P ASHOKAN",
                    githubUrl = "https://github.com/vivizzz007",
                    developerAvatarResId = R.drawable.dev,
                    onClose = { showDeveloperSheet = false }
                )
            }
        }
        if (showErrorBottom1Sheet) {
            ModalBottomSheet(
                onDismissRequest = { showErrorBottom1Sheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Not Available",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "PayPal is not supported at the moment. Please try another donation method.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showErrorBottom1Sheet = false
                            showDonationCardDialog = true
                        }
                    ) {
                        Text("Back")
                    }
                }
            }
        }

        if (showErrorBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showErrorBottomSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Not Available",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Buy Me a Coffee is currently unavailable in India. We'll support it soon.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showErrorBottomSheet = false
                            showDonationCardDialog = true
                        }
                    ) {
                        Text("Back")
                    }
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

// Changelog ViewModel - MODIFIED TO ACCEPT versionTag
class ChangelogViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChangelogState())
    val uiState: StateFlow<ChangelogState> = _uiState.asStateFlow()

    // Modified to accept a specific versionTag
    fun loadChangelog(repoOwner: String, repoName: String, versionTag: String) { // <--- ADDED versionTag PARAMETER
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Pass the versionTag to the fetching function
                val markdownContent = fetchReleaseMarkdown(repoOwner, repoName, versionTag) // <--- PASSING versionTag
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
                        error = "Error loading changelog for $versionTag: ${e.message}. Tag not found or network issue. May be you are using beta version " // Improved error message
                    )
                }
            }
        }
    }

    // Modified to fetch a specific release by tag
    private suspend fun fetchReleaseMarkdown(owner: String, repo: String, versionTag: String): String { // <--- ADDED versionTag PARAMETER
        return withContext(Dispatchers.IO) {
            val url = URL("https://api.github.com/repos/$owner/$repo/releases/tags/$versionTag") // <--- FETCHING BY SPECIFIC TAG
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                // Throwing an IOException when the tag isn't found will be caught by the ViewModel
                throw IOException("HTTP error: ${connection.responseCode}. Release tag '$versionTag' might not exist on GitHub.")
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

@Composable
fun AutoChangelogCard(
    viewModel: ChangelogViewModel,
    repoOwner: String,
    repoName: String
) {
    val uiState by viewModel.uiState.collectAsState()

    ElevatedCard(
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth(),
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
                    Text("No changelog available or tag not found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
fun DonationCard(
    onDonateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .clickable(onClick = onDonateClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Donate",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Support VIVI MUSIC",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Help us keep the music playing and improve the app!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = "Go to donation page",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


@Composable
fun DonationOptionItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
fun DeveloperDetailsSheet(
    developerName: String,
    githubUrl: String,
    developerAvatarResId: Int, // Or imageUrl: String for network image
    onClose: () -> Unit // Function to dismiss the sheet
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Make content scrollable if it gets long
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Large Developer Image
        Image(
            painter = painterResource(id = developerAvatarResId),
            contentDescription = "Developer Avatar",
            modifier = Modifier
                .size(120.dp) // Larger size for the bottom sheet
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = developerName,
            style = MaterialTheme.typography.headlineSmall, // Even larger for prominence
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // GitHub Link as a clickable Button
        Button(
            onClick = {
//                uriHandler.openUri(githubUrl)
                 uriHandler.openUri("https://github.com/vivizzz007")
                onClose() // Close sheet after opening link
            },
            modifier = Modifier
                .fillMaxWidth(0.8f) // Make the button take 80% of the width
                .height(56.dp), // Give it a decent height
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary // Use primary color for the button
            ),
            shape = RoundedCornerShape(12.dp) // Slightly rounded corners for the button
        ) {
            Icon(
                painter = painterResource(id = R.drawable.github_icon), // Use your GitHub icon
                contentDescription = "GitHub",
                modifier = Modifier.size(24.dp), // Adjust icon size within the button
                tint = MaterialTheme.colorScheme.onPrimary // Icon color contrasts with button color
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Visit GitHub Profile", // Clearer button text
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(modifier = Modifier.height(8.dp)) // Space between button and the URL text


        Spacer(modifier = Modifier.height(16.dp))

        Divider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        // About the Developer text
        Text(
            text = "About the Developer:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Vividh is the creator and maintainer of Vivi Music. He is so passionate about open-source development and creating engaging music experiences. You can explore his projects on GitHub.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp)) // Add some bottom spacing
    }
}

// NEW: WebsiteDetailsSheet Composable
@Composable
fun WebsiteDetailsSheet(
    websiteUrl: String,
    onClose: () -> Unit // Function to dismiss the sheet
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

//        Icon(
//            imageVector = Icons.Filled.Language, // A globe icon for website
//            contentDescription = "Website Icon",
//            modifier = Modifier.size(100.dp), // Large icon for prominence
//            tint = MaterialTheme.colorScheme.primary
//        )
//        Image( // Use Image composable for painterResource
//            painter = painterResource(id = R.drawable.website_vivi),
//            contentDescription = "Website Icon",
//            modifier = Modifier.size(100.dp), // Large icon for prominence
//            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.primary) // Apply tint to match theme
//        )
//        Image( // Use Image composable for painterResource
//            painter = painterResource(id = R.drawable.website_vivi),
//            contentDescription = "Visit Website",
//            modifier = Modifier.size(100.dp)
//        )
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.website)) // Replace with your Lottie JSON file
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever, // Loop the animation
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "VIVI MUSIC OFFICIAL WEBSITE",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Button to visit the website
        Button(
            onClick = {
                uriHandler.openUri(websiteUrl)
                onClose() // Close sheet after opening link
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = "Visit Website",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Visit Website",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Divider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "About the Website:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Explore more about Vivi Music, discover new features, listen online, and stay updated with the latest news directly on our official website.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}


@Composable
fun GithubDetailsSheet(
    githubRepoUrl: String,
    onClose: () -> Unit // Function to dismiss the sheet
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Lottie Animation
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.githubanimation)) // Replace with your Lottie JSON file
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever, // Loop the animation
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "VIVI MUSIC GitHub Repository",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                uriHandler.openUri(githubRepoUrl)
                onClose()
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon( // Using Icon for GitHub if it's a vector drawable and you want it tinted
                painter = painterResource(id = R.drawable.github_icon),
                contentDescription = "Visit GitHub Repository",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onPrimary // Apply tint if GitHub icon is monochrome
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Visit Repository",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Divider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "About the Repository:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This is the official open-source repository for VIVI MUSIC. Here you can find the source code, report issues, contribute, and see the project's progress.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}


@Composable
fun FeedbackDetailsSheet(
    onClose: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Feedback icon (you can replace with Lottie animation if preferred)
        Icon(
            imageVector = Icons.Filled.Feedback,
            contentDescription = "Feedback",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Provide Feedback",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // GitHub Issues Button
        Button(
            onClick = {
                uriHandler.openUri("https://github.com/vivizzz007/vivi-music/issues")
                onClose()
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.github_icon),
                contentDescription = "GitHub Issues",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "GitHub Issues",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Email Button
        Button(
            onClick = {
                try {
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:mkmdevilmi@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Vivi Music Feedback")
                    }
                    context.startActivity(emailIntent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                }
                onClose()
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = "Email",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Email Developer",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Divider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your Feedback Matters",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Report bugs, suggest features, or share your thoughts to help improve Vivi Music. We appreciate your support!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}


