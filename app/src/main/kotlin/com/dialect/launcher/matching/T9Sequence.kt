package com.dialect.launcher.matching

import java.text.Normalizer

/** Precomputes T9 digit sequences for an app's display name (FR-3, FR-4). */
object T9Sequence {

    private val combiningMarks = Regex("\\p{Mn}+")
    private val wordSplitter = Regex("[\\s\\-_]+")

    /** Case-folds, strips diacritics, and drops leading non-alphanumeric characters (emoji/symbol edge case, §10). */
    fun normalizeForIndexing(name: String): String {
        val decomposed = Normalizer.normalize(name, Normalizer.Form.NFD)
        val stripped = combiningMarks.replace(decomposed, "")
        return stripped.dropWhile { !it.isLetterOrDigit() }
    }

    /** Continuous digit sequence of the whole name, spaces/punctuation dropped (FR-3). */
    fun fullPrefixDigits(name: String): String {
        val normalized = normalizeForIndexing(name)
        return buildString {
            for (c in normalized) {
                T9Mapper.charToDigit(c)?.let { append(it) }
            }
        }
    }

    /** Compact digit sequence of each word's first mappable character (FR-4, word-initial mode). */
    fun wordInitialDigits(name: String): String {
        val normalized = normalizeForIndexing(name)
        val words = wordSplitter.split(normalized).filter { it.isNotEmpty() }
        return buildString {
            for (word in words) {
                val firstDigit = word.firstNotNullOfOrNull { T9Mapper.charToDigit(it) }
                firstDigit?.let { append(it) }
            }
        }
    }
}
