package com.dialect.launcher.matching

/** Standard phone-dialpad letter-to-digit mapping (PRD FR-1), extended to other scripts (1.2.0). */
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

        // Latin letters that don't NFD-decompose to a base letter + combining mark (so
        // T9Sequence's diacritic stripping never reduces them), placed by the same
        // "same key as the letter it sounds/alphabetizes like" convention real keypads use.
        // Both case forms of ß are listed explicitly since Char.uppercaseChar()'s handling of
        // it (single ẞ vs unchanged) is JVM-version-dependent.
        put('ß', '7') // German sharp s -> same key as S
        put('ẞ', '7')
        put('Œ', '6') // French ligature -> same key as O

        // Cyrillic (Russian): ETSI ES 202 130 / standard Russian mobile keypad, alphabetical order.
        "АБВГ".forEach { put(it, '2') }
        "ДЕЁЖЗ".forEach { put(it, '3') }
        "ИЙКЛ".forEach { put(it, '4') }
        "МНОП".forEach { put(it, '5') }
        "РСТУ".forEach { put(it, '6') }
        "ФХЦЧ".forEach { put(it, '7') }
        "ШЩЪЫ".forEach { put(it, '8') }
        "ЬЭЮЯ".forEach { put(it, '9') }

        // Greek: alphabetical order, 3 letters per key (24 letters / 8 keys divides evenly).
        "ΑΒΓ".forEach { put(it, '2') }
        "ΔΕΖ".forEach { put(it, '3') }
        "ΗΘΙ".forEach { put(it, '4') }
        "ΚΛΜ".forEach { put(it, '5') }
        "ΝΞΟ".forEach { put(it, '6') }
        "ΠΡΣ".forEach { put(it, '7') }
        "ΤΥΦ".forEach { put(it, '8') }
        "ΧΨΩ".forEach { put(it, '9') }
    }

    /** Returns the digit a character maps to, or null if it doesn't contribute a digit (FR-2, FR-3). */
    fun charToDigit(c: Char): Char? {
        val upper = c.uppercaseChar()
        if (upper in '0'..'9') return upper
        return letterToDigit[upper]
    }
}
