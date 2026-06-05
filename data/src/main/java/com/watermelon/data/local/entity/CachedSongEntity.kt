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

fun CachedSongEntity.toSong(): Song = Song(
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
    releaseDate = releaseDate
)

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
