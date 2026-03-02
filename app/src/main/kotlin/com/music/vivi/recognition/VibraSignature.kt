package com.music.vivi.recognition

/**
 * Native library interface for generating Shazam-compatible audio fingerprints.
 * Uses the vibra_fp library which implements the Shazam signature algorithm.
 */
object VibraSignature {

    const val REQUIRED_SAMPLE_RATE = 16_000

    /**
     * Generates a Shazam signature from PCM audio data.
     * 
     * @param samples Raw PCM audio data (mono, 16-bit signed, 16kHz sample rate)
     * @return The encoded signature string suitable for Shazam API
     * @throws RuntimeException if signature generation fails
     */
    @JvmStatic
    fun fromI16(samples: ByteArray): String = ShazamSignatureGenerator.fromI16(samples)
}
