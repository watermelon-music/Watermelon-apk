package com.watermelon.data.repository

import com.watermelon.data.local.dao.UserActionDao
import com.watermelon.data.local.entity.UserActionEntity
import com.watermelon.data.local.entity.toSong
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.UserActionsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserActionsRepositoryImpl @Inject constructor(
    private val userActionDao: UserActionDao
) : UserActionsRepository {

    override fun getRecentlyPlayed(): Flow<List<Song>> =
        userActionDao.getRecentlyPlayed().map { list -> list.map { it.toSong() } }

    override fun getFavorites(): Flow<List<Song>> =
        userActionDao.getFavorites().map { list -> list.map { it.toSong() } }

    override suspend fun addToFavorites(song: Song): Result<Unit> = runCatching {
        val entity = UserActionEntity(
            songId = song.id,
            songTitle = song.title,
            songArtist = song.artistName,
            songCoverUrl = song.coverUrl,
            actionType = "favorite"
        )
        userActionDao.insert(entity)
    }

    override suspend fun removeFromFavorites(songId: String): Result<Unit> = runCatching {
        userActionDao.removeFavorite(songId)
    }

    override suspend fun recordRecentlyPlayed(song: Song): Result<Unit> = runCatching {
        userActionDao.removeRecent(song.id)
        val entity = UserActionEntity(
            songId = song.id,
            songTitle = song.title,
            songArtist = song.artistName,
            songCoverUrl = song.coverUrl,
            actionType = "recent"
        )
        userActionDao.insert(entity)
        val count = userActionDao.countRecent()
        if (count > 50) {
            userActionDao.trimRecentTo(50)
        }
    }
}
