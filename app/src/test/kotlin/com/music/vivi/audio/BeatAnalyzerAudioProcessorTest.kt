package com.music.vivi.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(UnstableApi::class)
class BeatAnalyzerAudioProcessorTest {
    private class RecordingSink : BeatPulseSink {
        val published = mutableListOf<Pair<Float, Float>>()
        var resets = 0

        override fun publish(pulse: Float, energy: Float) {
            published += pulse to energy
        }

        override fun reset() {
            resets++
        }
    }

    private val stereo16 = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)

    private fun pcm(vararg samples: Short): ByteBuffer =
        ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.nativeOrder()).apply {
            samples.forEach { putShort(it) }
            flip()
        }

    private fun drain(processor: AudioProcessor): ByteArray {
        val output = processor.getOutput()
        val bytes = ByteArray(output.remaining())
        output.get(bytes)
        return bytes
    }

    private fun bytesOf(buffer: ByteBuffer): ByteArray {
        val copy = buffer.duplicate()
        val bytes = ByteArray(copy.remaining())
        copy.get(bytes)
        return bytes
    }

    @Test
    fun processorIsInactiveUntilConfigured() {
        val processor = BeatAnalyzerAudioProcessor(RecordingSink())

        assertFalse(processor.isActive())
    }

    @Test
    fun configureActivatesProcessorAndKeepsPcm16Format() {
        val processor = BeatAnalyzerAudioProcessor(RecordingSink())

        val output = processor.configure(stereo16)

        assertTrue(processor.isActive())
        assertEquals(stereo16, output)
    }

    @Test
    fun configureWithoutSampleRateKeepsProcessorInactive() {
        val processor = BeatAnalyzerAudioProcessor(RecordingSink())

        processor.configure(AudioProcessor.AudioFormat(0, 2, C.ENCODING_PCM_16BIT))

        assertFalse(processor.isActive())
    }

    @Test
    fun passthroughOutputIsByteIdenticalToInput() {
        val processor = BeatAnalyzerAudioProcessor(RecordingSink())
        processor.configure(stereo16)
        val input = pcm(100, -200, 300, -400, 32767, -32768)
        val expected = bytesOf(input)

        processor.queueInput(input)

        assertArrayEquals(expected, drain(processor))
    }

    @Test
    fun queueInputConsumesWholeInputBuffer() {
        val processor = BeatAnalyzerAudioProcessor(RecordingSink())
        processor.configure(stereo16)
        val input = pcm(1, 2, 3, 4)

        processor.queueInput(input)

        assertEquals(0, input.remaining())
    }

    @Test
    fun emptyInputProducesNoOutputAndNoPulse() {
        val sink = RecordingSink()
        val processor = BeatAnalyzerAudioProcessor(sink)
        processor.configure(stereo16)

        processor.queueInput(ByteBuffer.allocate(0))

        assertEquals(0, processor.getOutput().remaining())
        assertTrue(sink.published.isEmpty())
    }

    @Test
    fun getOutputReturnsEmptyBufferAfterBeingDrained() {
        val processor = BeatAnalyzerAudioProcessor(RecordingSink())
        processor.configure(stereo16)
        processor.queueInput(pcm(5, 5))
        drain(processor)

        assertEquals(0, processor.getOutput().remaining())
    }

    @Test
    fun fullScaleBlockReportsEnergyNearOne() {
        val sink = RecordingSink()
        val processor = BeatAnalyzerAudioProcessor(sink)
        processor.configure(stereo16)

        processor.queueInput(pcm(Short.MAX_VALUE, Short.MIN_VALUE, Short.MAX_VALUE, Short.MIN_VALUE))

        assertEquals(1f, sink.published.single().second, 1e-3f)
    }

    @Test
    fun silentBlockReportsZeroEnergy() {
        val sink = RecordingSink()
        val processor = BeatAnalyzerAudioProcessor(sink)
        processor.configure(stereo16)

        processor.queueInput(pcm(0, 0, 0, 0))

        assertEquals(0f, sink.published.single().second, 0f)
    }

    @Test
    fun nonPcm16FormatStaysPassthroughAndReportsZeroEnergy() {
        val sink = RecordingSink()
        val processor = BeatAnalyzerAudioProcessor(sink)
        val floatFormat = AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_FLOAT)
        assertEquals(floatFormat, processor.configure(floatFormat))
        val floatBytes = ByteBuffer.allocate(8).order(ByteOrder.nativeOrder()).putFloat(0.5f).putFloat(-0.5f)
        floatBytes.flip()
        val expected = bytesOf(floatBytes)

        processor.queueInput(floatBytes)

        assertArrayEquals(expected, drain(processor))
        assertEquals(0f, sink.published.single().second, 0f)
    }

    @Test
    fun flushResetsSinkAndDropsPendingOutput() {
        val sink = RecordingSink()
        val processor = BeatAnalyzerAudioProcessor(sink)
        processor.configure(stereo16)
        processor.queueInput(pcm(10, 10))

        processor.flush()

        assertEquals(1, sink.resets)
        assertEquals(0, processor.getOutput().remaining())
    }

    @Test
    fun resetDeactivatesProcessorAndResetsSink() {
        val sink = RecordingSink()
        val processor = BeatAnalyzerAudioProcessor(sink)
        processor.configure(stereo16)

        processor.reset()

        assertFalse(processor.isActive())
        assertEquals(1, sink.resets)
    }

    @Test
    fun endOfStreamOnlyEndsProcessorOnceOutputIsDrained() {
        val processor = BeatAnalyzerAudioProcessor(RecordingSink())
        processor.configure(stereo16)
        processor.queueInput(pcm(1, 2))
        processor.queueEndOfStream()

        assertFalse(processor.isEnded())

        drain(processor)

        assertTrue(processor.isEnded())
    }

    @Test
    fun publishesOneEnergyValuePerQueuedBlock() {
        val sink = RecordingSink()
        val processor = BeatAnalyzerAudioProcessor(sink)
        processor.configure(stereo16)

        repeat(5) {
            processor.queueInput(pcm(1000, -1000))
            drain(processor)
        }

        assertEquals(5, sink.published.size)
    }
}
