/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.devicesync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo

/**
 * Discovers the desktop's local LAN relay via mDNS/NSD.
 *
 * The desktop (VIVI Music DE) advertises a `_vivimusic._tcp` service when its
 * LAN server is started; this helper resolves the first matching service to a
 * `ws://<host>:<port>` relay URL that can be dropped into the Devices screen.
 */
class LanDiscovery(context: Context) {

    private val nsdManager: NsdManager? =
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    /** Starts a one-shot discovery; [onFound] receives the resolved relay URL. */
    fun discover(onFound: (String) -> Unit, onError: (String) -> Unit) {
        val manager = nsdManager
        if (manager == null) {
            onError("NSD not available")
            return
        }
        stop()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        val host = resolved.host?.hostAddress ?: return
                        onFound("ws://$host:${resolved.port}")
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit

            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                onError("Discovery failed ($errorCode)")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }

        discoveryListener = listener
        manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun stop() {
        runCatching { discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) } }
        discoveryListener = null
    }

    companion object {
        /** Matches the desktop's `LanSyncRelay.MDNS_TYPE` (`.local.` stripped for NSD). */
        const val SERVICE_TYPE = "_vivimusic._tcp."
    }
}
