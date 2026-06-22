package com.watermelon.domain.repository

import com.watermelon.domain.model.LeaderboardEntry

interface LeaderboardRepository {
    suspend fun getGlobalLeaderboard(limit: Int = 50): Result<List<LeaderboardEntry>>
}