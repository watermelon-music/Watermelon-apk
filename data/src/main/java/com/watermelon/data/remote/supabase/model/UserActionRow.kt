package com.watermelon.data.remote.supabase.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteRow(
    val user_id: String,
    val song_id: String,
    val title: String,
    val artist: String? = null,
    val cover_url: String? = null,
    val audio_url: String? = null
)

@Serializable
data class HistoryRow(
    val user_id: String,
    val song_id: String,
    val title: String,
    val artist: String? = null,
    val cover_url: String? = null,
    val audio_url: String? = null,
    val duration_ms: Long = 0,
    val played_at: String? = null
)
