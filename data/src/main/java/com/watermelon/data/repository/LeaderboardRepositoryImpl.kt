package com.watermelon.data.repository

import com.watermelon.data.remote.supabase.model.LeaderboardRow
import com.watermelon.domain.model.LeaderboardEntry
import com.watermelon.domain.repository.LeaderboardRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaderboardRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : LeaderboardRepository {

    override suspend fun getGlobalLeaderboard(limit: Int): Result<List<LeaderboardEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val rows = client.postgrest.from("leaderboard_global")
                .select {
                    order("rank_position", Order.ASCENDING)
                }
                .decodeList<LeaderboardRow>()
                .take(limit)
            rows.map { it.toLeaderboardEntry() }
        }
    }

    private fun LeaderboardRow.toLeaderboardEntry() = LeaderboardEntry(
        rank = rank_position,
        userId = user_id,
        username = username ?: "User",
        displayName = display_name,
        avatarUrl = avatar_url,
        xpTotal = xp_total,
        rankTier = rank_tier,
        streakDays = streak_days,
    )
}