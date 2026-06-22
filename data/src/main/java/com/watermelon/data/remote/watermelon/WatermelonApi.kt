package com.watermelon.data.remote.watermelon

import com.watermelon.data.remote.watermelon.model.WatermelonHealth
import com.watermelon.data.remote.watermelon.model.WatermelonSearchResult
import com.watermelon.data.remote.watermelon.model.WatermelonSong
import com.watermelon.data.remote.watermelon.model.WatermelonStream
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WatermelonApi {

    @GET("search")
    suspend fun search(@Query("q") query: String): List<WatermelonSearchResult>

    @GET("song/{id}")
    suspend fun getSong(@Path("id") videoId: String): WatermelonSong

    @GET("stream/{id}")
    suspend fun getStream(@Path("id") videoId: String): WatermelonStream

    @GET("download/{id}")
    suspend fun getDownload(@Path("id") videoId: String): WatermelonStream

    @GET("health")
    suspend fun health(): WatermelonHealth
}
