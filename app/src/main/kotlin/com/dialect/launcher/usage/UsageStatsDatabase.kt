package com.dialect.launcher.usage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UsageStatEntity::class], version = 1, exportSchema = false)
abstract class UsageStatsDatabase : RoomDatabase() {
    abstract fun usageStatDao(): UsageStatDao
}
