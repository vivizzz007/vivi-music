package com.music.vivi.update.betaupdate

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.music.vivi.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dpi_settings")

@Serializable
data class GitHubRelease(
    val tag_name: String,
    val prerelease: Boolean = false,
    val assets: List<GitHubAsset> = emptyList(),
    val body: String = "",
    val published_at: String = "",
)

@Serializable
data class GitHubAsset(val name: String, val browser_download_url: String, val size: Long = 0L)

@HiltViewModel
class DpiSettingsViewModel @Inject constructor(application: Application, private val okHttpClient: OkHttpClient) :
    ViewModel() {
    private val dataStore = application.dataStore
    private val DPI_ENABLED_KEY = booleanPreferencesKey("dpi_enabled")
    private val DOWNLOADED_TAG_KEY = stringPreferencesKey("downloaded_tag")

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val _isDpiEnabled = MutableStateFlow(false)
    val isDpiEnabled: StateFlow<Boolean> = _isDpiEnabled.asStateFlow()

    private val _isApkDownloaded = MutableStateFlow(false)
    val isApkDownloaded: StateFlow<Boolean> = _isApkDownloaded.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadedApkUri = MutableStateFlow<Uri?>(null)
    val downloadedApkUri: StateFlow<Uri?> = _downloadedApkUri.asStateFlow()

    private val _showDetails = MutableStateFlow(false)
    val showDetails: StateFlow<Boolean> = _showDetails.asStateFlow()

    private val _latestPreRelease = MutableStateFlow<GitHubRelease?>(null)
    val latestPreRelease: StateFlow<GitHubRelease?> = _latestPreRelease.asStateFlow()

    private val _downloadedTag = MutableStateFlow<String?>(null)
    val downloadedTag: StateFlow<String?> = _downloadedTag.asStateFlow()

    private val _fetchState = MutableStateFlow<FetchState>(FetchState.Loading)
    val fetchState: StateFlow<FetchState> = _fetchState.asStateFlow()

    sealed class FetchState {
        object Loading : FetchState()
        object Success : FetchState()
        data class Error(val message: String) : FetchState()
    }

    private var autoCheckJob: Job? = null

    init {
        viewModelScope.launch {
            dataStore.data.map { prefs ->
                prefs[DPI_ENABLED_KEY] ?: false
            }.collect {
                _isDpiEnabled.value = it
                if (it) applyDpiCode("s0 -o1 -d1 -r1+s -Ar -o1 -At -f-1 -r1+s -As")
            }
        }
        viewModelScope.launch {
            dataStore.data.map { prefs ->
                prefs[DOWNLOADED_TAG_KEY]
            }.collect {
                _downloadedTag.value = it
                val apkFile = File(application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "vivibeta.apk")
                _isApkDownloaded.value = it != null && apkFile.exists()
                if (_isApkDownloaded.value) {
                    _downloadedApkUri.value = FileProvider.getUriForFile(
                        application,
                        "${application.packageName}.provider",
                        apkFile
                    )
                }
            }
        }
        fetchLatestPreRelease()
        startAutoCheckForUpdates()
    }

    private fun startAutoCheckForUpdates() {
        // Cancel if already running
        autoCheckJob?.cancel()
        autoCheckJob = viewModelScope.launch {
            while (isActive) {
                delay(10 * 60 * 1000L) // 10 minutes
                fetchLatestPreRelease()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoCheckJob?.cancel()
    }

    fun toggleDpi() {
        val newState = !_isDpiEnabled.value
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[DPI_ENABLED_KEY] = newState
            }
        }
        _isDpiEnabled.value = newState
        if (newState) {
            applyDpiCode("s0 -o1 -d1 -r1+s -Ar -o1 -At -f-1 -r1+s -As")
        } else {
            removeDpiCode()
        }
    }

    fun setApkDownloaded(downloaded: Boolean, uri: Uri? = null, tag: String? = null) {
        _isApkDownloaded.value = downloaded
        _downloadedApkUri.value = uri
        _isDownloading.value = false
        viewModelScope.launch {
            dataStore.edit { prefs ->
                if (tag != null) {
                    prefs[DOWNLOADED_TAG_KEY] = tag
                } else {
                    prefs.remove(DOWNLOADED_TAG_KEY)
                }
            }
            _downloadedTag.value = tag
        }
    }

    fun setDownloading(downloading: Boolean) {
        _isDownloading.value = downloading
    }

    fun setDownloadProgress(progress: Float) {
        _downloadProgress.value = progress
    }

    fun toggleDetails() {
        _showDetails.value = !_showDetails.value
    }

    private fun applyDpiCode(code: String) {
        println("DPI Code Applied: $code")
    }

    private fun removeDpiCode() {
        println("DPI Code Disabled")
    }

    fun fetchLatestPreRelease() {
        viewModelScope.launch {
            _fetchState.value = FetchState.Loading
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/repos/vivizzz007/vivi-music/releases")
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .addHeader("User-Agent", "Android-App/1.0")
                    .addHeader("X-GitHub-Api-Version", "2022-11-28")
                    .build()

                val response = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute()
                }

                Log.d("DpiSettingsViewModel", "Response code: ${response.code}")
                Log.d("DpiSettingsViewModel", "Response message: ${response.message}")

                if (response.isSuccessful) {
                    val json = response.body?.string()
                    Log.d("DpiSettingsViewModel", "Response body length: ${json?.length ?: 0}")

                    json?.let { jsonString ->
                        if (jsonString.isBlank()) {
                            Log.e("DpiSettingsViewModel", "Empty JSON response")
                            _fetchState.value = FetchState.Error("Empty response from GitHub API")
                            return@let
                        }

                        try {
                            val releases = this@DpiSettingsViewModel.json.decodeFromString<List<GitHubRelease>>(
                                jsonString
                            )

                            Log.d("DpiSettingsViewModel", "Total releases found: ${releases.size}")

                            releases.forEachIndexed { index, release ->
                                Log.d(
                                    "DpiSettingsViewModel",
                                    "Release $index: tag=${release.tag_name}, prerelease=${release.prerelease}"
                                )
                            }

                            val latestPreRelease = releases.firstOrNull { it.prerelease }
                            Log.d(
                                "DpiSettingsViewModel",
                                "Latest pre-release: ${latestPreRelease?.tag_name ?: "None found"}"
                            )

                            // Check if this is a new update
                            val isNewUpdate = latestPreRelease != null &&
                                latestPreRelease.tag_name != _downloadedTag.value &&
                                _latestPreRelease.value?.tag_name != latestPreRelease.tag_name

                            _latestPreRelease.value = latestPreRelease
                            _fetchState.value = FetchState.Success

                            // Auto-expand details for new updates
                            if (isNewUpdate) {
                                _showDetails.value = true
                            }

                            if (latestPreRelease == null) {
                                Log.w("DpiSettingsViewModel", "No pre-release found in ${releases.size} releases")
                            }
                        } catch (jsonException: Exception) {
                            Log.e("DpiSettingsViewModel", "JSON parsing error: ${jsonException.message}", jsonException)
                            Log.e("DpiSettingsViewModel", "JSON content preview: ${jsonString.take(1000)}")
                            _fetchState.value =
                                FetchState.Error("Failed to parse releases data: ${jsonException.message}")
                        }
                    } ?: run {
                        Log.e("DpiSettingsViewModel", "Response body is null")
                        _fetchState.value = FetchState.Error("Empty response body")
                    }
                } else {
                    val errorBody = response.body?.string()
                    Log.e("DpiSettingsViewModel", "API Error - Code: ${response.code}, Message: ${response.message}")
                    Log.e("DpiSettingsViewModel", "Error body: $errorBody")

                    // Handle specific GitHub API errors
                    val errorMessage = when (response.code) {
                        403 -> "Rate limit exceeded. Try again later."
                        404 -> "Repository not found. Check the repository name."
                        422 -> "Invalid request parameters."
                        500, 502, 503, 504 -> "GitHub server error. Try again later."
                        else -> "GitHub API error: ${response.code} ${response.message}"
                    }

                    _fetchState.value = FetchState.Error(errorMessage)
                }
            } catch (e: java.net.UnknownHostException) {
                Log.e("DpiSettingsViewModel", "Network error - No internet connection", e)
                _fetchState.value = FetchState.Error("No internet connection")
            } catch (e: java.net.SocketTimeoutException) {
                Log.e("DpiSettingsViewModel", "Network timeout", e)
                _fetchState.value = FetchState.Error("Request timed out")
            } catch (e: javax.net.ssl.SSLException) {
                Log.e("DpiSettingsViewModel", "SSL error", e)
                _fetchState.value = FetchState.Error("SSL connection error")
            } catch (e: java.io.IOException) {
                Log.e("DpiSettingsViewModel", "IO error", e)
                _fetchState.value = FetchState.Error("Network error: ${e.message}")
            } catch (e: Exception) {
                Log.e("DpiSettingsViewModel", "Unexpected error: ${e.message}", e)
                _fetchState.value = FetchState.Error("Unexpected error: ${e.message}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViviDpiSettings(navController: NavController, scrollBehavior: TopAppBarScrollBehavior? = null) {
    val context = LocalContext.current
    val viewModel: DpiSettingsViewModel = hiltViewModel()

    val isDpiEnabled by viewModel.isDpiEnabled.collectAsState()
    val isApkDownloaded by viewModel.isApkDownloaded.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadedApkUri by viewModel.downloadedApkUri.collectAsState()
    val showDetails by viewModel.showDetails.collectAsState()
    val latestPreRelease by viewModel.latestPreRelease.collectAsState()
    val downloadedTag by viewModel.downloadedTag.collectAsState()
    val fetchState by viewModel.fetchState.collectAsState()

    var downloadId by remember { mutableLongStateOf(-1) }

    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val latestPreReleaseValue = latestPreRelease
    val statusText = when {
        isDownloading -> stringResource(R.string.downloading_ellipsis)
        isApkDownloaded && downloadedTag == latestPreReleaseValue?.tag_name -> stringResource(R.string.ready_to_install)
        latestPreReleaseValue != null -> stringResource(R.string.available)
        fetchState is DpiSettingsViewModel.FetchState.Loading -> stringResource(R.string.checking_for_updates)
        fetchState is DpiSettingsViewModel.FetchState.Error -> stringResource(R.string.failed_to_load)
        else -> stringResource(R.string.no_update)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.beta_updater),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBackPressedDispatcher?.onBackPressed() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { /* Menu options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            OxygenOSStyleCard(
                isApkDownloaded = isApkDownloaded,
                isDownloading = isDownloading,
                downloadProgress = downloadProgress,
                statusText = statusText,
                onDownloadClick = {
                    latestPreReleaseValue?.assets?.firstOrNull { it.name.endsWith(".apk") }?.let { asset ->
                        downloadId = downloadApk(
                            context,
                            asset.browser_download_url,
                            viewModel,
                            "vivibeta.apk",
                            latestPreReleaseValue.tag_name
                        )
                    } ?: run {
                        Toast.makeText(context, context.getString(R.string.no_apk_found), Toast.LENGTH_SHORT).show()
                    }
                },
                onInstallClick = {
                    downloadedApkUri?.let { uri ->
                        installApk(context, uri)
                    }
                },
                onViewDetailsClick = { viewModel.toggleDetails() },
                onRecheckClick = { viewModel.fetchLatestPreRelease() },
                fetchState = fetchState,
                hasPreRelease = latestPreReleaseValue != null
            )

            AnimatedVisibility(
                visible = showDetails && latestPreReleaseValue != null,
                enter = expandVertically(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 200,
                        delayMillis = 100
                    )
                ),
                exit = shrinkVertically(
                    animationSpec = tween(
                        durationMillis = 250,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 150)
                )
            ) {
                if (latestPreReleaseValue != null) {
                    DetailsSection(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        changelog = latestPreReleaseValue.body,
                        version = latestPreReleaseValue.tag_name,
                        apkSize = latestPreReleaseValue.assets.firstOrNull { it.name.endsWith(".apk") }?.size ?: 0,
                        uploadDateTime = latestPreReleaseValue.published_at
                    )
                }
            }
        }
    }
}

@Composable
fun OxygenOSStyleCard(
    isApkDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    statusText: String,
    onDownloadClick: () -> Unit,
    onInstallClick: () -> Unit,
    onViewDetailsClick: () -> Unit,
    onRecheckClick: () -> Unit,
    fetchState: DpiSettingsViewModel.FetchState,
    hasPreRelease: Boolean,
    modifier: Modifier = Modifier,
) {
    Log.d("OxygenOSStyleCard", "Recomposition - isApkDownloaded: $isApkDownloaded, isDownloading: $isDownloading")
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF6B6B).copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "2.0",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .width(80.dp)
                    .height(20.dp)
            ) {
                val path = Path().apply {
                    moveTo(0f, size.height)
                    quadraticTo(size.width / 2, 0f, size.width, size.height)
                }
                drawPath(
                    path = path,
                    color = Color(0xFFFF6B6B),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.vivi_music),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.vivi_music_beta_update),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isDownloading) {
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFFF6B6B),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${(downloadProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hasPreRelease) {
                    OutlinedButton(
                        onClick = onViewDetailsClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Text(
                            stringResource(R.string.changelog),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onRecheckClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.recheck),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Button(
                    onClick = if (isApkDownloaded) onInstallClick else onDownloadClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isApkDownloaded) Color(0xFF4CAF50) else Color(0xFFFF6B6B),
                        contentColor = Color.White
                    ),
                    enabled = !isDownloading && hasPreRelease
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = if (isApkDownloaded) Icons.Default.InstallMobile else Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isApkDownloaded) {
                                stringResource(
                                    R.string.install
                                )
                            } else {
                                stringResource(R.string.action_download)
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailsSection(
    modifier: Modifier = Modifier,
    changelog: String,
    version: String,
    apkSize: Long,
    uploadDateTime: String,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.changelog_for_version, version),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            DetailItem(
                label = stringResource(R.string.version_label),
                value = version
            )

            DetailItem(
                label = stringResource(R.string.apk_size_label),
                value = "${"%.2f".format(apkSize / (1024.0 * 1024.0))} MB"
            )

            DetailItem(
                label = stringResource(R.string.upload_date_label),
                value = formatUploadDate(uploadDateTime)
            )

            DetailItem(
                label = stringResource(R.string.upload_time_label),
                value = formatUploadTime(uploadDateTime)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Text(
                text = stringResource(R.string.changelog),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = changelog,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

fun downloadApk(context: Context, url: String, viewModel: DpiSettingsViewModel, fileName: String, tag: String): Long {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        !context.packageManager.canRequestPackageInstalls()
    ) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        Toast.makeText(context, context.getString(R.string.enable_install_unknown_apps), Toast.LENGTH_LONG).show()
        return -1
    }

    val request = DownloadManager.Request(Uri.parse(url)).apply {
        setTitle(context.getString(R.string.downloading_vivi_music_apk))
        setDescription(context.getString(R.string.downloading_vivi_music_prerelease))
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setAllowedOverMetered(true)
        setAllowedOverRoaming(true)
        setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        addRequestHeader("User-Agent", "Mozilla/5.0 (Android)")
    }

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadId = try {
        downloadManager.enqueue(request)
    } catch (e: Exception) {
        Log.e("DownloadAPK", "Download error: ${e.message}", e)
        Toast.makeText(context, context.getString(R.string.download_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
        viewModel.setDownloading(false)
        return -1
    }

    viewModel.setDownloading(true)
    viewModel.setDownloadProgress(0f)

    startProgressTracking(context, downloadManager, downloadId, viewModel, tag)

    Toast.makeText(context, context.getString(R.string.download_started), Toast.LENGTH_SHORT).show()
    return downloadId
}

@RequiresApi(Build.VERSION_CODES.O)
private fun startProgressTracking(
    context: Context,
    downloadManager: DownloadManager,
    downloadId: Long,
    viewModel: DpiSettingsViewModel,
    tag: String,
) {
    val progressHandler = Handler(Looper.getMainLooper())
    var isTracking = true

    val progressRunnable = object : Runnable {
        override fun run() {
            if (!isTracking) return

            try {
                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)

                        if (statusIndex == -1) {
                            Log.e("DownloadAPK", "Status column not found")
                            viewModel.setApkDownloaded(false, null)
                            viewModel.setDownloading(false)
                            return@use
                        }

                        val status = cursor.getInt(statusIndex)

                        when (status) {
                            DownloadManager.STATUS_RUNNING -> {
                                val totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                                val downloadedIndex = cursor.getColumnIndex(
                                    DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                                )

                                if (totalIndex != -1 && downloadedIndex != -1) {
                                    val total = cursor.getLong(totalIndex)
                                    val downloaded = cursor.getLong(downloadedIndex)

                                    if (total > 0) {
                                        val progress = downloaded.toFloat() / total.toFloat()
                                        viewModel.setDownloadProgress(progress)
                                        Log.d("DownloadAPK", "Progress: ${(progress * 100).toInt()}%")
                                    }
                                }
                                progressHandler.postDelayed(this, 500)
                            }

                            DownloadManager.STATUS_SUCCESSFUL -> {
                                isTracking = false
                                val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                                if (uriIndex != -1) {
                                    val uriString = cursor.getString(uriIndex)
                                    val apkUri = Uri.parse(uriString)
                                    val apkFile = getFileFromUri(context, apkUri)
                                    if (apkFile != null && apkFile.exists()) {
                                        Log.d(
                                            "DownloadAPK",
                                            "Download completed: $uriString, file exists: ${apkFile.exists()}"
                                        )
                                        viewModel.setApkDownloaded(true, apkUri, tag)
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.download_completed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Log.e("DownloadAPK", "APK file not found at: $uriString")
                                        viewModel.setApkDownloaded(false, null)
                                        viewModel.setDownloading(false)
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.download_completed_file_not_found),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else {
                                    Log.e("DownloadAPK", "URI column not found")
                                    viewModel.setApkDownloaded(false, null)
                                    viewModel.setDownloading(false)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.download_completed_uri_not_found),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                            DownloadManager.STATUS_FAILED -> {
                                isTracking = false
                                val reason = if (reasonIndex != -1) cursor.getInt(reasonIndex) else -1
                                val errorMessage = getDownloadErrorMessage(context, reason)
                                Log.e("DownloadAPK", "Download failed with reason: $reason ($errorMessage)")
                                viewModel.setApkDownloaded(false, null)
                                viewModel.setDownloading(false)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.download_failed_error, errorMessage),
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            DownloadManager.STATUS_PAUSED -> {
                                Log.d("DownloadAPK", "Download paused")
                                progressHandler.postDelayed(this, 1000)
                            }

                            DownloadManager.STATUS_PENDING -> {
                                Log.d("DownloadAPK", "Download pending")
                                progressHandler.postDelayed(this, 1000)
                            }
                        }
                    } else {
                        isTracking = false
                        Log.e("DownloadAPK", "Download entry not found")
                        viewModel.setApkDownloaded(false, null)
                        viewModel.setDownloading(false)
                        Toast.makeText(
                            context,
                            context.getString(R.string.download_tracking_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                isTracking = false
                Log.e("DownloadAPK", "Error tracking download progress", e)
                viewModel.setApkDownloaded(false, null)
                viewModel.setDownloading(false)
                Toast.makeText(
                    context,
                    context.getString(R.string.download_tracking_error, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    progressHandler.post(progressRunnable)

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val safeContext = context ?: return
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
            if (id == downloadId) {
                isTracking = false
                progressHandler.removeCallbacks(progressRunnable)
                Log.d("DownloadAPK", "Download broadcast received for ID: $id")
                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        if (cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL && uriIndex != -1) {
                            val uriString = cursor.getString(uriIndex)
                            val apkUri = Uri.parse(uriString)
                            val apkFile = getFileFromUri(safeContext, apkUri)
                            if (apkFile != null && apkFile.exists()) {
                                viewModel.setApkDownloaded(true, apkUri, tag)
                            } else {
                                viewModel.setApkDownloaded(false, null)
                                Toast.makeText(
                                    safeContext,
                                    safeContext.getString(R.string.downloaded_file_not_found),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            viewModel.setApkDownloaded(false, null)
                        }
                    }
                }
                try {
                    safeContext.unregisterReceiver(this)
                } catch (e: IllegalArgumentException) {
                    Log.w("DownloadAPK", "Receiver was not registered or already unregistered")
                }
            }
        }
    }

    try {
        context.registerReceiver(
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_EXPORTED
        )
    } catch (e: Exception) {
        Log.e("DownloadAPK", "Failed to register broadcast receiver: ${e.message}", e)
        Toast.makeText(context, context.getString(R.string.failed_to_track_download), Toast.LENGTH_SHORT).show()
    }
}

private fun getDownloadErrorMessage(context: Context, reason: Int): String = when (reason) {
    DownloadManager.ERROR_CANNOT_RESUME -> context.getString(R.string.cannot_resume_download)
    DownloadManager.ERROR_DEVICE_NOT_FOUND -> context.getString(R.string.external_storage_not_found)
    DownloadManager.ERROR_FILE_ALREADY_EXISTS -> context.getString(R.string.file_already_exists)
    DownloadManager.ERROR_FILE_ERROR -> context.getString(R.string.file_error)
    DownloadManager.ERROR_HTTP_DATA_ERROR -> context.getString(R.string.http_data_error)
    DownloadManager.ERROR_INSUFFICIENT_SPACE -> context.getString(R.string.insufficient_storage_space)
    DownloadManager.ERROR_TOO_MANY_REDIRECTS -> context.getString(R.string.too_many_redirects)
    DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> context.getString(R.string.unhandled_http_code)
    DownloadManager.ERROR_UNKNOWN -> context.getString(R.string.unknown_error)
    else -> context.getString(R.string.error_message, "Error code: $reason")
}

fun installApk(context: Context, uri: Uri) {
    try {
        val apkFile = getFileFromUri(context, uri)
        if (apkFile == null || !apkFile.exists()) {
            Log.e("InstallAPK", "APK file not found at URI: $uri")
            Toast.makeText(context, context.getString(R.string.apk_file_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )

        Log.d("InstallAPK", "Attempting to install vivibeta.apk from URI: $apkUri")
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (installIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(installIntent)
        } else {
            Log.e("InstallAPK", "No application available to install APKs")
            Toast.makeText(context, context.getString(R.string.no_app_to_install_apks), Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e("InstallAPK", "Error installing APK: ${e.message}", e)
        Toast.makeText(
            context,
            context.getString(R.string.installation_error, e.message ?: ""),
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun getFileFromUri(context: Context, uri: Uri): File? {
    return try {
        when (uri.scheme) {
            "file" -> File(uri.path ?: return null)
            "content" -> {
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "vivibeta.apk")
            }
            else -> null
        }
    } catch (e: Exception) {
        Log.e("GetFileFromUri", "Error getting file from URI: $uri", e)
        null
    }
}

private fun formatUploadDate(dateTime: String): String = try {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    val date = inputFormat.parse(dateTime)
    outputFormat.format(date ?: Date())
} catch (e: Exception) {
    Log.e("FormatUploadDate", "Error formatting date: $dateTime", e)
    dateTime.substringBefore('T')
}
private fun formatUploadTime(dateTime: String): String = try {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val outputFormat = SimpleDateFormat("HH:mm:ss UTC", Locale.US)
    val date = inputFormat.parse(dateTime)
    outputFormat.format(date ?: Date())
} catch (e: Exception) {
    Log.e("FormatUploadTime", "Error formatting time: $dateTime", e)
    dateTime.substringAfter('T').substringBefore('Z') + " UTC"
}
