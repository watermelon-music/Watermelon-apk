package com.watermelon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.watermelon.data.local.entity.RadioStationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RadioStationDao {

    @Query("SELECT * FROM radio_stations WHERE actionType = 'favorite' ORDER BY timestamp DESC")
    fun getFavoriteStations(): Flow<List<RadioStationEntity>>

    @Query("SELECT * FROM radio_stations WHERE actionType = 'recent' ORDER BY timestamp DESC LIMIT 50")
    fun getRecentStations(): Flow<List<RadioStationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: RadioStationEntity)

    @Query("DELETE FROM radio_stations WHERE stationUuid = :stationUuid AND actionType = 'favorite'")
    suspend fun removeFavorite(stationUuid: String)

    @Query("DELETE FROM radio_stations WHERE actionType = 'recent' AND stationUuid = :stationUuid")
    suspend fun removeRecent(stationUuid: String)

    @Query("DELETE FROM radio_stations WHERE actionType = 'recent' AND stationUuid NOT IN (SELECT stationUuid FROM radio_stations WHERE actionType = 'recent' ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun trimRecentTo(limit: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM radio_stations WHERE stationUuid = :stationUuid AND actionType = 'favorite')")
    fun isFavorite(stationUuid: String): Flow<Boolean>
}
