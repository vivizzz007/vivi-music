/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.my.kizzy.rpc

import java.util.concurrent.ConcurrentHashMap

internal object ArtworkCache {
    private val cache = ConcurrentHashMap<String, String>()

    suspend fun getOrFetch(key: String, fetch: suspend () -> String?): String? {
        return cache[key] ?: fetch()?.also { cache[key] = it }
    }
}
