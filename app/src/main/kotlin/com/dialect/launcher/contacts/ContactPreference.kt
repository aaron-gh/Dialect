package com.dialect.launcher.contacts

/**
 * A contact's per-action override. Absent (null, from the repository) means "defer to the global
 * default". [AskEveryTime] is a genuine third state, distinct from absent: it means this contact
 * always gets the picker even if a global default is set for everyone else.
 */
sealed class ContactPreference {
    data class UseService(val service: CommunicationService) : ContactPreference()
    data object AskEveryTime : ContactPreference()
}
