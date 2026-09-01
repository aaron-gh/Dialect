package com.dialect.launcher.usage

/** Per-app launch stats used to rank matches (FR-6). Plain data, no Room dependency, so it's usable from unit tests. */
data class UsageStat(
    val launchCount: Int = 0,
    val lastLaunchedAtMillis: Long = 0L,
)
