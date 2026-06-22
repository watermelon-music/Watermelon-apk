package com.watermelon.data.repository

import android.content.Context
import com.watermelon.data.local.dao.PlayHistoryDao
import com.watermelon.data.local.dao.SkipDao
import com.watermelon.data.local.dao.SongScoreDao
import com.watermelon.data.local.dao.TransitionDao
import com.watermelon.data.local.dao.UserActionDao
import com.watermelon.data.local.entity.PlayHistoryEntity
import com.watermelon.data.local.entity.SkipEntity
import com.watermelon.data.local.entity.TransitionEntity
import com.watermelon.domain.autoplay.AutoplayEngine
import com.watermelon.domain.autoplay.RecommendationEngine
import com.watermelon.domain.autoplay.RecommendationScorer
import com.watermelon.domain.autoplay.RecommendationWeights
import com.watermelon.domain.autoplay.ScoredSong
import com.watermelon.domain.autoplay.TransitionTracker
import com.watermelon.domain.model.Song
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
    private val userActionDao: UserActionDao,
    private val recommendationEngine: RecommendationEngine
) : AutoplayEngine, TransitionTracker, RecommendationScorer {

    companion object {
        private const val BATCH_SIZE = 20
        private const val REFILL_THRESHOLD = 10
        private const val PREFS_NAME = "watermelon_autoplay"
        private const val KEY_AUTOPLAY = "autoplay_enabled"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Internal recommendation cache. Songs are popped FIFO.
    private val recommendationCache = ArrayDeque<Song>()
    private var lastCurrentSongId: String? = null

    override fun isAutoplayEnabled(): Boolean = prefs.getBoolean(KEY_AUTOPLAY, true)

    override fun setAutoplayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOPLAY, enabled).apply()
    }

    override suspend fun findNextSong(currentSong: Song, excludeIds: Set<String>): Song? {
        if (!isAutoplayEnabled()) return null

        // If the current song changed, invalidate cache so we get fresh recommendations
        if (lastCurrentSongId != null && lastCurrentSongId != currentSong.id) {
            recommendationCache.clear()
            recommendationEngine.invalidateCache()
        }
        lastCurrentSongId = currentSong.id

        // Filter out anything already in the player queue or recently played
        val recentHistory = playHistoryDao.getRecent().firstOrNull() ?: emptyList()
        val recentIds = recentHistory.map { it.songId }.toSet()
        val excludeSet = excludeIds + currentSong.id + recentIds

        // Refill cache if running low
        if (recommendationCache.size < REFILL_THRESHOLD) {
            val fresh = recommendationEngine.generateQueue(
                currentSong = currentSong,
                excludeIds = excludeSet + recommendationCache.map { it.id }.toSet(),
                count = BATCH_SIZE
            )
            fresh.forEach { recommendationCache.addLast(it) }
        }

        // Pop the best available candidate that is not excluded
        while (recommendationCache.isNotEmpty()) {
            val candidate = recommendationCache.removeFirst()
            if (candidate.id !in excludeSet) {
                return candidate
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
        recommendationCache.clear()
        recommendationEngine.invalidateCache()
    }

    // ------------------------------------------------------------------
    // RecommendationScorer (kept for backward compatibility; delegates)
    // ------------------------------------------------------------------

    override suspend fun score(
        candidate: Song,
        currentSong: Song?,
        weights: RecommendationWeights
    ): Double {
        if (currentSong == null) return 0.0
        val ranked = recommendationEngine.generateQueue(currentSong, setOf(currentSong.id), count = BATCH_SIZE)
        return ranked.find { it.id == candidate.id }?.let { 1.0 } ?: 0.0
    }

    override suspend fun rank(
        candidates: List<Song>,
        currentSong: Song?,
        weights: RecommendationWeights
    ): List<ScoredSong> {
        if (currentSong == null) return candidates.map { ScoredSong(it, 0.0) }
        val generated = recommendationEngine.generateQueue(currentSong, setOf(currentSong.id), count = BATCH_SIZE)
        val generatedIds = generated.map { it.id }.toSet()
        // Return scores for candidates that exist in generated; others get 0
        return candidates.map { song ->
            val score = if (song.id in generatedIds) 1.0 else 0.0
            ScoredSong(song, score)
        }.sortedByDescending { it.score }
    }
}
