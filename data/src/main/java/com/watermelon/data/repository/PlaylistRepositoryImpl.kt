package com.watermelon.data.repository

import android.content.Context
import com.watermelon.data.BuildConfig
import com.watermelon.data.local.dao.PlaylistCacheDao
import com.watermelon.data.local.entity.CachedPlaylistEntity
import com.watermelon.data.local.entity.CachedPlaylistSongEntity
import com.watermelon.data.remote.supabase.model.PlaylistRow
import com.watermelon.data.remote.supabase.model.PlaylistSongRow
import com.watermelon.domain.model.CommunityPlaylist
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
    private val playlistCacheDao: PlaylistCacheDao,
    private val authRepository: com.watermelon.domain.repository.AuthRepository,
    private val initializer: com.watermelon.data.remote.youtube.NewPipeInitializer
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

    override suspend fun getPlaylistById(playlistId: String): Result<Playlist> = withContext(Dispatchers.IO) {
        runCatching {
            _playlists.value.firstOrNull { it.id == playlistId }?.let { return@runCatching it }

            val playlistRow = client.postgrest.from("playlists")
                .select { filter { eq("id", playlistId) } }
                .decodeList<PlaylistRow>()
                .firstOrNull() ?: throw IllegalStateException("Playlist not found")
            val songRows = client.postgrest.from("playlist_songs")
                .select {
                    filter { eq("playlist_id", playlistId) }
                    order("position", Order.ASCENDING)
                }
                .decodeList<PlaylistSongRow>()
            playlistRow.toDomain(songRows)
        }.onFailure { Timber.e(it, "getPlaylistById failed for playlistId=$playlistId") }
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

    private suspend fun isLoggedIn(): Boolean = authRepository.getCurrentUserId() != null

    private suspend fun getUserId(): String? = authRepository.getCurrentUserId()

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

    override suspend fun getCommunityPlaylists(): Result<List<CommunityPlaylist>> = withContext(Dispatchers.IO) {
        runCatching {
            val rows = client.postgrest.from("playlists")
                .select {
                    filter { eq("is_public", true) }
                    order("like_count", Order.DESCENDING)
                }
                .decodeList<PlaylistRow>()
            rows.map { it.toCommunityPlaylist() }
        }.onFailure { Timber.e(it, "getCommunityPlaylists failed") }
    }

    override suspend fun likeCommunityPlaylist(playlistId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = getUserId() ?: throw IllegalStateException("Not authenticated")
            val base = BuildConfig.WATERMELON_API_URL.removeSuffix("/")
            val url = java.net.URL("$base/api/community/playlists/$playlistId/like")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("x-user-id", userId)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connect()
            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..299) {
                kotlin.runCatching { refreshPlaylists() }
                true
            } else throw IllegalStateException("Like failed: HTTP $code")
        }.onFailure { Timber.e(it, "likeCommunityPlaylist failed") }
    }

    override suspend fun saveCommunityPlaylist(playlist: CommunityPlaylist): Result<Playlist> = withContext(Dispatchers.IO) {
        runCatching {
            val newPlaylist = createPlaylist(
                name = playlist.name,
                description = playlist.description,
                coverUrl = playlist.coverUrl
            ).getOrThrow()
            refreshPlaylists()

            if (playlist.id.startsWith("ytpl_")) {
                val url = playlist.id.removePrefix("ytpl_")
                val songs = fetchPlaylistSongs(url).getOrNull() ?: emptyList()
                songs.forEach { song ->
                    addSongToPlaylist(newPlaylist.id, song)
                }
            } else if (!playlist.id.startsWith("demo_")) {
                val songs = runCatching {
                    client.postgrest.from("playlist_songs")
                        .select {
                            filter { eq("playlist_id", playlist.id) }
                            order("position", Order.ASCENDING)
                        }
                        .decodeList<PlaylistSongRow>()
                }.getOrNull() ?: emptyList()
                songs.forEach { songRow ->
                    val song = Song(
                        id = songRow.song_id,
                        title = songRow.title,
                        artistId = songRow.artist ?: "",
                        artistName = songRow.artist ?: "",
                        albumId = null,
                        albumName = null,
                        durationMs = 0L,
                        coverUrl = songRow.cover_url,
                        audioUrl = songRow.audio_url,
                        genre = "",
                        releaseDate = ""
                    )
                    addSongToPlaylist(newPlaylist.id, song)
                }
            }
            refreshPlaylists()
            getPlaylistById(newPlaylist.id).getOrThrow()
        }.onFailure { Timber.e(it, "saveCommunityPlaylist failed") }
    }

    override suspend fun fetchPlaylistSongs(playlistUrl: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            initializer.ensureInitialized()
            val youtube = org.schabi.newpipe.extractor.ServiceList.YouTube
            val fullUrl = if (playlistUrl.startsWith("http")) playlistUrl else "https://www.youtube.com$playlistUrl"
            val info = org.schabi.newpipe.extractor.playlist.PlaylistInfo.getInfo(youtube, fullUrl)
            info.relatedItems
                .filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()
                .map { item ->
                    val fullUrl = if (item.url.startsWith("http")) item.url else "https://www.youtube.com${item.url}"
                    val videoId = when {
                        fullUrl.contains("v=") -> fullUrl.substringAfter("v=").substringBefore("&")
                        fullUrl.contains("youtu.be/") -> fullUrl.substringAfter("youtu.be/")
                        else -> item.url.hashCode().toString()
                    }
                    Song(
                        id = videoId,
                        title = item.name,
                        artistId = item.uploaderName?.hashCode()?.toString() ?: videoId,
                        artistName = item.uploaderName ?: "Unknown Artist",
                        albumId = null,
                        albumName = null,
                        durationMs = if (item.duration > 0) item.duration * 1000L else 0L,
                        coverUrl = "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg",
                        audioUrl = fullUrl,
                        genre = "",
                        releaseDate = ""
                    )
                }
        }.onFailure { Timber.e(it, "fetchPlaylistSongs failed") }
    }

    override suspend fun searchPlaylists(query: String): Result<List<CommunityPlaylist>> = withContext(Dispatchers.IO) {
        runCatching {
            initializer.ensureInitialized()
            val youtube = org.schabi.newpipe.extractor.ServiceList.YouTube
            val queryHandler = youtube.getSearchQHFactory().fromQuery(
                query, listOf("playlists"), ""
            )
            val extractor = youtube.getSearchExtractor(queryHandler)
            extractor.fetchPage()
            extractor.initialPage.items
                .filterIsInstance<org.schabi.newpipe.extractor.playlist.PlaylistInfoItem>()
                .take(15)
                .map { item ->
                    val thumbnailUrl = item.thumbnails?.firstOrNull()?.url ?: ""
                    CommunityPlaylist(
                        id = "ytpl_${item.url}",
                        name = item.name,
                        description = null,
                        coverUrl = thumbnailUrl,
                        ownerId = item.uploaderUrl ?: "",
                        creatorDisplayName = item.uploaderName ?: "YouTube",
                        tags = emptyList(),
                        likeCount = 0,
                        songCount = item.streamCount.takeIf { it > 0 }?.toInt() ?: 0,
                        isPublic = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                }
        }.onFailure { Timber.e(it, "searchPlaylists failed") }
    }

    private fun PlaylistRow.toCommunityPlaylist(): CommunityPlaylist = CommunityPlaylist(
        id = id,
        name = name,
        description = description,
        coverUrl = cover_url,
        ownerId = user_id,
        creatorDisplayName = creator_display_name ?: "",
        tags = tags ?: emptyList(),
        likeCount = like_count,
        songCount = 0,
        isPublic = is_public,
        createdAt = kotlin.runCatching { java.time.Instant.parse(created_at ?: "").toEpochMilli() }.getOrDefault(System.currentTimeMillis()),
        updatedAt = kotlin.runCatching { java.time.Instant.parse(updated_at ?: "").toEpochMilli() }.getOrDefault(System.currentTimeMillis())
    )
}
