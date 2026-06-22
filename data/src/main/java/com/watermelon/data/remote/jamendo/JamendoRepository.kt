package com.watermelon.data.remote.jamendo

import com.watermelon.data.BuildConfig
import com.watermelon.data.remote.jamendo.model.JamendoTrack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JamendoRepository @Inject constructor(
    private val api: JamendoApi
) {
    private val clientId = BuildConfig.JAMENDO_CLIENT_ID.takeIf { it.isNotBlank() && it != "null" }
        ?: ""

    suspend fun getTrendingTracks(limit: Int = 20): List<JamendoTrack> {
        if (clientId.isBlank()) return emptyList()
        return runCatching {
            api.getTracks(clientId = clientId, limit = limit).results
        }.getOrDefault(emptyList())
    }

    suspend fun searchTracks(query: String, limit: Int = 20): List<JamendoTrack> {
        if (clientId.isBlank()) return emptyList()
        return runCatching {
            api.searchTracks(clientId = clientId, query = query, limit = limit).results
        }.getOrDefault(emptyList())
    }
}
