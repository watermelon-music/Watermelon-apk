package com.watermelon.data.di

import com.watermelon.data.BuildConfig
import com.watermelon.data.remote.audius.AudiusApi
import com.watermelon.data.remote.jamendo.JamendoApi
import com.watermelon.data.remote.watermelon.WatermelonApi
import com.watermelon.data.remote.lyrics.LyricsApi
import com.watermelon.data.remote.podcastindex.PodcastIndexApi
import com.watermelon.data.remote.podcastindex.PodcastIndexAuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
    }

    @Provides
    @Singleton
    @PodcastIndexClient
    fun providePodcastIndexClient(base: OkHttpClient): OkHttpClient {
        return base.newBuilder()
            .addInterceptor(PodcastIndexAuthInterceptor())
            .build()
    }

    @Provides
    @Singleton
    fun providePodcastIndexApi(@PodcastIndexClient client: OkHttpClient): PodcastIndexApi {
        return Retrofit.Builder()
            .baseUrl("https://api.podcastindex.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PodcastIndexApi::class.java)
    }

    @Provides
    @Singleton
    fun provideLyricsApi(base: OkHttpClient): LyricsApi {
        return Retrofit.Builder()
            .baseUrl("https://api.lyrics.ovh/")
            .client(base)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LyricsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideJamendoApi(base: OkHttpClient): JamendoApi {
        return Retrofit.Builder()
            .baseUrl("https://api.jamendo.com/v3.0/")
            .client(base)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JamendoApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAudiusApi(base: OkHttpClient): AudiusApi {
        return Retrofit.Builder()
            .baseUrl("https://discoveryprovider.audius.co/v1/")
            .client(base)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AudiusApi::class.java)
    }

    @Provides
    @Singleton
    @WatermelonClient
    fun provideWatermelonClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideWatermelonApi(@WatermelonClient client: OkHttpClient): WatermelonApi {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.WATERMELON_API_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WatermelonApi::class.java)
    }

    @Provides
    @Singleton
    fun providePaymentApi(@WatermelonClient client: OkHttpClient): com.watermelon.data.remote.payments.PaymentApi {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.WATERMELON_API_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.watermelon.data.remote.payments.PaymentApi::class.java)
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PodcastIndexClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WatermelonClient
