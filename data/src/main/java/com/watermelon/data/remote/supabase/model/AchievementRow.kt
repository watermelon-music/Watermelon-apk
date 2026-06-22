package com.watermelon.data.remote.supabase.model

import kotlinx.serialization.Serializable

@Serializable
data class AchievementRow(
    val id: String,
    val user_id: String,
    val badge_id: String,
    val badge_name: String,
    val badge_emoji: String,
    val description: String?,
    val unlocked_at: kotlinx.datetime.Instant?
)