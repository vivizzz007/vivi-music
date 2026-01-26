package com.music.vivi.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Observes network connectivity status changes using [ConnectivityManager].
 *
 * This class exposes a [Flow] of boolean values indicating whether the device has a valid
 * internet connection. It handles both synchronous checks and asynchronous callbacks.
 *
 * ## Usage Example
 * ```kotlin
 * val observer = NetworkConnectivityObserver(context)
 * observer.networkStatus.collect { hasInternet ->
 *     if (hasInternet) {
 *         // Resume playback or retry download
 *     }
 * }
 * ```
 *
 * @param context The application context used to get system services.
 */
class NetworkConnectivityObserver(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkStatus = Channel<Boolean>(Channel.CONFLATED)
    /**
     * A [Flow] that emits `true` when the network is available and validated, and `false` otherwise.
     * The flow is conflated, meaning new collectors will immediately receive the most recent state.
     */
    val networkStatus = _networkStatus.receiveAsFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // We'll rely on onCapabilitiesChanged for validation updates
        }

        override fun onLost(network: Network) {
            _networkStatus.trySend(false)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                hasInternet // Fallback for older APIs
            }

            if (hasInternet && isValidated) {
                _networkStatus.trySend(true)
            }
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            // If registration fails, we can't monitor, so maybe assume connected or retry?
            // For now, let's assume true to avoid blocking the app completely,
            // but effectively we are flying blind.
            _networkStatus.trySend(true)
        }

        // Send initial state
        _networkStatus.trySend(isCurrentlyConnected())
    }

    fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Already unregistered or failed
        }
    }

    /**
     * Checks the current connectivity state synchronously.
     *
     * This method verifies two conditions:
     * 1.  **Connected**: The device has an active network interface.
     * 2.  **Validated**: The network actually has internet access (checks [NetworkCapabilities.NET_CAPABILITY_VALIDATED] on Android M+).
     *
     * @return `true` if the device is connected to the internet, `false` otherwise.
     */
    fun isCurrentlyConnected(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

            val isValidated = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                hasInternet
            }

            hasInternet && isValidated
        } catch (e: Exception) {
            false
        }
    }
}
