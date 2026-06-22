package com.watermelon.data.di

import com.watermelon.data.remote.radio.RadioBrowserApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RadioModule {

    private const val BASE_URL = "https://all.api.radio-browser.info/"

    @Provides
    @Singleton
    fun provideRadioBrowserApi(okHttpClient: OkHttpClient): RadioBrowserApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RadioBrowserApi::class.java)
    }
}
