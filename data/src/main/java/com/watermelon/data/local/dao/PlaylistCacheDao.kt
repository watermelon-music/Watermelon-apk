package com.watermelon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.watermelon.data.local.entity.CachedPlaylistEntity
import com.watermelon.data.local.entity.CachedPlaylistSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistCacheDao {
    @Query("SELECT * FROM cached_playlists ORDER BY updatedAt DESC")
    suspend fun getAll(): List<CachedPlaylistEntity>

    @Query("SELECT * FROM cached_playlists WHERE id = :playlistId")
    suspend fun getById(playlistId: String): CachedPlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: CachedPlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<CachedPlaylistSongEntity>)

    @Query("DELETE FROM cached_playlist_songs WHERE playlistId = :playlistId")
    suspend fun deleteSongsForPlaylist(playlistId: String)

    @Query("DELETE FROM cached_playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("SELECT * FROM cached_playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getSongsForPlaylist(playlistId: String): Flow<List<CachedPlaylistSongEntity>>


}
