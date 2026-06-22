package com.watermelon.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.watermelon.data.local.dao.DownloadDao
import com.watermelon.data.local.entity.DownloadedSongEntity
import com.watermelon.domain.model.DownloadedSong
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

object PublicStorageHelper {
    const val PUBLIC_FOLDER = "watermelon"

    fun getRelativePath(): String = "${Environment.DIRECTORY_MUSIC}/$PUBLIC_FOLDER/"

    suspend fun saveToPublicStorage(
        context: Context,
        song: Song,
        mp3File: File
    ): String? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, song.title)
                put(MediaStore.Audio.Media.ARTIST, song.artistName)
                put(MediaStore.Audio.Media.RELATIVE_PATH, getRelativePath())
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.DISPLAY_NAME, "${song.id}.mp3")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext null

            context.contentResolver.openOutputStream(uri)?.use { output ->
                mp3File.inputStream().use { input -> input.copyTo(output) }
            } ?: return@withContext null

            contentValues.clear()
            contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)
            uri.toString()
        } else {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val watermelonDir = File(musicDir, PUBLIC_FOLDER)
            if (!watermelonDir.exists()) watermelonDir.mkdirs()
            val dest = File(watermelonDir, "${song.id}.mp3")
            mp3File.inputStream().use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest.absolutePath
        }
    }
}

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
            if (existing != null) {
                val oldPath = existing.localFilePath
                val exists = if (oldPath.startsWith("content://")) {
                    try {
                        context.contentResolver.query(
                            Uri.parse(oldPath),
                            arrayOf(MediaStore.Audio.Media._ID),
                            null, null, null
                        )?.use { it.moveToFirst() } ?: false
                    } catch (_: Exception) { false }
                } else {
                    File(oldPath).exists()
                }
                if (exists) return@runCatching // Already downloaded and file exists
            }

            val musicDir = File(context.filesDir, "downloads")
            if (!musicDir.exists()) musicDir.mkdirs()

            val file = File(musicDir, "${song.id}.mp3")

            // Remove stale local file if re-downloading
            file.delete()
            existing?.let {
                val oldPath = it.localFilePath
                if (oldPath.startsWith("content://")) {
                    try { context.contentResolver.delete(Uri.parse(oldPath), null, null) } catch (_: Exception) {}
                } else {
                    File(oldPath).delete()
                }
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36")
                .build()
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

            val publicUriOrPath = file.absolutePath

            downloadDao.insert(
                DownloadedSongEntity(
                    songId = song.id,
                    title = song.title,
                    artist = song.artistName,
                    coverUrl = song.coverUrl,
                    localFilePath = publicUriOrPath,
                    fileSize = fileSize,
                    downloadedAt = System.currentTimeMillis()
                )
            )
        }
    }.onFailure { Timber.e(it, "downloadSong failed for ${song.id}") }

    override suspend fun recordDownload(song: Song, filePath: String, fileSize: Long): Result<Unit> = runCatching {
        downloadMutex.withLock {
            val existing = downloadDao.getById(song.id)
            existing?.let {
                val oldPath = it.localFilePath
                if (oldPath.startsWith("content://")) {
                    try { context.contentResolver.delete(Uri.parse(oldPath), null, null) } catch (_: Exception) {}
                } else {
                    File(oldPath).delete()
                }
            }
            downloadDao.insert(
                DownloadedSongEntity(
                    songId = song.id,
                    title = song.title,
                    artist = song.artistName,
                    coverUrl = song.coverUrl,
                    localFilePath = filePath,
                    fileSize = fileSize,
                    downloadedAt = System.currentTimeMillis()
                )
            )
        }
    }.onFailure { Timber.e(it, "recordDownload failed for ${song.id}") }

    override suspend fun deleteDownload(songId: String): Result<Unit> = runCatching {
        val entity = downloadDao.getById(songId)
        if (entity != null) {
            val path = entity.localFilePath
            if (path.startsWith("content://")) {
                try { context.contentResolver.delete(Uri.parse(path), null, null) } catch (_: Exception) {}
            } else {
                File(path).delete()
            }
            downloadDao.deleteById(songId)
        }
    }.onFailure { Timber.e(it, "deleteDownload failed for $songId") }

    override suspend fun cleanupMissingFiles(): Result<Unit> = runCatching {
        val all = downloadDao.getAll().first()
        for (entity in all) {
            val path = entity.localFilePath
            val exists = if (path.startsWith("content://")) {
                try {
                    context.contentResolver.query(
                        Uri.parse(path),
                        arrayOf(MediaStore.Audio.Media._ID),
                        null, null, null
                    )?.use { it.moveToFirst() } ?: false
                } catch (_: Exception) { false }
            } else {
                File(path).exists()
            }
            if (!exists) {
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
