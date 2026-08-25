/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils.cipher

import timber.log.Timber
import java.security.MessageDigest

object FunctionNameExtractor {
    private const val TAG = "vivi_CipherFnExtract"

    data class SigFunctionInfo(
        val name: String,
        val constantArg: Int?,
        val constantArgs: List<Int>? = null,
        val preprocessFunc: String? = null,
        val preprocessArgs: List<Int>? = null,
        val jsExpression: String? = null,
        val isHardcoded: Boolean = false
    )

    data class NFunctionInfo(
        val name: String,
        val arrayIndex: Int?,
        val constantArgs: List<Int>? = null,
        val jsExpression: String? = null,
        val isHardcoded: Boolean = false
    )

    data class HardcodedPlayerConfig(
        val sigFuncName: String,
        val sigConstantArg: Int?,
        val sigConstantArgs: List<Int>? = null,
        val sigPreprocessFunc: String? = null,
        val sigPreprocessArgs: List<Int>? = null,
        val sigJsExpression: String? = null,
        val nFuncName: String,
        val nArrayIndex: Int?,
        val nConstantArgs: List<Int>?,
        val nJsExpression: String? = null,
        val signatureTimestamp: Int
    )

    private val ANCHORED_STS_PATTERN = Regex("""signatureTimestamp['":\s]+(\d+)""")
    private val LOOSE_STS_PATTERN = Regex("""sts['":\s]+(\d+)""")

    private val Q_ARRAY_PATTERN = Regex("""var\s+Q\s*=\s*"[^"]+"\s*\.\s*split\s*\(\s*"\}"\s*\)""")

    private val PLAYER_HASH_PATTERNS = listOf(
        Regex("""jsUrl['":\s]+[^"']*?/player/([a-f0-9]{8})/"""),
        Regex("""player_ias\.vflset/[^/]+/([a-f0-9]{8})/"""),
        Regex("""/s/player/([a-f0-9]{8})/""")
    )

    private val SIG_FUNCTION_PATTERNS = listOf(

        Regex("""&&\s*\(\s*[a-zA-Z0-9$]+\s*=\s*([a-zA-Z0-9$]+)\s*\(\s*(\d+)\s*,\s*decodeURIComponent\s*\(\s*[a-zA-Z0-9$]+\s*\)"""),

        Regex("""&&\s*\(\s*[a-zA-Z0-9$]+\s*=\s*([a-zA-Z0-9$]+)\s*\(\s*(\d+)\s*,\s*decodeURIComponent\s*\(\s*[a-zA-Z0-9$]+\s*\.\s*[a-z]\s*\)"""),

        Regex("""\b[cs]\s*&&\s*[adf]\.set\([^,]+\s*,\s*encodeURIComponent\(([a-zA-Z0-9$]+)\("""),
        Regex("""\b[a-zA-Z0-9]+\s*&&\s*[a-zA-Z0-9]+\.set\([^,]+\s*,\s*encodeURIComponent\(([a-zA-Z0-9$]+)\("""),
        Regex("""\bm=([a-zA-Z0-9${'$'}]{2,})\(decodeURIComponent\(h\.s\)\)"""),
        Regex("""\bc\s*&&\s*d\.set\([^,]+\s*,\s*(?:encodeURIComponent\s*\()([a-zA-Z0-9$]+)\("""),
        Regex("""\bc\s*&&\s*[a-z]\.set\([^,]+\s*,\s*encodeURIComponent\(([a-zA-Z0-9$]+)\("""),
    )

    private val N_FUNCTION_PATTERNS = listOf(

        Regex("""\.get\("n"\)\)&&\(b=([a-zA-Z0-9$]+)(?:\[(\d+)\])?\(([a-zA-Z0-9])\)"""),

        Regex("""\.get\("n"\)\)\s*&&\s*\(([a-zA-Z0-9$]+)\s*=\s*([a-zA-Z0-9$]+)(?:\[(\d+)\])?\(\1\)"""),

        Regex("""\.get\("n"\);if\([a-zA-Z0-9$]+\)\s*\{[^}]*match"""),

        Regex("""\(\s*([a-zA-Z0-9$]+)\s*=\s*String\.fromCharCode\(110\)"""),

        Regex("""([a-zA-Z0-9$]+)\s*=\s*function\([a-zA-Z0-9]\)\s*\{[^}]*?enhanced_except_"""),
    )

    fun hasQArrayObfuscation(playerJs: String): Boolean {
        val hasQArray = Q_ARRAY_PATTERN.containsMatchIn(playerJs)
        Timber.tag(TAG).d("Q-array obfuscation check: hasQArray=$hasQArray")

        if (hasQArray) {

            val match = Q_ARRAY_PATTERN.find(playerJs)
            if (match != null) {
                val start = match.range.first
                val qDefEnd = playerJs.indexOf(";", start)
                if (qDefEnd > start) {
                    val qDef = playerJs.substring(start, qDefEnd)
                    val elementCount = qDef.count { it == '}' } + 1
                    Timber.tag(TAG).d("Q-array detected with ~$elementCount elements")
                }
            }
        }
        return hasQArray
    }

