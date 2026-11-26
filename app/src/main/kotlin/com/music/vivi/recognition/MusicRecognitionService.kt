/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
/**
 * Music Recognition Feature
 * 
 * This feature is based on the original MusicRecognizer project by Aleksey Saenko.
 * Original project: https://github.com/aleksey-saenko/MusicRecognizer
 * 
 * Special thanks to Aleksey Saenko for the music recognition implementation.
 */

package com.music.vivi.recognition

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.music.shazamkit.Shazam
import com.music.shazamkit.models.RecognitionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteOrder

/**
 * Service for recognizing music using audio fingerprinting.
 * Records audio from the microphone, generates a Shazam-compatible fingerprint,
 * and sends it to the Shazam API for recognition.
 */
object MusicRecognitionService {
    
    // Recording parameters (prefer native 16kHz to avoid resampling; fallback to 44.1kHz if unsupported)
    private const val TARGET_SAMPLE_RATE = VibraSignature.REQUIRED_SAMPLE_RATE
    private const val FALLBACK_SAMPLE_RATE = 44100
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    // Progressive recognition checkpoints: check early (2.2s) and every ~1.6-2.5s up to 10s
    private val CHECKPOINT_MS = longArrayOf(2200L, 3800L, 5500L, 7500L, 10000L)
    private const val MAX_RECORDING_MS = 10000L
    
    private val _recognitionStatus = MutableStateFlow<RecognitionStatus>(RecognitionStatus.Ready)
    val recognitionStatus: StateFlow<RecognitionStatus> = _recognitionStatus.asStateFlow()
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var recognitionJob: Job? = null
    private var activeAudioRecord: AudioRecord? = null

    fun startRecognizing(context: Context) {
        recognitionJob?.cancel()
        val appContext = context.applicationContext
        recognitionJob = serviceScope.launch {
            recognize(appContext)
        }
    }

    fun stopRecognizing() {
        recognitionJob?.cancel()
        recognitionJob = null
        reset()
    }
    
