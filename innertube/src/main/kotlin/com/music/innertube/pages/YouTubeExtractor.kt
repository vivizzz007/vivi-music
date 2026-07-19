package com.music.innertube

import okhttp3.OkHttpClient
import okhttp3.Request
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Standalone Rhino-based signature and n-parameter transform deobfuscator.
 *
 * Dynamically resolves, extracts, and executes the decipher functions directly
 * from the YouTube base player JS using an in-memory Mozilla Rhino JavaScript engine.
 *
 * Features:
 * - Dynamic pattern matching: Parses function bodies directly from the player base script.
 * - Time-based caching: Persists deobfuscated JS snippets to disk for up to 6 hours,
 *   bypassing network lookups completely on subsequent plays.
 * - No WebView overhead: Executes scripts in milliseconds inside JVM memory.
 */
object YouTubeExtractor {
    private val client = OkHttpClient.Builder().build()
    private var cachedPlayerJs: String? = null
    private var deobfuscateJsCode: String? = null
    private var deobfuscateFuncName: String? = null
    private var transformNJsCode: String? = null
    private var transformNFuncName: String? = null
    private var currentResolvedUrl: String? = null

    var cacheDir: File? = null

    /**
     * Returns true if decipher scripts are already loaded in memory
     * (either from disk cache or from a prior network fetch).
     */
    val isReady: Boolean
        get() = deobfuscateJsCode != null && transformNJsCode != null

    /**
     * Pre-loads and compiles the YouTube player decipher scripts in the background.
     * Call this from App.onCreate() on a background thread so the very first
     * song play resolves without any waiting.
     */
    fun ensureInitialized() {
        if (isReady) return
        runCatching { getPlayerJs() }
    }


    private fun loadCache(resolvedPlayerJsUrl: String): Boolean {
        val dir = cacheDir ?: return false
        try {
            val cachedUrlFile = File(dir, "yt_player_url.txt")
            if (!cachedUrlFile.exists()) return false
            val cachedUrl = cachedUrlFile.readText().trim()
            if (cachedUrl != resolvedPlayerJsUrl) {
                println("[YouTubeExtractor] Cache is stale: cached=$cachedUrl, resolved=$resolvedPlayerJsUrl")
                return false
            }

            val sigJsFile = File(dir, "yt_sig_js.txt")
            val sigFuncFile = File(dir, "yt_sig_func.txt")
            val nJsFile = File(dir, "yt_n_js.txt")
            val nFuncFile = File(dir, "yt_n_func.txt")

            if (sigJsFile.exists() && sigFuncFile.exists() && nJsFile.exists() && nFuncFile.exists()) {
                deobfuscateJsCode = sigJsFile.readText()
                deobfuscateFuncName = sigFuncFile.readText().trim()
                transformNJsCode = nJsFile.readText()
                transformNFuncName = nFuncFile.readText().trim()
                println("[YouTubeExtractor] Loaded decipher snippets from disk cache successfully")
                return true
            }
        } catch (e: Exception) {
            println("[YouTubeExtractor] Loading disk cache failed: ${e.message}")
        }
        return false
    }

    private fun saveCache(resolvedPlayerJsUrl: String) {
        val dir = cacheDir ?: return
        try {
            File(dir, "yt_player_url.txt").writeText(resolvedPlayerJsUrl)
            File(dir, "yt_player_cache_time.txt").writeText(System.currentTimeMillis().toString())
            deobfuscateJsCode?.let { File(dir, "yt_sig_js.txt").writeText(it) }
            deobfuscateFuncName?.let { File(dir, "yt_sig_func.txt").writeText(it) }
            transformNJsCode?.let { File(dir, "yt_n_js.txt").writeText(it) }
            transformNFuncName?.let { File(dir, "yt_n_func.txt").writeText(it) }
            println("[YouTubeExtractor] Saved decipher snippets to disk cache successfully")
        } catch (e: Exception) {
            println("[YouTubeExtractor] Saving disk cache failed: ${e.message}")
        }
    }

    private fun saveCacheIfComplete() {
        val resolvedUrl = currentResolvedUrl ?: return
        if (deobfuscateJsCode != null && transformNJsCode != null) {
            saveCache(resolvedUrl)
        }
    }

