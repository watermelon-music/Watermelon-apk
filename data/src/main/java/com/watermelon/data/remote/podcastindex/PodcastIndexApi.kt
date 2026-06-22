package com.watermelon.data.remote.podcastindex

import com.watermelon.data.remote.podcastindex.model.EpisodesResponse
import com.watermelon.data.remote.podcastindex.model.RecentEpisodesResponse
import com.watermelon.data.remote.podcastindex.model.SearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PodcastIndexApi {

    @GET("api/1.0/search/byterm")
    suspend fun searchByTerm(
        @Query("q") query: String,
        @Query("max") max: Int = 20
    ): SearchResponse

    @GET("api/1.0/episodes/byfeedid")
    suspend fun getEpisodesByFeedId(
        @Query("id") feedId: Long,
        @Query("max") max: Int = 20
    ): EpisodesResponse

    @GET("api/1.0/recent/episodes")
    suspend fun getRecentEpisodes(
        @Query("max") max: Int = 20,
        @Query("lang") lang: String? = null
    ): RecentEpisodesResponse
    @GET("api/1.0/episodes/byterm")
    suspend fun searchEpisodesByTerm(
        @Query("q") query: String,
        @Query("max") max: Int = 20
    ): EpisodesResponse
}
