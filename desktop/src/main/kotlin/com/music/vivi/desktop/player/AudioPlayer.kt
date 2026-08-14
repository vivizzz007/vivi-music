package com.music.vivi.desktop.player

import net.sourceforge.jaad.aac.Decoder
import net.sourceforge.jaad.aac.SampleBuffer
import net.sourceforge.jaad.mp4.MP4Container
import net.sourceforge.jaad.mp4.api.AudioTrack
import net.sourceforge.jaad.mp4.api.Frame
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/**
 * Self-contained AAC player: demuxes an MP4 stream with `jaad`, decodes AAC
 * frames to PCM, and plays them through Java Sound. No native libraries or
 * external binaries are required.
 */
class AudioPlayer {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var thread: Thread? = null

    /** Incremented on every [play]; stale threads ignore their callbacks. */
    private var generation = 0

    @Volatile private var paused = false
    @Volatile private var stopped = false
    private val lock = Object()

    @Volatile private var line: SourceDataLine? = null

    /**
     * Starts streaming [url] on a background thread. [onPosition] reports the
     * decoded playback position in milliseconds; [onComplete] is invoked once
     * when the stream ends or is stopped.
     */
    fun play(url: String, onPosition: (Long) -> Unit, onComplete: () -> Unit) {
        stop()
        val gen = ++generation
        paused = false
        stopped = false

        thread = Thread {
            try {
                decodeAndPlay(url, gen) { pos ->
                    if (gen == generation) onPosition(pos)
                }
            } catch (_: Exception) {
                // fall through to onComplete
            } finally {
                if (gen == generation) onComplete()
            }
        }.apply {
            isDaemon = true
            name = "vivimusic-audio"
            start()
        }
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

    private fun decodeAndPlay(url: String, gen: Int, onPosition: (Long) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return
            val input = response.body?.byteStream() ?: return

            val container = MP4Container(input)
            val movie = container.movie ?: return
            val track = movie.tracks.firstOrNull { it is AudioTrack } as? AudioTrack ?: return

            val decoder = Decoder(track.decoderSpecificInfo)
            val buffer = SampleBuffer()

            // Decode the first frame to learn the PCM format, then open the line.
            var frame: Frame? = movie.readNextFrame() ?: return
            decoder.decodeFrame(frame!!.data, buffer)

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

            var elapsedSeconds = 0.0
            fun emit() {
                out.write(buffer.data, 0, buffer.data.size)
                elapsedSeconds += buffer.length
                onPosition((elapsedSeconds * 1000).toLong())
            }
            emit()

            while (true) {
                synchronized(lock) {
                    while (paused && !stopped) lock.wait()
                }
                if (stopped || gen != generation) break

                frame = movie.readNextFrame() ?: break
                decoder.decodeFrame(frame!!.data, buffer)
                emit()
            }

            out.drain()
            out.stop()
            out.close()
            if (line === out) line = null
        }
    }
}
