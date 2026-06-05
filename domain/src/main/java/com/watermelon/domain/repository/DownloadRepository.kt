package com.watermelon.domain.repository

interface DownloadRepository {
    suspend fun download(url: String, fileName: String): Result<Unit>
    fun getDownloadStatus(downloadId: Long): Int?
}
