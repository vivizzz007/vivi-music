package com.music.vivi.utils

import android.content.Context
import com.music.lastfm.models.PendingFavorite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

object FavoriteCache {
    private const val FILE_NAME = "pending_favorites.json"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun save(context: Context, favorite: PendingFavorite) = withContext(Dispatchers.IO) {
        val existing = read(context).toMutableList()
        // If the same track is in the cache, replace it with the latest action
        existing.removeAll { it.artist == favorite.artist && it.track == favorite.track }
        existing.add(favorite)
        write(context, existing)
    }

    suspend fun saveAll(context: Context, favorites: List<PendingFavorite>) = withContext(Dispatchers.IO) {
        val existing = read(context).toMutableList()
        favorites.forEach { fav ->
            existing.removeAll { it.artist == fav.artist && it.track == fav.track }
            existing.add(fav)
        }
        write(context, existing)
    }

    suspend fun read(context: Context): List<PendingFavorite> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return@withContext emptyList()
            json.decodeFromString<List<PendingFavorite>>(file.readText())
        } catch (e: Exception) {
            Timber.e(e, "Failed to read pending favorites from cache")
            emptyList()
        }
    }

    suspend fun remove(context: Context, favoritesToRemove: List<PendingFavorite>) = withContext(Dispatchers.IO) {
        val existing = read(context).toMutableList()
        favoritesToRemove.forEach { fav ->
            existing.removeAll { it.artist == fav.artist && it.track == fav.track && it.isFavorite == fav.isFavorite }
        }
        write(context, existing)
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear pending favorites")
        }
    }

    private fun write(context: Context, favorites: List<PendingFavorite>) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(json.encodeToString(favorites))
        } catch (e: Exception) {
            Timber.e(e, "Failed to write pending favorites to cache")
        }
    }
}
