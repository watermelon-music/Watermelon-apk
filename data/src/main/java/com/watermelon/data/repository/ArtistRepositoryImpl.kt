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
                .filter { isMusicChannel(it.name, it.description) }
                .map { item ->
                    val rawThumbnail = item.thumbnails?.firstOrNull()?.url ?: ""
                    val thumbnailUrl = if (rawThumbnail.contains("googleusercontent.com")) rawThumbnail.replace(Regex("=s\\d+(-c)?"), "=s720$1") else rawThumbnail
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

    private fun isMusicChannel(name: String, description: String?): Boolean {
        val lower = (name + " " + (description ?: "")).lowercase()
        val blocked = listOf(
            "xxx",
            "porn",
            "sex",
            "nude",
            "naked",
            "adult",
            "erotic",
            "stripper",
            "blowjob",
            "anal",
            "milf",
            "onlyfans",
            "camgirl",
            "webcam",
            "sexual",
            "fetish",
            "twerk",
            "nipple",
            "orgasm",
            "masturbation",
            "cum",
            "dick",
            "pussy",
            "cock",
            "penis",
            "vagina",
            "slut",
            "whore",
            "bitch",
            "bikini",
            "lingerie",
            "uncensored",
            "leaked",
            "nsfw",
            "hentai",
            "jav",
            "porntube",
            "redtube",
            "xvideos",
            "pornhub",
            "brazzers",
            "only fans",
            "fans only",
            "sex tape",
            "sex video",
            "hot girl",
            "sexy girl",
            "sexy dance",
            "topless",
            "bottomless",
            "breaking news",
            "live news",
            "news update",
            "press conference",
            "live stream",
            "breaking",
            "headline",
            "ticker",
            "debate",
            "election",
            "politics",
            "political",
            "government",
            "minister",
            "prime minister",
            "president",
            "parliament",
            "senate",
            "congress",
            "republican",
            "democrat",
            "cricket",
            "ipl",
            "world cup",
            "match highlights",
            "india vs",
            "pakistan vs",
            "football",
            "soccer",
            "nba",
            "nfl",
            "basketball",
            "tennis",
            "wrestling",
            "ufc",
            "mma",
            "boxing",
            "goal",
            "score",
            "tournament",
            "fixture",
            "man of the match",
            "live commentary",
            "sports news",
            "full movie",
            "hd movie",
            "movie trailer",
            "full film",
            "cinema",
            "netflix",
            "movie review",
            "trailer reaction",
            "scene",
            "clip",
            "tv show",
            "web series",
            "episode",
            "season",
            "sitcom",
            "drama",
            "anime episode",
            "cartoon episode",
            "gameplay",
            "walkthrough",
            "playthrough",
            "gaming",
            "speedrun",
            "gta",
            "fortnite",
            "minecraft",
            "call of duty",
            "cod",
            "pubg",
            "free fire",
            "valorant",
            "among us",
            "roblox",
            "boss fight",
            "no commentary",
            "indie game",
            "mobile game",
            "android gameplay",
            "ios gameplay",
            "game music video",
            "gmv",
            "vlog",
            "unboxing",
            "tutorial",
            "how to",
            "diy",
            "makeup tutorial",
            "cooking",
            "recipe",
            "fitness",
            "workout",
            "gym",
            "yoga",
            "meditation",
            "prank",
            "challenge",
            "reaction",
            "review",
            "asmr",
            "mukbang",
            "q and a",
            "bhajan",
            "kirtan",
            "aarti",
            "puja",
            "prayer",
            "sermon",
            "preach",
            "quran",
            "bible",
            "gospel",
            "worship",
            "devotional",
            "hymn",
            "chanting",
            "mantra",
            "podcast",
            "audiobook",
            "full audiobook",
            "audio book",
            "talk show",
            "interview",
            "speech",
            "lecture",
            "ted talk",
            "motivational",
            "comedy special",
            "standup",
            "whatsapp status",
            "tiktok",
            "shorts compilation",
            "reels compilation",
            "compilation",
            "funny moments",
            "fail compilation",
            "try not to laugh",
            "meme"
        )
        return !blocked.any { lower.contains(it) }
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
