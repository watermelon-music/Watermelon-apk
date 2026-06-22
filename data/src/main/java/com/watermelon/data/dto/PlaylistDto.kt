package com.watermelon.data.dto

import com.watermelon.domain.model.Playlist
import kotlinx.serialization.Serializable

@Serializable
 data class PlaylistDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val cover_url: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val share_code: String? = null,
    val is_public: Boolean = false,
    val share_count: Long = 0,
    val save_count: Long = 0,
    val copy_count: Long = 0
)

@Serializable
 data class PlaylistSongEntryDto(
    val playlist_id: String,
    val song_id: String,
    val song_title: String,
    val song_artist: String,
    val song_cover_url: String? = null,
    val position: Int = 0
)

fun PlaylistDto.toDomain(): Playlist = Playlist(
    id = id,
    name = name,
    description = description,
    coverUrl = cover_url,
    ownerId = "user",
    shareCode = share_code,
    isPublic = is_public,
    shareCount = share_count,
    saveCount = save_count,
    copyCount = copy_count
)
