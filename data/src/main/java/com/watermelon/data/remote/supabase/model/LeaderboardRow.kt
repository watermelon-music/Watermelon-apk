package com.watermelon.data.remote.supabase.model

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardRow(
    val user_id: String,
    val username: String? = null,
    val display_name: String? = null,
    val avatar_url: String? = null,
    val xp_total: Long = 0,
    val xp_level: Int = 1,
    val rank_tier: String = "🌱 Seed Listener",
    val streak_days: Int = 0,
    val hours_listened: Float = 0f,
    val rank_position: Int = 0,
)