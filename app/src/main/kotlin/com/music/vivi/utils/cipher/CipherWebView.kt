/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils.cipher

import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * WebView-based cipher executor for YouTube stream URL deobfuscation
 *
 * Executes signature decipher and n-transform functions extracted from player.js.
 * Supports both regex-extracted functions and hardcoded fallback for Q-array obfuscated players.
 */
class CipherWebView private constructor(
    context: Context,
    private val playerJs: String,
    private val sigInfo: FunctionNameExtractor.SigFunctionInfo?,
    private val nFuncInfo: FunctionNameExtractor.NFunctionInfo?,
    private val initContinuation: Continuation<CipherWebView>,
) {
    private val webView = WebView(context)
    private var sigContinuation: Continuation<String>? = null
    private var nContinuation: Continuation<String>? = null

    @Volatile
    var nFunctionAvailable: Boolean = false
        private set

    @Volatile
    var sigFunctionAvailable: Boolean = false
        private set

    @Volatile
    var discoveredNFuncName: String? = null
        private set

    @Volatile
    var usingHardcodedMode: Boolean = false
        private set

    init {
        Timber.tag(TAG).d("Initializing CipherWebView...")
        Timber.tag(TAG).d("  sigInfo: name=${sigInfo?.name}, constantArg=${sigInfo?.constantArg}, hardcoded=${sigInfo?.isHardcoded}")
        Timber.tag(TAG).d("  nFuncInfo: name=${nFuncInfo?.name}, arrayIdx=${nFuncInfo?.arrayIndex}, hardcoded=${nFuncInfo?.isHardcoded}")

        val settings = webView.settings
        @Suppress("SetJavaScriptEnabled")
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        @Suppress("DEPRECATION")
        settings.allowFileAccessFromFileURLs = true
        settings.blockNetworkLoads = true

        webView.addJavascriptInterface(this, JS_INTERFACE)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                val msg = m.message()
                val src = "${m.sourceId()}:${m.lineNumber()}"

                // Log all console messages for debugging
                when (m.messageLevel()) {
                    ConsoleMessage.MessageLevel.ERROR -> {
                        if (!msg.contains("is not defined")) {
                            Timber.tag(TAG).e("JS ERROR: $msg at $src")
                        }
                    }
                    ConsoleMessage.MessageLevel.WARNING -> {
                        Timber.tag(TAG).w("JS WARN: $msg at $src")
                    }
                    else -> {
                        Timber.tag(TAG).v("JS LOG: $msg")
                    }
                }
                return super.onConsoleMessage(m)
            }
        }

        Timber.tag(TAG).d("WebView settings configured")
    }

    private fun loadPlayerJsFromFile() {
        val sigFuncName = sigInfo?.name
        val nFuncName = nFuncInfo?.name
        val nArrayIdx = nFuncInfo?.arrayIndex
        val isHardcoded = sigInfo?.isHardcoded == true || nFuncInfo?.isHardcoded == true

        Timber.tag(TAG).d("=== LOADING PLAYER.JS INTO WEBVIEW ===")
        Timber.tag(TAG).d("Player.js size: ${playerJs.length} chars")
        Timber.tag(TAG).d("Export mode: ${if (isHardcoded) "HARDCODED" else "EXTRACTED"}")
        Timber.tag(TAG).d("Sig function: $sigFuncName (constantArg=${sigInfo?.constantArg})")
        Timber.tag(TAG).d("N function: $nFuncName (arrayIdx=$nArrayIdx)")

        usingHardcodedMode = isHardcoded

        val exports = buildList {
            val sigJsExpr = sigInfo?.jsExpression
            if (sigJsExpr != null) {
                val expr = sigJsExpr.replace("INPUT", "sig")
                Timber.tag(TAG).d("Sig: expression-based export: $expr")
                add("window._cipherSigFunc = function(sig) { try { return $expr; } catch(e) { return null; } };")
            } else if (sigFuncName != null) {
                val sigConstArgs = sigInfo?.constantArgs
                val preprocessFunc = sigInfo?.preprocessFunc
                val preprocessArgs = sigInfo?.preprocessArgs

                if (!sigConstArgs.isNullOrEmpty() && preprocessFunc != null && !preprocessArgs.isNullOrEmpty()) {
                    val mainArgsStr = sigConstArgs.joinToString(", ")
                    val prepArgsStr = preprocessArgs.joinToString(", ")
                    Timber.tag(TAG).d("Sig function needs full wrapper:")
                    Timber.tag(TAG).d("  $sigFuncName($mainArgsStr, $preprocessFunc($prepArgsStr, sig))")
                    add("window._cipherSigFunc = function(sig) { return $sigFuncName($mainArgsStr, $preprocessFunc($prepArgsStr, sig)); };")
                } else if (!sigConstArgs.isNullOrEmpty()) {
                    val argsStr = sigConstArgs.joinToString(", ")
                    Timber.tag(TAG).d("Sig function needs wrapper with constant args: $argsStr")
                    add("window._cipherSigFunc = function(sig) { return $sigFuncName($argsStr, sig); };")
                } else if (isHardcoded) {
                    Timber.tag(TAG).d("Will export sig function $sigFuncName in hardcoded mode (legacy)")
                    add("window._cipherSigFunc = typeof $sigFuncName !== 'undefined' ? $sigFuncName : null;")
                } else {
                    add("window._cipherSigFunc = typeof $sigFuncName !== 'undefined' ? $sigFuncName : null;")
                }
            }
            val nJsExpr = nFuncInfo?.jsExpression
            if (nJsExpr != null) {
                val expr = nJsExpr.replace("INPUT", "n")
                Timber.tag(TAG).d("N: expression-based export: ${expr.take(80)}")
                add("window._nTransformFunc = function(n) { try { return $expr; } catch(e) { return n; } };")
            } else if (nFuncName != null) {
                val nConstArgs = nFuncInfo?.constantArgs
                if (!nConstArgs.isNullOrEmpty()) {
                    val argsStr = nConstArgs.joinToString(", ")
                    Timber.tag(TAG).d("N-function needs wrapper with constant args: $argsStr")
                    add("window._nTransformFunc = function(n) { return $nFuncName($argsStr, n); };")
                } else {
                    val nExpr = if (nArrayIdx != null) {
                        "$nFuncName[$nArrayIdx]"
                    } else {
                        nFuncName
                    }
                    add("window._nTransformFunc = typeof $nFuncName !== 'undefined' ? $nExpr : null;")
                }
            }
        }

        Timber.tag(TAG).d("Export statements: ${exports.size}")
        exports.forEachIndexed { idx, stmt ->
            Timber.tag(TAG).v("  Export[$idx]: ${stmt.take(80)}...")
        }

        val modifiedJs = if (exports.isNotEmpty()) {
            val exportCode = "; " + exports.joinToString(" ")
            val modified = playerJs.replace("})(_yt_player);", "$exportCode })(_yt_player);")
            if (modified == playerJs) {
                Timber.tag(TAG).w("Export injection point '})(_yt_player);' not found, appending exports")
                playerJs + "\n" + exportCode
            } else {
                Timber.tag(TAG).d("Exports injected into IIFE closure")
                modified
            }
        } else {
            Timber.tag(TAG).w("No exports to inject")
            playerJs
        }

        val cacheDir = File(webView.context.cacheDir, "cipher")
        cacheDir.mkdirs()
        val playerJsFile = File(cacheDir, "player.js")
        playerJsFile.writeText(modifiedJs)
        Timber.tag(TAG).d("Player.js written to cache: ${playerJsFile.absolutePath} (${modifiedJs.length} chars)")

        // Build HTML with comprehensive discovery and validation
        val html = buildDiscoveryHtml()
        Timber.tag(TAG).d("Discovery HTML built (${html.length} chars)")

        webView.loadDataWithBaseURL(
            "file://${cacheDir.absolutePath}/",
            html, "text/html", "utf-8", null
        )
        Timber.tag(TAG).d("WebView loading started...")
    }

    /**
     * Build HTML with JS discovery logic
     */
    private fun buildDiscoveryHtml(): String = """<!DOCTYPE html>
<html><head><script>
// ============================================================
// SIGNATURE DEOBFUSCATION
// ============================================================
function deobfuscateSig(funcName, constantArg, obfuscatedSig) {
    CipherBridge.logDebug("deobfuscateSig called: funcName=" + funcName + ", constantArg=" + constantArg + ", sigLen=" + obfuscatedSig.length);

    try {
        var func = window._cipherSigFunc;
        CipherBridge.logDebug("window._cipherSigFunc type: " + typeof func + ", length: " + (func ? func.length : "N/A"));

        if (typeof func !== 'function') {
            CipherBridge.onSigError("Sig func not found on window (type: " + typeof func + ")");
            return;
        }

        var result;
        // Check if this is a wrapper function (takes 1 arg: sig) vs direct function (takes 2+ args)
        if (func.length === 1) {
            // Wrapper function
            CipherBridge.logDebug("Calling wrapped sig func with just sig (func.length=1)");
            result = func(obfuscatedSig);
        } else if (constantArg !== null && constantArg !== undefined) {
            // Direct function with constantArg
            CipherBridge.logDebug("Calling sig func with constantArg: " + constantArg);
            result = func(constantArg, obfuscatedSig);
        } else {
            CipherBridge.logDebug("Calling sig func without constantArg");
            result = func(obfuscatedSig);
        }

        if (result === undefined || result === null) {
            CipherBridge.onSigError("Function returned null/undefined");
            return;
        }

        CipherBridge.logDebug("Sig result type: " + typeof result + ", length: " + String(result).length);
        CipherBridge.onSigResult(String(result));
    } catch (error) {
        CipherBridge.onSigError(error + "\n" + (error.stack || ""));
    }
}

// ============================================================
// N-PARAMETER TRANSFORM
// ============================================================
function transformN(nValue) {
    CipherBridge.logDebug("transformN called: nValue=" + nValue);

    try {
        var func = window._nTransformFunc;
        CipherBridge.logDebug("window._nTransformFunc type: " + typeof func);

        if (typeof func !== 'function') {
            CipherBridge.onNError("N-transform func not available (type: " + typeof func + ")");
            return;
        }

        var result = func(nValue);
        CipherBridge.logDebug("N-transform raw result: " + (result ? String(result).substring(0, 50) : "null/undefined"));

        if (result === undefined || result === null) {
            CipherBridge.onNError("N-transform returned null/undefined");
            return;
        }

        var resultStr = String(result);
        CipherBridge.logDebug("N-transform result: length=" + resultStr.length + ", value=" + resultStr.substring(0, 30));
        CipherBridge.onNResult(resultStr);
    } catch (error) {
        CipherBridge.onNError(error + "\n" + (error.stack || ""));
    }
}

// ============================================================
// FUNCTION DISCOVERY AND INITIALIZATION
// ============================================================
function discoverAndInit() {
    CipherBridge.logDebug("========== DISCOVERY AND INIT ==========");

    var nFuncName = "";
    var sigFuncName = "";
    var info = "";

    // Check if signature function was exported
    if (typeof window._cipherSigFunc === 'function') {
        sigFuncName = "exported_sig_func";
        CipherBridge.logDebug("Signature function found on window._cipherSigFunc");
    } else {
        CipherBridge.logDebug("WARNING: window._cipherSigFunc not available (type=" + typeof window._cipherSigFunc + ")");
    }

    // Check if N-transform function was exported
    if (typeof window._nTransformFunc === 'function') {
        CipherBridge.logDebug("Testing exported window._nTransformFunc...");
        try {
            var testInput = "KdrqFlzJXl9EcCwlmEy";
            var testResult = window._nTransformFunc(testInput);

            CipherBridge.logDebug("N-func test input: " + testInput);
            CipherBridge.logDebug("N-func test result: " + (testResult ? String(testResult).substring(0, 50) : "null"));

            if (typeof testResult === 'string' && testResult !== testInput && testResult.length >= 5) {
                if (/^[a-zA-Z0-9_-]+$/.test(testResult)) {
                    nFuncName = "exported_n_func";
                    info = "export_valid,test=" + testResult.substring(0, 20);
                    CipherBridge.logDebug("N-function VALID: " + testResult);
                } else {
                    info = "export_bad_chars:" + testResult.substring(0, 20);
                    CipherBridge.logDebug("N-function has invalid characters");
                    window._nTransformFunc = null;
                }
            } else {
                info = "export_bad_result:type=" + typeof testResult + ",eq=" + (testResult === testInput);
                CipherBridge.logDebug("N-function test failed: " + info);
                window._nTransformFunc = null;
            }
        } catch(e) {
            info = "export_threw:" + e;
            CipherBridge.logDebug("N-function threw exception: " + e);
            window._nTransformFunc = null;
        }
    } else {
        CipherBridge.logDebug("window._nTransformFunc not exported, trying brute force discovery...");
    }

    // Brute force discovery if export failed
    if (!nFuncName) {
        try {
            var testInput = "T2Xw3pWQ_Wk0xbOg";
            var keys = Object.getOwnPropertyNames(window);
            var tested = 0;
            var candidates = [];
            var skipped = 0;

            CipherBridge.logDebug("Brute force: scanning " + keys.length + " window properties");

            for (var i = 0; i < keys.length; i++) {
                try {
                    var key = keys[i];
                    if (key.startsWith("webkit") || key.startsWith("on") ||
                        key === "CipherBridge" || key === "_cipherSigFunc" ||
                        key === "_nTransformFunc" || key === "window" || key === "self") {
                        skipped++;
                        continue;
                    }

                    var fn = window[key];
                    if (typeof fn !== 'function') continue;
                    if (fn.length !== 1) continue;

                    tested++;
                    var result = fn(testInput);

                    if (typeof result === 'string' && result !== testInput && result.length >= 5) {
                        if (/^[a-zA-Z0-9_-]+$/.test(result)) {
                            candidates.push({
                                name: key,
                                result: result.substring(0, 30),
                                len: result.length
                            });

                            if (!nFuncName) {
                                window._nTransformFunc = fn;
                                nFuncName = key;
                                CipherBridge.logDebug("N-function discovered: " + key + " -> " + result.substring(0, 30));
                            }
                        }
                    }
                } catch(e) {}
            }

            info = "brute_force:tested=" + tested + "/skipped=" + skipped + "/total=" + keys.length;
            if (candidates.length > 0) {
                info += ",candidates=" + candidates.length;
                CipherBridge.logDebug("Candidates found: " + JSON.stringify(candidates.slice(0, 5)));
            }
        } catch(e) {
            info = "brute_force_error:" + e;
            CipherBridge.logDebug("Brute force failed: " + e);
        }
    }

    CipherBridge.logDebug("Discovery complete:");
    CipherBridge.logDebug("  sigFuncName=" + sigFuncName);
    CipherBridge.logDebug("  nFuncName=" + nFuncName);
    CipherBridge.logDebug("  info=" + info);

    CipherBridge.onDiscoveryDone(sigFuncName, nFuncName, info);
    CipherBridge.onPlayerJsLoaded();
}
</script>
<script src="player.js"
    onload="discoverAndInit()"
    onerror="CipherBridge.onPlayerJsError('Failed to load player.js from file')">
</script>
</head><body></body></html>"""

    // ==================== JAVASCRIPT INTERFACE ====================

    @JavascriptInterface
    fun logDebug(message: String) {
        Timber.tag(TAG).d("JS: $message")
    }

    @JavascriptInterface
    fun onDiscoveryDone(sigFuncName: String, nFuncName: String, info: String) {
        Timber.tag(TAG).d("=== DISCOVERY COMPLETE ===")
        Timber.tag(TAG).d("Sig function: ${sigFuncName.ifEmpty { "NOT FOUND" }}")
        Timber.tag(TAG).d("N function: ${nFuncName.ifEmpty { "NOT FOUND" }}")
        Timber.tag(TAG).d("Info: $info")

        sigFunctionAvailable = sigFuncName.isNotEmpty()
        if (nFuncName.isNotEmpty()) {
            discoveredNFuncName = nFuncName
            nFunctionAvailable = true
            Timber.tag(TAG).d("N-function AVAILABLE: $nFuncName")
        } else {
            Timber.tag(TAG).e("N-function NOT AVAILABLE")
            nFunctionAvailable = false
        }
    }

    @JavascriptInterface
    fun onNDiscoveryDone(funcName: String, info: String) {
        // Legacy interface - redirects to new combined discovery
        Timber.tag(TAG).d("Legacy onNDiscoveryDone: funcName=$funcName, info=$info")
        if (funcName.isNotEmpty()) {
            discoveredNFuncName = funcName
            nFunctionAvailable = true
        }
    }

    @JavascriptInterface
    fun onPlayerJsLoaded() {
        Timber.tag(TAG).d("=== PLAYER.JS LOAD COMPLETE ===")
        Timber.tag(TAG).d("sigFunctionAvailable=$sigFunctionAvailable")
        Timber.tag(TAG).d("nFunctionAvailable=$nFunctionAvailable")
        Timber.tag(TAG).d("discoveredNFuncName=$discoveredNFuncName")
        Timber.tag(TAG).d("usingHardcodedMode=$usingHardcodedMode")

        initContinuation.resume(this)
    }

    @JavascriptInterface
    fun onPlayerJsError(error: String) {
        Timber.tag(TAG).e("=== PLAYER.JS LOAD FAILED ===")
        Timber.tag(TAG).e("Error: $error")
        initContinuation.resumeWithException(CipherException("Player JS load failed: $error"))
    }

    // ==================== SIGNATURE DEOBFUSCATION ====================

    suspend fun deobfuscateSignature(obfuscatedSig: String): String {
        Timber.tag(TAG).d("========== DEOBFUSCATE SIGNATURE ==========")
        Timber.tag(TAG).d("Input sig length: ${obfuscatedSig.length}")
        Timber.tag(TAG).d("Input sig preview: ${obfuscatedSig.take(50)}...")
        Timber.tag(TAG).d("sigInfo: name=${sigInfo?.name}, constantArg=${sigInfo?.constantArg}")

        if (sigInfo == null) {
            Timber.tag(TAG).e("Signature function info not available")
            throw CipherException("Signature function info not available")
        }

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                sigContinuation = cont
                val constArgJs = if (sigInfo.constantArg != null) "${sigInfo.constantArg}" else "null"
                val jsCall = "deobfuscateSig('${sigInfo.name}', $constArgJs, '${escapeJsString(obfuscatedSig)}')"
                Timber.tag(TAG).d("Evaluating JS: ${jsCall.take(100)}...")
                webView.evaluateJavascript(jsCall, null)
            }
        }
    }

    @JavascriptInterface
    fun onSigResult(result: String) {
        Timber.tag(TAG).d("========== SIGNATURE RESULT ==========")
        Timber.tag(TAG).d("Result length: ${result.length}")
        Timber.tag(TAG).d("Result preview: ${result.take(50)}...")
        sigContinuation?.resume(result)
        sigContinuation = null
    }

    @JavascriptInterface
    fun onSigError(error: String) {
        Timber.tag(TAG).e("========== SIGNATURE ERROR ==========")
        Timber.tag(TAG).e("Error: $error")
        sigContinuation?.resumeWithException(CipherException("Sig deobfuscation failed: $error"))
        sigContinuation = null
    }

    // ==================== N-TRANSFORM ====================

    suspend fun transformN(nValue: String): String {
        Timber.tag(TAG).d("========== N-TRANSFORM ==========")
        Timber.tag(TAG).d("Input n value: $nValue")
        Timber.tag(TAG).d("nFunctionAvailable: $nFunctionAvailable")
        Timber.tag(TAG).d("discoveredNFuncName: $discoveredNFuncName")

        if (!nFunctionAvailable) {
            Timber.tag(TAG).e("N-transform function not discovered")
            throw CipherException("N-transform function not discovered")
        }

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                nContinuation = cont
                val jsCall = "transformN('${escapeJsString(nValue)}')"
                Timber.tag(TAG).d("Evaluating JS: $jsCall")
                webView.evaluateJavascript(jsCall, null)
            }
        }
    }

    @JavascriptInterface
    fun onNResult(result: String) {
        Timber.tag(TAG).d("========== N-TRANSFORM RESULT ==========")
        Timber.tag(TAG).d("Result: $result")
        Timber.tag(TAG).d("Result length: ${result.length}")
        nContinuation?.resume(result)
        nContinuation = null
    }

    @JavascriptInterface
    fun onNError(error: String) {
        Timber.tag(TAG).e("========== N-TRANSFORM ERROR ==========")
        Timber.tag(TAG).e("Error: $error")
        nContinuation?.resumeWithException(CipherException("N-transform failed: $error"))
        nContinuation = null
    }

    // ==================== CLEANUP ====================

    fun close() {
        Timber.tag(TAG).d("Closing CipherWebView...")
        webView.clearHistory()
        webView.clearCache(true)
        webView.loadUrl("about:blank")
        webView.onPause()
        webView.removeAllViews()
        webView.destroy()
        Timber.tag(TAG).d("CipherWebView closed")
    }

    // ==================== UTILITIES ====================

    private fun escapeJsString(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    companion object {
        private const val TAG = "vivimusic_CipherWebView"
        private const val JS_INTERFACE = "CipherBridge"

        suspend fun create(
            context: Context,
            playerJs: String,
            sigInfo: FunctionNameExtractor.SigFunctionInfo?,
            nFuncInfo: FunctionNameExtractor.NFunctionInfo? = null,
        ): CipherWebView {
            Timber.tag(TAG).d("=== CREATING CIPHER WEBVIEW ===")
            Timber.tag(TAG).d("playerJs size: ${playerJs.length} chars")
            Timber.tag(TAG).d("sigInfo: $sigInfo")
            Timber.tag(TAG).d("nFuncInfo: $nFuncInfo")

            return withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { cont ->
                    val wv = CipherWebView(context, playerJs, sigInfo, nFuncInfo, cont)
                    wv.loadPlayerJsFromFile()
                }
            }
        }
    }
}

class CipherException(message: String) : Exception(message)
