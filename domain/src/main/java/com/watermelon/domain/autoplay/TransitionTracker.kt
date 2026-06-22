package com.watermelon.domain.autoplay

import com.watermelon.domain.model.Song

interface TransitionTracker {
    suspend fun recordPlayStart(song: Song, source: String = "")
    suspend fun recordSkip(song: Song, context: String = "")
    suspend fun recordTransition(fromSongId: String, toSongId: String)
    suspend fun clearAll()
}