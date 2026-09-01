package com.dialect.launcher.contacts

import androidx.room.Database
import androidx.room.RoomDatabase

// version 2: added the "kind"/nullable "serviceId" columns for the AskEveryTime tri-state.
@Database(entities = [ContactServicePreferenceEntity::class], version = 2, exportSchema = false)
abstract class ContactPreferenceDatabase : RoomDatabase() {
    abstract fun contactServicePreferenceDao(): ContactServicePreferenceDao
}
