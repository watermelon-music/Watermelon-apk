package com.watermelon.data.remote.audius

import com.watermelon.data.remote.audius.model.AudiusTrack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudiusRepository @Inject constructor(
    private val api: AudiusApi
) {
    suspend fun getTrendingTracks(): List<AudiusTrack> {
        return runCatching {
            api.getTrendingTracks().data
        }.getOrDefault(emptyList())
    }

    suspend fun searchTracks(query: String): List<AudiusTrack> {
        return runCatching {
            api.searchTracks(query).data
        }.getOrDefault(emptyList())
    }

    fun getStreamUrl(trackId: String): String {
        return "https://discoveryprovider.audius.co/v1/tracks/$trackId/stream"
    }
}
