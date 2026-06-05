package com.watermelon.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.watermelon.data.local.dao.CachedSongDao
import com.watermelon.data.local.dao.UserActionDao
import com.watermelon.data.local.entity.CachedSongEntity
import com.watermelon.data.local.entity.UserActionEntity

@Database(
    entities = [CachedSongEntity::class, UserActionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedSongDao(): CachedSongDao
    abstract fun userActionDao(): UserActionDao
}
