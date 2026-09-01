package com.dialect.launcher.matching

/**
 * The minimal shape [MatchEngine] needs from an app-index entry, kept free of Android framework
 * types so the matching engine is testable with plain JVM unit tests (no Robolectric/device needed).
 */
interface T9Nameable {
    val componentKey: String
    val displayName: String
    val fullPrefixDigits: String
    val wordInitialDigits: String
}
