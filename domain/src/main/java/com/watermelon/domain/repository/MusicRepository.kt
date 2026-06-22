package com.watermelon.domain.repository

import com.watermelon.domain.model.Playlist
import com.watermelon.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun getRecentlyPlayed(): Flow<List<Song>>
    fun getFavorites(): Flow<List<Song>>
    fun getTrendingMusic(): Flow<List<Song>>
    fun getRecommendedPlaylists(): Flow<List<Playlist>>
}
