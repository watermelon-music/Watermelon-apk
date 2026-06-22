package com.watermelon.data.repository

import com.watermelon.data.local.dao.UserActionDao
import com.watermelon.data.local.entity.UserActionEntity
import com.watermelon.data.local.entity.toSong
import com.watermelon.data.remote.supabase.model.FavoriteRow
import com.watermelon.data.remote.supabase.model.HistoryRow
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.UserActionsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserActionsRepositoryImpl @Inject constructor(
    private val userActionDao: UserActionDao,
    private val client: SupabaseClient
) : UserActionsRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch { syncUnsynced() }
    }

    override fun getRecentlyPlayed(): Flow<List<Song>> =
        userActionDao.getRecentlyPlayed().map { list -> list.map { it.toSong() } }

    override fun getFavorites(): Flow<List<Song>> =
        userActionDao.getFavorites().map { list -> list.map { it.toSong() } }

    override suspend fun addToFavorites(song: Song): Result<Unit> = runCatching {
        val insertedId = userActionDao.insert(
            UserActionEntity(
                songId = song.id,
                songTitle = song.title,
                songArtist = song.artistName,
                songCoverUrl = song.coverUrl,
                audioUrl = song.audioUrl,
                actionType = "favorite"
            )
        )
        val userId = getUserId()
        if (userId != null) {
            runCatching {
                client.postgrest.from("favorites").upsert(
                    FavoriteRow(
                        user_id = userId,
                        song_id = song.id,
                        title = song.title,
                        artist = song.artistName,
                        cover_url = song.coverUrl,
                        audio_url = song.audioUrl
                    )
                )
            }.onSuccess {
                runCatching { userActionDao.markSynced(insertedId) }
            }.onFailure { Timber.e(it, "Supabase addToFavorites failed") }
        }
    }

    override suspend fun removeFromFavorites(songId: String): Result<Unit> = runCatching {
        userActionDao.removeFavorite(songId)
        val userId = getUserId()
        if (userId != null) {
            runCatching {
                client.postgrest.from("favorites").delete {
                    filter {
                        eq("user_id", userId)
                        eq("song_id", songId)
                    }
                }
            }.onFailure { Timber.e(it, "Supabase removeFromFavorites failed") }
        }
    }

    override suspend fun recordRecentlyPlayed(song: Song): Result<Unit> = runCatching {
        userActionDao.removeRecent(song.id)
        val entity = UserActionEntity(
            songId = song.id,
            songTitle = song.title,
            songArtist = song.artistName,
            songCoverUrl = song.coverUrl,
            audioUrl = song.audioUrl,
            actionType = "recent"
        )
        val insertedId = userActionDao.insert(entity)
        val count = userActionDao.countRecent()
        if (count > 50) userActionDao.trimRecentTo(50)

        val userId = getUserId()
        if (userId != null) {
            runCatching {
                client.postgrest.from("listening_history").insert(
                    HistoryRow(
                        user_id = userId,
                        song_id = song.id,
                        title = song.title,
                        artist = song.artistName,
                        cover_url = song.coverUrl,
                        audio_url = song.audioUrl,
                        duration_ms = song.durationMs,
                        played_at = Instant.now().toString()
                    )
                )
            }.onSuccess {
                runCatching { userActionDao.markSynced(insertedId) }
            }.onFailure { Timber.e(it, "Supabase history insert failed") }
        }
    }

    private suspend fun fetchRemoteFavorites(): List<Song> {
        val userId = getUserId() ?: return emptyList()
        val rows = client.postgrest.from("favorites")
            .select { filter { eq("user_id", userId) } }
            .decodeList<FavoriteRow>()
        return rows.map {
            Song(
                id = it.song_id,
                title = it.title,
                artistId = "",
                artistName = it.artist ?: "",
                albumId = null,
                albumName = null,
                durationMs = 0,
                coverUrl = it.cover_url,
                audioUrl = it.audio_url,
                genre = null,
                releaseDate = null
            )
        }
    }

    private suspend fun syncLocalFavorites(remote: List<Song>) {
        withContext(Dispatchers.IO) {
            userActionDao.clearFavorites()
            remote.forEach { song ->
                userActionDao.insert(
                    UserActionEntity(
                        songId = song.id,
                        songTitle = song.title,
                        songArtist = song.artistName,
                        songCoverUrl = song.coverUrl,
                        audioUrl = song.audioUrl,
                        actionType = "favorite"
                    )
                )
            }
        }
    }

    private suspend fun syncUnsynced() {
        val unsynced = runCatching { userActionDao.getUnsynced() }.getOrDefault(emptyList())
        val userId = getUserId() ?: return
        for (entity in unsynced) {
            when (entity.actionType) {
                "recent" -> syncUnsyncedRecent(entity, userId)
                "favorite" -> syncUnsyncedFavorite(entity, userId)
            }
        }
    }

    private suspend fun syncUnsyncedRecent(entity: UserActionEntity, userId: String) {
        runCatching {
            client.postgrest.from("listening_history").insert(
                HistoryRow(
                    user_id = userId,
                    song_id = entity.songId,
                    title = entity.songTitle,
                    artist = entity.songArtist,
                    cover_url = entity.songCoverUrl,
                    audio_url = entity.audioUrl,
                    duration_ms = 0,
                    played_at = Instant.ofEpochMilli(entity.timestamp).toString()
                )
            )
        }.onSuccess {
            runCatching { userActionDao.markSynced(entity.id) }
        }.onFailure { Timber.w(it, "Retry sync recent failed for ${entity.songId}") }
    }

    private suspend fun syncUnsyncedFavorite(entity: UserActionEntity, userId: String) {
        runCatching {
            client.postgrest.from("favorites").upsert(
                FavoriteRow(
                    user_id = userId,
                    song_id = entity.songId,
                    title = entity.songTitle,
                    artist = entity.songArtist,
                    cover_url = entity.songCoverUrl,
                    audio_url = entity.audioUrl
                )
            )
        }.onSuccess {
            runCatching { userActionDao.markSynced(entity.id) }
        }.onFailure { Timber.w(it, "Retry sync favorite failed for ${entity.songId}") }
    }

    private fun getUserId(): String? = client.auth.currentUserOrNull()?.id
}
