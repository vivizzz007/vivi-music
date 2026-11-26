/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.recognition

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin

/**
 * Pure Kotlin implementation of the Shazam audio fingerprinting algorithm.
 *
 * Ported from the vibra C++ library (https://github.com/marin-m/SongRec) which implements
 * the Shazam signature algorithm using FFT-based audio fingerprinting.
 *
 * This replaces the native C++ + FFTW3 implementation with a pure JVM solution.
 */
internal object ShazamSignatureGenerator {

    private const val SAMPLE_RATE = 16_000
    private const val FFT_SIZE = 2048
    private const val FFT_STAGES = 11
    private const val FFT_OUTPUT_SIZE = FFT_SIZE / 2 + 1  // 1025
    private const val MAX_PEAKS = 255
    private const val MAX_TIME_SECONDS = 12.0

    // Spread ring buffer size
    private const val RING_BUF_SIZE = 256

    // Band IDs matching FrequencyBand enum in C++ (0=250-520Hz, 1=520-1450Hz, 2=1450-3500Hz, 3=3500-5500Hz)
    private const val BAND_250_520 = 0
    private const val BAND_520_1450 = 1
    private const val BAND_1450_3500 = 2
    private const val BAND_3500_5500 = 3

    private val TIME_SPREAD_OFFSETS = intArrayOf(-1, -3, -6)
    private val NEIGHBOR_OFFSETS = intArrayOf(-10, -7, -4, -3, 1, 2, 5, 8)
    private val OTHER_OFFSETS = intArrayOf(-53, -45, 165, 172, 179, 186, 193, 200, 214, 221, 228, 235, 242, 249)

    private val BIT_REVERSE = IntArray(FFT_SIZE) { i ->
        Integer.reverse(i) ushr (32 - FFT_STAGES)
    }

    private val TWIDDLE_RE = Array(FFT_STAGES) { stage ->
        val halfLen = 1 shl stage
        DoubleArray(halfLen) { k -> cos(-PI * k / halfLen) }
    }

    private val TWIDDLE_IM = Array(FFT_STAGES) { stage ->
        val halfLen = 1 shl stage
        DoubleArray(halfLen) { k -> sin(-PI * k / halfLen) }
    }

    /**
     * Hanning window: w[i] = 0.5 * (1 - cos(2π*(i+1)/2049)) for i=0..2047.
     *
     * This matches the precomputed HANNIG_MATRIX values in the C++ hanning.h header.
     */
    private val HANNING = DoubleArray(FFT_SIZE) { i ->
        0.5 * (1.0 - cos(2.0 * PI * (i + 1).toDouble() / 2049.0))
    }

    fun createSession(): SignatureGeneratorState = SignatureGeneratorState()

    /**
     * Generates a Shazam-compatible audio fingerprint from raw 16-bit PCM samples.
     *
     * @param samples ByteArray of mono PCM audio (16-bit signed little-endian, 16kHz)
     * @return Signature URI string (data:audio/vnd.shazam.sig;base64,...)
     */
    fun fromI16(samples: ByteArray): String {
        require(samples.size >= 2 && samples.size % 2 == 0) {
            "samples must be a non-empty byte array with even length (16-bit PCM)"
        }
        return SignatureGeneratorState().apply {
            feedI16(samples, samples.size)
        }.encodeSignature()
    }

    internal class SignatureGeneratorState {
        // Circular buffer for 2048 raw samples (as Shorts stored in Int for speed)
        private val samplesRing = IntArray(FFT_SIZE)
        private var samplesPos = 0
        private var hopCount = 0

        // Reusable FFT and spread scratch buffers to avoid per-hop allocations
        private val fftRe = DoubleArray(FFT_SIZE)
        private val fftIm = DoubleArray(FFT_SIZE)
        private val spreadScratch = DoubleArray(FFT_OUTPUT_SIZE)