    fun hasRecordPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Start the music recognition process.
     * Records audio, incrementally generates fingerprints, and queries Shazam at 3s/6s/10s checkpoints.
     */
    @SuppressLint("MissingPermission")
    suspend fun recognize(context: Context): RecognitionStatus = withContext(Dispatchers.IO) {
        if (!hasRecordPermission(context)) {
            return@withContext RecognitionStatus.Error("Microphone permission not granted")
        }
        require(ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            "Little-endian PCM required for fingerprint generation"
        }

        _recognitionStatus.value = RecognitionStatus.Listening

        var audioRecord: AudioRecord? = null
        var inFlightCheck: Deferred<Result<com.music.shazamkit.models.RecognitionResult>>? = null

        try {
            val (record, sampleRate, bufferSize) = openAudioRecord(context)
            audioRecord = record
            activeAudioRecord = record

            val session = VibraSignature.createSession()
            val buffer = ByteArray(bufferSize)
            val startTime = System.currentTimeMillis()
            var nextCheckpointIdx = 0
            var matchedResult: com.music.shazamkit.models.RecognitionResult? = null
            var lastFailure: Throwable? = null

            record.startRecording()

            while (isActive && System.currentTimeMillis() - startTime < MAX_RECORDING_MS) {
                val bytesRead = record.read(buffer, 0, bufferSize)
                if (bytesRead > 0) {
                    if (sampleRate == TARGET_SAMPLE_RATE) {
                        session.feedI16(buffer, bytesRead)
                    } else {
                        val chunk = DecodedAudio(
                            data = buffer.copyOf(bytesRead),
                            channelCount = 1,
                            sampleRate = sampleRate,
                            pcmEncoding = AUDIO_FORMAT
                        )
                        val resampled = AudioResampler.resample(chunk, TARGET_SAMPLE_RATE).getOrElse { error ->
                            if (isActive) {
                                _recognitionStatus.value = RecognitionStatus.Error("Failed to resample audio: ${error.message}")
                            }
                            return@withContext _recognitionStatus.value
                        }
                        session.feedI16(resampled.data, resampled.data.size)
                    }
                }

                // Check if an in-flight Shazam request has already finished
                val pending = inFlightCheck
                if (pending != null && pending.isCompleted) {
                    inFlightCheck = null
                    val result = pending.await()
                    if (result.isSuccess) {
                        matchedResult = result.getOrThrow()
                        break
                    } else {
                        lastFailure = result.exceptionOrNull()
                    }
                }

                // Trigger the next progressive checkpoint if no request is currently in flight
                val elapsedMs = System.currentTimeMillis() - startTime
                if (inFlightCheck == null &&
                    nextCheckpointIdx < CHECKPOINT_MS.size &&
                    elapsedMs >= CHECKPOINT_MS[nextCheckpointIdx] &&
                    session.sampleDurationMs > 0
                ) {
                    nextCheckpointIdx++
                    val signature = session.encodeSignature()
                    val sampleDurationMs = session.sampleDurationMs
                    inFlightCheck = async { Shazam.recognize(signature, sampleDurationMs) }
                }
            }

            // Stop microphone as soon as we have a match or hit 10s
            releaseAudioRecord(audioRecord)
            audioRecord = null

            if (!isActive) return@withContext _recognitionStatus.value

            if (matchedResult != null) {
                _recognitionStatus.value = RecognitionStatus.Success(matchedResult)
                return@withContext _recognitionStatus.value
            }

            _recognitionStatus.value = RecognitionStatus.Processing

            // Await any in-flight request from an earlier checkpoint
            inFlightCheck?.let { pending ->
                val result = pending.await()
                if (!isActive) return@withContext _recognitionStatus.value
                if (result.isSuccess) {
                    _recognitionStatus.value = RecognitionStatus.Success(result.getOrThrow())
                    return@withContext _recognitionStatus.value
                } else {
                    lastFailure = result.exceptionOrNull()
                }
            }

            // If we haven't queried the full recorded audio yet, run one final check
            if (session.sampleDurationMs > 0 &&
                (nextCheckpointIdx < CHECKPOINT_MS.size || lastFailure != null)
            ) {
                val finalResult = Shazam.recognize(session.encodeSignature(), session.sampleDurationMs)
                if (!isActive) return@withContext _recognitionStatus.value

                finalResult.fold(
                    onSuccess = { recognitionResult ->
                        _recognitionStatus.value = RecognitionStatus.Success(recognitionResult)
                    },
                    onFailure = { error ->
                        _recognitionStatus.value = mapFailureToStatus(error)
                    }
                )
            } else {
                _recognitionStatus.value = mapFailureToStatus(lastFailure)
            }

            _recognitionStatus.value
        } catch (e: CancellationException) {
            inFlightCheck?.cancel()
            throw e
        } catch (e: Exception) {
            if (isActive) {
                _recognitionStatus.value = RecognitionStatus.Error(e.message ?: "Recognition failed")
            }
            _recognitionStatus.value
        } finally {
            releaseAudioRecord(audioRecord)
        }
    }

    private fun mapFailureToStatus(error: Throwable?): RecognitionStatus {
        val message = error?.message ?: "Unknown error"
        return if (message.contains("No match", ignoreCase = true)) {
            RecognitionStatus.NoMatch("No matches found. Try again with clearer audio.")
        } else {
            RecognitionStatus.Error(message)
        }
    }

    @SuppressLint("MissingPermission")
    private fun openAudioRecord(context: Context): Triple<AudioRecord, Int, Int> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val supportsUnprocessed =
            audioManager?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        val sources = if (supportsUnprocessed) {
            intArrayOf(MediaRecorder.AudioSource.UNPROCESSED, MediaRecorder.AudioSource.MIC)
        } else {
            intArrayOf(MediaRecorder.AudioSource.MIC)
        }

        for (source in sources) {
            for (rate in intArrayOf(TARGET_SAMPLE_RATE, FALLBACK_SAMPLE_RATE)) {
                val minBuf = AudioRecord.getMinBufferSize(rate, CHANNEL_CONFIG, AUDIO_FORMAT)
                if (minBuf <= 0) continue
                val bufSize = maxOf(minBuf, 2048)
                val record = AudioRecord(
                    source,
                    rate,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufSize
                )
                if (record.state == AudioRecord.STATE_INITIALIZED) {
                    return Triple(record, rate, bufSize)
                }
                record.release()
            }
        }
        throw IllegalStateException("Unable to initialize AudioRecord")
    }

    private fun releaseAudioRecord(record: AudioRecord?) {
        if (record == null) return
        try {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
            record.release()
        } catch (_: Exception) {}
        if (activeAudioRecord == record) {
            activeAudioRecord = null
        }
    }
    
    fun reset() {
        try {
            activeAudioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {}
        activeAudioRecord = null
        _recognitionStatus.value = RecognitionStatus.Ready
    }
}
