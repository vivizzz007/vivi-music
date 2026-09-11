/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.auth

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import com.music.innertube.YouTube
import com.music.innertube.models.AccountInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Reactive state representing the watch's authentication status.
 */
sealed interface WearAuthState {
    data object LoggedOut : WearAuthState
    data class Pairing(
        val ip: String,
        val port: Int,
        val pin: String,
        val qrBitmap: ImageBitmap?,
    ) : WearAuthState
    data object Validating : WearAuthState
    data class LoggedIn(
        val name: String,
        val email: String?,
    ) : WearAuthState
    data class Error(val message: String) : WearAuthState
}

/**
 * Manages YouTube Music session authentication for the standalone Wear OS app.
 *
 * Coordinates DataStore persistence via [WearAuthPreferences], keeps the [YouTube] singleton
 * in sync, manages the pairing server lifecycle, and enriches profile data via [YouTube.accountInfo].
 */
class WearAuthManager(
    private val context: Context,
    private val preferences: WearAuthPreferences = WearAuthPreferences(context),
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _authState = MutableStateFlow<WearAuthState>(WearAuthState.LoggedOut)
    val authState: StateFlow<WearAuthState> = _authState.asStateFlow()

    /** Synchronous helper returning true when the account is currently verified and logged in. */
    val isConnected: Boolean get() = _authState.value is WearAuthState.LoggedIn

    /** Emits true when the watch has a verified, active user session. */
    val isAccountConnected: StateFlow<Boolean> = _authState
        .map { it is WearAuthState.LoggedIn }
        .stateIn(CoroutineScope(Dispatchers.Unconfined), SharingStarted.Eagerly, false)

    /** Emits true when a valid cookie is stored in DataStore. */
    val isLoggedIn: Flow<Boolean> = preferences.innerTubeCookie
        .map { !it.isNullOrBlank() }

    /** Emits the stored account name, or null if not logged in. */
    val accountName: Flow<String?> = preferences.accountName

    /** Emits the stored account email, or null if not logged in. */
    val accountEmail: Flow<String?> = preferences.accountEmail

    /** Emits the stored channel handle, or null if not logged in. */
    val channelHandle: Flow<String?> = preferences.accountChannelHandle

    private var activePairingServer: WearPairingServer? = null

    /**
     * Reads stored credentials from DataStore and applies them to the [YouTube] singleton.
     * Updates [authState] accordingly.
     */
    suspend fun initFromDataStore() {
        try {
            val cookie = preferences.innerTubeCookie.first()
            val visitorData = preferences.visitorData.first()
            val dataSyncId = preferences.dataSyncId.first()
            val name = preferences.accountName.first()
            val email = preferences.accountEmail.first()

            if (!cookie.isNullOrBlank()) {
                YouTube.cookie = cookie
                YouTube.visitorData = visitorData?.takeIf { it.isNotBlank() }
                YouTube.dataSyncId = WearAuthUtils.normalizeDataSyncId(dataSyncId)
                _authState.value = WearAuthState.LoggedIn(
                    name = name ?: "My Account",
                    email = email,
                )
                Timber.i("WearAuthManager restored credentials for %s", name ?: "My Account")
            } else {
                _authState.value = WearAuthState.LoggedOut
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize credentials from DataStore")
            _authState.value = WearAuthState.LoggedOut
        }
    }

    /**
     * Starts a local pairing server, generates the pairing QR code, and updates [authState] to [WearAuthState.Pairing].
     */
    fun startPairing(port: Int = WearPairingServer.DEFAULT_PORT): PairingServerInfo {
        stopPairing()
        val server = WearPairingServer(context) { cookie, dataSyncId, visitorData ->
            validateAndSaveSession(cookie, dataSyncId, visitorData)
        }
        val info = server.start(port)
        activePairingServer = server

        val qr = try {
            WearQrCodeGenerator.generate(info.pairingUrl, 240)
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate QR code bitmap")
            null
        }

        _authState.value = WearAuthState.Pairing(
            ip = info.ip,
            port = info.port,
            pin = info.pin,
            qrBitmap = qr,
        )
        return info
    }

    /**
     * Stops any active pairing server and transitions back to [WearAuthState.LoggedOut] if pairing.
     */
    fun stopPairing() {
        activePairingServer?.stop()
        activePairingServer = null
        if (_authState.value is WearAuthState.Pairing) {
            _authState.value = WearAuthState.LoggedOut
        }
    }

    /**
     * Accepts credentials received from pairing, persists them immediately into DataStore,
     * updates [YouTube.cookie], and enriches profile data in background via [YouTube.accountInfo].
     */
    suspend fun validateAndSaveSession(
        cookie: String,
        dataSyncId: String?,
        visitorData: String?,
    ): Result<AccountInfo> {
        val trimmedCookie = cookie.trim()
        if (trimmedCookie.isBlank()) {
            _authState.value = WearAuthState.Error("Cookie cannot be empty")
            return Result.failure(IllegalArgumentException("Cookie cannot be empty"))
        }

        _authState.value = WearAuthState.Validating

        // 1. Immediately apply credentials to YouTube singleton
        YouTube.cookie = trimmedCookie
        YouTube.visitorData = visitorData?.takeIf { it.isNotBlank() }
        YouTube.dataSyncId = WearAuthUtils.normalizeDataSyncId(dataSyncId)

        // 2. Persist immediately into DataStore with default account name
        val initialName = "My Account"
        try {
            preferences.saveCredentials(
                cookie = trimmedCookie,
                dataSyncId = dataSyncId,
                visitorData = visitorData,
                accountName = initialName,
                accountEmail = null,
                accountChannelHandle = null,
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to persist credentials to DataStore")
            _authState.value = WearAuthState.Error("Failed to save credentials")
            return Result.failure(e)
        }

        // 3. Stop pairing server and mark logged in
        activePairingServer?.stop()
        activePairingServer = null
        _authState.value = WearAuthState.LoggedIn(
            name = initialName,
            email = null,
        )
        Timber.i("Session saved successfully. Enriching account info in background...")

        // 4. Best-effort background profile enrichment (does not block or fail login if account_menu fails)
        var enrichedInfo = AccountInfo(name = initialName, email = null, channelHandle = null, thumbnailUrl = null)
        try {
            val accountResult = YouTube.accountInfo()
            accountResult.onSuccess { info ->
                enrichedInfo = info
                preferences.saveCredentials(
                    cookie = trimmedCookie,
                    dataSyncId = dataSyncId,
                    visitorData = visitorData,
                    accountName = info.name,
                    accountEmail = info.email,
                    accountChannelHandle = info.channelHandle,
                )
                _authState.value = WearAuthState.LoggedIn(
                    name = info.name,
                    email = info.email,
                )
                Timber.i("Enriched profile info: %s (%s)", info.name, info.email)
            }.onFailure { err ->
                Timber.w(err, "Background accountInfo enrichment skipped (non-fatal)")
            }
        } catch (e: Exception) {
            Timber.w(e, "Background accountInfo enrichment threw (non-fatal)")
        }

        return Result.success(enrichedInfo)
    }

    /**
     * Legacy adapter for backward compatibility with earlier call sites.
     */
    suspend fun acceptCredentials(
        cookie: String,
        visitorData: String?,
        dataSyncId: String?,
    ): AccountInfo? {
        return validateAndSaveSession(cookie, dataSyncId, visitorData).getOrNull()
    }

    /**
     * Clears all stored credentials, resets the [YouTube] singleton, and transitions to [WearAuthState.LoggedOut].
     */
    suspend fun logout() {
        stopPairing()
        YouTube.cookie = null
        YouTube.visitorData = null
        YouTube.dataSyncId = null
        preferences.clearCredentials()
        _authState.value = WearAuthState.LoggedOut
        Timber.i("User logged out, credentials cleared")
    }
}
