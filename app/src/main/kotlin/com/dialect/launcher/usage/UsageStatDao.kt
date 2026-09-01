package com.dialect.launcher.usage

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface UsageStatDao {
    @Query("SELECT * FROM usage_stat")
    suspend fun getAll(): List<UsageStatEntity>

    @Upsert
    suspend fun upsert(stat: UsageStatEntity)
}
