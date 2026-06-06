package com.watermelon.data.repository

import com.watermelon.domain.model.Playlist
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor() : PlaylistRepository {

    private val fallback = listOf(
        Playlist("p1","My Favorites","Songs I love",null,"user"),
        Playlist("p2","Workout Mix","Gym motivation",null,"user")
    )

    override fun getUserPlaylists(): Flow<List<Playlist>> = flowOf(fallback)

    override suspend fun createPlaylist(name: String, description: String?, coverUrl: String?): Result<Playlist> =
        Result.success(Playlist("new", name, description ?: "", coverUrl, "user"))

    override suspend fun addSongToPlaylist(playlistId: String, song: Song): Result<Unit> =
        Result.success(Unit)

    override suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun deletePlaylist(playlistId: String): Result<Unit> =
        Result.success(Unit)
}
