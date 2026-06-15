package com.watermelon.domain.repository

import com.watermelon.domain.model.DownloadedSong
import com.watermelon.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getDownloads(): Flow<List<DownloadedSong>>
    suspend fun downloadSong(song: Song, url: String): Result<Unit>
    suspend fun deleteDownload(songId: String): Result<Unit>
    suspend fun cleanupMissingFiles(): Result<Unit>
    suspend fun isDownloaded(songId: String): Boolean
    suspend fun getDownloadPath(songId: String): String?
}
