package com.watermelon.domain.repository

import com.watermelon.domain.model.RadioStation
import kotlinx.coroutines.flow.Flow

interface RadioStationRepository {
    fun getFavoriteStations(): Flow<List<RadioStation>>
    fun getRecentStations(): Flow<List<RadioStation>>
    suspend fun addFavorite(station: RadioStation)
    suspend fun removeFavorite(stationUuid: String)
    suspend fun recordRecentlyPlayed(station: RadioStation)
    fun isFavorite(stationUuid: String): Flow<Boolean>
}
