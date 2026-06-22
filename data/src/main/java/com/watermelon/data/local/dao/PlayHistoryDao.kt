package com.watermelon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.watermelon.data.local.entity.PlayHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayHistoryDao {
    @Insert
    suspend fun insert(history: PlayHistoryEntity): Long

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT 50")
    fun getRecent(): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM play_history WHERE songId = :songId ORDER BY playedAt DESC")
    suspend fun getForSong(songId: String): List<PlayHistoryEntity>

    @Query("DELETE FROM play_history")
    suspend fun clearAll()
}