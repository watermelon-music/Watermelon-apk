package com.watermelon.data.repository

import com.watermelon.data.remote.youtube.NewPipeInitializer
import com.watermelon.domain.model.Artist
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.ArtistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistRepositoryImpl @Inject constructor(
    private val initializer: NewPipeInitializer
) : ArtistRepository {

    private val youtube = ServiceList.YouTube

    override suspend fun searchArtists(query: String): Result<List<Artist>> = withContext(Dispatchers.IO) {
        runCatching {
            initializer.ensureInitialized()
            val queryHandler = youtube.getSearchQHFactory().fromQuery(query, listOf("channels"), "")
            val extractor = youtube.getSearchExtractor(queryHandler)
            extractor.fetchPage()
            extractor.initialPage.items
                .filterIsInstance<org.schabi.newpipe.extractor.channel.ChannelInfoItem>()
                .map { item ->
                    val thumbnailUrl = item.thumbnails?.firstOrNull()?.url ?: ""
                    Artist(
                        id = item.url,
                        name = item.name,
                        bio = item.description,
                        imageUrl = thumbnailUrl,
                        subscriberCount = item.subscriberCount,
                        verified = item.isVerified ?: false
                    )
                }
        }.onFailure { e -> Timber.e(e, "searchArtists failed for query=$query") }
    }

    override suspend fun getArtistDetails(channelUrl: String): Result<Artist> = withContext(Dispatchers.IO) {
        runCatching {
            initializer.ensureInitialized()
            val info = ChannelInfo.getInfo(youtube, channelUrl)
            Artist(
                id = channelUrl,
                name = info.name,
                bio = info.description,
                imageUrl = info.avatars.firstOrNull()?.url,
                subscriberCount = info.subscriberCount,
                songCount = 0,
                verified = info.isVerified ?: false,
                bannerUrl = info.banners.firstOrNull()?.url
            )
        }.onFailure { e -> Timber.e(e, "getArtistDetails failed for channelUrl=$channelUrl") }
    }

    override suspend fun getArtistSongs(channelUrl: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            initializer.ensureInitialized()
            val artistInfo = ChannelInfo.getInfo(youtube, channelUrl)
            val artistName = artistInfo.name
            val queryHandler = youtube.getSearchQHFactory().fromQuery("$artistName songs", listOf("videos"), "")
            val extractor = youtube.getSearchExtractor(queryHandler)
            extractor.fetchPage()
            extractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    val fullUrl = if (item.url.startsWith("http")) item.url else "https://youtube.com${item.url}"
                    val videoId = when {
                        fullUrl.contains("v=") -> fullUrl.substringAfter("v=").substringBefore("&")
                        fullUrl.contains("youtu.be/") -> fullUrl.substringAfter("youtu.be/")
                        else -> item.url.hashCode().toString()
                    }
                    Song(
                        id = videoId,
                        title = item.name,
                        artistId = channelUrl,
                        artistName = artistName,
                        albumId = null,
                        albumName = null,
                        durationMs = if (item.duration > 0) item.duration * 1000L else 0L,
                        coverUrl = "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg",
                        audioUrl = fullUrl,
                        genre = null,
                        releaseDate = null
                    )
                }
        }.onFailure { e -> Timber.e(e, "getArtistSongs failed for channelUrl=$channelUrl") }
    }
}
