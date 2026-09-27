package com.dialect.launcher.quickactions

import com.dialect.launcher.contacts.ContactActionType

/**
 * Drives the (multi-step) assignment dialog for an unassigned digit. Plain data, shared between the
 * Home long-press flow (HomeViewModel-held) and the Settings reassignment flow (locally
 * Compose-remember-held) so the dialog Composables in QuickActionDialogs.kt are written once.
 */
sealed class QuickActionFlowState {
    data class ChoosingType(val digit: Char) : QuickActionFlowState()
    data class ChoosingContact(val digit: Char, val actionType: ContactActionType) : QuickActionFlowState()
    data class ChoosingApp(val digit: Char) : QuickActionFlowState()
}
