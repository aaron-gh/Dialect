package com.dialect.launcher.matching

import com.dialect.launcher.usage.UsageStat
import org.junit.Assert.assertEquals
import org.junit.Test

class RankingComparatorTest {

    private val appA = TestApp("a", "Zebra")
    private val appB = TestApp("b", "Apple")

    @Test
    fun `full prefix match ranks above word initial match`() {
        val comparator = RankingComparator<TestApp>(emptyMap())
        val prefixMatch = ScoredMatch(appA, MatchType.FULL_PREFIX)
        val wordInitialMatch = ScoredMatch(appB, MatchType.WORD_INITIAL)
        assertEquals(-1, comparator.compare(prefixMatch, wordInitialMatch).coerceIn(-1, 1))
    }

    @Test
    fun `higher launch count ranks first`() {
        val stats = mapOf("a" to UsageStat(launchCount = 5), "b" to UsageStat(launchCount = 1))
        val comparator = RankingComparator<TestApp>(stats)
        val matchA = ScoredMatch(appA, MatchType.FULL_PREFIX)
        val matchB = ScoredMatch(appB, MatchType.FULL_PREFIX)
        assertEquals(true, comparator.compare(matchA, matchB) < 0)
    }

    @Test
    fun `more recent last launch ranks first when counts are equal`() {
        val stats = mapOf(
            "a" to UsageStat(launchCount = 3, lastLaunchedAtMillis = 200L),
            "b" to UsageStat(launchCount = 3, lastLaunchedAtMillis = 100L),
        )
        val comparator = RankingComparator<TestApp>(stats)
        val matchA = ScoredMatch(appA, MatchType.FULL_PREFIX)
        val matchB = ScoredMatch(appB, MatchType.FULL_PREFIX)
        assertEquals(true, comparator.compare(matchA, matchB) < 0)
    }

    @Test
    fun `identical stats fall back to deterministic alphabetical order`() {
        // §10 edge case: identical digit sequences and identical usage stats must not be random.
        val comparator = RankingComparator<TestApp>(emptyMap())
        val matchA = ScoredMatch(appA, MatchType.FULL_PREFIX) // "Zebra"
        val matchB = ScoredMatch(appB, MatchType.FULL_PREFIX) // "Apple"
        assertEquals(true, comparator.compare(matchA, matchB) > 0)
        assertEquals(true, comparator.compare(matchB, matchA) < 0)
    }
}
