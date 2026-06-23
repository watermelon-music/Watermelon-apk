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

    // Block non-music content aggressively (adult, news, gaming, sports, movies, etc.)
    private val blockedKeywords = listOf(
        "xxx", "porn", "sex", "nude", "naked", "adult", "erotic", "stripper",
        "blowjob", "anal", "milf", "onlyfans", "camgirl", "webcam", "sexual", "fetish",
        "twerk", "nipple", "orgasm", "masturbation", "cum", "dick", "pussy", "cock",
        "penis", "vagina", "slut", "whore", "bitch", "bikini", "lingerie",
        "uncensored", "leaked", "nsfw", "hentai", "jav", "porntube", "redtube", "xvideos",
        "pornhub", "brazzers", "only fans", "fans only", "sex tape", "sex video",
        "hot girl", "sexy girl", "sexy dance", "topless", "bottomless",
        "breaking news", "live news", "news update", "press conference", "live stream",
        "breaking", "headline", "ticker", "debate", "election", "politics", "political",
        "government", "minister", "prime minister", "president", "parliament",
        "senate", "congress", "republican", "democrat",
        "cricket", "ipl", "world cup", "match highlights", "india vs", "pakistan vs",
        "football", "soccer", "nba", "nfl", "basketball", "tennis", "wrestling", "ufc",
        "mma", "boxing", "goal", "score", "tournament", "fixture", "man of the match",
        "live commentary", "sports news",
        "full movie", "hd movie", "movie trailer", "full film", "cinema", "netflix",
        "movie review", "trailer reaction", "scene", "clip", "tv show", "web series",
        "episode", "season", "sitcom", "drama", "anime episode", "cartoon episode",
        "gameplay", "walkthrough", "playthrough", "gaming", "speedrun",
        "gta", "fortnite", "minecraft", "call of duty", "cod", "pubg", "free fire",
        "valorant", "among us", "roblox", "boss fight", "no commentary", "indie game",
        "mobile game", "android gameplay", "ios gameplay", "game music video", "gmv",
        "vlog", "unboxing", "tutorial", "how to", "diy", "makeup tutorial", "cooking",
        "recipe", "fitness", "workout", "gym", "yoga", "meditation", "prank", "challenge",
        "reaction", "review", "asmr", "mukbang", "q and a",
        "bhajan", "kirtan", "aarti", "puja", "prayer", "sermon", "preach", "quran",
        "bible", "gospel", "worship", "devotional", "hymn", "chanting", "mantra",
        "podcast", "audiobook", "full audiobook", "audio book", "talk show", "interview",
        "speech", "lecture", "ted talk", "motivational", "comedy special", "standup",
        "whatsapp status", "tiktok", "shorts compilation", "reels compilation",
        "compilation", "funny moments", "fail compilation", "try not to laugh", "meme"
    )
    private fun isMusicContent(title: String, durationSec: Long = 0): Boolean {
        val lower = title.lowercase()
        val hasBlocked = blockedKeywords.any { lower.contains(it.lowercase()) }
        if (hasBlocked) return false
        if (durationSec > 0) {
            if (durationSec < 45) return false
            if (durationSec > 600) return false
        }
        return true
    }

    override fun getTrendingMusic(): Flow<List<Song>> = flow {
        val fresh = withContext(Dispatchers.IO) {
            runCatching { fetchSearchFromYouTube("latest billboard trending music hits 2026") }.getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?: runCatching { fetchTrendingFromYouTube() }.getOrNull()
                ?: emptyList()
        }
        emit(fresh)
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
        val thumbUrl = "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
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
