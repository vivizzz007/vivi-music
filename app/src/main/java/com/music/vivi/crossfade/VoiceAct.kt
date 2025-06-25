package com.music.vivi.crossfade

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.music.vivi.R

import androidx.activity.ComponentActivity


class VoiceAct(private val activity: ComponentActivity) {
    private var onVoiceResult: ((String) -> Unit)? = null
    private var onPermissionDenied: (() -> Unit)? = null

    // Permission launcher
    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceRecognition()
        } else {
            onPermissionDenied?.invoke()
        }
    }

    // Voice recognition launcher
    private val voiceRecognitionLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                results?.firstOrNull()?.let { spokenText ->
                    onVoiceResult?.invoke(spokenText)
                }
            }
        }

    fun startVoiceSearch(
        onResult: (String) -> Unit,
        onPermissionDenied: () -> Unit = { /* default empty */ }
    ) {
        this.onVoiceResult = onResult
        this.onPermissionDenied = onPermissionDenied

        when {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startVoiceRecognition()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Speak now..."
            )
        }

        try {
            voiceRecognitionLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            onPermissionDenied?.invoke()
            Toast.makeText(
                activity,
                "Voice search not supported on this device",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}