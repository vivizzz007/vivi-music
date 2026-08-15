package com.music.vivi.desktop.player

import net.sourceforge.jaad.aac.Decoder
import net.sourceforge.jaad.aac.SampleBuffer
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.containers.mp4.MP4Util
import org.jcodec.containers.mp4.boxes.MovieFragmentBox
import org.jcodec.containers.mp4.boxes.NodeBox
import org.jcodec.containers.mp4.boxes.TrackFragmentBox
import org.jcodec.containers.mp4.boxes.TrackFragmentHeaderBox
import org.jcodec.containers.mp4.boxes.TrunBox
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack
import org.jcodec.containers.mp4.demuxer.MP4Demuxer
import org.jcodec.containers.mp4.demuxer.MP4DemuxerTrackMeta
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/**
 * Self-contained AAC player: downloads the MP4 stream to a local cache file,
 * demuxes the (fragmented/DASH) MP4 container with `jcodec`, decodes the raw
 * AAC frames to PCM with the bundled `jaad` decoder, and plays them through
 * Java Sound. No native libraries or external binaries are required.
 *
 * YouTube serves its `audio/mp4` streams as *fragmented* MP4 (fMP4, `ftyp`
 * brand "dash"): the `moov` sample table is empty and the real samples live in
 * `moof`/`trun` boxes, which `jaad`'s own `MP4Container` demuxer does not
 * understand. This player walks the `moof` fragments directly.
 *
 * Every failure stage reports a human-readable message through [onError]
 * instead of failing silently, so playback problems are visible in the UI.
 */
class AudioPlayer {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val cacheDir =
        File(System.getProperty("user.home"), ".vivimusic/cache/audio").apply { mkdirs() }

    private var thread: Thread? = null

    /** Incremented on every (re)start; stale threads ignore their callbacks. */
    private var generation = 0

    @Volatile private var paused = false
    @Volatile private var stopped = false
    @Volatile private var volume = 1f
    private val lock = Object()

    @Volatile private var line: SourceDataLine? = null

    private var onPosition: ((Long) -> Unit)? = null
    private var onDuration: ((Long) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var onComplete: (() -> Unit)? = null

    private var currentUrl: String? = null
    private var currentCacheKey: String? = null

    /**
     * Starts playing [url] on a background thread. [cacheKey] names the local
     * cache file (use a stable id such as the videoId so repeats/seeks don't
     * re-download). [onPosition] reports decoded position, [onDuration] the
     * total track length, [onError] a human-readable failure reason, and
     * [onComplete] fires when the stream ends or is stopped.
     */
    fun play(
        url: String,
        cacheKey: String,
        startAtMs: Long = 0L,
        startPaused: Boolean = false,
        onPosition: (Long) -> Unit,
        onDuration: (Long) -> Unit,
        onError: (String) -> Unit,
        onComplete: () -> Unit,
    ) {
        this.onPosition = onPosition
        this.onDuration = onDuration
        this.onError = onError
        this.onComplete = onComplete
        startDecode(url, cacheKey, startAtMs, startPaused)
    }

    /** Seeks to [ms] by restarting decode from the cached file. */
    fun seekTo(ms: Long) {
        val url = currentUrl ?: return
        val key = currentCacheKey ?: return
        startDecode(url, key, ms.coerceAtLeast(0L), startPaused = false)
    }

    /** Sets playback volume in the 0f..1f range. */
    fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
    }

    fun pause() {
        paused = true
        line?.stop()
    }

    fun resume() {
        paused = false
        synchronized(lock) { lock.notifyAll() }
        line?.start()
    }

    fun stop() {
        stopped = true
        paused = false
        synchronized(lock) { lock.notifyAll() }
        runCatching { line?.stop() }
        runCatching { line?.close() }
        line = null
    }

    private fun startDecode(url: String, cacheKey: String, startAtMs: Long, startPaused: Boolean) {
        // Invalidate any running thread and reset the play flags.
        val gen = ++generation
        stopped = true
        synchronized(lock) { lock.notifyAll() }
        runCatching { line?.stop() }
        runCatching { line?.close() }
        line = null
        paused = startPaused
        stopped = false
        currentUrl = url
        currentCacheKey = cacheKey

        thread = Thread {
            try {
                val file = ensureDownloaded(url, cacheKey)
                decodeAndPlay(file, gen, startAtMs)
            } catch (e: Exception) {
                if (gen == generation) {
                    onError?.invoke(e.message ?: e::class.simpleName ?: "Unknown playback error")
                }
            } finally {
                if (gen == generation) onComplete?.invoke()
            }
        }.apply {
            isDaemon = true
            name = "vivimusic-audio"
            start()
        }
    }

