package com.watermelon.data.repository

import com.watermelon.data.remote.supabase.model.AchievementRow
import com.watermelon.data.remote.supabase.model.ProfileStatsRow
import com.watermelon.domain.model.AchievementBadge
import com.watermelon.domain.model.ProfileStats
import com.watermelon.domain.repository.ProfileStatsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileStatsRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : ProfileStatsRepository {

    override suspend fun getProfileStats(): Result<ProfileStats> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = client.auth.currentUserOrNull()?.id
                ?: throw IllegalStateException("Not signed in")
            val row = client.postgrest.from("profiles")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<ProfileStatsRow>()
                ?: throw IllegalStateException("Profile not found")
            row.toProfileStats()
        }
    }

    override suspend fun getAchievements(): Result<List<AchievementBadge>> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = client.auth.currentUserOrNull()?.id
                ?: throw IllegalStateException("Not signed in")
            val rows = client.postgrest.from("user_achievements")
                .select {
                    filter { eq("user_id", userId) }
                    order("unlocked_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<AchievementRow>()
            rows.map { row -> row.toAchievementBadge() }
        }
    }

    private fun ProfileStatsRow.toProfileStats() = ProfileStats(
        xpTotal = xp_total,
        xpLevel = xp_level,
        rankTier = rank_tier,
        hoursListened = hours_listened,
        minutesListened = minutes_listened,
        streakDays = streak_days,
        longestStreak = longest_streak,
        songsCompleted = songs_completed,
        songsPlayed = songs_played,
        artistsDiscovered = artists_discovered,
        playlistsCreated = playlists_created,
        likedSongsCount = liked_songs_count,
        topGenre = top_genre,
        topArtist = top_artist,
    )

    private fun AchievementRow.toAchievementBadge() = AchievementBadge(
        id = badge_id,
        name = badge_name,
        emoji = badge_emoji,
        description = description ?: "",
        unlockedAt = unlocked_at?.toEpochMilliseconds()
    )
}
