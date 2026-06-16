package com.watermelon.data.repository

import android.content.Context
import com.watermelon.data.BuildConfig
import com.watermelon.data.local.dao.PlaylistCacheDao
import com.watermelon.data.local.entity.CachedPlaylistEntity
import com.watermelon.data.local.entity.CachedPlaylistSongEntity
import com.watermelon.data.remote.supabase.model.PlaylistRow
import com.watermelon.data.remote.supabase.model.PlaylistSongRow
import com.watermelon.domain.model.Playlist
import com.watermelon.domain.model.PlaylistSong
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.PlaylistRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
    @ApplicationContext private val context: Context,
    private val playlistCacheDao: PlaylistCacheDao
) : PlaylistRepository {

    private val prefs = context.getSharedPreferences("watermelon_playlists", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val _playlists = MutableStateFlow(loadLocalCache())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            if (_playlists.value.isEmpty()) {
                val cached = playlistCacheDao.getAll()
                _playlists.value = cached.map { it.toDomain() }
            }
        }
        scope.launch {
            // Immediate refresh if already logged in, then listen to session changes
            if (isLoggedIn()) {
                kotlin.runCatching { refreshPlaylists() }.onFailure { Timber.e(it, "Playlist init refresh failed") }
            }
            client.auth.sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) {
                    kotlin.runCatching { refreshPlaylists() }.onFailure { Timber.e(it, "Playlist session refresh failed") }
                }
            }
        }
    }

    override fun getUserPlaylists(): Flow<List<Playlist>> = _playlists.asStateFlow()

    override suspend fun refresh(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { refreshPlaylists() }
    }

    override suspend fun createPlaylist(
        name: String,
        description: String?,
        coverUrl: String?
    ): Result<Playlist> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = getUserId() ?: throw IllegalStateException("Not authenticated. Please sign in to create playlists.")
            val newRow = PlaylistRow(
                id = java.util.UUID.randomUUID().toString(),
                user_id = userId,
                name = name,
                description = description,
                cover_url = coverUrl,
                share_code = generateShareCode()
            )
            client.postgrest.from("playlists").insert(newRow)
            refreshPlaylists()
            _playlists.value.firstOrNull { it.id == newRow.id } ?: newRow.toDomain(emptyList())
        }.onFailure { Timber.e(it, "createPlaylist failed for user=${getUserId()}") }
    }

    override suspend fun addSongToPlaylist(playlistId: String, song: Song): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val playlist = _playlists.value.firstOrNull { it.id == playlistId }
                    ?: throw IllegalStateException("Playlist not found")
                if (playlist.songs.any { it.songId == song.id }) {
                    throw IllegalStateException("Song already in playlist")
                }
                val position = playlist.songs.size
                val row = PlaylistSongRow(
                    playlist_id = playlistId,
                    song_id = song.id,
                    title = song.title,
                    artist = song.artistName,
                    cover_url = song.coverUrl,
                    audio_url = song.audioUrl,
                    position = position
                )
                client.postgrest.from("playlist_songs").insert(row)
                refreshPlaylists()
            }
        }

    override suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.postgrest.from("playlist_songs").delete {
                    filter {
                        eq("playlist_id", playlistId)
                        eq("song_id", songId)
                    }
                }
                refreshPlaylists()
            }
        }

    override suspend fun deletePlaylist(playlistId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.postgrest.from("playlist_songs").delete {
                    filter { eq("playlist_id", playlistId) }
                }
                client.postgrest.from("playlists").delete {
                    filter { eq("id", playlistId) }
                }
                val updated = _playlists.value.filter { it.id != playlistId }
                _playlists.value = updated
                saveLocalCache(updated)
                playlistCacheDao.deleteSongsForPlaylist(playlistId)
                playlistCacheDao.deletePlaylist(playlistId)
            }.onFailure { Timber.e(it, "deletePlaylist failed for playlistId=$playlistId") }
        }

    override suspend fun sharePlaylist(playlistId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val playlist = _playlists.value.firstOrNull { it.id == playlistId }
                ?: throw IllegalStateException("Playlist not found")
            val code = playlist.shareCode ?: generateShareCode()
            if (playlist.shareCode == null) {
                client.postgrest.from("playlists").update({
                    set("share_code", code)
                    set("share_count", playlist.shareCount + 1)
                }) {
                    filter { eq("id", playlistId) }
                }
            } else {
                client.postgrest.from("playlists").update({
                    set("share_count", playlist.shareCount + 1)
                }) {
                    filter { eq("id", playlistId) }
                }
            }
            val updated = _playlists.value.map {
                if (it.id == playlistId) it.copy(shareCode = code, shareCount = it.shareCount + 1) else it
            }
            _playlists.value = updated
            saveLocalCache(updated)
            code
        }
    }

    override suspend fun editPlaylist(playlistId: String, name: String, description: String?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.postgrest.from("playlists").update({
                set("name", name)
                set("description", description)
            }) {
                filter { eq("id", playlistId) }
            }
            val updated = _playlists.value.map {
                if (it.id == playlistId) it.copy(name = name, description = description) else it
            }
            _playlists.value = updated
            saveLocalCache(updated)
        }
    }

    private fun generateShareCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..8).map { chars.random() }.joinToString("")
    }

    private suspend fun fetchRemotePlaylists(): List<Playlist> {
        val userId = getUserId() ?: return emptyList()
        val rows = client.postgrest.from("playlists")
            .select { filter { eq("user_id", userId) } }
            .decodeList<PlaylistRow>()
        return rows.map { row ->
            val songs = client.postgrest.from("playlist_songs")
                .select {
                    filter { eq("playlist_id", row.id) }
                    order("position", Order.ASCENDING)
                }
                .decodeList<PlaylistSongRow>()
            row.toDomain(songs)
        }
    }

    private suspend fun refreshPlaylists() {
        runCatching { fetchRemotePlaylists() }.getOrNull()?.let { remote ->
            _playlists.value = remote
            saveLocalCache(remote)
            remote.forEach { playlist ->
                playlistCacheDao.deleteSongsForPlaylist(playlist.id)
                playlistCacheDao.insertPlaylist(playlist.toEntity())
                playlistCacheDao.insertSongs(playlist.songs.map { it.toEntity(playlist.id) })
            }
        }
    }

    private fun isLoggedIn(): Boolean = client.auth.currentUserOrNull() != null

    private fun getUserId(): String? = client.auth.currentUserOrNull()?.id

    private fun loadLocalCache(): List<Playlist> {
        val raw = prefs.getString("playlist_cache", null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Playlist>>(raw) }.getOrDefault(emptyList())
    }

    private fun saveLocalCache(list: List<Playlist>) {
        prefs.edit().putString("playlist_cache", json.encodeToString(list)).apply()
    }

    private fun PlaylistRow.toDomain(songRows: List<PlaylistSongRow>): Playlist {
        return Playlist(
            id = id,
            name = name,
            description = description,
            coverUrl = cover_url,
            ownerId = user_id,
            songs = songRows.map {
                PlaylistSong(
                    songId = it.song_id,
                    position = it.position,
                    title = it.title ?: "",
                    artist = it.artist ?: "",
                    coverUrl = it.cover_url,
                    audioUrl = it.audio_url
                )
            },
            createdAt = kotlin.runCatching {
                java.time.Instant.parse(created_at ?: "")
            }.getOrDefault(java.time.Instant.now()).toEpochMilli(),
            updatedAt = kotlin.runCatching {
                java.time.Instant.parse(updated_at ?: "")
            }.getOrDefault(java.time.Instant.now()).toEpochMilli(),
            shareCode = share_code,
            isPublic = is_public,
            shareCount = share_count,
            saveCount = save_count,
            copyCount = copy_count
        )
    }

    private fun Playlist.toEntity(): CachedPlaylistEntity = CachedPlaylistEntity(
        id = id,
        name = name,
        description = description,
        coverUrl = coverUrl,
        ownerId = ownerId,
        shareCode = shareCode,
        isPublic = isPublic,
        shareCount = shareCount,
        saveCount = saveCount,
        copyCount = copyCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun PlaylistSong.toEntity(playlistId: String): CachedPlaylistSongEntity = CachedPlaylistSongEntity(
        playlistId = playlistId,
        songId = songId,
        position = position,
        title = title,
        artist = artist,
        coverUrl = coverUrl,
        audioUrl = audioUrl
    )

    private fun CachedPlaylistEntity.toDomain(): Playlist = Playlist(
        id = id,
        name = name,
        description = description,
        coverUrl = coverUrl,
        ownerId = ownerId,
        songs = emptyList(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        shareCode = shareCode,
        isPublic = isPublic,
        shareCount = shareCount,
        saveCount = saveCount,
        copyCount = copyCount
    )
}
