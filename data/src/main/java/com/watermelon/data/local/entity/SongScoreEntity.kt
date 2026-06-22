package com.watermelon.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_scores")
data class SongScoreEntity(
    @PrimaryKey val songId: String,
    val score: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)