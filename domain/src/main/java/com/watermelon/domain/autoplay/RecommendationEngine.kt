package com.watermelon.domain.autoplay

import com.watermelon.domain.model.Song

/**
 * Generates a ranked queue of song recommendations based on
 * artist similarity, genre, user history, favorites, and diversity rules.
 */
interface RecommendationEngine {
    /**
     * Generate up to [count] recommended songs for [currentSong].
     * Excludes songs whose IDs are in [excludeIds].
     */
    suspend fun generateQueue(
        currentSong: Song,
        excludeIds: Set<String> = emptySet(),
        count: Int = 20
    ): List<Song>

    /** Force-invalidates any internal caches. */
    fun invalidateCache()
}
