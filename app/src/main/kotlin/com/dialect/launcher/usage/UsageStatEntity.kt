package com.dialect.launcher.usage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_stat")
data class UsageStatEntity(
    @PrimaryKey val componentKey: String,
    val launchCount: Int,
    val lastLaunchedAtMillis: Long,
)
