package com.dialect.launcher.matching

/** Standard phone-dialpad letter-to-digit mapping (PRD FR-1). */
object T9Mapper {
    private val letterToDigit: Map<Char, Char> = buildMap {
        "ABC".forEach { put(it, '2') }
        "DEF".forEach { put(it, '3') }
        "GHI".forEach { put(it, '4') }
        "JKL".forEach { put(it, '5') }
        "MNO".forEach { put(it, '6') }
        "PQRS".forEach { put(it, '7') }
        "TUV".forEach { put(it, '8') }
        "WXYZ".forEach { put(it, '9') }
    }

    /** Returns the digit a character maps to, or null if it doesn't contribute a digit (FR-2, FR-3). */
    fun charToDigit(c: Char): Char? {
        val upper = c.uppercaseChar()
        if (upper in '0'..'9') return upper
        return letterToDigit[upper]
    }
}
