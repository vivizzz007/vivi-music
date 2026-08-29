/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils.cipher

import com.music.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File

object PlayerJsFetcher {
    private const val TAG = "vivi_CipherFetcher"
    private const val IFRAME_API_URL = "https://www.youtube.com/iframe_api"
    private const val PLAYER_JS_URL_TEMPLATE = "https://www.youtube.com/s/player/%s/player_ias.vflset/en_GB/base.js"
    private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .proxySelector(object : java.net.ProxySelector() {
                override fun select(uri: java.net.URI?): List<java.net.Proxy> =
                    listOfNotNull(YouTube.proxy ?: java.net.Proxy.NO_PROXY)
                override fun connectFailed(uri: java.net.URI?, sa: java.net.SocketAddress?, ioe: java.io.IOException?) {
                    Timber.tag(TAG).e(ioe, "Proxy connection failed for URI: $uri")
                }
            })
            .proxyAuthenticator { _, response ->
                YouTube.proxyAuth?.let { auth ->
                    response.request.newBuilder().header("Proxy-Authorization", auth).build()
                } ?: response.request
            }
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private val PLAYER_HASH_REGEX = Regex("""\\?/s\\?/player\\?/([a-zA-Z0-9_-]+)\\?/""")

    private val cacheWriteLock = Any()

    private fun getCacheDir(): File = File(CipherDeobfuscator.appContext.filesDir, "cipher_cache")

    private fun getCacheFile(hash: String): File = File(getCacheDir(), "player_$hash.js")

    private fun getHashFile(): File = File(getCacheDir(), "current_hash.txt")

    suspend fun getPlayerJs(forceRefresh: Boolean = false): Pair<String, String>? = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("=== GET PLAYER.JS ===")
        Timber.tag(TAG).d("forceRefresh: $forceRefresh")

