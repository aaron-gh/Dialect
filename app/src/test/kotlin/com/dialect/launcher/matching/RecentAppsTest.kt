package com.dialect.launcher.matching

import com.dialect.launcher.usage.UsageStat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentAppsTest {

    private val discord = TestApp("discord", "Discord")
    private val maps = TestApp("maps", "Maps")
    private val email = TestApp("email", "Email")
    private val clock = TestApp("clock", "Clock")
    private val neverLaunched = TestApp("never", "Never Opened")
    private val apps = listOf(discord, maps, email, clock, neverLaunched)

    @Test
    fun `orders by last launched time, newest first`() {
        val stats = mapOf(
            "discord" to UsageStat(launchCount = 1, lastLaunchedAtMillis = 100),
            "maps" to UsageStat(launchCount = 1, lastLaunchedAtMillis = 300),
            "email" to UsageStat(launchCount = 1, lastLaunchedAtMillis = 200),
        )
        val result = mostRecentlyLaunched(apps, stats, limit = 4)
        assertEquals(listOf("Maps", "Email", "Discord"), result.map { it.displayName })
    }

    @Test
    fun `a high launch count from long ago does not outrank a recent launch`() {
        // The reported bug: apps opened many times right after install stayed on top forever.
        val stats = mapOf(
            "discord" to UsageStat(launchCount = 50, lastLaunchedAtMillis = 100),
            "maps" to UsageStat(launchCount = 40, lastLaunchedAtMillis = 200),
            "clock" to UsageStat(launchCount = 1, lastLaunchedAtMillis = 900),
        )
        val result = mostRecentlyLaunched(apps, stats, limit = 2)
        assertEquals(listOf("Clock", "Maps"), result.map { it.displayName })
    }

    @Test
    fun `apps without usage are excluded and the limit is respected`() {
        assertTrue(mostRecentlyLaunched(apps, emptyMap(), limit = 4).isEmpty())

        val stats = mapOf(
            "discord" to UsageStat(1, 1),
            "maps" to UsageStat(1, 2),
            "email" to UsageStat(1, 3),
        )
        assertEquals(2, mostRecentlyLaunched(apps, stats, limit = 2).size)
    }
}
