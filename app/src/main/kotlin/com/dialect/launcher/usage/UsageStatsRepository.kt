package com.dialect.launcher.usage

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

/**
 * The whole (small) usage-stats table lives in memory. A launch updates that map synchronously,
 * so the very next keystroke's ranking reflects it with zero DB latency; the Room write is
 * fire-and-forget, keeping every-keystroke ranking entirely off DB I/O.
 */
class UsageStatsRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        UsageStatsDatabase::class.java,
        "usage_stats.db",
    ).build()
    private val dao = db.usageStatDao()
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _stats = MutableStateFlow<Map<String, UsageStat>>(emptyMap())
    val stats: StateFlow<Map<String, UsageStat>> = _stats.asStateFlow()

    init {
        repoScope.launch {
            _stats.value = dao.getAll().associate { entity ->
                entity.componentKey to UsageStat(entity.launchCount, entity.lastLaunchedAtMillis)
            }
        }
    }

    fun recordLaunch(componentKey: String) {
        val now = System.currentTimeMillis()
        val updated = _stats.updateAndGet { current ->
            val existing = current[componentKey] ?: UsageStat()
            current + (componentKey to UsageStat(existing.launchCount + 1, now))
        }
        val stat = updated.getValue(componentKey)
        repoScope.launch {
            dao.upsert(UsageStatEntity(componentKey, stat.launchCount, stat.lastLaunchedAtMillis))
        }
    }
}
