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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val initializer: NewPipeInitializer
) : MusicCatalogRepository {

    private val youtube by lazy { org.schabi.newpipe.extractor.ServiceList.YouTube }
    private val cacheTtlMs = 60 * 60 * 1000L // 1 hour
    private val extractorMutex = Mutex()

    // Block non-music content aggressively (news, gaming, vlogs, tutorials, etc.)
    private val gamingVlogKeywords = listOf(
        "gameplay", "let's play", "playthrough", "walkthrough", "gaming",
        "vlog", "vlogging", "day in my life", "reaction", "reacts to",
        "unboxing", "review", "tutorial", "how to", "guide", "tips and tricks",
        "minecraft", "fortnite", "pubg", "call of duty", "gta", "among us",
        "roblox", "valorant", "apex legends", "cs:go", "counter strike",
        "live stream", "streaming", "podcast", "podcasts", "talk show",
        "interview", "interviews", "news", "breaking", "politics",
        "weather", "sports highlight", "football highlight", "cricket highlight",
        "movie review", "film review", "trailer", "teaser",
        "speedrun", "esports", "e-sports", "mmorpg", "rpg", "mmo",
        "boss fight", "no commentary", "indie game", "mobile game",
        "android gameplay", "ios gameplay", "game music video", "gmv"
    )
    private fun isMusicContent(title: String, durationSec: Long = 0): Boolean {
        val lower = title.lowercase()
        // Zero-tolerance block for gaming/vlog/news/irrelevant content
        val hasBlocked = gamingVlogKeywords.any { lower.contains(it.lowercase()) }
        if (hasBlocked) return false
        // Duration heuristic: songs are typically 45 seconds to 12 minutes
        if (durationSec > 0) {
            if (durationSec < 45) return false      // Too short (shorts/reels)
            if (durationSec > 720) return false     // Too long (podcasts, streams)
        }
        return true
    }

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

    override fun getSongsByGenre(genre: String): Flow<List<Song>> {
        val query = when (genre.lowercase()) {
            "bollywood" -> "bollywood trending songs"
            "hollywood" -> "english trending songs"
            "pop" -> "pop music trending"
            "rock" -> "rock music"
            "jazz" -> "jazz music"
            "classical" -> "classical music"
            "hiphop" -> "hip hop music"
            "electronic" -> "electronic music"
            else -> "$genre music"
        }
        return search(query)
    }

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
        extractorMutex.withLock {
            initializer.ensureInitialized()
            val kioskList = youtube.getKioskList()
            // Try YouTube Music Charts first (music-only), fallback to generic Trending
            val extractor = try {
                kioskList.getExtractorById("trending_music", null)
            } catch (_: Exception) {
                kioskList.getExtractorById("Trending", null)
            }
            extractor.fetchPage()
            extractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .filter { isMusicContent(it.name, it.duration) }
                .take(20)
                .map { it.toSong() }
        }
    }

    private suspend fun fetchSearchFromYouTube(query: String): List<Song> = withContext(Dispatchers.IO) {
        extractorMutex.withLock {
            initializer.ensureInitialized()
            // Use YouTube Music search filter for higher-quality music results
            val queryHandler = youtube.getSearchQHFactory().fromQuery(
                query, listOf("music_songs"), ""
            )
            val extractor = youtube.getSearchExtractor(queryHandler)
            extractor.fetchPage()
            extractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .filter { isMusicContent(it.name, it.duration) }
                .take(20)
                .map { it.toSong() }
        }
    }

    private fun StreamInfoItem.toSong(): Song {
        val fullUrl = if (url.startsWith("http")) url else "https://www.youtube.com$url"
        val videoId = extractVideoId(fullUrl)
        val thumbUrl = thumbnails.orEmpty().maxByOrNull { it.height }?.url?.takeIf { it.isNotBlank() }
            ?: "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
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
