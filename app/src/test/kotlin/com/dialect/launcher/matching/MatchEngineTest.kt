package com.dialect.launcher.matching

import com.dialect.launcher.usage.UsageStat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchEngineTest {

    private val discord = TestApp("discord", "Discord")
    private val facebook = TestApp("facebook", "Facebook")
    private val email = TestApp("email", "Email")
    private val obtainium = TestApp("obtainium", "Obtainium")
    private val maps = TestApp("maps", "Maps")
    private val googleMaps = TestApp("googlemaps", "Google Maps")
    private val amazonPrime = TestApp("amazonprime", "Amazon Prime")
    private val index = listOf(discord, facebook, email, obtainium, maps, googleMaps, amazonPrime)

    @Test
    fun `empty buffer yields no matches`() {
        val result = MatchEngine.filterAndRank("", index, emptyMap(), wordInitialModeEnabled = false)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `typing 3 matches all apps starting with D E or F`() {
        // §14: "3" -> Discord, Facebook, Email...
        val result = MatchEngine.filterAndRank("3", index, emptyMap(), wordInitialModeEnabled = false)
        val names = result.map { it.entry.displayName }
        assertTrue(names.containsAll(listOf("Discord", "Facebook", "Email")))
    }

    @Test
    fun `typing 3-4 narrows to Discord only`() {
        // §14: "34" -> Discord (D, I).
        val result = MatchEngine.filterAndRank("34", index, emptyMap(), wordInitialModeEnabled = false)
        assertEquals(listOf("Discord"), result.map { it.entry.displayName })
    }

    @Test
    fun `progressive prefixes of Obtainium each independently match it`() {
        // §1.1: 6, 62, 628, 6282 should all keep Obtainium as a live candidate.
        for (prefix in listOf("6", "62", "628", "6282")) {
            val result = MatchEngine.filterAndRank(prefix, index, emptyMap(), wordInitialModeEnabled = false)
            assertTrue(
                "prefix '$prefix' should match Obtainium",
                result.any { it.entry.displayName == "Obtainium" },
            )
        }
    }

    @Test
    fun `word initial mode matches Google Maps on first letters of each word`() {
        // §14: "4-6" (G, M) matches Google Maps via word-initial digits.
        val result = MatchEngine.filterAndRank("46", index, emptyMap(), wordInitialModeEnabled = true)
        assertTrue(result.any { it.entry.displayName == "Google Maps" })
    }

    @Test
    fun `word initial mode is off by default and does not match on word-initial-only sequences`() {
        // FR-4: word-initial mode ships off by default. "Amazon Prime" (A, P -> 27) only matches
        // full-name-prefix ("26...", from "Am"), so with the mode off, "27" must not match it.
        val offResult = MatchEngine.filterAndRank("27", index, emptyMap(), wordInitialModeEnabled = false)
        assertTrue(offResult.none { it.entry.displayName == "Amazon Prime" })

        val onResult = MatchEngine.filterAndRank("27", index, emptyMap(), wordInitialModeEnabled = true)
        assertTrue(onResult.any { it.entry.displayName == "Amazon Prime" })
    }

    @Test
    fun `no matches for a digit sequence no app maps to`() {
        val result = MatchEngine.filterAndRank("00000000", index, emptyMap(), wordInitialModeEnabled = false)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `higher usage count ranks first among equal match types`() {
        val stats = mapOf(facebook.componentKey to UsageStat(launchCount = 10))
        val result = MatchEngine.filterAndRank("3", index, stats, wordInitialModeEnabled = false)
        assertEquals("Facebook", result.first().entry.displayName)
    }

    @Test
    fun `identical stats fall back to deterministic alphabetical order`() {
        val result1 = MatchEngine.filterAndRank("3", index, emptyMap(), wordInitialModeEnabled = false)
        val result2 = MatchEngine.filterAndRank("3", index, emptyMap(), wordInitialModeEnabled = false)
        assertEquals(result1.map { it.entry.displayName }, result2.map { it.entry.displayName })
        assertEquals(listOf("Discord", "Email", "Facebook"), result1.map { it.entry.displayName })
    }
}
