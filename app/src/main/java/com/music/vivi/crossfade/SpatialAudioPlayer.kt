package com.music.vivi.crossfade

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.*

class SpatialAudioPlayer(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var sampleRate = 44100
    private var is360Enabled = false

    // HRTF (Head-Related Transfer Function) parameters for simple spatial effect
    private val hrtfLeft = mutableListOf<Double>()
    private val hrtfRight = mutableListOf<Double>()

    // Current sound position in degrees (0 = front, 90 = right, 180 = back, 270 = left)
    private var soundPosition = 0f

    init {
        // Initialize simple HRTF coefficients
        initializeHRTF()
        setupAudioTrack()
    }

    private fun initializeHRTF() {
        // This is a simplified HRTF model for demonstration
        // In a real app, you would use proper HRTF data or a spatial audio library
        for (i in 0..359) {
            val angle = i.toDouble()
            // Simple delay and amplitude difference based on angle
            hrtfLeft.add(0.5 * (1 + cos(Math.toRadians(angle))))
            hrtfRight.add(0.5 * (1 + cos(Math.toRadians(angle - 90))))
        }
    }

    private fun setupAudioTrack() {
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build(),
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    fun enable360Audio(enable: Boolean) {
        is360Enabled = enable
        if (enable) {
            // Set audio attributes for headphones when 360 audio is enabled
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
            audioManager.setParameters("head_tracking_enabled=true")
        }
    }

    fun setSoundPosition(degrees: Float) {
        soundPosition = degrees % 360f
    }

    fun playAudio(audioData: ByteArray) {
        if (isPlaying) return

        isPlaying = true
        audioTrack?.play()

        Thread {
            val processedData = if (is360Enabled) {
                applySpatialEffect(audioData)
            } else {
                audioData
            }

            audioTrack?.write(processedData, 0, processedData.size)
        }.start()
    }

    private fun applySpatialEffect(audioData: ByteArray): ByteArray {
        // Convert byte array to short array for processing
        val shortSamples = ByteArrayToShortArray(audioData)

        // Process each sample with HRTF
        for (i in 0 until shortSamples.size / 2) {
            val leftIndex = i * 2
            val rightIndex = i * 2 + 1

            val angleIndex = (soundPosition.toInt() + 360) % 360
            val leftGain = hrtfLeft[angleIndex]
            val rightGain = hrtfRight[angleIndex]

            // Apply simple HRTF effect
            shortSamples[leftIndex] = (shortSamples[leftIndex] * leftGain).toInt().toShort()
            shortSamples[rightIndex] = (shortSamples[rightIndex] * rightGain).toInt().toShort()

            // Simulate slight delay for more realistic effect (very basic)
            if (i > 1 && angleIndex > 90 && angleIndex < 270) {
                shortSamples[leftIndex] = (shortSamples[leftIndex] * 0.9).toInt().toShort()
            } else if (i > 1) {
                shortSamples[rightIndex] = (shortSamples[rightIndex] * 0.9).toInt().toShort()
            }
        }

        return ShortArrayToByteArray(shortSamples)
    }

    fun stop() {
        isPlaying = false
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    // Helper functions for audio conversion
    private fun ByteArrayToShortArray(byteArray: ByteArray): ShortArray {
        val shortArray = ShortArray(byteArray.size / 2)
        for (i in 0 until shortArray.size) {
            val hi = byteArray[i * 2 + 1].toInt()
            val lo = byteArray[i * 2].toInt()
            shortArray[i] = ((hi shl 8) or (lo and 0xff)).toShort()
        }
        return shortArray
    }

    private fun ShortArrayToByteArray(shortArray: ShortArray): ByteArray {
        val byteArray = ByteArray(shortArray.size * 2)
        for (i in 0 until shortArray.size) {
            byteArray[i * 2] = (shortArray[i].toInt() and 0xff).toByte()
            byteArray[i * 2 + 1] = (shortArray[i].toInt() shr 8).toByte()
        }
        return byteArray
    }
}