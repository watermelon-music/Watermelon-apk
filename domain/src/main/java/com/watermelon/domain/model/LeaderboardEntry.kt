package com.watermelon.domain.model

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val username: String,
    val displayName: String?,
    val avatarUrl: String?,
    val xpTotal: Long,
    val rankTier: String,
    val streakDays: Int,
)