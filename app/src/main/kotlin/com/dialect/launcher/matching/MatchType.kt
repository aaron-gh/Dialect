package com.dialect.launcher.matching

/** Lower priority value ranks first (FR-6). Substring/"anywhere" matching (FR-5) is P2, excluded from MVP. */
enum class MatchType(val priority: Int) {
    FULL_PREFIX(0),
    WORD_INITIAL(1),
}
