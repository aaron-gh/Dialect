package com.dialect.launcher.matching

/** Android-framework-free stand-in for AppIndexEntry, so matching logic is testable on the plain JVM. */
data class TestApp(
    override val componentKey: String,
    override val displayName: String,
    override val fullPrefixDigits: String = T9Sequence.fullPrefixDigits(displayName),
    override val wordInitialDigits: String = T9Sequence.wordInitialDigits(displayName),
) : T9Nameable
