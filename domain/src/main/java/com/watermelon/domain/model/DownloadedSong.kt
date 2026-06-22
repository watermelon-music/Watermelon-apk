package com.watermelon.domain.model

data class DownloadedSong(
    val songId: String,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val localFilePath: String,
    val fileSize: Long,
    val downloadedAt: Long
)
