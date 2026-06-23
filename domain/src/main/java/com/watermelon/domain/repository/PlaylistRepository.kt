package com.watermelon.domain.repository

import com.watermelon.domain.model.CommunityPlaylist
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
    suspend fun getPlaylistById(playlistId: String): Result<Playlist>
    suspend fun getCommunityPlaylists(): Result<List<CommunityPlaylist>>
    suspend fun likeCommunityPlaylist(playlistId: String): Result<Boolean>
    suspend fun saveCommunityPlaylist(playlist: CommunityPlaylist): Result<Playlist>
    suspend fun searchPlaylists(query: String): Result<List<CommunityPlaylist>>
    suspend fun fetchPlaylistSongs(playlistUrl: String): Result<List<Song>>
}
