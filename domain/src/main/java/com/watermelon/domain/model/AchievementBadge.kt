package com.watermelon.domain.model

data class AchievementBadge(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val unlockedAt: Long?
)