package com.watermelon.data.repository

import com.watermelon.data.local.dao.CachedSongDao
import com.watermelon.data.local.entity.toCachedEntity
import com.watermelon.data.local.entity.toSong
import com.watermelon.data.remote.audius.AudiusRepository
import com.watermelon.data.remote.audius.model.AudiusTrack
import com.watermelon.data.remote.jamendo.JamendoRepository
import com.watermelon.data.remote.jamendo.model.JamendoTrack
import com.watermelon.data.remote.podcastindex.PodcastIndexRepository
import com.watermelon.data.remote.podcastindex.model.PodcastEpisode
import com.watermelon.data.remote.watermelon.WatermelonRepository
import com.watermelon.data.remote.youtube.NewPipeInitializer
import com.watermelon.domain.model.Playlist
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.MusicCatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicCatalogRepositoryImpl @Inject constructor(
    private val watermelonRepository: WatermelonRepository,
    private val jamendoRepository: JamendoRepository,
    private val audiusRepository: AudiusRepository,
    private val podcastIndexRepository: PodcastIndexRepository,
    private val cachedSongDao: CachedSongDao,
    initializer: NewPipeInitializer
) : MusicCatalogRepository {

    private val youtube by lazy { org.schabi.newpipe.extractor.ServiceList.YouTube }
    private val cacheTtlMs = 60 * 60 * 1000L // 1 hour

    // Filter out non-music content (news, politics, etc.)
    private val newsKeywords = listOf(
        "breaking news", "news update", "live news", "president", "election",
        "bbc news", "abc news", "cnn ", "fox news", "press conference",
        "government", "minister", "senate", "parliament", "inflation", "budget",
        "weather alert", "emergency", "war update", "official statement",
        "chief minister", "prime minister", "donald trump", "joe biden"
    )
    private val musicKeywords = listOf(
        "song", " music", "album", "track", "audio", "official video",
        "lyrics", "live performance", "concert", "remix", "cover", " ft ", " feat ",
        "music video", "dj ", "mix", "playlist", "single", " ep ", " ost ",
        "soundtrack", "theme song"
    )
    private fun isMusicContent(title: String): Boolean {
        val lower = title.lowercase()
        val hasNews = newsKeywords.any { lower.contains(it.lowercase()) }
        if (!hasNews) return true // No news signals = keep it
        val hasMusic = musicKeywords.any { lower.contains(it.lowercase()) }
        return hasMusic // If it has news words BUT also music words, it's probably a song
    }

    private val mockPlaylists = listOf(
        Playlist("1","Chill Vibes","Relax and unwind","https://picsum.photos/seed/p1/300/300","system"),
        Playlist("2","Workout Energy","Pump it up","https://picsum.photos/seed/p2/300/300","system"),
        Playlist("3","Late Night Drive","Midnight cruisers","https://picsum.photos/seed/p3/300/300","system"),
        Playlist("4","Top Hits 2024","Best of the year","https://picsum.photos/seed/p4/300/300","system")
    )

    override fun getTrendingMusic(): Flow<List<Song>> = flow {
        val now = System.currentTimeMillis()
        val cached = cachedSongDao.getTrendingSongs().firstOrNull() ?: emptyList()

        if (cached.isNotEmpty()) {
            val freshest = cached.maxOf { it.cachedAt }
            if (now - freshest < cacheTtlMs) {
                emit(cached.map { it.toSong() })
            }
        }

        val fresh = withContext(Dispatchers.IO) {
            runCatching { fetchTrendingFromYouTube() }.getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?: runCatching { watermelonRepository.search("top hits 2024") }.getOrNull()
                    ?.takeIf { it.isNotEmpty() }
                ?: runCatching { jamendoRepository.getTrendingTracks(limit = 20) }.getOrNull()
                    ?.takeIf { it.isNotEmpty() }
                    ?.map { it.toSong() }
                ?: runCatching { audiusRepository.getTrendingTracks() }.getOrNull()
                    ?.takeIf { it.isNotEmpty() }
                    ?.map { it.toSong() }
            // Podcasts removed from Trending — they are talk, not music
        }

        if (fresh != null) {
            cachedSongDao.clearTrending()
            cachedSongDao.insertAll(fresh.map { it.toCachedEntity("trending") })
            emit(fresh)
        } else {
            emit(emptyList())
        }
    }

    override fun getRecommendedPlaylists(): Flow<List<Playlist>> = flowOf(mockPlaylists)

    override fun search(query: String): Flow<List<Song>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }

        val cached = cachedSongDao.getSearchResults(query).firstOrNull() ?: emptyList()
        if (cached.isNotEmpty()) {
            emit(cached.map { it.toSong() })
        }

        val fresh = withContext(Dispatchers.IO) {
            runCatching { fetchSearchFromYouTube(query) }.getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?: runCatching { watermelonRepository.search(query) }.getOrNull()
                    ?.takeIf { it.isNotEmpty() }
                ?: runCatching { jamendoRepository.searchTracks(query, limit = 20) }.getOrNull()
                    ?.takeIf { it.isNotEmpty() }
                    ?.map { it.toSong() }
                ?: runCatching { audiusRepository.searchTracks(query) }.getOrNull()
                    ?.takeIf { it.isNotEmpty() }
                    ?.map { it.toSong() }
                ?: runCatching { podcastIndexRepository.searchEpisodes(query) }.getOrNull()
                    ?.takeIf { it.isNotEmpty() }
                    ?.map { it.toSong() }
        }

        if (fresh != null) {
            cachedSongDao.clearSearchResults(query)
            cachedSongDao.insertAll(fresh.map { it.toCachedEntity("search", query) })
            emit(fresh)
        } else {
            emit(emptyList())
        }
    }

    private fun JamendoTrack.toSong(): Song {
        return Song(
            id = id,
            title = name,
            artistId = artist_id,
            artistName = artist_name,
            albumId = album_id,
            albumName = album_name,
            durationMs = duration * 1000L,
            coverUrl = image.takeIf { it.isNotBlank() } ?: album_image,
            audioUrl = audio,
            genre = musicinfo?.tags?.genres?.firstOrNull() ?: "",
            releaseDate = releasedate
        )
    }

    private fun AudiusTrack.toSong(): Song {
        val artworkUrl = artwork?._480x480?.takeIf { it.isNotBlank() }
            ?: artwork?._150x150?.takeIf { it.isNotBlank() }
            ?: ""
        return Song(
            id = id,
            title = title,
            artistId = user.id,
            artistName = user.name,
            albumId = null,
            albumName = null,
            durationMs = duration * 1000L,
            coverUrl = artworkUrl,
            audioUrl = "https://discoveryprovider.audius.co/v1/tracks/$id/stream",
            genre = "",
            releaseDate = ""
        )
    }

    private fun PodcastEpisode.toSong(): Song {
        val thumbUrl = image.takeIf { it.isNotBlank() }
            ?: feedImage.takeIf { it.isNotBlank() }
            ?: "https://picsum.photos/seed/${id}/300/300"
        return Song(
            id = id.toString(),
            title = title,
            artistId = feedId.toString(),
            artistName = feedTitle,
            albumId = null,
            albumName = null,
            durationMs = if (duration > 0) duration * 1000L else 0L,
            coverUrl = thumbUrl,
            audioUrl = enclosureUrl,
            genre = feedLanguage,
            releaseDate = ""
        )
    }

    private suspend fun fetchTrendingFromYouTube(): List<Song> = withContext(Dispatchers.IO) {
        val kioskList = youtube.getKioskList()
        val extractor = kioskList.getDefaultKioskExtractor()
        extractor.fetchPage()
        extractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .filter { isMusicContent(it.name) }
            .take(20)
            .map { it.toSong() }
    }

    private suspend fun fetchSearchFromYouTube(query: String): List<Song> = withContext(Dispatchers.IO) {
        val extractor = youtube.getSearchExtractor(query)
        extractor.fetchPage()
        extractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .filter { isMusicContent(it.name) }
            .take(20)
            .map { it.toSong() }
    }

    private fun StreamInfoItem.toSong(): Song {
        val fullUrl = if (url.startsWith("http")) url else "https://www.youtube.com$url"
        val videoId = extractVideoId(fullUrl)
        val thumbUrl = thumbnails.orEmpty().firstOrNull()?.url?.takeIf { it.isNotBlank() }
            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        return Song(
            id = videoId,
            title = name,
            artistId = uploaderName?.hashCode()?.toString() ?: videoId,
            artistName = uploaderName ?: "Unknown Artist",
            albumId = null,
            albumName = null,
            durationMs = if (duration > 0) (duration * 1000L) else 0L,
            coverUrl = thumbUrl,
            audioUrl = fullUrl,
            genre = "",
            releaseDate = ""
        )
    }

    private fun extractVideoId(url: String): String {
        return when {
            url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
            url.contains("youtu.be/") -> url.substringAfter("youtu.be/")
            else -> url.hashCode().toString()
        }
    }
}
