package com.watermelon.domain.repository

interface LyricsRepository {
    suspend fun getLyrics(artist: String, title: String): Result<String>
}
