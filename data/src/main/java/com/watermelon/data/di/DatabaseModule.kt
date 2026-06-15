package com.watermelon.data.di

import android.content.Context
import androidx.room.Room
import com.watermelon.data.local.AppDatabase
import com.watermelon.data.local.dao.CachedSongDao
import com.watermelon.data.local.dao.DownloadDao
import com.watermelon.data.local.dao.PlaylistCacheDao
import com.watermelon.data.local.dao.RadioStationDao
import com.watermelon.data.local.dao.UserActionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "watermelon.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideCachedSongDao(db: AppDatabase): CachedSongDao = db.cachedSongDao()

    @Provides
    fun provideUserActionDao(db: AppDatabase): UserActionDao = db.userActionDao()

    @Provides
    fun provideRadioStationDao(db: AppDatabase): RadioStationDao = db.radioStationDao()

    @Provides
    fun provideDownloadDao(db: AppDatabase): DownloadDao = db.downloadDao()

    @Provides
    fun providePlaylistCacheDao(db: AppDatabase): PlaylistCacheDao = db.playlistCacheDao()
}
