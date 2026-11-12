/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.ctrlfxx.ctrlmusic.di

import android.content.Context
import com.ctrlfxx.ctrlmusic.db.DatabaseDao
import com.ctrlfxx.ctrlmusic.ui.screens.wrapped.WrappedAudioService
import com.ctrlfxx.ctrlmusic.ui.screens.wrapped.WrappedManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WrappedModule {
    @Provides
    @Singleton
    fun provideWrappedManager(
        databaseDao: DatabaseDao,
        @ApplicationContext context: Context,
    ): WrappedManager = WrappedManager(databaseDao, context)

    @Provides
    @Singleton
    fun provideWrappedAudioService(@ApplicationContext context: Context): WrappedAudioService = WrappedAudioService(context)
}
