/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils.cipher

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.music.innertube.YouTube
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import com.music.vivi.constants.CipherLastUpdatedKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File

/**
 * Manages the player-config mappings. Loads the bundled local fallback configs
 * and updates them dynamically in the background from Cloudflare without complex caching or ETag overhead.
 */
object PlayerConfigStore {
    private const val TAG = "vivimusic_CipherConfig"
    private val remoteUrl by lazy {
        val shifted = intArrayOf(
            111, 123, 123, 119, 122, 65, 54, 54, 125, 112, 125, 112, 116, 124, 122, 112,
            106, 52, 106, 112, 119, 111, 108, 121, 53, 116, 114, 116, 107, 108, 125, 112,
            115, 116, 112, 53, 126, 118, 121, 114, 108, 121, 122, 53, 107, 108, 125
        )
        shifted.map { (it - 7).toChar() }.joinToString("") + "/player_configs.json"
    }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .apply { YouTube.proxy?.let { proxy(it) } }
            .build()
    }

    @Volatile
    private var configs: Map<String, FunctionNameExtractor.HardcodedPlayerConfig> = emptyMap()

    fun initialize(context: Context) {
        val appContext = context.applicationContext

        // Load bundled asset fallback configs if present
        val bundledJson = try {
            appContext.assets.open("player_configs.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        }
        val bundledMap = bundledJson?.let { parseJson(it) } ?: emptyMap()

        // Load cached remote configs if present
        val cacheDir = File(appContext.filesDir, "cipher_configs").apply { mkdirs() }
        val cacheFile = File(cacheDir, "configs_remote.json")
        val cachedMap = if (cacheFile.exists()) {
            try {
                parseJson(cacheFile.readText())
            } catch (e: Exception) {
                cacheFile.delete()
                emptyMap()
            }
        } else {
            emptyMap()
        }

        // Merge them: Remote updates overlay bundled configs
        configs = bundledMap + cachedMap
        Timber.tag(TAG).d("Loaded ${configs.size} configs (bundled: ${bundledMap.size}, cached: ${cachedMap.size})")

        // Trigger silent background sync if cache is older than 6 hours (or never loaded)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val lastUpdated = appContext.dataStore.get(CipherLastUpdatedKey, 0L)
                val now = System.currentTimeMillis()
                if (now - lastUpdated > 6 * 60 * 60 * 1000L) {
                    Timber.tag(TAG).d("Config cache is older than 6 hours. Running background update...")
                    triggerUpdate()
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to run background update check")
            }
        }
    }

    /**
     * Trigger a background update from the Cloudflare Worker URL.
     */
    suspend fun triggerUpdate(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(remoteUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrEmpty()) {
                        val remoteMap = parseJson(body)
                        if (remoteMap.isNotEmpty()) {
                            // Load bundled asset fallback configs to merge
                            val bundledJson = try {
                                CipherDeobfuscator.appContext.assets.open("player_configs.json").bufferedReader().use { it.readText() }
                            } catch (e: Exception) {
                                null
                            }
                            val bundledMap = bundledJson?.let { parseJson(it) } ?: emptyMap()

                            // Update active configurations
                            configs = bundledMap + remoteMap

                            // Save to local cache file
                            val cacheDir = File(CipherDeobfuscator.appContext.filesDir, "cipher_configs").apply { mkdirs() }
                            val cacheFile = File(cacheDir, "configs_remote.json")
                            cacheFile.writeText(body)
                            Timber.tag(TAG).d("Successfully updated remote configs, total: ${configs.size}")
                            
                            // Save update timestamp to preferences
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    CipherDeobfuscator.appContext.dataStore.edit {
                                        it[com.music.vivi.constants.CipherLastUpdatedKey] = System.currentTimeMillis()
                                    }
                                } catch (e: Exception) {
                                    Timber.tag(TAG).e(e, "Failed to save update timestamp")
                                }
                            }

                            return@withContext true
                        }
                    }
                } else {
                    Timber.tag(TAG).w("Remote configs update returned HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to update remote configs: ${e.message}")
        }
        false
    }

    /**
     * Manually trigger updates and return status in callback on main thread (for Settings UI).
     */
    fun forceUpdateNow(onResult: (success: Boolean, message: String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val success = triggerUpdate()
            withContext(Dispatchers.Main) {
                if (success) {
                    onResult(true, "")
                } else {
                    onResult(false, "Download failed or invalid configurations")
                }
            }
        }
    }

    fun get(hash: String): FunctionNameExtractor.HardcodedPlayerConfig? = configs[hash]

    fun knownHashes(): Set<String> = configs.keys

    private fun parseJson(text: String): Map<String, FunctionNameExtractor.HardcodedPlayerConfig> {
        val result = PlayerConfigParser.parse(text)
        return if (result is PlayerConfigParser.ParseResult.Success) {
            result.configs
        } else {
            emptyMap()
        }
    }
}
