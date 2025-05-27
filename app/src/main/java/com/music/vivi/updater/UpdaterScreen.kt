package com.music.vivi.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.music.vivi.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UpdaterScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentVersion = BuildConfig.VERSION_NAME
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var changelog by remember { mutableStateOf<String?>(null) }
    var downloadUrl by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val release = withContext(Dispatchers.IO) {
                GithubUpdater.fetchLatestRelease()
            }
            release?.let {
                latestVersion = it.tag_name
                changelog = it.body
                downloadUrl = it.assets.firstOrNull()?.browser_download_url
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Current Version: $currentVersion")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Latest Version: ${latestVersion ?: "Checking..."}")
        Spacer(modifier = Modifier.height(16.dp))

        if (latestVersion != null && latestVersion != currentVersion) {
            Text(text = "Changelog:\n${changelog ?: "No details."}")
            Spacer(modifier = Modifier.height(16.dp))

            if (!isDownloading) {
                Button(onClick = {
                    downloadUrl?.let { url ->
                        downloadApk(context, url)
                        isDownloading = true
                    }
                }) {
                    Text("Download Update")
                }
            } else {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Downloading...")
            }
        } else if (latestVersion == currentVersion) {
            Text("Your app is up to date! 🎉")
        }
    }
}

fun downloadApk(context: Context, url: String) {
    val request = DownloadManager.Request(Uri.parse(url))
    request.setTitle("Vivi Update")
    request.setDescription("Downloading update...")
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "vivi-latest.apk")
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadId = manager.enqueue(request)

    // Listener
    val onComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                val installIntent = Intent(Intent.ACTION_VIEW)
                installIntent.setDataAndType(
                    Uri.parse("file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/vivi-latest.apk"),
                    "application/vnd.android.package-archive"
                )
                installIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context?.startActivity(installIntent)
            }
        }
    }

    // Register receiver for download complete event
    context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

    // Unregister receiver when no longer needed, for example, in your Activity/Fragment lifecycle
    // You should unregister the receiver when you no longer need it to avoid memory leaks
    // context.unregisterReceiver(onComplete)
}
