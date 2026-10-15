/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Analyses audio without changing it: every buffer is copied through untouched while its loudness feeds AudioPulse.
internal interface BeatPulseSink {
    fun publish(pulse: Float, energy: Float)
    fun reset()
}

internal object AudioPulseSink : BeatPulseSink {
    override fun publish(pulse: Float, energy: Float) = AudioPulse.publishBeat(pulse, energy)
    override fun reset() = AudioPulse.reset()
}

@UnstableApi
class BeatAnalyzerAudioProcessor internal constructor(
    private val sink: BeatPulseSink,
) : AudioProcessor {

    constructor() : this(AudioPulseSink)


    private var sampleRate = 0
    private var bytesPerFrame = 0
    private var encoding = C.ENCODING_INVALID
    private var active = false
    private var inputEnded = false
    private var streamTimeMs = 0L

    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private val detector = BeatDetector()

    companion object {
        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        bytesPerFrame = inputAudioFormat.bytesPerFrame
        encoding = inputAudioFormat.encoding
        active = sampleRate > 0
        return inputAudioFormat
    }

    override fun isActive(): Boolean = active

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return

        val rms = if (encoding == C.ENCODING_PCM_16BIT) pcm16Rms(inputBuffer) else 0f
        if (bytesPerFrame > 0 && sampleRate > 0) {
            val frames = size / bytesPerFrame
            streamTimeMs += frames * 1000L / sampleRate
        }
        val pulse = detector.onBlock(rms, streamTimeMs)
        sink.publish(pulse, rms)

        if (outputBuffer === EMPTY_BUFFER || outputBuffer.capacity() < size) {
            outputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }
        outputBuffer.put(inputBuffer)
        outputBuffer.flip()
    }

    override fun getOutput(): ByteBuffer {
        val buffer = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return buffer
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer.remaining() == 0

    @Deprecated("Deprecated in Java")
    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        inputEnded = false
        streamTimeMs = 0L
        detector.reset()
        sink.reset()
    }

    override fun reset() {
        @Suppress("DEPRECATION")
        flush()
        sampleRate = 0
        bytesPerFrame = 0
        encoding = C.ENCODING_INVALID
        active = false
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }
}
