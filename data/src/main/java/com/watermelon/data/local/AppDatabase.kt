package com.watermelon.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.watermelon.data.local.dao.CachedSongDao
import com.watermelon.data.local.dao.DownloadDao
import com.watermelon.data.local.dao.PlaylistCacheDao
import com.watermelon.data.local.dao.RadioStationDao
import com.watermelon.data.local.dao.UserActionDao
import com.watermelon.data.local.entity.CachedPlaylistEntity
import com.watermelon.data.local.entity.CachedPlaylistSongEntity
import com.watermelon.data.local.entity.CachedSongEntity
import com.watermelon.data.local.entity.DownloadedSongEntity
import com.watermelon.data.local.entity.RadioStationEntity
import com.watermelon.data.local.entity.UserActionEntity

@Database(
    entities = [CachedSongEntity::class, UserActionEntity::class, RadioStationEntity::class, DownloadedSongEntity::class, CachedPlaylistEntity::class, CachedPlaylistSongEntity::class],
    version = 5,
    autoMigrations = [AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5)],
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedSongDao(): CachedSongDao
    abstract fun userActionDao(): UserActionDao
    abstract fun radioStationDao(): RadioStationDao
    abstract fun downloadDao(): DownloadDao
    abstract fun playlistCacheDao(): PlaylistCacheDao
}
