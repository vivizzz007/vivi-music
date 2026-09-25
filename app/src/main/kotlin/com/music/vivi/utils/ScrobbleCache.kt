package com.music.vivi.utils

import android.content.Context
import com.music.lastfm.models.PendingScrobble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

object ScrobbleCache {
    private const val FILE_NAME = "pending_scrobbles.json"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun save(context: Context, scrobble: PendingScrobble) = withContext(Dispatchers.IO) {
        val existing = read(context).toMutableList()
        existing.add(scrobble)
        write(context, existing)
    }
    
    suspend fun saveAll(context: Context, scrobbles: List<PendingScrobble>) = withContext(Dispatchers.IO) {
        val existing = read(context).toMutableList()
        existing.addAll(scrobbles)
        write(context, existing)
    }

    suspend fun read(context: Context): List<PendingScrobble> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return@withContext emptyList()
            json.decodeFromString<List<PendingScrobble>>(file.readText())
        } catch (e: Exception) {
            Timber.e(e, "Failed to read pending scrobbles from cache")
            emptyList()
        }
    }

    suspend fun remove(context: Context, scrobblesToRemove: List<PendingScrobble>) = withContext(Dispatchers.IO) {
        val existing = read(context).toMutableList()
        existing.removeAll(scrobblesToRemove)
        write(context, existing)
    }
    
    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear pending scrobbles")
        }
    }

    private fun write(context: Context, scrobbles: List<PendingScrobble>) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(json.encodeToString(scrobbles))
        } catch (e: Exception) {
            Timber.e(e, "Failed to write pending scrobbles to cache")
        }
    }
}
