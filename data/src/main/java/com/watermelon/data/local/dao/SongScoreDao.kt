package com.watermelon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.watermelon.data.local.entity.SongScoreEntity

@Dao
interface SongScoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(score: SongScoreEntity)

    @Query("SELECT * FROM song_scores WHERE songId = :songId")
    suspend fun getScore(songId: String): SongScoreEntity?

    @Query("DELETE FROM song_scores")
    suspend fun clearAll()
}