    private fun fetchUrl(url: String): String {
        println("[YouTubeExtractor] Fetching URL: $url")
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                println("[YouTubeExtractor] HTTP error ${response.code} for URL: $url")
                throw java.io.IOException("HTTP error: ${response.code}")
            }
            val body = response.body?.string() ?: ""
            println("[YouTubeExtractor] Successfully fetched URL: $url (length=${body.length})")
            return body
        }
    }

    private fun getPlayerJs(): String {
        cachedPlayerJs?.let { return it }

        // Fast-path: Check if cached decipher files are present and fresh (less than 6 hours old)
        val dir = cacheDir
        if (dir != null) {
            try {
                val timeFile = File(dir, "yt_player_cache_time.txt")
                if (timeFile.exists()) {
                    val lastSaved = timeFile.readText().trim().toLongOrNull() ?: 0L
                    val age = System.currentTimeMillis() - lastSaved
                    if (age in 0 until (6 * 3600 * 1000)) { // 6 hours
                        val sigJsFile = File(dir, "yt_sig_js.txt")
                        val sigFuncFile = File(dir, "yt_sig_func.txt")
                        val nJsFile = File(dir, "yt_n_js.txt")
                        val nFuncFile = File(dir, "yt_n_func.txt")
                        if (sigJsFile.exists() && sigFuncFile.exists() && nJsFile.exists() && nFuncFile.exists()) {
                            deobfuscateJsCode = sigJsFile.readText()
                            deobfuscateFuncName = sigFuncFile.readText().trim()
                            transformNJsCode = nJsFile.readText()
                            transformNFuncName = nFuncFile.readText().trim()
                            println("[YouTubeExtractor] Loaded fresh decipher snippets from cache immediately (age=${age/1000}s)")
                            cachedPlayerJs = "" // Set non-null to bypass subsequent fetches
                            return ""
                        }
                    }
                }
            } catch (e: Exception) {
                println("[YouTubeExtractor] Failed to read fresh cache check: ${e.message}")
            }
        }

        println("[YouTubeExtractor] Resolving YouTube player JS URL...")
        val iframeApi = fetchUrl("https://www.youtube.com/iframe_api")
        val hashMatch = Regex("""player\\/([a-z0-9]{8})\\/""").find(iframeApi)
        val playerJsUrl = if (hashMatch != null) {
            val url = "https://www.youtube.com/s/player/${hashMatch.groupValues[1]}/player_ias.vflset/en_GB/base.js"
            println("[YouTubeExtractor] Found player JS URL via iframe_api: $url")
            url
        } else {
            println("[YouTubeExtractor] iframe_api regex match failed. Trying watch embed fallback...")
            val embedPage = fetchUrl("https://www.youtube.com/embed/dQw4w9WgXcQ")
            val embedMatch = Regex(""""jsUrl":"(/s/player/[A-Za-z0-9]+/player_ias\.vflset/[A-Za-z_-]+/base\.js)"""").find(embedPage)
            if (embedMatch != null) {
                val url = "https://www.youtube.com" + embedMatch.groupValues[1]
                println("[YouTubeExtractor] Found player JS URL via embed page fallback: $url")
                url
            } else {
                println("[YouTubeExtractor] Embed page fallback also failed!")
                throw IllegalStateException("Failed to locate YouTube base player JS URL")
            }
        }

        currentResolvedUrl = playerJsUrl

        if (loadCache(playerJsUrl)) {
            cachedPlayerJs = "" // Non-null to avoid re-runs
            return ""
        }

        val playerJs = fetchUrl(playerJsUrl)
        cachedPlayerJs = playerJs
        return playerJs
    }

    private fun matchToClosingBrace(str: String, startIndex: Int): String {
        var braceCount = 0
        var inString = false
        var stringChar = ' '
        var isEscaped = false
        for (i in startIndex until str.length) {
            val c = str[i]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (c == '\\') {
                isEscaped = true
                continue
            }
            if (inString) {
                if (c == stringChar) {
                    inString = false
                }
                continue
            }
            if (c == '"' || c == '\'') {
                inString = true
                stringChar = c
                continue
            }
            if (c == '{') {
                braceCount++
            } else if (c == '}') {
                braceCount--
                if (braceCount == 0) {
                    return str.substring(startIndex, i + 1)
                }
            }
        }
        throw IllegalArgumentException("No matching closing brace found")
    }

    private fun prepareSignatureDeobfuscator(playerJs: String) {
        if (deobfuscateJsCode != null) return
        println("[YouTubeExtractor] Preparing signature deobfuscator...")

        val sigPatterns = listOf(
            Regex("""\b(?:[a-zA-Z0-9_${'$'}]+)&&\((?:[a-zA-Z0-9_${'$'}]+)=([a-zA-Z0-9_${'$'}]{2,})\((\d+,)decodeURIComponent\((?:[a-zA-Z0-9_${'$'}]+)\)\)\)"""),
            Regex("""\b(?:[a-zA-Z0-9_${'$'}]+)&&\((?:[a-zA-Z0-9_${'$'}]+)=([a-zA-Z0-9_${'$'}]{2,})\(decodeURIComponent\((?:[a-zA-Z0-9_${'$'}]+)\)\)\)"""),
            Regex("""\bm=([a-zA-Z0-9${'$'}]{2,})\(decodeURIComponent\(h\.s\)\)"""),
            Regex("""\bc&&\(c=([a-zA-Z0-9${'$'}]{2,})\(decodeURIComponent\(c\)\)"""),
            Regex("""(?:\b|[^a-zA-Z0-9${'$'}])([a-zA-Z0-9${'$'}]{2,})\s*=\s*function\(\s*a\s*\)\s*\{\s*a\s*=\s*a\.split\(\s*""\s*\)"""),
            Regex("""([\w${'$'}]+)\s*=\s*function\((\w+)\)\{\s*\2=\s*\2\.split\(""\)\s*;""")
        )

        var funcName: String? = null
        for (pattern in sigPatterns) {
            val match = pattern.find(playerJs)
            if (match != null) {
                funcName = match.groupValues[1]
                println("[YouTubeExtractor] Matched signature function name: $funcName")
                break
            }
        }

        if (funcName == null) throw IllegalStateException("Signature deobfuscate function name not found")

        val funcStartKey = "$funcName=function"
        val funcIndex = playerJs.indexOf(funcStartKey)
        if (funcIndex == -1) throw IllegalStateException("Signature deobfuscate function body not found")

        val funcBody = funcStartKey + matchToClosingBrace(playerJs, funcIndex + funcStartKey.length)
        println("[YouTubeExtractor] Extracted signature function body (length=${funcBody.length})")

        val helperObjNameMatch = Regex("""[;,]([A-Za-z0-9_${'$'}]{2,})\[..""").find(funcBody)
            ?: Regex("""([A-Za-z0-9_${'$'}]{2,})\.""").find(funcBody)
            ?: throw IllegalStateException("Helper object name not found")

        val helperObjName = helperObjNameMatch.groupValues[1]
        println("[YouTubeExtractor] Matched signature helper object name: $helperObjName")

        val helperStartKey = "var $helperObjName="
        val helperIndex = playerJs.indexOf(helperStartKey)
        if (helperIndex == -1) throw IllegalStateException("Helper object body not found")

        val helperBody = helperStartKey + matchToClosingBrace(playerJs, helperIndex + helperStartKey.length) + ";"
        println("[YouTubeExtractor] Extracted signature helper object body (length=${helperBody.length})")

        deobfuscateFuncName = funcName
        deobfuscateJsCode = "$helperBody\nvar $funcBody;"
        println("[YouTubeExtractor] Signature deobfuscator prepared successfully")
        
        saveCacheIfComplete()
    }

    private fun prepareThrottlingDeobfuscator(playerJs: String) {
        if (transformNJsCode != null) return
        println("[YouTubeExtractor] Preparing throttling deobfuscator...")

        val nPatterns = listOf(
            Regex("""([A-Za-z0-9_\${'$'}]{2,})=function.*return [A-Z]\[\d+\]"""),
            Regex("""[a-zA-Z0-9_${'$'}]="nn"\[\+[a-zA-Z0-9_${'$'}]+\.[a-zA-Z0-9_${'$'}]+\],[a-zA-Z0-9_${'$'}]+\([a-zA-Z0-9_${'$'}]+\),[a-zA-Z0-9_${'$'}]+=[a-zA-Z0-9_${'$'}]+\.[a-zA-Z0-9_${'$'}]+\[[a-zA-Z0-9_${'$'}]+\]\|\|null\)&&\([a-zA-Z0-9_${'$'}]+=\b([a-zA-Z0-9_${'$'}]+)\[\d+\]"""),
            Regex("""\.get\("n"\)\)&&\([a-zA-Z0-9_${'$'}]+=\b([a-zA-Z0-9_${'$'}]+)(?:\[\d+\])?\([a-zA-Z0-9_${'$'}]+\)""")
        )

        var funcName: String? = null
        for (pattern in nPatterns) {
            val match = pattern.find(playerJs)
            if (match != null) {
                funcName = match.groupValues[1]
                println("[YouTubeExtractor] Matched throttling function name: $funcName")
                break
            }
        }

        if (funcName == null) throw IllegalStateException("Throttling deobfuscate function name not found")

        val funcStartKey = "$funcName=function"
        val funcIndex = playerJs.indexOf(funcStartKey)
        if (funcIndex == -1) throw IllegalStateException("Throttling deobfuscate function body not found")

        var funcBody = funcStartKey + matchToClosingBrace(playerJs, funcIndex + funcStartKey.length)
        println("[YouTubeExtractor] Extracted throttling function body (length=${funcBody.length})")

        // Remove early return
        val firstArgMatch = Regex("""=function\s*\(\s*([^)]*)\s*\)""").find(funcBody)
        if (firstArgMatch != null) {
            val argName = firstArgMatch.groupValues[1].split(",")[0].trim()
            funcBody = funcBody.replace(Regex(""";\s*if\s*\(\s*typeof\s+[a-zA-Z0-9_${'$'}]+\s*===?\s*(["'])undefined\1\s*\)\s*return\s+${Regex.escape(argName)};"""), ";")
            println("[YouTubeExtractor] Cleaned early return checking '$argName'")
        }

        transformNFuncName = funcName
        transformNJsCode = "var $funcBody;"
        println("[YouTubeExtractor] Throttling deobfuscator prepared successfully")
        
        saveCacheIfComplete()
    }

    fun decryptSignature(s: String): String {
        try {
            val playerJs = getPlayerJs()
            if (deobfuscateJsCode == null && playerJs.isNotEmpty()) {
                prepareSignatureDeobfuscator(playerJs)
            }
            val code = deobfuscateJsCode ?: return s
            val func = deobfuscateFuncName ?: return s

            println("[YouTubeExtractor] decryptSignature: decrypting $s ...")
            val res = evaluateJs(code, func, s)
            println("[YouTubeExtractor] decryptSignature result: original=$s -> decrypted=$res")
            return res
        } catch (e: Exception) {
            println("[YouTubeExtractor] decryptSignature failed: ${e.message}")
            e.printStackTrace()
            return s
        }
    }

    fun deobfuscateThrottling(n: String): String {
        try {
            val playerJs = getPlayerJs()
            if (transformNJsCode == null && playerJs.isNotEmpty()) {
                prepareThrottlingDeobfuscator(playerJs)
            }
            val code = transformNJsCode ?: return n
            val func = transformNFuncName ?: return n

            println("[YouTubeExtractor] deobfuscateThrottling: deobfuscating $n ...")
            val res = evaluateJs(code, func, n)
            println("[YouTubeExtractor] deobfuscateThrottling result: original=$n -> deobfuscated=$res")
            return res
        } catch (e: Exception) {
            println("[YouTubeExtractor] deobfuscateThrottling failed: ${e.message}")
            e.printStackTrace()
            return n
        }
    }

    private fun evaluateJs(code: String, functionName: String, parameter: String): String {
        val context = Context.enter()
        return try {
            context.optimizationLevel = -1
            val scope = context.initSafeStandardObjects()
            context.evaluateString(scope, code, "deobfuscator", 1, null)
            val jsFunction = scope.get(functionName, scope) as Function
            val result = jsFunction.call(context, scope, scope, arrayOf(parameter))
            result?.toString() ?: parameter
        } finally {
            Context.exit()
        }
    }

    fun parseQueryParams(query: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (pair in query.split("&")) {
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                result[key] = value
            }
        }
        return result
    }

    fun decryptUrl(signatureCipher: String): String {
        println("[YouTubeExtractor] decryptUrl: processing signatureCipher...")
        val params = parseQueryParams(signatureCipher)
        val obfuscatedSig = params["s"] ?: return ""
        val sigParam = params["sp"] ?: "signature"
        val baseUrl = params["url"] ?: return ""

        val decryptedSig = decryptSignature(obfuscatedSig)
        val separator = if ("?" in baseUrl) "&" else "?"
        val urlWithSig = "$baseUrl${separator}${sigParam}=${URLEncoder.encode(decryptedSig, "UTF-8")}"

        return deobfuscateUrlNParam(urlWithSig)
    }

    fun deobfuscateUrlNParam(url: String): String {
        val nMatch = Regex("[?&]n=([^&]+)").find(url) ?: return url
        val obfuscatedN = URLDecoder.decode(nMatch.groupValues[1], "UTF-8")
        val deobfuscatedN = deobfuscateThrottling(obfuscatedN)
        return url.replaceFirst(
            Regex("([?&])n=[^&]+"),
            "$1n=${URLEncoder.encode(deobfuscatedN, "UTF-8")}"
        )
    }
}
