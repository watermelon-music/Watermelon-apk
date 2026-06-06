package com.watermelon.data.remote.lyrics

import retrofit2.http.GET
import retrofit2.http.Path

interface LyricsApi {

    @GET("v1/{artist}/{title}")
    suspend fun getLyrics(
        @Path("artist") artist: String,
        @Path("title") title: String
    ): LyricsResponse
}

data class LyricsResponse(
    val lyrics: String?,
    val error: String?
)
