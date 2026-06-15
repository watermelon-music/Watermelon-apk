package com.watermelon.domain.repository

import com.watermelon.domain.model.Playlist
import com.watermelon.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getUserPlaylists(): Flow<List<Playlist>>
    suspend fun refresh(): Result<Unit>
    suspend fun createPlaylist(name: String, description: String?, coverUrl: String?): Result<Playlist>
    suspend fun addSongToPlaylist(playlistId: String, song: Song): Result<Unit>
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Result<Unit>
    suspend fun deletePlaylist(playlistId: String): Result<Unit>
    suspend fun sharePlaylist(playlistId: String): Result<String>
    suspend fun editPlaylist(playlistId: String, name: String, description: String?): Result<Unit>
}
