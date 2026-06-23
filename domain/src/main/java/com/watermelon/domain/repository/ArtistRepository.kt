package com.watermelon.domain.repository

import com.watermelon.domain.model.Artist
import com.watermelon.domain.model.Song

interface ArtistRepository {
    suspend fun searchArtists(query: String): Result<List<Artist>>
    suspend fun getArtistDetails(channelUrl: String): Result<Artist>
    suspend fun getArtistSongs(channelUrl: String): Result<List<Song>>
}