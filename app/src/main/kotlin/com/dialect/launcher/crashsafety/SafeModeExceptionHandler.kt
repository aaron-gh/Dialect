package com.dialect.launcher.crashsafety

import android.content.Context
import android.content.SharedPreferences

/**
 * NFR-4: as a HOME app, a crash here can strand the user with no way to reach any other app, so
 * this is a much higher bar than for an ordinary app. Tracks recent crashes in plain
 * SharedPreferences (not Room/DataStore, so this path survives even if those subsystems are what's
 * failing) and flags Safe Mode when crashes repeat in a short window, rather than crash-looping.
 */
object SafeModeExceptionHandler {
    private const val PREFS_NAME = "dialect_crash_safety"
    private const val KEY_CRASH_COUNT = "crash_count"
    private const val KEY_WINDOW_START = "crash_window_start"
    private const val WINDOW_MILLIS = 60_000L
    private const val CRASH_THRESHOLD = 2

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            recordCrash(appContext)
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    fun shouldEnterSafeMode(context: Context): Boolean {
        return try {
            val prefs = prefs(context)
            val now = System.currentTimeMillis()
            val windowStart = prefs.getLong(KEY_WINDOW_START, 0L)
            if (now - windowStart >= WINDOW_MILLIS) return false
            prefs.getInt(KEY_CRASH_COUNT, 0) >= CRASH_THRESHOLD
        } catch (e: Exception) {
            false
        }
    }

    fun resetAfterStableForeground(context: Context) {
        try {
            prefs(context).edit().clear().apply()
        } catch (e: Exception) {
            // No-op: a failure here just means the counter persists a bit longer than intended.
        }
    }

    private fun recordCrash(context: Context) {
        try {
            val prefs = prefs(context)
            val now = System.currentTimeMillis()
            val windowStart = prefs.getLong(KEY_WINDOW_START, 0L)
            val withinWindow = now - windowStart < WINDOW_MILLIS
            val count = if (withinWindow) prefs.getInt(KEY_CRASH_COUNT, 0) + 1 else 1
            // commit() (synchronous), not apply(): the process is about to die, so the write must
            // be durable before that happens.
            prefs.edit()
                .putInt(KEY_CRASH_COUNT, count)
                .putLong(KEY_WINDOW_START, if (withinWindow) windowStart else now)
                .commit()
        } catch (e: Exception) {
            // No-op: recording the crash must never itself throw during crash handling.
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
