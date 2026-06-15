package com.watermelon.domain.autoplay

import com.watermelon.domain.model.Song

data class RecommendationWeights(
    val transitionFreq: Double = 30.0,
    val likeSkipRatio: Double = 20.0,
    val skipPenalty: Double = 15.0,
    val recencyDecay: Double = 0.5,
    val tagSimilarity: Double = 5.0
)

interface RecommendationScorer {
    suspend fun score(candidate: Song, currentSong: Song?, weights: RecommendationWeights = RecommendationWeights()): Double
    suspend fun rank(candidates: List<Song>, currentSong: Song?, weights: RecommendationWeights = RecommendationWeights()): List<ScoredSong>
}