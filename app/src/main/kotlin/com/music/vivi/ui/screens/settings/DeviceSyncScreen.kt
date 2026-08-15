/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.music.vivi.constants.DeviceSyncServerUrlKey
import com.music.vivi.devicesync.LanDiscovery
import com.music.vivi.sync.SyncServer
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.DeviceSyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSyncScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val viewModel: DeviceSyncViewModel = hiltViewModel()
    val paired by viewModel.paired.collectAsState()
    val status by viewModel.status.collectAsState()

    val (serverUrl, onServerUrlChange) = rememberPreference(DeviceSyncServerUrlKey, SyncServer.DEFAULT_URL)
    var joinCode by remember { mutableStateOf("") }

    val context = LocalContext.current
    val lanDiscovery = remember { LanDiscovery(context) }
    var discovering by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { lanDiscovery.stop() }
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result: ScanIntentResult ->
        result.contents?.let { scanned -> onServerUrlChange(scanned.trim()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.device_sync),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            Text(
                text = stringResource(R.string.device_sync_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            OutlinedTextField(
                value = serverUrl,
                onValueChange = onServerUrlChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.device_sync_server)) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        discovering = true
                        lanDiscovery.discover(
                            onFound = { url ->
                                discovering = false
                                onServerUrlChange(url)
                            },
                            onError = { discovering = false },
                        )
                    },
                    enabled = !discovering,
                ) {
                    Text(stringResource(R.string.device_sync_discover))
                }
                OutlinedButton(
                    onClick = {
                        scanLauncher.launch(
                            ScanOptions()
                                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                .setPrompt(context.getString(R.string.device_sync_scan_qr))
                                .setBeepEnabled(false)
                        )
                    },
                ) {
                    Text(stringResource(R.string.device_sync_scan_qr))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (paired) stringResource(R.string.device_sync_paired) else stringResource(R.string.device_sync_not_paired),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (paired) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (status.isNotBlank()) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!paired) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { joinCode = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.device_sync_join_code)) },
                    )
                    Button(
                        onClick = { viewModel.joinPair(joinCode.trim()) },
                        enabled = joinCode.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.device_sync_pair))
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.unpair() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.device_sync_unpair))
                }
            }

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}