        // Circular buffer of FFT magnitude outputs (RING_BUF_SIZE x FFT_OUTPUT_SIZE)
        private val fftOutputs = Array(RING_BUF_SIZE) { DoubleArray(FFT_OUTPUT_SIZE) }
        private var fftPos = 0
        private var fftNumWritten = 0

        // Circular buffer of time-spread FFT outputs (RING_BUF_SIZE x FFT_OUTPUT_SIZE)
        private val spreadFfts = Array(RING_BUF_SIZE) { DoubleArray(FFT_OUTPUT_SIZE) }
        private var spreadPos = 0
        private var spreadNumWritten = 0

        // Accumulated samples count (for signature header)
        private var numSamples = 0

        // Band → list of peaks (bands 0..3)
        private val bandPeaks = Array(4) { mutableListOf<FrequencyPeak>() }
        private var totalPeaks = 0

        val sampleDurationMs: Long
            get() = numSamples * 1000L / SAMPLE_RATE

        fun feedI16(bytes: ByteArray, length: Int = bytes.size) {
            val evenLen = length and 1.inv()
            var i = 0
            val mask = FFT_SIZE - 1
            while (i < evenLen) {
                // Match C++ stopping condition: stop when BOTH time≥max AND peaks≥max
                if (numSamples.toDouble() / SAMPLE_RATE >= MAX_TIME_SECONDS && totalPeaks >= MAX_PEAKS) {
                    break
                }
                val lo = bytes[i].toInt() and 0xFF
                val hi = bytes[i + 1].toInt()
                samplesRing[samplesPos] = (hi shl 8) or lo
                samplesPos = (samplesPos + 1) and mask
                numSamples++
                hopCount++
                if (hopCount == 128) {
                    hopCount = 0
                    doFFT()
                    doPeakSpreadingAndRecognition()
                }
                i += 2
            }
        }

        private fun doFFT() {
            val re = fftRe
            val im = fftIm
            val ring = samplesRing
            val pos = samplesPos
            val mask = FFT_SIZE - 1

            for (i in 0 until FFT_SIZE) {
                val rev = BIT_REVERSE[i]
                re[rev] = ring[(pos + i) and mask].toDouble() * HANNING[i]
                im[rev] = 0.0
            }

            for (stage in 0 until FFT_STAGES) {
                val halfLen = 1 shl stage
                val len = halfLen shl 1
                val wReArr = TWIDDLE_RE[stage]
                val wImArr = TWIDDLE_IM[stage]
                var i = 0
                while (i < FFT_SIZE) {
                    for (k in 0 until halfLen) {
                        val u = i + k
                        val v = u + halfLen
                        val wRe = wReArr[k]
                        val wIm = wImArr[k]
                        val evenRe = re[u]
                        val evenIm = im[u]
                        val oddRe = re[v] * wRe - im[v] * wIm
                        val oddIm = re[v] * wIm + im[v] * wRe
                        re[u] = evenRe + oddRe
                        im[u] = evenIm + oddIm
                        re[v] = evenRe - oddRe
                        im[v] = evenIm - oddIm
                    }
                    i += len
                }
            }

            val out = fftOutputs[fftPos]
            val scaleFactor = 1.0 / (1 shl 17)
            val minVal = 1e-10
            for (idx in 0 until FFT_OUTPUT_SIZE) {
                val r = re[idx]
                val img = im[idx]
                val mag = (r * r + img * img) * scaleFactor
                out[idx] = if (mag < minVal) minVal else mag
            }
            fftPos = (fftPos + 1) % RING_BUF_SIZE
            fftNumWritten++
        }

        private fun doPeakSpreadingAndRecognition() {
            doPeakSpreading()
            if (spreadNumWritten >= 47) {
                doPeakRecognition()
            }
        }

