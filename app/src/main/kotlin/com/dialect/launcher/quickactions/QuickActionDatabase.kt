package com.dialect.launcher.quickactions

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [QuickActionEntity::class], version = 1, exportSchema = false)
abstract class QuickActionDatabase : RoomDatabase() {
    abstract fun quickActionDao(): QuickActionDao
}
