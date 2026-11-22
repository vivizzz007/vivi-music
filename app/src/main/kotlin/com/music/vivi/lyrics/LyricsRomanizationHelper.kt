/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.lyrics

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Lightweight, on-demand lyric romanization powered by Google Translate.
 *
 * Avoids bundling heavy multi-megabyte offline dictionaries (such as Kuromoji at ~15MB).
 * Requests are batched into single HTTP requests using Unicode private-use delimiter markers,
 * and results are cached both in-memory (LRU) and on-disk (gzipped, bounded to 2MB).
 */
object LyricsRomanizationHelper {
    private const val ENDPOINT = "https://translate.googleapis.com/translate_a/single"
    private const val CACHE_DIRECTORY = "lyrics_romanization_v1"
    private const val MAX_CACHE_BYTES = 2L * 1024L * 1024L // 2 MB
    private const val MAX_BATCH_CHARS = 3_000

    private val markerRegex = Regex("\\uE000[^\\uE001]*\\uE001")
    private fun marker(index: Int): String = "\uE000${index.toString().padStart(4, '0')}\uE001"

    private val client by lazy {
        OkHttpClient.Builder()
            .callTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val diskMutex = Mutex()

    private val memoryCache = object {
        private val entries = LinkedHashMap<String, List<String>>(16, 0.75f, true)

        @Synchronized
        fun get(key: String): List<String>? = entries[key]

        @Synchronized
        fun put(key: String, value: List<String>) {
            entries[key] = value
            if (entries.size > 24) {
                val oldest = entries.keys.firstOrNull()
                if (oldest != null) entries.remove(oldest)
            }
        }

        @Synchronized
        fun clear() = entries.clear()
    }

    /**
     * Checks if text contains non-Latin letters (e.g. Japanese, Korean, Chinese, Cyrillic, Indic).
     */
    fun hasNonLatinLetters(text: String): Boolean {
        for (i in 0 until text.length) {
            val codePoint = text.codePointAt(i)
            if (Character.isLetter(codePoint)) {
                val script = Character.UnicodeScript.of(codePoint)
                if (script != Character.UnicodeScript.LATIN) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Detects dominant script or language hint to optimize translation / romanization parsing.
     */
    fun detectLanguage(text: String): String {
        return when {
            // Malayalam
            text.any { it in '\u0D00'..'\u0D7F' } -> "ml"
            // Tamil
            text.any { it in '\u0B80'..'\u0BFF' } -> "ta"
            // Telugu
            text.any { it in '\u0C00'..'\u0C7F' } -> "te"
            // Kannada
            text.any { it in '\u0C80'..'\u0CFF' } -> "kn"
            // Bengali
            text.any { it in '\u0980'..'\u09FF' } -> "bn"
            // Devanagari (Hindi, Marathi)
            text.any { it in '\u0900'..'\u097F' } -> "hi"
            // Gurmukhi (Punjabi)
            text.any { it in '\u0A00'..'\u0A7F' } -> "pa"
            // Korean (Hangul)
            text.any { it in '\uAC00'..'\uD7AF' || it in '\u1100'..'\u11FF' || it in '\u3130'..'\u318F' } -> "ko"
            // Japanese (Hiragana or Katakana present)
            text.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' } -> "ja"
            // Chinese (Han ideographs without Kana)
            text.any { it in '\u4E00'..'\u9FFF' || it in '\u3400'..'\u4DBF' } -> "zh-CN"
            // Cyrillic
            text.any { it in '\u0400'..'\u04FF' } -> "ru"
            // Arabic
            text.any { it in '\u0600'..'\u06FF' } -> "ar"
            // Thai
            text.any { it in '\u0E00'..'\u0E7F' } -> "th"
            // Greek
            text.any { it in '\u0370'..'\u03FF' } -> "el"
            else -> "auto"
        }
    }

    private fun cacheKey(songId: String, texts: List<String>): String {
        val source = buildString {
            append(songId)
            texts.forEach {
                append('\u0000')
                append(it)
            }
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private suspend fun readDiskCache(cacheDir: File?, key: String): List<String>? =
        withContext(Dispatchers.IO) {
            diskMutex.withLock {
                val root = cacheDir ?: return@withLock null
                val file = File(File(root, CACHE_DIRECTORY), "$key.gz")
                if (!file.isFile) return@withLock null
                runCatching {
                    GZIPInputStream(FileInputStream(file)).bufferedReader().use { reader ->
                        reader.readLines()
                    }
                }.getOrNull()
            }
        }

    private suspend fun writeDiskCache(cacheDir: File?, key: String, lines: List<String>) =
        withContext(Dispatchers.IO) {
            diskMutex.withLock {
                val root = cacheDir ?: return@withLock
                val directory = File(root, CACHE_DIRECTORY)
                if (!directory.exists() && !directory.mkdirs()) return@withLock
                val destination = File(directory, "$key.gz")
                val temporary = File(directory, "$key.tmp")
                runCatching {
                    GZIPOutputStream(FileOutputStream(temporary)).bufferedWriter().use { writer ->
                        lines.forEach { line ->
                            writer.write(line)
                            writer.newLine()
                        }
                    }
                    if (!temporary.renameTo(destination)) {
                        temporary.copyTo(destination, overwrite = true)
                        temporary.delete()
                    }
                    trimCache(directory)
                }.onFailure { temporary.delete() }
            }
        }

    private fun trimCache(directory: File) {
        val files = directory.listFiles { file -> file.extension == "gz" }
            ?.sortedByDescending(File::lastModified)
            .orEmpty()
        var kept = 0L
        files.forEach { file ->
            kept += file.length()
            if (kept > MAX_CACHE_BYTES) file.delete()
        }
    }

    private suspend fun Call.awaitBody(): String = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = if (it.isSuccessful) it.body?.string() else null
                    if (!continuation.isActive) return
                    if (body != null) {
                        continuation.resume(body)
                    } else {
                        continuation.resumeWithException(IOException("Romanization HTTP ${it.code}"))
                    }
                }
            }
        })
    }

    private suspend fun requestRomanization(payload: String, sourceLanguage: String): String? {
        val body = FormBody.Builder()
            .add("client", "dict-chrome-ex")
            .add("sl", sourceLanguage)
            .add("tl", "en")
            .add("dt", "rm")
            .add("q", payload)
            .build()
        val request = Request.Builder()
            .url(ENDPOINT)
            .header("User-Agent", "vivi-music/1.0")
            .header("Accept", "application/json")
            .post(body)
            .build()

        val response = try {
            client.newCall(request).awaitBody()
        } catch (_: Exception) {
            return null
        }

        return runCatching {
            val root = JSONArray(response)
            val segments = root.optJSONArray(0) ?: return@runCatching null
            val sb = StringBuilder()
            for (i in 0 until segments.length()) {
                val segment = segments.optJSONArray(i)
                val rm = segment?.optString(3)?.takeIf { it != "null" && it.isNotBlank() }
                if (rm != null) {
                    sb.append(rm)
                }
            }
            sb.toString().takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    /**
     * Romanizes a single line of text on demand (used as a fallback or for individual lines).
     */
    suspend fun romanizeText(text: String, languageHint: String? = null, context: Context? = null): String =
        withContext(Dispatchers.IO) {
            if (text.isBlank() || !hasNonLatinLetters(text)) return@withContext text
            val lang = languageHint ?: detectLanguage(text)
            val result = requestRomanization(text, lang)
            result?.trim() ?: text
        }

    /**
     * Romanizes all lines in a song's lyrics in batches.
     * Batches are separated by Unicode private-use markers to preserve line boundaries.
     * Caches result in memory and disk for instant, offline re-use.
     */
    suspend fun romanizeLyrics(
        lyrics: List<LyricsEntry>,
        songId: String,
        context: Context,
        enabledLanguages: Set<String>? = null,
        force: Boolean = false,
    ) = withContext(Dispatchers.IO) {
        if (lyrics.isEmpty()) return@withContext

        // Identify non-empty entries and filter out lines that have no non-Latin characters
        val nonBlankEntries = lyrics.filter { it.text.isNotBlank() }
        if (nonBlankEntries.isEmpty()) return@withContext

        // Check if any line has non-Latin text
        val hasAnyNonLatin = nonBlankEntries.any { hasNonLatinLetters(it.text) }
        if (!hasAnyNonLatin) return@withContext

        val allTexts = nonBlankEntries.map { it.text }
        val dominantLanguage = detectLanguage(allTexts.joinToString(" "))

        // If enabledLanguages is provided, check if the dominant language is enabled
        if (enabledLanguages != null && dominantLanguage != "auto" && !enabledLanguages.contains(dominantLanguage)) {
            // Check if any specific language in enabledLanguages matches
            val hasMatchingLang = enabledLanguages.any { lang ->
                when (lang) {
                    "ja" -> allTexts.any { it.any { c -> c in '\u3040'..'\u309F' || c in '\u30A0'..'\u30FF' } }
                    "ko" -> allTexts.any { it.any { c -> c in '\uAC00'..'\uD7AF' || c in '\u1100'..'\u11FF' } }
                    "zh-CN" -> allTexts.any { it.any { c -> c in '\u4E00'..'\u9FFF' } }
                    "ml" -> allTexts.any { it.any { c -> c in '\u0D00'..'\u0D7F' } }
                    "hi" -> allTexts.any { it.any { c -> c in '\u0900'..'\u097F' } }
                    "pa" -> allTexts.any { it.any { c -> c in '\u0A00'..'\u0A7F' } }
                    "ru", "uk", "be", "sr", "bg", "ky", "mk" -> allTexts.any { it.any { c -> c in '\u0400'..'\u04FF' } }
                    else -> false
                }
            }
            if (!hasMatchingLang) return@withContext
        }

        val cacheKey = cacheKey(songId, allTexts)

        // 1. Check in-memory cache
        val memCached = if (!force) memoryCache.get(cacheKey) else null
        if (memCached != null && memCached.size == nonBlankEntries.size) {
            nonBlankEntries.forEachIndexed { index, entry ->
                entry.romanizedTextFlow.value = memCached[index]
            }
            return@withContext
        }

        // 2. Check disk cache
        val diskCached = if (!force) readDiskCache(context.cacheDir, cacheKey) else null
        if (diskCached != null && diskCached.size == nonBlankEntries.size) {
            memoryCache.put(cacheKey, diskCached)
            nonBlankEntries.forEachIndexed { index, entry ->
                entry.romanizedTextFlow.value = diskCached[index]
            }
            return@withContext
        }

        // 3. Request romanization from network in batches
        val results = mutableListOf<String>()
        var currentBatch = mutableListOf<String>()
        var currentBatchLength = 0

        suspend fun flushBatch() {
            if (currentBatch.isEmpty()) return
            val payload = buildString {
                currentBatch.forEachIndexed { index, text ->
                    if (index > 0) append('\n').append(marker(index)).append('\n')
                    append(text)
                }
            }
            val romanizedBody = requestRomanization(payload, dominantLanguage)
            if (romanizedBody != null) {
                val parts = romanizedBody.split(markerRegex).map { it.trim() }
                if (parts.size == currentBatch.size) {
                    results.addAll(parts)
                } else {
                    // Fallback: add original or trimmed body if marker split size differed
                    results.addAll(currentBatch)
                }
            } else {
                results.addAll(currentBatch)
            }
            currentBatch.clear()
            currentBatchLength = 0
        }

        for (text in allTexts) {
            val added = text.length + 8
            if (currentBatch.isNotEmpty() && currentBatchLength + added > MAX_BATCH_CHARS) {
                flushBatch()
            }
            currentBatch.add(text)
            currentBatchLength += added
        }
        flushBatch()

        if (results.size == nonBlankEntries.size) {
            nonBlankEntries.forEachIndexed { index, entry ->
                val romanized = results[index]
                if (romanized != entry.text && romanized.isNotBlank()) {
                    entry.romanizedTextFlow.value = romanized
                }
            }
            memoryCache.put(cacheKey, results)
            writeDiskCache(context.cacheDir, cacheKey, results)
        }
    }
}
