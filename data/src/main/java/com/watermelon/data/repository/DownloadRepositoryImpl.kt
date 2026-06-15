package com.watermelon.data.repository

import android.content.Context
import android.os.Environment
import com.watermelon.data.local.dao.DownloadDao
import com.watermelon.data.local.entity.DownloadedSongEntity
import com.watermelon.domain.model.DownloadedSong
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao,
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context
) : DownloadRepository {

    private val downloadMutex = Mutex()

    override fun getDownloads(): Flow<List<DownloadedSong>> {
        return downloadDao.getAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun downloadSong(song: Song, url: String): Result<Unit> = runCatching {
        downloadMutex.withLock {
            val existing = downloadDao.getById(song.id)
            if (existing != null && File(existing.localFilePath).exists()) {
                return@runCatching // Already downloaded and file exists
            }

            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: throw IllegalStateException("Cannot access music directory")
            if (!musicDir.exists()) {
                musicDir.mkdirs()
            }

            val file = File(musicDir, "${song.id}.mp3")

            // Remove stale file if re-downloading
            existing?.let { File(it.localFilePath).delete() }

            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                response.close()
                throw IOException("Download failed: HTTP ${response.code}")
            }

            val body = response.body
                ?: run {
                    response.close()
                    throw IOException("Empty response body")
                }

            body.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            val fileSize = file.length()

            downloadDao.insert(
                DownloadedSongEntity(
                    songId = song.id,
                    title = song.title,
                    artist = song.artistName,
                    coverUrl = song.coverUrl,
                    localFilePath = file.absolutePath,
                    fileSize = fileSize,
                    downloadedAt = System.currentTimeMillis()
                )
            )
        }
    }.onFailure { Timber.e(it, "downloadSong failed for ${song.id}") }

    override suspend fun deleteDownload(songId: String): Result<Unit> = runCatching {
        val entity = downloadDao.getById(songId)
        if (entity != null) {
            File(entity.localFilePath).delete()
            downloadDao.deleteById(songId)
        }
    }.onFailure { Timber.e(it, "deleteDownload failed for $songId") }

    override suspend fun cleanupMissingFiles(): Result<Unit> = runCatching {
        val all = downloadDao.getAll().first()
        for (entity in all) {
            if (!File(entity.localFilePath).exists()) {
                downloadDao.deleteById(entity.songId)
            }
        }
    }.onFailure { Timber.e(it, "cleanupMissingFiles failed") }

    override suspend fun isDownloaded(songId: String): Boolean {
        return downloadDao.exists(songId)
    }

    override suspend fun getDownloadPath(songId: String): String? {
        return downloadDao.getById(songId)?.localFilePath
    }
}

private fun DownloadedSongEntity.toDomain(): DownloadedSong {
    return DownloadedSong(
        songId = songId,
        title = title,
        artist = artist,
        coverUrl = coverUrl,
        localFilePath = localFilePath,
        fileSize = fileSize,
        downloadedAt = downloadedAt
    )
}
