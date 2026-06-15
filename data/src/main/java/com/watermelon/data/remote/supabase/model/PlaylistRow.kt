package com.watermelon.data.remote.supabase.model

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistRow(
    val id: String,
    val user_id: String,
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
data class PlaylistSongRow(
    val id: String? = null,
    val playlist_id: String,
    val song_id: String,
    val title: String,
    val artist: String? = null,
    val cover_url: String? = null,
    val audio_url: String? = null,
    val position: Int = 0
)
