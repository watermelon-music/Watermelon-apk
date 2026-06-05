package com.watermelon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.watermelon.data.local.entity.CachedSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedSongDao {

    @Query("SELECT * FROM cached_songs WHERE cacheType = 'trending' ORDER BY cachedAt DESC LIMIT 20")
    fun getTrendingSongs(): Flow<List<CachedSongEntity>>

    @Query("SELECT * FROM cached_songs WHERE cacheType = 'search' AND searchQuery = :query ORDER BY cachedAt DESC LIMIT 20")
    fun getSearchResults(query: String): Flow<List<CachedSongEntity>>

    @Query("SELECT * FROM cached_songs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CachedSongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<CachedSongEntity>)

    @Query("DELETE FROM cached_songs WHERE cacheType = 'trending'")
    suspend fun clearTrending()

    @Query("DELETE FROM cached_songs WHERE cacheType = 'search' AND searchQuery = :query")
    suspend fun clearSearchResults(query: String)

    @Query("DELETE FROM cached_songs WHERE cachedAt < :olderThanMs")
    suspend fun deleteOlderThan(olderThanMs: Long)
}
