/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.music.vivi.wear.WearApp
import com.music.vivi.wear.auth.WearAuthManager
import com.music.vivi.wear.auth.WearAuthState
import com.music.vivi.wear.auth.WearAuthUtils
import com.music.vivi.wear.auth.WearPairingServer
import com.music.vivi.wear.auth.WearQrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Login & Pairing screen for Wear OS.
 *
 * Implements phone-to-watch QR code authentication transfer:
 * 1. Detects local Wi-Fi state; prompts user if Wi-Fi is disconnected.
 * 2. Starts an embedded [WearPairingServer] on Wi-Fi.
 * 3. Generates and renders a crisp high-contrast QR code and 4-digit pairing PIN.
 * 4. Displays session validation progress upon receiving credentials.
 * 5. When logged in, displays user profile details and a "Disconnect / Log Out" option.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authManager: WearAuthManager = remember { WearApp.instance.authManager }

    val authState by authManager.authState.collectAsState()
    val isAccountConnected by authManager.isAccountConnected.collectAsState()
    val accountName by authManager.accountName.collectAsState(initial = null)
    val accountEmail by authManager.accountEmail.collectAsState(initial = null)

    var wifiIp by remember { mutableStateOf(WearAuthUtils.getLocalIpAddress(context)) }
    var pairingServer by remember { mutableStateOf<WearPairingServer?>(null) }
    var qrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var localUrl by remember { mutableStateOf("") }
    var currentPin by remember { mutableStateOf("") }
    var isValidating by remember { mutableStateOf(false) }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }

    fun refreshWifiAndStartServer() {
        val ip = WearAuthUtils.getLocalIpAddress(context)
        wifiIp = ip
        localErrorMessage = null

        if (ip != null && !ip.startsWith("127.")) {
            pairingServer?.stop()
            val server = WearPairingServer(context) { cookie, dataSyncId, visitorData ->
                withContext(Dispatchers.Main) {
                    isValidating = true
                    localErrorMessage = null
                }
                val result = authManager.validateAndSaveSession(cookie, dataSyncId, visitorData)
                withContext(Dispatchers.Main) {
                    isValidating = false
                    result.fold(
                        onSuccess = {
                            pairingServer?.stop()
                            pairingServer = null
                            onLoginSuccess()
                        },
                        onFailure = { err ->
                            localErrorMessage = err.message ?: "Account validation failed"
                        },
                    )
                }
            }
            val info = server.start()
            pairingServer = server
            currentPin = info.pin
            localUrl = info.pairingUrl

            scope.launch(Dispatchers.IO) {
                val bmp = WearQrCodeGenerator.generate(info.pairingUrl, 240)
                withContext(Dispatchers.Main) {
                    qrBitmap = bmp
                }
            }
        }
    }

    DisposableEffect(isAccountConnected) {
        if (!isAccountConnected) {
            refreshWifiAndStartServer()
        }
        onDispose {
            pairingServer?.stop()
            pairingServer = null
        }
    }

    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when {
            // State 1: User is already logged in
            isAccountConnected || authState is WearAuthState.LoggedIn -> {
                val displayName = accountName ?: (authState as? WearAuthState.LoggedIn)?.name ?: "YouTube Music User"
                val displayEmail = accountEmail ?: (authState as? WearAuthState.LoggedIn)?.email

                ScalingLazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .focusable(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item {
                        Text(
                            text = "Account",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    item {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Connected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(vertical = 4.dp),
                        )
                    }
                    item {
                        Text(
                            text = "Signed in as",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    if (!displayEmail.isNullOrBlank()) {
                        item {
                            Text(
                                text = displayEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                    item {
                        Spacer(Modifier.height(12.dp))
                    }
                    item {
                        Button(
                            onClick = {
                                scope.launch {
                                    authManager.logout()
                                    refreshWifiAndStartServer()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(0.85f),
                        ) {
                            Text("Disconnect / Log Out", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // State 2: Validating credentials received from phone
            isValidating || authState is WearAuthState.Validating -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        colors = androidx.wear.compose.material3.ProgressIndicatorDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Verifying account…",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Connecting to YouTube Music",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // State 3: Wi-Fi is disconnected or no active local IPv4
            wifiIp == null || wifiIp!!.startsWith("127.") -> {
                ScalingLazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .focusable(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item {
                        Text(
                            text = "Pair Account",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    item {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Wi-Fi Off",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(36.dp)
                                .padding(vertical = 4.dp),
                        )
                    }
                    item {
                        Text(
                            text = "Connect to Wi-Fi to pair",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    item {
                        Text(
                            text = "Watch and phone must be on the same Wi-Fi network to transfer credentials.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        )
                    }
                    item {
                        Button(
                            onClick = { refreshWifiAndStartServer() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(0.7f),
                        ) {
                            Text("Retry", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // State 4: Server is running and QR code is ready
            else -> {
                val error = localErrorMessage ?: (authState as? WearAuthState.Error)?.message

                ScalingLazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .focusable(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item {
                        Text(
                            text = "Pair Account",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    item {
                        Text(
                            text = "Scan with phone camera",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item {
                        Spacer(Modifier.height(6.dp))
                    }
                    item {
                        if (qrBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    bitmap = qrBitmap!!,
                                    contentDescription = "Pairing QR code",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                colors = androidx.wear.compose.material3.ProgressIndicatorDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                    item {
                        Spacer(Modifier.height(6.dp))
                    }
                    item {
                        Text(
                            text = "PIN: $currentPin",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp,
                            ),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    item {
                        Text(
                            text = localUrl,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    if (!error.isNullOrBlank()) {
                        item {
                            Spacer(Modifier.height(4.dp))
                        }
                        item {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                    }
                    item {
                        Button(
                            onClick = { refreshWifiAndStartServer() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(0.7f),
                        ) {
                            Text("Refresh QR", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
