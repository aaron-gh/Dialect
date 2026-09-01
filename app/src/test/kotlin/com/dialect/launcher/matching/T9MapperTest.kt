package com.dialect.launcher.matching

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class T9MapperTest {

    @Test
    fun `letters map to standard phone-dialpad digits`() {
        assertEquals('2', T9Mapper.charToDigit('A'))
        assertEquals('2', T9Mapper.charToDigit('C'))
        assertEquals('7', T9Mapper.charToDigit('P'))
        assertEquals('7', T9Mapper.charToDigit('S'))
        assertEquals('9', T9Mapper.charToDigit('Z'))
    }

    @Test
    fun `mapping is case-insensitive`() {
        assertEquals(T9Mapper.charToDigit('d'), T9Mapper.charToDigit('D'))
    }

    @Test
    fun `digits pass through literally, not through letter mapping`() {
        // FR-2: "1Password", "7-Zip", "9GAG" must match their literal digit keys.
        assertEquals('1', T9Mapper.charToDigit('1'))
        assertEquals('7', T9Mapper.charToDigit('7'))
        assertEquals('9', T9Mapper.charToDigit('9'))
        assertEquals('0', T9Mapper.charToDigit('0'))
    }

    @Test
    fun `1 and 0 have no letters mapped to them`() {
        // FR-1: digit 1 and 0 are reserved buckets with no letters.
        for (c in 'A'..'Z') {
            val digit = T9Mapper.charToDigit(c)
            assertEquals(false, digit == '1')
            assertEquals(false, digit == '0')
        }
    }

    @Test
    fun `unmappable characters return null`() {
        assertNull(T9Mapper.charToDigit(' '))
        assertNull(T9Mapper.charToDigit('-'))
        assertNull(T9Mapper.charToDigit('!'))
    }
}
