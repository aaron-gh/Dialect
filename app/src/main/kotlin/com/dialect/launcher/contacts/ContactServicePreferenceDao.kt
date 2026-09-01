package com.dialect.launcher.contacts

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ContactServicePreferenceDao {
    @Query("SELECT * FROM contact_service_preference")
    suspend fun getAll(): List<ContactServicePreferenceEntity>

    @Upsert
    suspend fun upsert(preference: ContactServicePreferenceEntity)
}