        private fun doPeakSpreading() {
            // Start with a copy of the last FFT output
            val lastFftIdx = (fftPos - 1 + RING_BUF_SIZE) % RING_BUF_SIZE
            val spread = spreadScratch
            fftOutputs[lastFftIdx].copyInto(spread)

            // Frequency spreading: 3-point running max (in-place, forward pass)
            for (pos in 0 until FFT_OUTPUT_SIZE - 2) {
                spread[pos] = maxOf(spread[pos], spread[pos + 1], spread[pos + 2])
            }

            // Time spreading: propagate max to/from older spread entries at offsets -1, -3, -6
            // Only older entries are updated; the new entry keeps only frequency spreading (matches C++).
            for (pos in 0 until FFT_OUTPUT_SIZE) {
                var maxVal = spread[pos]
                for (offset in TIME_SPREAD_OFFSETS) {
                    val idx = ((spreadPos + offset) % RING_BUF_SIZE + RING_BUF_SIZE) % RING_BUF_SIZE
                    val oldVal = spreadFfts[idx][pos]
                    if (oldVal > maxVal) maxVal = oldVal
                    spreadFfts[idx][pos] = maxVal
                }
            }

            spread.copyInto(spreadFfts[spreadPos])
            spreadPos = (spreadPos + 1) % RING_BUF_SIZE
            spreadNumWritten++
        }

        private fun doPeakRecognition() {
            val fftMinus46 = fftOutputs[(fftPos - 46 + RING_BUF_SIZE * 2) % RING_BUF_SIZE]
            val spreadMinus49 = spreadFfts[(spreadPos - 49 + RING_BUF_SIZE * 2) % RING_BUF_SIZE]

            for (binPos in 10 until FFT_OUTPUT_SIZE - 8) {
                val fftVal = fftMinus46[binPos]
                if (fftVal < 1.0 / 64.0 || fftVal < spreadMinus49[binPos]) continue

                // Check 8 neighbors in spreadMinus49
                var maxNeighborSpread49 = 0.0
                for (neighborOffset in NEIGHBOR_OFFSETS) {
                    val v = spreadMinus49[binPos + neighborOffset]
                    if (v > maxNeighborSpread49) maxNeighborSpread49 = v
                }
                if (fftVal <= maxNeighborSpread49) continue

                // Check 14 other spread FFT offsets
                var maxNeighborOther = maxNeighborSpread49
                for (otherOffset in OTHER_OFFSETS) {
                    val spreadIdx = ((spreadPos + otherOffset) % RING_BUF_SIZE + RING_BUF_SIZE) % RING_BUF_SIZE
                    val v = spreadFfts[spreadIdx][binPos - 1]
                    if (v > maxNeighborOther) maxNeighborOther = v
                }
                if (fftVal <= maxNeighborOther) continue

                // Valid peak found: compute corrected bin and frequency
                val fftNumber = spreadNumWritten - 46

                val peakMag = ln(max(1.0 / 64.0, fftVal)) * 1477.3 + 6144
                val peakMagBefore = ln(max(1.0 / 64.0, fftMinus46[binPos - 1])) * 1477.3 + 6144
                val peakMagAfter = ln(max(1.0 / 64.0, fftMinus46[binPos + 1])) * 1477.3 + 6144

                val peakVariation1 = peakMag * 2 - peakMagBefore - peakMagAfter
                val peakVariation2 = (peakMagAfter - peakMagBefore) * 32 / peakVariation1

                val correctedBin = binPos * 64.0 + peakVariation2
                val frequencyHz = correctedBin * (16000.0 / 2.0 / 1024.0 / 64.0)

                val band = when {
                    frequencyHz < 250.0  -> continue
                    frequencyHz < 520.0  -> BAND_250_520
                    frequencyHz < 1450.0 -> BAND_520_1450
                    frequencyHz < 3500.0 -> BAND_1450_3500
                    frequencyHz <= 5500.0 -> BAND_3500_5500
                    else -> continue
                }

                bandPeaks[band].add(
                    FrequencyPeak(
                        fftPassNumber = fftNumber,
                        peakMagnitude = peakMag.toInt(),
                        correctedPeakFrequencyBin = correctedBin.toInt()
                    )
                )
                totalPeaks++
            }
        }

