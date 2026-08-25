/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils.cipher

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

object CipherDeobfuscator {
    private const val TAG = "vivimusic_CipherDeobfusc"

    lateinit var appContext: Context
        private set

    fun initialize(context: Context) {
        Timber.tag(TAG).d("CipherDeobfuscator initializing...")
        appContext = context.applicationContext
        PlayerConfigStore.initialize(appContext)
        Timber.tag(TAG).d("CipherDeobfuscator initialized")
    }

    private var cipherWebView: CipherWebView? = null

    @Volatile
    private var currentPlayerHash: String? = null

    private val deobfuscateMutex = Mutex()

    private val rendererRecoveryPolicy = RendererRecoveryPolicy()

    val lastUsedPlayerHash: String? get() = currentPlayerHash

    suspend fun signatureTimestamp(): Int? {
        Timber.tag(TAG).d("Resolving cipher player signatureTimestamp...")
        val (playerJs, hash) = PlayerJsFetcher.getPlayerJs(forceRefresh = false) ?: run {
            Timber.tag(TAG).w("signatureTimestamp: could not fetch player JS")
            return null
        }
        val sts = FunctionNameExtractor.extractSignatureTimestamp(playerJs, hash)
        Timber.tag(TAG).d("Cipher player STS (hash=$hash): $sts")
        return sts
    }

    suspend fun prewarm() {
        Timber.tag(TAG).d("Prewarming cipher WebView...")
        deobfuscateMutex.withLock {
            try {
                getOrCreateWebView(forceRefresh = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: CipherRendererGoneException) {
                onRendererGone(e, "prewarm")
            }
        }
    }

    suspend fun deobfuscateStreamUrl(signatureCipher: String, videoId: String): String? = deobfuscateMutex.withLock {
        try {
            deobfuscateInternal(signatureCipher, videoId, isRetry = false)
                ?.also { rendererRecoveryPolicy.onSuccess() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: CipherRendererGoneException) {
            onRendererGone(e, "deobfuscate")
            null
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Cipher deobfuscation failed, retrying with fresh JS: ${e.message}")
            try {
                PlayerJsFetcher.invalidateCache()
                closeWebView()

                Timber.tag(TAG).d("Triggering remote config update before retry...")
                PlayerConfigStore.triggerUpdate()
                deobfuscateInternal(signatureCipher, videoId, isRetry = true)
                    ?.also { rendererRecoveryPolicy.onSuccess() }
            } catch (retryE: CancellationException) {
                throw retryE
            } catch (retryE: CipherRendererGoneException) {
                onRendererGone(retryE, "deobfuscate-retry")
                null
            } catch (retryE: Exception) {
                Timber.tag(TAG).e(retryE, "Cipher deobfuscation retry also failed: ${retryE.message}")
                null
            }
        }
    }

    suspend fun forceRefreshConfig(): Boolean {
        Timber.tag(TAG).d("forceRefreshConfig: self-heal triggered by stream rejection")
        return deobfuscateMutex.withLock {
            try {
                PlayerJsFetcher.invalidateCache()
                closeWebView()
                val changed = PlayerConfigStore.triggerUpdate()
                getOrCreateWebView(forceRefresh = true)
                changed
            } catch (e: CancellationException) {
                throw e
            } catch (e: CipherRendererGoneException) {
                onRendererGone(e, "force-refresh")
                false
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "forceRefreshConfig failed: ${e.message}")
                false
            }
        }
    }

    suspend fun transformNParamInUrl(url: String): String = deobfuscateMutex.withLock {
        try {
            transformNInternal(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: CipherRendererGoneException) {
            onRendererGone(e, "n-transform")
            url
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "N-transform failed, returning original URL: ${e.message}")
            url
        }
    }

    private suspend fun onRendererGone(e: CipherRendererGoneException, where: String) {
        rendererRecoveryPolicy.onFailure(SystemClock.elapsedRealtime())
        Timber.tag(TAG).e(
            e,
            "WebView renderer gone during $where (consecutive failures: " +
                "${rendererRecoveryPolicy.consecutiveFailures}) - dropping cipher WebView"
        )
        closeWebView()
    }

    private suspend fun deobfuscateInternal(signatureCipher: String, videoId: String, isRetry: Boolean): String? {
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
        val deobfuscatedSig = webView.deobfuscateSignature(obfuscatedSig)

        val separator = if ("?" in baseUrl) "&" else "?"
        val finalUrl = "$baseUrl${separator}${sigParam}=${Uri.encode(deobfuscatedSig)}"

        Timber.tag(TAG).d("Custom cipher deobfuscation succeeded for $videoId")
        return finalUrl
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
        rendererRecoveryPolicy.onSuccess()

        return url.replaceFirst(
            Regex("([?&])n=[^&]+"),
            "$1n=${Uri.encode(transformedN)}"
        )
    }

    private suspend fun getOrCreateWebView(forceRefresh: Boolean): CipherWebView? {

        if (cipherWebView?.isDead == true) {
            Timber.tag(TAG).w("Cached cipher WebView renderer is dead - discarding")
            closeWebView()
        }

        val nowMs = SystemClock.elapsedRealtime()
        if (!rendererRecoveryPolicy.shouldAttempt(nowMs)) {
            Timber.tag(TAG).w(
                "Skipping cipher WebView creation: ${rendererRecoveryPolicy.consecutiveFailures} " +
                    "consecutive renderer deaths, in backoff window (backoffUntilMs=${rendererRecoveryPolicy.backoffUntilMs})"
            )
            return null
        }

        if (!forceRefresh && cipherWebView != null) {
            return cipherWebView
        }
        if (cipherWebView != null) {
            closeWebView()
        }

        val result = PlayerJsFetcher.getPlayerJs(forceRefresh = forceRefresh)
        if (result == null) {
            Timber.tag(TAG).e("Failed to get player JS")
            return null
        }
        val (playerJs, hash) = result

        if (PlayerConfigStore.get(hash) == null) {
            Timber.tag(TAG).d("Lookup miss for player hash $hash - fetching remote config table...")
            PlayerConfigStore.triggerUpdate()
        }

        val analysis = FunctionNameExtractor.analyzePlayerJs(playerJs, knownHash = hash)
        if (analysis.sigInfo == null) {
            Timber.tag(TAG).e("Could not extract signature function info from player JS (hash=$hash)")
            return null
        }
        if (analysis.nFuncInfo == null) {
            Timber.tag(TAG).w("Could not extract n-function info from player JS (hash=$hash)")
        }

        val webView = CipherWebView.create(
            context = appContext,
            playerJs = playerJs,
            sigInfo = analysis.sigInfo,
            nFuncInfo = analysis.nFuncInfo,
        )

        cipherWebView = webView
        currentPlayerHash = hash
        return webView
    }

    private suspend fun closeWebView() {
        withContext(Dispatchers.Main) {
            runCatching { cipherWebView?.close() }
                .onFailure { Timber.tag(TAG).w("closeWebView threw: $it") }
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

    fun getDebugInfo(): Map<String, Any?> = mapOf(
        "hasWebView" to (cipherWebView != null),
        "playerHash" to currentPlayerHash,
        "nFunctionAvailable" to cipherWebView?.nFunctionAvailable,
        "sigFunctionAvailable" to cipherWebView?.sigFunctionAvailable,
        "discoveredNFuncName" to cipherWebView?.discoveredNFuncName,
        "usingHardcodedMode" to cipherWebView?.usingHardcodedMode,
    )
}
