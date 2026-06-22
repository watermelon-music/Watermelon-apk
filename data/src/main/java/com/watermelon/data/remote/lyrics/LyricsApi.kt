package com.watermelon.data.remote.lyrics

import retrofit2.http.GET
import retrofit2.http.Query

interface LyricsApi {

    @GET("api/search")
    suspend fun searchLyrics(
        @Query("q") query: String
    ): List<LrclibResult>
}

data class LrclibResult(
    val id: Int,
    val name: String?,
    val trackName: String?,
    val artistName: String?,
    val albumName: String?,
    val duration: Double?,
    val instrumental: Boolean?,
    val plainLyrics: String?,
    val syncedLyrics: String?
)
