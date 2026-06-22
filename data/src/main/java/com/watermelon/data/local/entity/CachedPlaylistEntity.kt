package com.watermelon.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_playlists")
data class CachedPlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val ownerId: String,
    val shareCode: String? = null,
    val isPublic: Boolean = false,
    val shareCount: Long = 0,
    val saveCount: Long = 0,
    val copyCount: Long = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val cachedAt: Long = System.currentTimeMillis()
)
