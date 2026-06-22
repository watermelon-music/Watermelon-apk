package com.watermelon.data.remote.jamendo

import com.watermelon.data.remote.jamendo.model.JamendoTracksResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface JamendoApi {

    @GET("tracks")
    suspend fun getTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("order") order: String = "popularity_total",
        @Query("include") include: String = "musicinfo"
    ): JamendoTracksResponse

    @GET("tracks")
    suspend fun searchTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("search") query: String,
        @Query("include") include: String = "musicinfo"
    ): JamendoTracksResponse
}
