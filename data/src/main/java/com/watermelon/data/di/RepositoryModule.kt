package com.watermelon.data.di

import com.watermelon.data.repository.AuthRepositoryImpl
import com.watermelon.data.repository.MusicCatalogRepositoryImpl
import com.watermelon.data.repository.MusicRepositoryImpl
import com.watermelon.data.repository.UserActionsRepositoryImpl
import com.watermelon.domain.repository.AuthRepository
import com.watermelon.domain.repository.MusicCatalogRepository
import com.watermelon.domain.repository.MusicRepository
import com.watermelon.domain.repository.UserActionsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // Legacy binding — kept for backward compatibility until full migration
    @Binds
    @Singleton
    abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindMusicCatalogRepository(impl: MusicCatalogRepositoryImpl): MusicCatalogRepository

    @Binds
    @Singleton
    abstract fun bindUserActionsRepository(impl: UserActionsRepositoryImpl): UserActionsRepository
}
