package com.watermelon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.watermelon.data.local.entity.TransitionEntity

@Dao
interface TransitionDao {
    @Query("SELECT * FROM song_transitions WHERE fromSongId = :songId")
    suspend fun getFrom(songId: String): List<TransitionEntity>

    @Query("SELECT * FROM song_transitions WHERE fromSongId = :from AND toSongId = :to")
    suspend fun getTransition(from: String, to: String): TransitionEntity?

    @Insert
    suspend fun insert(transition: TransitionEntity): Long

    @Update
    suspend fun update(transition: TransitionEntity): Int

    @Query("DELETE FROM song_transitions")
    suspend fun clearAll()
}