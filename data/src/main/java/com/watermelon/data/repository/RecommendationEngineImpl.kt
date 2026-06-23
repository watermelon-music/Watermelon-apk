package com.watermelon.data.repository

import com.watermelon.data.local.dao.PlayHistoryDao
import com.watermelon.data.local.dao.UserActionDao
import com.watermelon.domain.autoplay.RecommendationEngine
import com.watermelon.domain.autoplay.ScoredSong
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.MusicCatalogRepository
import com.watermelon.domain.repository.UserActionsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.random.Random

/**
 * Spotify-style recommendation engine.
 *
 * Scoring sources (configurable weights):
 *  - 40% related artist (same/similar artist search)
 *  - 30% same genre
 *  - 20% user favorites / listening history
 *  - 10% random discovery (trending)
 *
 * Penalties:
 *  - Title similarity > 70% -> heavy score reduction
 *  - Recent plays -> decay penalty
 *  - Skipped songs -> skip penalty
 *
 * Diversity constraints:
 *  - No more than 2 songs from the same artist in a batch
 *  - No more than 2 songs from the same album in a batch
 */
@Singleton
class RecommendationEngineImpl @Inject constructor(
    private val catalogRepository: MusicCatalogRepository,
    private val userActionsRepository: UserActionsRepository,
    private val playHistoryDao: PlayHistoryDao,
    private val userActionDao: UserActionDao
) : RecommendationEngine {

    data class Weights(
        val relatedArtist: Double = 40.0,
        val sameGenre: Double = 30.0,
        val userHistory: Double = 20.0,
        val randomDiscovery: Double = 10.0,
        val titleSimilarityPenalty: Double = 80.0,
        val skipPenalty: Double = 50.0,
        val recencyDecayBase: Double = 25.0
    )

    private val weights = Weights()
    private val random = Random(System.currentTimeMillis())
    private var lastCurrentSongId: String? = null

    private fun isValidMusic(title: String, durationSec: Long = 0): Boolean {
        val lower = title.lowercase()
        val blocked = listOf(
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
        if (blocked.any { lower.contains(it.lowercase()) }) return false
        if (durationSec > 0 && (durationSec < 45 || durationSec > 600)) return false
        return true
    }

    override fun invalidateCache() {
        lastCurrentSongId = null
    }

    override suspend fun generateQueue(
        currentSong: Song,
        excludeIds: Set<String>,
        count: Int
    ): List<Song> {
        // Invalidate if current song changed
        if (lastCurrentSongId != null && lastCurrentSongId != currentSong.id) {
            invalidateCache()
        }
        lastCurrentSongId = currentSong.id

        val excludeSet = excludeIds + currentSong.id

        // 1. Fetch candidates from multiple sources in parallel
        val artistCandidates = fetchRelatedArtistCandidates(currentSong, excludeSet)
        val genreCandidates = fetchSameGenreCandidates(currentSong, excludeSet)
        val historyCandidates = fetchUserHistoryCandidates(excludeSet)
        val randomCandidates = fetchRandomDiscoveryCandidates(excludeSet)
        val hashtagCandidates = fetchHashtagCandidates(currentSong, excludeSet)

        // Combine all candidates, dedup by ID, strict content filter
        val allCandidates = mutableMapOf<String, Song>()
        artistCandidates.filter { isValidMusic(it.title, it.durationMs / 1000) }.forEach { allCandidates[it.id] = it }
        genreCandidates.filter { isValidMusic(it.title, it.durationMs / 1000) }.forEach { allCandidates[it.id] = it }
        historyCandidates.filter { isValidMusic(it.title, it.durationMs / 1000) }.forEach { allCandidates[it.id] = it }
        randomCandidates.filter { isValidMusic(it.title, it.durationMs / 1000) }.forEach { allCandidates[it.id] = it }
        hashtagCandidates.filter { isValidMusic(it.title, it.durationMs / 1000) }.forEach { allCandidates[it.id] = it }

        if (allCandidates.isEmpty()) return emptyList()

        // 2. Load user signals
        val favorites = userActionDao.getFavorites().firstOrNull() ?: emptyList()
        val skipEntities = userActionDao.getSkips().firstOrNull() ?: emptyList()
        val skips = skipEntities.map { it.songId }.toSet()
        val recentPlays = playHistoryDao.getRecent().firstOrNull() ?: emptyList()
        val recentIndexMap = recentPlays.mapIndexed { idx, entity -> entity.songId to idx }.toMap()
        val favoriteArtistNames = favorites.mapNotNull { it.songArtist?.lowercase() }.toSet()

        // 3. Score every candidate
        val scored = allCandidates.values.mapNotNull { candidate ->
            var score = 0.0

            // Strict filter: Do not play the exact same song or film song with just different text
            val titleSim = titleSimilarity(currentSong.title.lowercase(), candidate.title.lowercase())
            if (titleSim > 0.25) return@mapNotNull null // Drop highly similar titles outright

            // Strict containment: if candidate contains all significant words of current title
            val currentWords = currentSong.title.lowercase().split("\\s+".toRegex()).filter { it.length > 2 }
            if (currentWords.size >= 2 && currentWords.all { candidate.title.lowercase().contains(it) }) return@mapNotNull null

            // === Source scores (the 40/30/20/10 split) ===
            if (candidate.id in artistCandidates.map { it.id }) {
                score += weights.relatedArtist
            }
            if (candidate.id in genreCandidates.map { it.id }) {
                score += weights.sameGenre
            }
            if (candidate.id in historyCandidates.map { it.id }) {
                score += weights.userHistory
            }
            if (candidate.id in randomCandidates.map { it.id }) {
                score += weights.randomDiscovery
            }
            if (candidate.id in hashtagCandidates.map { it.id }) {
                score += 10.0 // Reduced boost for hashtag matches
            }

            // === Fine-grained adjustments ===
            // Same artist boost (limited by diversity later)
            if (candidate.artistName.lowercase() == currentSong.artistName.lowercase()) {
                score += 15.0
            }

            // Favorite artist boost
            if (candidate.artistName.lowercase() in favoriteArtistNames) {
                score += 10.0
            }

            // Skip penalty
            if (candidate.id in skips) {
                score -= weights.skipPenalty
            }

            // Recency penalty
            val recentIndex = recentIndexMap[candidate.id] ?: -1
            if (recentIndex >= 0) {
                score -= (recentPlays.size - recentIndex).toDouble() / recentPlays.size * weights.recencyDecayBase
            }

            ScoredSong(candidate, score)
        }

        // 4. Apply diversity filter
        val diversified = applyDiversity(
            scored.sortedByDescending { it.score },
            maxPerArtist = 2,
            maxPerAlbum = 2,
            targetCount = count
        )

        // 5. Shuffle slightly within score tiers to avoid predictability
        return shuffleWithinTiers(diversified.map { it.song })
    }

    // ---------------------------------------------------------------------------
    // Candidate fetchers
    // ---------------------------------------------------------------------------

    private suspend fun fetchHashtagCandidates(
        currentSong: Song,
        excludeIds: Set<String>
    ): List<Song> {
        val results = mutableListOf<Song>()
        val words = currentSong.title.split(Regex("\\s+"))
            .map { it.replace(Regex("[^a-zA-Z0-9]"), "") }
            .filter { it.length > 3 && !it.equals("song", true) && !it.equals("video", true) && !it.equals("lyric", true) }
            .shuffled(random)
            .take(2)
        
        if (words.isNotEmpty()) {
            val query = words.joinToString(" ")
            runCatching {
                catalogRepository.search(query).firstOrNull()
                    ?.filter { it.id !in excludeIds }
                    ?.filter { titleSimilarity(currentSong.title.lowercase(), it.title.lowercase()) < 0.20 }
                    ?.let { results.addAll(it.take(10)) }
            }
        }
        return results.distinctBy { it.id }
    }

    private suspend fun fetchRelatedArtistCandidates(
        currentSong: Song,
        excludeIds: Set<String>
    ): List<Song> {
        val results = mutableListOf<Song>()

        // Search for the artist name — this yields the artist's other songs
        if (currentSong.artistName.isNotBlank()) {
            runCatching {
                catalogRepository.search(currentSong.artistName).firstOrNull()
                    ?.filter { it.id !in excludeIds }
                    ?.filter { isValidMusic(it.title, it.durationMs / 1000) }
                    ?.filter { titleSimilarity(currentSong.title.lowercase(), it.title.lowercase()) < 0.20 }
                    ?.let { results.addAll(it.take(15)) }
            }
        }

        // If we don't have enough, try splitting artist by common separators
        val artists = currentSong.artistName.split(",", "&", "feat.", "ft.", " x ", " X ")
        for (artist in artists) {
            val trimmed = artist.trim()
            if (trimmed.isBlank() || trimmed.equals(currentSong.artistName, ignoreCase = true)) continue
            runCatching {
                catalogRepository.search(trimmed).firstOrNull()
                    ?.filter { it.id !in excludeIds && it.id !in results.map { r -> r.id } }
                    ?.filter { isValidMusic(it.title, it.durationMs / 1000) }
                    ?.filter { titleSimilarity(currentSong.title.lowercase(), it.title.lowercase()) < 0.20 }
                    ?.let { results.addAll(it.take(8)) }
            }
        }

        return results.distinctBy { it.id }
    }

    private suspend fun fetchSameGenreCandidates(
        currentSong: Song,
        excludeIds: Set<String>
    ): List<Song> {
        val genre = currentSong.genre?.takeIf { it.isNotBlank() } ?: return emptyList()
        return runCatching {
            catalogRepository.getSongsByGenre(genre).firstOrNull()
                ?.filter { it.id !in excludeIds }
                ?.take(15)
        }.getOrNull() ?: emptyList()
    }

    private suspend fun fetchUserHistoryCandidates(excludeIds: Set<String>): List<Song> {
        val results = mutableListOf<Song>()

        // Favorites
        runCatching {
            userActionsRepository.getFavorites().firstOrNull()
                ?.filter { it.id !in excludeIds }
                ?.shuffled()
                ?.take(10)
                ?.let { results.addAll(it) }
        }

        // Recently played artists → find more from those artists
        val recentArtists = runCatching {
            playHistoryDao.getRecent().firstOrNull()
                ?.mapNotNull { it.songArtist }
                ?.distinct()
                ?.take(5)
        }.getOrNull() ?: emptyList()

        for (artist in recentArtists) {
            runCatching {
                catalogRepository.search(artist).firstOrNull()
                    ?.filter { it.id !in excludeIds && it.id !in results.map { r -> r.id } }
                    ?.take(5)
                    ?.let { results.addAll(it) }
            }
        }

        return results.distinctBy { it.id }
    }

    private suspend fun fetchRandomDiscoveryCandidates(excludeIds: Set<String>): List<Song> {
        return runCatching {
            catalogRepository.getTrendingMusic().firstOrNull()
                ?.filter { it.id !in excludeIds }
                ?.shuffled()
                ?.take(10)
        }.getOrNull() ?: emptyList()
    }

    // ---------------------------------------------------------------------------
    // Diversity filter
    // ---------------------------------------------------------------------------

    internal fun applyDiversity(
        scored: List<ScoredSong>,
        maxPerArtist: Int,
        maxPerAlbum: Int,
        targetCount: Int
    ): List<ScoredSong> {
        val artistCounts = mutableMapOf<String, Int>()
        val albumCounts = mutableMapOf<String?, Int>()
        val selected = mutableListOf<ScoredSong>()

        for (item in scored) {
            val artist = item.song.artistName.lowercase()
            val album = item.song.albumName?.lowercase()

            if ((artistCounts[artist] ?: 0) >= maxPerArtist) continue
            if (album != null && (albumCounts[album] ?: 0) >= maxPerAlbum) continue

            selected.add(item)
            artistCounts[artist] = (artistCounts[artist] ?: 0) + 1
            if (album != null) albumCounts[album] = (albumCounts[album] ?: 0) + 1

            if (selected.size >= targetCount) break
        }
        return selected
    }

    // ---------------------------------------------------------------------------
    // Shuffle within score tiers so the queue isn't 100% deterministic
    // ---------------------------------------------------------------------------

    private fun shuffleWithinTiers(songs: List<Song>): List<Song> {
        if (songs.size <= 3) return songs
        // Group into tiers of 3 and shuffle each tier
        return songs.chunked(3).flatMap { it.shuffled(random) }
    }

    // ---------------------------------------------------------------------------
    // Title similarity using normalized Levenshtein distance
    // ---------------------------------------------------------------------------

    internal fun titleSimilarity(a: String, b: String): Double {
        val na = normalizeTitle(a)
        val nb = normalizeTitle(b)
        if (na == nb) return 1.0
        if (na.isEmpty() || nb.isEmpty()) return 0.0
        val dist = levenshteinDistance(na, nb)
        val maxLen = max(na.length, nb.length)
        return 1.0 - (dist.toDouble() / maxLen)
    }

    private fun normalizeTitle(title: String): String {
        return title
            .replace(Regex("\\(.*?\\)|\\[.*?\\]"), "") // remove brackets
            .replace(Regex("\\b(remix|edit|extended|radio|mix|cover|live|acoustic|slowed|reverb|phonk| Instrumental|karaoke|version|vip|bootleg|flip)\\b"), "")
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }

    // Standard Levenshtein distance
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        if (len1 == 0) return len2
        if (len2 == 0) return len1

        val prev = IntArray(len2 + 1)
        val curr = IntArray(len2 + 1)

        for (j in 0..len2) prev[j] = j

        for (i in 1..len1) {
            curr[0] = i
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,      // insertion
                    prev[j] + 1,          // deletion
                    prev[j - 1] + cost    // substitution
                )
            }
            prev.indices.forEach { prev[it] = curr[it] }
        }
        return curr[len2]
    }
}
