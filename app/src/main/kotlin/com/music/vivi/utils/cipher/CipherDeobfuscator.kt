/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils.cipher

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Main cipher deobfuscation orchestrator for YouTube stream URLs.
 *
 * Handles both signature deobfuscation (for signatureCipher streams) and
 * n-parameter transformation (for throttle avoidance / 403 fix).
 */
object CipherDeobfuscator {
    private const val TAG = "vivimusic_CipherDeobfusc"

    lateinit var appContext: Context
        private set

    fun initialize(context: Context) {
        Timber.tag(TAG).d("CipherDeobfuscator initializing...")
        appContext = context.applicationContext
        
        // Initialize player configs (bundled fallback + cached remote configs)
        PlayerConfigStore.initialize(appContext)
        Timber.tag(TAG).d("CipherDeobfuscator initialized")
    }

    private var cipherWebView: CipherWebView? = null
    private var currentPlayerHash: String? = null

    private val deobfuscateMutex = Mutex()

    /**
     * SignatureTimestamp of the player JS this cipher actually deciphers with.
     */
    suspend fun signatureTimestamp(): Int? {
        Timber.tag(TAG).d("Resolving cipher player signatureTimestamp...")
        val (playerJs, hash) = PlayerJsFetcher.getPlayerJs(forceRefresh = false) ?: run {
            Timber.tag(TAG).w("signatureTimestamp: could not fetch player JS")
            return null
        }
        val sts = FunctionNameExtractor.extractSignatureTimestamp(playerJs)
        Timber.tag(TAG).d("Cipher player STS (hash=$hash): $sts")
        return sts
    }

    /**
     * Prewarm the cipher WebView ahead of playback.
     */
    suspend fun prewarm() {
        Timber.tag(TAG).d("Prewarming cipher WebView...")
        deobfuscateMutex.withLock {
            getOrCreateWebView(forceRefresh = false)
        }
    }

    /**
     * Deobfuscate a signatureCipher stream URL.
     */
    suspend fun deobfuscateStreamUrl(signatureCipher: String, videoId: String): String? = deobfuscateMutex.withLock {
        try {
            deobfuscateInternal(signatureCipher, videoId, isRetry = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Cipher deobfuscation failed, retrying with fresh JS: ${e.message}")
            try {
                PlayerJsFetcher.invalidateCache()
                closeWebView()

                // Trigger remote update first to self-heal
                Timber.tag(TAG).d("Playback failure. Triggering remote config update...")
                PlayerConfigStore.triggerUpdate()

                deobfuscateInternal(signatureCipher, videoId, isRetry = true)
            } catch (retryE: CancellationException) {
                throw retryE
            } catch (retryE: Exception) {
                Timber.tag(TAG).e(retryE, "Cipher deobfuscation retry also failed: ${retryE.message}")
                null
            }
        }
    }

    private suspend fun deobfuscateInternal(signatureCipher: String, videoId: String, isRetry: Boolean): String? {
        // Parse the signatureCipher query string
        val params = parseQueryParams(signatureCipher)
        val obfuscatedSig = params["s"]
        val sigParam = params["sp"] ?: "signature"
        val baseUrl = params["url"]

        if (obfuscatedSig == null || baseUrl == null) {
            Timber.tag(TAG).e("Could not parse signatureCipher params: s=${obfuscatedSig != null}, url=${baseUrl != null}")
            return null
        }

        Timber.tag(TAG).d("Deobfuscating cipher for $videoId: sig=${obfuscatedSig.take(20)}..., sp=$sigParam")

        val webView = getOrCreateWebView(forceRefresh = isRetry) ?: return null

        // Deobfuscate signature
        val deobfuscatedSig = webView.deobfuscateSignature(obfuscatedSig)

        // Build the URL with deobfuscated signature
        val separator = if ("?" in baseUrl) "&" else "?"
        val finalUrl = "$baseUrl${separator}${sigParam}=${Uri.encode(deobfuscatedSig)}"

        Timber.tag(TAG).d("Custom cipher deobfuscation succeeded for $videoId")
        return finalUrl
    }

    /**
     * Transform the 'n' parameter in a streaming URL to avoid throttling/403.
     */
    suspend fun transformNParamInUrl(url: String): String = deobfuscateMutex.withLock {
        try {
            transformNInternal(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "N-transform failed, returning original URL: ${e.message}")
            url
        }
    }

    private suspend fun transformNInternal(url: String): String {
        val nMatch = Regex("[?&]n=([^&]+)").find(url)
        if (nMatch == null) {
            Timber.tag(TAG).d("No 'n' parameter found in URL, skipping transform")
            return url
        }
        val nValue = Uri.decode(nMatch.groupValues[1])

        val webView = getOrCreateWebView(forceRefresh = false) ?: return url

        if (!webView.nFunctionAvailable) {
            Timber.tag(TAG).e("N-transform function was not discovered at init time")
            return url
        }

        val transformedN = webView.transformN(nValue)

        return url.replaceFirst(
            Regex("([?&])n=[^&]+"),
            "$1n=${Uri.encode(transformedN)}"
        )
    }

    private suspend fun getOrCreateWebView(forceRefresh: Boolean): CipherWebView? {
        if (!forceRefresh && cipherWebView != null) {
            return cipherWebView
        }

        if (cipherWebView != null) {
            closeWebView()
        }

        // Fetch player JS
        val result = PlayerJsFetcher.getPlayerJs(forceRefresh = forceRefresh)
        if (result == null) {
            Timber.tag(TAG).e("Failed to get player JS")
            return null
        }
        val (playerJs, hash) = result

        // If hash is unknown, trigger a fetch of the remote config
        val hasConfig = PlayerConfigStore.get(hash) != null
        if (!hasConfig) {
            Timber.tag(TAG).d("Lookup miss for player hash $hash. Performing synchronous config fetch...")
            PlayerConfigStore.triggerUpdate()
        }

        // Extract signature function info
        val sigInfo = FunctionNameExtractor.extractSigFunctionInfo(playerJs, knownHash = hash)
        if (sigInfo == null) {
            Timber.tag(TAG).e("Could not extract signature function info from player JS")
            return null
        }

        // Extract n-transform function info (for throttle avoidance / 403 fix)
        val nFuncInfo = FunctionNameExtractor.extractNFunctionInfo(playerJs, knownHash = hash)

        // Create WebView — n-function is exported to window if found
        val webView = CipherWebView.create(
            context = appContext,
            playerJs = playerJs,
            sigInfo = sigInfo,
            nFuncInfo = nFuncInfo,
        )

        cipherWebView = webView
        currentPlayerHash = hash
        return webView
    }

    private suspend fun closeWebView() {
        withContext(Dispatchers.Main) {
            cipherWebView?.close()
        }
        cipherWebView = null
        currentPlayerHash = null
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (pair in query.split("&")) {
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val key = Uri.decode(pair.substring(0, idx))
                val value = Uri.decode(pair.substring(idx + 1))
                result[key] = value
            }
        }
        return result
    }
}
