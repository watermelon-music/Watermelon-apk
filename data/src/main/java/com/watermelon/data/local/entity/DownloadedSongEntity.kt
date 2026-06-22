package com.watermelon.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_songs")
data class DownloadedSongEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val localFilePath: String,
    val fileSize: Long = 0,
    val downloadedAt: Long = System.currentTimeMillis()
)
