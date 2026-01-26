package com.music.vivi.di

import android.content.Context
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [CacheModule::class]
)
object TestCacheModule {

    @Singleton
    @Provides
    @PlayerCache
    fun providePlayerCache(@ApplicationContext context: Context, databaseProvider: DatabaseProvider): SimpleCache {
         val uniqueDir = File(context.cacheDir, "test_exoplayer_${UUID.randomUUID()}")
         return SimpleCache(uniqueDir, NoOpCacheEvictor(), databaseProvider)
    }

    @Singleton
    @Provides
    @DownloadCache
    fun provideDownloadCache(@ApplicationContext context: Context, databaseProvider: DatabaseProvider): SimpleCache {
         val uniqueDir = File(context.cacheDir, "test_download_${UUID.randomUUID()}")
         return SimpleCache(uniqueDir, NoOpCacheEvictor(), databaseProvider)
    }
}
