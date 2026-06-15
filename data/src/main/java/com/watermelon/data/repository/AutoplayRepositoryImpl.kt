package com.watermelon.data.repository

import android.content.Context
import com.watermelon.data.local.dao.PlayHistoryDao
import com.watermelon.data.local.dao.SkipDao
import com.watermelon.data.local.dao.SongScoreDao
import com.watermelon.data.local.dao.TransitionDao
import com.watermelon.data.local.dao.UserActionDao
import com.watermelon.data.local.entity.PlayHistoryEntity
import com.watermelon.data.local.entity.SkipEntity
import com.watermelon.data.local.entity.SongScoreEntity
import com.watermelon.data.local.entity.TransitionEntity
import com.watermelon.domain.autoplay.AutoplayEngine
import com.watermelon.domain.autoplay.RecommendationScorer
import com.watermelon.domain.autoplay.RecommendationWeights
import com.watermelon.domain.autoplay.ScoredSong
import com.watermelon.domain.autoplay.TransitionTracker
import com.watermelon.domain.model.Song
import com.watermelon.domain.model.YtDlpMetadata
import com.watermelon.domain.repository.MusicCatalogRepository
import com.watermelon.domain.repository.UrlExtractorRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoplayRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val playHistoryDao: PlayHistoryDao,
    private val skipDao: SkipDao,
    private val transitionDao: TransitionDao,
    private val songScoreDao: SongScoreDao,
    private val catalogRepository: MusicCatalogRepository,
    private val urlExtractor: UrlExtractorRepository,
    private val userActionDao: UserActionDao
) : AutoplayEngine, TransitionTracker, RecommendationScorer {

    private val prefs = context.getSharedPreferences("watermelon_autoplay", Context.MODE_PRIVATE)

    override fun isAutoplayEnabled(): Boolean = prefs.getBoolean("autoplay_enabled", true)
    override fun setAutoplayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("autoplay_enabled", enabled).apply()
    }

    override suspend fun findNextSong(currentSong: Song, excludeIds: Set<String>): Song? {
        if (!isAutoplayEnabled()) return null

        // Recency filter: exclude last 50 played songs
        val recentHistory = playHistoryDao.getRecent().first()
        val recentIds = recentHistory.map { it.songId }.toSet()
        val excludeSet = excludeIds + currentSong.id + recentIds

        // Metadata-based query building
        val audioUrl = currentSong.audioUrl
        val youtubeUrl = if (audioUrl != null && audioUrl.contains("youtube")) {
            audioUrl
        } else {
            "https://www.youtube.com/watch?v=${currentSong.id}"
        }
        val metadata: YtDlpMetadata? = urlExtractor.extractMetadata(youtubeUrl).getOrNull()

        val queries = mutableListOf<String>()
        if (metadata != null) {
            val channel = metadata.channel
            if (!channel.isNullOrBlank()) {
                queries.add("$channel music mixes")
                queries.add(channel)
            }
            val tags = metadata.tags
            tags.take(3).forEach { tag ->
                if (tag.isNotBlank()) queries.add("$tag music")
            }
            val artist = metadata.artist
            val title = metadata.title
            if (!artist.isNullOrBlank() && !title.isNullOrBlank()) {
                queries.add("$artist $title")
            } else if (!title.isNullOrBlank()) {
                queries.add(title)
            }
        } else {
            if (currentSong.artistName.isNotBlank() && currentSong.title.isNotBlank()) {
                queries.add("${currentSong.artistName} ${currentSong.title}")
            }
            if (currentSong.artistName.isNotBlank()) {
                queries.add("${currentSong.artistName} music")
            }
            currentSong.genre?.takeIf { it.isNotBlank() }?.let { queries.add("$it music") }
        }

        for (query in queries) {
            val candidates = runCatching {
                catalogRepository.search(query).first()
                    .filter { it.id !in excludeSet }
            }.getOrNull()
            if (!candidates.isNullOrEmpty()) {
                val ranked = rank(candidates, currentSong)
                return ranked.firstOrNull()?.song
            }
        }
        return null
    }

    override suspend fun recordPlayStart(song: Song, source: String) {
        playHistoryDao.insert(
            PlayHistoryEntity(
                songId = song.id,
                songTitle = song.title,
                songArtist = song.artistName,
                songCoverUrl = song.coverUrl,
                audioUrl = song.audioUrl,
                source = source,
                playedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun recordSkip(song: Song, context: String) {
        skipDao.insert(
            SkipEntity(
                songId = song.id,
                songTitle = song.title,
                songArtist = song.artistName,
                skippedAt = System.currentTimeMillis(),
                context = context
            )
        )
    }

    override suspend fun recordTransition(fromSongId: String, toSongId: String) {
        val existing = transitionDao.getTransition(fromSongId, toSongId)
        if (existing != null) {
            transitionDao.update(
                existing.copy(
                    count = existing.count + 1,
                    lastTransitionAt = System.currentTimeMillis()
                )
            )
        } else {
            transitionDao.insert(
                TransitionEntity(
                    fromSongId = fromSongId,
                    toSongId = toSongId,
                    count = 1,
                    lastTransitionAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun clearAll() {
        playHistoryDao.clearAll()
        skipDao.clearAll()
        transitionDao.clearAll()
        songScoreDao.clearAll()
    }

    override suspend fun score(candidate: Song, currentSong: Song?, weights: RecommendationWeights): Double {
        return rank(listOf(candidate), currentSong, weights).firstOrNull()?.score ?: 0.0
    }

    override suspend fun rank(candidates: List<Song>, currentSong: Song?, weights: RecommendationWeights): List<ScoredSong> {
        if (currentSong == null) return candidates.map { ScoredSong(it, 0.0) }

        val transitions = transitionDao.getFrom(currentSong.id)
        val transitionMap = transitions.associate { it.toSongId to it.count }

        val favorites = userActionDao.getFavorites().first()
        val skipList = userActionDao.getSkips().first()
        val recentPlays = playHistoryDao.getRecent().first()
        val recentIndexMap = recentPlays.mapIndexed { idx, entity -> entity.songId to idx }.toMap()

        return candidates.map { candidate ->
            var score = 0.0

            val transitionCount = transitionMap[candidate.id] ?: 0
            score += transitionCount * weights.transitionFreq

            val likes = favorites.count { it.songId == candidate.id }
            val skips = skipList.count { it.songId == candidate.id }
            val plays = recentPlays.count { it.songId == candidate.id }
            val totalActions = plays.coerceAtLeast(1)
            score += (likes.toDouble() / totalActions) * weights.likeSkipRatio
            score -= (skips.toDouble() / totalActions) * weights.skipPenalty

            val recentIndex = recentIndexMap[candidate.id] ?: -1
            if (recentIndex >= 0) {
                score -= (50 - recentIndex) * weights.recencyDecay
            }

            ScoredSong(candidate, score)
        }.sortedByDescending { it.score }
    }
}
