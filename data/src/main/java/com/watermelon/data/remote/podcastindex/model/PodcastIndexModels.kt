package com.watermelon.data.remote.podcastindex.model

data class SearchResponse(
    val status: String,
    val feeds: List<PodcastFeed>,
    val count: Int,
    val query: String,
    val description: String
)

data class EpisodesResponse(
    val status: String,
    val items: List<PodcastEpisode>,
    val count: Int
)

data class RecentEpisodesResponse(
    val status: String,
    val items: List<PodcastEpisode>,
    val count: Int
)

data class PodcastFeed(
    val id: Long,
    val title: String,
    val url: String,
    val originalUrl: String,
    val link: String,
    val description: String,
    val author: String,
    val ownerName: String,
    val image: String,
    val artwork: String,
    val lastUpdateTime: Long,
    val lastCrawlTime: Long,
    val lastParseTime: Long,
    val lastGoodHttpStatusTime: Long,
    val lastHttpStatus: Int,
    val contentType: String,
    val itunesId: Long?,
    val generator: String,
    val language: String,
    val type: Int,
    val dead: Int,
    val chaptersUrl: String?,
    val episodeCount: Int
)

data class PodcastEpisode(
    val id: Long,
    val title: String,
    val link: String,
    val description: String,
    val guid: String,
    val datePublished: Long,
    val dateCrawled: Long,
    val enclosureUrl: String,
    val enclosureLength: Long,
    val enclosureType: String,
    val duration: Int,
    val explicit: Int,
    val episode: Int?,
    val season: Int?,
    val image: String,
    val feedImage: String,
    val feedId: Long,
    val feedUrl: String,
    val feedTitle: String,
    val feedLanguage: String,
    val chaptersUrl: String?
)