    private fun ensureDownloaded(url: String, cacheKey: String): File {
        val safe = cacheKey.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(cacheDir, "$safe.m4a")
        if (file.exists() && file.length() > 0) return file
        val part = File(cacheDir, "$safe.m4a.part")
        if (part.exists()) part.delete()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} downloading audio")
            val body = response.body ?: throw IOException("Empty audio response body")
            part.outputStream().use { out -> body.byteStream().copyTo(out) }
        }

        if (part.length() <= 0) {
            part.delete()
            throw IOException("Downloaded audio file is empty")
        }
        if (!part.renameTo(file)) {
            part.copyTo(file, overwrite = true)
            part.delete()
        }
        return file
    }

    /**
     * Walks the `moof`/`trun` boxes of a fragmented MP4 and returns the
     * absolute file offset + size of every raw AAC sample of [trackId], in
     * decode order. YouTube fMP4 sets `trun.data_offset` relative to the start
     * of the enclosing `moof`, and stores the samples contiguously, so the
     * sample offset is `moofOffset + dataOffset + sum(previous sizes)`.
     */
    private fun collectAacSamples(channel: SeekableByteChannel, trackId: Int): List<Pair<Long, Int>> {
        val samples = mutableListOf<Pair<Long, Int>>()
        for (atom in MP4Util.getRootAtoms(channel)) {
            if (atom.header.fourcc != "moof") continue
            val moof = atom.parseBox(channel) as MovieFragmentBox
            for (traf in moof.tracks) {
                val tfhd = NodeBox.findFirst(traf, TrackFragmentHeaderBox::class.java, "tfhd") ?: continue
                if (tfhd.trackId != trackId) continue
                val trun = NodeBox.findFirst(traf, TrunBox::class.java, "trun") ?: continue
                val base = atom.offset + (if (trun.isDataOffsetAvailable) trun.dataOffset.toLong() else 0L)
                var offset = base
                for (size in trun.sampleSizes) {
                    samples.add(offset to size)
                    offset += size
                }
            }
        }
        return samples
    }

    private fun decodeAndPlay(file: File, gen: Int, startAtMs: Long) {
        NIOUtils.readableChannel(file).use { channel ->
            val demuxer = MP4Demuxer.createMP4Demuxer(channel)
            val track = demuxer.audioTracks.firstOrNull() as? AbstractMP4DemuxerTrack
                ?: throw IOException("No audio track found in stream")

            // Total duration in seconds (jcodec reads it from mvhd/mdhd, which
            // is populated even for fragmented files).
            runCatching { track.meta.totalDuration }
                .getOrNull()
                ?.takeIf { it > 0 }
                ?.let { seconds -> if (gen == generation) onDuration?.invoke((seconds * 1000).toLong()) }

            val dsi = MP4DemuxerTrackMeta.getCodecPrivate(track)
                ?: throw IOException("No AAC decoder info found in stream")
            val decoder = Decoder(NIOUtils.toArray(dsi))
            val buffer = SampleBuffer()

            val trackId = track.box.trackHeader.trackId
            val samples = collectAacSamples(channel, trackId)
            if (samples.isEmpty()) throw IOException("No audio frames to decode")

            fun decodeAt(index: Int) {
                val (offset, size) = samples[index]
                channel.setPosition(offset)
                val raw = ByteArray(size)
                val bb = ByteBuffer.wrap(raw)
                while (bb.hasRemaining()) {
                    if (channel.read(bb) < 0) break
                }
                decoder.decodeFrame(raw, buffer)
            }

            // Decode the first frame to learn the PCM format and open the line.
            decodeAt(0)
            val format = AudioFormat(
                buffer.sampleRate.toFloat(),
                buffer.bitsPerSample,
                buffer.channels,
                true,
                buffer.isBigEndian,
            )
            val out = AudioSystem.getSourceDataLine(format)
                ?: throw IOException("No audio output device supports $format")
            line = out
            out.open(format, 8192)
            out.start()

            val bigEndian = buffer.isBigEndian
            val bitsPerSample = buffer.bitsPerSample
            val targetSeconds = startAtMs / 1000.0
            var elapsedSeconds = 0.0

            fun emit() {
                // Decode-and-discard frames until the seek target is reached.
                if (elapsedSeconds + buffer.length >= targetSeconds) {
                    if (!paused) {
                        val data = if (volume < 0.999f && bitsPerSample == 16) {
                            scale16(buffer.data, volume, bigEndian)
                        } else {
                            buffer.data
                        }
                        var written = 0
                        while (written < data.size) {
                            val n = out.write(data, written, data.size - written)
                            if (n <= 0) break
                            written += n
                        }
                    }
                    if (gen == generation) {
                        onPosition?.invoke(((elapsedSeconds + buffer.length) * 1000).toLong())
                    }
                }
                elapsedSeconds += buffer.length
            }
            emit()

            var index = 0
            while (index < samples.size - 1) {
                synchronized(lock) {
                    while (paused && !stopped) lock.wait()
                }
                if (stopped || gen != generation) break
                index++
                decodeAt(index)
                emit()
            }

            out.drain()
            out.stop()
            out.close()
            if (line === out) line = null
        }
    }

    /** Scales 16-bit PCM samples by [gain] (0..1), honoring [bigEndian] order. */
    private fun scale16(data: ByteArray, gain: Float, bigEndian: Boolean): ByteArray {
        if (gain >= 0.999f) return data
        val n = data.size / 2
        val out = ByteArray(data.size)
        for (i in 0 until n) {
            val hi: Int
            val lo: Int
            if (bigEndian) {
                hi = data[2 * i].toInt() and 0xFF
                lo = data[2 * i + 1].toInt() and 0xFF
            } else {
                lo = data[2 * i].toInt() and 0xFF
                hi = data[2 * i + 1].toInt() and 0xFF
            }
            var s = (hi shl 8) or lo
            if (s >= 0x8000) s -= 0x10000 // sign-extend to signed 16-bit
            s = (s * gain).toInt().coerceIn(-32768, 32767)
            val u = s and 0xFFFF
            if (bigEndian) {
                out[2 * i] = (u shr 8).toByte()
                out[2 * i + 1] = u.toByte()
            } else {
                out[2 * i] = u.toByte()
                out[2 * i + 1] = (u shr 8).toByte()
            }
        }
        return out
    }
}
