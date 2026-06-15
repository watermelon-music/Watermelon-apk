package com.watermelon.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skips")
data class SkipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val songId: String,
    val songTitle: String,
    val songArtist: String,
    val skippedAt: Long = System.currentTimeMillis(),
    val context: String = ""
)