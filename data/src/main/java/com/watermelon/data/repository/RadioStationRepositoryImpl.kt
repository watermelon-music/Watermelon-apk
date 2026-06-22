package com.watermelon.data.repository

import com.watermelon.data.local.dao.RadioStationDao
import com.watermelon.data.local.entity.toDomain
import com.watermelon.data.local.entity.toEntity
import com.watermelon.data.remote.supabase.model.RadioFavoriteRow
import com.watermelon.domain.model.RadioStation
import com.watermelon.domain.repository.RadioStationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioStationRepositoryImpl @Inject constructor(
    private val dao: RadioStationDao,
    private val client: SupabaseClient
) : RadioStationRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch { syncRemoteFavorites() }
    }

    override fun getFavoriteStations(): Flow<List<RadioStation>> {
        return dao.getFavoriteStations().map { list -> list.map { it.toDomain() } }
    }

    override fun getRecentStations(): Flow<List<RadioStation>> {
        return dao.getRecentStations().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addFavorite(station: RadioStation) {
        val uuid = station.stationuuid ?: "${station.name}_${station.url}"
        dao.insertStation(station.toEntity("favorite"))
        val userId = getUserId()
        if (userId != null) {
            runCatching {
                client.postgrest.from("radio_favorites").upsert(
                    RadioFavoriteRow(
                        user_id = userId,
                        station_uuid = uuid,
                        name = station.name,
                        url = station.url,
                        favicon = station.favicon,
                        country = station.country,
                        tags = station.tags
                    )
                )
            }.onFailure { Timber.e(it, "Supabase addFavorite failed") }
        }
    }

    override suspend fun removeFavorite(stationUuid: String) {
        dao.removeFavorite(stationUuid)
        val userId = getUserId()
        if (userId != null) {
            runCatching {
                client.postgrest.from("radio_favorites").delete {
                    filter {
                        eq("user_id", userId)
                        eq("station_uuid", stationUuid)
                    }
                }
            }.onFailure { Timber.e(it, "Supabase removeFavorite failed") }
        }
    }

    override suspend fun recordRecentlyPlayed(station: RadioStation) {
        dao.insertStation(station.toEntity("recent"))
        dao.trimRecentTo(50)
    }

    override fun isFavorite(stationUuid: String): Flow<Boolean> {
        return dao.isFavorite(stationUuid)
    }

    private suspend fun syncRemoteFavorites() {
        val userId = getUserId() ?: return
        val remote = runCatching {
            client.postgrest.from("radio_favorites")
                .select { filter { eq("user_id", userId) } }
                .decodeList<RadioFavoriteRow>()
        }.getOrNull() ?: return

        // Merge remote favorites into local DB
        remote.forEach { row ->
            val station = RadioStation(
                stationuuid = row.station_uuid,
                name = row.name,
                url = row.url,
                urlResolved = row.url,
                homepage = null,
                favicon = row.favicon,
                country = row.country,
                countrycode = null,
                language = null,
                tags = row.tags,
                bitrate = 0,
                votes = 0
            )
            dao.insertStation(station.toEntity("favorite"))
        }
    }

    private fun getUserId(): String? = client.auth.currentUserOrNull()?.id
}
