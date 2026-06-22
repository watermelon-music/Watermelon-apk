package com.watermelon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.watermelon.data.local.entity.UserActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserActionDao {

    @Query("SELECT * FROM user_actions WHERE actionType = 'recent' ORDER BY timestamp DESC LIMIT 50")
    fun getRecentlyPlayed(): Flow<List<UserActionEntity>>

    @Query("SELECT * FROM user_actions WHERE actionType = 'favorite' ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<UserActionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: UserActionEntity): Long

    @Query("DELETE FROM user_actions WHERE songId = :songId AND actionType = 'favorite'")
    suspend fun removeFavorite(songId: String)

    @Query("DELETE FROM user_actions WHERE actionType = 'recent' AND songId = :songId")
    suspend fun removeRecent(songId: String)

    @Query("SELECT COUNT(*) FROM user_actions WHERE actionType = 'recent'")
    suspend fun countRecent(): Int

    @Query("DELETE FROM user_actions WHERE actionType = 'recent' AND id NOT IN (SELECT id FROM user_actions WHERE actionType = 'recent' ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun trimRecentTo(limit: Int)

    @Query("DELETE FROM user_actions WHERE actionType = 'favorite'")
    suspend fun clearFavorites()

    @Query("SELECT * FROM user_actions WHERE actionType = 'skip' ORDER BY timestamp DESC LIMIT 50")
    fun getSkips(): Flow<List<UserActionEntity>>

    @Query("DELETE FROM user_actions WHERE actionType = 'skip'")
    suspend fun clearSkips()

    @Query("UPDATE user_actions SET syncedToServer = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT * FROM user_actions WHERE syncedToServer = 0")
    suspend fun getUnsynced(): List<UserActionEntity>
}
