package com.watermelon.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_playlist_songs")
data class CachedPlaylistSongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: String,
    val songId: String,
    val position: Int,
    val title: String = "",
    val artist: String = "",
    val coverUrl: String? = null,
    val audioUrl: String? = null
)
