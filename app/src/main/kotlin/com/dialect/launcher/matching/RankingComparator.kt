package com.dialect.launcher.matching

import com.dialect.launcher.usage.UsageStat

/**
 * Deterministic ranking (FR-6): match type, then usage count, then recency, then alphabetical.
 * Never falls back to unordered iteration, so identical inputs always produce the same top match (§10).
 */
class RankingComparator<T : T9Nameable>(
    private val usageStats: Map<String, UsageStat>,
) : Comparator<ScoredMatch<T>> {
    override fun compare(a: ScoredMatch<T>, b: ScoredMatch<T>): Int {
        val typeCompare = a.matchType.priority.compareTo(b.matchType.priority)
        if (typeCompare != 0) return typeCompare

        val statA = usageStats[a.entry.componentKey] ?: UsageStat()
        val statB = usageStats[b.entry.componentKey] ?: UsageStat()

        val countCompare = statB.launchCount.compareTo(statA.launchCount)
        if (countCompare != 0) return countCompare

        val recencyCompare = statB.lastLaunchedAtMillis.compareTo(statA.lastLaunchedAtMillis)
        if (recencyCompare != 0) return recencyCompare

        return a.entry.displayName.compareTo(b.entry.displayName, ignoreCase = true)
    }
}
