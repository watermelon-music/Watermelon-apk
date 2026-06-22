package com.watermelon.domain.autoplay

import com.watermelon.domain.model.Song

interface AutoplayEngine {
    suspend fun findNextSong(currentSong: Song, excludeIds: Set<String> = emptySet()): Song?
    fun isAutoplayEnabled(): Boolean
    fun setAutoplayEnabled(enabled: Boolean)
    suspend fun clearAll()
}