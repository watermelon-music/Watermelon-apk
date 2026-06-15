package com.watermelon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.watermelon.data.local.entity.SkipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SkipDao {
    @Insert
    suspend fun insert(skip: SkipEntity): Long

    @Query("SELECT * FROM skips ORDER BY skippedAt DESC LIMIT 50")
    fun getRecent(): Flow<List<SkipEntity>>

    @Query("SELECT * FROM skips WHERE songId = :songId")
    suspend fun getForSong(songId: String): List<SkipEntity>

    @Query("DELETE FROM skips")
    suspend fun clearAll()
}