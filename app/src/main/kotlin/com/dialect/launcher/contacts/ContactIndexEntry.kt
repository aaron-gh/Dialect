package com.dialect.launcher.contacts

import com.dialect.launcher.matching.T9Nameable

/** One contact with a phone number, indexed the same way apps are (T9 prefix/word-initial matching). */
data class ContactIndexEntry(
    val contactId: Long,
    val phoneNumber: String,
    override val displayName: String,
    override val fullPrefixDigits: String,
    override val wordInitialDigits: String,
) : T9Nameable {
    override val componentKey: String get() = "contact:$contactId"
}
