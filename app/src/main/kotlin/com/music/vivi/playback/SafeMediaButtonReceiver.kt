package com.music.vivi.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import coil3.request.*
import android.util.Log
import androidx.media3.session.MediaButtonReceiver

/**
 * A wrapper around [MediaButtonReceiver] that safely handles [android.app.ForegroundServiceStartNotAllowedException].
 *
 * On Android 12+ (API 31+), starting a foreground service from the background is restricted.
 * If a media button press (e.g., from a headset) is received while the app is in the background
 * and has lost its "while-in-use" exemption, the default [MediaButtonReceiver] attempts to
 * start the service with [Context.startForegroundService], causing a crash.
 *
 * This class catches that exception to prevent the app from crashing.
 */
class SafeMediaButtonReceiver : BroadcastReceiver() {
    private val delegate = MediaButtonReceiver()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        try {
            delegate.onReceive(context, intent)
        } catch (e: Exception) {
            // This catches ForegroundServiceStartNotAllowedException (Android 12+)
            // and other potential service start failures.
            Log.e("SafeMediaButtonReceiver", "Failed to process media button event", e)
        }
    }
}
