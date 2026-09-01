package com.dialect.launcher.home

import com.dialect.launcher.contacts.ContactActionType
import com.dialect.launcher.matching.ScoredMatch

data class HomeUiState(
    val buffer: String = "",
    val matches: List<ScoredMatch<MatchTarget>> = emptyList(),
    // FR-9: most-used/most-recent apps shown (silently) when the buffer is empty.
    val emptyStateApps: List<MatchTarget.AppTarget> = emptyList(),
) {
    val topMatch: MatchTarget? get() = matches.firstOrNull()?.entry

    // A11Y-5/6: Enter's accessible label always names its target, or explains why it's disabled,
    // instead of a static "Enter" that gives a TalkBack user no equivalent of seeing the top row.
    val enterContentDescription: String
        get() = when (val match = topMatch) {
            null -> "Enter, no matches to open"
            is MatchTarget.AppTarget -> "Enter, opens ${match.displayName}"
            is MatchTarget.ContactTarget -> when (match.actionType) {
                ContactActionType.CALL -> "Enter, calls ${match.displayName}"
                ContactActionType.MESSAGE -> "Enter, messages ${match.displayName}"
            }
        }
}