        internal fun encodeSignature(): String {
            val contentsStream = ByteArrayOutputStream()

            // Write each frequency band's peaks in ascending band order (matches C++ std::map iteration)
            for (bandId in 0..3) {
                val peaks = bandPeaks[bandId]
                if (peaks.isEmpty()) continue

                val peakBuf = ByteArrayOutputStream()
                var prevFftPassNumber = 0

                for (peak in peaks) {
                    val diff = peak.fftPassNumber - prevFftPassNumber
                    if (diff >= 255) {
                        // Encode absolute position with 0xFF marker
                        peakBuf.write(0xFF)
                        writeLittleEndian32(peakBuf, peak.fftPassNumber)
                        prevFftPassNumber = peak.fftPassNumber
                    }
                    peakBuf.write(peak.fftPassNumber - prevFftPassNumber)
                    writeLittleEndian16(peakBuf, peak.peakMagnitude)
                    writeLittleEndian16(peakBuf, peak.correctedPeakFrequencyBin)
                    prevFftPassNumber = peak.fftPassNumber
                }

                val peakBytes = peakBuf.toByteArray()

                // Band tag: 0x60030040 + bandId
                writeLittleEndian32(contentsStream, 0x60030040 + bandId)
                writeLittleEndian32(contentsStream, peakBytes.size)
                contentsStream.write(peakBytes)

                // Pad to 4-byte alignment
                val padBytes = (4 - peakBytes.size % 4) % 4
                repeat(padBytes) { contentsStream.write(0) }
            }

            val contents = contentsStream.toByteArray()
            val sizeMinusHeader = contents.size + 8
            val samplesAndOffset = (numSamples + SAMPLE_RATE * 0.24).toInt()

            // Build 48-byte header struct (all fields little-endian)
            val headerBytes = ByteBuffer.allocate(48).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(0xcafe2580.toInt())     // magic1
                putInt(0)                      // crc32 placeholder
                putInt(sizeMinusHeader)        // size_minus_header
                putInt(0x94119c00.toInt())     // magic2
                putInt(0); putInt(0); putInt(0) // void1[3]
                putInt(3 shl 27)               // shifted_sample_rate_id
                putInt(0); putInt(0)           // void2[2]
                putInt(samplesAndOffset)       // number_samples_plus_divided_sample_rate
                putInt((15 shl 19) + 0x40000) // fixed_value
            }.array()

            // Assemble full buffer: header(48) + 0x40000000(4) + sizeMinusHeader(4) + contents
            val fullBuf = ByteArrayOutputStream(56 + contents.size)
            fullBuf.write(headerBytes)
            writeLittleEndian32(fullBuf, 0x40000000)
            writeLittleEndian32(fullBuf, contents.size + 8)
            fullBuf.write(contents)

            val fullBytes = fullBuf.toByteArray()

            // CRC32 over bytes from offset 8 to end (skipping magic1 and the crc32 field itself)
            val crc = CRC32()
            crc.update(fullBytes, 8, fullBytes.size - 8)
            val crc32Value = crc.value.toInt()

            // Write CRC32 at offset 4 (little-endian)
            fullBytes[4] = (crc32Value and 0xFF).toByte()
            fullBytes[5] = ((crc32Value shr 8) and 0xFF).toByte()
            fullBytes[6] = ((crc32Value shr 16) and 0xFF).toByte()
            fullBytes[7] = ((crc32Value shr 24) and 0xFF).toByte()

            val base64 = Base64.encodeToString(fullBytes, Base64.NO_WRAP)
            return "data:audio/vnd.shazam.sig;base64,$base64"
        }
    }

    private data class FrequencyPeak(
        val fftPassNumber: Int,
        val peakMagnitude: Int,
        val correctedPeakFrequencyBin: Int
    )

    private fun writeLittleEndian32(out: ByteArrayOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
        out.write((value ushr 16) and 0xFF)
        out.write((value ushr 24) and 0xFF)
    }

    private fun writeLittleEndian16(out: ByteArrayOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
    }
}
