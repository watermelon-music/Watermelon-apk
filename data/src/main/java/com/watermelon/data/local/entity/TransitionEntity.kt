package com.watermelon.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "song_transitions",
    indices = [Index(value = ["fromSongId", "toSongId"], unique = true)]
)
data class TransitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromSongId: String,
    val toSongId: String,
    val count: Int = 1,
    val lastTransitionAt: Long = System.currentTimeMillis()
)