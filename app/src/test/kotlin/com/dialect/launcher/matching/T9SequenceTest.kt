package com.dialect.launcher.matching

import org.junit.Assert.assertEquals
import org.junit.Test

class T9SequenceTest {

    @Test
    fun `full prefix digits for Discord match PRD worked example`() {
        // §14: typing 3-4 (D, I) matches Discord.
        assertEquals("3472673", T9Sequence.fullPrefixDigits("Discord"))
    }

    @Test
    fun `full prefix digits for Obtainium match PRD worked example`() {
        // §1.1 / §14: typing 6-2-8-2 (O, B, T, A) matches Obtainium.
        assertEquals("628246486", T9Sequence.fullPrefixDigits("Obtainium"))
    }

    @Test
    fun `word initial digits for Google Maps match PRD worked example`() {
        // §14: typing 4-6 (G, M) matches Google Maps in word-initial mode.
        assertEquals("46", T9Sequence.wordInitialDigits("Google Maps"))
    }

    @Test
    fun `diacritics are stripped before mapping`() {
        // Cafe behaves like Cafe (FR-3).
        assertEquals(T9Sequence.fullPrefixDigits("Cafe"), T9Sequence.fullPrefixDigits("Café"))
    }

    @Test
    fun `literal digits in a name pass through unchanged`() {
        // "1Password" (FR-2) - leading '1' matches key 1 literally, not a letter mapping.
        assertEquals("172779673", T9Sequence.fullPrefixDigits("1Password"))
    }

    @Test
    fun `leading emoji or symbols are stripped so the app stays reachable`() {
        // §10 edge case: app name starting with an emoji must not become unreachable.
        assertEquals(T9Sequence.fullPrefixDigits("Steam"), T9Sequence.fullPrefixDigits("🎮 Steam"))
    }

    @Test
    fun `word initial digits split on space hyphen and underscore`() {
        assertEquals(T9Sequence.wordInitialDigits("Foo Bar"), T9Sequence.wordInitialDigits("Foo-Bar"))
        assertEquals(T9Sequence.wordInitialDigits("Foo Bar"), T9Sequence.wordInitialDigits("Foo_Bar"))
    }
}
