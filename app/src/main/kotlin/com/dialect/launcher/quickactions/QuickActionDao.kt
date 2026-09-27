package com.dialect.launcher.quickactions

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface QuickActionDao {
    @Query("SELECT * FROM quick_action")
    suspend fun getAll(): List<QuickActionEntity>

    @Upsert
    suspend fun upsert(action: QuickActionEntity)

    @Query("DELETE FROM quick_action WHERE digit = :digit")
    suspend fun deleteByDigit(digit: Int)
}
