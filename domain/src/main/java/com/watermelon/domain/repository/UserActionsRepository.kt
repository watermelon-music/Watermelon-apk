package com.watermelon.domain.repository

import com.watermelon.domain.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * User-specific actions stored in Supabase.
 * (favorites, recently played, user-created playlists, etc.)
 */
interface UserActionsRepository {
    fun getRecentlyPlayed(): Flow<List<Song>>
    fun getFavorites(): Flow<List<Song>>

    suspend fun addToFavorites(song: Song): Result<Unit>
    suspend fun removeFromFavorites(songId: String): Result<Unit>
    suspend fun recordRecentlyPlayed(song: Song): Result<Unit>
}