        try {
            val cacheDir = getCacheDir()
            if (!cacheDir.exists()) {
                Timber.tag(TAG).d("Creating cache directory: ${cacheDir.absolutePath}")
                cacheDir.mkdirs()
            }

            if (!forceRefresh) {
                val cached = readFromCache()
                if (cached != null) {
                    Timber.tag(TAG).d("=== CACHE HIT ===")
                    Timber.tag(TAG).d("Using cached player JS (hash=${cached.second}, length=${cached.first.length})")
                    return@withContext cached
                }
                Timber.tag(TAG).d("Cache miss, will fetch fresh")
            }

            Timber.tag(TAG).d("Fetching player hash from iframe_api...")
            val hash = fetchPlayerHash()
            if (hash == null) {
                Timber.tag(TAG).e("Failed to extract player hash from iframe_api")
                return@withContext null
            }
            Timber.tag(TAG).d("Extracted player hash: $hash")

            Timber.tag(TAG).d("Downloading player JS for hash: $hash...")
            val playerJs = downloadPlayerJs(hash)
            if (playerJs == null) {
                Timber.tag(TAG).e("Failed to download player JS for hash=$hash")
                return@withContext null
            }

            Timber.tag(TAG).d("=== PLAYER.JS DOWNLOADED ===")
            Timber.tag(TAG).d("hash: $hash")
            Timber.tag(TAG).d("length: ${playerJs.length} chars")
            Timber.tag(TAG).d("preview: ${playerJs.take(100)}...")

            writeToCache(hash, playerJs)

            Pair(playerJs, hash)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "getPlayerJs exception: ${e.message}")
            null
        }
    }

    fun invalidateCache() {
        Timber.tag(TAG).d("Invalidating cache...")
        synchronized(cacheWriteLock) {
            try {
                val cacheDir = getCacheDir()
                if (cacheDir.exists()) {
                    val files = cacheDir.listFiles()?.filter {
                        it.name.startsWith("player_") || it.name == "current_hash.txt"
                    }
                    Timber.tag(TAG).d("Deleting ${files?.size ?: 0} player-JS cache files")
                    files?.forEach {
                        Timber.tag(TAG).v("Deleting: ${it.name}")
                        it.delete()
                    }
                }
                Timber.tag(TAG).d("Cache invalidated successfully")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to invalidate cache: ${e.message}")
            }
        }
    }

    private fun readFromCache(): Pair<String, String>? {
        Timber.tag(TAG).d("Checking cache...")
        try {
            val hashFile = getHashFile()
            if (!hashFile.exists()) {
                Timber.tag(TAG).d("Hash file does not exist")
                return null
            }

            val hashData = hashFile.readText().split("\n")
            if (hashData.size < 2) {
                Timber.tag(TAG).d("Hash file malformed (expected 2 lines, got ${hashData.size})")
                return null
            }

            val hash = hashData[0]
            val timestamp = hashData[1].toLongOrNull()
            if (timestamp == null) {
                Timber.tag(TAG).d("Could not parse timestamp from hash file")
                return null
            }

            val ageMs = System.currentTimeMillis() - timestamp
            val ageHours = ageMs / (1000 * 60 * 60)
            Timber.tag(TAG).d("Cache age: ${ageHours}h (TTL: ${CACHE_TTL_MS / (1000 * 60 * 60)}h)")

            if (!withinWindow(System.currentTimeMillis(), timestamp, CACHE_TTL_MS)) {
                Timber.tag(TAG).d("Cache expired (hash=$hash, age=${ageHours}h)")
                return null
            }

            val cacheFile = getCacheFile(hash)
            if (!cacheFile.exists()) {
                Timber.tag(TAG).d("Cache file does not exist for hash: $hash")
                return null
            }

            val playerJs = cacheFile.readText()
            if (playerJs.isEmpty()) {
                Timber.tag(TAG).d("Cache file is empty")
                return null
            }

            Timber.tag(TAG).d("Cache valid: hash=$hash, length=${playerJs.length}, age=${ageHours}h")
            return Pair(playerJs, hash)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error reading cache: ${e.message}")
            return null
        }
    }

    private fun writeToCache(hash: String, playerJs: String) {
        Timber.tag(TAG).d("Writing to cache: hash=$hash, length=${playerJs.length}")
        synchronized(cacheWriteLock) {
            try {
                val cacheDir = getCacheDir()

                val oldFiles = cacheDir.listFiles()?.filter { it.name.startsWith("player_") }
                Timber.tag(TAG).d("Cleaning ${oldFiles?.size ?: 0} old cache files")
                oldFiles?.forEach { it.delete() }

                writeAtomic(getCacheFile(hash), playerJs)
                writeAtomic(getHashFile(), "$hash\n${System.currentTimeMillis()}")

                Timber.tag(TAG).d("Cache written successfully")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error writing cache: ${e.message}")
            }
        }
    }

    private fun fetchPlayerHash(): String? {
        Timber.tag(TAG).d("Fetching iframe_api from: $IFRAME_API_URL")

        val request = Request.Builder()
            .url(IFRAME_API_URL)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        val body = httpClient.newCall(request).execute().use { response ->
            Timber.tag(TAG).d("iframe_api response: HTTP ${response.code}")
            if (!response.isSuccessful) {
                Timber.tag(TAG).e("iframe_api HTTP ${response.code}")
                return null
            }
            response.body?.string()
        }
        if (body == null) {
            Timber.tag(TAG).e("iframe_api response body is null")
            return null
        }

        Timber.tag(TAG).d("iframe_api body length: ${body.length}")
        Timber.tag(TAG).v("iframe_api body preview: ${body.take(200)}...")

        val match = PLAYER_HASH_REGEX.find(body)
        if (match == null) {
            Timber.tag(TAG).e("Could not find player hash in iframe_api response")
            Timber.tag(TAG).d("Regex pattern: ${PLAYER_HASH_REGEX.pattern}")
            return null
        }

        val hash = match.groupValues[1]
        Timber.tag(TAG).d("Found player hash: $hash")
        return hash
    }

    private fun downloadPlayerJs(hash: String): String? {
        val url = PLAYER_JS_URL_TEMPLATE.format(hash)
        Timber.tag(TAG).d("Downloading player.js from: $url")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        val body = httpClient.newCall(request).execute().use { response ->
            Timber.tag(TAG).d("player.js response: HTTP ${response.code}")
            if (!response.isSuccessful) {
                Timber.tag(TAG).e("player.js download HTTP ${response.code}")
                return null
            }
            response.body?.string()
        }
        if (body == null) {
            Timber.tag(TAG).e("player.js response body is null")
            return null
        }

        Timber.tag(TAG).d("player.js downloaded: ${body.length} chars")
        return body
    }

    private fun withinWindow(now: Long, stampMs: Long, windowMs: Long) =
        (now - stampMs) in 0 until windowMs

    private fun writeAtomic(file: File, content: String) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(content)
        if (!tmp.renameTo(file)) {

            file.delete()
            if (!tmp.renameTo(file)) {
                file.writeText(content)
                tmp.delete()
            }
        }
    }

    fun getCacheInfo(): Map<String, Any?> {
        return try {
            val hashFile = getHashFile()
            if (!hashFile.exists()) {
                return mapOf("exists" to false)
            }

            val hashData = hashFile.readText().split("\n")
            val hash = hashData.getOrNull(0)
            val timestamp = hashData.getOrNull(1)?.toLongOrNull()
            val cacheFile = hash?.let { getCacheFile(it) }

            mapOf(
                "exists" to true,
                "hash" to hash,
                "timestamp" to timestamp,
                "ageMs" to (timestamp?.let { System.currentTimeMillis() - it }),
                "fileExists" to (cacheFile?.exists() == true),
                "fileSize" to (cacheFile?.length()),
            )
        } catch (e: Exception) {
            mapOf("error" to e.message)
        }
    }
}