    fun extractPlayerHash(playerJs: String): String? {
        Timber.tag(TAG).d("Extracting player hash from playerJs (${playerJs.length} chars)")

        for ((index, pattern) in PLAYER_HASH_PATTERNS.withIndex()) {
            val match = pattern.find(playerJs)
            if (match != null) {
                val hash = match.groupValues[1]
                Timber.tag(TAG).d("Player hash found via pattern $index: $hash")
                return hash
            }
        }

        val contentToHash = playerJs.take(10000)
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(contentToHash.toByteArray())
        val computedHash = digest.take(4).joinToString("") { "%02x".format(it) }
        Timber.tag(TAG).d("Player hash computed from content: $computedHash")
        return computedHash
    }

    fun getHardcodedConfig(playerHash: String): HardcodedPlayerConfig? {
        val config = PlayerConfigStore.get(playerHash)
        if (config != null) {
            Timber.tag(TAG).d("Found config for hash $playerHash: sig=${config.sigFuncName} nFunc=${config.nFuncName} sts=${config.signatureTimestamp}")
        } else {
            Timber.tag(TAG).w("No config for hash: $playerHash")
        }
        return config
    }

    fun extractSigFunctionInfo(playerJs: String, knownHash: String? = null): SigFunctionInfo? {
        Timber.tag(TAG).d("========== EXTRACTING SIG FUNCTION ==========")
        Timber.tag(TAG).d("Player.js size: ${playerJs.length} chars")

        val hashToUse = knownHash ?: extractPlayerHash(playerJs)
        Timber.tag(TAG).d("Using hash for config lookup: $hashToUse (knownHash=$knownHash)")
        if (hashToUse != null) {
            val config = getHardcodedConfig(hashToUse)
            if (config != null) {
                if (config.sigJsExpression != null) {
                    Timber.tag(TAG).d("USING EXPRESSION-BASED SIG: ${config.sigJsExpression}")
                } else {
                    Timber.tag(TAG).d("USING HARDCODED SIG FUNCTION: ${config.sigFuncName}(${config.sigConstantArgs}, ...)")
                    Timber.tag(TAG).d("Sig preprocess: ${config.sigPreprocessFunc}(${config.sigPreprocessArgs}, sig)")
                }
                return SigFunctionInfo(
                    name = config.sigFuncName,
                    constantArg = config.sigConstantArg,
                    constantArgs = config.sigConstantArgs,
                    preprocessFunc = config.sigPreprocessFunc,
                    preprocessArgs = config.sigPreprocessArgs,
                    jsExpression = config.sigJsExpression,
                    isHardcoded = true
                )
            }
        }

        Timber.tag(TAG).w("No config for hash $hashToUse, trying legacy sig patterns...")
        for ((index, pattern) in SIG_FUNCTION_PATTERNS.withIndex()) {
            Timber.tag(TAG).v("Trying sig pattern $index: ${pattern.pattern.take(60)}...")
            val match = pattern.find(playerJs)
            if (match != null) {
                val name = match.groupValues[1]
                val constArg = if (match.groupValues.size > 2) match.groupValues[2].toIntOrNull() else null
                Timber.tag(TAG).d("SIG FUNCTION FOUND via pattern $index:")
                Timber.tag(TAG).d("  name=$name, constantArg=$constArg")
                Timber.tag(TAG).d("  match context: ...${playerJs.substring(maxOf(0, match.range.first - 20), minOf(playerJs.length, match.range.last + 20))}...")
                return SigFunctionInfo(name, constArg, isHardcoded = false)
            }
        }

        Timber.tag(TAG).e("========== SIG FUNCTION EXTRACTION FAILED ==========")
        Timber.tag(TAG).e("Could not find signature deobfuscation function name")
        return null
    }

