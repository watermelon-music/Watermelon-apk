package com.watermelon.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.watermelon.domain.model.Song

@Entity(tableName = "cached_songs")
data class CachedSongEntity(
    @PrimaryKey(autoGenerate = true) val cacheId: Long = 0,
    val id: String,
    val title: String,
    val artistId: String,
    val artistName: String,
    val albumId: String?,
    val albumName: String?,
    val durationMs: Long,
    val coverUrl: String?,
    val audioUrl: String?,
    val genre: String?,
    val releaseDate: String?,
    val cacheType: String, // "trending", "search"
    @ColumnInfo(index = true) val searchQuery: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

fun CachedSongEntity.toSong(): Song {
    val mappedCoverUrl = coverUrl?.let { url ->
        if (url.contains("i.ytimg.com/vi/")) {
            val viIndex = url.indexOf("/vi/")
            if (viIndex != -1) {
                val afterVi = url.substring(viIndex + 4)
                val firstSlash = afterVi.indexOf('/')
                if (firstSlash != -1) {
                    val videoId = afterVi.substring(0, firstSlash)
                    "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
                } else {
                    url
                }
            } else {
                url
            }
        } else {
            url
        }
    }
    return Song(
        id = id,
        title = title,
        artistId = artistId,
        artistName = artistName,
        albumId = albumId,
        albumName = albumName,
        durationMs = durationMs,
        coverUrl = mappedCoverUrl,
        audioUrl = audioUrl,
        genre = genre,
        releaseDate = releaseDate
    )
}

fun Song.toCachedEntity(cacheType: String, searchQuery: String? = null): CachedSongEntity = CachedSongEntity(
    id = id,
    title = title,
    artistId = artistId,
    artistName = artistName,
    albumId = albumId,
    albumName = albumName,
    durationMs = durationMs,
    coverUrl = coverUrl,
    audioUrl = audioUrl,
    genre = genre,
    releaseDate = releaseDate,
    cacheType = cacheType,
    searchQuery = searchQuery
)
