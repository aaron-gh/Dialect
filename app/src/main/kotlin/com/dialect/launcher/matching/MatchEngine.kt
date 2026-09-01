package com.dialect.launcher.matching

import com.dialect.launcher.usage.UsageStat

data class ScoredMatch<T : T9Nameable>(val entry: T, val matchType: MatchType)

/**
 * Pure, synchronous filter + rank over precomputed digit sequences (FR-3, FR-6, FR-7).
 * At 150-250 apps a linear scan comfortably clears the <16ms target (NFR-1); no trie needed for MVP.
 */
object MatchEngine {
    fun <T : T9Nameable> filterAndRank(
        buffer: String,
        index: List<T>,
        usageStats: Map<String, UsageStat>,
        wordInitialModeEnabled: Boolean,
    ): List<ScoredMatch<T>> {
        if (buffer.isEmpty()) return emptyList()

        // FR-4: a typed '0' is a word-boundary hint, ignorable when comparing against the compact
        // word-initial sequence. In full-prefix mode '0' only matches a literal '0' in the name.
        val wordInitialBuffer = buffer.replace("0", "")

        val matches = mutableListOf<ScoredMatch<T>>()
        for (entry in index) {
            if (entry.fullPrefixDigits.startsWith(buffer)) {
                matches += ScoredMatch(entry, MatchType.FULL_PREFIX)
            } else if (wordInitialModeEnabled && wordInitialBuffer.isNotEmpty() &&
                entry.wordInitialDigits.startsWith(wordInitialBuffer)
            ) {
                matches += ScoredMatch(entry, MatchType.WORD_INITIAL)
            }
        }

        return matches.sortedWith(RankingComparator(usageStats))
    }
}
