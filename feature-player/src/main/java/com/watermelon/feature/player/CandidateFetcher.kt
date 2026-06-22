package com.watermelon.feature.player

import com.watermelon.domain.model.Song

/**
 * CandidateFetcher applies recency and duplicate filters to autoplay candidates.
 * Recent tracks and duplicate ids are removed from the candidate pool.
 */
object CandidateFetcher {
    fun filter(
        candidates: List<Song>,
        recentIds: Set<String>,
        excludeIds: Set<String>
    ): List<Song> {
        return candidates.filter { it.id !in recentIds && it.id !in excludeIds }
    }
}