package com.music.vivi.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(UnstableApi::class)
class BassBeatDetectionTest {
    private val sampleRate = 44100
    private val blockFrames = 1024

    private fun sine(
        frequencyHz: Float,
        amplitude: Float,
        frames: Int,
        channels: Int = 1,
        startFrame: Int = 0,
    ): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(frames * channels * 2).order(ByteOrder.nativeOrder())
        repeat(frames) { i ->
            val t = (startFrame + i).toDouble() / sampleRate
            val sample = (sin(2 * PI * frequencyHz * t) * amplitude * Short.MAX_VALUE).roundToInt().toShort()
            repeat(channels) { buffer.putShort(sample) }
        }
        buffer.flip()
        return buffer
    }

    private fun steadyRms(meter: BassLevelMeter, frequencyHz: Float, amplitude: Float, channels: Int = 1): Float {
        // The first blocks let the filter settle, so the reading reflects the steady-state gain.
        var frame = 0
        repeat(4) {
            meter.rms(sine(frequencyHz, amplitude, blockFrames, channels, frame))
            frame += blockFrames
        }
        return meter.rms(sine(frequencyHz, amplitude, blockFrames, channels, frame))
    }

    private class RecordingSink : BeatPulseSink {
        val pulses = mutableListOf<Float>()
        override fun publish(pulse: Float, energy: Float) {
            pulses += pulse
        }
        override fun reset() = Unit
    }

    @Test
    fun beatStrengthIsZeroAtOrBelowThreshold() {
        assertEquals(0f, beatStrength(BEAT_THRESHOLD_RATIO, BEAT_THRESHOLD_RATIO), 0f)
        assertEquals(0f, beatStrength(1f, BEAT_THRESHOLD_RATIO), 0f)
    }

    @Test
    fun beatStrengthIsFullAtAndAboveFullStrengthRatio() {
        assertEquals(1f, beatStrength(BEAT_FULL_STRENGTH_RATIO, BEAT_THRESHOLD_RATIO), 1e-6f)
        assertEquals(1f, beatStrength(10f, BEAT_THRESHOLD_RATIO), 0f)
    }

    @Test
    fun beatStrengthGrowsLinearlyBetweenThresholdAndFull() {
        val midpoint = (BEAT_THRESHOLD_RATIO + BEAT_FULL_STRENGTH_RATIO) / 2
        assertEquals(0.5f, beatStrength(midpoint, BEAT_THRESHOLD_RATIO), 1e-5f)
    }

    @Test
    fun beatStrengthWithThresholdAboveFullRatioIsAllOrNothing() {
        assertEquals(1f, beatStrength(3.1f, thresholdRatio = 3f), 0f)
        assertEquals(0f, beatStrength(2.9f, thresholdRatio = 3f), 0f)
    }

    @Test
    fun gentleSwellGivesMuchWeakerPulseThanDrumHit() {
        val gentle = BeatDetector()
        val drum = BeatDetector()
        repeat(20) {
            gentle.onBlock(rms = 0.1f, timeMs = it * 20L)
            drum.onBlock(rms = 0.1f, timeMs = it * 20L)
        }

        val swell = gentle.onBlock(rms = 0.135f, timeMs = 400L)
        val hit = drum.onBlock(rms = 0.5f, timeMs = 400L)

        assertTrue(swell > 0f)
        assertTrue(swell < BEAT_PEAK * 0.2f)
        assertEquals(BEAT_PEAK, hit, 0f)
    }

    @Test
    fun bassMeterPassesLowFrequencies() {
        val rms = steadyRms(BassLevelMeter(sampleRate, 1), frequencyHz = 60f, amplitude = 0.5f)

        // A 0.5 amplitude sine has RMS ~0.354; two 150 Hz poles keep most of a 60 Hz tone.
        assertTrue("rms=$rms", rms > 0.25f)
    }

    @Test
    fun bassMeterRejectsHighFrequencies() {
        val rms = steadyRms(BassLevelMeter(sampleRate, 1), frequencyHz = 3000f, amplitude = 0.5f)

        assertTrue("rms=$rms", rms < 0.01f)
    }

    @Test
    fun bassMeterAveragesStereoChannelsToMono() {
        val mono = steadyRms(BassLevelMeter(sampleRate, 1), frequencyHz = 60f, amplitude = 0.5f)
        val stereo = steadyRms(BassLevelMeter(sampleRate, 2), frequencyHz = 60f, amplitude = 0.5f, channels = 2)

        assertEquals(mono, stereo, 1e-4f)
    }

    @Test
    fun bassMeterReportsZeroForSilenceAndEmptyBuffers() {
        val meter = BassLevelMeter(sampleRate, 2)

        assertEquals(0f, meter.rms(ByteBuffer.allocate(0)), 0f)
        assertEquals(0f, meter.rms(sine(60f, 0f, blockFrames, channels = 2)), 0f)
    }

    @Test
    fun bassMeterDoesNotConsumeTheInputBuffer() {
        val buffer = sine(60f, 0.5f, blockFrames)

        BassLevelMeter(sampleRate, 1).rms(buffer)

        assertEquals(blockFrames * 2, buffer.remaining())
    }

    @Test
    fun bassMeterResetClearsFilterMemory() {
        val meter = BassLevelMeter(sampleRate, 1)
        steadyRms(meter, frequencyHz = 60f, amplitude = 0.9f)
        meter.reset()

        assertEquals(0f, meter.rms(sine(60f, 0f, blockFrames)), 0f)
    }

    @Test
    fun loudTrebleBurstDoesNotTriggerBeat() {
        val sink = RecordingSink()
        val processor = BeatAnalyzerAudioProcessor(sink)
        processor.configure(AudioProcessor.AudioFormat(sampleRate, 1, C.ENCODING_PCM_16BIT))
        var frame = 0
        repeat(20) {
            processor.queueInput(sine(60f, 0.1f, blockFrames, startFrame = frame))
            processor.getOutput()
            frame += blockFrames
        }

        // A piano-like high note far louder than the quiet bass bed.
        val burst = sine(2000f, 0.9f, blockFrames, startFrame = frame)
        repeat(blockFrames) { i ->
            val bed = (sin(2 * PI * 60f * (frame + i).toDouble() / sampleRate) * 0.1f * Short.MAX_VALUE)
            burst.putShort(i * 2, (burst.getShort(i * 2) * 0.9f + bed).roundToInt().toShort())
        }
        processor.queueInput(burst)

        assertEquals(0f, sink.pulses.last(), 1e-3f)
    }

    @Test
    fun loudBassHitTriggersBeat() {
        val sink = RecordingSink()
        val processor = BeatAnalyzerAudioProcessor(sink)
        processor.configure(AudioProcessor.AudioFormat(sampleRate, 1, C.ENCODING_PCM_16BIT))
        var frame = 0
        repeat(20) {
            processor.queueInput(sine(60f, 0.1f, blockFrames, startFrame = frame))
            processor.getOutput()
            frame += blockFrames
        }

        processor.queueInput(sine(60f, 0.6f, blockFrames, startFrame = frame))

        assertTrue("pulse=${sink.pulses.last()}", sink.pulses.last() > BEAT_PEAK * 0.9f)
    }

    @Test
    fun flushClearsBassFilterSoNextStreamStartsClean() {
        val sink = RecordingSink()
        val processor = BeatAnalyzerAudioProcessor(sink)
        processor.configure(AudioProcessor.AudioFormat(sampleRate, 1, C.ENCODING_PCM_16BIT))
        processor.queueInput(sine(60f, 0.9f, blockFrames))
        @Suppress("DEPRECATION")
        processor.flush()

        processor.queueInput(sine(60f, 0f, blockFrames))

        assertEquals(0f, sink.pulses.last(), 0f)
    }
}
