/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.playback

import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioTagEmbedder {

    /**
     * Embeds JPEG or PNG cover artwork directly into MP4/M4A audio containers.
     * If the audio format is not supported or parsing fails, returns original bytes safely.
     */
    fun embedArtwork(audioBytes: ByteArray, artworkBytes: ByteArray, isM4a: Boolean): ByteArray {
        if (artworkBytes.isEmpty() || audioBytes.isEmpty()) return audioBytes
        if (!isM4a) return audioBytes

        return try {
            embedM4aCover(audioBytes, artworkBytes)
        } catch (e: Exception) {
            Timber.tag("AudioTagEmbedder").w(e, "Failed to embed artwork into M4A, returning original audio")
            audioBytes
        }
    }

    private fun embedM4aCover(audio: ByteArray, artwork: ByteArray): ByteArray {
        val buffer = ByteBuffer.wrap(audio).order(ByteOrder.BIG_ENDIAN)
        var moovOffset = -1
        var moovSize = 0
        var mdatOffset = -1

        // Scan top-level atoms
        while (buffer.remaining() >= 8) {
            val offset = buffer.position()
            val size = buffer.int
            val typeBytes = ByteArray(4)
            buffer.get(typeBytes)
            val type = String(typeBytes, Charsets.US_ASCII)

            val atomSize = when (size) {
                0 -> audio.size - offset
                1 -> {
                    if (buffer.remaining() >= 8) buffer.long.toInt() else return audio
                }
                else -> size
            }

            if (atomSize < 8 || offset + atomSize > audio.size) break

            if (type == "moov") {
                moovOffset = offset
                moovSize = atomSize
            } else if (type == "mdat") {
                mdatOffset = offset
            }

            buffer.position(offset + atomSize)
        }

        if (moovOffset == -1 || moovSize < 8) return audio

        // Build covr box:
        // [size: 4][covr: 4] -> [size: 4][data: 4][flag: 4 (0x0d for jpg, 0x0e for png)][locale: 4 (0)][image]
        val isPng = artwork.size >= 8 && artwork[0] == 0x89.toByte() && artwork[1] == 0x50.toByte()
        val dataTypeFlag = if (isPng) 14 else 13

        val dataPayload = ByteArrayOutputStream()
        val dataHeader = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)
        val dataBoxSize = 16 + artwork.size
        dataHeader.putInt(dataBoxSize)
        dataHeader.put("data".toByteArray(Charsets.US_ASCII))
        dataHeader.putInt(dataTypeFlag)
        dataHeader.putInt(0) // locale
        dataPayload.write(dataHeader.array())
        dataPayload.write(artwork)

        val covrBox = ByteArrayOutputStream()
        val covrHeader = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
        val covrBoxSize = 8 + dataPayload.size()
        covrHeader.putInt(covrBoxSize)
        covrHeader.put("covr".toByteArray(Charsets.US_ASCII))
        covrBox.write(covrHeader.array())
        covrBox.write(dataPayload.toByteArray())

        val covrBytes = covrBox.toByteArray()

        // Locate or create udta -> meta -> ilst inside moov
        val moovBytes = audio.copyOfRange(moovOffset, moovOffset + moovSize)
        val modifiedMoov = insertCovrIntoMoov(moovBytes, covrBytes) ?: return audio

        val deltaSize = modifiedMoov.size - moovSize

        // Adjust chunk offsets in stco / co64 if moov comes BEFORE mdat
        val finalMoov = if (moovOffset < mdatOffset && deltaSize != 0) {
            adjustChunkOffsets(modifiedMoov, deltaSize)
        } else {
            modifiedMoov
        }

        val out = ByteArrayOutputStream(audio.size + deltaSize)
        out.write(audio, 0, moovOffset)
        out.write(finalMoov)
        out.write(audio, moovOffset + moovSize, audio.size - (moovOffset + moovSize))
        return out.toByteArray()
    }

    private fun insertCovrIntoMoov(moov: ByteArray, covrBytes: ByteArray): ByteArray? {
        val buf = ByteBuffer.wrap(moov).order(ByteOrder.BIG_ENDIAN)
        buf.position(8) // Skip moov header

        var udtaOffset = -1
        var udtaSize = 0

        while (buf.remaining() >= 8) {
            val pos = buf.position()
            val size = buf.int
            val typeBytes = ByteArray(4)
            buf.get(typeBytes)
            val type = String(typeBytes, Charsets.US_ASCII)

            if (size < 8 || pos + size > moov.size) break

            if (type == "udta") {
                udtaOffset = pos
                udtaSize = size
                break
            }
            buf.position(pos + size)
        }

        val newUdta: ByteArray = if (udtaOffset != -1) {
            insertCovrIntoUdta(moov.copyOfRange(udtaOffset, udtaOffset + udtaSize), covrBytes)
        } else {
            createUdtaWithCovr(covrBytes)
        }

        val out = ByteArrayOutputStream()
        if (udtaOffset != -1) {
            out.write(moov, 0, udtaOffset)
            out.write(newUdta)
            out.write(moov, udtaOffset + udtaSize, moov.size - (udtaOffset + udtaSize))
        } else {
            out.write(moov, 0, moov.size)
            out.write(newUdta)
        }

        val result = out.toByteArray()
        ByteBuffer.wrap(result).order(ByteOrder.BIG_ENDIAN).putInt(0, result.size)
        return result
    }

    private fun insertCovrIntoUdta(udta: ByteArray, covrBytes: ByteArray): ByteArray {
        val buf = ByteBuffer.wrap(udta).order(ByteOrder.BIG_ENDIAN)
        buf.position(8)

        var metaOffset = -1
        var metaSize = 0

        while (buf.remaining() >= 8) {
            val pos = buf.position()
            val size = buf.int
            val typeBytes = ByteArray(4)
            buf.get(typeBytes)
            val type = String(typeBytes, Charsets.US_ASCII)

            if (size < 8 || pos + size > udta.size) break

            if (type == "meta") {
                metaOffset = pos
                metaSize = size
                break
            }
            buf.position(pos + size)
        }

        val newMeta: ByteArray = if (metaOffset != -1) {
            insertCovrIntoMeta(udta.copyOfRange(metaOffset, metaOffset + metaSize), covrBytes)
        } else {
            createMetaWithCovr(covrBytes)
        }

        val out = ByteArrayOutputStream()
        if (metaOffset != -1) {
            out.write(udta, 0, metaOffset)
            out.write(newMeta)
            out.write(udta, metaOffset + metaSize, udta.size - (metaOffset + metaSize))
        } else {
            out.write(udta, 0, udta.size)
            out.write(newMeta)
        }

        val result = out.toByteArray()
        ByteBuffer.wrap(result).order(ByteOrder.BIG_ENDIAN).putInt(0, result.size)
        return result
    }

    private fun insertCovrIntoMeta(meta: ByteArray, covrBytes: ByteArray): ByteArray {
        // Meta has 8-byte header + 4-byte version/flags = 12 bytes
        val buf = ByteBuffer.wrap(meta).order(ByteOrder.BIG_ENDIAN)
        if (meta.size >= 12) buf.position(12) else buf.position(8)

        var ilstOffset = -1
        var ilstSize = 0

        while (buf.remaining() >= 8) {
            val pos = buf.position()
            val size = buf.int
            val typeBytes = ByteArray(4)
            buf.get(typeBytes)
            val type = String(typeBytes, Charsets.US_ASCII)

            if (size < 8 || pos + size > meta.size) break

            if (type == "ilst") {
                ilstOffset = pos
                ilstSize = size
                break
            }
            buf.position(pos + size)
        }

        val newIlst: ByteArray = if (ilstOffset != -1) {
            val ilst = meta.copyOfRange(ilstOffset, ilstOffset + ilstSize)
            val out = ByteArrayOutputStream()
            out.write(ilst, 0, ilst.size)
            out.write(covrBytes)
            val res = out.toByteArray()
            ByteBuffer.wrap(res).order(ByteOrder.BIG_ENDIAN).putInt(0, res.size)
            res
        } else {
            val out = ByteArrayOutputStream()
            val h = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
            h.putInt(8 + covrBytes.size)
            h.put("ilst".toByteArray(Charsets.US_ASCII))
            out.write(h.array())
            out.write(covrBytes)
            out.toByteArray()
        }

        val out = ByteArrayOutputStream()
        if (ilstOffset != -1) {
            out.write(meta, 0, ilstOffset)
            out.write(newIlst)
            out.write(meta, ilstOffset + ilstSize, meta.size - (ilstOffset + ilstSize))
        } else {
            out.write(meta, 0, meta.size)
            out.write(newIlst)
        }

        val result = out.toByteArray()
        ByteBuffer.wrap(result).order(ByteOrder.BIG_ENDIAN).putInt(0, result.size)
        return result
    }

    private fun createUdtaWithCovr(covrBytes: ByteArray): ByteArray {
        val meta = createMetaWithCovr(covrBytes)
        val out = ByteArrayOutputStream()
        val h = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
        h.putInt(8 + meta.size)
        h.put("udta".toByteArray(Charsets.US_ASCII))
        out.write(h.array())
        out.write(meta)
        return out.toByteArray()
    }

    private fun createMetaWithCovr(covrBytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val h = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN)
        h.putInt(12 + 8 + covrBytes.size)
        h.put("meta".toByteArray(Charsets.US_ASCII))
        h.putInt(0) // 4 bytes version & flags

        val ilstH = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
        ilstH.putInt(8 + covrBytes.size)
        ilstH.put("ilst".toByteArray(Charsets.US_ASCII))

        out.write(h.array())
        out.write(ilstH.array())
        out.write(covrBytes)
        return out.toByteArray()
    }

    private fun adjustChunkOffsets(moov: ByteArray, delta: Int): ByteArray {
        // Find all 'stco' atoms and increment each 32-bit offset by delta
        val copy = moov.clone()
        val buf = ByteBuffer.wrap(copy).order(ByteOrder.BIG_ENDIAN)

        fun scan(offset: Int, length: Int) {
            var pos = offset
            while (pos + 8 <= offset + length) {
                buf.position(pos)
                val size = buf.int
                val typeBytes = ByteArray(4)
                buf.get(typeBytes)
                val type = String(typeBytes, Charsets.US_ASCII)
                if (size < 8 || pos + size > offset + length) break

                if (type == "stco" && size >= 16) {
                    buf.position(pos + 12) // Skip size(4), 'stco'(4), version/flags(4)
                    val count = buf.int
                    for (i in 0 until count) {
                        if (buf.remaining() < 4) break
                        val curPos = buf.position()
                        val currentOffset = buf.int
                        buf.putInt(curPos, currentOffset + delta)
                    }
                } else if (type in listOf("trak", "mdia", "minf", "stbl")) {
                    scan(pos + 8, size - 8)
                }
                pos += size
            }
        }

        scan(8, copy.size - 8)
        return copy
    }
}
