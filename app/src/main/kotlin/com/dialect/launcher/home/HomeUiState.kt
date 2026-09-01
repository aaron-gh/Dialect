package com.dialect.launcher.home

import com.dialect.launcher.appindex.AppIndexEntry
import com.dialect.launcher.matching.ScoredMatch

data class HomeUiState(
    val buffer: String = "",
    val matches: List<ScoredMatch<AppIndexEntry>> = emptyList(),
    // FR-9: most-used/most-recent apps shown (silently) when the buffer is empty.
    val emptyStateApps: List<AppIndexEntry> = emptyList(),
) {
    val topMatch: AppIndexEntry? get() = matches.firstOrNull()?.entry

    // A11Y-5/6: Enter's accessible label always names its target, or explains why it's disabled,
    // instead of a static "Enter" that gives a TalkBack user no equivalent of seeing the top row.
    val enterContentDescription: String
        get() = topMatch?.let { "Enter, opens ${it.displayName}" } ?: "Enter, no matches to open"
}
