package com.watermelon.data.remote.supabase.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfileStatsRow(
    val id: String,
    val xp_total: Long = 0,
    val xp_level: Int = 1,
    val rank_tier: String = "🌱 Seed Listener",
    val hours_listened: Float = 0f,
    val minutes_listened: Float = 0f,
    val streak_days: Int = 0,
    val longest_streak: Int = 0,
    val songs_completed: Int = 0,
    val songs_played: Int = 0,
    val artists_discovered: Int = 0,
    val playlists_created: Int = 0,
    val liked_songs_count: Int = 0,
    val top_genre: String? = null,
    val top_artist: String? = null,
)