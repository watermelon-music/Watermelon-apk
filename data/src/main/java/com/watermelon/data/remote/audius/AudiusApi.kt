package com.watermelon.data.remote.audius

import com.watermelon.data.remote.audius.model.AudiusTracksResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface AudiusApi {

    @GET("tracks/trending")
    suspend fun getTrendingTracks(): AudiusTracksResponse

    @GET("tracks/search")
    suspend fun searchTracks(@Query("query") query: String): AudiusTracksResponse
}
