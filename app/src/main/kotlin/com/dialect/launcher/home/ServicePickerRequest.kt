package com.dialect.launcher.home

import com.dialect.launcher.contacts.CommunicationService
import com.dialect.launcher.contacts.ContactActionType

/**
 * Pending "which service?" prompt for a contact action. [allowSetAsDefault] distinguishes the two
 * entry points: a tap that couldn't resolve an override/default (one-time choice, not saved) vs.
 * a long-press (always shown, offers to save the choice as that contact's override).
 */
data class ServicePickerRequest(
    val contactId: Long,
    val contactName: String,
    val phoneNumber: String,
    val actionType: ContactActionType,
    val availableServices: List<CommunicationService>,
    val allowSetAsDefault: Boolean,
)
