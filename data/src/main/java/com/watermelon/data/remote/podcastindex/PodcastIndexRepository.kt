package com.watermelon.data.remote.podcastindex

import com.watermelon.data.remote.podcastindex.model.PodcastEpisode
import com.watermelon.data.remote.podcastindex.model.PodcastFeed
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PodcastIndexRepository @Inject constructor(
    private val api: PodcastIndexApi
) {

    suspend fun searchPodcasts(query: String): List<PodcastFeed> {
        return runCatching {
            api.searchByTerm(query, max = 20).feeds
        }.getOrDefault(emptyList())
    }

    suspend fun getRecentEpisodes(max: Int = 20): List<PodcastEpisode> {
        return runCatching {
            api.getRecentEpisodes(max = max).items
        }.getOrDefault(emptyList())
    }

    suspend fun getEpisodesByFeedId(feedId: Long, max: Int = 20): List<PodcastEpisode> {
        return runCatching {
            api.getEpisodesByFeedId(feedId, max = max).items
        }.getOrDefault(emptyList())
    }

    suspend fun searchEpisodes(query: String): List<PodcastEpisode> {
        return runCatching {
            api.searchEpisodesByTerm(query, max = 20).items
        }.getOrDefault(emptyList())
    }
}
