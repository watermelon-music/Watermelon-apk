package com.watermelon.data.repository

import com.watermelon.data.remote.lyrics.LyricsApi
import com.watermelon.domain.repository.LyricsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepositoryImpl @Inject constructor(
    private val api: LyricsApi
) : LyricsRepository {

    override suspend fun getLyrics(artist: String, title: String): Result<String> {
        return runCatching {
            val query = "$artist $title".trim()
            val results = api.searchLyrics(query)
            val best = results.firstOrNull { !it.plainLyrics.isNullOrBlank() }
                ?: results.firstOrNull { !it.syncedLyrics.isNullOrBlank() }
                ?: throw IllegalStateException("No lyrics found")
            best.plainLyrics ?: best.syncedLyrics ?: throw IllegalStateException("No lyrics found")
        }
    }
}
