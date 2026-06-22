package com.watermelon.domain.repository

import com.watermelon.domain.model.AchievementBadge
import com.watermelon.domain.model.ProfileStats

interface ProfileStatsRepository {
    suspend fun getProfileStats(): Result<ProfileStats>
    suspend fun getAchievements(): Result<List<AchievementBadge>>
}