    fun extractNFunctionInfo(playerJs: String, knownHash: String? = null): NFunctionInfo? {
        Timber.tag(TAG).d("========== EXTRACTING N-FUNCTION ==========")
        Timber.tag(TAG).d("Player.js size: ${playerJs.length} chars")

        val hashToUse = knownHash ?: extractPlayerHash(playerJs)
        Timber.tag(TAG).d("Using hash for config lookup: $hashToUse (knownHash=$knownHash)")
        if (hashToUse != null) {
            val config = getHardcodedConfig(hashToUse)
            if (config != null) {
                if (config.nJsExpression != null) {
                    Timber.tag(TAG).d("USING EXPRESSION-BASED N-FUNCTION: ${config.nJsExpression.take(60)}")
                } else {
                    Timber.tag(TAG).d("USING HARDCODED N-FUNCTION: ${config.nFuncName}[${config.nArrayIndex}]")
                    Timber.tag(TAG).d("N-function constant args: ${config.nConstantArgs}")
                }
                return NFunctionInfo(config.nFuncName, config.nArrayIndex, config.nConstantArgs, config.nJsExpression, isHardcoded = true)
            }
        }

        Timber.tag(TAG).w("No config for hash $hashToUse, trying legacy n-func patterns...")
        for ((index, pattern) in N_FUNCTION_PATTERNS.withIndex()) {
            Timber.tag(TAG).v("Trying n-func pattern $index: ${pattern.pattern.take(60)}...")
            val match = pattern.find(playerJs)
            if (match != null) {
                when (index) {
                    0 -> {
                        val name = match.groupValues[1]
                        val arrayIdx = match.groupValues[2].toIntOrNull()
                        Timber.tag(TAG).d("N-FUNCTION FOUND via pattern $index:")
                        Timber.tag(TAG).d("  name=$name, arrayIndex=$arrayIdx")
                        return NFunctionInfo(name, arrayIdx, isHardcoded = false)
                    }
                    1 -> {
                        val name = match.groupValues[2]
                        val arrayIdx = match.groupValues[3].toIntOrNull()
                        Timber.tag(TAG).d("N-FUNCTION FOUND via pattern $index:")
                        Timber.tag(TAG).d("  name=$name, arrayIndex=$arrayIdx")
                        return NFunctionInfo(name, arrayIdx, isHardcoded = false)
                    }
                    else -> {

                        if (pattern.toPattern().matcher("").groupCount() < 1) {
                            Timber.tag(TAG).d("N-pattern $index matched but has no capture groups; skipping")
                            continue
                        }
                        val name = match.groupValues[1]
                        Timber.tag(TAG).d("N-FUNCTION FOUND via pattern $index:")
                        Timber.tag(TAG).d("  name=$name")
                        return NFunctionInfo(name, null, isHardcoded = false)
                    }
                }
            }
        }

        Timber.tag(TAG).e("========== N-FUNCTION EXTRACTION FAILED ==========")
        Timber.tag(TAG).e("Could not find n-transform function name")
        return null
    }

    fun extractSignatureTimestamp(playerJs: String, knownHash: String? = null): Int? {
        Timber.tag(TAG).d("Extracting signatureTimestamp...")

        val anchored = ANCHORED_STS_PATTERN.find(playerJs)?.groupValues?.get(1)?.toIntOrNull()
        if (anchored != null) {
            Timber.tag(TAG).d("signatureTimestamp from player JS literal: $anchored")
            return anchored
        }

        val playerHash = knownHash ?: extractPlayerHash(playerJs)
        if (playerHash != null) {
            val config = getHardcodedConfig(playerHash)
            if (config != null) {
                Timber.tag(TAG).d("Using hardcoded signatureTimestamp: ${config.signatureTimestamp}")
                return config.signatureTimestamp
            }
        }

        val loose = LOOSE_STS_PATTERN.find(playerJs)?.groupValues?.get(1)?.toIntOrNull()
        if (loose != null) {
            Timber.tag(TAG).d("signatureTimestamp via loose sts pattern: $loose")
            return loose
        }

        Timber.tag(TAG).w("Could not extract signatureTimestamp")
        return null
    }

    fun analyzePlayerJs(playerJs: String, knownHash: String? = null): PlayerAnalysis {
        Timber.tag(TAG).d("=== PLAYER.JS CIPHER ANALYSIS ===")

        val playerHash = if (knownHash != null) {
            Timber.tag(TAG).d("Using known hash from PlayerJsFetcher: $knownHash")
            knownHash
        } else {
            extractPlayerHash(playerJs)
        }

        val hasQArray = hasQArrayObfuscation(playerJs)
        val sigInfo = extractSigFunctionInfo(playerJs, playerHash)
        val nFuncInfo = extractNFunctionInfo(playerJs, playerHash)
        val signatureTimestamp = extractSignatureTimestamp(playerJs, playerHash)

        Timber.tag(TAG).d("=== ANALYSIS SUMMARY ===")
        Timber.tag(TAG).d("Player Hash:        ${playerHash ?: "unknown"}")
        Timber.tag(TAG).d("Q-Array Obfuscated: $hasQArray")
        Timber.tag(TAG).d("Sig Function:       ${sigInfo?.name ?: "NOT FOUND"} (hardcoded=${sigInfo?.isHardcoded})")
        Timber.tag(TAG).d("Sig Constant Arg:   ${sigInfo?.constantArg}")
        Timber.tag(TAG).d("N-Function:         ${nFuncInfo?.name ?: "NOT FOUND"} (hardcoded=${nFuncInfo?.isHardcoded})")
        Timber.tag(TAG).d("N-Array Index:      ${nFuncInfo?.arrayIndex}")
        Timber.tag(TAG).d("Signature TS:       $signatureTimestamp")

        return PlayerAnalysis(
            playerHash = playerHash,
            hasQArrayObfuscation = hasQArray,
            sigInfo = sigInfo,
            nFuncInfo = nFuncInfo,
            signatureTimestamp = signatureTimestamp
        )
    }

    data class PlayerAnalysis(
        val playerHash: String?,
        val hasQArrayObfuscation: Boolean,
        val sigInfo: SigFunctionInfo?,
        val nFuncInfo: NFunctionInfo?,
        val signatureTimestamp: Int?
    )
}
