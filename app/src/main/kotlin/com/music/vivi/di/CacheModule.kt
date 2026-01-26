package com.music.vivi.di

import android.content.Context
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.music.vivi.constants.MaxSongCacheSizeKey
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CacheModule {

    @Singleton
    @Provides
    @PlayerCache
    fun providePlayerCache(@ApplicationContext context: Context, databaseProvider: DatabaseProvider): SimpleCache {
        val cacheSize = context.dataStore[MaxSongCacheSizeKey] ?: 1024
        return SimpleCache(
            context.filesDir.resolve("exoplayer"),
            when (cacheSize) {
                -1 -> NoOpCacheEvictor()
                else -> LeastRecentlyUsedCacheEvictor(cacheSize * 1024 * 1024L)
            },
            databaseProvider
        )
    }

    @Singleton
    @Provides
    @DownloadCache
    fun provideDownloadCache(@ApplicationContext context: Context, databaseProvider: DatabaseProvider): SimpleCache =
        SimpleCache(
            context.filesDir.resolve("download"),
            NoOpCacheEvictor(),
            databaseProvider
        )
}
