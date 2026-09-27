package com.dialect.launcher.quickactions

/**
 * A digit's assigned speed-dial-style action. [contactId]/[componentKey] are the same join keys
 * used elsewhere (ContactIndexEntry.contactId, AppIndexEntry.componentKey) — the live entry is
 * looked up fresh at execution time, so a renamed contact or app still resolves correctly. [label]
 * is only a cached display name for the Settings list, same tradeoff already accepted for
 * ContactIndexEntry.displayName.
 */
sealed class QuickAction {
    abstract val label: String

    data class CallContact(val contactId: Long, override val label: String) : QuickAction()
    data class MessageContact(val contactId: Long, override val label: String) : QuickAction()
    data class OpenApp(val componentKey: String, override val label: String) : QuickAction()
}
