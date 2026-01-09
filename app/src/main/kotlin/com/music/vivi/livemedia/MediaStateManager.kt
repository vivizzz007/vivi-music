package com.music.vivi.livemedia

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log

class MediaStateManager(
    private val context: Context,
    private val onStateUpdated: (MusicState) -> Unit,
    private val noActiveMedia: () -> Unit,
) {
    private val TAG = "MediaStateManager"
    private var activeMediaController: MediaController? = null
    private var currentState: MusicState? = null

    private val mediaControllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            handleOnPlaybackStateChanged(state = state)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            handleOnMetadataChanged(metadata)
        }
    }

    init {
        Log.i(TAG, "start")
        maybeUpdateMediaController()
    }

    fun getUpdatedMusicState(
        metadata: MediaMetadata? = null,
        state: PlaybackState? = null
    ): MusicState? {
        activeMediaController?.let { controller ->
            val metadataObj = metadata ?: controller.metadata
            val playbackState = state ?: controller.playbackState

            if (playbackState == null) {
                Log.i(TAG, "playbackState == null")
                return null
            }

            if (playbackState.state == PlaybackState.STATE_STOPPED || playbackState.state == PlaybackState.STATE_NONE) {
                Log.i(TAG, "STATE_STOPPED")
                return null
            }

            if (metadataObj != null) {
                return MusicState(metadataObj, playbackState, controller.packageName)
            }
        }

        return null
    }

    fun maybeUpdateMediaController() {
        val mediaSessionManager =
            context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(context, LiveMediaService::class.java)
        
        try {
            val controllers = mediaSessionManager.getActiveSessions(componentName)
            val myController = controllers.firstOrNull { it.packageName == context.packageName }

            if (myController != activeMediaController) {
                activeMediaController?.unregisterCallback(mediaControllerCallback)
                activeMediaController = myController?.also {
                    it.registerCallback(mediaControllerCallback)
                    Log.i(TAG, "Found and registered local media controller")
                    pushCurrentState()
                }
            }

            if (myController == null && activeMediaController != null) {
                Log.i(TAG, "Local controller lost, notifying media stopped")
                activeMediaController?.unregisterCallback(mediaControllerCallback)
                activeMediaController = null
                noActiveMedia()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: No notification listener access?", e)
        }
    }

    fun handleTransportControl(action: String?) {
        Log.i(TAG, "handleTransportControl action: $action")
        if (action == null) return

        activeMediaController?.transportControls?.let { controls ->
            when (action) {
                ACTION_PLAY_PAUSE -> {
                    if (activeMediaController?.playbackState?.state == PlaybackState.STATE_PLAYING) {
                        controls.pause()
                    } else {
                        controls.play()
                    }
                }
                ACTION_SKIP_TO_NEXT -> controls.skipToNext()
                ACTION_SKIP_TO_PREVIOUS -> controls.skipToPrevious()
            }
        }
    }

    private fun pushCurrentState() {
        Log.i(TAG, "pushCurrentState")
        val newState = getUpdatedMusicState()

        if (newState != null && newState != currentState) {
            currentState = newState
            Log.i(TAG, "State updated: $newState")
            onStateUpdated(newState)
        }
    }

    private fun handleOnMetadataChanged(metadata: MediaMetadata?) {
        val newState = getUpdatedMusicState(metadata = metadata)
        if (currentState != newState) {
            pushCurrentState()
        }
    }

    private fun handleOnPlaybackStateChanged(state: PlaybackState?) {
        Log.i(TAG, "Playback state changed: ${state?.state}")
        val newState = getUpdatedMusicState(state = state)
        if (currentState != newState) {
            pushCurrentState()
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.music.vivi.ACTION_PLAY_PAUSE"
        const val ACTION_SKIP_TO_NEXT = "com.music.vivi.ACTION_SKIP_TO_NEXT"
        const val ACTION_SKIP_TO_PREVIOUS = "com.music.vivi.ACTION_SKIP_TO_PREVIOUS"
        const val REQUEST_CODE_PLAY_PAUSE = 100
        const val REQUEST_CODE_NEXT = 101
        const val REQUEST_CODE_PREVIOUS = 102
    }
}
