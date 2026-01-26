package com.music.vivi.extensions

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.music.innertube.utils.parseCookieString
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.YtmSyncKey
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import kotlinx.coroutines.runBlocking

/**
 * Checks if background synchronization is enabled and the user is logged in.
 *
 * @return true if sync is allowed and possible.
 */
fun Context.isSyncEnabled(): Boolean = runBlocking {
    dataStore.get(YtmSyncKey, true) && isUserLoggedIn()
}

/**
 * Checks if the user is currently logged in to YouTube Music.
 * Verifies the presence of a valid cookie (SAPISID) and an active internet connection.
 *
 * @return true if logged in and connected.
 */
fun Context.isUserLoggedIn(): Boolean = runBlocking {
    val cookie = dataStore[InnerTubeCookieKey] ?: ""
    "SAPISID" in parseCookieString(cookie) && isInternetConnected()
}

/**
 * Checks if the device has a network connection with INTERNET capability.
 *
 * @return true if connected.
 */
fun Context.isInternetConnected(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
}
