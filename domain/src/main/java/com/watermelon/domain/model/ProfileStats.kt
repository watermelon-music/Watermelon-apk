package com.watermelon.domain.model

data class ProfileStats(
    val xpTotal: Long = 0,
    val xpLevel: Int = 1,
    val rankTier: String = "🌱 Seed Listener",
    val hoursListened: Float = 0f,
    val minutesListened: Float = 0f,
    val streakDays: Int = 0,
    val longestStreak: Int = 0,
    val songsCompleted: Int = 0,
    val songsPlayed: Int = 0,
    val artistsDiscovered: Int = 0,
    val playlistsCreated: Int = 0,
    val likedSongsCount: Int = 0,
    val topGenre: String? = null,
    val topArtist: String? = null,
)