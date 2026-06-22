package com.watermelon.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val songId: String,
    val songTitle: String,
    val songArtist: String,
    val songCoverUrl: String?,
    val audioUrl: String?,
    val durationPlayedMs: Long = 0L,
    val source: String = "",
    val playedAt: Long = System.currentTimeMillis()
)