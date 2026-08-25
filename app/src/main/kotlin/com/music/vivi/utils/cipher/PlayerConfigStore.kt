/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils.cipher

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.music.innertube.YouTube
import com.music.vivi.constants.CipherLastUpdatedKey
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File

object PlayerConfigStore {
    private const val TAG = "vivimusic_CipherConfig"

    private const val REMOTE_URL =
        "https://raw.githubusercontent.com/ZemerTeam/zemer-cipher/master/library/src/main/assets/player_configs.json"

    private const val BACKGROUND_REFRESH_TTL_MS = 6 * 60 * 60 * 1000L

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .apply { YouTube.proxy?.let { proxy(it) } }
            .build()
    }

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var bundledConfigs: Map<String, FunctionNameExtractor.HardcodedPlayerConfig> = emptyMap()

    @Volatile
    private var configs: Map<String, FunctionNameExtractor.HardcodedPlayerConfig> = emptyMap()

    private fun cacheDir(context: Context) = File(context.filesDir, "cipher_configs").apply { mkdirs() }
    private fun cacheFile(context: Context) = File(cacheDir(context), "configs_remote.json")

    fun initialize(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx

        bundledConfigs = loadFromSource("bundled asset") {
            ctx.assets.open("player_configs.json").bufferedReader().use { it.readText() }
        }
        if (bundledConfigs.isEmpty()) {
            Timber.tag(TAG).e("Bundled player_configs.json missing or invalid - config table starts empty")
        }

        val cachedConfigs = cacheFile(ctx).takeIf { it.exists() }?.let { file ->
            loadFromSource("cached remote copy") { file.readText() }
        } ?: emptyMap()

        configs = PlayerConfigParser.merge(bundledConfigs, cachedConfigs)
        Timber.tag(TAG).d("Loaded ${configs.size} configs (bundled: ${bundledConfigs.size}, cached: ${cachedConfigs.size})")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val lastUpdated = ctx.dataStore.get(CipherLastUpdatedKey, 0L)
                if (System.currentTimeMillis() - lastUpdated > BACKGROUND_REFRESH_TTL_MS) {
                    Timber.tag(TAG).d("Config cache older than ${BACKGROUND_REFRESH_TTL_MS / 3_600_000}h - refreshing in background")
                    triggerUpdate()
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Background config refresh check failed")
            }
        }
    }

    suspend fun triggerUpdate(): Boolean = withContext(Dispatchers.IO) {
        val context = appContext ?: run {
            Timber.tag(TAG).w("triggerUpdate called before initialize()")
            return@withContext false
        }
        try {
            val request = Request.Builder()
                .url(REMOTE_URL)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.tag(TAG).w("Remote config fetch HTTP ${response.code} - keeping previous configs")
                    return@withContext false
                }
                val body = response.body?.string()
                if (body.isNullOrEmpty()) {
                    Timber.tag(TAG).w("Remote config fetch returned empty body - keeping previous configs")
                    return@withContext false
                }

                val remote = when (val result = PlayerConfigParser.parse(body)) {
                    is PlayerConfigParser.ParseResult.Failure -> {
                        Timber.tag(TAG).w("Remote configs rejected: ${result.reason} - keeping previous configs")
                        return@withContext false
                    }
                    is PlayerConfigParser.ParseResult.Success -> {
                        if (result.skippedEntries.isNotEmpty()) {
                            Timber.tag(TAG).w("Remote configs: skipped invalid entries ${result.skippedEntries}")
                        }
                        result.configs
                    }
                }

                configs = PlayerConfigParser.merge(bundledConfigs, remote)
                cacheFile(context).writeText(body)
                Timber.tag(TAG).d("Remote configs applied (${remote.size} hashes, total merged: ${configs.size})")

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        context.dataStore.edit { it[CipherLastUpdatedKey] = System.currentTimeMillis() }
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "Failed to save config-update timestamp")
                    }
                }
                true
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to update remote configs: ${e.message}")
            false
        }
    }

    fun forceUpdateNow(onResult: (success: Boolean, message: String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val success = triggerUpdate()
            withContext(Dispatchers.Main) {
                onResult(success, if (success) "" else "Download failed or invalid configurations")
            }
        }
    }

    fun get(hash: String): FunctionNameExtractor.HardcodedPlayerConfig? = configs[hash]

    fun knownHashes(): Set<String> = configs.keys

    private fun loadFromSource(
        label: String,
        read: () -> String?,
    ): Map<String, FunctionNameExtractor.HardcodedPlayerConfig> {
        val text = try {
            read() ?: return emptyMap()
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Could not read $label: ${e.message}")
            return emptyMap()
        }
        return when (val result = PlayerConfigParser.parse(text)) {
            is PlayerConfigParser.ParseResult.Failure -> {
                Timber.tag(TAG).w("Rejected $label: ${result.reason}")
                emptyMap()
            }
            is PlayerConfigParser.ParseResult.Success -> {
                if (result.skippedEntries.isNotEmpty()) {
                    Timber.tag(TAG).w("$label: skipped invalid entries ${result.skippedEntries}")
                }
                result.configs
            }
        }
    }
}
