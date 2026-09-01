package com.dialect.launcher.home

import com.dialect.launcher.appindex.AppIndexEntry
import com.dialect.launcher.contacts.ContactActionType
import com.dialect.launcher.contacts.ContactIndexEntry
import com.dialect.launcher.matching.T9Nameable

/**
 * Unifies apps and contacts into one thing the (already-generic) MatchEngine can rank together.
 * A contact appears twice in usage-stat terms (componentKey includes the action type) so call and
 * message frequency for the same person are tracked independently.
 */
sealed class MatchTarget : T9Nameable {
    data class AppTarget(val entry: AppIndexEntry) : MatchTarget() {
        override val componentKey get() = entry.componentKey
        override val displayName get() = entry.displayName
        override val fullPrefixDigits get() = entry.fullPrefixDigits
        override val wordInitialDigits get() = entry.wordInitialDigits
    }

    data class ContactTarget(val entry: ContactIndexEntry, val actionType: ContactActionType) : MatchTarget() {
        override val componentKey get() = "${entry.componentKey}:${actionType.name}"
        override val displayName get() = entry.displayName
        override val fullPrefixDigits get() = entry.fullPrefixDigits
        override val wordInitialDigits get() = entry.wordInitialDigits
    }
}
