package com.watermelon.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.watermelon.domain.model.Song

@Entity(tableName = "user_actions")
data class UserActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val songId: String,
    val songTitle: String,
    val songArtist: String,
    val songCoverUrl: String?,
    @ColumnInfo(index = true) val actionType: String, // "favorite", "recent"
    val timestamp: Long = System.currentTimeMillis(),
    val syncedToServer: Boolean = false
)

fun UserActionEntity.toSong(): Song = Song(
    id = songId,
    title = songTitle,
    artistId = "",
    artistName = songArtist,
    albumId = null,
    albumName = null,
    durationMs = 0L,
    coverUrl = songCoverUrl,
    audioUrl = null,
    genre = null,
    releaseDate = null
)
