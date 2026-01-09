package com.music.vivi.livemedia

import android.os.Handler
import android.os.Looper
import android.util.Log

class NotificationUpdateScheduler(
    private val updateAction: () -> Unit
) {
    private val TAG = "NotificationUpdateScheduler"
    private val updateHandler = Handler(Looper.getMainLooper())
    private var isPlaying = false

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isPlaying) {
                updateAction()
                updateHandler.postDelayed(this, STATE_UPDATE_DELAY_MS)
            } else {
                Log.i(TAG, "Runnable stopped: Not playing.")
            }
        }
    }

    fun scheduleUpdate(state: MusicState) {
        updateAction()

        val wasPlaying = isPlaying
        isPlaying = state.isPlaying

        if (isPlaying && !wasPlaying) {
            Log.i(TAG, "Started playing, scheduling periodic updates.")
            updateHandler.removeCallbacks(updateRunnable)
            updateHandler.postDelayed(updateRunnable, 2000)
        } else if (!isPlaying) {
            Log.i(TAG, "Paused/Stopped, removing periodic updates.")
            updateHandler.removeCallbacks(updateRunnable)
        }
    }

    companion object {
        private const val STATE_UPDATE_DELAY_MS = 1000L
    }
}
