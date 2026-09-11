/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.auth

import android.content.Context
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom
import java.util.Locale

/**
 * Utility functions for Wear OS authentication, network address resolution,
 * PIN generation, and InnerTube credential normalization.
 */
object WearAuthUtils {

    private val secureRandom = SecureRandom()

    /**
     * Resolves the active local IPv4 address of the watch on Wi-Fi/LAN.
     * Queries [NetworkInterface] first, prioritizing Wi-Fi interfaces (wlan, ap, eth),
     * and falls back to [WifiManager].
     *
     * @return IPv4 address string (e.g. "192.168.1.50"), or null if unavailable.
     */
    fun getLocalIpAddress(context: Context): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return getWifiManagerIp(context)
            val ipList = mutableListOf<String>()

            for (networkInterface in interfaces.toList()) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                val name = networkInterface.name.lowercase(Locale.ROOT)

                for (inetAddress in networkInterface.inetAddresses.toList()) {
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        val hostAddress = inetAddress.hostAddress ?: continue
                        if (hostAddress.startsWith("127.")) continue
                        // Prioritize Wi-Fi interfaces
                        if (name.contains("wlan") || name.contains("ap") || name.contains("eth")) {
                            return hostAddress
                        }
                        ipList.add(hostAddress)
                    }
                }
            }

            if (ipList.isNotEmpty()) {
                return ipList.first()
            }
        } catch (_: Exception) {
            // Fall through to WifiManager fallback
        }

        return getWifiManagerIp(context)
    }

    private fun getWifiManagerIp(context: Context): String? {
        return try {
            @Suppress("DEPRECATION")
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            val ip = wm?.connectionInfo?.ipAddress ?: 0
            if (ip != 0) {
                String.format(
                    Locale.US,
                    "%d.%d.%d.%d",
                    ip and 0xff,
                    (ip shr 8) and 0xff,
                    (ip shr 16) and 0xff,
                    (ip shr 24) and 0xff,
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Generates a random 4-digit numeric PIN formatted as "%04d".
     */
    fun generatePairingPin(): String {
        val pinNumber = secureRandom.nextInt(10000)
        return String.format(Locale.US, "%04d", pinNumber)
    }

    /**
     * Normalizes InnerTube dataSyncId string.
     * If the raw string contains "||", extracts the delegated identifier after "||",
     * falling back to the portion before "||" if empty.
     */
    fun normalizeDataSyncId(raw: String?): String? {
        if (raw == null) return null
        if (!raw.contains("||")) return raw
        val delegated = raw.substringAfter("||")
        return delegated.ifEmpty { raw.substringBefore("||") }
    }
}
