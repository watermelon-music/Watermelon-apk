package com.watermelon.data.di

import com.watermelon.data.repository.AuthRepositoryImpl
import com.watermelon.data.repository.AutoplayRepositoryImpl
import com.watermelon.data.repository.DownloadRepositoryImpl
import com.watermelon.data.repository.LyricsRepositoryImpl
import com.watermelon.data.repository.MusicCatalogRepositoryImpl
import com.watermelon.data.repository.MusicRepositoryImpl
import com.watermelon.data.repository.PlaylistRepositoryImpl
import com.watermelon.data.repository.RadioStationRepositoryImpl
import com.watermelon.data.repository.StreamingRepositoryImpl
import com.watermelon.data.repository.UserActionsRepositoryImpl
import com.watermelon.data.remote.youtube.NewPipeUrlExtractorImpl
import com.watermelon.domain.autoplay.AutoplayEngine
import com.watermelon.domain.autoplay.RecommendationScorer
import com.watermelon.domain.autoplay.TransitionTracker
import com.watermelon.domain.repository.LyricsRepository
import com.watermelon.domain.repository.AuthRepository
import com.watermelon.domain.repository.MusicCatalogRepository
import com.watermelon.domain.repository.MusicRepository
import com.watermelon.domain.repository.PlaylistRepository
import com.watermelon.domain.repository.RadioStationRepository
import com.watermelon.domain.repository.StreamingRepository
import com.watermelon.domain.repository.DownloadRepository
import com.watermelon.domain.repository.UrlExtractorRepository
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

    @Binds
    @Singleton
    abstract fun bindStreamingRepository(impl: StreamingRepositoryImpl): StreamingRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindUrlExtractorRepository(impl: NewPipeUrlExtractorImpl): UrlExtractorRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindLyricsRepository(impl: LyricsRepositoryImpl): LyricsRepository

    @Binds
    @Singleton
    abstract fun bindRadioStationRepository(impl: RadioStationRepositoryImpl): RadioStationRepository

    @Binds
    @Singleton
    abstract fun bindAutoplayEngine(impl: AutoplayRepositoryImpl): AutoplayEngine

    @Binds
    @Singleton
    abstract fun bindTransitionTracker(impl: AutoplayRepositoryImpl): TransitionTracker

    @Binds
    @Singleton
    abstract fun bindRecommendationScorer(impl: AutoplayRepositoryImpl): RecommendationScorer
}
