package com.dialect.launcher.home

import com.dialect.launcher.contacts.ContactActionType
import com.dialect.launcher.matching.ScoredMatch

// 1.2.0: kept as plain data (not a resolved String) so this class stays Android/Compose-framework
// free, same reasoning as the matching engine's testability split — localized text is resolved
// from this at the Compose layer, where stringResource is actually available.
sealed class EnterDescription {
    data object NoMatches : EnterDescription()
    data class OpensApp(val name: String) : EnterDescription()
    data class Calls(val name: String) : EnterDescription()
    data class Messages(val name: String) : EnterDescription()
}

data class HomeUiState(
    val buffer: String = "",
    val matches: List<ScoredMatch<MatchTarget>> = emptyList(),
    // FR-9: most-used/most-recent apps shown (silently) when the buffer is empty.
    val emptyStateApps: List<MatchTarget.AppTarget> = emptyList(),
) {
    val topMatch: MatchTarget? get() = matches.firstOrNull()?.entry

    // A11Y-5/6: Enter's accessible label always names its target, or explains why it's disabled,
    // instead of a static "Enter" that gives a TalkBack user no equivalent of seeing the top row.
    val enterDescription: EnterDescription
        get() = when (val match = topMatch) {
            null -> EnterDescription.NoMatches
            is MatchTarget.AppTarget -> EnterDescription.OpensApp(match.displayName)
            is MatchTarget.ContactTarget -> when (match.actionType) {
                ContactActionType.CALL -> EnterDescription.Calls(match.displayName)
                ContactActionType.MESSAGE -> EnterDescription.Messages(match.displayName)
            }
        }
}
