package com.dialect.launcher.matching

import com.dialect.launcher.usage.UsageStat

/**
 * FR-9: the empty-buffer default list. Strictly most-recently-launched first — launch count must not
 * factor in, or the apps opened first after install accumulate counts and never leave the list.
 * Only entries with recorded usage qualify; a fresh install has none.
 */
fun <T : T9Nameable> mostRecentlyLaunched(
    items: List<T>,
    usageStats: Map<String, UsageStat>,
    limit: Int,
): List<T> {
    return items
        .filter { usageStats.containsKey(it.componentKey) }
        .sortedWith(
            compareByDescending<T> { usageStats.getValue(it.componentKey).lastLaunchedAtMillis }
                .thenBy { it.displayName.lowercase() },
        )
        .take(limit)
}
