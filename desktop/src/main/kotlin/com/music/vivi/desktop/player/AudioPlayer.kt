package com.music.vivi.desktop.player

import net.sourceforge.jaad.aac.Decoder
import net.sourceforge.jaad.aac.SampleBuffer
import net.sourceforge.jaad.mp4.MP4Container
import net.sourceforge.jaad.mp4.api.AudioTrack
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/**
 * Self-contained AAC player: downloads the MP4 stream to a local cache file,
 * demuxes it with `jaad`, decodes AAC frames to PCM, and plays them through
 * Java Sound. No native libraries or external binaries are required.
 *
 * Caching to a seekable file is what unlocks duration reporting and seek:
 * `Movie.getDuration()` and re-reading from a random position both need a
 * `RandomAccessFile`-backed container (streaming `InputStream`s can't seek).
 */
class AudioPlayer {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
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
    private var onComplete: (() -> Unit)? = null

    private var currentUrl: String? = null
    private var currentCacheKey: String? = null
    @Volatile private var currentFile: File? = null

    /**
     * Starts playing [url] on a background thread. [cacheKey] names the local
     * cache file (use a stable id such as the videoId so repeats/seeks don't
     * re-download). [onPosition] reports decoded position, [onDuration] the
     * total track length, and [onComplete] fires when the stream ends or is
     * stopped.
     */
    fun play(
        url: String,
        cacheKey: String,
        startAtMs: Long = 0L,
        startPaused: Boolean = false,
        onPosition: (Long) -> Unit,
        onDuration: (Long) -> Unit,
        onComplete: () -> Unit,
    ) {
        this.onPosition = onPosition
        this.onDuration = onDuration
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
                val file = currentFile ?: ensureDownloaded(url, cacheKey)
                decodeAndPlay(file, gen, startAtMs)
            } catch (_: Exception) {
                // fall through to onComplete
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
        if (file.exists() && file.length() > 0) {
            currentFile = file
            return file
        }
        val part = File(cacheDir, "$safe.m4a.part")
        if (part.exists()) part.delete()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty response body")
            part.outputStream().use { out -> body.byteStream().copyTo(out) }
        }

        currentFile = if (part.renameTo(file)) file else part
        return currentFile!!
    }

    private fun decodeAndPlay(file: File, gen: Int, startAtMs: Long) {
        RandomAccessFile(file, "r").use { raf ->
            val container = MP4Container(raf)
            val movie = container.movie ?: return
            val track = movie.tracks.firstOrNull { it is AudioTrack } as? AudioTrack ?: return

            runCatching { movie.getDuration() }
                .getOrNull()
                ?.takeIf { it > 0 }
                ?.let { seconds -> if (gen == generation) onDuration?.invoke((seconds * 1000).toLong()) }

            val decoder = Decoder(track.decoderSpecificInfo)
            val buffer = SampleBuffer()

            var frame = track.readNextFrame() ?: return
            decoder.decodeFrame(frame.data, buffer)

            val format = AudioFormat(
                buffer.sampleRate.toFloat(),
                buffer.bitsPerSample,
                buffer.channels,
                true,
                buffer.isBigEndian,
            )
            val out = AudioSystem.getSourceDataLine(format) ?: return
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
                        out.write(data, 0, data.size)
                    }
                    if (gen == generation) {
                        onPosition?.invoke(((elapsedSeconds + buffer.length) * 1000).toLong())
                    }
                }
                elapsedSeconds += buffer.length
            }
            emit()

            while (true) {
                synchronized(lock) {
                    while (paused && !stopped) lock.wait()
                }
                if (stopped || gen != generation) break

                frame = track.readNextFrame() ?: break
                decoder.decodeFrame(frame.data, buffer)
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
